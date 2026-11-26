package com.ks.expense

import com.ks.expense.transactions.Currencies
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class CurrenciesTest {
    @Test fun commonFirstThenTheRestOnce() {
        val all = Currencies.all(Locale.ENGLISH)
        assertEquals("INR", all.first())
        assertEquals(Currencies.COMMON, all.take(Currencies.COMMON.size))
        assertEquals(all.size, all.toSet().size)
        assertTrue(all.size > 100)
    }

    @Test fun onlyRealCurrencies() {
        assertTrue(Currencies.isValid("INR"))
        assertTrue(Currencies.isValid("JPY"))
        assertFalse(Currencies.isValid("XAU"))
        assertFalse(Currencies.isValid("XTS"))
        assertFalse(Currencies.isValid("USN"))
        assertTrue(Currencies.isValid("XOF"))
        assertFalse(Currencies.isValid("inr"))
        assertFalse(Currencies.isValid("RUPEE"))
        assertFalse(Currencies.isValid("ZZZ"))
        assertFalse(Currencies.all(Locale.ENGLISH).any { !Currencies.isValid(it) })
    }

    @Test fun filterByCodeOrName() {
        val all = Currencies.all(Locale.ENGLISH)
        assertEquals(listOf("USD"), Currencies.filter(all, "usd", Locale.ENGLISH))
        assertTrue("INR" in Currencies.filter(all, "rupee", Locale.ENGLISH))
        assertEquals(all, Currencies.filter(all, " ", Locale.ENGLISH))
    }
}
