package com.ks.expense.settings

import android.content.Context
import androidx.core.content.edit
import com.ks.expense.transactions.Currencies
import com.ks.expense.transactions.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The app's few settings, in Android's own SharedPreferences (no library). The language is not
 * here: AppCompat keeps it.
 */
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val currency = MutableStateFlow(
        prefs.getString(KEY_CURRENCY, null)?.takeIf { Currencies.isValid(it) } ?: Money.DEFAULT_CURRENCY,
    )

    /** The currency new expenses and budgets start in; rupees until the user picks another. */
    val defaultCurrency: StateFlow<String> = currency.asStateFlow()

    fun setDefaultCurrency(code: String) {
        require(Currencies.isValid(code)) { "not a currency: $code" }
        prefs.edit { putString(KEY_CURRENCY, code) }
        currency.value = code
    }

    private companion object {
        const val KEY_CURRENCY = "default_currency"
    }
}
