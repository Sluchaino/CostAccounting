package ru.costaccounting.mobile

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class IncomeForecast(
    val month: YearMonth, val received: Long, val recordedFuture: Long, val scheduled: Long,
    val additionalEstimate: Long, val historyMonths: Int
) {
    val total: Long get() = Math.addExact(Math.addExact(received, recordedFuture), Math.addExact(scheduled, additionalEstimate))
}

/** Income is estimated by completed months, so a salary is never extrapolated as daily income. */
fun incomeForecast(ledger: Ledger, month: YearMonth, today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault()): IncomeForecast {
    fun Entry.day() = Instant.ofEpochMilli(date).atZone(zone).toLocalDate()
    fun List<Entry>.sumMoney() = fold(0L) { sum, e -> Math.addExact(sum, e.amount) }
    val incomes = ledger.entries.filter { it.type == CategoryType.Income }
    val start = month.atDay(1)
    val end = month.atEndOfMonth()
    val actual = incomes.filter { it.day() in start..minOf(today, end) }
    val future = incomes.filter { it.day() in maxOf(start, today.plusDays(1))..end }
    val planned = upcomingPayments(ledger, maxOf(start, today.plusDays(1)), end)
        .filter { it.payment.type == CategoryType.Income }
    val scheduled = planned.fold(0L) { sum, p -> Math.addExact(sum, p.payment.amount) }
    val currentMonth = YearMonth.from(today)
    val ordinary = incomes.filter { it.recurringId == null && it.day() < currentMonth.atDay(1) }
    val firstMonth = ordinary.minOfOrNull { YearMonth.from(it.day()) }
    val months = firstMonth?.let { ChronoUnit.MONTHS.between(it, currentMonth).toInt().coerceIn(0, 3) } ?: 0
    val historyStart = currentMonth.minusMonths(months.toLong()).atDay(1)
    val historyByCategory = ordinary.filter { it.day() >= historyStart }.groupBy { it.categoryId }
    val knownByCategory = (actual + future).groupBy { it.categoryId }.mapValues { (_, rows) -> rows.sumMoney() }.toMutableMap()
    planned.forEach { p -> knownByCategory[p.payment.categoryId] = Math.addExact(knownByCategory[p.payment.categoryId] ?: 0L, p.payment.amount) }
    val additional = if (months > 0 && month >= currentMonth) historyByCategory.values.fold(0L) { sum, rows ->
        val average = BigDecimal.valueOf(rows.sumMoney()).divide(BigDecimal.valueOf(months.toLong()), 0, RoundingMode.HALF_UP).longValueExact()
        Math.addExact(sum, (average - (knownByCategory[rows.first().categoryId] ?: 0L)).coerceAtLeast(0))
    } else 0L
    return IncomeForecast(month, actual.sumMoney(), future.sumMoney(), scheduled, additional, months)
}
