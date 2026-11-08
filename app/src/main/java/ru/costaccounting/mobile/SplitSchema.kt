package ru.costaccounting.mobile

object SplitSchema {
    const val ADD_TITLE = "ALTER TABLE entries ADD COLUMN title TEXT NOT NULL DEFAULT ''"
    const val CREATE_ALLOCATIONS = """CREATE TABLE entry_allocations (
        entry_id TEXT NOT NULL REFERENCES entries(id) ON DELETE CASCADE,
        category_id TEXT NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
        amount INTEGER CHECK(amount IS NULL OR amount > 0),
        PRIMARY KEY(entry_id,category_id))"""
}
