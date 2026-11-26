package com.ks.expense.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ks.expense.R
import com.ks.expense.data.Category
import com.ks.expense.data.Expense
import com.ks.expense.reports.Months
import com.ks.expense.transactions.Money
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Quick add and edit. Adding takes three taps from launch: +, a category, Save (the amount
 * field is focused with the number keyboard open).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExpenseSheet(
    existing: Expense?,
    categories: List<Category>,
    defaultCurrency: String = Money.DEFAULT_CURRENCY,
    onSave: (Expense) -> Unit,
    onDelete: (Expense) -> Unit,
    onDismiss: () -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val key = existing?.id ?: 0L
    var currency by rememberSaveable(key) { mutableStateOf(existing?.currency ?: defaultCurrency) }
    var pickingCurrency by rememberSaveable { mutableStateOf(false) }
    var amountText by rememberSaveable(key) { mutableStateOf(existing?.let { Money.toInput(it.amountMinor, it.currency) } ?: "") }
    var categoryId by rememberSaveable(key) { mutableStateOf(existing?.categoryId) }
    var note by rememberSaveable(key) { mutableStateOf(existing?.note ?: "") }
    var dateText by rememberSaveable(key) {
        mutableStateOf((existing?.let { Months.localDate(it.occurredAt, zone) } ?: LocalDate.now(zone)).toString())
    }
    val date = LocalDate.parse(dateText)
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val amount = Money.parse(amountText, currency)
    val shown = categories.filter { !it.hidden || it.id == existing?.categoryId }
    val focus = remember { FocusRequester() }
    val haptics = LocalHapticFeedback.current

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 16.dp).imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Save stays at the top, so the keyboard never covers it.
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(
                    stringResource(if (existing == null) R.string.expense_add else R.string.expense_edit),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    enabled = amount != null && categoryId != null,
                    onClick = {
                        val now = System.currentTimeMillis()
                        val occurredAt = when {
                            existing != null && Months.localDate(existing.occurredAt, zone) == date -> existing.occurredAt
                            existing == null && date == LocalDate.now(zone) -> now
                            else -> {
                                val time = existing?.let { Instant.ofEpochMilli(it.occurredAt).atZone(zone).toLocalTime() } ?: LocalTime.now(zone)
                                date.atTime(time).atZone(zone).toInstant().toEpochMilli()
                            }
                        }
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        onSave(
                            Expense(
                                id = existing?.id ?: 0,
                                amountMinor = amount!!,
                                currency = currency,
                                categoryId = categoryId!!,
                                note = note.trim(),
                                occurredAt = occurredAt,
                                createdAt = existing?.createdAt ?: now,
                            ),
                        )
                    },
                ) { Text(stringResource(R.string.common_save)) }
            }
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.take(20) },
                label = { Text(stringResource(R.string.expense_amount)) },
                prefix = { Text(Money.symbol(currency, appLocale()) + " ") },
                singleLine = true,
                isError = amountText.isNotBlank() && amount == null,
                supportingText = if (amountText.isNotBlank() && amount == null) {
                    { Text(stringResource(R.string.expense_amount_invalid)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                textStyle = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth().focusRequester(focus).testTag("amount"),
            )
            Text(stringResource(R.string.expense_category), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                shown.forEach { category ->
                    FilterChip(
                        selected = category.id == categoryId,
                        onClick = { categoryId = category.id },
                        label = {
                            Text(category.icon, modifier = Modifier.clearAndSetSemantics {})
                            Spacer(Modifier.width(6.dp))
                            Text(categoryLabel(category))
                        },
                    )
                }
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(200) },
                label = { Text(stringResource(R.string.expense_note)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { pickingCurrency = true },
                    label = { Text(stringResource(R.string.expense_currency, currency)) },
                )
                AssistChip(
                    onClick = { pickingDate = true },
                    label = {
                        Text(
                            stringResource(R.string.expense_date) + ": " +
                                date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(appLocale())),
                        )
                    },
                )
            }
            if (existing != null) {
                TextButton(onClick = { confirmDelete = true }) { Text(stringResource(R.string.common_delete)) }
            }
        }
    }

    if (existing == null) {
        LaunchedEffect(Unit) { focus.requestFocus() }
    }

    if (pickingCurrency) {
        CurrencyPicker(
            title = stringResource(R.string.expense_currency_title),
            selected = currency,
            onPick = {
                // Keep the typed amount if it still fits the new currency's decimals.
                currency = it
                pickingCurrency = false
            },
            onDismiss = { pickingCurrency = false },
        )
    }

    if (pickingDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        dateText = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    }
                    pickingDate = false
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text(stringResource(R.string.common_cancel)) } },
        ) { DatePicker(state = state) }
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.expense_delete_title)) },
            text = { Text(stringResource(R.string.common_confirm_cannot_undo)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    haptics.performHapticFeedback(HapticFeedbackType.Reject)
                    onDelete(existing)
                }) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}
