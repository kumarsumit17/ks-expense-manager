package com.ks.expense

import com.ks.expense.backup.BackupData
import com.ks.expense.data.Budget
import com.ks.expense.data.Category
import com.ks.expense.data.Expense
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupDataTest {
    private val food = Category(1, "food", null, "🍽️", false, 0)
    private val mine = Category(2, null, "Chai", "📦", true, 1)
    private val lunch = Expense(1, 25_000, "INR", 1, "Lunch", 1_000, 1_000)

    @Test fun validData() {
        assertTrue(BackupData(listOf(food, mine), listOf(lunch)).isValid())
        assertTrue(BackupData(emptyList(), emptyList()).isValid())
        assertTrue(BackupData(emptyList(), emptyList(), emptyList(), "USD").isValid())
        assertTrue(BackupData(listOf(food), emptyList(), listOf(Budget(1, null, 1_000_000, "INR"), Budget(2, 1, 200_000, "INR"))).isValid())
    }

    @Test fun invalidData() {
        listOf(
            BackupData(listOf(food), listOf(lunch.copy(categoryId = 9))),
            BackupData(listOf(food, food), emptyList()),
            BackupData(listOf(food), listOf(lunch, lunch)),
            BackupData(listOf(food), listOf(lunch.copy(amountMinor = 0))),
            BackupData(listOf(food), listOf(lunch.copy(amountMinor = -5))),
            BackupData(listOf(food), listOf(lunch.copy(currency = "inr"))),
            BackupData(listOf(food), listOf(lunch.copy(currency = "RUPEE"))),
            BackupData(listOf(food.copy(builtinKey = null)), emptyList()),
            BackupData(listOf(food.copy(icon = " ")), emptyList()),
            BackupData(listOf(food.copy(id = 0)), emptyList()),
            BackupData(listOf(food), emptyList(), listOf(Budget(1, 9, 100, "INR"))),
            BackupData(listOf(food), emptyList(), emptyList(), "XAU"),
            BackupData(listOf(food), emptyList(), emptyList(), "usd"),
            BackupData(listOf(food), emptyList(), listOf(Budget(1, 1, 0, "INR"))),
            BackupData(listOf(food), emptyList(), listOf(Budget(1, 1, 100, "inr"))),
            BackupData(listOf(food), emptyList(), listOf(Budget(1, null, 100, "INR"), Budget(2, null, 200, "INR"))),
            BackupData(listOf(food), emptyList(), listOf(Budget(1, 1, 100, "INR"), Budget(1, null, 200, "INR"))),
        ).forEach { assertFalse(it.toString(), it.isValid()) }
    }
}
