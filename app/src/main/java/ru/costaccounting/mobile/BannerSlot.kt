package ru.costaccounting.mobile

import android.content.Context
import android.graphics.Color
import android.widget.FrameLayout
import kotlin.math.floor

/** Reserved outside the page scroll. Attach the future ad view here after this view is laid out. */
class BannerSlot(context: Context) : FrameLayout(context) {
    companion object { const val HEIGHT_DP = 50 }

    // Re-read after a size change, including rotation and split-screen. Never assume 320 dp.
    // Future SDK size: BannerAdSize.fixed(context, availableWidthDp, HEIGHT_DP).
    val availableWidthDp: Int
        get() = floor((width - paddingLeft - paddingRight).coerceAtLeast(0) / resources.displayMetrics.density).toInt()

    init {
        setBackgroundColor(Color.WHITE)
        // The empty reservation should not become a focus stop for accessibility services.
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }
}
