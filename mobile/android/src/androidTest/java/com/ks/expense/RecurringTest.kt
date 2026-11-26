package com.ks.expense

import android.content.Intent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ks.expense.data.Expense
import com.ks.expense.data.ExpenseDatabase
import com.ks.expense.data.RecurringRule
import com.ks.expense.recurring.Cycle
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class RecurringTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val zone = ZoneId.of("Asia/Kolkata")

    @Test fun dueRulesAddExpensesOnceAndMoveOn() = runBlocking {
        val db = ExpenseDatabase.build(context, name = null)
        try {
            val dao = db.dao()
            val bills = dao.allCategories().first { it.builtinKey == "bills" }.id
            val today = LocalDate.of(2026, 11, 26)
            dao.insertRule(RecurringRule(amountMinor = 1_500_000, currency = "INR", categoryId = bills, note = "Rent", cycle = "monthly", anchorDay = 5, nextDue = LocalDate.of(2026, 10, 5).toEpochDay(), createdAt = 1))
            dao.insertRule(RecurringRule(amountMinor = 100, currency = "INR", categoryId = bills, note = "Paused", cycle = "weekly", anchorDay = 1, nextDue = LocalDate.of(2026, 11, 1).toEpochDay(), active = false, createdAt = 1))
            assertEquals(2, dao.applyDueRules(today, zone, 0))
            assertEquals(0, dao.applyDueRules(today, zone, 0))
            val added = dao.allExpenses()
            assertEquals(listOf("2026-10-05", "2026-11-05"), added.map { java.time.Instant.ofEpochMilli(it.occurredAt).atZone(zone).toLocalDate().toString() })
            assertEquals(LocalDate.of(2026, 12, 5).toEpochDay(), dao.allRecurringRules().first { it.note == "Rent" }.nextDue)
        } finally {
            db.close()
        }
    }

    @Test fun addingWithRepeatCreatesARule() = runBlocking {
        val app = context.applicationContext as ExpenseApp
        val dao = app.store.dao
        dao.replaceAll(dao.allCategories(), emptyList())
        val food = dao.allCategories().first().id
        val at = LocalDate.of(2026, 1, 31).atTime(10, 0).atZone(zone).toInstant().toEpochMilli()
        app.store.addExpense(Expense(amountMinor = 500, currency = "INR", categoryId = food, note = "Gym", occurredAt = at, createdAt = at), Cycle.MONTHLY, zone)
        val rule = dao.allRecurringRules().single()
        assertEquals(31, rule.anchorDay)
        assertEquals(LocalDate.of(2026, 2, 28).toEpochDay(), rule.nextDue)
        dao.replaceAll(dao.allCategories(), emptyList())
    }

    /** ks-tv-launcher shares its CSV; the dialog lists the rows and adds them as recurring expenses. */
    @Test fun sharedSubscriptionsBecomeRecurringExpenses() {
        val app = context.applicationContext as ExpenseApp
        runBlocking { app.store.dao.replaceAll(app.store.dao.allCategories(), emptyList()) }
        val next = LocalDate.now().plusDays(10)
        val csv = "name,amount_minor,currency,cycle,next_due\r\nNetflix,64900,INR,monthly,$next\r\nGym,50000,INR,yearly,$next\r\n"
        val intent = Intent(context, MainActivity::class.java).setAction(Intent.ACTION_SEND).setType("text/csv").putExtra(Intent.EXTRA_TEXT, csv)
        ActivityScenario.launch<MainActivity>(intent).use {
            val res = context.resources
            val add = res.getQuantityString(R.plurals.import_button, 2, 2)
            compose.waitUntil(10_000) { compose.onAllNodes(androidx.compose.ui.test.hasText(add)).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Netflix").performClick()
            compose.onNodeWithText(res.getQuantityString(R.plurals.import_button, 1, 1)).performClick()
            compose.waitUntil(5_000) { runBlocking { app.store.dao.allRecurringRules().isNotEmpty() } }
            val rules = runBlocking { app.store.dao.allRecurringRules() }
            assertEquals(listOf("Gym"), rules.map { it.note })
            assertEquals("yearly", rules.single().cycle)
            assertEquals(next.toEpochDay(), rules.single().nextDue)
        }
        runBlocking { app.store.dao.replaceAll(app.store.dao.allCategories(), emptyList()) }
    }
}
