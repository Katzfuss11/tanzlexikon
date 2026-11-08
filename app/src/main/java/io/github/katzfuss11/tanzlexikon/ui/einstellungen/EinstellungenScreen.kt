package io.github.katzfuss11.tanzlexikon.ui.einstellungen

import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.katzfuss11.tanzlexikon.BuildConfig
import io.github.katzfuss11.tanzlexikon.R
import io.github.katzfuss11.tanzlexikon.transfer.ArchiveError
import io.github.katzfuss11.tanzlexikon.transfer.ImportOutcome
import io.github.katzfuss11.tanzlexikon.ui.BackButton
import io.github.katzfuss11.tanzlexikon.ui.appViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EinstellungenScreen(
    onBack: () -> Unit,
    onTrash: () -> Unit,
    vm: EinstellungenViewModel = appViewModel { EinstellungenViewModel(it.backupService, it.contentResolver) },
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var pendingImport by rememberSaveable { mutableStateOf<String?>(null) }
    val running = state is BackupState.Running

    // Während Export oder Import läuft, bleibt der Bildschirm an.
    val view = LocalView.current
    DisposableEffect(running) {
        view.keepScreenOn = running
        onDispose { view.keepScreenOn = false }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) vm.export(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingImport = uri.toString()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.menu_settings)) }, navigationIcon = { BackButton(onBack) }) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            SectionTitle(stringResource(R.string.settings_backup))
            ListItem(
                headlineContent = { Text(stringResource(R.string.export_all)) },
                supportingContent = { Text(stringResource(R.string.export_all_hint)) },
                modifier = Modifier.clickable(enabled = !running) {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
                    exportLauncher.launch("tanzlexikon-$date.zip")
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.import_all)) },
                supportingContent = { Text(stringResource(R.string.import_all_hint)) },
                modifier = Modifier.clickable(enabled = !running) {
                    importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "application/x-zip-compressed"))
                },
            )
            (state as? BackupState.Running)?.let { r ->
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        stringResource(if (r.importing) R.string.importing else R.string.exporting) + " ${(r.progress * 100).toInt()} %",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    LinearProgressIndicator(progress = { r.progress }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SectionTitle(stringResource(R.string.menu_trash))
            ListItem(
                headlineContent = { Text(stringResource(R.string.menu_trash)) },
                supportingContent = { Text(stringResource(R.string.open_trash_hint)) },
                modifier = Modifier.clickable(onClick = onTrash),
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SectionTitle(stringResource(R.string.settings_about))
            ListItem(headlineContent = { Text(stringResource(R.string.installed_version, BuildConfig.VERSION_NAME)) })
        }
    }

    pendingImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text(stringResource(R.string.import_warning_title)) },
            text = { Text(stringResource(R.string.import_warning_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingImport = null
                        vm.import(android.net.Uri.parse(uri))
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.overwrite)) }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    resultMessage(state)?.let { message ->
        AlertDialog(
            onDismissRequest = vm::dismiss,
            text = { Text(message) },
            confirmButton = { TextButton(onClick = vm::dismiss) { Text(stringResource(R.string.ok)) } },
        )
    }
}

@Composable
private fun resultMessage(state: BackupState): String? {
    val context = LocalContext.current
    return when (state) {
        BackupState.ExportDone -> stringResource(R.string.export_done)
        is BackupState.Error -> stringResource(R.string.error_generic, state.message)
        is BackupState.ImportDone -> when (val o = state.outcome) {
            is ImportOutcome.Success -> buildString {
                append(pluralStringResource(R.plurals.import_done, o.figures, o.figures))
                if (o.missingMedia > 0) append(" ").append(pluralStringResource(R.plurals.import_done_missing, o.missingMedia, o.missingMedia))
            }
            is ImportOutcome.Failed -> stringResource(
                when (o.error) {
                    ArchiveError.NOT_TANZLEXIKON -> R.string.error_not_tanzlexikon
                    ArchiveError.NEWER_FORMAT -> R.string.error_newer_format
                    ArchiveError.CORRUPT -> R.string.error_corrupt
                },
            )
            ImportOutcome.WrongKind -> stringResource(R.string.error_wrong_kind)
            is ImportOutcome.NotEnoughSpace -> stringResource(R.string.error_space, Formatter.formatShortFileSize(context, o.requiredBytes))
        }
        else -> null
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}
