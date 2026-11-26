package com.ks.expense.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A category. A built-in one has [builtinKey] (its name comes from the language files, so it
 * follows the app language) until the user renames it, which sets [name].
 */
@Entity(tableName = "category")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "builtin_key") val builtinKey: String?,
    val name: String?,
    val icon: String,
    val hidden: Boolean = false,
    val sort: Int,
)

/** One expense: [amountMinor] in minor units of [currency]; [occurredAt] is UTC epoch millis. */
@Entity(
    tableName = "expense",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("category_id"), Index("occurred_at")],
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    val currency: String,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    val note: String,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

/** The starting categories: key (language file `category.<key>`) and icon. */
object DefaultCategories {
    val list = listOf(
        "food" to "🍛",
        "transport" to "🚌",
        "shopping" to "🛍️",
        "bills" to "🧾",
        "health" to "💊",
        "entertainment" to "🎬",
        "other" to "📦",
    )

    /** Icons offered for a new category. */
    val icons = listOf("🍛", "☕", "🚌", "🛍️", "🧾", "💊", "🎬", "📦", "🏠", "⛽", "📚", "🎁", "✈️", "👕", "📱", "🐾", "💡")
}

/**
 * A standing monthly limit: for one category, or for the whole month when [categoryId] is null.
 * Compared only with expenses in the same [currency]. At most one per category and one overall.
 */
@Entity(
    tableName = "budget",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["category_id"], unique = true)],
)
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "category_id") val categoryId: Long?,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    val currency: String,
)

/**
 * A recurring expense: on [nextDue] (a local date as epoch day) an expense is added and the date
 * moves on by [cycle] (`weekly`, `monthly`, `yearly`), keeping [anchorDay] for months.
 */
@Entity(
    tableName = "recurring_rule",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("category_id")],
)
data class RecurringRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    val currency: String,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    val note: String,
    val cycle: String,
    @ColumnInfo(name = "anchor_day") val anchorDay: Int,
    @ColumnInfo(name = "next_due") val nextDue: Long,
    val active: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
