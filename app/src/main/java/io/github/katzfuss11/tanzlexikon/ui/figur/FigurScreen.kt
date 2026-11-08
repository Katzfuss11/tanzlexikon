package io.github.katzfuss11.tanzlexikon.ui.figur

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import io.github.katzfuss11.tanzlexikon.TanzlexikonApp
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.katzfuss11.tanzlexikon.R
import io.github.katzfuss11.tanzlexikon.data.MediaEntity
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

    // Teilen: erst die Wahl "Als Nachricht" / "Als Datei", beim Packen ein Fortschritt.
    val app = LocalContext.current.applicationContext as TanzlexikonApp
    val scope = rememberCoroutineScope()
    var sharing by remember { mutableStateOf(false) }
    var packing by remember { mutableStateOf(false) }
    var packProgress by remember { mutableStateOf(0f) }
    if (sharing) {
        AlertDialog(
            onDismissRequest = { sharing = false },
            title = { Text(stringResource(R.string.share)) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.share_as_message)) },
                        supportingContent = { Text(stringResource(R.string.share_as_message_hint)) },
                        modifier = Modifier.clickable {
                            sharing = false
                            state.detail?.let { d -> scope.launch { app.figureShare.shareAsMessage(d) } }
                        },
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.share_as_file)) },
                        supportingContent = { Text(stringResource(R.string.share_as_file_hint)) },
                        modifier = Modifier.clickable {
                            sharing = false
                            state.detail?.let { d ->
                                packing = true
                                packProgress = 0f
                                scope.launch {
                                    try {
                                        app.figureShare.shareAsFile(d) { done, total ->
                                            packProgress = if (total > 0) done.toFloat() / total else 0f
                                        }
                                    } catch (e: Exception) {
                                        snackbar.showSnackbar(e.message ?: e.javaClass.simpleName)
                                    } finally {
                                        packing = false
                                    }
                                }
                            }
                        },
                    )
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { sharing = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (packing) {
        AlertDialog(
            onDismissRequest = {},
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.packing))
                    LinearProgressIndicator(progress = { packProgress }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {},
        )
    }

    val errorText = state.error?.let { stringResource(it) }
    LaunchedEffect(errorText) {
        if (errorText != null) {
            snackbar.showSnackbar(errorText)
            vm.errorShown()
        }
    }
    // Beim Bearbeiten wirkt die Zurück-Taste wie "Abbrechen".
    BackHandler(enabled = state.editing) { vm.cancelEditing() }

    // Wohin das nächste ausgewählte Medium gehört: in einen Bereich oder als Ersatz für ein fehlendes.
    var addTarget by rememberSaveable { mutableStateOf<Section?>(null) }
    var relinkTarget by remember { mutableStateOf<MediaEntity?>(null) }
    var youtubeFor by rememberSaveable { mutableStateOf<Section?>(null) }
    val adder = rememberMediaAdder { picked ->
        val relink = relinkTarget
        if (relink != null) {
            picked.firstOrNull()?.let { vm.relinkMedia(relink, it) }
            relinkTarget = null
        } else {
            addTarget?.let { vm.addMedia(it, picked) }
        }
    }
    youtubeFor?.let { section ->
        YouTubeDialog(
            onDismiss = { youtubeFor = null },
            onConfirm = {
                vm.addMedia(section, listOf(it))
                youtubeFor = null
            },
        )
    }

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
                        IconButton(onClick = { sharing = true }, enabled = state.detail != null && !packing) {
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
                        MediaSection(
                            media = draft.media.filter { it.section == section },
                            editing = true,
                            onRemove = vm::removeMedia,
                            onRelink = {
                                relinkTarget = it
                                adder.pickOne()
                            },
                        )
                        AddMediaButton(
                            onRecord = { addTarget = section; adder.recordVideo() },
                            onGallery = { addTarget = section; adder.pickFromGallery() },
                            onYouTube = { youtubeFor = section },
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
                        MediaSection(media = detail.media.filter { it.section == section }, editing = false)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AddMediaButton(onRecord: () -> Unit, onGallery: () -> Unit, onYouTube: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.add_media))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.record_video)) },
                leadingIcon = { Icon(Icons.Default.Videocam, contentDescription = null) },
                onClick = { open = false; onRecord() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.from_gallery)) },
                leadingIcon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                onClick = { open = false; onGallery() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.youtube_link)) },
                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                onClick = { open = false; onYouTube() },
            )
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
