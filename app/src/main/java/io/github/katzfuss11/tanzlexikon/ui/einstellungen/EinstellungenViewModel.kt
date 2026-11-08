package io.github.katzfuss11.tanzlexikon.ui.einstellungen

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.katzfuss11.tanzlexikon.transfer.BackupService
import io.github.katzfuss11.tanzlexikon.transfer.ImportOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

/** Was der Einstellungen-Bildschirm gerade zeigt. */
sealed interface BackupState {
    data object Idle : BackupState
    data class Running(val importing: Boolean, val progress: Float) : BackupState
    data object ExportDone : BackupState
    data class ImportDone(val outcome: ImportOutcome) : BackupState
    data class Error(val message: String) : BackupState
}

class EinstellungenViewModel(
    private val backup: BackupService,
    private val resolver: ContentResolver,
) : ViewModel() {
    private val _state = MutableStateFlow<BackupState>(BackupState.Idle)
    val state: StateFlow<BackupState> = _state.asStateFlow()

    fun export(target: Uri) = run(importing = false) {
        val out = resolver.openOutputStream(target) ?: throw IOException("Ziel nicht beschreibbar")
        out.use { backup.export(it, ::progress) }
        BackupState.ExportDone
    }

    fun import(source: Uri) = run(importing = true) {
        val outcome = backup.import({ resolver.openInputStream(source) ?: throw IOException("Datei nicht lesbar") }, ::progress)
        BackupState.ImportDone(outcome)
    }

    fun dismiss() {
        _state.value = BackupState.Idle
    }

    private fun progress(done: Long, total: Long) {
        val s = _state.value
        if (s is BackupState.Running) _state.value = s.copy(progress = if (total > 0) done.toFloat() / total else 0f)
    }

    private fun run(importing: Boolean, block: suspend () -> BackupState) {
        if (_state.value is BackupState.Running) return
        _state.value = BackupState.Running(importing, 0f)
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = try {
                block()
            } catch (e: Exception) {
                BackupState.Error(e.message ?: e.javaClass.simpleName)
            }
        }
    }
}
