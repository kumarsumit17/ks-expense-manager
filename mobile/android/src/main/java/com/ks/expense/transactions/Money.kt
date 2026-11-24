package com.ks.expense.transactions

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Money is a Long in minor units (paise, cents) plus an ISO 4217 code; never a Double or Float.
 */
object Money {
    const val DEFAULT_CURRENCY = "INR"

    /** Largest amount one expense may have: far below Long.MAX_VALUE, so sums never overflow. */
    const val MAX_MINOR = 1_000_000_000_000_000L

    private val INPUT = Regex("""\d*(\.\d*)?""")

    /** Digits after the decimal point for [currency]: 2 for INR and USD, 0 for JPY, 3 for KWD. */
    fun fractionDigits(currency: String): Int =
        runCatching { Currency.getInstance(currency).defaultFractionDigits }.getOrNull()
            ?.takeIf { it >= 0 } ?: 2

    /**
     * Reads what the user typed ("1,23,456.5", "99.50", ".5") as minor units of [currency].
     * Null for empty, zero, negative, too many decimals, too large, or anything else.
     */
    fun parse(text: String, currency: String): Long? {
        val cleaned = text.trim().filterNot { it == ',' || it == ' ' || it == ' ' }
        if (cleaned.isEmpty() || cleaned == "." || !INPUT.matches(cleaned)) return null
        val value = cleaned.toBigDecimalOrNull() ?: return null
        val digits = fractionDigits(currency)
        if (value.stripTrailingZeros().scale() > digits) return null
        val minor = try {
            value.movePointRight(digits).setScale(0).longValueExact()
        } catch (_: ArithmeticException) {
            return null
        }
        return minor.takeIf { it in 1..MAX_MINOR }
    }

    /** The amount as the user would type it again: 25050 INR → "250.5", 10000 → "100". */
    fun toInput(minor: Long, currency: String): String =
        BigDecimal.valueOf(minor, fractionDigits(currency)).stripTrailingZeros().toPlainString()

    /** The amount as a plain decimal for files: 25050 INR → "250.50". */
    fun toPlain(minor: Long, currency: String): String =
        BigDecimal.valueOf(minor, fractionDigits(currency)).toPlainString()

    /**
     * The amount for display, with the platform's currency formatter. Rupees always use the
     * Indian region, so they get Indian digit grouping (₹1,23,456.50); whole amounts drop the
     * decimals (₹450). Digits are always Latin.
     */
    fun format(minor: Long, currency: String, language: Locale): String {
        val format = NumberFormat.getCurrencyInstance(formatLocale(currency, language))
        format.currency = Currency.getInstance(currency)
        val digits = fractionDigits(currency)
        val whole = BigDecimal.valueOf(minor).remainder(BigDecimal.TEN.pow(digits)).signum() == 0
        format.minimumFractionDigits = if (whole) 0 else digits
        format.maximumFractionDigits = digits
        return format.format(BigDecimal.valueOf(minor, digits))
    }

    /** The currency symbol as shown next to the amount field: "₹" for INR. */
    fun symbol(currency: String, language: Locale): String =
        Currency.getInstance(currency).getSymbol(formatLocale(currency, language))

    fun formatLocale(currency: String, language: Locale): Locale {
        val builder = Locale.Builder().setLanguage(if (language.language == "hi") "hi" else "en")
        val region = if (currency == "INR") "IN" else language.country.ifEmpty { Locale.getDefault().country }
        if (region.isNotEmpty()) builder.setRegion(region)
        return builder.setUnicodeLocaleKeyword("nu", "latn").build()
    }
}
