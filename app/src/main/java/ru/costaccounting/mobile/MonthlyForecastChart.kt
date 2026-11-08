package ru.costaccounting.mobile

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View

/** One shared scale for the two monthly totals; money is never rounded in labels. */
class MonthlyForecastChart(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private var income = 0L
    private var expense = 0L

    fun bind(income: Long, expense: Long) {
        this.income = income; this.expense = expense
        contentDescription = "График прогноза за месяц. Доходы: ${Money.format(income)}. Расходы: ${Money.format(expense)}."
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val top = dp(40)
        val baseline = height - dp(36)
        val plotHeight = (baseline - top).coerceAtLeast(0f)
        val left = dp(12)
        val right = width - dp(12)
        if (plotHeight <= 0 || right <= left) return
        paint.color = Color.rgb(234, 238, 246); paint.strokeWidth = dp(1)
        repeat(4) { index ->
            val y = baseline - plotHeight * index / 3
            canvas.drawLine(left, y, right, y, paint)
        }
        val maximum = maxOf(income, expense).coerceAtLeast(1L)
        val centers = floatArrayOf(width * .28f, width * .72f)
        val barWidth = minOf(dp(72), width * .22f)
        val amounts = longArrayOf(income, expense)
        val colors = intArrayOf(Color.rgb(9, 133, 104), Color.rgb(204, 71, 91))
        amounts.forEachIndexed { index, amount ->
            val barHeight = (amount.toDouble() / maximum * plotHeight).toFloat()
            val barTop = baseline - barHeight
            paint.color = if (amount == 0L) Color.rgb(220, 225, 237) else colors[index]
            canvas.drawRoundRect(centers[index] - barWidth / 2, if (amount == 0L) baseline - dp(3) else barTop,
                centers[index] + barWidth / 2, baseline, dp(10), dp(10), paint)
            val value = Money.format(amount)
            textPaint.color = Color.rgb(28, 35, 58); textPaint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
            textPaint.textSize = sp(16)
            val available = width * .44f - dp(8)
            while (textPaint.measureText(value) > available && textPaint.textSize > sp(8)) textPaint.textSize -= sp(1)
            canvas.drawText(value, centers[index], barTop - dp(11), textPaint)
            textPaint.typeface = Typeface.create("sans-serif", Typeface.NORMAL); textPaint.textSize = sp(13)
            textPaint.color = colors[index]
            canvas.drawText(if (index == 0) "Доходы" else "Расходы", centers[index], baseline + dp(24), textPaint)
        }
    }
    private fun dp(value: Int) = value * resources.displayMetrics.density
    private fun sp(value: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value.toFloat(), resources.displayMetrics)
}
