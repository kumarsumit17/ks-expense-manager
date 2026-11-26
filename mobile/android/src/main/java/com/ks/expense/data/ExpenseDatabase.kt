package com.ks.expense.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The app's database. Every schema change raises [version], exports the new schema to
 * `mobile/android/schemas/` and adds a [Migration] to [MIGRATIONS] with a migration test.
 */
@Database(entities = [Expense::class, Category::class, Budget::class, RecurringRule::class], version = 3, exportSchema = true)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun dao(): ExpenseDao

    companion object {
        const val NAME = "expenses.db"

        /** Version 2 (stage 2): monthly budgets. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `budget` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`category_id` INTEGER, `amount_minor` INTEGER NOT NULL, `currency` TEXT NOT NULL, " +
                        "FOREIGN KEY(`category_id`) REFERENCES `category`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_budget_category_id` ON `budget` (`category_id`)")
            }
        }

        /** Version 3 (stage 3): recurring expenses. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `recurring_rule` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`amount_minor` INTEGER NOT NULL, `currency` TEXT NOT NULL, `category_id` INTEGER NOT NULL, " +
                        "`note` TEXT NOT NULL, `cycle` TEXT NOT NULL, `anchor_day` INTEGER NOT NULL, `next_due` INTEGER NOT NULL, " +
                        "`active` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`category_id`) REFERENCES `category`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_rule_category_id` ON `recurring_rule` (`category_id`)")
            }
        }

        /** Migrations from each older version, in order. */
        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)

        /** [name] null opens an in-memory database (tests). */
        fun build(context: Context, name: String? = NAME): ExpenseDatabase {
            val builder = if (name == null) {
                Room.inMemoryDatabaseBuilder(context, ExpenseDatabase::class.java)
            } else {
                Room.databaseBuilder(context, ExpenseDatabase::class.java, name)
            }
            return builder.addMigrations(*MIGRATIONS).addCallback(SeedCategories).build()
        }
    }
}

/** Puts the starting categories into a new database. */
object SeedCategories : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        DefaultCategories.list.forEachIndexed { index, (key, icon) ->
            db.execSQL(
                "INSERT INTO category (builtin_key, name, icon, hidden, sort) VALUES (?, NULL, ?, 0, ?)",
                arrayOf<Any>(key, icon, index),
            )
        }
    }
}
