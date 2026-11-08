package ru.costaccounting.mobile

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID

enum class Frequency(val title: String) { Monthly("Каждый месяц"), Yearly("Каждый год") }

data class RecurringPayment(
    val id: String, val name: String, val amount: Long, val categoryId: String,
    val categoryName: String, val type: CategoryType, val startDate: LocalDate,
    val frequency: Frequency, val nextDate: LocalDate = startDate, val enabled: Boolean = true
)

data class ScheduledPayment(val payment: RecurringPayment, val date: LocalDate) {
    val entryId: String get() = UUID.nameUUIDFromBytes("cashflow:${payment.id}:$date".toByteArray(Charsets.UTF_8)).toString()
}

/** Always calculate from the original date: Jan 31 -> Feb 28 -> Mar 31. */
fun nextOccurrence(start: LocalDate, frequency: Frequency, onOrAfter: LocalDate): LocalDate {
    if (onOrAfter <= start) return start
    val periods = when (frequency) {
        Frequency.Monthly -> ChronoUnit.MONTHS.between(YearMonth.from(start), YearMonth.from(onOrAfter))
        Frequency.Yearly -> (onOrAfter.year - start.year).toLong()
    }.coerceAtLeast(0)
    fun at(index: Long) = when (frequency) {
        Frequency.Monthly -> start.plusMonths(index)
        Frequency.Yearly -> start.plusYears(index)
    }
    val candidate = at(periods)
    return if (candidate < onOrAfter) at(periods + 1) else candidate
}

fun scheduledPayments(payment: RecurringPayment, from: LocalDate, to: LocalDate): List<ScheduledPayment> {
    if (!payment.enabled || from > to) return emptyList()
    var date = nextOccurrence(payment.startDate, payment.frequency, maxOf(from, payment.nextDate))
    return buildList {
        while (date <= to) {
            require(size < 2400) { "Слишком большой период регулярных операций" }
            add(ScheduledPayment(payment, date))
            date = nextOccurrence(payment.startDate, payment.frequency, date.plusDays(1))
        }
    }
}

fun upcomingPayments(ledger: Ledger, from: LocalDate, to: LocalDate): List<ScheduledPayment> {
    val recordedIds = ledger.entries.mapTo(hashSetOf()) { it.id }
    return ledger.recurring.flatMap { scheduledPayments(it, from, to) }
        .filter { it.entryId !in recordedIds }
        .sortedWith(compareBy<ScheduledPayment> { it.date }.thenBy { it.payment.name })
}
