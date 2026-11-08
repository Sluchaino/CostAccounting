package ru.costaccounting.mobile

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView

object FormStyle {
    fun background(context: Context, error: Boolean = false) = StateListDrawable().apply {
        fun border(focused: Boolean) = GradientDrawable().apply {
            setColor(if (focused) Color.WHITE else Color.rgb(246, 248, 252))
            cornerRadius = 14 * context.resources.displayMetrics.density
            setStroke((context.resources.displayMetrics.density + .5f).toInt(),
                if (error) Color.rgb(204, 71, 91) else if (focused) Color.rgb(91, 76, 205) else Color.rgb(220, 226, 237))
        }
        addState(intArrayOf(android.R.attr.state_focused), border(true))
        addState(intArrayOf(), border(false))
    }

    fun spinner(context: Context, items: List<String>, selected: Int, description: String) = Spinner(context).apply {
        adapter = ArrayAdapter(context, R.layout.form_spinner_item, items).apply { setDropDownViewResource(R.layout.form_spinner_dropdown) }
        setSelection(selected.coerceAtLeast(0)); contentDescription = description
        minimumHeight = (56 * resources.displayMetrics.density).toInt()
        val chevron = requireNotNull(context.getDrawable(R.drawable.ic_form_dropdown))
        background = LayerDrawable(arrayOf(FormStyle.background(context), chevron)).apply {
            val density = context.resources.displayMetrics.density
            setLayerSize(1, (20 * density).toInt(), (20 * density).toInt())
            setLayerGravity(1, Gravity.END or Gravity.CENTER_VERTICAL)
            setLayerInset(1, 0, 0, (14 * density).toInt(), 0)
        }
        setPadding(0, 0, 0, 0)
        layoutParams = LinearLayout.LayoutParams(-1, -2)
    }
}

class FormPanel(context: Context) : LinearLayout(context) {
    private val errorBanner = TextView(context).apply {
        textSize = 13f; setTextColor(Color.rgb(177, 48, 70)); setPadding(dp(12), dp(12), dp(12), dp(12))
        background = GradientDrawable().apply { setColor(Color.rgb(255, 237, 241)); cornerRadius = dp(12).toFloat() }
        visibility = GONE; accessibilityLiveRegion = ACCESSIBILITY_LIVE_REGION_POLITE
    }
    init {
        orientation = VERTICAL; setPadding(dp(24), dp(4), dp(24), dp(12))
        isFocusableInTouchMode = true
        addView(errorBanner, LayoutParams(-1, -2).apply { bottomMargin = dp(14) })
    }

    fun section(title: String) {
        addView(TextView(context).apply {
            text = title; textSize = 12f; setTextColor(Color.rgb(91, 76, 205)); setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(6), 0, dp(12))
        })
    }

    fun field(label: String, view: View, hint: String? = null, limit: Int? = null) {
        if (view.id == NO_ID) view.id = generateViewId()
        addView(TextView(context).apply {
            text = label; textSize = 13f; setTextColor(Color.rgb(61, 71, 95)); setTypeface(typeface, Typeface.BOLD)
            labelFor = view.id; setPadding(0, 0, 0, dp(7))
        })
        val params = (view.layoutParams as? LayoutParams) ?: LayoutParams(-1, -2)
        params.width = -1; params.topMargin = 0; params.bottomMargin = 0
        addView(view, params)
        val help = if (hint != null || limit != null) TextView(context).apply {
            textSize = 12f; setTextColor(Color.rgb(111, 121, 144)); setPadding(dp(2), dp(6), dp(2), 0)
            text = hint ?: "${(view as? EditText)?.length() ?: 0} / $limit"
            if (limit != null) gravity = Gravity.END
        }.also { addView(it, LayoutParams(-1, -2)) } else null
        if (view is EditText) view.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { clearError(); if (limit != null) help?.text = "${s?.length ?: 0} / $limit" }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        if (view is MoneyInputView) {
            val existing = view.onValueChanged
            view.onValueChanged = { existing?.invoke(); clearError() }
        }
        addView(View(context), LayoutParams(-1, dp(18)))
    }

    fun note(message: String) {
        addView(TextView(context).apply {
            text = message; textSize = 12f; setTextColor(Color.rgb(111, 121, 144)); setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply { setColor(Color.rgb(241, 243, 251)); cornerRadius = dp(12).toFloat() }
        }, LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
    }

    fun showError(message: String) {
        errorBanner.text = message; errorBanner.visibility = VISIBLE
        errorBanner.post { errorBanner.requestRectangleOnScreen(Rect(0, 0, errorBanner.width, errorBanner.height), true) }
    }
    fun clearError() { errorBanner.visibility = GONE }
    private fun dp(value: Int) = (value * resources.displayMetrics.density + .5f).toInt()
}
