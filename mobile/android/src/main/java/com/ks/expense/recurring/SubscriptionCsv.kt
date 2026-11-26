package com.ks.expense.recurring

import com.ks.expense.transactions.Currencies
import com.ks.expense.transactions.Money
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Reads ks-tv-launcher's subscription export, the contract in KS-Suite `specs/cross-repo.md`:
 * header `name,amount_minor,currency,cycle,next_due`, RFC 4180 quoting, CRLF or LF rows.
 */
object SubscriptionCsv {
    const val HEADER = "name,amount_minor,currency,cycle,next_due"

    /** Largest text accepted from a share (about 10,000 rows). */
    const val MAX_CHARS = 1_000_000

    data class Row(val name: String, val amountMinor: Long, val currency: String, val cycle: Cycle, val nextDue: LocalDate)

    /** The readable rows and how many were skipped; null when the text is not this export at all. */
    data class Result(val rows: List<Row>, val skipped: Int)

    fun read(text: String): Result? {
        if (text.length > MAX_CHARS) return null
        val records = records(text.removePrefix("\uFEFF")) ?: return null
        val header = records.firstOrNull()?.map { it.trim().lowercase() } ?: return null
        if (header != HEADER.split(',')) return null
        var skipped = 0
        val rows = records.drop(1).filter { r -> r.any { it.isNotBlank() } }.mapNotNull { r ->
            row(r).also { if (it == null) skipped++ }
        }
        return Result(rows, skipped)
    }

    private fun row(fields: List<String>): Row? {
        if (fields.size != 5) return null
        val name = fields[0].trim().take(200)
        val amount = fields[1].trim().toLongOrNull()?.takeIf { it in 1..Money.MAX_MINOR } ?: return null
        val currency = fields[2].trim().uppercase().takeIf { Currencies.isValid(it) } ?: return null
        val cycle = Cycle.parse(fields[3]) ?: return null
        val nextDue = try {
            LocalDate.parse(fields[4].trim())
        } catch (_: DateTimeParseException) {
            return null
        }
        if (name.isEmpty()) return null
        return Row(name, amount, currency, cycle, nextDue)
    }

    /** RFC 4180 records; null when a quoted field never closes. */
    internal fun records(text: String): List<List<String>>? {
        val records = mutableListOf<List<String>>()
        var fields = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        field.append('"')
                        i++
                    } else {
                        quoted = false
                    }
                } else {
                    field.append(c)
                }
            } else {
                when (c) {
                    '"' -> quoted = true
                    ',' -> {
                        fields += field.toString()
                        field.clear()
                    }
                    '\r' -> Unit
                    '\n' -> {
                        fields += field.toString()
                        field.clear()
                        records += fields
                        fields = mutableListOf()
                    }
                    else -> field.append(c)
                }
            }
            i++
        }
        if (quoted) return null
        if (field.isNotEmpty() || fields.isNotEmpty()) {
            fields += field.toString()
            records += fields
        }
        return records
    }
}
