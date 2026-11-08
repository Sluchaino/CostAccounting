package ru.costaccounting.mobile

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.ScrollView
import android.widget.TextView

object AppDialogs {
    private val primary = Color.rgb(91, 76, 205)
    private val ink = Color.rgb(28, 35, 58)
    private val muted = Color.rgb(111, 121, 144)
    fun header(context: Context, title: String, subtitle: String) = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(context, 24), dp(context, 22), dp(context, 24), dp(context, 16))
        addView(TextView(context).apply { text = title; textSize = 22f; setTextColor(ink); setTypeface(typeface, Typeface.BOLD) })
        if (subtitle.isNotEmpty()) addView(TextView(context).apply { text = subtitle; textSize = 13f; setTextColor(muted); setPadding(0, dp(context, 6), 0, 0) })
    }
    fun style(dialog: AlertDialog) {
        val context = dialog.context
        dialog.window?.apply {
            setBackgroundDrawable(shape(context, Color.WHITE, 24))
            setLayout(minOf(context.resources.displayMetrics.widthPixels - dp(context, 32), dp(context, 560)), ViewGroup.LayoutParams.WRAP_CONTENT)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        listOf(AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL).forEach { which ->
            dialog.getButton(which)?.apply {
                isAllCaps = false; textSize = 14f; minimumHeight = dp(context, 48)
                setTextColor(when (which) { AlertDialog.BUTTON_POSITIVE -> primary; AlertDialog.BUTTON_NEUTRAL -> Color.rgb(204, 71, 91); else -> muted })
                if (which == AlertDialog.BUTTON_POSITIVE) setTypeface(typeface, Typeface.BOLD)
            }
        }
    }
    fun choose(context: Context, title: String, items: List<String>, selected: Int, result: (Int) -> Unit) {
        val body = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(context, 18), 0, dp(context, 18), dp(context, 12)) }
        val scroll = ScrollView(context).apply { addView(body) }
        val dialog = AlertDialog.Builder(context).setCustomTitle(header(context, title, "Выберите подходящий вариант"))
            .setView(scroll).setNegativeButton("Отмена", null).create()
        items.forEachIndexed { index, name ->
            val row = LinearLayout(context).apply {
                gravity = Gravity.CENTER_VERTICAL; minimumHeight = dp(context, 60); setPadding(dp(context, 8), dp(context, 6), dp(context, 12), dp(context, 6))
                background = shape(context, if (index == selected) Color.rgb(241, 238, 253) else Color.WHITE, 14)
                isClickable = true; isFocusable = true; contentDescription = name + if (index == selected) ", выбрано" else ""
                setOnClickListener { result(index); dialog.dismiss() }
            }
            row.addView(RadioButton(context).apply { isChecked = index == selected; isClickable = false; isFocusable = false; buttonTintList = ColorStateList.valueOf(primary) })
            row.addView(TextView(context).apply { text = name; textSize = 15f; setTextColor(ink); maxLines = 3 }, LinearLayout.LayoutParams(0, -2, 1f))
            body.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(context, 4) })
        }
        dialog.setOnShowListener { style(dialog); scroll.layoutParams = scroll.layoutParams.apply { height = minOf(dp(context, 420), dp(context, items.size * 64 + 12)) } }
        dialog.show()
    }
    fun chooseMultiple(context: Context, title: String, items: List<String>, selected: Set<Int>, result: (List<Int>) -> Unit) {
        val checked = selected.toMutableSet()
        val body = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(context, 18), 0, dp(context, 18), dp(context, 12)) }
        val error = TextView(context).apply { textSize = 13f; setTextColor(Color.rgb(204, 71, 91)); visibility = android.view.View.GONE }
        body.addView(error)
        items.forEachIndexed { index, name ->
            val box = CheckBox(context).apply {
                text = name; textSize = 15f; setTextColor(ink); isChecked = index in checked; minHeight = dp(context, 60)
                buttonTintList = ColorStateList.valueOf(primary); setPadding(dp(context, 8), dp(context, 6), dp(context, 8), dp(context, 6))
                setOnCheckedChangeListener { _, yes -> if (yes) checked.add(index) else checked.remove(index); error.visibility = android.view.View.GONE }
            }
            body.addView(box, LinearLayout.LayoutParams(-1, -2))
        }
        val scroll = ScrollView(context).apply { addView(body) }
        val dialog = AlertDialog.Builder(context).setCustomTitle(header(context, title, "Выберите от 2 до 50 категорий расходов"))
            .setView(scroll).setNegativeButton("Отмена", null).setPositiveButton("Готово", null).create()
        dialog.setOnShowListener {
            style(dialog); scroll.layoutParams = scroll.layoutParams.apply { height = minOf(dp(context, 420), dp(context, items.size * 60 + 12)) }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (checked.size !in 2..50) { error.text = "Выберите от 2 до 50 категорий"; error.visibility = android.view.View.VISIBLE; scroll.smoothScrollTo(0, 0) }
                else { result(checked.sorted()); dialog.dismiss() }
            }
        }
        dialog.show()
    }
    fun confirm(context: Context, message: String, action: () -> Unit) {
        val dialog = AlertDialog.Builder(context).setCustomTitle(header(context, "Подтверждение", message))
            .setNegativeButton("Отмена", null).setPositiveButton("Удалить") { _, _ -> action() }.create()
        dialog.setOnShowListener { style(dialog); dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.rgb(204, 71, 91)) }
        dialog.show()
    }
    private fun shape(context: Context, color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(context, radius).toFloat() }
    private fun dp(context: Context, value: Int) = (value * context.resources.displayMetrics.density + .5f).toInt()
}
