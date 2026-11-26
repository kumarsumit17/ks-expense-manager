package com.ks.expense.backup

import com.ks.expense.data.Budget
import com.ks.expense.data.Category
import com.ks.expense.data.Expense
import com.ks.expense.transactions.Money

/** Everything a backup file holds. */
data class BackupData(
    val categories: List<Category>,
    val expenses: List<Expense>,
    val budgets: List<Budget> = emptyList(),
    /** Null in files from before version 3, which leave the setting as it is. */
    val defaultCurrency: String? = null,
) {
    /** True when the data can be put into the database as it is. */
    fun isValid(): Boolean {
        val categoryIds = categories.map { it.id }.toSet()
        if (categoryIds.size != categories.size || categoryIds.any { it <= 0 }) return false
        if (categories.any { it.icon.isBlank() || (it.builtinKey.isNullOrBlank() && it.name.isNullOrBlank()) }) return false
        val expenseIds = expenses.map { it.id }.toSet()
        if (expenseIds.size != expenses.size || expenseIds.any { it <= 0 }) return false
        val expensesOk = expenses.all {
            it.categoryId in categoryIds &&
                it.amountMinor in 1..Money.MAX_MINOR &&
                CURRENCY.matches(it.currency)
        }
        val budgetIds = budgets.map { it.id }.toSet()
        val budgetsOk = budgetIds.size == budgets.size && budgetIds.none { it <= 0 } &&
            budgets.map { it.categoryId }.toSet().size == budgets.size &&
            budgets.all {
                (it.categoryId == null || it.categoryId in categoryIds) &&
                    it.amountMinor in 1..Money.MAX_MINOR &&
                    CURRENCY.matches(it.currency)
            }
        val settingsOk = defaultCurrency == null || com.ks.expense.transactions.Currencies.isValid(defaultCurrency)
        return expensesOk && budgetsOk && settingsOk
    }

    companion object {
        private val CURRENCY = Regex("[A-Z]{3}")
    }
}
