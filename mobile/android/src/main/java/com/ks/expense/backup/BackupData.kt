package com.ks.expense.backup

import com.ks.expense.data.Category
import com.ks.expense.data.Expense
import com.ks.expense.transactions.Money

/** Everything a backup file holds. */
data class BackupData(val categories: List<Category>, val expenses: List<Expense>) {
    /** True when the data can be put into the database as it is. */
    fun isValid(): Boolean {
        val categoryIds = categories.map { it.id }.toSet()
        if (categoryIds.size != categories.size || categoryIds.any { it <= 0 }) return false
        if (categories.any { it.icon.isBlank() || (it.builtinKey.isNullOrBlank() && it.name.isNullOrBlank()) }) return false
        val expenseIds = expenses.map { it.id }.toSet()
        if (expenseIds.size != expenses.size || expenseIds.any { it <= 0 }) return false
        return expenses.all {
            it.categoryId in categoryIds &&
                it.amountMinor in 1..Money.MAX_MINOR &&
                CURRENCY.matches(it.currency)
        }
    }

    companion object {
        private val CURRENCY = Regex("[A-Z]{3}")
    }
}
