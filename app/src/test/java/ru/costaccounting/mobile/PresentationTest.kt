package ru.costaccounting.mobile

import org.junit.Assert.*
import org.junit.Test

class PresentationTest {
    private fun category(id: String, amount: Long) = CategoryTotal(id, id, CategoryType.Expense, amount)

    @Test fun `currency always displays two decimal digits including zero kopecks`() {
        listOf(0L, 1L, 5L, 10L, 99L, 100L, 105L, 12345L, Money.MAX, -105L).forEach { amount ->
            val formatted = Money.format(amount)
            assertTrue(formatted, Regex(",[0-9]{2}[^0-9]*$").containsMatchIn(formatted))
            assertTrue(formatted, formatted.contains("₽"))
        }
        assertTrue(Money.format(1).contains("0,01"))
        assertTrue(Money.format(105).contains("1,05"))
    }

    @Test fun `input is a single comma separated amount and accepts a decimal keyboard dot`() {
        assertEquals("123,45", MoneyInputText.display("123.45"))
        assertEquals("0,01", MoneyInputText.display("0.01"))
        assertEquals("123,45", MoneyInputText.display("123,45"))
        assertEquals(12345, Money.parse(MoneyInputText.decimalValue("123,45")))
        assertEquals(12345, Money.parse(MoneyInputText.decimalValue("123.45")))
        assertEquals(5, Money.parse(MoneyInputText.decimalValue(",05")))
        assertEquals(1250, Money.parse(MoneyInputText.decimalValue("12,5")))
        assertEquals(1200, Money.parse(MoneyInputText.decimalValue("12,")))
    }

    @Test fun `grouped pasted amounts keep every kopek`() {
        listOf("1 234,56", "1\u00a0234,56", "1\u202f234.56").forEach { input ->
            assertEquals(123456, Money.parse(MoneyInputText.decimalValue(input)))
        }
        assertEquals(Money.MAX, Money.parse(MoneyInputText.decimalValue("9 999 999 999,99")))
    }

    @Test fun `invalid and excessive cents are rejected instead of silently rounded`() {
        listOf("12,345", "12.3.4", "-12,50", "text", "", ",").forEach { input ->
            assertThrows(IllegalArgumentException::class.java) { MoneyInputText.decimalValue(input) }
        }
        assertThrows(IllegalArgumentException::class.java) { Money.parse(MoneyInputText.decimalValue("0,00")) }
    }

    @Test fun `chart preserves every kopek and spans exactly one circle`() {
        val slices = donutSlices(listOf(category("food", 12345), category("rent", 20001), category("transport", 1)))
        assertEquals(32347, slices.sumOf { it.amount })
        assertEquals(360.0, slices.sumOf { it.sweep }, 0.0000001)
        assertEquals(0.0, slices.first().start, 0.0000001)
        assertEquals(slices[0].start + slices[0].sweep, slices[1].start, 0.0000001)
        assertTrue(slices.all { it.sweep > 0 })
    }

    @Test fun `empty zero and single category charts have defined geometry`() {
        assertTrue(donutSlices(emptyList()).isEmpty())
        assertTrue(donutSlices(listOf(category("none", 0))).isEmpty())
        val single = donutSlices(listOf(category("food", 1))).single()
        assertEquals(360.0, single.sweep, 0.0)
        assertEquals("100,0 %", single.percentage)
    }

    @Test fun `small shares are not labelled as zero`() {
        val slices = donutSlices(listOf(category("big", 99999), category("tiny", 1)))
        assertEquals("<0,1 %", slices.last().percentage)
        assertEquals(1, slices.last().amount)
    }

    @Test fun `sector taps handle boundaries and angle wrap around`() {
        val slices = donutSlices(listOf(category("a", 1), category("b", 1), category("c", 2)))
        assertEquals(0, donutSliceAt(slices, 0.0))
        assertEquals(0, donutSliceAt(slices, 89.999))
        assertEquals(1, donutSliceAt(slices, 90.0))
        assertEquals(2, donutSliceAt(slices, 180.0))
        assertEquals(2, donutSliceAt(slices, -90.0))
        assertEquals(0, donutSliceAt(slices, 360.0))
        assertEquals(-1, donutSliceAt(emptyList(), 10.0))
        assertEquals(-1, donutSliceAt(slices, Double.NaN))
    }
}
