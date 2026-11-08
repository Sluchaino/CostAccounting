package ru.costaccounting.mobile

import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ForecastUiTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun find(root: View, predicate: (View) -> Boolean): View? {
        if (predicate(root)) return root
        if (root is ViewGroup) for (i in 0 until root.childCount) find(root.getChildAt(i), predicate)?.let { return it }
        return null
    }

    @Test fun changingMonthsKeepsTheSameScrollViewAndViewport() {
        val activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        try {
            instrumentation.runOnMainSync { find(activity.window.decorView) { it.contentDescription == "Отчёты" }!!.performClick() }
            var panel: ForecastPanel? = null
            val deadline = SystemClock.uptimeMillis() + 30000
            while (panel == null && SystemClock.uptimeMillis() < deadline) {
                instrumentation.runOnMainSync { panel = find(activity.window.decorView) { it is ForecastPanel && it.isLaidOut } as? ForecastPanel }
                if (panel == null) SystemClock.sleep(100)
            }
            val original = requireNotNull(panel)
            assertNotNull(find(original) { it is MonthlyForecastChart })
            val initialMonth = original.displayedMonth
            lateinit var scroll: ScrollView
            var offset = 0
            instrumentation.runOnMainSync {
                var parent = original.parent
                while (parent !is ScrollView) parent = parent.parent
                scroll = parent
                val maxScroll = (scroll.getChildAt(0).height - scroll.height).coerceAtLeast(0)
                scroll.scrollTo(0, minOf(original.top, maxScroll))
                offset = scroll.scrollY
                assertTrue(offset > 0)
                repeat(3) { find(original) { it.contentDescription == "Следующий месяц прогноза" }!!.performClick() }
            }
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync {
                assertSame(original, find(activity.window.decorView) { it is ForecastPanel })
                assertEquals(initialMonth.plusMonths(3), original.displayedMonth)
                assertEquals(offset, scroll.scrollY)
                find(original) { it.contentDescription == "Предыдущий месяц прогноза" }!!.performClick()
            }
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync { assertEquals(offset, scroll.scrollY); assertEquals(initialMonth.plusMonths(2), original.displayedMonth) }
        } finally { instrumentation.runOnMainSync { activity.finish() } }
    }
}
