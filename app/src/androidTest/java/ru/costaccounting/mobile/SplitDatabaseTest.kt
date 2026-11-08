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
class SplitDatabaseTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: LedgerDatabase
    private lateinit var name: String
    @Before fun setup() { name = "test-${UUID.randomUUID()}.db"; db = LedgerDatabase(context, name) }
    @After fun cleanup() { db.close(); context.deleteDatabase(name) }

    @Test fun purchaseCanBeSavedThenDistributedAndReopenedWithoutChangingTotal() {
        val cats = db.load().categories.filter { it.type == CategoryType.Expense }.take(2)
        val parts = cats.map { EntryAllocation(it.id, it.name, it.type, null) }
        db.saveEntry(null, 123, 10001, cats[0].id, "Чек", parts, "Магазин")
        val first = db.load().entries.single()
        assertEquals(10001, first.unallocatedAmount())
        db.saveEntry(first.id, first.date, first.amount, first.categoryId, first.comment,
            listOf(parts[0].copy(amount = 6000), parts[1].copy(amount = 4001)), first.title)
        db.close(); db = LedgerDatabase(context, name)
        val updated = db.load().entries.single()
        assertEquals(first.id, updated.id); assertEquals(first.createdAt, updated.createdAt)
        assertEquals(0, updated.unallocatedAmount()); assertEquals(10001, totals(listOf(updated)).expense)
        assertEquals(4001, totals(listOf(updated), cats[1].id).expense)
        assertEquals(db.load(), Backup.decode(Backup.encode(db.load())))
    }

    @Test fun allocationCategoryCannotBeDeletedOrTurnedIntoIncomeUntilRemovedFromPurchase() {
        val cats = db.load().categories.filter { it.type == CategoryType.Expense }.take(2)
        db.saveEntry(null, 123, 100, cats[0].id, "", cats.map { EntryAllocation(it.id, it.name, it.type, null) })
        assertThrows(IllegalArgumentException::class.java) { db.deleteCategory(cats[1].id) }
        assertThrows(IllegalArgumentException::class.java) { db.saveCategory(cats[1].id, cats[1].name, CategoryType.Income) }
        db.deleteEntry(db.load().entries.single().id)
        db.deleteCategory(cats[1].id)
        assertFalse(db.load().categories.any { it.id == cats[1].id })
    }

    @Test fun invalidAllocationNeverModifiesTheOriginalPurchase() {
        val cats = db.load().categories.filter { it.type == CategoryType.Expense }.take(2)
        db.saveEntry(null, 123, 100, cats[0].id, "До изменения")
        val original = db.load().entries.single()
        assertThrows(IllegalArgumentException::class.java) {
            db.saveEntry(original.id, 456, 100, cats[0].id, "Нельзя сохранить", listOf(
                EntryAllocation(cats[0].id, cats[0].name, cats[0].type, 101),
                EntryAllocation(cats[1].id, cats[1].name, cats[1].type, null)))
        }
        assertEquals(original, db.load().entries.single())
    }
}
