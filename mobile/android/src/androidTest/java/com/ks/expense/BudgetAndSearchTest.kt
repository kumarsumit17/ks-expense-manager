package com.ks.expense

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ks.expense.data.Expense
import com.ks.expense.data.ExpenseDatabase
import com.ks.expense.transactions.Search
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BudgetAndSearchTest {
    private lateinit var db: ExpenseDatabase
    private var food = 0L
    private var bills = 0L

    @Before fun open() = runBlocking {
        db = ExpenseDatabase.build(InstrumentationRegistry.getInstrumentation().targetContext, name = null)
        val categories = db.dao().allCategories()
        food = categories.first { it.builtinKey == "food" }.id
        bills = categories.first { it.builtinKey == "bills" }.id
        listOf(
            Expense(1, 45_000, "INR", food, "Lunch with Ravi", 1_000, 1_000),
            Expense(2, 120_000, "INR", bills, "Electricity 50% share", 2_000, 2_000),
            Expense(3, 9_950, "INR", food, "chai_and_snacks", 3_000, 3_000),
            Expense(4, 45_000, "INR", bills, "", 4_000, 4_000),
        ).forEach { db.dao().insert(it) }
    }

    @After fun close() = db.close()

    private fun search(
        text: String = "",
        nameCategories: List<Long> = emptyList(),
        categories: List<Long> = emptyList(),
        from: Long? = null,
        to: Long? = null,
    ): List<Long> = runBlocking {
        db.dao().search(
            Search.likePattern(text), Search.amount(text, "INR"), nameCategories.ifEmpty { listOf(-1L) },
            categories.isEmpty(), categories.ifEmpty { listOf(-1L) }, from, to, Search.LIMIT,
        ).first().map { it.id }
    }

    @Test fun budgetsSetReplaceRemove() = runBlocking {
        val dao = db.dao()
        dao.setBudget(null, 1_000_000, "INR")
        dao.setBudget(food, 200_000, "INR")
        dao.setBudget(food, 300_000, "INR")
        assertEquals(setOf(null to 1_000_000L, food to 300_000L), dao.allBudgets().map { it.categoryId to it.amountMinor }.toSet())
        dao.setBudget(null, null, "INR")
        assertEquals(listOf(food), dao.allBudgets().map { it.categoryId })
    }

    @Test fun textAmountAndNames() {
        assertEquals(listOf(1L), search("ravi"))
        assertEquals(listOf(2L), search("50%"))
        assertEquals(listOf(3L), search("_and_"))
        assertEquals(emptyList<Long>(), search("chai and"))
        assertEquals(listOf(4L, 1L), search("450"))
        assertEquals(listOf(4L, 2L), search("bil", nameCategories = listOf(bills)))
    }

    @Test fun filters() {
        assertEquals(listOf(3L, 1L), search(categories = listOf(food)))
        assertEquals(listOf(3L, 2L), search(from = 2_000, to = 4_000))
        assertEquals(listOf(1L), search("450", categories = listOf(food)))
        assertEquals(listOf(4L, 3L, 2L, 1L), search())
    }
}
