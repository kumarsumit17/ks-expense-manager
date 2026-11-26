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
import androidx.compose.material.icons.filled.Search
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
import com.ks.expense.budgets.BudgetLevel
import com.ks.expense.budgets.BudgetStatus
import com.ks.expense.data.Budget
import com.ks.expense.transactions.Money
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** The budget dialog for the whole month (category ids start at 1). */
private const val OVERALL = 0L

/** Home: one month, its totals, a bar per category and the expenses grouped by day. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthScreen(
    store: ExpenseStore,
    openAdd: Boolean = false,
    onAddOpened: () -> Unit = {},
    onSearch: () -> Unit = {},
    onSettings: () -> Unit,
) {
    val zone = ZoneId.systemDefault()
    var monthText by rememberSaveable { mutableStateOf(YearMonth.now(zone).toString()) }
    val month = YearMonth.parse(monthText)
    val (from, to) = Months.range(month, zone)
    val expenses by remember(from, to) { store.dao.between(from, to) }.collectAsState(initial = null)
    val categories by remember { store.dao.categories() }.collectAsState(initial = emptyList())
    val byId = categories.associateBy { it.id }
    val summary = remember(expenses, month) { expenses?.let { Months.summarize(month, it, zone) } }
    val budgets by remember { store.dao.budgets() }.collectAsState(initial = emptyList())
    val overallBudget = budgets.firstOrNull { it.categoryId == null }
    val categoryBudgets = budgets.filter { it.categoryId != null }.associateBy { it.categoryId!! }
    val bars = remember(summary, budgets, categories) { summary?.let { barRows(it.categories, categoryBudgets, byId) }.orEmpty() }
    // Which budget dialog is open: OVERALL for the whole month, a category id, or null.
    var budgetTarget by rememberSaveable { mutableStateOf<Long?>(null) }

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
                    IconButton(onClick = onSearch) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.common_search))
                    }
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
                item { TotalCard(summary.totals, summary.count, overallBudget, onClick = { budgetTarget = OVERALL }) }
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
                items(bars, key = { "c${it.category.id}-${it.currency}" }) { row ->
                    CategoryBar(row, onClick = { budgetTarget = row.category.id })
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

    budgetTarget?.let { target ->
        val category = if (target == OVERALL) null else byId[target]
        val current = if (target == OVERALL) overallBudget else categoryBudgets[target]
        BudgetDialog(
            title = category?.let { stringResource(R.string.budget_for_category, categoryLabel(it)) }
                ?: stringResource(R.string.budget_title),
            currency = current?.currency ?: Money.DEFAULT_CURRENCY,
            currentMinor = current?.amountMinor,
            onSave = { amount ->
                scope.launch { store.dao.setBudget(category?.id, amount, current?.currency ?: Money.DEFAULT_CURRENCY) }
                budgetTarget = null
            },
            onDismiss = { budgetTarget = null },
        )
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
private fun TotalCard(totals: List<Total>, count: Int, budget: Budget?, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text(
            stringResource(R.string.month_total),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (totals.isEmpty()) {
            Text(money(0, Money.DEFAULT_CURRENCY), style = MaterialTheme.typography.displaySmall)
        }
        totals.forEach {
            Text(money(it.amountMinor, it.currency), style = MaterialTheme.typography.displaySmall)
        }
        Text(
            pluralStringResource(R.plurals.month_count, count, count),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (budget != null) {
            val spent = totals.firstOrNull { it.currency == budget.currency }?.amountMinor ?: 0L
            val status = BudgetStatus(spent, budget.amountMinor)
            Spacer(Modifier.height(8.dp))
            ProgressBar(status.fraction, budgetColor(status.level))
            Spacer(Modifier.height(4.dp))
            Row {
                Text(
                    stringResource(R.string.budget_of, money(budget.amountMinor, budget.currency)),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    budgetLeftText(status, budget.currency),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (status.level == BudgetLevel.OK) MaterialTheme.colorScheme.onSurfaceVariant else budgetColor(status.level),
                )
            }
        } else {
            Text(
                stringResource(R.string.budget_set),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** One category's line on the month screen: spent this month, and its budget if it has one. */
internal data class BarRow(val category: Category, val currency: String, val spentMinor: Long, val share: Float, val budget: Budget?)

/** Categories with spending, then budgeted categories with none yet this month. */
internal fun barRows(totals: List<CategoryTotal>, budgets: Map<Long, Budget>, categories: Map<Long, Category>): List<BarRow> {
    val spent = totals.mapNotNull { t ->
        categories[t.categoryId]?.let { BarRow(it, t.currency, t.amountMinor, t.share, budgets[t.categoryId]?.takeIf { b -> b.currency == t.currency }) }
    }
    val shown = spent.map { it.category.id to it.currency }.toSet()
    val unspent = budgets.values
        .filter { (it.categoryId!! to it.currency) !in shown }
        .mapNotNull { b -> categories[b.categoryId]?.let { BarRow(it, b.currency, 0, 0f, b) } }
        .sortedBy { it.category.sort }
    return spent + unspent
}

@Composable
private fun CategoryBar(row: BarRow, onClick: () -> Unit) {
    val label = categoryLabel(row.category)
    val amount = money(row.spentMinor, row.currency)
    val status = row.budget?.let { BudgetStatus(row.spentMinor, it.amountMinor) }
    val sentence = if (status != null) {
        stringResource(R.string.month_category_budget, label, amount, money(row.budget.amountMinor, row.currency), status.percent.toString()) +
            if (status.level == BudgetLevel.OK) "" else ", " + budgetLeftText(status, row.currency)
    } else {
        stringResource(R.string.month_category_share, label, amount, (row.share * 100).roundToInt().toString())
    }
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 6.dp)
            .clearAndSetSemantics { contentDescription = sentence },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${row.category.icon}  $label", modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (row.budget != null) stringResource(R.string.budget_spent_of, amount, money(row.budget.amountMinor, row.currency)) else amount,
                fontWeight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.height(4.dp))
        if (status != null) {
            ProgressBar(status.fraction, budgetColor(status.level))
            if (status.level != BudgetLevel.OK) {
                Text(budgetLeftText(status, row.currency), style = MaterialTheme.typography.bodySmall, color = budgetColor(status.level))
            }
        } else {
            ProgressBar(row.share, MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
internal fun DayHeader(day: DayGroup, withYear: Boolean = false) {
    val locale = appLocale()
    val formatter = DateTimeFormatter.ofPattern(if (withYear) "EEE, d MMM yyyy" else "EEE, d MMM", locale)
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
internal fun ExpenseRow(expense: Expense, category: Category?, onClick: () -> Unit) {
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
