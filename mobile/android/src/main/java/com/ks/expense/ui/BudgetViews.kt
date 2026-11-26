package com.ks.expense.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ks.expense.R
import com.ks.expense.budgets.BudgetLevel
import com.ks.expense.budgets.BudgetStatus
import com.ks.expense.transactions.Money

/** The colour for a budget's bar and "over" text. */
@Composable
fun budgetColor(level: BudgetLevel): Color = when (level) {
    BudgetLevel.OK -> MaterialTheme.colorScheme.primary
    BudgetLevel.NEAR -> if (isSystemInDarkTheme()) BudgetColors.nearDark else BudgetColors.nearLight
    BudgetLevel.OVER -> MaterialTheme.colorScheme.error
}

/** A thin bar filled to [fraction] (0..1) in [color]. */
@Composable
fun ProgressBar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(6.dp).clip(RoundedCornerShape(3.dp))
                .background(color),
        )
    }
}

/** "₹5,500 left" or "₹500 over". */
@Composable
fun budgetLeftText(status: BudgetStatus, currency: String): String =
    if (status.leftMinor >= 0) {
        stringResource(R.string.budget_left, money(status.leftMinor, currency))
    } else {
        stringResource(R.string.budget_over, money(-status.leftMinor, currency))
    }

/** Sets or removes a monthly budget. [title] names what it is for. */
@Composable
fun BudgetDialog(
    title: String,
    currency: String,
    currentMinor: Long?,
    onSave: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(currentMinor?.let { Money.toInput(it, currency) } ?: "") }
    val amount = Money.parse(text, currency)
    val focus = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(20) },
                    label = { Text(stringResource(R.string.budget_limit)) },
                    prefix = { Text(Money.symbol(currency, appLocale()) + " ") },
                    singleLine = true,
                    isError = text.isNotBlank() && amount == null,
                    supportingText = { Text(stringResource(R.string.budget_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus).testTag("budget"),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = amount != null, onClick = { onSave(amount) }) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            Row {
                if (currentMinor != null) {
                    TextButton(onClick = { onSave(null) }) { Text(stringResource(R.string.common_remove)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
            }
        },
    )
    LaunchedEffect(Unit) { focus.requestFocus() }
}
