package com.ks.expense.transactions

import java.util.Currency
import java.util.Locale

/** The currencies the user can pick: common ones first, then every other ISO 4217 currency by name. */
object Currencies {
    /** Shown first, in this order (rupees, then the ones Indian users meet most). */
    val COMMON = listOf("INR", "USD", "EUR", "GBP", "AED", "SAR", "SGD", "AUD", "CAD", "JPY", "NPR", "LKR", "BDT", "THB", "CHF", "CNY")

    private val CODE = Regex("[A-Z]{3}")

    /**
     * ISO 4217 codes that are not money people spend: precious metals, IMF and bond units, funds and
     * testing codes. Listed by name because Android reports decimals for some of them (gold: 2).
     */
    private val NOT_MONEY = setOf(
        "XAU", "XAG", "XPT", "XPD", "XDR", "XSU", "XUA", "XBA", "XBB", "XBC", "XBD", "XTS", "XXX", "XFO", "XFU", "XRE",
        "BOV", "CHE", "CHW", "CLF", "COU", "MXV", "USN", "USS", "UYI", "UYW",
    )

    /** True for a real currency code with a normal number of decimals (no gold, funds or test codes). */
    fun isValid(code: String): Boolean =
        CODE.matches(code) && code !in NOT_MONEY &&
            runCatching { Currency.getInstance(code).defaultFractionDigits >= 0 }.getOrDefault(false)

    /** Every pickable code: [COMMON] first, then the rest sorted by name in [locale]. */
    fun all(locale: Locale): List<String> {
        val rest = Currency.getAvailableCurrencies()
            .map { it.currencyCode }
            .filter { isValid(it) && it !in COMMON }
            .sortedBy { name(it, locale).lowercase(locale) }
        return COMMON.filter { isValid(it) } + rest
    }

    fun name(code: String, locale: Locale): String = Currency.getInstance(code).getDisplayName(locale)

    /** "Indian Rupee (INR)". */
    fun label(code: String, locale: Locale): String = "${name(code, locale)} ($code)"

    /** Codes whose code or name contains [text], in [all]'s order. */
    fun filter(codes: List<String>, text: String, locale: Locale): List<String> {
        val t = text.trim()
        if (t.isEmpty()) return codes
        return codes.filter { it.contains(t, ignoreCase = true) || name(it, locale).contains(t, ignoreCase = true) }
    }
}
