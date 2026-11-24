package com.ks.expense

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.ks.expense.settings.SettingsScreen
import com.ks.expense.ui.KsTheme
import com.ks.expense.ui.MonthScreen

/** AppCompat activity: it applies the per-app language on Android 8 to 12. */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val store = (application as ExpenseApp).store
        setContent {
            KsTheme {
                var settings by rememberSaveable { mutableStateOf(false) }
                if (settings) {
                    BackHandler { settings = false }
                    SettingsScreen(store, onBack = { settings = false })
                } else {
                    MonthScreen(store, onSettings = { settings = true })
                }
            }
        }
    }
}
