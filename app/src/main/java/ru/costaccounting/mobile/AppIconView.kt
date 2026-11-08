package ru.costaccounting.mobile

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View

/** Small native vector icons; no icon font, images, downloads or animation loop. */
class AppIconView(context: Context, private val icon: Int, color: Int) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color; style = Paint.Style.STROKE; strokeWidth = 1.8f
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }

    override fun onDraw(canvas: Canvas) {
        val scale = minOf(width, height) / 24f
        canvas.save()
        canvas.translate((width - scale * 24) / 2, (height - scale * 24) / 2)
        canvas.scale(scale, scale)
        path.reset()
        when (icon) {
            0 -> {
                path.moveTo(3f, 10f); path.lineTo(12f, 3f); path.lineTo(21f, 10f)
                path.moveTo(5f, 9f); path.lineTo(5f, 21f); path.lineTo(10f, 21f); path.lineTo(10f, 15f)
                path.lineTo(14f, 15f); path.lineTo(14f, 21f); path.lineTo(19f, 21f); path.lineTo(19f, 9f)
                canvas.drawPath(path, paint)
            }
            1 -> {
                path.moveTo(3f, 7f); path.lineTo(20f, 7f); path.lineTo(16f, 3f)
                path.moveTo(20f, 7f); path.lineTo(16f, 11f)
                path.moveTo(21f, 17f); path.lineTo(4f, 17f); path.lineTo(8f, 13f)
                path.moveTo(4f, 17f); path.lineTo(8f, 21f)
                canvas.drawPath(path, paint)
            }
            2 -> {
                canvas.drawRoundRect(3f, 3f, 10f, 10f, 2f, 2f, paint)
                canvas.drawRoundRect(14f, 3f, 21f, 10f, 2f, 2f, paint)
                canvas.drawRoundRect(3f, 14f, 10f, 21f, 2f, 2f, paint)
                canvas.drawRoundRect(14f, 14f, 21f, 21f, 2f, 2f, paint)
            }
            3 -> {
                canvas.drawCircle(12f, 12f, 9f, paint)
                path.moveTo(12f, 3f); path.lineTo(12f, 12f); path.lineTo(19f, 17f)
                canvas.drawPath(path, paint)
            }
            4 -> {
                canvas.drawArc(3f, 3f, 21f, 21f, -55f, 300f, false, paint)
                path.moveTo(16f, 3f); path.lineTo(21f, 4f); path.lineTo(20f, 9f)
                canvas.drawPath(path, paint)
            }
        }
        canvas.restore()
    }
}
