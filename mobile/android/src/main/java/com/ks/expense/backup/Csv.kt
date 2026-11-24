package com.ks.expense.backup

import com.ks.expense.transactions.Money
import java.time.LocalDate

/** The CSV export, for spreadsheets (RFC 4180, UTF-8 with a byte-order mark so Excel reads Hindi). */
object Csv {
    const val HEADER = "date,amount,currency,category,note"

    data class Row(val date: LocalDate, val amountMinor: Long, val currency: String, val category: String, val note: String)

    fun write(rows: List<Row>, out: Appendable) {
        out.append('\uFEFF').append(HEADER).append("\r\n")
        for (row in rows) {
            out.append(row.date.toString()).append(',')
                .append(Money.toPlain(row.amountMinor, row.currency)).append(',')
                .append(row.currency).append(',')
                .append(field(row.category)).append(',')
                .append(field(row.note)).append("\r\n")
        }
    }

    /**
     * Quotes a text field when needed. A field that a spreadsheet would read as a formula
     * (starting with =, +, -, @, tab or carriage return) gets a leading apostrophe.
     */
    fun field(value: String): String {
        val safe = if (value.isNotEmpty() && value[0] in "=+-@\t\r") "'$value" else value
        val quote = safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' } ||
            safe.startsWith(' ') || safe.endsWith(' ')
        return if (quote) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }
}
