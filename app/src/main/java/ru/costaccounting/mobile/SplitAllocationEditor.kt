package ru.costaccounting.mobile

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class SplitAllocationEditor(context: Context, categories: List<Category>, initial: List<EntryAllocation>,
    private val total: () -> Long?, saved: Bundle? = null) : LinearLayout(context) {
    private val categories = categories.filter { it.type == CategoryType.Expense }
    private val selected = mutableListOf<String>()
    private val inputs = linkedMapOf<String, MoneyInputView>()
    private val states = mutableMapOf<String, Bundle>()
    private val initialAmounts = initial.associate { it.categoryId to it.amount }
    private val rows = LinearLayout(context).apply { orientation = VERTICAL }
    private val status = TextView(context).apply { textSize = 13f; setPadding(0, dp(8), 0, dp(8)) }
    private val choose = Button(context).apply {
        textSize = 14f; isAllCaps = false; minHeight = dp(52); setTextColor(Color.rgb(91, 76, 205)); background = FormStyle.background(context)
        setOnClickListener {
            AppDialogs.chooseMultiple(context, "Категории покупки", this@SplitAllocationEditor.categories.map { it.name },
                this@SplitAllocationEditor.categories.indices.filter { this@SplitAllocationEditor.categories[it].id in selected }.toSet()) { indices ->
                remember(); selected.clear(); selected.addAll(indices.map { this@SplitAllocationEditor.categories[it].id }); rebuild()
            }
        }
    }
    var onChanged: (() -> Unit)? = null
    init {
        orientation = VERTICAL
        @Suppress("DEPRECATION")
        val draftRows = saved?.getParcelableArrayList<Bundle>("parts").orEmpty()
        draftRows.forEach { row -> row.getString("id")?.let { states[it] = row.getBundle("input") ?: Bundle() } }
        selected.addAll(saved?.getStringArrayList("ids") ?: initial.map { it.categoryId })
        selected.retainAll(this.categories.map { it.id }.toSet())
        addView(choose, LayoutParams(-1, dp(52))); addView(rows); addView(status)
        rebuild()
    }
    private fun remember() { inputs.forEach { (id, input) -> states[id] = input.snapshot() } }
    private fun rebuild() {
        rows.removeAllViews(); inputs.clear()
        choose.isEnabled = categories.size >= 2
        choose.text = if (categories.size < 2) "Нужны две категории расходов" else if (selected.isEmpty()) "Выбрать категории" else "Изменить категории (${selected.size})"
        selected.forEach { id ->
            val category = categories.first { it.id == id }
            val row = LinearLayout(context).apply { orientation = VERTICAL; setPadding(0, dp(14), 0, 0) }
            row.addView(TextView(context).apply { text = category.name; textSize = 14f; setTextColor(Color.rgb(28, 35, 58)); setPadding(0, 0, 0, dp(6)) })
            val input = MoneyInputView(context, initialAmounts[id]?.let(Money::input) ?: "", "Сумма в категории ${category.name}", false)
            input.restore(states[id]); input.onValueChanged = { refreshTotal(); onChanged?.invoke() }
            inputs[id] = input
            row.addView(input); rows.addView(row)
        }
        refreshTotal(); onChanged?.invoke()
    }
    fun allocations(): List<EntryAllocation> = selected.map { id ->
        val category = categories.first { it.id == id }; val input = inputs.getValue(id)
        val amount = if (input.rawText.isBlank()) null else try { Money.parse(input.decimalValue()) }
            catch (error: IllegalArgumentException) { input.error = error.message; throw IllegalArgumentException("${category.name}: ${error.message}") }
        EntryAllocation(id, category.name, category.type, amount)
    }
    fun refreshTotal() {
        val budget = total()
        val assigned = try {
            inputs.values.fold(0L) { sum, input -> Math.addExact(sum, if (input.rawText.isBlank()) 0L else Money.parse(input.decimalValue())) }
        } catch (_: IllegalArgumentException) { status.text = "Проверьте введённые суммы категорий"; status.setTextColor(Color.rgb(204, 71, 91)); return }
        val remaining = budget?.let { it - assigned }
        status.setTextColor(if (remaining != null && remaining < 0) Color.rgb(204, 71, 91) else Color.rgb(111, 121, 144))
        status.text = when {
            categories.size < 2 -> "Сначала создайте как минимум две категории расходов на странице «Категории»"
            remaining == null -> "Укажите общую стоимость покупки выше"
            remaining < 0 -> "Распределено больше общей суммы на ${Money.format(-remaining)}"
            remaining == 0L -> "Распределено полностью: ${Money.format(assigned)}"
            else -> "Распределено ${Money.format(assigned)} · осталось ${Money.format(remaining)}"
        }
    }
    fun snapshot(): Bundle {
        remember()
        return Bundle().apply {
            putStringArrayList("ids", ArrayList(selected))
            putParcelableArrayList("parts", ArrayList(selected.map { id -> Bundle().apply { putString("id", id); putBundle("input", states[id]) } }))
        }
    }
    private fun dp(value: Int) = (value * context.resources.displayMetrics.density + .5f).toInt()
}
