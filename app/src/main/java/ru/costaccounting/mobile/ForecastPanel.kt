package ru.costaccounting.mobile

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** One monthly chart. Month arrows bind the existing views without rebuilding the screen. */
class ForecastPanel(context: Context, private val ledger: Ledger, initialMonth: YearMonth,
    private val onMonthChanged: (YearMonth) -> Unit) : LinearLayout(context) {
    var displayedMonth: YearMonth = initialMonth
        private set
    private val ink = Color.rgb(28, 35, 58)
    private val muted = Color.rgb(111, 121, 144)
    private val primary = Color.rgb(91, 76, 205)
    private val green = Color.rgb(9, 133, 104)
    private val red = Color.rgb(204, 71, 91)
    private val monthLabel = label("", 17, ink, true).apply { gravity = Gravity.CENTER; maxLines = 1 }
    private val previous = arrow("‹", "Предыдущий месяц прогноза") { changeMonth(-1) }
    private val next = arrow("›", "Следующий месяц прогноза") { changeMonth(1) }
    private val chart = MonthlyForecastChart(context)
    private val net = moneyLabel(20).apply { gravity = Gravity.END or Gravity.CENTER_VERTICAL }
    private val details = LinearLayout(context).apply { orientation = VERTICAL; visibility = GONE }
    private val incomeValues = List(4) { moneyLabel(14).apply { setTextColor(green) } }
    private val expenseValues = List(4) { moneyLabel(14).apply { setTextColor(red); gravity = Gravity.END or Gravity.CENTER_VERTICAL } }
    private val incomeExplanation = label("", 12, muted).apply { minLines = 5 }
    private val expenseExplanation = label("", 12, muted).apply { minLines = 5 }

    init {
        orientation = VERTICAL
        addView(label("Прогноз за месяц", 17, ink, true).apply { setPadding(0, dp(16), 0, dp(12)) })
        val selector = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        selector.addView(previous, LayoutParams(dp(48), dp(48)))
        selector.addView(monthLabel, LayoutParams(0, dp(48), 1f))
        selector.addView(next, LayoutParams(dp(48), dp(48)))
        addView(selector, spaced())
        val card = LinearLayout(context).apply {
            orientation = VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(18)); background = shape(Color.WHITE, 22)
        }
        card.addView(label("Доходы и расходы", 16, ink, true))
        card.addView(label("Ожидаемые суммы за весь выбранный месяц", 12, muted).apply { setPadding(0, dp(5), 0, 0) })
        card.addView(chart, LayoutParams(-1, dp(250)).apply { topMargin = dp(12) })
        val balance = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12), dp(10), dp(12), dp(10))
            background = shape(Color.rgb(241, 243, 251), 14)
        }
        balance.addView(label("Итог месяца", 14, ink, true), LayoutParams(0, -2, 1f))
        balance.addView(net, LayoutParams(0, dp(36), 1.4f))
        card.addView(balance, spaced())
        card.addView(label("Доходы минус расходы. Учтены фактические операции, будущие записи, регулярные платежи и оценка по истории.", 12, muted), spaced())
        val toggle = Button(context).apply {
            text = "Показать расчёт"; isAllCaps = false; textSize = 13f; setTextColor(primary)
            background = RippleDrawable(ColorStateList.valueOf(0x185b4ccd), shape(Color.rgb(235, 234, 248), 14), null)
            stateListAnimator = null; minimumHeight = dp(48)
            setOnClickListener {
                details.visibility = if (details.visibility == GONE) VISIBLE else GONE
                text = if (details.visibility == GONE) "Показать расчёт" else "Скрыть расчёт"
            }
        }
        card.addView(toggle, LayoutParams(-1, dp(48)))
        val headers = LinearLayout(context)
        headers.addView(label("Доходы", 12, green, true), LayoutParams(0, -2, 1f))
        headers.addView(label("Расходы", 12, red, true).apply { gravity = Gravity.END }, LayoutParams(0, -2, 1f))
        details.addView(headers, LayoutParams(-1, -2).apply { topMargin = dp(16); bottomMargin = dp(12) })
        listOf("Фактические операции", "Внесённые будущие операции", "Регулярные платежи", "Оценка по истории").forEachIndexed { index, name ->
            details.addView(label(name, 12, muted))
            val line = LinearLayout(context)
            line.addView(incomeValues[index], LayoutParams(0, dp(38), 1f).apply { marginEnd = dp(8) })
            line.addView(expenseValues[index], LayoutParams(0, dp(38), 1f))
            details.addView(line, LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        }
        details.addView(incomeExplanation, spaced()); details.addView(expenseExplanation, spaced())
        details.addView(label("Прогноз — ориентир по вашим данным, а не гарантированная сумма.", 12, muted))
        card.addView(details, LayoutParams(-1, -2))
        addView(card, spaced())
        bind()
    }

    private fun changeMonth(delta: Long) {
        val candidate = displayedMonth.plusMonths(delta)
        if (candidate < YearMonth.now() || candidate > YearMonth.now().plusMonths(12)) return
        displayedMonth = candidate; bind(); onMonthChanged(candidate)
    }

    private fun bind() {
        monthLabel.text = displayedMonth.atDay(1).format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.forLanguageTag("ru")))
        previous.isEnabled = displayedMonth > YearMonth.now()
        next.isEnabled = displayedMonth < YearMonth.now().plusMonths(12)
        val expenses = expenseForecast(ledger, displayedMonth)
        val incomes = incomeForecast(ledger, displayedMonth)
        chart.bind(incomes.total, expenses.total)
        listOf(incomes.received, incomes.recordedFuture, incomes.scheduled, incomes.additionalEstimate).forEachIndexed { i, value -> incomeValues[i].text = Money.format(value) }
        listOf(expenses.spent, expenses.recordedFuture, expenses.scheduled, expenses.variableEstimate).forEachIndexed { i, value -> expenseValues[i].text = Money.format(value) }
        incomeExplanation.text = if (incomes.historyMonths > 0)
            "Доходы: среднемесячный доход по категориям за ${incomes.historyMonths} завершённых месяцев, до 3, за вычетом уже учтённых сумм. Регулярные операции из истории исключены."
            else "Доходы: для дополнительной оценки нужен завершённый месяц истории обычных доходов. Зарплата и другие платежи из плана учитываются сразу."
        expenseExplanation.text = if (expenses.hasHistory)
            "Расходы: средний дневной расход за ${expenses.historyDays} дней, до 90. Регулярные операции исключены; будущие обычные расходы уменьшают оценку."
            else "Расходы: для оценки обычных трат нужно 7 дней истории. Пока учтены фактические и известные будущие расходы."
        val difference = Math.subtractExact(incomes.total, expenses.total)
        net.text = Money.format(difference); net.setTextColor(if (difference >= 0) green else red)
    }

    private fun arrow(text: String, description: String, click: () -> Unit) = Button(context).apply {
        this.text = text; contentDescription = description; textSize = 22f; isAllCaps = false
        setTextColor(ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()), intArrayOf(Color.rgb(174, 181, 199), primary)))
        background = RippleDrawable(ColorStateList.valueOf(0x185b4ccd), shape(Color.rgb(235, 234, 248), 15), null)
        setPadding(0, 0, 0, 0); minWidth = 0; minimumWidth = 0; stateListAnimator = null
        setOnClickListener { click() }
    }
    private fun moneyLabel(size: Int) = label("", size, ink, true).apply {
        maxLines = 1; gravity = Gravity.CENTER_VERTICAL
        setAutoSizeTextTypeUniformWithConfiguration(9, size, 1, TypedValue.COMPLEX_UNIT_SP)
    }
    private fun label(value: String, size: Int, color: Int, bold: Boolean = false) = TextView(context).apply {
        text = value; textSize = size.toFloat(); setTextColor(color); includeFontPadding = false
        setLineSpacing(dp(2).toFloat(), 1f); if (bold) setTypeface(typeface, Typeface.BOLD)
    }
    private fun shape(color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun spaced() = LayoutParams(-1, -2).apply { bottomMargin = dp(12) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density + .5f).toInt()
}
