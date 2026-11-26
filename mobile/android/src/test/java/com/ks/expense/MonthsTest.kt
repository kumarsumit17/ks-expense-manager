package com.ks.expense

import com.ks.expense.data.Expense
import com.ks.expense.reports.Months
import com.ks.expense.reports.Total
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime

class MonthsTest {
    private val ist = ZoneId.of("Asia/Kolkata")

    private fun at(text: String, zone: ZoneId = ist) = ZonedDateTime.of(java.time.LocalDateTime.parse(text), zone).toInstant().toEpochMilli()

    private fun expense(id: Long, amount: Long, time: Long, category: Long = 1, currency: String = "INR") =
        Expense(id, amount, currency, category, "", time, time)

    @Test fun monthRangeIsInTheUsersZone() {
        val (from, to) = Months.range(YearMonth.of(2026, 11), ist)
        assertEquals(at("2026-11-01T00:00"), from)
        assertEquals(at("2026-12-01T00:00"), to)
        // 1 Nov 00:00 in India is 31 Oct 18:30 UTC.
        assertEquals(at("2026-10-31T18:30", ZoneId.of("UTC")), from)
    }

    @Test fun februaryAndYearEnd() {
        val (from, to) = Months.range(YearMonth.of(2028, 2), ist)
        assertEquals(29L * 24 * 3600 * 1000, to - from)
        assertEquals(at("2027-01-01T00:00"), Months.range(YearMonth.of(2026, 12), ist).second)
    }

    @Test fun groupsByDayInTheZone() {
        // 23:59 and 00:01 IST are on different days, though only two minutes apart.
        val late = expense(1, 100, at("2026-11-23T23:59"))
        val early = expense(2, 200, at("2026-11-24T00:01"))
        val summary = Months.summarize(YearMonth.of(2026, 11), listOf(late, early), ist)
        assertEquals(listOf(LocalDate.of(2026, 11, 24), LocalDate.of(2026, 11, 23)), summary.days.map { it.date })
        assertEquals(listOf(Total("INR", 200)), summary.days[0].totals)
        // In UTC both are on 23 November.
        val utc = Months.summarize(YearMonth.of(2026, 11), listOf(late, early), ZoneId.of("UTC"))
        assertEquals(1, utc.days.size)
    }

    @Test fun totalsPerCurrencyAndCategoryShares() {
        val list = listOf(
            expense(1, 30_000, at("2026-11-02T10:00"), category = 1),
            expense(2, 10_000, at("2026-11-03T10:00"), category = 2),
            expense(3, 10_000, at("2026-11-04T10:00"), category = 1),
            expense(4, 500, at("2026-11-04T11:00"), category = 1, currency = "USD"),
        )
        val summary = Months.summarize(YearMonth.of(2026, 11), list, ist)
        assertEquals(listOf(Total("INR", 50_000), Total("USD", 500)), summary.totals)
        // With dollars as the default currency, dollars come first.
        assertEquals(listOf("USD", "INR"), Months.summarize(YearMonth.of(2026, 11), list, ist, first = "USD").totals.map { it.currency })
        assertEquals(4, summary.count)
        val inr = summary.categories.filter { it.currency == "INR" }
        assertEquals(listOf(1L, 2L), inr.map { it.categoryId })
        assertEquals(0.8f, inr[0].share, 0.0001f)
        assertEquals(0.2f, inr[1].share, 0.0001f)
        assertEquals("USD", summary.categories.last().currency)
        assertEquals(1f, summary.categories.last().share, 0.0001f)
    }

    @Test fun newestFirstWithinADay() {
        val a = expense(1, 100, at("2026-11-05T09:00"))
        val b = expense(2, 100, at("2026-11-05T18:00"))
        val summary = Months.summarize(YearMonth.of(2026, 11), listOf(a, b), ist)
        assertEquals(listOf(2L, 1L), summary.days.single().expenses.map { it.id })
    }

    @Test fun daysAcrossMonthsNewestFirst() {
        val oct = expense(1, 100, at("2026-10-31T10:00"))
        val nov = expense(2, 200, at("2026-11-01T10:00"))
        val days = Months.days(listOf(oct, nov), ist)
        assertEquals(listOf(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 10, 31)), days.map { it.date })
        assertEquals(listOf(Total("INR", 300)), Months.totals(listOf(oct, nov)))
    }

    @Test fun emptyMonth() {
        val summary = Months.summarize(YearMonth.of(2026, 11), emptyList(), ist)
        assertEquals(0, summary.count)
        assertEquals(emptyList<Total>(), summary.totals)
    }

    @Test(expected = ArithmeticException::class)
    fun overflowIsAnErrorNotAWrongTotal() {
        val list = (1..10).map { expense(it.toLong(), Long.MAX_VALUE / 5, at("2026-11-05T09:00")) }
        Months.summarize(YearMonth.of(2026, 11), list, ist)
    }
}
