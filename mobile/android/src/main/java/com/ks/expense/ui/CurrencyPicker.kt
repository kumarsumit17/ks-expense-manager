package com.ks.expense.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ks.expense.R
import com.ks.expense.transactions.Currencies

/** Picks a currency: common ones first, a search field for the rest. */
@Composable
fun CurrencyPicker(title: String, selected: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val locale = appLocale()
    val all = remember(locale) { Currencies.all(locale) }
    var text by rememberSaveable { mutableStateOf("") }
    val shown = remember(all, text, locale) { Currencies.filter(all, text, locale) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(40) },
                    placeholder = { Text(stringResource(R.string.common_search)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("currency-search"),
                )
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(shown, key = { it }) { code ->
                        ListItem(
                            modifier = Modifier.clickable(role = Role.RadioButton) { onPick(code) },
                            leadingContent = { RadioButton(selected = code == selected, onClick = null) },
                            headlineContent = { Text(Currencies.label(code, locale)) },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}
