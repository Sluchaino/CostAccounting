package ru.costaccounting.mobile

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

class SplitPurchaseTest {
    private val food = Category("00000000-0000-0000-0000-000000000001", "Еда", CategoryType.Expense)
    private val home = Category("00000000-0000-0000-0000-000000000002", "Дом", CategoryType.Expense)
    private val salary = Category("00000000-0000-0000-0000-000000000003", "Зарплата", CategoryType.Income)
    private fun part(c: Category, value: Long?) = EntryAllocation(c.id, c.name, c.type, value)
    private fun purchase(parts: List<EntryAllocation> = listOf(part(food, 6000), part(home, 2500))) =
        Entry("00000000-0000-0000-0000-000000000004", 123, 10000, food.id, food.name, food.type, "Чек", 123,
            title = "Магазин", allocations = parts)
    private fun income() = Entry("00000000-0000-0000-0000-000000000005", 123, 30000, salary.id, salary.name, salary.type, "", 123)

    @Test fun `partial allocation counts the purchase once and reports the remainder separately`() {
        val entry = purchase()
        assertEquals(Totals(0, 10000), totals(listOf(entry)))
        assertEquals(1500, entry.unallocatedAmount())
        assertEquals(mapOf(food.id to 6000L, home.id to 2500L, UNALLOCATED_CATEGORY to 1500L), categoryTotals(listOf(entry)).associate { it.categoryId to it.amount })
    }

    @Test fun `categories can be chosen before any amounts are entered`() {
        val entry = purchase(listOf(part(food, null), part(home, null)))
        validateAllocations(entry.amount, entry.allocations)
        assertEquals(10000, categoryTotals(listOf(entry)).single().amount)
        assertEquals(UNALLOCATED_CATEGORY, categoryTotals(listOf(entry)).single().categoryId)
        assertEquals(10000, totals(listOf(entry)).expense)
        assertTrue(entry.hasCategory(food.id))
        assertEquals(0, totals(listOf(entry), food.id).expense)
    }

    @Test fun `fully allocated purchase has no unallocated sector`() {
        val entry = purchase(listOf(part(food, 6000), part(home, 4000)))
        assertEquals(0, entry.unallocatedAmount())
        assertEquals(2, categoryTotals(listOf(entry)).size)
        assertEquals(10000, categoryTotals(listOf(entry)).sumOf { it.amount })
    }

    @Test fun `invalid amounts duplicates mixed types and excess allocations are rejected`() {
        listOf(listOf(part(food, 10000)), listOf(part(food, 6000), part(food, 4000)),
            listOf(part(food, 10001), part(home, null)), listOf(part(food, -1), part(home, null)),
            listOf(part(food, 0), part(home, null)), listOf(part(food, 5000), part(salary, 5000))).forEach { parts ->
            assertThrows(IllegalArgumentException::class.java) { validateAllocations(10000, parts) }
        }
    }

    @Test fun `category filters keep the original editable entry but summarize only its allocated share`() {
        val entry = purchase()
        val matches = filtered(listOf(entry, income()), null, null, categoryId = home.id)
        assertSame(entry, matches.single())
        assertEquals(10000, matches.single().amount)
        assertEquals(2500, totals(matches, home.id).expense)
        assertEquals(1500, totals(filtered(listOf(entry), null, null, categoryId = UNALLOCATED_CATEGORY), UNALLOCATED_CATEGORY).expense)
    }

    @Test fun `combined donut uses turnover and recalculates shares after excluding a flow`() {
        val entries = listOf(purchase(), income())
        val all = donutSlices(categoryTotals(entries))
        val expense = donutSlices(categoryTotals(entries.filter { it.type == CategoryType.Expense }))
        val incomes = donutSlices(categoryTotals(entries.filter { it.type == CategoryType.Income }))
        assertEquals(40000, all.sumOf { it.amount })
        assertEquals("15,0 %", all.single { it.id == food.id }.percentage)
        assertEquals("60,0 %", expense.single { it.id == food.id }.percentage)
        assertEquals("100,0 %", incomes.single().percentage)
        assertEquals(Totals(30000, 10000), totals(entries))
    }

    @Test fun `backup preserves optional allocations and still imports old formats`() {
        val ledger = Ledger(listOf(food, home, salary), listOf(purchase(listOf(part(food, null), part(home, 2500)))))
        assertEquals(ledger, Backup.decode(Backup.encode(ledger)))
        val plain = ledger.copy(entries = listOf(income()))
        for (version in 1..2) {
            val json = JSONObject(Backup.encode(plain)).put("version", version)
            json.getJSONArray("entries").getJSONObject(0).apply { remove("allocations"); remove("title") }
            assertEquals(plain, Backup.decode(json.toString()))
        }
    }

    @Test fun `backup rejects invalid allocation references sums and primary category`() {
        val ledger = Ledger(listOf(food, home, salary), listOf(purchase()))
        listOf<(JSONObject) -> Unit>(
            { it.getJSONArray("allocations").getJSONObject(0).put("amount", 10001) },
            { it.getJSONArray("allocations").getJSONObject(1).put("categoryId", "missing") },
            { it.getJSONArray("allocations").getJSONObject(1).put("categoryId", salary.id) },
            { it.put("categoryId", home.id) }
        ).forEach { change ->
            val json = JSONObject(Backup.encode(ledger)); change(json.getJSONArray("entries").getJSONObject(0))
            assertThrows(Exception::class.java) { Backup.decode(json.toString()) }
        }
    }
}
