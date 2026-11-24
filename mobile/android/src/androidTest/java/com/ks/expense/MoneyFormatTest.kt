package com.ks.expense

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ks.expense.transactions.Money
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/** Formatting runs on the device: Android's formatter (ICU) is the one that groups Indian style. */
@RunWith(AndroidJUnit4::class)
class MoneyFormatTest {
    private val english = Locale.forLanguageTag("en")
    private val hindi = Locale.forLanguageTag("hi")

    @Test fun rupeesWithIndianGrouping() {
        assertEquals("₹1,23,456.50", Money.format(12_345_650, "INR", english))
        assertEquals("₹1,23,456", Money.format(12_345_600, "INR", english))
        assertEquals("₹1,00,00,000", Money.format(1_000_000_000, "INR", Locale.US))
        assertEquals("₹0.05", Money.format(5, "INR", english))
        assertEquals("₹450", Money.format(45_000, "INR", english))
    }

    @Test fun hindiKeepsLatinDigits() {
        val text = Money.format(12_345_650, "INR", hindi)
        assertEquals("₹1,23,456.50", text)
    }

    @Test fun otherCurrencies() {
        assertEquals("$1,234.50", Money.format(123_450, "USD", Locale.US))
        assertEquals("¥500", Money.format(500, "JPY", Locale.US).replace("JP¥", "¥"))
    }

    @Test fun symbol() {
        assertEquals("₹", Money.symbol("INR", english))
        assertEquals("₹", Money.symbol("INR", hindi))
    }
}
