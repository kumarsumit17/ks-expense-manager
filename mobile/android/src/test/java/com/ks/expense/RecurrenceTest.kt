package com.ks.expense

import com.ks.expense.recurring.Cycle
import com.ks.expense.recurring.Recurrence
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RecurrenceTest {
    private fun d(s: String) = LocalDate.parse(s)

    @Test fun monthlyKeepsTheDayOfTheMonth() {
        assertEquals(d("2027-02-28"), Recurrence.next(d("2027-01-31"), Cycle.MONTHLY, 31))
        assertEquals(d("2027-03-31"), Recurrence.next(d("2027-02-28"), Cycle.MONTHLY, 31))
        assertEquals(d("2028-02-29"), Recurrence.next(d("2028-01-30"), Cycle.MONTHLY, 30))
        assertEquals(d("2026-12-15"), Recurrence.next(d("2026-11-15"), Cycle.MONTHLY, 15))
    }

    @Test fun yearlyAndWeekly() {
        assertEquals(d("2029-02-28"), Recurrence.next(d("2028-02-29"), Cycle.YEARLY, 29))
        assertEquals(d("2032-02-29"), Recurrence.next(d("2031-02-28"), Cycle.YEARLY, 29))
        assertEquals(d("2026-12-03"), Recurrence.next(d("2026-11-26"), Cycle.WEEKLY, 26))
    }

    @Test fun dueDatesUpToToday() {
        val (dates, next) = Recurrence.due(d("2026-09-30"), Cycle.MONTHLY, 30, d("2026-11-30"))
        assertEquals(listOf(d("2026-09-30"), d("2026-10-30"), d("2026-11-30")), dates)
        assertEquals(d("2026-12-30"), next)
        val (none, same) = Recurrence.due(d("2026-12-01"), Cycle.MONTHLY, 1, d("2026-11-26"))
        assertEquals(emptyList<LocalDate>(), none)
        assertEquals(d("2026-12-01"), same)
    }

    @Test fun capStopsAFloodButMovesThePast() {
        val (dates, next) = Recurrence.due(d("2000-01-01"), Cycle.WEEKLY, 1, d("2026-11-26"))
        assertEquals(Recurrence.MAX_AT_ONCE, dates.size)
        assertEquals(true, next.isAfter(d("2026-11-26")))
    }

    @Test fun rollForwardForImports() {
        assertEquals(d("2026-12-05"), Recurrence.rollForward(d("2026-10-05"), Cycle.MONTHLY, 5, d("2026-11-26")))
        assertEquals(d("2026-11-26"), Recurrence.rollForward(d("2026-11-26"), Cycle.MONTHLY, 26, d("2026-11-26")))
        assertEquals(d("2027-01-10"), Recurrence.rollForward(d("2027-01-10"), Cycle.YEARLY, 10, d("2026-11-26")))
    }

    @Test fun cycleKeys() {
        assertEquals(Cycle.MONTHLY, Cycle.parse(" Monthly "))
        assertEquals(null, Cycle.parse("daily"))
    }
}
