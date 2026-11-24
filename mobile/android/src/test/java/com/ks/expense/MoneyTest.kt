package com.ks.expense

import com.ks.expense.transactions.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test fun parsesRupeesToPaise() {
        assertEquals(25_000L, Money.parse("250", "INR"))
        assertEquals(9_950L, Money.parse("99.50", "INR"))
        assertEquals(9_950L, Money.parse("99.5", "INR"))
        assertEquals(50L, Money.parse(".5", "INR"))
        assertEquals(5L, Money.parse("0.05", "INR"))
        assertEquals(100L, Money.parse("1.", "INR"))
        assertEquals(12_345_650L, Money.parse("1,23,456.5", "INR"))
        assertEquals(12_345_650L, Money.parse(" 123 456.50 ", "INR"))
        assertEquals(1_000L, Money.parse("10.000", "INR"))
    }

    @Test fun rejectsWhatIsNotAnAmount() {
        listOf("", " ", ".", "0", "0.00", "-5", "+5", "1e3", "12.345", "1.2.3", "abc", "₹10", "1,000.0.5")
            .forEach { assertNull(it, Money.parse(it, "INR")) }
    }

    @Test fun rejectsTooLarge() {
        assertEquals(Money.MAX_MINOR, Money.parse("10000000000000", "INR"))
        assertNull(Money.parse("10000000000000.01", "INR"))
        assertNull(Money.parse("99999999999999999999999", "INR"))
    }

    @Test fun followsTheCurrencysDecimals() {
        assertEquals(0, Money.fractionDigits("JPY"))
        assertEquals(3, Money.fractionDigits("KWD"))
        assertEquals(2, Money.fractionDigits("INR"))
        assertEquals(500L, Money.parse("500", "JPY"))
        assertNull(Money.parse("500.5", "JPY"))
        assertEquals(1_250L, Money.parse("1.25", "KWD"))
    }

    @Test fun inputAndPlainText() {
        assertEquals("250.5", Money.toInput(25_050, "INR"))
        assertEquals("100", Money.toInput(10_000, "INR"))
        assertEquals("0.05", Money.toInput(5, "INR"))
        assertEquals("250.50", Money.toPlain(25_050, "INR"))
        assertEquals("100.00", Money.toPlain(10_000, "INR"))
        assertEquals("500", Money.toPlain(500, "JPY"))
    }

    @Test fun rupeesAlwaysUseTheIndianRegion() {
        assertEquals("en-IN-u-nu-latn", Money.formatLocale("INR", java.util.Locale.US).toLanguageTag())
        assertEquals("hi-IN-u-nu-latn", Money.formatLocale("INR", java.util.Locale.forLanguageTag("hi")).toLanguageTag())
        assertEquals("en-US-u-nu-latn", Money.formatLocale("USD", java.util.Locale.US).toLanguageTag())
    }
}
