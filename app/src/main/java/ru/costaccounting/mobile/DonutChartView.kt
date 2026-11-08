package ru.costaccounting.mobile

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextUtils
import android.text.TextPaint
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

object ChartColors {
    private val colors = intArrayOf(0xff6554c0.toInt(), 0xff098c86.toInt(), 0xffd96745.toInt(), 0xff327bd1.toInt(),
        0xffc34a82.toInt(), 0xffa27616.toInt(), 0xff5c8b3c.toInt(), 0xff6359a8.toInt(), 0xffb54c50.toInt(), 0xff31758c.toInt())
    fun forId(id: String): Int = colors[(id.hashCode() and Int.MAX_VALUE) % colors.size]
    fun forIndex(index: Int): Int = colors[index % colors.size]
}

class DonutChartView(context: Context, private var slices: List<DonutSlice>, private var caption: String) : View(context) {
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.BUTT }
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val bounds = RectF()
    private var total = slices.fold(0L) { sum, slice -> Math.addExact(sum, slice.amount) }
    private var selected = -1
    private var radius = 0f
    private var stroke = 0f

    init { isClickable = true; isFocusable = true; describe() }

    fun bind(slices: List<DonutSlice>, caption: String) {
        this.slices = slices; this.caption = caption
        total = slices.fold(0L) { sum, slice -> Math.addExact(sum, slice.amount) }
        selected = -1; describe(); invalidate()
    }

    fun select(index: Int) {
        selected = if (index == selected || index !in slices.indices) -1 else index
        describe(); invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        radius = min(width, height) / 2f - dp(22)
        stroke = min(dp(34), radius * .32f)
        if (radius <= 0) return
        bounds.set(cx - radius, cy - radius, cx + radius, cy + radius)
        arcPaint.strokeWidth = stroke
        arcPaint.color = Color.rgb(234, 237, 245)
        canvas.drawCircle(cx, cy, radius, arcPaint)
        slices.forEachIndexed { index, slice ->
            val gap = if (slices.size == 1) 0f else min(2.2f, slice.sweep.toFloat() * .15f)
            arcPaint.color = ChartColors.forIndex(index)
            arcPaint.alpha = if (selected == -1 || selected == index) 255 else 85
            arcPaint.strokeWidth = if (index == selected) stroke + dp(5) else stroke
            canvas.drawArc(bounds, slice.start.toFloat() - 90 + gap / 2, (slice.sweep.toFloat() - gap).coerceAtLeast(0f), false, arcPaint)
        }
        val current = slices.getOrNull(selected)
        val available = (radius - stroke / 2) * 2 - dp(18)
        textPaint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        textPaint.textSize = sp(12)
        textPaint.color = Color.rgb(112, 120, 142)
        val label = current?.name ?: if (slices.isEmpty()) "Пока нет операций" else caption
        canvas.drawText(TextUtils.ellipsize(label, textPaint, available, TextUtils.TruncateAt.END).toString(), cx, cy - dp(14), textPaint)
        val value = Money.format(current?.amount ?: total)
        textPaint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        textPaint.textSize = sp(24)
        while (textPaint.measureText(value) > available && textPaint.textSize > sp(10)) textPaint.textSize -= sp(1)
        textPaint.color = Color.rgb(28, 35, 58)
        canvas.drawText(value, cx, cy + dp(13), textPaint)
        textPaint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        textPaint.textSize = sp(11)
        textPaint.color = Color.rgb(112, 120, 142)
        canvas.drawText(current?.percentage ?: if (slices.isEmpty()) "Добавьте первую запись" else "Нажмите на сектор", cx, cy + dp(34), textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val x = event.x - width / 2f
            val y = event.y - height / 2f
            val distance = hypot(x, y)
            if (distance <= radius + stroke / 2 + dp(8)) {
                if (distance < radius - stroke / 2) select(-1)
                else select(donutSliceAt(slices, Math.toDegrees(atan2(y.toDouble(), x.toDouble())) + 90))
                performClick()
            }
            return true
        }
        return event.action == MotionEvent.ACTION_DOWN || super.onTouchEvent(event)
    }

    override fun performClick(): Boolean { super.performClick(); return true }

    private fun describe() {
        contentDescription = "Круговая диаграмма. $caption: ${Money.format(total)}. " +
            (slices.getOrNull(selected)?.let { "Выбрано: ${it.name}, ${Money.format(it.amount)}, ${it.percentage}." }
                ?: slices.joinToString(". ") { "${it.name}: ${Money.format(it.amount)}, ${it.percentage}" })
    }
    private fun dp(value: Int): Float = value * resources.displayMetrics.density
    private fun sp(value: Int): Float = value * resources.displayMetrics.scaledDensity
}
