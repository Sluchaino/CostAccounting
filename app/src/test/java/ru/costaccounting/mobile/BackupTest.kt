package ru.costaccounting.mobile

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class BackupTest {
    private val category = Category("00000000-0000-0000-0000-000000000001", "Дом", CategoryType.Expense)
    private val rule = RecurringPayment("00000000-0000-0000-0000-000000000002", "Аренда", 50000, category.id, category.name,
        category.type, LocalDate.of(2027, 1, 31), Frequency.Monthly, LocalDate.of(2027, 2, 28))
    private val entry = Entry("00000000-0000-0000-0000-000000000003", 1234567, 50000, category.id,
        category.name, category.type, "Регулярно: Аренда", 1234567, rule.id)
    private val ledger = Ledger(listOf(category), listOf(entry), listOf(rule))

    @Test fun `backup preserves schedule cursor enabled state and entry origin`() {
        assertEquals(ledger, Backup.decode(Backup.encode(ledger)))
        val paused = ledger.copy(recurring = listOf(rule.copy(enabled = false)))
        assertEquals(paused, Backup.decode(Backup.encode(paused)))
    }

    @Test fun `v1 backups remain compatible without recurring records`() {
        val old = JSONObject(Backup.encode(ledger)).apply {
            put("version", 1); remove("recurring"); getJSONArray("entries").getJSONObject(0).remove("recurringId")
        }
        val restored = Backup.decode(old.toString())
        assertTrue(restored.recurring.isEmpty())
        assertNull(restored.entries.single().recurringId)
        assertEquals(ledger.categories, restored.categories)
    }

    @Test fun `origin survives deletion of the recurring rule`() {
        val deleted = ledger.copy(recurring = emptyList())
        assertEquals(deleted, Backup.decode(Backup.encode(deleted)))
    }

    @Test fun `invalid rule references amounts and cursor alignment are rejected`() {
        listOf<(JSONObject) -> Unit>(
            { it.put("categoryId", "missing") },
            { it.put("amount", 0) },
            { it.put("amount", 1.5) },
            { it.put("nextDate", "2027-02-27") },
            { it.put("nextDate", "2026-12-31") },
            { it.put("frequency", "Weekly") },
            { it.put("name", " ") }
        ).forEach { change ->
            val json = JSONObject(Backup.encode(ledger))
            change(json.getJSONArray("recurring").getJSONObject(0))
            assertThrows(Exception::class.java) { Backup.decode(json.toString()) }
        }
    }

    @Test fun `unknown format and duplicate rules are rejected`() {
        val json = JSONObject(Backup.encode(ledger))
        json.getJSONArray("recurring").put(json.getJSONArray("recurring").getJSONObject(0))
        assertThrows(IllegalArgumentException::class.java) { Backup.decode(json.toString()) }
        assertThrows(IllegalArgumentException::class.java) { Backup.decode(JSONObject(Backup.encode(ledger)).put("version", 999).toString()) }
    }
}
