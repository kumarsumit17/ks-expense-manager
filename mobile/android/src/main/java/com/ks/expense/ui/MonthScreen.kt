package com.ks.expense.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.ks.expense.R
import com.ks.expense.data.Category
import com.ks.expense.data.Expense
import com.ks.expense.data.ExpenseStore
import com.ks.expense.reports.CategoryTotal
import com.ks.expense.reports.DayGroup
import com.ks.expense.reports.Months
import com.ks.expense.reports.Total
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Home: one month, its totals, a bar per category and the expenses grouped by day. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthScreen(store: ExpenseStore, openAdd: Boolean = false, onAddOpened: () -> Unit = {}, onSettings: () -> Unit) {
    val zone = ZoneId.systemDefault()
    var monthText by rememberSaveable { mutableStateOf(YearMonth.now(zone).toString()) }
    val month = YearMonth.parse(monthText)
    val (from, to) = Months.range(month, zone)
    val expenses by remember(from, to) { store.dao.between(from, to) }.collectAsState(initial = null)
    val categories by remember { store.dao.categories() }.collectAsState(initial = emptyList())
    val byId = categories.associateBy { it.id }
    val summary = remember(expenses, month) { expenses?.let { Months.summarize(month, it, zone) } }

    var adding by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()
    // The launcher shortcut opens the add sheet straight away.
    LaunchedEffect(openAdd) {
        if (openAdd) {
            editingId = null
            adding = true
            onAddOpened()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.common_settings))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { adding = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.expense_add))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 88.dp,
            ),
        ) {
            item { MonthHeader(month, onMove = { monthText = month.plusMonths(it).toString() }) }
            if (summary != null) {
                item { TotalCard(summary.totals, summary.count) }
                if (summary.count == 0) {
                    item {
                        Text(
                            stringResource(R.string.month_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp),
                        )
                    }
                }
                items(summary.categories, key = { "c${it.categoryId}-${it.currency}" }) { total ->
                    byId[total.categoryId]?.let { CategoryBar(it, total) }
                }
                summary.days.forEach { day ->
                    item(key = "d${day.date}") { DayHeader(day) }
                    items(day.expenses, key = { "e${it.id}" }) { expense ->
                        ExpenseRow(expense, byId[expense.categoryId], onClick = { editingId = expense.id })
                    }
                }
            }
        }
    }

    val editing = editingId?.let { id -> expenses?.firstOrNull { it.id == id } }
    if (adding || editing != null) {
        ExpenseSheet(
            existing = editing,
            categories = categories,
            onSave = { expense ->
                scope.launch { if (expense.id == 0L) store.dao.insert(expense) else store.dao.update(expense) }
                adding = false
                editingId = null
            },
            onDelete = { expense ->
                scope.launch { store.dao.delete(expense) }
                editingId = null
            },
            onDismiss = {
                adding = false
                editingId = null
            },
        )
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onMove: (Long) -> Unit) {
    val formatter = DateTimeFormatter.ofPattern("LLLL yyyy", appLocale())
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onMove(-1) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.month_previous))
        }
        Text(
            month.format(formatter),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        IconButton(onClick = { onMove(1) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.month_next))
        }
    }
}

@Composable
private fun TotalCard(totals: List<Total>, count: Int) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text(
            stringResource(R.string.month_total),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (totals.isEmpty()) {
            Text(money(0, com.ks.expense.transactions.Money.DEFAULT_CURRENCY), style = MaterialTheme.typography.displaySmall)
        }
        totals.forEach {
            Text(money(it.amountMinor, it.currency), style = MaterialTheme.typography.displaySmall)
        }
        Text(
            pluralStringResource(R.plurals.month_count, count, count),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CategoryBar(category: Category, total: CategoryTotal) {
    val label = categoryLabel(category)
    val amount = money(total.amountMinor, total.currency)
    val sentence = stringResource(R.string.month_category_share, label, amount, (total.share * 100).roundToInt().toString())
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp)
            .clearAndSetSemantics { contentDescription = sentence },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${category.icon}  $label", modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(amount, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                Modifier.fillMaxWidth(total.share.coerceIn(0f, 1f)).height(6.dp).clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
private fun DayHeader(day: DayGroup) {
    val locale = appLocale()
    val formatter = DateTimeFormatter.ofPattern("EEE, d MMM", locale)
    Column {
        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 4.dp)
                .semantics(mergeDescendants = true) { heading() },
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(day.date.format(formatter), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                day.totals.joinToString(" · ") { com.ks.expense.transactions.Money.format(it.amountMinor, it.currency, locale) },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ExpenseRow(expense: Expense, category: Category?, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 8.dp),
        leadingContent = {
            Text(category?.icon ?: "", style = MaterialTheme.typography.titleLarge, modifier = Modifier.width(32.dp).clearAndSetSemantics {})
        },
        headlineContent = { Text(category?.let { categoryLabel(it) } ?: "") },
        supportingContent = expense.note.takeIf { it.isNotBlank() }?.let { note ->
            { Text(note, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        },
        trailingContent = { Text(money(expense.amountMinor, expense.currency), style = MaterialTheme.typography.titleMedium) },
    )
}
