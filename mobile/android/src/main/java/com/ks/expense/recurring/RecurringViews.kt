package com.ks.expense.recurring

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ks.expense.R
import com.ks.expense.data.Category
import com.ks.expense.data.RecurringRule
import com.ks.expense.transactions.Money
import com.ks.expense.ui.appLocale
import com.ks.expense.ui.categoryLabel
import com.ks.expense.ui.money
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun cycleLabel(cycle: Cycle?): String = stringResource(
    when (cycle) {
        null -> R.string.recurring_none
        Cycle.WEEKLY -> R.string.recurring_weekly
        Cycle.MONTHLY -> R.string.recurring_monthly
        Cycle.YEARLY -> R.string.recurring_yearly
    },
)

@Composable
fun formatDate(date: LocalDate): String = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(appLocale()))

/** "Every month · next 1 Dec 2026", or "Paused". */
@Composable
fun ruleSummary(rule: RecurringRule): String =
    if (!rule.active) {
        cycleLabel(Cycle.parse(rule.cycle)) + " · " + stringResource(R.string.recurring_paused)
    } else {
        cycleLabel(Cycle.parse(rule.cycle)) + " · " + stringResource(R.string.recurring_next, formatDate(LocalDate.ofEpochDay(rule.nextDue)))
    }

/** Edits a recurring expense: amount, category, note, how often, the next date, paused or not. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecurringDialog(
    rule: RecurringRule,
    categories: List<Category>,
    onSave: (RecurringRule) -> Unit,
    onDelete: (RecurringRule) -> Unit,
    onDismiss: () -> Unit,
) {
    var amountText by rememberSaveable { mutableStateOf(Money.toInput(rule.amountMinor, rule.currency)) }
    var categoryId by rememberSaveable { mutableLongStateOf(rule.categoryId) }
    var note by rememberSaveable { mutableStateOf(rule.note) }
    var cycle by rememberSaveable { mutableStateOf(Cycle.parse(rule.cycle) ?: Cycle.MONTHLY) }
    var nextDue by rememberSaveable { mutableLongStateOf(rule.nextDue) }
    var active by rememberSaveable { mutableStateOf(rule.active) }
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val amount = Money.parse(amountText, rule.currency)
    val today = LocalDate.now()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.recurring_edit)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.take(20) },
                    label = { Text(stringResource(R.string.expense_amount)) },
                    prefix = { Text(Money.symbol(rule.currency, appLocale()) + " ") },
                    singleLine = true,
                    isError = amount == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.filter { !it.hidden || it.id == rule.categoryId }.forEach { c ->
                        FilterChip(
                            selected = c.id == categoryId,
                            onClick = { categoryId = c.id },
                            label = {
                                Text(c.icon, modifier = Modifier.clearAndSetSemantics {})
                                Spacer(Modifier.width(6.dp))
                                Text(categoryLabel(c))
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
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Cycle.entries.forEach { c ->
                        FilterChip(selected = c == cycle, onClick = { cycle = c }, label = { Text(cycleLabel(c)) })
                    }
                }
                AssistChip(
                    onClick = { pickingDate = true },
                    label = { Text(stringResource(R.string.recurring_next, formatDate(LocalDate.ofEpochDay(nextDue)))) },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.recurring_active), modifier = Modifier.weight(1f))
                    Switch(checked = active, onCheckedChange = { active = it })
                }
                TextButton(onClick = { confirmDelete = true }) { Text(stringResource(R.string.common_delete)) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = amount != null,
                onClick = {
                    val next = LocalDate.ofEpochDay(nextDue)
                    // A new date sets the day of the month the rule keeps.
                    val anchor = if (nextDue != rule.nextDue || cycle.key != rule.cycle) next.dayOfMonth else rule.anchorDay
                    onSave(
                        rule.copy(
                            amountMinor = amount!!,
                            categoryId = categoryId,
                            note = note.trim(),
                            cycle = cycle.key,
                            anchorDay = anchor,
                            nextDue = nextDue,
                            active = active,
                        ),
                    )
                },
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )

    if (pickingDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.ofEpochDay(nextDue).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        val picked = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        // The next date is never in the past; today adds it the next time the app opens.
                        nextDue = maxOf(picked, today).toEpochDay()
                    }
                    pickingDate = false
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text(stringResource(R.string.common_cancel)) } },
        ) { DatePicker(state = state) }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.recurring_delete_title)) },
            text = { Text(stringResource(R.string.recurring_delete_hint)) },
            confirmButton = { TextButton(onClick = { onDelete(rule) }) { Text(stringResource(R.string.common_delete)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

/** Shared subscriptions from ks-tv-launcher: tick the ones to add as recurring expenses, pick their category. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImportDialog(
    result: SubscriptionCsv.Result,
    existing: List<RecurringRule>,
    categories: List<Category>,
    onImport: (List<SubscriptionCsv.Row>, Long) -> Unit,
    onDismiss: () -> Unit,
) {
    fun isDuplicate(row: SubscriptionCsv.Row) =
        existing.any { it.note == row.name && it.amountMinor == row.amountMinor && it.currency == row.currency && it.cycle == row.cycle.key }
    var ticked by rememberSaveable { mutableStateOf(result.rows.indices.filter { !isDuplicate(result.rows[it]) }) }
    val visible = categories.filter { !it.hidden }
    var categoryId by rememberSaveable {
        mutableStateOf((visible.firstOrNull { it.builtinKey == "bills" } ?: visible.firstOrNull())?.id)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_title)) },
        text = {
            if (result.rows.isEmpty()) {
                Text(stringResource(R.string.import_none))
            } else {
                Column {
                    Text(stringResource(R.string.import_category), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        visible.forEach { c ->
                            FilterChip(
                                selected = c.id == categoryId,
                                onClick = { categoryId = c.id },
                                label = {
                                    Text(c.icon, modifier = Modifier.clearAndSetSemantics {})
                                    Spacer(Modifier.width(6.dp))
                                    Text(categoryLabel(c))
                                },
                            )
                        }
                    }
                    if (result.skipped > 0) {
                        Text(
                            pluralStringResource(R.plurals.import_skipped, result.skipped, result.skipped),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        itemsIndexed(result.rows) { index, row ->
                            val checked = index in ticked
                            ListItem(
                                modifier = Modifier.clickable { ticked = if (checked) ticked - index else ticked + index },
                                leadingContent = { Checkbox(checked = checked, onCheckedChange = null) },
                                headlineContent = { Text(row.name) },
                                supportingContent = {
                                    Text(
                                        money(row.amountMinor, row.currency) + " · " + cycleLabel(row.cycle) + " · " +
                                            stringResource(R.string.recurring_next, formatDate(row.nextDue)) +
                                            if (isDuplicate(row)) " · " + stringResource(R.string.import_duplicate) else "",
                                    )
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (result.rows.isNotEmpty()) {
                TextButton(
                    enabled = ticked.isNotEmpty() && categoryId != null,
                    onClick = { onImport(ticked.sorted().map { result.rows[it] }, categoryId!!) },
                ) { Text(pluralStringResource(R.plurals.import_button, ticked.size, ticked.size)) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}
