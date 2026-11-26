package com.ks.expense.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ExpenseDao {
    /** Expenses with [from] ≤ occurred_at < [to] (UTC millis), newest first. */
    @Query("SELECT * FROM expense WHERE occurred_at >= :from AND occurred_at < :to ORDER BY occurred_at DESC, id DESC")
    abstract fun between(from: Long, to: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expense ORDER BY occurred_at, id")
    abstract suspend fun allExpenses(): List<Expense>

    @Insert
    abstract suspend fun insert(expense: Expense): Long

    @Update
    abstract suspend fun update(expense: Expense)

    @Delete
    abstract suspend fun delete(expense: Expense)

    @Query("SELECT * FROM category ORDER BY sort, id")
    abstract fun categories(): Flow<List<Category>>

    @Query("SELECT * FROM category ORDER BY sort, id")
    abstract suspend fun allCategories(): List<Category>

    @Insert
    abstract suspend fun insertCategory(category: Category): Long

    @Update
    abstract suspend fun updateCategory(category: Category)

    @Query("SELECT COALESCE(MAX(sort), -1) FROM category")
    abstract suspend fun maxSort(): Int

    /**
     * Search across all months, newest first, at most [limit] rows. [pattern] is a LIKE pattern
     * with `\` as escape (null: no text); [amountMinor] matches an amount exactly; [nameCategories]
     * are categories whose shown name matches the text. Category and date filters apply on top.
     */
    @Query(
        """
        SELECT * FROM expense
        WHERE (:pattern IS NULL OR note LIKE :pattern ESCAPE '\'
               OR amount_minor = :amountMinor OR category_id IN (:nameCategories))
          AND (:anyCategory OR category_id IN (:categories))
          AND (:from IS NULL OR occurred_at >= :from)
          AND (:to IS NULL OR occurred_at < :to)
        ORDER BY occurred_at DESC, id DESC
        LIMIT :limit
        """,
    )
    abstract fun search(
        pattern: String?,
        amountMinor: Long?,
        nameCategories: List<Long>,
        anyCategory: Boolean,
        categories: List<Long>,
        from: Long?,
        to: Long?,
        limit: Int,
    ): Flow<List<Expense>>

    @Query("SELECT * FROM budget")
    abstract fun budgets(): Flow<List<Budget>>

    @Query("SELECT * FROM budget ORDER BY id")
    abstract suspend fun allBudgets(): List<Budget>

    @Query("DELETE FROM budget WHERE category_id IS NULL")
    protected abstract suspend fun deleteOverallBudget()

    @Query("DELETE FROM budget WHERE category_id = :categoryId")
    protected abstract suspend fun deleteCategoryBudget(categoryId: Long)

    @Insert
    protected abstract suspend fun insertBudget(budget: Budget)

    /** Sets or, with [amountMinor] null, removes the budget for [categoryId] (null: the whole month). */
    @Transaction
    open suspend fun setBudget(categoryId: Long?, amountMinor: Long?, currency: String) {
        if (categoryId == null) deleteOverallBudget() else deleteCategoryBudget(categoryId)
        if (amountMinor != null) insertBudget(Budget(categoryId = categoryId, amountMinor = amountMinor, currency = currency))
    }

    @Insert
    protected abstract suspend fun insertBudgets(budgets: List<Budget>)

    @Query("DELETE FROM budget")
    protected abstract suspend fun deleteAllBudgets()

    @Insert
    protected abstract suspend fun insertCategories(categories: List<Category>)

    @Insert
    protected abstract suspend fun insertExpenses(expenses: List<Expense>)

    @Query("DELETE FROM expense")
    protected abstract suspend fun deleteAllExpenses()

    @Query("DELETE FROM category")
    protected abstract suspend fun deleteAllCategories()

    /** Restore: replaces everything in one transaction, so a failure changes nothing. */
    @Transaction
    open suspend fun replaceAll(categories: List<Category>, expenses: List<Expense>, budgets: List<Budget> = emptyList()) {
        deleteAllBudgets()
        deleteAllExpenses()
        deleteAllCategories()
        insertCategories(categories)
        insertExpenses(expenses)
        insertBudgets(budgets)
    }
}
