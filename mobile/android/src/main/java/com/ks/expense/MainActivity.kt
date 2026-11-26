package com.ks.expense

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.ks.expense.settings.SettingsScreen
import com.ks.expense.ui.KsTheme
import com.ks.expense.ui.MonthScreen
import com.ks.expense.ui.SearchScreen

/** AppCompat activity: it applies the per-app language on Android 8 to 12. */
class MainActivity : AppCompatActivity() {
    /** Set by the launcher shortcut "Add expense"; cleared once the month screen has opened the sheet. */
    private var pendingAdd by mutableStateOf(false)

    /**
     * Android 8 to 12: cover the content while the app is paused, so the Recents thumbnail shows no
     * amounts (Android 13+ uses setRecentsScreenshotEnabled). Not in split screen, where the app stays
     * visible while paused. Recents opened straight from the app shows the live screen on every
     * version; only FLAG_SECURE would hide that, and it would also block the user's screenshots.
     */
    private var covered by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) setRecentsScreenshotEnabled(false)
        if (savedInstanceState == null) handle(intent)
        publishShortcut()
        val store = (application as ExpenseApp).store
        setContent {
            KsTheme {
                var screen by rememberSaveable { mutableStateOf(Screen.MONTH) }
                if (pendingAdd) screen = Screen.MONTH
                when (screen) {
                    Screen.SETTINGS -> {
                        BackHandler { screen = Screen.MONTH }
                        SettingsScreen(store, onBack = { screen = Screen.MONTH })
                    }
                    Screen.SEARCH -> {
                        BackHandler { screen = Screen.MONTH }
                        SearchScreen(store, onBack = { screen = Screen.MONTH })
                    }
                    Screen.MONTH -> MonthScreen(
                        store,
                        openAdd = pendingAdd,
                        onAddOpened = { pendingAdd = false },
                        onSearch = { screen = Screen.SEARCH },
                        onSettings = { screen = Screen.SETTINGS },
                    )
                }
                if (covered) Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    override fun onPause() {
        super.onPause()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) covered = !isInMultiWindowMode
    }

    override fun onResume() {
        super.onResume()
        covered = false
    }

    private fun handle(intent: Intent?) {
        if (intent?.action == ACTION_ADD_EXPENSE) {
            pendingAdd = true
            ShortcutManagerCompat.reportShortcutUsed(this, "add_expense")
        }
    }

    /** The launcher's long-press "Add expense", in the current language. */
    private fun publishShortcut() {
        val shortcut = ShortcutInfoCompat.Builder(this, "add_expense")
            .setShortLabel(getString(R.string.expense_add))
            .setIcon(IconCompat.createWithResource(this, R.drawable.ic_shortcut_add))
            .setIntent(Intent(this, MainActivity::class.java).setAction(ACTION_ADD_EXPENSE))
            .build()
        runCatching { ShortcutManagerCompat.setDynamicShortcuts(this, listOf(shortcut)) }
    }

    private enum class Screen { MONTH, SEARCH, SETTINGS }

    companion object {
        const val ACTION_ADD_EXPENSE = "com.ks.expense.action.ADD_EXPENSE"
    }
}
