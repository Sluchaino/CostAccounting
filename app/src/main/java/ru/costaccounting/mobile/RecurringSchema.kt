package ru.costaccounting.mobile

object RecurringSchema {
    const val ADD_ORIGIN = "ALTER TABLE entries ADD COLUMN recurring_id TEXT"
    const val CREATE_PAYMENTS = """CREATE TABLE recurring_payments (
        id TEXT PRIMARY KEY, name TEXT NOT NULL, amount INTEGER NOT NULL CHECK(amount > 0),
        category_id TEXT NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
        start_date TEXT NOT NULL, frequency TEXT NOT NULL CHECK(frequency IN ('Monthly','Yearly')),
        next_date TEXT NOT NULL, enabled INTEGER NOT NULL CHECK(enabled IN (0,1)))"""
}
