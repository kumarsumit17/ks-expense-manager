package com.ks.expense.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import com.ks.expense.BuildConfig
import com.ks.expense.R
import com.ks.expense.backup.BackupData
import com.ks.expense.data.Category
import com.ks.expense.data.DefaultCategories
import com.ks.expense.data.ExpenseStore
import com.ks.expense.transactions.Currencies
import com.ks.expense.ui.CurrencyPicker
import com.ks.expense.ui.appLocale
import com.ks.expense.recurring.RecurringDialog
import com.ks.expense.recurring.ruleSummary
import com.ks.expense.ui.categoryLabel
import com.ks.expense.ui.money
import com.ks.utility.appupdate.AppUpdate
import com.ks.utility.appupdate.GitHubRepo
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId

private val REPO = GitHubRepo.parse("kumasumit/ks-expense-manager")!!

/**
 * The releases list, newest first. Not `releases/latest`: Android releases have their own
 * `android-v…` tags and are not marked latest (specs/app-updates.md).
 */
private val RELEASES_PAGE = "https://github.com/${REPO.owner}/${REPO.name}/releases"

/** Settings: language, categories, your data (export, back up, restore) and app updates. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(store: ExpenseStore, onBack: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val categories by remember { store.dao.categories() }.collectAsState(initial = emptyList())
    var editing by remember { mutableStateOf<Category?>(null) }
    var addingCategory by rememberSaveable { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<BackupData?>(null) }
    val defaultCurrency by store.settings.defaultCurrency.collectAsState()
    var pickingCurrency by rememberSaveable { mutableStateOf(false) }
    val rules by remember { store.dao.recurringRules() }.collectAsState(initial = emptyList())
    var editingRule by rememberSaveable { mutableStateOf<Long?>(null) }

    fun report(block: suspend () -> String) {
        scope.launch {
            val message = try {
                block()
            } catch (_: IOException) {
                resources.getString(R.string.common_error_generic)
            } catch (_: SecurityException) {
                resources.getString(R.string.common_error_permission)
            }
            snackbar.showSnackbar(message)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        report {
            val count = store.exportCsv(uri, ZoneId.systemDefault()) { resources.categoryLabel(it) }
            resources.getQuantityString(R.plurals.data_exported, count, count)
        }
    }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        report {
            store.backup(uri)
            resources.getString(R.string.data_backed_up)
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val data = store.readBackup(uri)
            if (data == null) snackbar.showSnackbar(resources.getString(R.string.data_bad_backup)) else pendingRestore = data
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.common_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp)) {
            item { Section(stringResource(R.string.common_language)) }
            item { LanguageChoice() }

            item { Section(stringResource(R.string.settings_currency)) }
            item {
                ListItem(
                    modifier = Modifier.clickable { pickingCurrency = true }.padding(horizontal = 8.dp),
                    headlineContent = { Text(Currencies.label(defaultCurrency, appLocale())) },
                    supportingContent = { Text(stringResource(R.string.settings_currency_hint)) },
                )
            }

            item { Section(stringResource(R.string.categories_title)) }
            items(categories, key = { it.id }) { category ->
                val label = categoryLabel(category)
                ListItem(
                    modifier = Modifier.clickable { editing = category },
                    leadingContent = { Text(category.icon, style = MaterialTheme.typography.titleLarge, modifier = Modifier.clearAndSetSemantics {}) },
                    headlineContent = { Text(label) },
                    trailingContent = {
                        Switch(
                            checked = !category.hidden,
                            onCheckedChange = { show -> scope.launch { store.dao.updateCategory(category.copy(hidden = !show)) } },
                            modifier = Modifier.padding(start = 8.dp).semantics {
                                contentDescription = resources.getString(R.string.categories_show, label)
                            },
                        )
                    },
                )
            }
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    TextButton(onClick = { addingCategory = true }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text(stringResource(R.string.categories_add), modifier = Modifier.padding(start = 8.dp))
                    }
                    Hint(stringResource(R.string.categories_hidden_hint))
                }
            }

            item { Section(stringResource(R.string.recurring_title)) }
            if (rules.isEmpty()) {
                item { Column(Modifier.padding(horizontal = 16.dp)) { Hint(stringResource(R.string.recurring_empty)) } }
            }
            items(rules, key = { "r${it.id}" }) { rule ->
                val category = categories.firstOrNull { it.id == rule.categoryId }
                ListItem(
                    modifier = Modifier.clickable { editingRule = rule.id }.padding(horizontal = 8.dp),
                    leadingContent = { Text(category?.icon ?: "", style = MaterialTheme.typography.titleLarge, modifier = Modifier.clearAndSetSemantics {}) },
                    headlineContent = { Text(rule.note.ifBlank { category?.let { categoryLabel(it) } ?: "" }) },
                    supportingContent = { Text(ruleSummary(rule)) },
                    trailingContent = { Text(money(rule.amountMinor, rule.currency)) },
                )
            }

            item { Section(stringResource(R.string.data_title)) }
            item {
                DataAction(stringResource(R.string.data_export), stringResource(R.string.data_export_hint)) {
                    exportLauncher.launch("ks-expenses-${LocalDate.now()}.csv")
                }
            }
            item {
                DataAction(stringResource(R.string.data_backup), stringResource(R.string.data_backup_hint)) {
                    backupLauncher.launch("ks-expense-backup-${LocalDate.now()}.json")
                }
            }
            item {
                DataAction(stringResource(R.string.data_restore), null) {
                    restoreLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                }
            }

            item { Section(stringResource(R.string.common_app_update_section)) }
            item {
                val version = BuildConfig.VERSION_NAME
                Column(Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        if (AppUpdate.isDevVersion(version)) stringResource(R.string.common_app_update_dev)
                        else stringResource(R.string.common_app_update_current, version),
                    )
                    OutlinedButton(
                        onClick = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, RELEASES_PAGE.toUri()))
                            } catch (_: ActivityNotFoundException) {
                                scope.launch { snackbar.showSnackbar(resources.getString(R.string.common_error_generic)) }
                            }
                        },
                        modifier = Modifier.padding(top = 8.dp),
                    ) { Text(stringResource(R.string.common_app_update_get_button)) }
                }
            }
        }
    }

    editingRule?.let { id -> rules.firstOrNull { it.id == id } }?.let { rule ->
        RecurringDialog(
            rule = rule,
            categories = categories,
            onSave = {
                scope.launch { store.dao.updateRule(it) }
                editingRule = null
            },
            onDelete = {
                scope.launch { store.dao.deleteRule(it) }
                editingRule = null
            },
            onDismiss = { editingRule = null },
        )
    }

    if (pickingCurrency) {
        CurrencyPicker(
            title = stringResource(R.string.settings_currency),
            selected = defaultCurrency,
            onPick = {
                store.settings.setDefaultCurrency(it)
                pickingCurrency = false
            },
            onDismiss = { pickingCurrency = false },
        )
    }

    editing?.let { category ->
        CategoryDialog(
            initialName = categoryLabel(category),
            initialIcon = category.icon,
            onDismiss = { editing = null },
            onSave = { name, icon ->
                val builtinName = resources.categoryLabel(category.copy(name = null))
                val newName = name.takeIf { category.builtinKey == null || it != builtinName }
                scope.launch { store.dao.updateCategory(category.copy(name = newName, icon = icon)) }
                editing = null
            },
        )
    }
    if (addingCategory) {
        CategoryDialog(
            initialName = "",
            initialIcon = DefaultCategories.icons.first(),
            onDismiss = { addingCategory = false },
            onSave = { name, icon ->
                scope.launch {
                    store.dao.insertCategory(Category(builtinKey = null, name = name, icon = icon, sort = store.dao.maxSort() + 1))
                }
                addingCategory = false
            },
        )
    }
    pendingRestore?.let { data ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text(stringResource(R.string.data_restore_title)) },
            text = { Text(stringResource(R.string.data_restore_warning) + " " + stringResource(R.string.common_confirm_cannot_undo)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestore = null
                    report {
                        store.restore(data)
                        resources.getQuantityString(R.plurals.data_restored, data.expenses.size, data.expenses.size)
                    }
                }) { Text(stringResource(R.string.data_restore)) }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp))
}

@Composable
private fun DataAction(title: String, hint: String?, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 8.dp),
        headlineContent = { Text(title) },
        supportingContent = hint?.let { { Text(it) } },
    )
}

/** "Same as the system", English, Hindi: the system's per-app language remembers the choice. */
@Composable
private fun LanguageChoice() {
    val current = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-').substringBefore(',')
    val options = listOf(
        "" to stringResource(R.string.common_language_system),
        "en" to stringResource(R.string.common_language_english),
        "hi" to stringResource(R.string.common_language_hindi),
    )
    Column {
        options.forEach { (tag, label) ->
            ListItem(
                modifier = Modifier.selectable(
                    selected = current == tag,
                    role = Role.RadioButton,
                    onClick = {
                        AppCompatDelegate.setApplicationLocales(
                            if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
                        )
                    },
                ).padding(horizontal = 8.dp),
                leadingContent = { RadioButton(selected = current == tag, onClick = null) },
                headlineContent = { Text(label) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryDialog(initialName: String, initialIcon: String, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var icon by rememberSaveable { mutableStateOf(initialIcon) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initialName.isEmpty()) R.string.categories_add else R.string.common_rename)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = { Text(stringResource(R.string.categories_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (listOf(initialIcon) + DefaultCategories.icons).distinct().forEach {
                        FilterChip(selected = it == icon, onClick = { icon = it }, label = { Text(it) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onSave(name.trim(), icon) }) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}
