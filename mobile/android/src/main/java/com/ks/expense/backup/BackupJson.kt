package com.ks.expense.backup

import com.ks.expense.data.Budget
import com.ks.expense.data.Category
import com.ks.expense.data.Expense
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** The backup file: JSON written and read with Android's built-in org.json. */
object BackupJson {
    const val FORMAT = "ks-expense-backup"
    /** 2 added budgets, 3 the default currency; older files still read. */
    const val VERSION = 3

    fun write(data: BackupData, createdAt: Long): String {
        val categories = JSONArray()
        data.categories.forEach {
            categories.put(
                JSONObject()
                    .put("id", it.id)
                    .put("builtinKey", it.builtinKey ?: JSONObject.NULL)
                    .put("name", it.name ?: JSONObject.NULL)
                    .put("icon", it.icon)
                    .put("hidden", it.hidden)
                    .put("sort", it.sort),
            )
        }
        val expenses = JSONArray()
        data.expenses.forEach {
            expenses.put(
                JSONObject()
                    .put("id", it.id)
                    .put("amountMinor", it.amountMinor)
                    .put("currency", it.currency)
                    .put("categoryId", it.categoryId)
                    .put("note", it.note)
                    .put("occurredAt", it.occurredAt)
                    .put("createdAt", it.createdAt),
            )
        }
        val budgets = JSONArray()
        data.budgets.forEach {
            budgets.put(
                JSONObject()
                    .put("id", it.id)
                    .put("categoryId", it.categoryId ?: JSONObject.NULL)
                    .put("amountMinor", it.amountMinor)
                    .put("currency", it.currency),
            )
        }
        return JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("createdAt", createdAt)
            .put("categories", categories)
            .put("expenses", expenses)
            .put("budgets", budgets)
            .put("settings", JSONObject().put("defaultCurrency", data.defaultCurrency ?: JSONObject.NULL))
            .toString(1)
    }

    /** The backup's data, or null when the text is not a valid backup this version can read. */
    fun read(text: String): BackupData? = try {
        val root = JSONObject(text)
        if (root.getString("format") != FORMAT || root.getInt("version") !in 1..VERSION) {
            null
        } else {
            val categories = root.getJSONArray("categories").objects().map {
                Category(
                    id = it.strictLong("id"),
                    builtinKey = it.optStringOrNull("builtinKey"),
                    name = it.optStringOrNull("name"),
                    icon = it.getString("icon"),
                    hidden = it.getBoolean("hidden"),
                    sort = it.getInt("sort"),
                )
            }
            val expenses = root.getJSONArray("expenses").objects().map {
                Expense(
                    id = it.strictLong("id"),
                    amountMinor = it.strictLong("amountMinor"),
                    currency = it.getString("currency"),
                    categoryId = it.strictLong("categoryId"),
                    note = it.getString("note"),
                    occurredAt = it.strictLong("occurredAt"),
                    createdAt = it.strictLong("createdAt"),
                )
            }
            val budgets = if (root.has("budgets")) {
                root.getJSONArray("budgets").objects().map {
                    Budget(
                        id = it.strictLong("id"),
                        categoryId = if (it.isNull("categoryId")) null else it.strictLong("categoryId"),
                        amountMinor = it.strictLong("amountMinor"),
                        currency = it.getString("currency"),
                    )
                }
            } else {
                emptyList()
            }
            val defaultCurrency = root.optJSONObject("settings")?.let { if (it.isNull("defaultCurrency")) null else it.getString("defaultCurrency") }
            BackupData(categories, expenses, budgets, defaultCurrency).takeIf { it.isValid() }
        }
    } catch (_: JSONException) {
        null
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    /** A whole number only: org.json's getLong would turn 1.5 into 1. */
    private fun JSONObject.strictLong(key: String): Long = when (val value = get(key)) {
        is Int -> value.toLong()
        is Long -> value
        else -> throw JSONException("$key is not a whole number")
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else getString(key)
}
