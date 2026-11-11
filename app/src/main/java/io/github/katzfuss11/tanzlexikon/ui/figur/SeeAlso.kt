package io.github.katzfuss11.tanzlexikon.ui.figur

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.katzfuss11.tanzlexikon.R
import io.github.katzfuss11.tanzlexikon.lexikon.FigureLinks
import io.github.katzfuss11.tanzlexikon.lexikon.FigureListItem
import io.github.katzfuss11.tanzlexikon.lexikon.LinkSuggestion

/**
 * „Siehe auch“ unter den drei Bereichen: dieselbe Figur in anderen Tänzen und verbundene Figuren
 * im selben Tanz. Beim Bearbeiten mit Lösen-Knöpfen und „Figur verknüpfen“.
 */
@Composable
fun SeeAlsoSection(
    links: FigureLinks,
    editing: Boolean,
    onOpen: (FigureListItem) -> Unit,
    onRemove: (FigureListItem) -> Unit,
    onAdd: () -> Unit,
) {
    if (!editing && links.otherDances.isEmpty() && links.sameDance.isEmpty()) return
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.see_also),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            LinkGroup(R.string.see_also_other_dances, links.otherDances, editing, onOpen, onRemove)
            LinkGroup(R.string.see_also_same_dance, links.sameDance, editing, onOpen, onRemove)
            if (editing) {
                TextButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.link_figure))
                }
            }
        }
    }
}

@Composable
private fun LinkGroup(
    title: Int,
    items: List<FigureListItem>,
    editing: Boolean,
    onOpen: (FigureListItem) -> Unit,
    onRemove: (FigureListItem) -> Unit,
) {
    if (items.isEmpty()) return
    Text(stringResource(title), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    items.forEach { item ->
        ListItem(
            headlineContent = { Text(item.name) },
            supportingContent = { Text(item.danceName) },
            trailingContent = if (editing) {
                {
                    IconButton(onClick = { onRemove(item) }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.unlink))
                    }
                }
            } else {
                null
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = if (editing) Modifier else Modifier.clickable { onOpen(item) },
        )
    }
}

/** Auswahl einer Figur zum Verknüpfen, mit Suche im Namen. Ein Tipp wählt aus und schließt. */
@Composable
fun LinkPickerDialog(candidates: List<FigureListItem>, onPick: (FigureListItem) -> Unit, onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val shown = candidates.filter { it.name.contains(query.trim(), ignoreCase = true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.link_figure)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.search_figure)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (shown.isEmpty()) {
                    Text(stringResource(R.string.no_matching_figures), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(shown, key = { it.id }) { item ->
                        ListItem(
                            headlineContent = { Text(item.name) },
                            supportingContent = { Text(item.danceName) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { onPick(item) },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Vorschlag, gleichnamige Figuren aus anderen Tänzen zu verknüpfen. Schließen zählt als „Nicht verknüpfen“. */
@Composable
fun LinkSuggestionDialog(suggestion: LinkSuggestion, onAnswer: (Boolean) -> Unit) {
    val dances = suggestion.candidates.map { it.danceName }.distinct()
    val list = if (dances.size == 1) dances[0]
    else dances.dropLast(1).joinToString(", ") + " " + stringResource(R.string.and) + " " + dances.last()
    AlertDialog(
        onDismissRequest = { onAnswer(false) },
        title = { Text(stringResource(R.string.link_suggestion_title, suggestion.figureName, list)) },
        text = { Text(stringResource(R.string.link_suggestion_text)) },
        confirmButton = { TextButton(onClick = { onAnswer(true) }) { Text(stringResource(R.string.link)) } },
        dismissButton = { TextButton(onClick = { onAnswer(false) }) { Text(stringResource(R.string.dont_link)) } },
    )
}
