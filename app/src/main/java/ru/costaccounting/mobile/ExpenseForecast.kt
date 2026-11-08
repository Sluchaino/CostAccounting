package ru.costaccounting.mobile

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class ExpenseForecast(
    val month: YearMonth, val spent: Long, val recordedFuture: Long, val scheduled: Long,
    val variableEstimate: Long, val historyDays: Int, val remainingDays: Int
) {
    val total: Long get() = Math.addExact(Math.addExact(spent, recordedFuture), Math.addExact(scheduled, variableEstimate))
    val hasHistory: Boolean get() = historyDays >= 7
}

/** Local estimate, not a bank prediction: actuals + known future + scheduled + daily spending trend. */
fun expenseForecast(ledger: Ledger, month: YearMonth, today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault()): ExpenseForecast {
    val start = month.atDay(1)
    val end = month.atEndOfMonth()
    val expenses = ledger.entries.filter { it.type == CategoryType.Expense }
    fun Entry.day() = Instant.ofEpochMilli(date).atZone(zone).toLocalDate()
    fun List<Entry>.sumMoney() = fold(0L) { sum, e -> Math.addExact(sum, e.amount) }
    val spent = expenses.filter { it.day() in start..minOf(today, end) }.sumMoney()
    val future = expenses.filter { it.day() in maxOf(start, today.plusDays(1))..end }
    val ordinary = expenses.filter { it.recurringId == null && it.day() < today }
    val first = ordinary.minOfOrNull { it.day() }
    val historyStart = maxOf(today.minusDays(90), first ?: today)
    val days = ChronoUnit.DAYS.between(historyStart, today).toInt().coerceAtLeast(0)
    val historyTotal = ordinary.filter { it.day() >= historyStart }.sumMoney()
    val remaining = ChronoUnit.DAYS.between(maxOf(start, today.plusDays(1)), end.plusDays(1)).toInt().coerceAtLeast(0)
    val estimate = if (days >= 7 && remaining > 0) BigDecimal.valueOf(historyTotal)
        .multiply(BigDecimal.valueOf(remaining.toLong())).divide(BigDecimal.valueOf(days.toLong()), 0, RoundingMode.HALF_UP)
        .longValueExact() else 0L
    // Future ordinary entries already consume part of the estimated variable budget.
    val variable = (estimate - future.filter { it.recurringId == null }.sumMoney()).coerceAtLeast(0)
    val scheduled = upcomingPayments(ledger, maxOf(start, today.plusDays(1)), end)
        .filter { it.payment.type == CategoryType.Expense }.fold(0L) { sum, p -> Math.addExact(sum, p.payment.amount) }
    return ExpenseForecast(month, spent, future.sumMoney(), scheduled, variable, days, remaining)
}
