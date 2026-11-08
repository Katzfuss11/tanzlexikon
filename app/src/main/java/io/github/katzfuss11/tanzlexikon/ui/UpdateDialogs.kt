package io.github.katzfuss11.tanzlexikon.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.katzfuss11.tanzlexikon.R
import io.github.katzfuss11.tanzlexikon.TanzlexikonApp
import io.github.katzfuss11.tanzlexikon.update.UpdateState

/** Alle Update-Dialoge, egal ob vom Start oder aus den Einstellungen ausgelöst. */
@Composable
fun UpdateDialogs() {
    val app = LocalContext.current.applicationContext as TanzlexikonApp
    val m = app.updateManager
    val state by m.state.collectAsStateWithLifecycle()
    // Beim ersten Anzeigen einmal still prüfen; der Manager sorgt dafür, dass es pro Start nur einmal passiert.
    LaunchedEffect(Unit) { m.startupCheck() }

    when (val s = state) {
        UpdateState.Idle -> Unit
        UpdateState.Checking -> AlertDialog(
            onDismissRequest = {},
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.update_checking))
                    CircularProgressIndicator()
                }
            },
            confirmButton = {},
        )
        is UpdateState.Offer -> AlertDialog(
            onDismissRequest = m::dismiss,
            title = { Text(stringResource(R.string.update_available, s.release.version.toString())) },
            text = { Text(stringResource(R.string.update_available_text)) },
            confirmButton = { TextButton(onClick = { m.download(s.release) }) { Text(stringResource(R.string.update_download)) } },
            dismissButton = { TextButton(onClick = m::dismiss) { Text(stringResource(R.string.later)) } },
        )
        is UpdateState.Downloading -> AlertDialog(
            onDismissRequest = {},
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.update_downloading))
                    LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {},
        )
        is UpdateState.NeedPermission -> AlertDialog(
            onDismissRequest = m::dismiss,
            title = { Text(stringResource(R.string.update_permission_title)) },
            text = { Text(stringResource(R.string.update_permission_text)) },
            confirmButton = {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    TextButton(onClick = m::openPermissionSettings) { Text(stringResource(R.string.update_open_settings)) }
                    TextButton(onClick = { m.download(s.release) }) { Text(stringResource(R.string.update_retry)) }
                }
            },
            dismissButton = { TextButton(onClick = m::dismiss) { Text(stringResource(R.string.later)) } },
        )
        is UpdateState.UpToDate -> Info(stringResource(R.string.update_up_to_date, s.version), m::dismiss)
        UpdateState.CheckFailed -> Info(stringResource(R.string.update_check_failed), m::dismiss)
        UpdateState.DownloadFailed -> Info(stringResource(R.string.update_download_failed), m::dismiss)
    }
}

@Composable
private fun Info(text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) } },
    )
}
