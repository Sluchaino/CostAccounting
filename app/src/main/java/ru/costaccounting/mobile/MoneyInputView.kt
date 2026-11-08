package ru.costaccounting.mobile

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.LocaleList
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

/** One decimal field, always displaying a comma and preserving kopecks exactly. */
class MoneyInputView(context: Context, initial: String, description: String, private val showHint: Boolean = true) : LinearLayout(context) {
    private val amount = EditText(context)
    private val frame = LinearLayout(context)
    private val help = TextView(context)
    private var changing = false
    private var errorMessage: CharSequence? = null
    var onValueChanged: (() -> Unit)? = null
    val rawText: String get() = amount.text.toString()
    var error: CharSequence?
        get() = errorMessage
        set(value) {
            errorMessage = value
            help.text = value ?: "Например, 1 250,45 ₽"
            help.visibility = if (value != null || showHint) VISIBLE else GONE
            help.setTextColor(if (value == null) Color.rgb(111, 121, 144) else Color.rgb(204, 71, 91))
            frame.background = FormStyle.background(context, value != null)
        }

    init {
        orientation = VERTICAL
        val field = frame.apply {
            orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setAddStatesFromChildren(true)
            setPadding(dp(14), 0, dp(16), 0)
            background = FormStyle.background(context)
        }
        amount.apply {
            contentDescription = description
            textSize = 24f; setTypeface(typeface, Typeface.BOLD); setTextColor(Color.rgb(28, 35, 58))
            setSingleLine(); setSelectAllOnFocus(true); background = null
            setPadding(0, dp(8), dp(8), dp(8)); hint = "0,00"
            keyListener = DigitsKeyListener.getInstance("0123456789., \u00a0\u202f")
            // Setting a custom key listener can otherwise remove the decimal keyboard flag.
            setRawInputType(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
            imeHintLocales = LocaleList(Locale.forLanguageTag("ru-RU"))
            setText(MoneyInputText.display(initial))
        }
        field.addView(amount, LayoutParams(0, dp(64), 1f))
        field.addView(TextView(context).apply {
            text = "₽"; textSize = 23f; setTextColor(Color.rgb(111, 121, 144))
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        })
        addView(field, LayoutParams(-1, -2))
        addView(help.apply {
            text = "Например, 1 250,45 ₽"; textSize = 12f; setTextColor(Color.rgb(111, 121, 144))
            setPadding(dp(2), dp(6), 0, 0)
            visibility = if (showHint) VISIBLE else GONE
        })
        layoutParams = LayoutParams(-1, -2).apply { bottomMargin = dp(16) }
        amount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(value: Editable?) {
                if (changing || value == null) return
                error = null; onValueChanged?.invoke()
                if (!value.contains('.')) return
                val cursor = amount.selectionStart.coerceAtLeast(0)
                changing = true
                amount.setText(MoneyInputText.display(value.toString()))
                amount.setSelection(cursor.coerceAtMost(amount.length()))
                changing = false
            }
        })
    }

    fun decimalValue(): String = MoneyInputText.decimalValue(amount.text.toString())
    fun snapshot(): Bundle = Bundle().apply { putString("text", amount.text.toString()) }
    fun restore(state: Bundle?) { state?.getString("text")?.let { amount.setText(MoneyInputText.display(it)) } }
    private fun dp(value: Int) = (value * resources.displayMetrics.density + .5f).toInt()
}
