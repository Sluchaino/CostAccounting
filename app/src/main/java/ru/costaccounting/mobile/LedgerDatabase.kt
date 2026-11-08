package ru.costaccounting.mobile

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.Locale
import java.util.UUID
import java.time.LocalDate
import java.time.ZoneId

class LedgerDatabase(context: Context, name: String = "cashflow.db",
    private val today: () -> LocalDate = { LocalDate.now() }) : SQLiteOpenHelper(context, name, null, 3) {
    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE categories (
            id TEXT PRIMARY KEY, name TEXT NOT NULL, normalized_name TEXT NOT NULL UNIQUE,
            type TEXT NOT NULL CHECK(type IN ('Expense','Income')))""")
        db.execSQL("""CREATE TABLE entries (
            id TEXT PRIMARY KEY, date INTEGER NOT NULL, amount INTEGER NOT NULL CHECK(amount > 0),
            category_id TEXT NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
            comment TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL)""")
        db.execSQL("CREATE INDEX entries_date ON entries(date)")
        db.execSQL("CREATE INDEX entries_category ON entries(category_id)")
        migrateRecurring(db)
        migrateSplits(db)
        listOf("Еда", "Транспорт", "Подписки", "Развлечения").forEach {
            insertCategory(db, UUID.randomUUID().toString(), it, CategoryType.Expense)
        }
        insertCategory(db, UUID.randomUUID().toString(), "Зарплата", CategoryType.Income)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) migrateRecurring(db)
        if (oldVersion < 3) migrateSplits(db)
    }

    private fun migrateSplits(db: SQLiteDatabase) {
        db.execSQL(SplitSchema.ADD_TITLE); db.execSQL(SplitSchema.CREATE_ALLOCATIONS)
    }

    private fun migrateRecurring(db: SQLiteDatabase) {
        db.execSQL(RecurringSchema.ADD_ORIGIN)
        db.execSQL(RecurringSchema.CREATE_PAYMENTS)
    }

    fun load(): Ledger {
        postDuePayments()
        val db = readableDatabase
        val categories = db.rawQuery("SELECT id,name,type FROM categories ORDER BY type,name", null).use { cursor ->
            buildList { while (cursor.moveToNext()) add(Category(cursor.getString(0), cursor.getString(1), CategoryType.valueOf(cursor.getString(2)))) }
        }
        val allocations = mutableMapOf<String, MutableList<EntryAllocation>>()
        db.rawQuery("SELECT a.entry_id,a.category_id,c.name,c.type,a.amount FROM entry_allocations a JOIN categories c ON c.id=a.category_id ORDER BY a.rowid", null).use { cursor ->
            while (cursor.moveToNext()) allocations.getOrPut(cursor.getString(0)) { mutableListOf() }.add(
                EntryAllocation(cursor.getString(1), cursor.getString(2), CategoryType.valueOf(cursor.getString(3)), if (cursor.isNull(4)) null else cursor.getLong(4)))
        }
        val entries = db.rawQuery("""SELECT e.id,e.date,e.amount,e.category_id,c.name,c.type,e.comment,e.created_at,e.recurring_id,e.title
            FROM entries e JOIN categories c ON c.id=e.category_id ORDER BY e.date DESC,e.created_at DESC""", null).use { cursor ->
            buildList { while (cursor.moveToNext()) add(Entry(cursor.getString(0), cursor.getLong(1), cursor.getLong(2),
                cursor.getString(3), cursor.getString(4), CategoryType.valueOf(cursor.getString(5)), cursor.getString(6), cursor.getLong(7), cursor.getString(8),
                cursor.getString(9), allocations[cursor.getString(0)].orEmpty())) }
        }
        return Ledger(categories, entries, readRecurring(db))
    }

    fun saveCategory(id: String?, name: String, type: CategoryType) {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Введите название категории" }
        require(trimmed.length <= 100) { "Название должно быть не длиннее 100 символов" }
        if (id != null) {
            val current = readableDatabase.rawQuery("SELECT type FROM categories WHERE id=?", arrayOf(id)).use { if (it.moveToFirst()) it.getString(0) else null }
            val used = readableDatabase.rawQuery("SELECT 1 FROM entry_allocations WHERE category_id=? LIMIT 1", arrayOf(id)).use { it.moveToFirst() }
            require(!used || current == type.name) { "Категория используется в составных покупках. Сначала измените распределение этих покупок" }
        }
        try {
            if (id == null) insertCategory(writableDatabase, UUID.randomUUID().toString(), trimmed, type)
            else check(writableDatabase.update("categories", categoryValues(trimmed, type), "id=?", arrayOf(id)) == 1) {
                "Категория не найдена"
            }
        } catch (_: SQLiteConstraintException) { throw IllegalArgumentException("Категория с таким названием уже существует") }
    }

    fun deleteCategory(id: String) {
        try { writableDatabase.delete("categories", "id=?", arrayOf(id)) }
        catch (_: SQLiteConstraintException) { throw IllegalArgumentException("Категория используется в операциях или регулярных платежах. Сначала измените или удалите их") }
    }

    fun saveEntry(id: String?, date: Long, amount: Long, categoryId: String, comment: String,
        allocations: List<EntryAllocation> = emptyList(), title: String = "") {
        require(amount in 1..Money.MAX) { "Некорректная сумма" }
        require(comment.trim().length <= 255) { "Комментарий должен быть не длиннее 255 символов" }
        require(title.trim().length <= 100) { "Название покупки должно быть не длиннее 100 символов" }
        val db = writableDatabase
        val verified = allocations.map { part ->
            db.rawQuery("SELECT name,type FROM categories WHERE id=?", arrayOf(part.categoryId)).use { row ->
                require(row.moveToFirst()) { "Категория покупки не найдена" }
                part.copy(categoryName = row.getString(0), categoryType = CategoryType.valueOf(row.getString(1)))
            }
        }
        validateAllocations(amount, verified)
        val key = id ?: UUID.randomUUID().toString()
        val values = ContentValues().apply {
            put("date", date); put("amount", amount); put("category_id", verified.firstOrNull()?.categoryId ?: categoryId); put("comment", comment.trim())
            put("title", title.trim())
        }
        db.beginTransaction()
        try {
            if (id == null) {
                values.put("id", key); values.put("created_at", System.currentTimeMillis())
                db.insertOrThrow("entries", null, values)
            } else check(db.update("entries", values, "id=?", arrayOf(id)) == 1) { "Операция не найдена" }
            db.delete("entry_allocations", "entry_id=?", arrayOf(key))
            verified.forEach { insertAllocation(db, key, it) }
            db.setTransactionSuccessful()
        } catch (_: SQLiteConstraintException) { throw IllegalArgumentException("Выберите существующую категорию") }
        finally { db.endTransaction() }
    }

    fun deleteEntry(id: String) { writableDatabase.delete("entries", "id=?", arrayOf(id)) }

    private fun readRecurring(db: SQLiteDatabase): List<RecurringPayment> = db.rawQuery("""SELECT r.id,r.name,r.amount,
        r.category_id,c.name,c.type,r.start_date,r.frequency,r.next_date,r.enabled
        FROM recurring_payments r JOIN categories c ON c.id=r.category_id ORDER BY r.enabled DESC,r.next_date,r.name""", null).use { cursor ->
        buildList { while (cursor.moveToNext()) add(RecurringPayment(cursor.getString(0), cursor.getString(1), cursor.getLong(2),
            cursor.getString(3), cursor.getString(4), CategoryType.valueOf(cursor.getString(5)), LocalDate.parse(cursor.getString(6)),
            Frequency.valueOf(cursor.getString(7)), LocalDate.parse(cursor.getString(8)), cursor.getInt(9) != 0)) }
    }

    fun saveRecurring(id: String?, name: String, amount: Long, categoryId: String, startDate: LocalDate,
        frequency: Frequency, enabled: Boolean) {
        require(name.trim().isNotEmpty() && name.trim().length <= 100) { "Введите название до 100 символов" }
        require(amount in 1..Money.MAX) { "Некорректная сумма" }
        require(startDate.year in 1900..9999) { "Некорректная дата начала" }
        val db = writableDatabase
        val previous = id?.let { key -> requireNotNull(readRecurring(db).find { it.id == key }) { "Платёж не найден" } }
        val next = when {
            previous == null -> startDate
            previous.startDate != startDate || previous.frequency != frequency || (!previous.enabled && enabled) ->
                nextOccurrence(startDate, frequency, today())
            else -> previous.nextDate
        }
        val values = ContentValues().apply {
            put("name", name.trim()); put("amount", amount); put("category_id", categoryId)
            put("start_date", startDate.toString()); put("frequency", frequency.name)
            put("next_date", next.toString()); put("enabled", if (enabled) 1 else 0)
        }
        try {
            if (id == null) { values.put("id", UUID.randomUUID().toString()); db.insertOrThrow("recurring_payments", null, values) }
            else check(db.update("recurring_payments", values, "id=?", arrayOf(id)) == 1) { "Платёж не найден" }
        } catch (_: SQLiteConstraintException) { throw IllegalArgumentException("Выберите существующую категорию") }
    }

    fun deleteRecurring(id: String) {
        // Keep generated entries and their origin marker; they must not become variable spending in forecasts.
        writableDatabase.delete("recurring_payments", "id=?", arrayOf(id))
    }

    private fun postDuePayments() {
        val db = writableDatabase
        val currentDay = today()
        db.beginTransaction()
        try {
            readRecurring(db).filter { it.enabled && it.nextDate <= currentDay }.forEach { payment ->
                val due = scheduledPayments(payment, payment.nextDate, currentDay)
                due.forEach { occurrence ->
                    db.insertWithOnConflict("entries", null, ContentValues().apply {
                        put("id", occurrence.entryId); put("date", occurrence.date.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                        put("amount", payment.amount); put("category_id", payment.categoryId); put("comment", "Регулярно: ${payment.name}")
                        put("created_at", System.currentTimeMillis()); put("recurring_id", payment.id)
                    }, SQLiteDatabase.CONFLICT_IGNORE)
                }
                if (due.isNotEmpty()) db.update("recurring_payments", ContentValues().apply {
                    put("next_date", nextOccurrence(payment.startDate, payment.frequency, due.last().date.plusDays(1)).toString())
                }, "id=?", arrayOf(payment.id))
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun restore(ledger: Ledger) {
        ledger.entries.forEach { entry ->
            validateAllocations(entry.amount, entry.allocations)
            require(entry.title.length <= 100) { "Слишком длинное название покупки" }
            if (entry.allocations.isNotEmpty()) require(entry.type == CategoryType.Expense && entry.categoryId == entry.allocations.first().categoryId &&
                entry.allocations.all { part -> ledger.categories.any { it.id == part.categoryId && it.type == CategoryType.Expense } }) { "Некорректные категории составной покупки" }
        }
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("entries", null, null)
            db.delete("recurring_payments", null, null)
            db.delete("categories", null, null)
            ledger.categories.forEach { insertCategory(db, it.id, it.name, it.type) }
            ledger.recurring.forEach { p ->
                db.insertOrThrow("recurring_payments", null, ContentValues().apply {
                    put("id", p.id); put("name", p.name); put("amount", p.amount); put("category_id", p.categoryId)
                    put("start_date", p.startDate.toString()); put("frequency", p.frequency.name)
                    put("next_date", p.nextDate.toString()); put("enabled", if (p.enabled) 1 else 0)
                })
            }
            ledger.entries.forEach { e ->
                db.insertOrThrow("entries", null, ContentValues().apply {
                    put("id", e.id); put("date", e.date); put("amount", e.amount); put("category_id", e.categoryId)
                    put("comment", e.comment); put("created_at", e.createdAt)
                    put("recurring_id", e.recurringId)
                    put("title", e.title)
                })
                e.allocations.forEach { insertAllocation(db, e.id, it) }
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    private fun insertCategory(db: SQLiteDatabase, id: String, name: String, type: CategoryType) {
        db.insertOrThrow("categories", null, categoryValues(name, type).apply { put("id", id) })
    }
    private fun insertAllocation(db: SQLiteDatabase, entryId: String, allocation: EntryAllocation) {
        db.insertOrThrow("entry_allocations", null, ContentValues().apply {
            put("entry_id", entryId); put("category_id", allocation.categoryId)
            if (allocation.amount == null) putNull("amount") else put("amount", allocation.amount)
        })
    }
    private fun categoryValues(name: String, type: CategoryType) = ContentValues().apply {
        put("name", name); put("normalized_name", name.lowercase(Locale.ROOT)); put("type", type.name)
    }
}
