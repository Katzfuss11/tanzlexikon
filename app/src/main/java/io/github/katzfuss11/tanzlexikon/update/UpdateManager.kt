package io.github.katzfuss11.tanzlexikon.update

import io.github.katzfuss11.tanzlexikon.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Was die Update-Dialoge gerade zeigen. */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class Offer(val release: Release) : UpdateState
    data class Downloading(val release: Release, val progress: Float) : UpdateState
    data class NeedPermission(val release: Release) : UpdateState
    data class UpToDate(val version: String) : UpdateState
    data object CheckFailed : UpdateState
    data object DownloadFailed : UpdateState
}

/**
 * Steuert Prüfen, Herunterladen und Installieren. Lebt in der App, damit der Hinweis beim Start
 * und der Knopf in den Einstellungen dieselben Dialoge benutzen.
 */
class UpdateManager(
    private val checker: suspend () -> UpdateResult,
    private val installer: ApkInstaller,
    private val scope: CoroutineScope,
    private val current: String = BuildConfig.VERSION_NAME,
) {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()
    private var startupDone = false

    /** Einmal pro Start: meldet sich nur, wenn es wirklich eine neue Version gibt. */
    fun startupCheck() {
        if (startupDone) return
        startupDone = true
        scope.launch {
            val r = checker()
            if (r is UpdateResult.Available && _state.value == UpdateState.Idle) _state.value = UpdateState.Offer(r.release)
        }
    }

    /** Der Knopf in den Einstellungen: meldet jedes Ergebnis. */
    fun manualCheck() {
        if (_state.value == UpdateState.Checking || _state.value is UpdateState.Downloading) return
        _state.value = UpdateState.Checking
        scope.launch {
            _state.value = when (val r = checker()) {
                is UpdateResult.Available -> UpdateState.Offer(r.release)
                UpdateResult.UpToDate -> UpdateState.UpToDate(current)
                UpdateResult.Failed -> UpdateState.CheckFailed
            }
        }
    }

    /** Lädt die neue Version und öffnet das Android-Installationsfenster. */
    fun download(release: Release) {
        if (!installer.canInstall()) {
            _state.value = UpdateState.NeedPermission(release)
            return
        }
        _state.value = UpdateState.Downloading(release, 0f)
        scope.launch {
            _state.value = try {
                val apk = installer.download(release) { done, total ->
                    _state.value = UpdateState.Downloading(release, if (total > 0) done.toFloat() / total else 0f)
                }
                installer.install(apk)
                UpdateState.Idle
            } catch (e: Exception) {
                UpdateState.DownloadFailed
            }
        }
    }

    fun openPermissionSettings() = installer.openPermissionSettings()

    fun dismiss() {
        _state.value = UpdateState.Idle
    }
}
