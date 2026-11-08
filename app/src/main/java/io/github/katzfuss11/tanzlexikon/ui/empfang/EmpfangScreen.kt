package io.github.katzfuss11.tanzlexikon.ui.empfang

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.katzfuss11.tanzlexikon.R
import io.github.katzfuss11.tanzlexikon.data.MediaKind
import io.github.katzfuss11.tanzlexikon.data.Section
import io.github.katzfuss11.tanzlexikon.share.ImportTarget
import io.github.katzfuss11.tanzlexikon.transfer.ArchiveError
import io.github.katzfuss11.tanzlexikon.ui.BackButton
import io.github.katzfuss11.tanzlexikon.ui.appViewModel
import io.github.katzfuss11.tanzlexikon.ui.figur.SectionCard
import io.github.katzfuss11.tanzlexikon.ui.figur.sectionTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmpfangScreen(
    uri: Uri,
    onClose: () -> Unit,
    onAdded: (Long) -> Unit,
    vm: EmpfangViewModel = appViewModel(key = "empfang$uri") { app ->
        EmpfangViewModel(app.repository, app.galleryWriter) {
            app.contentResolver.openInputStream(uri) ?: throw java.io.IOException("Datei nicht lesbar")
        }
    },
) {
    val s by vm.state.collectAsStateWithLifecycle()
    var choosing by rememberSaveable { mutableStateOf(false) }
    var creatingNew by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(s.addedFigureId) { s.addedFigureId?.let(onAdded) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.receive_title)) }, navigationIcon = { BackButton(onClose) }) },
        bottomBar = {
            if (s.figure != null && s.error == null) {
                Surface(tonalElevation = 3.dp) {
                    Row(
                        // Die App zeichnet bis zum Rand; die Knöpfe gehören über die Android-Navigationsleiste.
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    ) {
                        OutlinedButton(onClick = onClose) { Text(stringResource(R.string.cancel)) }
                        Button(onClick = vm::add, enabled = !s.adding && s.figureName.isNotBlank()) {
                            Text(stringResource(R.string.add_figure))
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val error = s.error
            val figure = s.figure
            when {
                s.loading || s.adding -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                error != null || s.notAFigure -> ErrorMessage(
                    text = stringResource(
                        when (error) {
                            null -> R.string.error_not_a_figure
                            ArchiveError.NOT_TANZLEXIKON -> R.string.error_not_a_figure
                            ArchiveError.NEWER_FORMAT -> R.string.error_newer_format
                            ArchiveError.CORRUPT -> R.string.error_corrupt
                        },
                    ),
                    onClose = onClose,
                )
                figure != null -> Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = s.figureName,
                        onValueChange = vm::rename,
                        label = { Text(stringResource(R.string.name)) },
                        singleLine = true,
                        isError = s.duplicateName || s.figureName.isBlank(),
                        supportingText = { if (s.duplicateName) Text(stringResource(R.string.receive_duplicate)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            when (val t = s.target) {
                                is ImportTarget.Existing -> stringResource(R.string.receive_target, t.categoryName, t.danceName)
                                is ImportTarget.New -> stringResource(R.string.receive_target_new, t.categoryName, t.danceName)
                                null -> ""
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { choosing = true }) { Text(stringResource(R.string.change)) }
                    }
                    HorizontalDivider()
                    val texts = mapOf(Section.GENERAL to figure.generalText, Section.LEADER to figure.leaderText, Section.FOLLOWER to figure.followerText)
                    Section.entries.filter { texts[it]!!.isNotBlank() }.forEach { section ->
                        SectionCard(section) { Text(texts[section]!!) }
                    }
                    if (figure.media.isNotEmpty()) {
                        Text(stringResource(R.string.receive_media), style = MaterialTheme.typography.titleSmall)
                        figure.media.forEach { m ->
                            val name = when (m.kind) {
                                MediaKind.YOUTUBE -> m.youtubeUrl.orEmpty()
                                else -> m.displayName ?: m.file.orEmpty()
                            }
                            ListItem(
                                leadingContent = {
                                    Icon(
                                        when (m.kind) {
                                            MediaKind.VIDEO -> Icons.Default.Videocam
                                            MediaKind.IMAGE -> Icons.Default.Image
                                            MediaKind.YOUTUBE -> Icons.Default.Link
                                        },
                                        contentDescription = null,
                                    )
                                },
                                headlineContent = {
                                    Text(if (m.kind != MediaKind.YOUTUBE && m.file == null) stringResource(R.string.receive_media_missing, name) else name)
                                },
                                supportingContent = { Text(sectionTitle(m.section)) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (choosing) {
        AlertDialog(
            onDismissRequest = { choosing = false },
            title = { Text(stringResource(R.string.choose_dance)) },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.new_dance_ellipsis)) },
                        modifier = Modifier.clickable { choosing = false; creatingNew = true },
                    )
                    s.categories.forEach { c ->
                        Text(
                            c.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                        )
                        c.dances.forEach { d ->
                            ListItem(
                                headlineContent = { Text(d.name) },
                                modifier = Modifier.clickable { vm.chooseExisting(d.id); choosing = false },
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { choosing = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    if (creatingNew) {
        val suggestion = s.target
        var dance by rememberSaveable { mutableStateOf((suggestion as? ImportTarget.New)?.danceName ?: "") }
        var category by rememberSaveable {
            mutableStateOf(
                when (suggestion) {
                    is ImportTarget.New -> suggestion.categoryName
                    is ImportTarget.Existing -> suggestion.categoryName
                    null -> ""
                },
            )
        }
        AlertDialog(
            onDismissRequest = { creatingNew = false },
            title = { Text(stringResource(R.string.new_dance)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = dance, onValueChange = { dance = it }, label = { Text(stringResource(R.string.dance_name)) }, singleLine = true)
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text(stringResource(R.string.category)) }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { vm.chooseNew(category, dance); creatingNew = false },
                    enabled = dance.isNotBlank() && category.isNotBlank(),
                ) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { creatingNew = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun ErrorMessage(text: String, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.width(0.dp).padding(8.dp))
        Button(onClick = onClose) { Text(stringResource(R.string.close)) }
    }
}
