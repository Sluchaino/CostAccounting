package ru.costaccounting.mobile

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiSmokeTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private fun find(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        fun walk(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null
            if (predicate(node)) return node
            for (index in 0 until node.childCount) walk(node.getChild(index))?.let { return it }
            return null
        }
        return walk(instrumentation.uiAutomation.rootInActiveWindow)
    }

    private fun waitFor(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo {
        val end = SystemClock.uptimeMillis() + 30000
        while (SystemClock.uptimeMillis() < end) {
            find(predicate)?.let { return it }
            SystemClock.sleep(100)
        }
        throw AssertionError("Expected native UI element was not found")
    }

    @Test fun nativeScreensAndUnsavedFormSurviveActivityRecreation() {
        var activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        try {
            assertNotNull(waitFor { it.text?.toString() == "Обзор финансов" })
            waitFor { it.contentDescription?.toString() == "Операции" }.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            waitFor { it.text?.toString() == "+ Новая операция" }.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            val amount = waitFor { it.contentDescription?.toString() == "Сумма в рублях" }
            assertTrue(amount.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "123.45")
            }))
            val monitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)
            instrumentation.runOnMainSync { activity.recreate() }
            activity = requireNotNull(monitor.waitForActivityWithTimeout(30000)) as MainActivity
            instrumentation.removeMonitor(monitor)
            assertNotNull(waitFor { it.contentDescription?.toString() == "Сумма в рублях" && it.text?.toString() == "123,45" })
            waitFor { it.text?.toString()?.equals("Отмена", ignoreCase = true) == true }.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            waitFor { it.contentDescription?.toString() == "Категории" }.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            assertNotNull(waitFor { it.text?.toString() == "+ Новая категория" })
            waitFor { it.contentDescription?.toString() == "Отчёты" }.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            assertNotNull(waitFor { it.text?.toString() == "По категориям" })
            assertNotNull(waitFor { it.contentDescription?.toString()?.startsWith("Круговая диаграмма") == true })
            waitFor { it.contentDescription?.toString() == "План" }.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            waitFor { it.text?.toString() == "+ Регулярный платёж" }.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            assertNotNull(waitFor { it.contentDescription?.toString() == "Сумма регулярного платежа" })
            assertNotNull(waitFor { it.contentDescription?.toString() == "Периодичность" })
            waitFor { it.text?.toString()?.equals("Отмена", ignoreCase = true) == true }.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
        }
    }
}
