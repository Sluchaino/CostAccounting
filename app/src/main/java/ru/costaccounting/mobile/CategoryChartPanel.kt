package ru.costaccounting.mobile

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** One donut recalculated over the included flows; filtering never rebuilds the page. */
class CategoryChartPanel(context: Context, private val entries: List<Entry>, initialType: CategoryType?,
    private val onFilterChanged: (CategoryType?) -> Unit) : LinearLayout(context) {
    private val primary = Color.rgb(91, 76, 205)
    private val ink = Color.rgb(28, 35, 58)
    private val muted = Color.rgb(111, 121, 144)
    private var selectedType = initialType
    private val chart = DonutChartView(context, emptyList(), "Общий оборот")
    private val legend = LinearLayout(context).apply { orientation = VERTICAL }
    private val note = label("", 12, muted)
    private val filters = listOf<CategoryType?>(null, CategoryType.Expense, CategoryType.Income)
    private val buttons = mutableListOf<Button>()

    init {
        orientation = VERTICAL
        val picker = LinearLayout(context)
        filters.forEachIndexed { index, filter ->
            val button = Button(context).apply {
                text = listOf("Всё", "Расходы", "Доходы")[index]; isAllCaps = false; textSize = 13f; stateListAnimator = null
                contentDescription = "Диаграмма: $text"; setPadding(dp(6), 0, dp(6), 0)
                setOnClickListener { selectedType = filter; bind(); onFilterChanged(filter) }
            }
            buttons += button
            picker.addView(button, LayoutParams(0, dp(48), 1f).apply { if (index > 0) marginStart = dp(6) })
        }
        addView(picker, LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        val card = LinearLayout(context).apply {
            orientation = VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(18)); background = shape(Color.WHITE, 22)
        }
        card.addView(chart, LayoutParams(-1, dp(260)))
        card.addView(note, LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        card.addView(legend, LayoutParams(-1, -2))
        addView(card, LayoutParams(-1, -2))
        bind()
    }

    private fun bind() {
        buttons.forEachIndexed { index, button ->
            val active = filters[index] == selectedType
            button.background = shape(if (active) primary else Color.rgb(235, 234, 248), 14)
            button.setTextColor(if (active) Color.WHITE else primary)
            button.isSelected = active
        }
        val grouped = categoryTotals(entries.filter { selectedType == null || it.type == selectedType })
        val slices = donutSlices(grouped)
        val caption = when (selectedType) { null -> "Общий оборот"; CategoryType.Expense -> "Всего расходов"; CategoryType.Income -> "Всего доходов" }
        chart.bind(slices, caption)
        note.text = if (slices.isEmpty()) "Нет операций за выбранный период" else if (selectedType == null)
            "Доходы и расходы вместе. Размер сектора — доля в общем обороте, а не баланс."
            else "Доли пересчитаны только среди ${if (selectedType == CategoryType.Expense) "расходов" else "доходов"}. Нажмите на сектор или категорию."
        legend.removeAllViews()
        slices.forEachIndexed { index, slice ->
            val type = grouped[index].type
            val line = LinearLayout(context).apply {
                gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(10), 0, dp(10)); minimumHeight = dp(54)
                isClickable = true; isFocusable = true
                contentDescription = "${slice.name}, ${type.title}, ${Money.format(slice.amount)}, ${slice.percentage}"
                setOnClickListener { chart.select(index) }
            }
            line.addView(View(context).apply { background = shape(ChartColors.forIndex(index), 4) }, LayoutParams(dp(10), dp(10)).apply { marginEnd = dp(10) })
            val captionBox = LinearLayout(context).apply { orientation = VERTICAL }
            captionBox.addView(label(slice.name, 14, ink, true).apply { maxLines = 2 })
            captionBox.addView(label("${type.title} · ${slice.percentage}", 11, muted))
            line.addView(captionBox, LayoutParams(0, -2, 1f).apply { marginEnd = dp(8) })
            line.addView(label(Money.format(slice.amount), 15, if (type == CategoryType.Income) Color.rgb(9, 133, 104) else ink, true).apply {
                maxLines = 1; gravity = Gravity.END or Gravity.CENTER_VERTICAL
                setAutoSizeTextTypeUniformWithConfiguration(9, 15, 1, TypedValue.COMPLEX_UNIT_SP)
            }, LayoutParams(0, dp(36), 1f))
            legend.addView(line)
        }
    }
    private fun label(value: String, size: Int, color: Int, bold: Boolean = false) = TextView(context).apply {
        text = value; textSize = size.toFloat(); setTextColor(color); includeFontPadding = false
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }
    private fun shape(color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun dp(value: Int) = (value * resources.displayMetrics.density + .5f).toInt()
}
