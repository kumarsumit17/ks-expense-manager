package com.ks.expense

import com.ks.expense.budgets.BudgetLevel
import com.ks.expense.budgets.BudgetStatus
import com.ks.expense.data.Budget
import com.ks.expense.data.Category
import com.ks.expense.reports.CategoryTotal
import com.ks.expense.ui.barRows
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetsTest {
    @Test fun levels() {
        assertEquals(BudgetLevel.OK, BudgetStatus(0, 10_000).level)
        assertEquals(BudgetLevel.OK, BudgetStatus(7_999, 10_000).level)
        assertEquals(BudgetLevel.NEAR, BudgetStatus(8_000, 10_000).level)
        assertEquals(BudgetLevel.NEAR, BudgetStatus(10_000, 10_000).level)
        assertEquals(BudgetLevel.OVER, BudgetStatus(10_001, 10_000).level)
    }

    @Test fun leftAndPercent() {
        assertEquals(5_500L, BudgetStatus(4_500, 10_000).leftMinor)
        assertEquals(-500L, BudgetStatus(10_500, 10_000).leftMinor)
        assertEquals(45, BudgetStatus(4_500, 10_000).percent)
        assertEquals(99, BudgetStatus(9_996, 10_000).percent)
        assertEquals(105, BudgetStatus(10_500, 10_000).percent)
        assertEquals(1.05f, BudgetStatus(10_500, 10_000).fraction, 0.0001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun limitMustBePositive() {
        BudgetStatus(1, 0)
    }

    private val food = Category(1, "food", null, "🍛", false, 0)
    private val bills = Category(2, "bills", null, "🧾", false, 3)
    private val health = Category(3, "health", null, "💊", false, 4)

    @Test fun barRowsAddBudgetedCategoriesWithoutSpending() {
        val rows = barRows(
            listOf(CategoryTotal(1, "INR", 45_000, 1f)),
            mapOf(2L to Budget(1, 2, 200_000, "INR"), 1L to Budget(2, 1, 100_000, "INR")),
            mapOf(1L to food, 2L to bills, 3L to health),
        )
        assertEquals(listOf(1L, 2L), rows.map { it.category.id })
        assertEquals(100_000L, rows[0].budget?.amountMinor)
        assertEquals(0L, rows[1].spentMinor)
    }

    @Test fun budgetInAnotherCurrencyDoesNotApply() {
        val rows = barRows(
            listOf(CategoryTotal(1, "USD", 500, 1f)),
            mapOf(1L to Budget(1, 1, 100_000, "INR")),
            mapOf(1L to food),
        )
        assertEquals(listOf("USD", "INR"), rows.map { it.currency })
        assertEquals(null, rows[0].budget)
        assertEquals(0L, rows[1].spentMinor)
    }
}
