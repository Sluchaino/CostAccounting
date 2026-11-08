package ru.costaccounting.mobile

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: LedgerDatabase
    private lateinit var name: String

    @Before fun setUp() { name = "test-${UUID.randomUUID()}.db"; database = LedgerDatabase(context, name) }
    @After fun tearDown() { database.close(); context.deleteDatabase(name) }

    @Test fun seedsOnceAndPersistsAfterReopening() {
        val category = database.load().categories.first()
        database.saveEntry(null, 1234567, 12345, category.id, "Проверка")
        val original = database.load()
        database.close()
        database = LedgerDatabase(context, name)
        assertEquals(original, database.load())
        assertEquals(5, database.load().categories.size)
    }

    @Test fun categoryCrudValidatesNamesAndDuplicateCyrillicCase() {
        database.saveCategory(null, "  Проверка  ", CategoryType.Income)
        val category = database.load().categories.single { it.name == "Проверка" }
        assertThrows(IllegalArgumentException::class.java) { database.saveCategory(null, "ПРОВЕРКА", CategoryType.Expense) }
        assertThrows(IllegalArgumentException::class.java) { database.saveCategory(null, "  ", CategoryType.Expense) }
        database.saveCategory(category.id, "Изменено", CategoryType.Expense)
        assertEquals(CategoryType.Expense, database.load().categories.single { it.id == category.id }.type)
        database.deleteCategory(category.id)
        assertFalse(database.load().categories.any { it.id == category.id })
    }

    @Test fun transactionCrudKeepsCreationTimeAndRestrictsCategoryDeletion() {
        val category = database.load().categories.first()
        database.saveEntry(null, 1000, 101, category.id, "  текст  ")
        val created = database.load().entries.single()
        assertEquals("текст", created.comment)
        assertThrows(IllegalArgumentException::class.java) { database.deleteCategory(category.id) }
        database.saveEntry(created.id, 2000, 202, category.id, "Изменено")
        val updated = database.load().entries.single()
        assertEquals(created.createdAt, updated.createdAt)
        assertEquals(202, updated.amount)
        database.saveCategory(category.id, category.name, CategoryType.Income)
        assertEquals(202, totals(database.load().entries).income)
        database.deleteEntry(created.id)
        database.deleteCategory(category.id)
        assertTrue(database.load().entries.isEmpty())
    }

    @Test fun rejectsInvalidAmountsAndReferences() {
        val category = database.load().categories.first()
        assertThrows(IllegalArgumentException::class.java) { database.saveEntry(null, 0, 0, category.id, "") }
        assertThrows(IllegalArgumentException::class.java) { database.saveEntry(null, 0, 1, "missing", "") }
        assertThrows(IllegalArgumentException::class.java) { database.saveEntry(null, 0, 1, category.id, "x".repeat(256)) }
        assertTrue(database.load().entries.isEmpty())
    }

    @Test fun backupRoundTripAndRejectedRestorePreserveData() {
        val category = database.load().categories.first()
        database.saveEntry(null, 1000, 303, category.id, "Резервная копия")
        val original = database.load()
        val encoded = Backup.encode(original)
        val decoded = Backup.decode(encoded)
        database.restore(Ledger())
        assertTrue(database.load().categories.isEmpty())
        database.restore(decoded)
        assertEquals(original, database.load())
        assertThrows(Exception::class.java) { Backup.decode(encoded.replace("303", "1.5")) }
        assertThrows(Exception::class.java) { Backup.decode(encoded.replace("\"version\": 3", "\"version\": 999")) }
        val broken = original.copy(categories = original.categories + original.categories.first())
        assertThrows(Exception::class.java) { database.restore(broken) }
        assertEquals(original, database.load())
    }

    @Test fun appDoesNotRequestInternetPermission() {
        @Suppress("DEPRECATION")
        val permissions = context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_PERMISSIONS).requestedPermissions
        assertFalse(permissions?.contains("android.permission.INTERNET") == true)
    }
}
