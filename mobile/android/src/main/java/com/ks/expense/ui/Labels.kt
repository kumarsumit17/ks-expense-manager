package com.ks.expense.ui

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import com.ks.expense.R
import com.ks.expense.data.Category
import com.ks.expense.transactions.Money
import java.util.Locale

/** A category's name: the user's own, or the built-in one in the app language. */
fun Resources.categoryLabel(category: Category): String =
    category.name ?: getString(
        when (category.builtinKey) {
            "food" -> R.string.category_food
            "transport" -> R.string.category_transport
            "shopping" -> R.string.category_shopping
            "bills" -> R.string.category_bills
            "health" -> R.string.category_health
            "entertainment" -> R.string.category_entertainment
            else -> R.string.category_other
        },
    )

@Composable
@ReadOnlyComposable
fun categoryLabel(category: Category): String = LocalResources.current.categoryLabel(category)

/** The app language, for formatting. */
@Composable
@ReadOnlyComposable
fun appLocale(): Locale = LocalConfiguration.current.locales.let { if (it.isEmpty) Locale.ENGLISH else it[0] }

@Composable
@ReadOnlyComposable
fun money(minor: Long, currency: String): String = Money.format(minor, currency, appLocale())
