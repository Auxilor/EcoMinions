package com.exanthiax.ecominions.core.text

/** Roman numerals, shared by the upgrade menu and the level placeholders. */
object Numerals {

    private val VALUES = listOf(
        1000 to "M", 900 to "CM", 500 to "D", 400 to "CD",
        100 to "C", 90 to "XC", 50 to "L", 40 to "XL",
        10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I"
    )

    /** @return [value] as a numeral, or its digits when it has no numeral form. */
    fun roman(value: Int): String {
        if (value <= 0) {
            return value.toString()
        }

        val builder = StringBuilder()
        var remaining = value

        for ((amount, numeral) in VALUES) {
            while (remaining >= amount) {
                builder.append(numeral)
                remaining -= amount
            }
        }
        return builder.toString()
    }
}
