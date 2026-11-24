package com.ks.expense

import com.ks.expense.backup.Csv
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CsvTest {
    @Test fun writesHeaderBomAndPlainAmounts() {
        val out = StringBuilder()
        Csv.write(
            listOf(
                Csv.Row(LocalDate.of(2026, 11, 24), 12_345_650, "INR", "Food", "Lunch"),
                Csv.Row(LocalDate.of(2026, 11, 25), 500, "JPY", "खाना", ""),
            ),
            out,
        )
        assertEquals(
            "\uFEFFdate,amount,currency,category,note\r\n" +
                "2026-11-24,123456.50,INR,Food,Lunch\r\n" +
                "2026-11-25,500,JPY,खाना,\r\n",
            out.toString(),
        )
    }

    @Test fun quotesWhenNeeded() {
        assertEquals("plain", Csv.field("plain"))
        assertEquals("\"a,b\"", Csv.field("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", Csv.field("say \"hi\""))
        assertEquals("\"two\nlines\"", Csv.field("two\nlines"))
        assertEquals("\" padded \"", Csv.field(" padded "))
    }

    @Test fun neverAFormula() {
        assertEquals("'=SUM(A1)", Csv.field("=SUM(A1)"))
        assertEquals("'+91 phone", Csv.field("+91 phone"))
        assertEquals("'-5", Csv.field("-5"))
        assertEquals("'@x", Csv.field("@x"))
        assertEquals("\"'=1,2\"", Csv.field("=1,2"))
    }
}
