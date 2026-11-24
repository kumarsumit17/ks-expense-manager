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
@Database(entities = [Expense::class, Category::class], version = 1, exportSchema = true)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun dao(): ExpenseDao

    companion object {
        const val NAME = "expenses.db"

        /** Migrations from each older version; none yet (version 1 is the first). */
        val MIGRATIONS: Array<Migration> = arrayOf()

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
