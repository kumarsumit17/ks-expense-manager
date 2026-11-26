package com.ks.expense

import com.ks.expense.recurring.Cycle
import com.ks.expense.recurring.SubscriptionCsv
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** The contract with ks-tv-launcher (KS-Suite specs/cross-repo.md), in the exact form it writes. */
class SubscriptionCsvTest {
    private val export = "name,amount_minor,currency,cycle,next_due\r\n" +
        "Netflix,64900,INR,monthly,2026-12-05\r\n" +
        "\"Prime, yearly \"\"family\"\"\",149900,INR,yearly,2027-03-01\r\n"

    @Test fun readsTheLauncherExport() {
        val result = SubscriptionCsv.read(export)!!
        assertEquals(0, result.skipped)
        assertEquals(
            listOf(
                SubscriptionCsv.Row("Netflix", 64_900, "INR", Cycle.MONTHLY, LocalDate.of(2026, 12, 5)),
                SubscriptionCsv.Row("Prime, yearly \"family\"", 149_900, "INR", Cycle.YEARLY, LocalDate.of(2027, 3, 1)),
            ),
            result.rows,
        )
    }

    @Test fun lfRowsBomWeeklyAndLowerCaseCurrency() {
        val result = SubscriptionCsv.read("\uFEFFname,amount_minor,currency,cycle,next_due\nGym,50000,inr,weekly,2026-11-30")!!
        assertEquals(listOf(SubscriptionCsv.Row("Gym", 50_000, "INR", Cycle.WEEKLY, LocalDate.of(2026, 11, 30))), result.rows)
    }

    @Test fun badRowsAreSkippedAndCounted() {
        val text = "name,amount_minor,currency,cycle,next_due\r\n" +
            "A,100,INR,daily,2026-12-01\r\n" +
            "B,-5,INR,monthly,2026-12-01\r\n" +
            "C,12.5,INR,monthly,2026-12-01\r\n" +
            "D,100,XAU,monthly,2026-12-01\r\n" +
            "E,100,INR,monthly,1 Dec\r\n" +
            ",100,INR,monthly,2026-12-01\r\n" +
            "F,100,INR,monthly\r\n" +
            "\r\n" +
            "G,100,USD,monthly,2026-12-01\r\n"
        val result = SubscriptionCsv.read(text)!!
        assertEquals(listOf("G"), result.rows.map { it.name })
        assertEquals(7, result.skipped)
    }

    @Test fun notThisExport() {
        assertNull(SubscriptionCsv.read("date,amount,currency,category,note\r\n2026-11-26,10.00,INR,Food,\r\n"))
        assertNull(SubscriptionCsv.read(""))
        assertNull(SubscriptionCsv.read("name,amount_minor,currency,cycle,next_due\r\n\"open,1,INR,monthly,2026-12-01\r\n"))
        assertNull(SubscriptionCsv.read("x".repeat(SubscriptionCsv.MAX_CHARS + 1)))
    }
}
