package com.ks.expense

import android.content.pm.PackageManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val res get() = compose.activity.resources

    @Before fun emptyDatabase() = runBlocking {
        val dao = (compose.activity.application as ExpenseApp).store.dao
        dao.replaceAll(dao.allCategories(), emptyList())
    }

    /** Quick add: +, (type the amount), a category, Save. Three taps. */
    @Test fun quickAddInThreeTaps() {
        compose.onNodeWithContentDescription(res.getString(R.string.expense_add)).performClick()
        compose.onNodeWithTag("amount").performTextInput("1234.5")
        compose.onNodeWithText("🍛 " + res.getString(R.string.category_food)).performClick()
        compose.onNodeWithText(res.getString(R.string.common_save)).performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("₹1,234.50").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("₹1,234.50").onFirst().assertIsDisplayed()
    }

    @Test fun noInternetPermission() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        assertFalse(info.requestedPermissions.orEmpty().contains(android.Manifest.permission.INTERNET))
    }
}
