package com.ks.expense

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ks.expense.data.ExpenseDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Every exported schema must open with the current database and keep its data. When version 2
 * comes, add: create version 1 here, then `helper.runMigrationsAndValidate(NAME, 2, true, *MIGRATIONS)`.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val name = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), ExpenseDatabase::class.java)

    @Test fun version1OpensWithTheCurrentDatabaseAndKeepsData() {
        helper.createDatabase(name, 1).use { db ->
            db.execSQL("INSERT INTO category (id, builtin_key, name, icon, hidden, sort) VALUES (1, 'food', NULL, 'x', 0, 0)")
            db.execSQL("INSERT INTO expense (id, amount_minor, currency, category_id, note, occurred_at, created_at) VALUES (1, 25000, 'INR', 1, 'Lunch', 10, 10)")
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.databaseBuilder(context, ExpenseDatabase::class.java, name)
            .addMigrations(*ExpenseDatabase.MIGRATIONS)
            .build()
        try {
            val expenses = runBlocking { db.dao().allExpenses() }
            assertEquals(listOf(25_000L), expenses.map { it.amountMinor })
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
