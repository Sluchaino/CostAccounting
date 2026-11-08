package ru.costaccounting.mobile

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RecurringDatabaseTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: LedgerDatabase
    private lateinit var name: String
    private var clock = LocalDate.of(2026, 3, 31)
    @Before fun setUp() { name = "test-${UUID.randomUUID()}.db"; db = LedgerDatabase(context, name) { clock } }
    @After fun tearDown() { db.close(); context.deleteDatabase(name) }
    private fun day(e: Entry) = Instant.ofEpochMilli(e.date).atZone(ZoneId.systemDefault()).toLocalDate().toString()

    @Test fun catchUpIsIdempotentAndDeletedEntriesNeverReappear() {
        val category = db.load().categories.first()
        db.saveRecurring(null, "Аренда", 5000, category.id, LocalDate.of(2026, 1, 31), Frequency.Monthly, true)
        val first = db.load()
        assertEquals(listOf("2026-03-31", "2026-02-28", "2026-01-31"), first.entries.map(::day))
        assertEquals(first, db.load())
        db.deleteEntry(first.entries.last().id)
        assertEquals(2, db.load().entries.size)
        db.close(); db = LedgerDatabase(context, name) { clock }
        clock = LocalDate.of(2026, 4, 30)
        assertEquals(listOf("2026-04-30", "2026-03-31", "2026-02-28"), db.load().entries.map(::day))
    }

    @Test fun pauseAndResumeSkipPausedMonthsAndEditingKeepsPastAmounts() {
        val category = db.load().categories.first()
        val start = LocalDate.of(2026, 1, 1)
        db.saveRecurring(null, "Подписка", 100, category.id, start, Frequency.Monthly, false)
        val p = db.load().recurring.single()
        assertTrue(db.load().entries.isEmpty())
        db.saveRecurring(p.id, p.name, p.amount, p.categoryId, start, p.frequency, true)
        assertTrue(db.load().entries.isEmpty())
        clock = LocalDate.of(2026, 4, 1)
        assertEquals(100, db.load().entries.single().amount)
        db.saveRecurring(p.id, "Новая цена", 200, p.categoryId, start, p.frequency, false)
        clock = LocalDate.of(2026, 6, 1)
        assertEquals(1, db.load().entries.size)
        clock = LocalDate.of(2026, 7, 1)
        db.saveRecurring(p.id, "Новая цена", 200, p.categoryId, start, p.frequency, true)
        assertEquals(listOf("2026-07-01", "2026-04-01"), db.load().entries.map(::day))
        assertEquals(listOf(200L, 100L), db.load().entries.map { it.amount })
    }

    @Test fun deletingRulePreservesHistoryAndBackupPreservesCursor() {
        val category = db.load().categories.first()
        db.saveRecurring(null, "Аренда", 5000, category.id, LocalDate.of(2026, 1, 31), Frequency.Monthly, true)
        db.deleteEntry(db.load().entries.last().id)
        val snapshot = db.load()
        db.restore(Backup.decode(Backup.encode(snapshot)))
        assertEquals(snapshot, db.load())
        db.deleteRecurring(snapshot.recurring.single().id)
        assertTrue(db.load().recurring.isEmpty())
        assertEquals(snapshot.entries, db.load().entries)
        assertTrue(db.load().entries.all { it.recurringId != null })
    }

    @Test fun categoriesUsedByFutureRulesAreProtected() {
        val category = db.load().categories.first()
        db.saveRecurring(null, "Страховка", 100, category.id, LocalDate.of(2030, 1, 1), Frequency.Yearly, true)
        assertThrows(IllegalArgumentException::class.java) { db.deleteCategory(category.id) }
        db.deleteRecurring(db.load().recurring.single().id)
        db.deleteCategory(category.id)
        assertFalse(db.load().categories.any { it.id == category.id })
    }

    @Test fun upgradingV1PreservesExistingOperationsAndCategories() {
        db.close()
        context.openOrCreateDatabase(name, 0, null).use { old ->
            old.execSQL("CREATE TABLE categories(id TEXT PRIMARY KEY,name TEXT NOT NULL,normalized_name TEXT NOT NULL UNIQUE,type TEXT NOT NULL)")
            old.execSQL("CREATE TABLE entries(id TEXT PRIMARY KEY,date INTEGER NOT NULL,amount INTEGER NOT NULL,category_id TEXT NOT NULL REFERENCES categories(id),comment TEXT NOT NULL,created_at INTEGER NOT NULL)")
            old.execSQL("INSERT INTO categories VALUES('cat','Дом','дом','Expense')")
            old.execSQL("INSERT INTO entries VALUES('old',123,1001,'cat','Старая запись',123)")
            old.version = 1
        }
        db = LedgerDatabase(context, name) { clock }
        val migrated = db.load()
        assertEquals(1, migrated.categories.size)
        assertEquals(1001, migrated.entries.single().amount)
        assertNull(migrated.entries.single().recurringId)
        assertTrue(migrated.recurring.isEmpty())
        assertEquals(3, db.readableDatabase.version)
    }
}
