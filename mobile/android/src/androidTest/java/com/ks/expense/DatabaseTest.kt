package com.ks.expense

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ks.expense.backup.BackupData
import com.ks.expense.data.Category
import com.ks.expense.data.DefaultCategories
import com.ks.expense.data.Expense
import com.ks.expense.data.ExpenseDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private lateinit var db: ExpenseDatabase

    @Before fun open() {
        db = ExpenseDatabase.build(InstrumentationRegistry.getInstrumentation().targetContext, name = null)
    }

    @After fun close() = db.close()

    @Test fun newDatabaseHasTheStartingCategories() = runBlocking {
        val categories = db.dao().allCategories()
        assertEquals(DefaultCategories.list.map { it.first }, categories.map { it.builtinKey })
        assertTrue(categories.none { it.hidden || it.name != null })
    }

    @Test fun betweenIsHalfOpenAndNewestFirst() = runBlocking {
        val dao = db.dao()
        val food = dao.allCategories().first().id
        listOf(100L, 200L, 300L).forEach { dao.insert(Expense(amountMinor = it, currency = "INR", categoryId = food, note = "", occurredAt = it, createdAt = it)) }
        assertEquals(listOf(200L, 100L), dao.between(100, 300).first().map { it.occurredAt })
    }

    @Test fun replaceAllSwapsEverything() = runBlocking {
        val dao = db.dao()
        dao.insert(Expense(amountMinor = 1, currency = "INR", categoryId = dao.allCategories().first().id, note = "", occurredAt = 1, createdAt = 1))
        val data = BackupData(
            listOf(Category(5, null, "Chai", "☕", false, 0)),
            listOf(Expense(9, 2_000, "INR", 5, "two cups", 50, 50)),
        )
        dao.replaceAll(data.categories, data.expenses)
        assertEquals(data.categories, dao.allCategories())
        assertEquals(data.expenses, dao.allExpenses())
    }

    @Test fun categoryWithExpensesCannotBeDeletedByRestoreHalfway() = runBlocking {
        val dao = db.dao()
        val before = dao.allCategories()
        // An expense pointing at a missing category breaks the foreign key: nothing changes.
        val failed = runCatching { dao.replaceAll(emptyList(), listOf(Expense(1, 100, "INR", 77, "", 1, 1))) }.isFailure
        assertTrue(failed)
        assertEquals(before, dao.allCategories())
    }
}
