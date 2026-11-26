package com.ks.expense.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ks.expense.R
import com.ks.expense.data.ExpenseStore
import com.ks.expense.reports.Months
import com.ks.expense.transactions.Search
import com.ks.expense.transactions.SearchFilter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Search across all months: note text, an amount or a category name, plus category and date filters. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(store: ExpenseStore, onBack: () -> Unit) {
    val zone = ZoneId.systemDefault()
    val resources = LocalResources.current
    var text by rememberSaveable { mutableStateOf("") }
    var categoryIds by rememberSaveable { mutableStateOf(listOf<Long>()) }
    var fromDay by rememberSaveable { mutableStateOf<String?>(null) }
    var toDay by rememberSaveable { mutableStateOf<String?>(null) }
    var pickingDates by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }

    // The query follows the typed text after a short pause, so typing never waits on the database.
    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(text) {
        if (text.isNotBlank()) delay(250)
        query = text
    }

    val defaultCurrency by store.settings.defaultCurrency.collectAsState()
    val categories by remember { store.dao.categories() }.collectAsState(initial = emptyList())
    val byId = categories.associateBy { it.id }
    val filter = SearchFilter(
        text = query,
        categoryIds = categoryIds.toSet(),
        fromMillis = fromDay?.let { LocalDate.parse(it).atStartOfDay(zone).toInstant().toEpochMilli() },
        toMillis = toDay?.let { LocalDate.parse(it).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() },
    )
    val nameMatches = categories.filter { Search.nameMatches(resources.categoryLabel(it), query) }.map { it.id }
    val results by remember(filter, nameMatches) {
        store.dao.search(
            pattern = Search.likePattern(filter.text),
            amountMinor = Search.amount(filter.text, defaultCurrency),
            nameCategories = nameMatches.ifEmpty { listOf(-1L) },
            anyCategory = filter.categoryIds.isEmpty(),
            categories = filter.categoryIds.toList().ifEmpty { listOf(-1L) },
            from = filter.fromMillis,
            to = filter.toMillis,
            limit = Search.LIMIT,
        )
    }.collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                title = {
                    TextField(
                        value = text,
                        onValueChange = { text = it.take(100) },
                        placeholder = { Text(stringResource(R.string.search_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        trailingIcon = if (text.isNotEmpty()) {
                            {
                                IconButton(onClick = { text = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_close))
                                }
                            }
                        } else {
                            null
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                        ),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus).testTag("search"),
                    )
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val format = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(appLocale())
                    AssistChip(
                        onClick = { pickingDates = true },
                        leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                        label = {
                            Text(
                                if (fromDay != null && toDay != null) {
                                    stringResource(R.string.filter_date_range, LocalDate.parse(fromDay).format(format), LocalDate.parse(toDay).format(format))
                                } else {
                                    stringResource(R.string.filter_dates)
                                },
                            )
                        },
                    )
                    categories.forEach { category ->
                        FilterChip(
                            selected = category.id in categoryIds,
                            onClick = {
                                categoryIds = if (category.id in categoryIds) categoryIds - category.id else categoryIds + category.id
                            },
                            label = {
                                Text(category.icon, modifier = Modifier.clearAndSetSemantics {})
                                Spacer(Modifier.width(6.dp))
                                Text(categoryLabel(category))
                            },
                        )
                    }
                }
            }
            if (!filter.isEmpty) {
                item {
                    TextButton(
                        onClick = {
                            text = ""
                            categoryIds = emptyList()
                            fromDay = null
                            toDay = null
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    ) { Text(stringResource(R.string.filter_clear)) }
                }
            }
            val list = results
            when {
                filter.isEmpty -> item { Hint(stringResource(R.string.search_start)) }
                list == null -> Unit
                list.isEmpty() -> item { Hint(stringResource(R.string.search_no_results)) }
                else -> {
                    item {
                        Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                            Text(
                                pluralStringResource(R.plurals.search_results, list.size, list.size),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Months.totals(list, defaultCurrency).forEach {
                                Text(money(it.amountMinor, it.currency), style = MaterialTheme.typography.headlineSmall)
                            }
                            if (list.size >= Search.LIMIT) {
                                Text(
                                    stringResource(R.string.search_limited, Search.LIMIT.toString()),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    Months.days(list, zone, defaultCurrency).forEach { day ->
                        item(key = "d${day.date}") { DayHeader(day, withYear = true) }
                        items(day.expenses, key = { "e${it.id}" }) { expense ->
                            ExpenseRow(expense, byId[expense.categoryId], onClick = { editingId = expense.id })
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) { focus.requestFocus() }

    if (pickingDates) {
        val state = rememberDateRangePickerState(
            initialSelectedStartDateMillis = fromDay?.let { LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() },
            initialSelectedEndDateMillis = toDay?.let { LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() },
        )
        DatePickerDialog(
            onDismissRequest = { pickingDates = false },
            confirmButton = {
                TextButton(
                    enabled = state.selectedStartDateMillis != null,
                    onClick = {
                        val start = state.selectedStartDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                        val end = state.selectedEndDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() } ?: start
                        fromDay = start?.toString()
                        toDay = end?.toString()
                        pickingDates = false
                    },
                ) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pickingDates = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        ) { DateRangePicker(state = state, modifier = Modifier.weight(1f)) }
    }

    val editing = editingId?.let { id -> results?.firstOrNull { it.id == id } }
    if (editing != null) {
        ExpenseSheet(
            existing = editing,
            categories = categories,
            onSave = { expense ->
                scope.launch { store.dao.update(expense) }
                editingId = null
            },
            onDelete = { expense ->
                scope.launch { store.dao.delete(expense) }
                editingId = null
            },
            onDismiss = { editingId = null },
        )
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(24.dp),
    )
}
