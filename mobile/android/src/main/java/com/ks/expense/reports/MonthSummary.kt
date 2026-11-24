package com.ks.expense.reports

import com.ks.expense.data.Expense
import com.ks.expense.transactions.Money
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** The expenses of one day, newest first, with that day's total per currency. */
data class DayGroup(val date: LocalDate, val expenses: List<Expense>, val totals: List<Total>)

/** A sum of minor units in one currency. */
data class Total(val currency: String, val amountMinor: Long)

/** One category's spending in one currency, and its share (0..1) of that currency's month total. */
data class CategoryTotal(val categoryId: Long, val currency: String, val amountMinor: Long, val share: Float)

data class MonthSummary(
    val month: YearMonth,
    val totals: List<Total>,
    val categories: List<CategoryTotal>,
    val days: List<DayGroup>,
    val count: Int,
)

object Months {
    /** The month as UTC millis [from, to) in [zone]. */
    fun range(month: YearMonth, zone: ZoneId): Pair<Long, Long> =
        month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() to
            month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

    fun localDate(epochMillis: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    /**
     * Groups [expenses] (already limited to [month]) by day in [zone]. Totals are per currency,
     * the default currency first; nothing is converted.
     */
    fun summarize(month: YearMonth, expenses: List<Expense>, zone: ZoneId): MonthSummary {
        val sorted = expenses.sortedWith(compareByDescending<Expense> { it.occurredAt }.thenByDescending { it.id })
        val days = sorted.groupBy { localDate(it.occurredAt, zone) }
            .map { (date, list) -> DayGroup(date, list, totals(list)) }
            .sortedByDescending { it.date }
        val totals = totals(sorted)
        val byCurrency = totals.associate { it.currency to it.amountMinor }
        val categories = sorted.groupBy { it.categoryId to it.currency }
            .map { (key, list) ->
                val sum = sum(list)
                val total = byCurrency.getValue(key.second)
                CategoryTotal(key.first, key.second, sum, if (total == 0L) 0f else (sum.toDouble() / total).toFloat())
            }
            .sortedWith(compareBy<CategoryTotal> { currencyOrder(it.currency) }.thenByDescending { it.amountMinor })
        return MonthSummary(month, totals, categories, days, sorted.size)
    }

    private fun totals(list: List<Expense>): List<Total> =
        list.groupBy { it.currency }
            .map { (currency, items) -> Total(currency, sum(items)) }
            .sortedBy { currencyOrder(it.currency) }

    private fun sum(list: List<Expense>): Long = list.fold(0L) { acc, e -> Math.addExact(acc, e.amountMinor) }

    private fun currencyOrder(currency: String): String =
        if (currency == Money.DEFAULT_CURRENCY) "" else currency
}
