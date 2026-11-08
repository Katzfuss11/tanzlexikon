package io.github.katzfuss11.tanzlexikon.ui.figur

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.katzfuss11.tanzlexikon.R
import io.github.katzfuss11.tanzlexikon.data.Section
import io.github.katzfuss11.tanzlexikon.ui.BackButton
import io.github.katzfuss11.tanzlexikon.ui.appViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FigurScreen(
    figureId: Long,
    onBack: () -> Unit,
    vm: FigurViewModel = appViewModel(key = "figur$figureId") { FigurViewModel(it.repository, figureId) },
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var menu by remember { mutableStateOf(false) }

    val errorText = state.error?.let { stringResource(it) }
    LaunchedEffect(errorText) {
        if (errorText != null) {
            snackbar.showSnackbar(errorText)
            vm.errorShown()
        }
    }
    // Beim Bearbeiten wirkt die Zurück-Taste wie "Abbrechen".
    BackHandler(enabled = state.editing) { vm.cancelEditing() }

    Scaffold(
        topBar = {
            if (state.editing) {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = vm::cancelEditing) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                        }
                    },
                    actions = { TextButton(onClick = vm::save) { Text(stringResource(R.string.save)) } },
                )
            } else {
                TopAppBar(
                    title = { Text(state.detail?.figure?.name.orEmpty()) },
                    navigationIcon = { BackButton(onBack) },
                    actions = {
                        IconButton(onClick = vm::startEditing) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                        }
                        IconButton(onClick = { /* Teilen kommt in einem späteren Schritt */ }, enabled = false) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share))
                        }
                        IconButton(onClick = { menu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more))
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.to_trash)) }, onClick = {
                                menu = false
                                vm.trash(System.currentTimeMillis())
                                onBack()
                            })
                        }
                    },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val draft = state.draft
        val detail = state.detail
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.editing && draft != null) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { v -> vm.updateDraft { it.copy(name = v) } },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                    isError = draft.name.isBlank(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Section.entries.forEach { section ->
                    SectionCard(section) {
                        OutlinedTextField(
                            value = draft.textOf(section),
                            onValueChange = { v -> vm.updateDraft { it.withText(section, v) } },
                            placeholder = { Text(stringResource(R.string.text_hint)) },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else if (detail != null) {
                val f = detail.figure
                val texts = mapOf(Section.GENERAL to f.generalText, Section.LEADER to f.leaderText, Section.FOLLOWER to f.followerText)
                // Ein Bereich ohne Text und ohne Medien wird nicht angezeigt.
                val visible = Section.entries.filter { s -> texts[s]!!.isNotBlank() || detail.media.any { it.section == s } }
                if (visible.isEmpty()) {
                    Text(stringResource(R.string.figure_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                visible.forEach { section ->
                    SectionCard(section) {
                        if (texts[section]!!.isNotBlank()) Text(texts[section]!!, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun FigurDraft.textOf(s: Section) = when (s) {
    Section.GENERAL -> generalText
    Section.LEADER -> leaderText
    Section.FOLLOWER -> followerText
}

private fun FigurDraft.withText(s: Section, v: String) = when (s) {
    Section.GENERAL -> copy(generalText = v)
    Section.LEADER -> copy(leaderText = v)
    Section.FOLLOWER -> copy(followerText = v)
}

@Composable
fun sectionTitle(s: Section): String = stringResource(
    when (s) {
        Section.GENERAL -> R.string.section_general
        Section.LEADER -> R.string.section_leader
        Section.FOLLOWER -> R.string.section_follower
    },
)

@Composable
private fun sectionColors(s: Section): Pair<Color, Color> = with(MaterialTheme.colorScheme) {
    when (s) {
        Section.GENERAL -> primaryContainer to primary
        Section.LEADER -> secondaryContainer to secondary
        Section.FOLLOWER -> tertiaryContainer to tertiary
    }
}

/** Ein farbig abgesetzter Bereich (Allgemein, Leader oder Follower). */
@Composable
fun SectionCard(section: Section, content: @Composable () -> Unit) {
    val (container, accent) = sectionColors(section)
    Surface(color = container, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                sectionTitle(section),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = accent,
            )
            content()
        }
    }
}
