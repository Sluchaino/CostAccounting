package ru.costaccounting.mobile

import java.math.BigDecimal
import java.math.RoundingMode

data class DonutSlice(val id: String, val name: String, val amount: Long, val start: Double, val sweep: Double, val percentage: String)

fun donutSlices(categories: List<CategoryTotal>): List<DonutSlice> {
    val positive = categories.filter { it.amount > 0 }
    val total = positive.fold(0L) { sum, item -> Math.addExact(sum, item.amount) }
    if (total == 0L) return emptyList()
    var start = 0.0
    return positive.mapIndexed { index, item ->
        val sweep = if (index == positive.lastIndex) 360.0 - start else item.amount.toDouble() / total * 360.0
        val percent = BigDecimal.valueOf(item.amount).multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP)
        val label = if (percent < BigDecimal("0.1")) "<0,1 %" else percent.toPlainString().replace('.', ',') + " %"
        DonutSlice(item.categoryId, item.name, item.amount, start, sweep, label).also { start += sweep }
    }
}

fun donutSliceAt(slices: List<DonutSlice>, degreesFromTop: Double): Int {
    if (!degreesFromTop.isFinite()) return -1
    val angle = ((degreesFromTop % 360) + 360) % 360
    return slices.indexOfFirst { angle >= it.start && angle < it.start + it.sweep }
}
