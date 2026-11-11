package io.github.katzfuss11.tanzlexikon.ui.start

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Was die Startseite zeigt: Kategorien mit Tänzen oder eine Liste aller Figuren. */
enum class StartView { BY_DANCE, ALL_FIGURES }

/** Merkt sich die gewählte Ansicht der Startseite auf diesem Gerät (nicht Teil der Sicherung). */
class StartViewSettings(private val prefs: SharedPreferences) {
    private val _view = MutableStateFlow(read())
    val view: StateFlow<StartView> = _view.asStateFlow()

    fun set(view: StartView) {
        prefs.edit().putString(KEY, view.name).apply()
        _view.value = view
    }

    private fun read(): StartView =
        StartView.entries.firstOrNull { it.name == prefs.getString(KEY, null) } ?: StartView.BY_DANCE

    companion object {
        private const val KEY = "start_ansicht"

        fun create(context: Context) =
            StartViewSettings(context.getSharedPreferences("einstellungen", Context.MODE_PRIVATE))
    }
}
