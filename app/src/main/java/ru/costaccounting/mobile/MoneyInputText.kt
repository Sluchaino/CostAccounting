package ru.costaccounting.mobile

object MoneyInputText {
    fun display(value: String): String = value.replace('.', ',')

    fun decimalValue(value: String): String {
        val normalized = display(value.trim().replace(" ", "").replace("\u00a0", "").replace("\u202f", ""))
        require(Regex("^[0-9]*(?:,[0-9]{0,2})?$").matches(normalized) && normalized.any { it in '0'..'9' }) {
            "Введите сумму через запятую, например 1250,45"
        }
        return if (normalized.startsWith(',')) "0$normalized" else normalized
    }
}
