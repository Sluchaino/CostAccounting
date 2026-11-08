package ru.costaccounting.mobile

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class MainActivity : Activity() {
    private val purple = Color.rgb(91, 76, 205)
    private val ink = Color.rgb(28, 35, 58)
    private val muted = Color.rgb(111, 121, 144)
    private val green = Color.rgb(9, 133, 104)
    private val red = Color.rgb(204, 71, 91)
    private val surface = Color.rgb(243, 245, 250)
    private val zone get() = ZoneId.systemDefault()
    private val dateFormat = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", java.util.Locale.forLanguageTag("ru"))
    private lateinit var database: LedgerDatabase
    private val worker = Executors.newSingleThreadExecutor()
    private lateinit var root: LinearLayout
    private lateinit var bannerSlot: BannerSlot
    private var ledger = Ledger()
    private var loaded = false
    private var busy = false
    private var selectedTab = 0
    private var from: LocalDate? = null
    private var to: LocalDate? = null
    private var filterType: CategoryType? = null
    private var filterCategory: String? = null
    private var reportType: CategoryType? = null
    private var overviewChartType: CategoryType? = null
    private var forecastMonth = YearMonth.now()
    private var draft: (() -> Bundle)? = null
    private var editorDialog: AlertDialog? = null
    private var restoredDraft: Bundle? = null
    private var pendingBackup: String? = null
    private var pageScroll: ScrollView? = null
    private var renderedTab = -1
    private var pageHadData = false
    private val scrollOffsets = IntArray(5)
    private var showForecastAfterRender = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = LedgerDatabase(applicationContext)
        selectedTab = savedInstanceState?.getInt("tab") ?: 0
        savedInstanceState?.getIntArray("scrollOffsets")?.let { saved ->
            saved.copyInto(scrollOffsets, endIndex = minOf(saved.size, scrollOffsets.size))
        }
        from = savedInstanceState?.getString("from")?.let(LocalDate::parse)
        to = savedInstanceState?.getString("to")?.let(LocalDate::parse)
        filterType = savedInstanceState?.getString("type")?.let(CategoryType::valueOf)
        filterCategory = savedInstanceState?.getString("category")
        reportType = savedInstanceState?.getString("reportType")?.let(CategoryType::valueOf)
        overviewChartType = savedInstanceState?.getString("overviewChartType")?.let(CategoryType::valueOf)
        forecastMonth = savedInstanceState?.getString("forecastMonth")?.let(YearMonth::parse) ?: YearMonth.now()
        restoredDraft = savedInstanceState?.getBundle("draft")
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(surface) }
        bannerSlot = BannerSlot(this)
        root.setOnApplyWindowInsetsListener { view, insets ->
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.ime())
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            } else {
                @Suppress("DEPRECATION")
                view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            }
            insets
        }
        setContentView(root)
        root.requestApplyInsets()
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            val appearance = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            window.insetsController?.setSystemBarsAppearance(appearance, appearance)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                (if (android.os.Build.VERSION.SDK_INT >= 27) View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0)
        }
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT) {
                if (selectedTab == 0) finish() else { selectedTab = 0; render() }
            }
        }
        render()
        execute({}) {
            restoredDraft?.let { state ->
                restoredDraft = null
                when (state.getString("editor")) {
                    "category" -> categoryEditor(ledger.categories.find { it.id == state.getString("id") }, state)
                    "recurring" -> recurringEditor(ledger.recurring.find { it.id == state.getString("id") }, state)
                    else -> entryEditor(ledger.entries.find { it.id == state.getString("id") }, state)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        rememberPageScroll()
        outState.putIntArray("scrollOffsets", scrollOffsets.copyOf())
        outState.putInt("tab", selectedTab)
        outState.putString("from", from?.toString()); outState.putString("to", to?.toString())
        outState.putString("type", filterType?.name); outState.putString("category", filterCategory)
        outState.putString("reportType", reportType?.name); outState.putBundle("draft", draft?.invoke())
        outState.putString("forecastMonth", forecastMonth.toString())
        outState.putString("overviewChartType", overviewChartType?.name)
        super.onSaveInstanceState(outState)
    }

    @Deprecated("For Android 8–12")
    @android.annotation.SuppressLint("GestureBackNavigation") // API 33+ uses OnBackInvokedDispatcher above.
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (selectedTab == 0) super.onBackPressed() else { selectedTab = 0; render() }
    }

    override fun onDestroy() {
        editorDialog?.dismiss()
        worker.execute { database.close() }
        worker.shutdown()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        if (loaded && !busy) execute({})
    }

    private fun execute(action: () -> Unit, onFailure: (String) -> Unit = { toast(it) }, success: () -> Unit = {}) {
        if (busy) return
        busy = true
        worker.execute {
            try {
                action()
                val updated = database.load()
                runOnUiThread {
                    if (isDestroyed || isFinishing) return@runOnUiThread
                    busy = false; loaded = true; ledger = updated; render(); success()
                }
            } catch (error: Exception) {
                runOnUiThread {
                    if (!isDestroyed && !isFinishing) {
                        busy = false
                        onFailure(error.message ?: "Не удалось сохранить данные")
                    }
                }
            }
        }
    }

    private fun render() {
        rememberPageScroll()
        root.removeAllViews()
        val header = column().apply { setPadding(dp(20), dp(12), dp(20), dp(8)) }
        val brand = row()
        brand.addView(ImageView(this).apply { setImageResource(R.drawable.ic_wallet); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO },
            LinearLayout.LayoutParams(dp(32), dp(32)).apply { marginEnd = dp(10) })
        brand.addView(text("CashFlow", 23, ink, true), LinearLayout.LayoutParams(0, -2, 1f))
        brand.addView(text("● Офлайн", 11, green, true).apply {
            setPadding(dp(10), dp(6), dp(10), dp(6)); background = shape(Color.rgb(228, 245, 239), 20)
        })
        header.addView(brand)
        header.addView(text("Ваш бюджет под контролем", 12, muted).apply { setPadding(0, dp(7), 0, 0) })
        root.addView(header)
        val scroll = ScrollView(this).apply { isFillViewport = true }
        pageScroll = scroll; renderedTab = selectedTab; pageHadData = loaded
        val page = column().apply { setPadding(dp(20), dp(14), dp(20), dp(24)) }
        scroll.addView(page)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        if (!loaded) {
            page.addView(ProgressBar(this)); page.addView(text("Открываем локальную базу…", 15, muted))
        } else when (selectedTab) {
            0 -> overview(page)
            1 -> operations(page)
            2 -> categories(page)
            3 -> reports(page)
            4 -> recurringPayments(page)
        }
        // A separate fixed-height sibling reduces the scroll viewport instead of covering it.
        // Keep the same container across renders so a future SDK view is not recreated on tab changes.
        root.addView(bannerSlot, LinearLayout.LayoutParams(-1, dp(BannerSlot.HEIGHT_DP)).apply { bottomMargin = dp(6) })
        val nav = row().apply { setPadding(dp(8), dp(7), dp(8), dp(7)); setBackgroundColor(Color.WHITE); elevation = dp(3).toFloat() }
        listOf("Обзор", "Операции", "Категории", "Отчёты", "План").forEachIndexed { index, label ->
            val item = column().apply {
                gravity = Gravity.CENTER; setPadding(dp(2), dp(7), dp(2), dp(7))
                background = shape(if (index == selectedTab) Color.rgb(239, 236, 253) else Color.WHITE, 18)
                contentDescription = label; isClickable = true; isFocusable = true
                setOnClickListener { selectedTab = index; render() }
            }
            item.addView(AppIconView(this, index, if (index == selectedTab) purple else muted),
                LinearLayout.LayoutParams(dp(23), dp(23)).apply { bottomMargin = dp(5) })
            item.addView(text(label, 10, if (index == selectedTab) purple else muted, index == selectedTab).apply {
                gravity = Gravity.CENTER; maxLines = 1; ellipsize = TextUtils.TruncateAt.END
            })
            nav.addView(item, LinearLayout.LayoutParams(0, dp(60), 1f).apply { marginStart = dp(2); marginEnd = dp(2) })
        }
        root.addView(nav)
        if (loaded) {
            val tab = selectedTab
            val jumpToForecast = showForecastAfterRender && tab == 3
            showForecastAfterRender = false
            scroll.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                override fun onLayoutChange(view: View, left: Int, top: Int, right: Int, bottom: Int,
                    oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
                    scroll.removeOnLayoutChangeListener(this)
                    if (pageScroll === scroll) {
                        val forecast = (0 until page.childCount).map { page.getChildAt(it) }.filterIsInstance<ForecastPanel>().firstOrNull()
                        scroll.scrollTo(0, if (jumpToForecast && forecast != null) forecast.top else scrollOffsets[tab])
                    }
                }
            })
        }
    }

    private fun rememberPageScroll() {
        if (pageHadData && renderedTab in scrollOffsets.indices) scrollOffsets[renderedTab] = pageScroll?.scrollY ?: 0
    }

    private fun overview(page: LinearLayout) {
        title(page, "Обзор финансов", "Доходы, расходы и ваш текущий баланс")
        val total = totals(ledger.entries)
        val balance = card().apply {
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.rgb(28, 39, 66), Color.rgb(41, 74, 87))).apply { cornerRadius = dp(24).toFloat() }
            setPadding(dp(22), dp(22), dp(22), dp(22))
        }
        balance.addView(text("ОБЩИЙ БАЛАНС", 11, Color.rgb(186, 211, 220), true).apply { letterSpacing = .10f })
        balance.addView(moneyText(total.balance, 34, Color.WHITE, 18).apply { setPadding(0, dp(14), 0, dp(14)) }, LinearLayout.LayoutParams(-1, dp(72)))
        balance.addView(text("За всё время  ·  ${ledger.entries.size} операций", 12, Color.rgb(203, 221, 228)))
        page.addView(balance, spaced())
        summary(page, total, showBalance = false)
        val forecast = expenseForecast(ledger, YearMonth.now())
        val income = incomeForecast(ledger, YearMonth.now())
        val forecastBox = card()
        forecastBox.background = shape(Color.rgb(237, 240, 254), 20)
        forecastBox.addView(text("Расходы к концу месяца", 14, muted))
        forecastBox.addView(moneyText(forecast.total, 25, purple), LinearLayout.LayoutParams(-1, dp(44)))
        forecastBox.addView(text("Доходы к концу месяца", 14, muted))
        forecastBox.addView(moneyText(income.total, 25, green), LinearLayout.LayoutParams(-1, dp(44)))
        forecastBox.addView(text(if (forecast.hasHistory) "С учётом истории и запланированных платежей" else "Пока только фактические и запланированные расходы", 12, muted), spaced(8))
        forecastBox.addView(button("Посмотреть прогноз", false) {
            forecastMonth = YearMonth.now(); selectedTab = 3; showForecastAfterRender = true; render()
        })
        page.addView(forecastBox, spaced())
        page.addView(button("+ Добавить операцию") { entryEditor() }, spaced())
        section(page, "Последние операции")
        if (ledger.entries.isEmpty()) empty(page, "Здесь начнётся ваша история", "Добавьте первую покупку или доход — баланс и отчёты рассчитаются автоматически.")
        else ledger.entries.take(5).forEach { entryCard(page, it) }
        section(page, "Структура финансов")
        page.addView(CategoryChartPanel(this, ledger.entries, overviewChartType) { overviewChartType = it }, spaced())
        section(page, "Резервная копия")
        page.addView(text("Данные хранятся только на телефоне. Сохраните копию перед переустановкой приложения.", 13, muted), spaced())
        page.addView(button("Сохранить копию в файл", false) { exportBackup() }, spaced())
        page.addView(button("Восстановить из файла", false) {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "*/*"; addCategory(Intent.CATEGORY_OPENABLE) }, 102)
        }, spaced())
    }

    private fun operations(page: LinearLayout) {
        title(page, "Операции", "История доходов и расходов")
        page.addView(button("+ Новая операция") { entryEditor() }, spaced())
        periodFilter(page)
        val filters = row()
        filters.addView(button(filterType?.title ?: "Все типы", false) {
            AppDialogs.choose(this, "Тип операций", listOf("Все типы", "Расходы", "Доходы"),
                when (filterType) { CategoryType.Expense -> 1; CategoryType.Income -> 2; else -> 0 }) { which ->
                filterType = when (which) { 1 -> CategoryType.Expense; 2 -> CategoryType.Income; else -> null }; render()
            }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        filters.addView(button(if (filterCategory == UNALLOCATED_CATEGORY) "Не распределено" else ledger.categories.find { it.id == filterCategory }?.name ?: "Все категории", false) {
            val choices = listOf(null to "Все категории") + ledger.categories.map { it.id to "${it.name} · ${it.type.title}" } +
                if (ledger.entries.any { it.unallocatedAmount() > 0 }) listOf(UNALLOCATED_CATEGORY to "Не распределено") else emptyList()
            AppDialogs.choose(this, "Категория", choices.map { it.second }, choices.indexOfFirst { it.first == filterCategory }) { which ->
                filterCategory = choices[which].first; render()
            }
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(8) })
        page.addView(filters, spaced())
        val entries = filtered(ledger.entries, from, to, filterType, filterCategory)
        summary(page, totals(entries, filterCategory))
        if (filterCategory != null) page.addView(text("Итоги — по выбранной категории. В карточках показана полная стоимость покупки.", 12, muted), spaced())
        section(page, "Найдено: ${entries.size}")
        if (entries.isEmpty()) empty(page, "Операций пока нет", "Добавьте операцию или измените фильтры.")
        entries.forEach { entryCard(page, it) }
    }

    private fun categories(page: LinearLayout) {
        title(page, "Категории", "Организуйте свой бюджет")
        page.addView(button("+ Новая категория") { categoryEditor() }, spaced())
        CategoryType.entries.forEach { type ->
            section(page, if (type == CategoryType.Expense) "Расходы" else "Доходы")
            val categories = ledger.categories.filter { it.type == type }
            if (categories.isEmpty()) page.addView(text("Нет категорий. Добавьте первую.", 14, muted), spaced())
            categories.forEach { c ->
                val box = card()
                val line = row()
                line.addView(text(c.name, 17, ink, true), LinearLayout.LayoutParams(0, -2, 1f))
                line.addView(text("Изменить ›", 12, purple, true))
                box.addView(line)
                val count = ledger.entries.count { it.hasCategory(c.id) }
                val plans = ledger.recurring.count { it.categoryId == c.id }
                box.addView(text("${type.title} · операций: $count · регулярных: $plans", 12, muted).apply { setPadding(0, dp(5), 0, 0) })
                box.contentDescription = "Категория ${c.name}, изменить"
                box.setOnClickListener { categoryEditor(c) }
                page.addView(box, spaced())
            }
        }
    }

    private fun reports(page: LinearLayout) {
        title(page, "Отчёты", "Понимайте, куда уходят деньги")
        periodFilter(page)
        val entries = filtered(ledger.entries, from, to)
        summary(page, totals(entries))
        section(page, "По категориям")
        page.addView(CategoryChartPanel(this, entries, reportType) { reportType = it }, spaced())
        forecastSection(page)
    }

    private fun forecastSection(page: LinearLayout) {
        page.addView(ForecastPanel(this, ledger, forecastMonth) { forecastMonth = it }, spaced())
    }

    private fun recurringPayments(page: LinearLayout) {
        title(page, "Регулярные платежи", "Доходы и расходы каждый месяц или каждый год")
        page.addView(text("В дату платежа приложение добавит операцию в историю при открытии или возвращении в приложение. Пропущенные даты будут учтены без повторов.", 13, muted), spaced())
        page.addView(button("+ Регулярный платёж") { recurringEditor() }, spaced())
        val upcoming = upcomingPayments(ledger, LocalDate.now(), LocalDate.now().plusYears(1))
        section(page, "Ближайшие платежи")
        if (upcoming.isEmpty()) empty(page, "Пока ничего не запланировано", "Добавьте, например, ежемесячную зарплату, аренду или ежегодную страховку.")
        else {
            val box = card()
            upcoming.take(8).forEach { occurrence ->
                val p = occurrence.payment
                box.addView(text("${occurrence.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))} · ${p.name}", 14, ink, true))
                box.addView(text("${if (p.type == CategoryType.Income) "+" else "−"} ${Money.format(p.amount)} · ${p.categoryName}", 13,
                    if (p.type == CategoryType.Income) green else red), spaced(10))
            }
            page.addView(box, spaced())
        }
        section(page, "Мои регулярные платежи")
        ledger.recurring.forEach { payment ->
            val box = card()
            box.addView(text(payment.name, 18, ink, true))
            box.addView(text("${payment.type.title} · ${Money.format(payment.amount)}", 17,
                if (payment.type == CategoryType.Income) green else red, true))
            box.addView(text("${payment.frequency.title} · ${payment.categoryName}", 13, muted))
            box.addView(text(if (payment.enabled) "Следующий: ${payment.nextDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}" else "Приостановлен", 13, muted), spaced(8))
            box.addView(button("Изменить", false) { recurringEditor(payment) }, spaced(6))
            box.addView(button(if (payment.enabled) "Приостановить" else "Возобновить", false) {
                execute({ database.saveRecurring(payment.id, payment.name, payment.amount, payment.categoryId,
                    payment.startDate, payment.frequency, !payment.enabled) })
            })
            page.addView(box, spaced())
        }
    }

    private fun recurringEditor(existing: RecurringPayment? = null, saved: Bundle? = null) {
        if (!loaded || busy) return
        if (ledger.categories.isEmpty()) { toast("Сначала создайте категорию"); selectedTab = 2; render(); return }
        val form = form()
        val name = input("Например, аренда или зарплата", saved?.getString("name") ?: existing?.name ?: "")
            .apply { filters = arrayOf(InputFilter.LengthFilter(100)) }
        val amount = MoneyInputView(this, saved?.getString("amount") ?: existing?.let { Money.input(it.amount) } ?: "", "Сумма регулярного платежа")
            .apply { restore(saved?.getBundle("amountInput")) }
        val choices = ledger.categories
        val category = FormStyle.spinner(this, choices.map { "${it.name} · ${it.type.title}" },
            choices.indexOfFirst { it.id == (saved?.getString("categoryId") ?: existing?.categoryId) }, "Категория регулярного платежа")
        val frequency = FormStyle.spinner(this, Frequency.entries.map { it.title },
            saved?.getInt("frequency") ?: existing?.frequency?.ordinal ?: 0, "Периодичность")
        var start = saved?.getString("start")?.let(LocalDate::parse) ?: existing?.startDate ?: LocalDate.now()
        lateinit var dateButton: Button
        dateButton = dateInput(start.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))) {
            pickDate(start) { value -> start = value; dateButton.text = start.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) }
        }
        val enabled = Switch(this).apply {
            text = "Добавлять операции"; textSize = 14f; setTextColor(ink)
            setPadding(dp(14), dp(8), dp(14), dp(8)); minimumHeight = dp(56); background = FormStyle.background(this@MainActivity)
            isChecked = saved?.getBoolean("enabled") ?: existing?.enabled ?: true
        }
        form.section("Основное")
        form.field("Название", name, limit = 100)
        form.field("Сумма", amount)
        form.field("Категория и тип", category)
        form.section("Расписание")
        form.field("Повторение", frequency)
        form.field("Первая дата", dateButton, "Нажмите, чтобы выбрать дату начала")
        form.field("Статус", enabled)
        form.note(if (existing == null) "Прошедшие платежи добавятся сразу. Для 29–31 числа используется последний день короткого месяца."
            else "Изменения касаются будущих операций. После смены расписания или возобновления расчёт идёт с сегодняшнего дня; пропуски во время паузы не начисляются.")
        val dialog = AlertDialog.Builder(this).setCustomTitle(formHeader(if (existing == null) "Регулярный платёж" else "Изменить платёж", "Доход или расход по вашему расписанию"))
            .setView(scrollForm(form)).setPositiveButton("Сохранить", null).setNegativeButton("Отмена", null)
            .apply { if (existing != null) setNeutralButton("Удалить", null) }.create()
        draft = { Bundle().apply {
            putString("editor", "recurring"); putString("id", existing?.id); putString("name", name.text.toString())
            putBundle("amountInput", amount.snapshot()); putString("categoryId", choices[category.selectedItemPosition].id)
            putString("start", start.toString()); putInt("frequency", frequency.selectedItemPosition); putBoolean("enabled", enabled.isChecked)
        } }
        editorDialog = dialog
        dialog.setOnDismissListener { if (editorDialog === dialog) { draft = null; editorDialog = null } }
        dialog.setOnShowListener {
            styleFormDialog(dialog)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                form.clearError()
                if (name.text.isBlank()) { form.showError("Введите название платежа"); name.requestFocus(); return@setOnClickListener }
                val minor = try { Money.parse(amount.decimalValue()) }
                    catch (error: IllegalArgumentException) { amount.error = error.message; return@setOnClickListener }
                saveForm(dialog, form) { database.saveRecurring(existing?.id, name.text.toString(), minor, choices[category.selectedItemPosition].id,
                    start, Frequency.entries[frequency.selectedItemPosition], enabled.isChecked) }
            }
            if (existing != null) dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                confirm("Удалить регулярный платёж «${existing.name}»? Уже созданные операции останутся.") {
                    saveForm(dialog, form) { database.deleteRecurring(existing.id) }
                }
            }
        }
        dialog.show()
    }

    private fun periodFilter(page: LinearLayout) {
        val box = card()
        box.addView(text("Период", 13, muted, true))
        val dates = row()
        dates.addView(button(from?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) ?: "С любой даты", false) {
            pickDate(from ?: LocalDate.now()) { date ->
                if (to != null && date.isAfter(to)) toast("Начало периода позже окончания")
                else { from = date; render() }
            }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        dates.addView(button(to?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) ?: "По сегодня и далее", false) {
            pickDate(to ?: LocalDate.now()) { date ->
                if (from != null && date.isBefore(from)) toast("Окончание периода раньше начала")
                else { to = date; render() }
            }
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(8) })
        box.addView(dates)
        if (from != null || to != null || filterCategory != null || filterType != null) {
            box.addView(button("Сбросить фильтры", false) { from = null; to = null; filterCategory = null; filterType = null; render() })
        }
        page.addView(box, spaced())
    }

    private fun summary(page: LinearLayout, total: Totals, showBalance: Boolean = true) {
        val stats = row()
        listOf("Доходы" to total.income, "Расходы" to total.expense).forEachIndexed { index, (label, value) ->
            val color = if (index == 0) green else red
            val box = card().apply { setPadding(dp(14), dp(16), dp(14), dp(16)) }
            box.addView(text((if (index == 0) "↗  " else "↙  ") + label, 12, color, true))
            box.addView(moneyText(value, 19, ink, 10), LinearLayout.LayoutParams(-1, dp(36)).apply { topMargin = dp(5) })
            stats.addView(box, LinearLayout.LayoutParams(0, -2, 1f).apply { if (index == 1) marginStart = dp(10) })
        }
        page.addView(stats, spaced())
        if (showBalance) {
            val line = row().apply { setPadding(dp(16), dp(12), dp(16), dp(12)); background = shape(Color.rgb(234, 237, 248), 14) }
            line.addView(text("Баланс", 14, ink, true), LinearLayout.LayoutParams(0, -2, 1f))
            line.addView(moneyText(total.balance, 18, purple, 11).apply { gravity = Gravity.END }, LinearLayout.LayoutParams(0, dp(30), 1.5f))
            page.addView(line, spaced())
        }
    }

    private fun entryCard(page: LinearLayout, entry: Entry) {
        val box = card()
        val line = row()
        val color = ChartColors.forId(entry.categoryId)
        val entryName = if (entry.allocations.isEmpty()) entry.categoryName else entry.title.ifBlank { "Составная покупка" }
        line.addView(text(entryName.take(1).uppercase(), 17, color, true).apply {
            gravity = Gravity.CENTER; background = shape(Color.argb(24, Color.red(color), Color.green(color), Color.blue(color)), 14)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginEnd = dp(10) })
        val name = column()
        name.addView(text(entryName, 15, ink, true).apply { maxLines = 2; ellipsize = TextUtils.TruncateAt.END })
        name.addView(text(Instant.ofEpochMilli(entry.date).atZone(zone).format(dateFormat), 11, muted))
        line.addView(name, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(8) })
        line.addView(moneyText(entry.amount, 17, if (entry.type == CategoryType.Income) green else red, 10,
            if (entry.type == CategoryType.Income) "+ " else "− ").apply { gravity = Gravity.END }, LinearLayout.LayoutParams(0, dp(40), 1.1f))
        box.addView(line)
        if (entry.comment.isNotEmpty()) box.addView(text(entry.comment, 13, muted).apply { setPadding(0, dp(9), 0, 0) })
        if (entry.recurringId != null) box.addView(text("Создано по расписанию", 11, purple))
        if (entry.allocations.isNotEmpty()) {
            box.addView(text(entry.allocations.take(3).joinToString(" · ") { "${it.categoryName}: ${it.amount?.let(Money::format) ?: "позже"}" } +
                if (entry.allocations.size > 3) " · ещё ${entry.allocations.size - 3}" else "", 12, muted).apply { setPadding(0, dp(8), 0, 0) })
            if (entry.unallocatedAmount() > 0) box.addView(text("Не распределено: ${Money.format(entry.unallocatedAmount())}", 12, purple))
        }
        box.setOnClickListener { entryEditor(entry) }
        page.addView(box, spaced())
    }

    private fun categoryEditor(existing: Category? = null, saved: Bundle? = null) {
        if (!loaded || busy) return
        val form = form()
        val name = input("Название категории", saved?.getString("name") ?: existing?.name ?: "")
        name.filters = arrayOf(InputFilter.LengthFilter(100))
        val type = FormStyle.spinner(this, listOf("Расход", "Доход"), saved?.getInt("type") ?: existing?.type?.ordinal ?: 0, "Тип категории")
        form.field("Название", name, limit = 100)
        form.field("Тип категории", type)
        if (existing != null) form.note("Изменение типа пересчитает связанные операции и регулярные платежи.")
        val dialog = AlertDialog.Builder(this).setCustomTitle(formHeader(if (existing == null) "Новая категория" else "Изменить категорию", "Порядок в доходах и расходах"))
            .setView(scrollForm(form)).setPositiveButton("Сохранить", null).setNegativeButton("Отмена", null)
            .apply { if (existing != null) setNeutralButton("Удалить", null) }.create()
        draft = { Bundle().apply { putString("editor", "category"); putString("id", existing?.id); putString("name", name.text.toString()); putInt("type", type.selectedItemPosition) } }
        editorDialog = dialog
        dialog.setOnDismissListener { if (editorDialog === dialog) { draft = null; editorDialog = null } }
        dialog.setOnShowListener {
            styleFormDialog(dialog)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                form.clearError()
                if (name.text.isBlank()) { form.showError("Введите название категории"); name.requestFocus(); return@setOnClickListener }
                saveForm(dialog, form) { database.saveCategory(existing?.id, name.text.toString(), CategoryType.entries[type.selectedItemPosition]) }
            }
            if (existing != null) dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                confirm("Удалить категорию «${existing.name}»?") { saveForm(dialog, form) { database.deleteCategory(existing.id) } }
            }
        }
        dialog.show()
    }

    private fun entryEditor(existing: Entry? = null, saved: Bundle? = null) {
        if (!loaded || busy) return
        if (ledger.categories.isEmpty()) { toast("Сначала создайте категорию"); selectedTab = 2; render(); return }
        val form = form()
        var date = saved?.getLong("date") ?: existing?.date ?: System.currentTimeMillis()
        val amount = MoneyInputView(this, saved?.getString("amount") ?: existing?.let { Money.input(it.amount) } ?: "", "Сумма в рублях")
            .apply { restore(saved?.getBundle("amountInput")) }
        val choices = ledger.categories
        val category = FormStyle.spinner(this, choices.map { "${it.name} · ${it.type.title}" },
            choices.indexOfFirst { it.id == (saved?.getString("categoryId") ?: existing?.categoryId) }, "Категория операции")
        val split = Switch(this).apply {
            text = "Составная покупка"; textSize = 15f; setTextColor(ink); minimumHeight = dp(56)
            setPadding(dp(14), dp(8), dp(14), dp(8)); background = FormStyle.background(this@MainActivity)
            isChecked = saved?.getBoolean("split") ?: existing?.allocations?.isNotEmpty() ?: false
            contentDescription = "Разделить покупку по категориям"
        }
        val purchaseTitle = input("Например, покупка в супермаркете", saved?.getString("purchaseTitle") ?: existing?.title ?: "")
            .apply { filters = arrayOf(InputFilter.LengthFilter(100)) }
        val distribution = SplitAllocationEditor(this, choices, existing?.allocations.orEmpty(), {
            try { Money.parse(amount.decimalValue()) } catch (_: IllegalArgumentException) { null }
        }, saved?.getBundle("distribution"))
        distribution.onChanged = { form.clearError() }
        amount.onValueChanged = { distribution.refreshTotal() }
        val ordinaryFields = FormPanel(this).apply {
            setPadding(0, 0, 0, 0); isFocusableInTouchMode = false; field("Категория", category)
        }
        val splitFields = FormPanel(this).apply {
            setPadding(0, 0, 0, 0); isFocusableInTouchMode = false
            field("Название покупки · необязательно", purchaseTitle, limit = 100)
            field("Категории и суммы", distribution)
            note("Можно сохранить выбранные категории без сумм и распределить стоимость позже. Остаток попадёт в «Не распределено». Общая сумма покупки учитывается один раз.")
        }
        ordinaryFields.visibility = if (split.isChecked) View.GONE else View.VISIBLE
        splitFields.visibility = if (split.isChecked) View.VISIBLE else View.GONE
        split.setOnCheckedChangeListener { _, yes ->
            ordinaryFields.visibility = if (yes) View.GONE else View.VISIBLE
            splitFields.visibility = if (yes) View.VISIBLE else View.GONE
            form.clearError(); distribution.refreshTotal()
        }
        val comment = input("На что потратили или откуда поступили деньги", saved?.getString("comment") ?: existing?.comment ?: "").apply {
            filters = arrayOf(InputFilter.LengthFilter(255)); contentDescription = "Комментарий"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setSingleLine(false); gravity = Gravity.TOP or Gravity.START; minLines = 3
            layoutParams = LinearLayout.LayoutParams(-1, dp(104))
        }
        lateinit var dateButton: Button
        dateButton = dateInput(Instant.ofEpochMilli(date).atZone(zone).format(dateFormat)) {
            val current = Instant.ofEpochMilli(date).atZone(zone)
            pickDate(current.toLocalDate()) { selected ->
                val timeDialog = TimePickerDialog(this, { _, hour, minute ->
                    date = selected.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
                    dateButton.text = Instant.ofEpochMilli(date).atZone(zone).format(dateFormat)
                }, current.hour, current.minute, true)
                timeDialog.setOnShowListener { AppDialogs.style(timeDialog) }; timeDialog.show()
            }
        }
        form.section("Основное")
        form.field("Сумма", amount)
        form.field("Тип записи", split, "Для покупки с несколькими категориями включите переключатель")
        form.addView(ordinaryFields); form.addView(splitFields)
        form.section("Детали")
        form.field("Дата и время", dateButton, "Нажмите, чтобы выбрать дату и время")
        form.field("Комментарий · необязательно", comment, limit = 255)
        val dialog = AlertDialog.Builder(this).setCustomTitle(formHeader(if (existing == null) "Новая операция" else "Изменить операцию", "Заполните сумму и выберите категорию"))
            .setView(scrollForm(form)).setPositiveButton("Сохранить", null).setNegativeButton("Отмена", null)
            .apply { if (existing != null) setNeutralButton("Удалить", null) }.create()
        draft = { Bundle().apply {
            putString("editor", "entry"); putString("id", existing?.id); putLong("date", date)
            putBundle("amountInput", amount.snapshot()); putString("comment", comment.text.toString())
            putString("categoryId", choices[category.selectedItemPosition].id)
            putBoolean("split", split.isChecked); putString("purchaseTitle", purchaseTitle.text.toString()); putBundle("distribution", distribution.snapshot())
        } }
        editorDialog = dialog
        dialog.setOnDismissListener { if (editorDialog === dialog) { draft = null; editorDialog = null } }
        dialog.setOnShowListener {
            styleFormDialog(dialog)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                form.clearError()
                val minor = try { Money.parse(amount.decimalValue()) }
                    catch (error: IllegalArgumentException) { amount.error = error.message; return@setOnClickListener }
                val parts = try {
                    if (split.isChecked) distribution.allocations().also { require(it.isNotEmpty()) { "Выберите категории покупки" }; validateAllocations(minor, it) } else emptyList()
                } catch (error: IllegalArgumentException) { form.showError(error.message ?: "Проверьте распределение"); return@setOnClickListener }
                saveForm(dialog, form) { database.saveEntry(existing?.id, date, minor, choices[category.selectedItemPosition].id, comment.text.toString(),
                    parts, if (split.isChecked) purchaseTitle.text.toString() else "") }
            }
            if (existing != null) dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                confirm("Удалить операцию на ${Money.format(existing.amount)}?") { saveForm(dialog, form) { database.deleteEntry(existing.id) } }
            }
        }
        dialog.show()
    }

    private fun exportBackup() {
        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = "application/json"; addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_TITLE, "CashFlow-${LocalDate.now()}.json")
        }, 101)
    }

    @Deprecated("Framework result API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        when (requestCode) {
            101 -> execute({
                val contents = Backup.encode(database.load())
                requireNotNull(contentResolver.openOutputStream(uri, "wt")) { "Не удалось открыть файл" }.bufferedWriter(Charsets.UTF_8).use { it.write(contents) }
            }) { toast("Резервная копия сохранена") }
            102 -> execute({
                val bytes = requireNotNull(contentResolver.openInputStream(uri)) { "Не удалось открыть файл" }.use { input ->
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        require(output.size() + count <= 10 * 1024 * 1024) { "Файл должен быть не больше 10 МБ" }
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                }
                val text = bytes.toString(Charsets.UTF_8)
                Backup.decode(text)
                pendingBackup = text
            }) { offerRestore() }
        }
    }

    private fun offerRestore() {
        val backup = pendingBackup ?: return
        val contents = Backup.decode(backup)
        val dialog = AlertDialog.Builder(this).setCustomTitle(formHeader("Восстановить данные?", "Замена текущих данных резервной копией"))
            .setMessage("Категорий: ${contents.categories.size}, операций: ${contents.entries.size}, регулярных платежей: ${contents.recurring.size}. Текущие данные будут заменены. Сначала сохраните копию, если она нужна.")
            .setNegativeButton("Отмена") { _, _ -> pendingBackup = null }
            .setPositiveButton("Заменить данные") { _, _ -> execute({ database.restore(contents) }) { pendingBackup = null; filterCategory = null; render(); toast("Данные восстановлены") } }
            .create()
        dialog.setOnShowListener { styleFormDialog(dialog) }; dialog.show()
    }

    private fun pickDate(date: LocalDate, result: (LocalDate) -> Unit) {
        val dialog = DatePickerDialog(this, { _, year, month, day -> result(LocalDate.of(year, month + 1, day)) }, date.year, date.monthValue - 1, date.dayOfMonth)
        dialog.setOnShowListener { AppDialogs.style(dialog) }; dialog.show()
    }
    private fun confirm(message: String, action: () -> Unit) {
        AppDialogs.confirm(this, message, action)
    }
    private fun title(page: LinearLayout, title: String, subtitle: String) {
        page.addView(text(title, 26, ink, true)); page.addView(text(subtitle, 13, muted).apply { setPadding(0, dp(5), 0, 0) }, spaced(18))
    }
    private fun section(page: LinearLayout, title: String) {
        page.addView(text(title, 17, ink, true).apply { setPadding(0, dp(16), 0, dp(12)) })
    }
    private fun empty(page: LinearLayout, title: String, detail: String) {
        val box = card(); box.addView(text(title, 16, ink, true)); box.addView(text(detail, 13, muted).apply { setPadding(0, dp(8), 0, 0) }); page.addView(box, spaced())
    }
    private fun card() = column().apply {
        setPadding(dp(18), dp(18), dp(18), dp(18))
        background = shape(Color.WHITE, 22).apply { setStroke(dp(1), Color.rgb(232, 236, 245)) }
        elevation = resources.displayMetrics.density * .5f
    }
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun form() = FormPanel(this)
    private fun formHeader(title: String, subtitle: String) = AppDialogs.header(this, title, subtitle)
    private fun scrollForm(form: FormPanel) = ScrollView(this).apply {
        isVerticalScrollBarEnabled = false; clipToPadding = false; addView(form); form.requestFocus()
    }
    private fun styleFormDialog(dialog: AlertDialog) {
        AppDialogs.style(dialog)
    }
    private fun saveForm(dialog: AlertDialog, form: FormPanel, action: () -> Unit) {
        if (busy) { form.showError("Подождите завершения предыдущей операции"); return }
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        execute(action, onFailure = { message ->
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true; form.showError(message)
        }) { dialog.dismiss() }
    }
    private fun dateInput(label: String, action: () -> Unit) = button(label, false, action).apply {
        textSize = 15f; gravity = Gravity.START or Gravity.CENTER_VERTICAL; minHeight = dp(56); minimumHeight = dp(56)
        setTextColor(ink); background = FormStyle.background(this@MainActivity)
    }
    private fun text(value: String, size: Int, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size.toFloat(); setTextColor(color); includeFontPadding = false
        setLineSpacing(dp(2).toFloat(), 1f)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }
    private fun input(hint: String, value: String) = EditText(this).apply {
        this.hint = hint; setText(value); textSize = 16f; setTextColor(ink); setSingleLine(); setSelectAllOnFocus(true)
        setHintTextColor(muted); setPadding(dp(14), dp(12), dp(14), dp(12)); background = FormStyle.background(this@MainActivity)
        layoutParams = LinearLayout.LayoutParams(-1, dp(56))
    }
    private fun button(label: String, primary: Boolean = true, action: () -> Unit) = Button(this).apply {
        text = label; textSize = 13f; isAllCaps = false; minHeight = dp(48); minimumHeight = dp(48)
        setTextColor(if (primary) Color.WHITE else purple)
        setTypeface(typeface, Typeface.BOLD)
        background = RippleDrawable(ColorStateList.valueOf(Color.argb(30, 100, 90, 180)),
            shape(if (primary) purple else Color.rgb(235, 234, 248), 15), null)
        stateListAnimator = null
        setPadding(dp(12), dp(10), dp(12), dp(10)); setOnClickListener { action() }
    }
    private fun moneyText(amount: Long, size: Int, color: Int, minimum: Int = 12, prefix: String = "") =
        text(prefix + Money.format(amount), size, color, true).apply {
            maxLines = 1; fontFeatureSettings = "tnum"; gravity = Gravity.CENTER_VERTICAL
            setAutoSizeTextTypeUniformWithConfiguration(minimum, size, 1, TypedValue.COMPLEX_UNIT_SP)
        }
    private fun shape(color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun spaced(bottom: Int = 12) = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4); bottomMargin = dp(bottom) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
