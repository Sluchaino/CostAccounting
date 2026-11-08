package ru.costaccounting.mobile

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class IncomeForecastTest {
    private val today = LocalDate.of(2026, 11, 8)
    private val zone = ZoneId.of("UTC")
    private fun entry(id: String, date: String, amount: Long, category: String = "salary", origin: String? = null,
        type: CategoryType = CategoryType.Income) = Entry(id, Instant.parse("${date}T12:00:00Z").toEpochMilli(), amount,
        category, category, type, "", 0, origin)
    private fun history(amount: Long = 100000, category: String = "salary") = listOf(
        entry("aug-$category", "2026-08-15", amount, category), entry("sep-$category", "2026-09-15", amount, category),
        entry("oct-$category", "2026-10-15", amount, category))
    private fun payment(amount: Long = 100000, type: CategoryType = CategoryType.Income) = RecurringPayment("rule", "Зарплата", amount,
        "salary", "Зарплата", type, LocalDate.of(2026, 11, 15), Frequency.Monthly)
    private fun forecast(ledger: Ledger, month: String = "2026-11") = incomeForecast(ledger, YearMonth.parse(month), today, zone)

    @Test fun `empty history relies on known income and never treats expenses as income`() {
        val ledger = Ledger(entries = listOf(entry("spent", "2026-11-01", 900000, type = CategoryType.Expense)),
            recurring = listOf(payment(), payment(900000, CategoryType.Expense).copy(id = "rent")))
        val result = forecast(ledger)
        assertEquals(100000, result.total)
        assertEquals(0, result.historyMonths)
        assertEquals(0, result.additionalEstimate)
    }

    @Test fun `salary received this month is not extrapolated as daily income`() {
        val result = forecast(Ledger(entries = history() + entry("nov", "2026-11-01", 100000)))
        assertEquals(100000, result.received)
        assertEquals(0, result.additionalEstimate)
        assertEquals(100000, result.total)
    }

    @Test fun `only completed months contribute to the additional income estimate`() {
        val currentOnly = Ledger(entries = listOf(entry("nov", "2026-11-01", 900000)))
        assertEquals(0, forecast(currentOnly).historyMonths)
        assertEquals(0, forecast(currentOnly, "2026-12").total)
        assertEquals(100000, forecast(Ledger(entries = history()), "2026-12").total)
    }

    @Test fun `known salary replaces historical expectation while freelance is estimated separately`() {
        val ledger = Ledger(entries = history() + history(20000, "freelance"), recurring = listOf(payment(120000)))
        val result = forecast(ledger)
        assertEquals(120000, result.scheduled)
        assertEquals(20000, result.additionalEstimate)
        assertEquals(140000, result.total)
    }

    @Test fun `already entered future payments reduce the unaccounted monthly income`() {
        val ledger = Ledger(entries = history() + entry("future", "2026-11-10", 60000), recurring = listOf(payment(40000)))
        assertEquals(100000, forecast(ledger).total)
        assertEquals(0, forecast(ledger).additionalEstimate)
        val larger = Ledger(entries = history() + entry("future", "2026-11-10", 120000))
        assertEquals(120000, forecast(larger).total)
    }

    @Test fun `generated salary is excluded from ordinary income history and not duplicated in the future`() {
        val rule = payment()
        val occurrence = ScheduledPayment(rule, rule.startDate)
        val ledger = Ledger(entries = listOf(entry("past", "2026-10-01", 100000, origin = rule.id),
            entry(occurrence.entryId, "2026-11-15", 100000, origin = rule.id)), recurring = listOf(rule))
        val result = forecast(ledger)
        assertEquals(0, result.historyMonths)
        assertEquals(100000, result.recordedFuture)
        assertEquals(0, result.scheduled)
        assertEquals(100000, result.total)
    }

    @Test fun `history window is limited to three months and includes months without income`() {
        val rows = listOf(entry("old", "2025-01-01", 99999999), entry("aug", "2026-08-15", 90000))
        val result = forecast(Ledger(entries = rows))
        assertEquals(3, result.historyMonths)
        assertEquals(30000, result.additionalEstimate)
    }

    @Test fun `monthly averages round only to whole kopecks and historical forecasts contain only actuals`() {
        val rows = listOf(entry("aug", "2026-08-01", 1), entry("sep", "2026-09-01", 1), entry("oct", "2026-10-01", 2))
        assertEquals(1, forecast(Ledger(entries = rows)).additionalEstimate)
        assertEquals(2, forecast(Ledger(entries = rows), "2026-10").total)
    }

    @Test fun `local timezone determines received versus future income`() {
        val e = entry("late", "2026-11-08", 5000).copy(date = Instant.parse("2026-11-08T22:30:00Z").toEpochMilli())
        val result = incomeForecast(Ledger(entries = listOf(e)), YearMonth.of(2026, 11), today, ZoneId.of("Europe/Moscow"))
        assertEquals(0, result.received)
        assertEquals(5000, result.recordedFuture)
    }
}
