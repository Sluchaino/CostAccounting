package ru.costaccounting.mobile

const val UNALLOCATED_CATEGORY = "__unallocated_expense__"

data class EntryAllocation(val categoryId: String, val categoryName: String, val categoryType: CategoryType, val amount: Long?)
data class CategoryContribution(val categoryId: String, val categoryName: String, val categoryType: CategoryType, val amount: Long)

fun validateAllocations(total: Long, allocations: List<EntryAllocation>) {
    require(total in 1..Money.MAX) { "Некорректная общая стоимость покупки" }
    if (allocations.isEmpty()) return
    require(allocations.size in 2..50) { "Выберите от 2 до 50 категорий покупки" }
    require(allocations.map { it.categoryId }.distinct().size == allocations.size) { "Категории покупки не должны повторяться" }
    require(allocations.all { it.categoryType == CategoryType.Expense }) { "В составной покупке могут быть только категории расходов" }
    require(allocations.all { it.amount == null || it.amount in 1..Money.MAX }) { "Укажите положительную сумму категории или оставьте поле пустым" }
    val assigned = allocations.fold(0L) { sum, part -> Math.addExact(sum, part.amount ?: 0L) }
    require(assigned <= total) { "Суммы по категориям превышают общую стоимость покупки на ${Money.format(assigned - total)}" }
}

fun Entry.unallocatedAmount(): Long = if (allocations.isEmpty()) 0 else
    (amount - allocations.fold(0L) { sum, part -> Math.addExact(sum, part.amount ?: 0L) }).coerceAtLeast(0)

fun Entry.hasCategory(id: String): Boolean = if (allocations.isEmpty()) categoryId == id
    else allocations.any { it.categoryId == id } || (id == UNALLOCATED_CATEGORY && unallocatedAmount() > 0)

fun Entry.categoryContributions(): List<CategoryContribution> {
    if (allocations.isEmpty()) return listOf(CategoryContribution(categoryId, categoryName, type, amount))
    validateAllocations(amount, allocations)
    val assigned = allocations.mapNotNull { part -> part.amount?.let { CategoryContribution(part.categoryId, part.categoryName, part.categoryType, it) } }
    val remaining = unallocatedAmount()
    return if (remaining > 0) assigned + CategoryContribution(UNALLOCATED_CATEGORY, "Не распределено", CategoryType.Expense, remaining) else assigned
}
