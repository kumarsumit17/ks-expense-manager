package com.ks.expense.recurring

import java.time.LocalDate

/** How often a recurring expense repeats. [key] is its name in the database and in files. */
enum class Cycle(val key: String) {
    WEEKLY("weekly"),
    MONTHLY("monthly"),
    YEARLY("yearly"),
    ;

    companion object {
        fun parse(key: String): Cycle? = entries.firstOrNull { it.key == key.trim().lowercase() }
    }
}

/** Due dates of recurring expenses. A rule keeps the day of the month it started on ([anchorDay]). */
object Recurrence {
    /** Most expenses one rule adds at once (about five years of a monthly rule), so a bad date never floods the app. */
    const val MAX_AT_ONCE = 60

    /**
     * The date after [date]. Monthly and yearly rules go back to [anchorDay] when the month allows,
     * so a rule started on the 31st falls on 28 or 29 February and on 31 March again.
     */
    fun next(date: LocalDate, cycle: Cycle, anchorDay: Int): LocalDate = when (cycle) {
        Cycle.WEEKLY -> date.plusWeeks(1)
        Cycle.MONTHLY -> date.plusMonths(1).let { it.withDayOfMonth(minOf(anchorDay, it.lengthOfMonth())) }
        Cycle.YEARLY -> date.plusYears(1).let { it.withDayOfMonth(minOf(anchorDay, it.lengthOfMonth())) }
    }

    /** The dates due on or before [today], starting at [nextDue], and the next date after them. */
    fun due(nextDue: LocalDate, cycle: Cycle, anchorDay: Int, today: LocalDate): Pair<List<LocalDate>, LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var d = nextDue
        while (!d.isAfter(today) && dates.size < MAX_AT_ONCE) {
            dates += d
            d = next(d, cycle, anchorDay)
        }
        // After the cap, skip ahead without adding, so the rule is never left in the past.
        while (!d.isAfter(today)) d = next(d, cycle, anchorDay)
        return dates to d
    }

    /** The first date on or after [today] in the rule's series starting at [date] (used when importing). */
    fun rollForward(date: LocalDate, cycle: Cycle, anchorDay: Int, today: LocalDate): LocalDate {
        var d = date
        while (d.isBefore(today)) d = next(d, cycle, anchorDay)
        return d
    }
}
