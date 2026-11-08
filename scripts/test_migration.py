"""Check the actual Kotlin migration SQL with host SQLite; no emulator required."""
from pathlib import Path
import re
import sqlite3
import unittest

SOURCE = (Path(__file__).resolve().parents[1] / "app/src/main/java/ru/costaccounting/mobile/RecurringSchema.kt").read_text(encoding="utf-8")
ALTER = re.search(r'const val ADD_ORIGIN = "([^"]+)"', SOURCE)[1]
CREATE = re.search(r'const val CREATE_PAYMENTS = """(.*?)"""', SOURCE, re.S)[1]
SPLIT_SOURCE = (Path(__file__).resolve().parents[1] / "app/src/main/java/ru/costaccounting/mobile/SplitSchema.kt").read_text(encoding="utf-8")
ADD_TITLE = re.search(r'const val ADD_TITLE = "([^"]+)"', SPLIT_SOURCE)[1]
CREATE_ALLOCATIONS = re.search(r'const val CREATE_ALLOCATIONS = """(.*?)"""', SPLIT_SOURCE, re.S)[1]


class MigrationTest(unittest.TestCase):
    def setUp(self):
        self.db = sqlite3.connect(":memory:")
        self.addCleanup(self.db.close)
        self.db.execute("PRAGMA foreign_keys=ON")
        self.db.executescript("""
            CREATE TABLE categories(id TEXT PRIMARY KEY,name TEXT NOT NULL,normalized_name TEXT NOT NULL UNIQUE,type TEXT NOT NULL);
            CREATE TABLE entries(id TEXT PRIMARY KEY,date INTEGER NOT NULL,amount INTEGER NOT NULL CHECK(amount>0),
                category_id TEXT NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,comment TEXT NOT NULL DEFAULT '',created_at INTEGER NOT NULL);
            INSERT INTO categories VALUES('cat','Дом','дом','Expense');
            INSERT INTO entries VALUES('old',123,1001,'cat','Прежняя операция',123);
            PRAGMA user_version=1;
        """)
        with self.db:
            self.db.execute(ALTER)
            self.db.execute(CREATE)
            self.db.execute("PRAGMA user_version=2")

    def rule(self, category="cat"):
        self.db.execute("INSERT INTO recurring_payments VALUES('plan','Аренда',5000,?,'2026-01-31','Monthly','2026-11-30',1)", (category,))

    def test_existing_data_survives_upgrade(self):
        self.assertEqual(self.db.execute("SELECT id,amount,comment,recurring_id FROM entries").fetchone(),
                         ("old", 1001, "Прежняя операция", None))
        self.assertEqual(self.db.execute("PRAGMA user_version").fetchone()[0], 2)

    def test_recurring_category_is_protected_and_validated(self):
        with self.assertRaises(sqlite3.IntegrityError):
            self.rule("missing")
        self.rule()
        self.db.execute("DELETE FROM entries")
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute("DELETE FROM categories")

    def test_removing_schedule_preserves_generated_history(self):
        self.rule()
        self.db.execute("INSERT INTO entries VALUES('generated',456,5000,'cat','По расписанию',456,'plan')")
        self.db.execute("DELETE FROM recurring_payments WHERE id='plan'")
        self.assertEqual(self.db.execute("SELECT amount,recurring_id FROM entries WHERE id='generated'").fetchone(), (5000, "plan"))

    def test_pending_cursor_and_entry_are_transactional(self):
        self.rule()
        self.db.commit()
        with self.assertRaises(sqlite3.IntegrityError):
            with self.db:
                self.db.execute("UPDATE recurring_payments SET next_date='2026-12-31'")
                self.db.execute("INSERT INTO entries VALUES('broken',456,5000,'missing','',456,'plan')")
        self.assertEqual(self.db.execute("SELECT next_date FROM recurring_payments").fetchone()[0], "2026-11-30")


class SplitMigrationTest(unittest.TestCase):
    def setUp(self):
        self.db = sqlite3.connect(":memory:")
        self.addCleanup(self.db.close)
        self.db.execute("PRAGMA foreign_keys=ON")
        self.db.executescript("""
            CREATE TABLE categories(id TEXT PRIMARY KEY,name TEXT NOT NULL,normalized_name TEXT NOT NULL UNIQUE,type TEXT NOT NULL);
            CREATE TABLE entries(id TEXT PRIMARY KEY,date INTEGER NOT NULL,amount INTEGER NOT NULL CHECK(amount>0),
                category_id TEXT NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,comment TEXT NOT NULL DEFAULT '',
                created_at INTEGER NOT NULL,recurring_id TEXT);
            INSERT INTO categories VALUES('food','Еда','еда','Expense');
            INSERT INTO categories VALUES('home','Дом','дом','Expense');
            INSERT INTO entries VALUES('old',123,1001,'food','Старая покупка',123,NULL);
            PRAGMA user_version=2;
        """)
        self.db.execute(CREATE)
        self.db.execute("INSERT INTO recurring_payments VALUES('plan','Подписка',50,'food','2026-01-01','Monthly','2026-12-01',1)")
        with self.db:
            self.db.execute(ADD_TITLE)
            self.db.execute(CREATE_ALLOCATIONS)
            self.db.execute("PRAGMA user_version=3")

    def test_upgrade_keeps_entries_and_schedules(self):
        self.assertEqual(self.db.execute("SELECT amount,comment,title FROM entries").fetchone(), (1001, "Старая покупка", ""))
        self.assertEqual(self.db.execute("SELECT amount FROM recurring_payments").fetchone()[0], 50)
        self.assertEqual(self.db.execute("PRAGMA user_version").fetchone()[0], 3)

    def test_null_allocations_are_supported_and_entry_deletion_cascades(self):
        self.db.execute("INSERT INTO entry_allocations VALUES('old','food',600)")
        self.db.execute("INSERT INTO entry_allocations VALUES('old','home',NULL)")
        self.assertEqual(self.db.execute("SELECT SUM(amount) FROM entry_allocations").fetchone()[0], 600)
        self.db.execute("DELETE FROM entries WHERE id='old'")
        self.assertEqual(self.db.execute("SELECT COUNT(*) FROM entry_allocations").fetchone()[0], 0)
        self.assertEqual(self.db.execute("SELECT COUNT(*) FROM categories").fetchone()[0], 2)

    def test_secondary_category_is_protected(self):
        self.db.execute("INSERT INTO entry_allocations VALUES('old','home',400)")
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute("DELETE FROM categories WHERE id='home'")

    def test_failed_distribution_update_rolls_back_purchase_and_parts(self):
        self.db.execute("INSERT INTO entry_allocations VALUES('old','home',400)")
        self.db.commit()
        with self.assertRaises(sqlite3.IntegrityError):
            with self.db:
                self.db.execute("UPDATE entries SET amount=10000 WHERE id='old'")
                self.db.execute("DELETE FROM entry_allocations WHERE entry_id='old'")
                self.db.execute("INSERT INTO entry_allocations VALUES('old','missing',10000)")
        self.assertEqual(self.db.execute("SELECT amount FROM entries WHERE id='old'").fetchone()[0], 1001)
        self.assertEqual(self.db.execute("SELECT category_id,amount FROM entry_allocations").fetchone(), ("home", 400))


if __name__ == "__main__":
    unittest.main(verbosity=2)
