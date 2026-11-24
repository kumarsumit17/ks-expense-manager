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
    open suspend fun replaceAll(categories: List<Category>, expenses: List<Expense>) {
        deleteAllExpenses()
        deleteAllCategories()
        insertCategories(categories)
        insertExpenses(expenses)
    }
}
