package ru.costaccounting.mobile

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

enum class CategoryType(val title: String) { Expense("Расход"), Income("Доход") }
data class Category(val id: String, val name: String, val type: CategoryType)
data class Entry(
    val id: String, val date: Long, val amount: Long, val categoryId: String,
    val categoryName: String, val type: CategoryType, val comment: String, val createdAt: Long,
    val recurringId: String? = null, val title: String = "", val allocations: List<EntryAllocation> = emptyList()
)
data class Ledger(val categories: List<Category> = emptyList(), val entries: List<Entry> = emptyList(),
    val recurring: List<RecurringPayment> = emptyList())
data class Totals(val income: Long, val expense: Long) { val balance: Long get() = income - expense }
data class CategoryTotal(val categoryId: String, val name: String, val type: CategoryType, val amount: Long)

object Money {
    const val MAX = 999_999_999_999L
    fun parse(value: String): Long {
        val decimal = try { BigDecimal(value.trim().replace(',', '.')) }
            catch (_: NumberFormatException) { throw IllegalArgumentException("Введите сумму") }
        val minor = try { decimal.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact() }
            catch (_: ArithmeticException) { throw IllegalArgumentException("Укажите не больше двух знаков после запятой") }
        require(minor in 1..MAX) { "Сумма должна быть от 0,01 до 9 999 999 999,99 ₽" }
        return minor
    }
    fun input(minor: Long): String = BigDecimal.valueOf(minor, 2).toPlainString()
    fun format(minor: Long): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("ru-RU"))
        .apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }
        .format(BigDecimal.valueOf(minor, 2))
}

fun filtered(entries: List<Entry>, from: LocalDate?, to: LocalDate?, type: CategoryType? = null,
    categoryId: String? = null, zone: ZoneId = ZoneId.systemDefault()): List<Entry> {
    require(from == null || to == null || !from.isAfter(to)) { "Начало периода должно быть не позже окончания" }
    return entries.filter { entry ->
        val date = Instant.ofEpochMilli(entry.date).atZone(zone).toLocalDate()
        (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to)) &&
            (type == null || type == entry.type) && (categoryId == null || entry.hasCategory(categoryId))
    }
}

fun totals(entries: List<Entry>, categoryId: String? = null): Totals {
    val rows = entries.flatMap { it.categoryContributions() }.filter { categoryId == null || it.categoryId == categoryId }
    return Totals(rows.filter { it.categoryType == CategoryType.Income }.fold(0L) { sum, e -> Math.addExact(sum, e.amount) },
        rows.filter { it.categoryType == CategoryType.Expense }.fold(0L) { sum, e -> Math.addExact(sum, e.amount) })
}

fun categoryTotals(entries: List<Entry>): List<CategoryTotal> = entries.flatMap { it.categoryContributions() }
    .groupBy { it.categoryId to it.categoryType }.map { (key, group) ->
    val first = group.first()
    CategoryTotal(key.first, first.categoryName, first.categoryType, group.fold(0L) { sum, e -> Math.addExact(sum, e.amount) })
}.sortedWith(compareByDescending<CategoryTotal> { it.amount }.thenBy { it.name })
