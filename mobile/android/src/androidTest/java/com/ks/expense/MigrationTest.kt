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

/** Every exported schema migrates to the current one and keeps its data. */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val name = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), ExpenseDatabase::class.java)

    @Test fun version2To3AddsRecurringRules() {
        helper.createDatabase(name, 2).use { db ->
            db.execSQL("INSERT INTO category (id, builtin_key, name, icon, hidden, sort) VALUES (1, 'bills', NULL, 'x', 0, 0)")
            db.execSQL("INSERT INTO budget (id, category_id, amount_minor, currency) VALUES (1, NULL, 1000000, 'INR')")
        }
        helper.runMigrationsAndValidate(name, 3, true, ExpenseDatabase.MIGRATION_2_3).use { db ->
            db.query("SELECT COUNT(*) FROM budget").use { c ->
                c.moveToFirst()
                assertEquals(1, c.getInt(0))
            }
        }
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(name)
    }

    @Test fun version1To2KeepsDataAndAddsBudgets() {
        helper.createDatabase(name, 1).use { db ->
            db.execSQL("INSERT INTO category (id, builtin_key, name, icon, hidden, sort) VALUES (1, 'food', NULL, 'x', 0, 0)")
            db.execSQL("INSERT INTO expense (id, amount_minor, currency, category_id, note, occurred_at, created_at) VALUES (1, 25000, 'INR', 1, 'Lunch', 10, 10)")
        }
        helper.runMigrationsAndValidate(name, 3, true, *ExpenseDatabase.MIGRATIONS).close()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.databaseBuilder(context, ExpenseDatabase::class.java, name)
            .addMigrations(*ExpenseDatabase.MIGRATIONS)
            .build()
        try {
            runBlocking {
                assertEquals(listOf(25_000L), db.dao().allExpenses().map { it.amountMinor })
                db.dao().setBudget(1, 100_000, "INR")
                assertEquals(listOf(100_000L), db.dao().allBudgets().map { it.amountMinor })
            }
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
