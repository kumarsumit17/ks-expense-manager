package com.ks.expense

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.core.content.pm.ShortcutManagerCompat
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShortcutTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /** The launcher shortcut opens the add sheet with the amount field ready. */
    @Test fun addExpenseShortcutOpensTheSheet() {
        val intent = Intent(context, MainActivity::class.java).setAction(MainActivity.ACTION_ADD_EXPENSE)
        ActivityScenario.launch<MainActivity>(intent).use {
            compose.waitUntil(5_000) { compose.onAllNodes(androidx.compose.ui.test.hasTestTag("amount")).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("amount").assertIsDisplayed()
        }
    }

    @Test fun shortcutIsPublished() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val shortcuts = ShortcutManagerCompat.getDynamicShortcuts(context)
            assertEquals(listOf("add_expense"), shortcuts.map { it.id })
        }
    }
}
