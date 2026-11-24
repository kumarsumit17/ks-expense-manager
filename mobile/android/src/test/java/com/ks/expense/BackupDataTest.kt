package com.ks.expense

import com.ks.expense.backup.BackupData
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
        ).forEach { assertFalse(it.toString(), it.isValid()) }
    }
}
