package io.github.katzfuss11.tanzlexikon.ui.papierkorb

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.katzfuss11.tanzlexikon.R
import io.github.katzfuss11.tanzlexikon.lexikon.TrashedFigure
import io.github.katzfuss11.tanzlexikon.ui.BackButton
import io.github.katzfuss11.tanzlexikon.ui.appViewModel
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PapierkorbScreen(
    onBack: () -> Unit,
    vm: PapierkorbViewModel = appViewModel { PapierkorbViewModel(it.repository) },
) {
    val trash by vm.trash.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf<TrashedFigure?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val restoredText = stringResource(R.string.restored)
    val now = remember { System.currentTimeMillis() }
    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.menu_trash)) }, navigationIcon = { BackButton(onBack) }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            item {
                Text(
                    stringResource(R.string.trash_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
            val list = trash
            if (list != null && list.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.trash_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                    )
                }
            }
            items(list.orEmpty(), key = { it.id }) { fig ->
                val left = daysLeft(now, fig.trashedAt)
                Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp)) {
                    Text(fig.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        listOf(
                            fig.danceName,
                            stringResource(R.string.trashed_on, dateFormat.format(Date(fig.trashedAt))),
                            pluralStringResource(R.plurals.days_left, left, left),
                        ).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { confirm = fig }) { Text(stringResource(R.string.delete_forever)) }
                        TextButton(onClick = {
                            vm.restore(fig.id)
                            scope.launch { snackbar.showSnackbar(restoredText) }
                        }) { Text(stringResource(R.string.restore)) }
                    }
                }
                HorizontalDivider()
            }
        }
    }

    confirm?.let { fig ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(stringResource(R.string.delete_forever)) },
            text = { Text(stringResource(R.string.delete_forever_question, fig.name)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteForever(fig.id)
                    confirm = null
                }) { Text(stringResource(R.string.delete_forever)) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
