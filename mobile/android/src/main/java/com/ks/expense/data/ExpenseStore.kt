package com.ks.expense.data

import android.content.Context
import android.net.Uri
import com.ks.expense.backup.BackupData
import com.ks.expense.backup.BackupJson
import com.ks.expense.backup.Csv
import com.ks.expense.recurring.Cycle
import com.ks.expense.recurring.Recurrence
import com.ks.expense.recurring.SubscriptionCsv
import com.ks.expense.reports.Months
import com.ks.expense.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId

/** The database plus the file work (export, backup, restore) through the Storage Access Framework. */
class ExpenseStore(private val context: Context, val database: ExpenseDatabase, val settings: AppSettings) {
    val dao: ExpenseDao get() = database.dao()

    /** Writes every expense as CSV to [uri]; returns how many. */
    suspend fun exportCsv(uri: Uri, zone: ZoneId, label: (Category) -> String): Int {
        val categories = dao.allCategories().associateBy { it.id }
        val expenses = dao.allExpenses()
        val rows = expenses.map {
            Csv.Row(
                date = Months.localDate(it.occurredAt, zone),
                amountMinor = it.amountMinor,
                currency = it.currency,
                category = categories[it.categoryId]?.let(label).orEmpty(),
                note = it.note,
            )
        }
        val text = StringBuilder().also { Csv.write(rows, it) }.toString()
        writeText(uri, text)
        return rows.size
    }

    /** Writes the backup file to [uri]; returns how many expenses it holds. */
    suspend fun backup(uri: Uri): Int {
        val data = BackupData(dao.allCategories(), dao.allExpenses(), dao.allBudgets(), settings.defaultCurrency.value, dao.allRecurringRules())
        writeText(uri, BackupJson.write(data, System.currentTimeMillis()))
        return data.expenses.size
    }

    /** Reads and checks a backup file; null when it is not a valid backup. Changes nothing. */
    suspend fun readBackup(uri: Uri): BackupData? = withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = input.readNBytesCompat(MAX_BACKUP_BYTES + 1)
                if (buffer.size > MAX_BACKUP_BYTES) null else buffer
            } ?: return@withContext null
            BackupJson.read(bytes.toString(Charsets.UTF_8))
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }

    /** Adds a new expense; with [repeat], also a recurring rule whose next date is one cycle later. */
    suspend fun addExpense(expense: Expense, repeat: Cycle?, zone: ZoneId) {
        dao.insert(expense)
        if (repeat != null) {
            val date = Months.localDate(expense.occurredAt, zone)
            dao.insertRule(
                RecurringRule(
                    amountMinor = expense.amountMinor,
                    currency = expense.currency,
                    categoryId = expense.categoryId,
                    note = expense.note,
                    cycle = repeat.key,
                    anchorDay = date.dayOfMonth,
                    nextDue = Recurrence.next(date, repeat, date.dayOfMonth).toEpochDay(),
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    /** Recurring rules from ks-tv-launcher's subscriptions, each starting on its next date (never in the past). */
    suspend fun importSubscriptions(rows: List<SubscriptionCsv.Row>, categoryId: Long, today: LocalDate): Int {
        val now = System.currentTimeMillis()
        rows.forEach { row ->
            val anchor = row.nextDue.dayOfMonth
            dao.insertRule(
                RecurringRule(
                    amountMinor = row.amountMinor,
                    currency = row.currency,
                    categoryId = categoryId,
                    note = row.name,
                    cycle = row.cycle.key,
                    anchorDay = anchor,
                    nextDue = Recurrence.rollForward(row.nextDue, row.cycle, anchor, today).toEpochDay(),
                    createdAt = now,
                ),
            )
        }
        return rows.size
    }

    /** Reads a shared CSV file (when the share has no text); null when it cannot be read or is too large. */
    suspend fun readSharedText(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val bytes = input.readNBytesCompat(SubscriptionCsv.MAX_CHARS * 4 + 1)
                if (bytes.size > SubscriptionCsv.MAX_CHARS * 4) null else bytes.toString(Charsets.UTF_8)
            }
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }

    suspend fun restore(data: BackupData) {
        dao.replaceAll(data.categories, data.expenses, data.budgets, data.rules)
        data.defaultCurrency?.let { settings.setDefaultCurrency(it) }
    }

    private suspend fun writeText(uri: Uri, text: String) = withContext(Dispatchers.IO) {
        val out = context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("No output stream")
        out.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    }

    private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(64 * 1024)
        while (out.size() < limit) {
            val n = read(chunk, 0, minOf(chunk.size, limit - out.size()))
            if (n < 0) break
            out.write(chunk, 0, n)
        }
        return out.toByteArray()
    }

    companion object {
        /** A backup larger than this is refused (about a million expenses). */
        const val MAX_BACKUP_BYTES = 256 * 1024 * 1024
    }
}
