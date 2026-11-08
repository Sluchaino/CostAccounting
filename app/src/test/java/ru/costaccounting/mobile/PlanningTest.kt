package ru.costaccounting.mobile

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class PlanningTest {
    private val today = LocalDate.of(2026, 11, 8)
    private val zone = ZoneId.of("UTC")
    private fun payment(start: String, frequency: Frequency = Frequency.Monthly, amount: Long = 5000,
        type: CategoryType = CategoryType.Expense) = RecurringPayment("rent", "Аренда", amount, "home", "Дом", type,
        LocalDate.parse(start), frequency)
    private fun entry(id: String, date: String, amount: Long, origin: String? = null, type: CategoryType = CategoryType.Expense) =
        Entry(id, Instant.parse("${date}T12:00:00Z").toEpochMilli(), amount, "home", "Дом", type, "", 0, origin)
    private fun forecast(ledger: Ledger, month: String = "2026-11") = expenseForecast(ledger, YearMonth.parse(month), today, zone)

    @Test fun `monthly schedule keeps original day after February`() {
        val p = payment("2025-01-31")
        assertEquals(listOf("2025-01-31", "2025-02-28", "2025-03-31", "2025-04-30", "2025-05-31"),
            scheduledPayments(p, p.startDate, LocalDate.of(2025, 5, 31)).map { it.date.toString() })
    }

    @Test fun `monthly schedule respects leap year and inclusive boundary`() {
        val p = payment("2024-01-31")
        assertEquals(LocalDate.of(2024, 2, 29), nextOccurrence(p.startDate, p.frequency, LocalDate.of(2024, 2, 1)))
        assertEquals(1, scheduledPayments(p, LocalDate.of(2024, 2, 29), LocalDate.of(2024, 2, 29)).size)
    }

    @Test fun `yearly leap day returns in next leap year without drifting`() {
        val p = payment("2024-02-29", Frequency.Yearly)
        assertEquals(listOf("2024-02-29", "2025-02-28", "2026-02-28", "2027-02-28", "2028-02-29"),
            scheduledPayments(p, p.startDate, LocalDate.of(2028, 3, 1)).map { it.date.toString() })
    }

    @Test fun `pending cursor prevents replay after a generated entry was deleted`() {
        val p = payment("2026-01-31").copy(nextDate = LocalDate.of(2026, 11, 30))
        assertTrue(scheduledPayments(p, p.startDate, today).isEmpty())
        assertEquals(LocalDate.of(2026, 11, 30), scheduledPayments(p, today, LocalDate.of(2026, 12, 1)).single().date)
    }

    @Test fun `disabled schedule and dates before its start produce no occurrences`() {
        val p = payment("2026-12-10")
        assertTrue(scheduledPayments(p, today, LocalDate.of(2026, 11, 30)).isEmpty())
        assertTrue(scheduledPayments(p.copy(enabled = false), today, today.plusYears(1)).isEmpty())
        assertEquals(p.startDate, nextOccurrence(p.startDate, p.frequency, today))
    }

    @Test fun `catch up creates each missed occurrence exactly once with stable unique IDs`() {
        val p = payment("2026-08-08")
        val due = scheduledPayments(p, p.startDate, today)
        assertEquals(4, due.size)
        assertEquals(4, due.map { it.entryId }.distinct().size)
        assertEquals(due.map { it.entryId }, scheduledPayments(p, p.startDate, today).map { it.entryId })
        assertNotEquals(due.first().entryId, due.first().copy(payment = p.copy(id = "another")).entryId)
    }

    @Test fun `resuming can skip paused periods and editing starts from the new date`() {
        assertEquals(LocalDate.of(2026, 11, 20), nextOccurrence(LocalDate.of(2026, 1, 20), Frequency.Monthly, today))
        assertEquals(LocalDate.of(2026, 12, 1), nextOccurrence(LocalDate.of(2026, 1, 1), Frequency.Monthly, today))
    }

    @Test fun `future generated operation is not also counted as an upcoming scheduled payment`() {
        val p = payment("2026-11-10")
        val occurrence = ScheduledPayment(p, p.startDate)
        val ledger = Ledger(entries = listOf(entry(occurrence.entryId, "2026-11-10", 5000, p.id)), recurring = listOf(p))
        assertTrue(upcomingPayments(ledger, today, today.plusDays(30)).isEmpty())
        assertEquals(5000, forecast(ledger).total)
        assertEquals(0, forecast(ledger).scheduled)
    }

    @Test fun `empty history uses only scheduled expenses and ignores recurring income`() {
        val ledger = Ledger(recurring = listOf(payment("2026-11-10"), payment("2026-11-15", amount = 100000, type = CategoryType.Income).copy(id = "salary")))
        val f = forecast(ledger)
        assertFalse(f.hasHistory)
        assertEquals(5000, f.total)
        assertEquals(0, f.variableEstimate)
    }

    @Test fun `forecast sums actual expenses planned expenses and daily variable trend`() {
        val ledger = Ledger(entries = listOf(entry("1", "2026-11-01", 7000)), recurring = listOf(payment("2026-11-10")))
        val f = forecast(ledger)
        assertEquals(7, f.historyDays)
        assertEquals(22, f.remainingDays)
        assertEquals(7000, f.spent)
        assertEquals(22000, f.variableEstimate)
        assertEquals(34000, f.total)
    }

    @Test fun `generated regular expenses never inflate the variable spending baseline`() {
        val ledger = Ledger(entries = listOf(entry("1", "2026-11-01", 7000), entry("2", "2026-11-02", 90000, "rent")))
        assertEquals(22000, forecast(ledger).variableEstimate)
        assertEquals(97000, forecast(ledger).spent)
    }

    @Test fun `future ordinary entries reduce remaining variable estimate instead of doubling it`() {
        val history = entry("1", "2026-11-01", 7000)
        val f = forecast(Ledger(entries = listOf(history, entry("2", "2026-11-10", 3000))))
        assertEquals(3000, f.recordedFuture)
        assertEquals(19000, f.variableEstimate)
        assertEquals(29000, f.total)
        val expensive = forecast(Ledger(entries = listOf(history, entry("2", "2026-11-10", 30000))))
        assertEquals(0, expensive.variableEstimate)
        assertEquals(37000, expensive.total)
    }

    @Test fun `next month uses its actual length and includes yearly payments only in the right month`() {
        val ledger = Ledger(entries = listOf(entry("1", "2026-11-01", 7000)),
            recurring = listOf(payment("2025-12-15", Frequency.Yearly, 50000).copy(nextDate = LocalDate.of(2026, 12, 15))))
        assertEquals(81000, forecast(ledger, "2026-12").total)
        assertEquals(31000, forecast(ledger, "2027-01").total)
    }

    @Test fun `less than seven days of history is not extrapolated`() {
        val f = forecast(Ledger(entries = listOf(entry("1", "2026-11-07", 10000))))
        assertFalse(f.hasHistory)
        assertEquals(10000, f.total)
        assertEquals(0, f.variableEstimate)
    }

    @Test fun `history is limited to ninety days and excludes future income`() {
        val history = listOf(entry("old", "2025-01-01", 999999), entry("recent", today.minusDays(90).toString(), 9000),
            entry("salary", "2026-11-09", 100000, type = CategoryType.Income))
        val f = forecast(Ledger(entries = history))
        assertEquals(90, f.historyDays)
        assertEquals(2200, f.variableEstimate)
        assertEquals(0, f.recordedFuture)
    }

    @Test fun `historical month has only actual expenses and no projected spending`() {
        val ledger = Ledger(entries = listOf(entry("1", "2026-10-01", 12345)), recurring = listOf(payment("2026-11-10")))
        assertEquals(12345, forecast(ledger, "2026-10").total)
        assertEquals(0, forecast(ledger, "2026-10").remainingDays)
    }

    @Test fun `phone timezone determines whether an entry is today or tomorrow`() {
        val tx = Entry("1", Instant.parse("2026-11-08T22:00:00Z").toEpochMilli(), 12345, "home", "Дом", CategoryType.Expense, "", 0)
        val f = expenseForecast(Ledger(entries = listOf(tx)), YearMonth.of(2026, 11), today, ZoneId.of("Europe/Moscow"))
        assertEquals(0, f.spent)
        assertEquals(12345, f.recordedFuture)
    }
}
