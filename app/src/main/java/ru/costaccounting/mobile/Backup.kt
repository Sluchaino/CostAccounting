package ru.costaccounting.mobile

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID
import java.time.LocalDate

object Backup {
    fun encode(ledger: Ledger): String = JSONObject().apply {
        put("version", 3)
        put("categories", JSONArray().apply { ledger.categories.forEach { c ->
            put(JSONObject().put("id", c.id).put("name", c.name).put("type", c.type.name))
        } })
        put("entries", JSONArray().apply { ledger.entries.forEach { e ->
            put(JSONObject().put("id", e.id).put("date", e.date).put("amount", e.amount)
                .put("categoryId", e.categoryId).put("comment", e.comment).put("createdAt", e.createdAt)
                .put("recurringId", e.recurringId ?: JSONObject.NULL).put("title", e.title)
                .put("allocations", JSONArray().apply { e.allocations.forEach { part ->
                    put(JSONObject().put("categoryId", part.categoryId).put("amount", part.amount ?: JSONObject.NULL))
                } }))
        } })
        put("recurring", JSONArray().apply { ledger.recurring.forEach { p ->
            put(JSONObject().put("id", p.id).put("name", p.name).put("amount", p.amount).put("categoryId", p.categoryId)
                .put("startDate", p.startDate.toString()).put("frequency", p.frequency.name)
                .put("nextDate", p.nextDate.toString()).put("enabled", p.enabled))
        } })
    }.toString(2)

    fun decode(text: String): Ledger {
        val root = JSONObject(text)
        val version = root.getInt("version")
        require(version in 1..3) { "Неизвестная версия резервной копии" }
        val categoryArray = root.getJSONArray("categories")
        val entryArray = root.getJSONArray("entries")
        require(categoryArray.length() <= 5000 && entryArray.length() <= 50000) { "Слишком большой файл" }
        val categories = (0 until categoryArray.length()).map { index ->
            val c = categoryArray.getJSONObject(index)
            val name = c.getString("name").trim()
            require(name.isNotEmpty() && name.length <= 100) { "Некорректное название категории" }
            Category(uuid(c.getString("id")), name, CategoryType.valueOf(c.getString("type")))
        }
        require(categories.distinctBy { it.id }.size == categories.size) { "Повторяются идентификаторы категорий" }
        require(categories.distinctBy { it.name.lowercase(Locale.ROOT) }.size == categories.size) { "Повторяются названия категорий" }
        val byId = categories.associateBy { it.id }
        val entries = (0 until entryArray.length()).map { index ->
            val e = entryArray.getJSONObject(index)
            val category = requireNotNull(byId[e.getString("categoryId")]) { "В операции указана неизвестная категория" }
            val amount = requireNotNull(e.get("amount").toString().toLongOrNull()) { "Некорректная сумма" }
            require(amount in 1..Money.MAX) { "Некорректная сумма" }
            val comment = e.getString("comment")
            require(comment.length <= 255) { "Слишком длинный комментарий" }
            val title = if (version >= 3) e.getString("title").trim() else ""
            require(title.length <= 100) { "Слишком длинное название покупки" }
            val partsArray = if (version >= 3) e.getJSONArray("allocations") else JSONArray()
            require(partsArray.length() <= 50) { "Слишком много категорий покупки" }
            val parts = (0 until partsArray.length()).map { partIndex ->
                val part = partsArray.getJSONObject(partIndex)
                val partCategory = requireNotNull(byId[part.getString("categoryId")]) { "Неизвестная категория покупки" }
                val value = if (part.isNull("amount")) null else requireNotNull(part.get("amount").toString().toLongOrNull()) { "Некорректная сумма категории" }
                EntryAllocation(partCategory.id, partCategory.name, partCategory.type, value)
            }
            validateAllocations(amount, parts)
            if (parts.isNotEmpty()) require(category.type == CategoryType.Expense && category.id == parts.first().categoryId) { "Некорректная основная категория покупки" }
            Entry(uuid(e.getString("id")), e.getLong("date"), amount, category.id, category.name, category.type,
                comment, e.getLong("createdAt"), if (version >= 2 && !e.isNull("recurringId")) uuid(e.getString("recurringId")) else null, title, parts)
        }
        require(entries.distinctBy { it.id }.size == entries.size) { "Повторяются идентификаторы операций" }
        val recurringArray = if (version >= 2) root.getJSONArray("recurring") else JSONArray()
        require(recurringArray.length() <= 1000) { "Слишком много регулярных платежей" }
        val recurring = (0 until recurringArray.length()).map { index ->
            val p = recurringArray.getJSONObject(index)
            val name = p.getString("name").trim()
            require(name.isNotEmpty() && name.length <= 100) { "Некорректное название платежа" }
            val amount = requireNotNull(p.get("amount").toString().toLongOrNull()) { "Некорректная сумма платежа" }
            require(amount in 1..Money.MAX) { "Некорректная сумма платежа" }
            val category = requireNotNull(byId[p.getString("categoryId")]) { "Неизвестная категория платежа" }
            val start = LocalDate.parse(p.getString("startDate"))
            val next = LocalDate.parse(p.getString("nextDate"))
            val frequency = Frequency.valueOf(p.getString("frequency"))
            require(start.year in 1900..9999 && next.year in 1900..9999 && next >= start && nextOccurrence(start, frequency, next) == next) { "Некорректные даты регулярного платежа" }
            RecurringPayment(uuid(p.getString("id")), name, amount, category.id, category.name, category.type,
                start, frequency, next, p.getBoolean("enabled"))
        }
        require(recurring.distinctBy { it.id }.size == recurring.size) { "Повторяются идентификаторы платежей" }
        return Ledger(categories, entries, recurring)
    }

    private fun uuid(value: String): String = UUID.fromString(value).toString()
}
