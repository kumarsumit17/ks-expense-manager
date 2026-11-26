package com.ks.expense

import com.ks.expense.transactions.Search
import com.ks.expense.transactions.SearchFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTest {
    @Test fun likePatternEscapesWildcards() {
        assertEquals("%lunch%", Search.likePattern(" lunch "))
        assertEquals("%50\\%%", Search.likePattern("50%"))
        assertEquals("%a\\_b%", Search.likePattern("a_b"))
        assertEquals("%c:\\\\x%", Search.likePattern("c:\\x"))
        assertEquals("%चाय%", Search.likePattern("चाय"))
        assertNull(Search.likePattern("   "))
    }

    @Test fun amountOnlyWhenTheTextIsOne() {
        assertEquals(45_000L, Search.amount("450", "INR"))
        assertEquals(12_345_650L, Search.amount("1,23,456.50", "INR"))
        assertNull(Search.amount("lunch", "INR"))
    }

    @Test fun categoryNames() {
        assertTrue(Search.nameMatches("Food", "foo"))
        assertTrue(Search.nameMatches("खाना", "खा"))
        assertFalse(Search.nameMatches("Food", " "))
        assertFalse(Search.nameMatches("Bills", "food"))
    }

    @Test fun emptyFilter() {
        assertTrue(SearchFilter().isEmpty)
        assertTrue(SearchFilter(text = "  ").isEmpty)
        assertFalse(SearchFilter(categoryIds = setOf(1)).isEmpty)
        assertFalse(SearchFilter(fromMillis = 0).isEmpty)
    }
}
