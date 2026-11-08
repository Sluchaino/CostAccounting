package ru.costaccounting.mobile

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class LedgerTest {
    private fun entry(id: String, amount: Long, type: CategoryType = CategoryType.Expense,
        date: String = "2026-11-08T12:00:00Z", category: String = "food", name: String = "Еда") =
        Entry(id, Instant.parse(date).toEpochMilli(), amount, category, name, type, "", 0)

    @Test fun `money parses comma and dot without floating point loss`() {
        assertEquals(12345L, Money.parse(" 123,45 "))
        assertEquals(30L, Money.parse("0.30"))
        assertEquals(100L, Money.parse("1"))
        assertEquals(100L, Money.parse("1.000"))
        assertEquals("123.45", Money.input(12345))
    }

    @Test fun `invalid and out of range amounts are rejected`() {
        listOf("", "NaN", "1.001", "0", "-1", "10000000000", "1e500").forEach {
            assertThrows(IllegalArgumentException::class.java) { Money.parse(it) }
        }
        assertEquals(Money.MAX, Money.parse("9999999999.99"))
    }

    @Test fun `totals are exact down to one kopek`() {
        val rows = listOf(entry("1", 10), entry("2", 20), entry("3", 100, CategoryType.Income))
        assertEquals(Totals(100, 30), totals(rows))
        assertEquals(70, totals(rows).balance)
        assertEquals(Totals(0, 0), totals(emptyList()))
    }

    @Test fun `period boundaries use the phone timezone inclusively`() {
        val rows = listOf(
            entry("before", 1, date = "2026-11-07T20:59:59Z"),
            entry("start", 1, date = "2026-11-07T21:00:00Z"),
            entry("end", 1, date = "2026-11-08T20:59:59.999Z"),
            entry("after", 1, date = "2026-11-08T21:00:00Z")
        )
        val day = LocalDate.parse("2026-11-08")
        assertEquals(listOf("start", "end"), filtered(rows, day, day, zone = ZoneId.of("Europe/Moscow")).map { it.id })
    }

    @Test fun `period filtering handles daylight saving transitions`() {
        val day = LocalDate.parse("2026-11-01")
        val rows = listOf(entry("1", 1, date = "2026-11-01T04:00:00Z"), entry("2", 1, date = "2026-11-02T04:59:59Z"), entry("3", 1, date = "2026-11-02T05:00:00Z"))
        assertEquals(2, filtered(rows, day, day, zone = ZoneId.of("America/New_York")).size)
    }

    @Test fun `filters can combine category and type`() {
        val rows = listOf(entry("1", 100), entry("2", 100, CategoryType.Income, category = "salary"))
        assertEquals(listOf("1"), filtered(rows, null, null, CategoryType.Expense, "food").map { it.id })
        assertTrue(filtered(rows, null, null, CategoryType.Income, "food").isEmpty())
    }

    @Test fun `inverted period is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            filtered(emptyList(), LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 8))
        }
    }

    @Test fun `category report groups sorts and sums exactly`() {
        val rows = listOf(entry("1", 10), entry("2", 20), entry("3", 50, category = "transport", name = "Транспорт"))
        val report = categoryTotals(rows)
        assertEquals(listOf("transport", "food"), report.map { it.categoryId })
        assertEquals(listOf(50L, 30L), report.map { it.amount })
        assertTrue(categoryTotals(emptyList()).isEmpty())
    }
}
