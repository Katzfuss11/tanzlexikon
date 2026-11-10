package io.github.katzfuss11.tanzlexikon.ui.theme

import android.app.UiModeManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Hell, dunkel oder wie am Handy eingestellt. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Merkt sich die gewählte Darstellung auf diesem Gerät (nicht Teil von Export oder Teilen).
 * [applyToSystem] gibt die Wahl an Android weiter, damit schon das Startbild die richtige Farbe hat.
 */
class ThemeSettings(
    private val prefs: SharedPreferences,
    private val applyToSystem: (ThemeMode) -> Unit = {},
) {
    private val _mode = MutableStateFlow(read())
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    init {
        applyToSystem(_mode.value)
    }

    fun set(mode: ThemeMode) {
        prefs.edit().putString(KEY, mode.name).apply()
        _mode.value = mode
        applyToSystem(mode)
    }

    private fun read(): ThemeMode =
        ThemeMode.entries.firstOrNull { it.name == prefs.getString(KEY, null) } ?: ThemeMode.SYSTEM

    companion object {
        private const val KEY = "theme_mode"

        fun create(context: Context): ThemeSettings = ThemeSettings(
            prefs = context.getSharedPreferences("einstellungen", Context.MODE_PRIVATE),
            applyToSystem = { mode ->
                // Ab Android 12 merkt sich das System die Wahl der App und zeichnet das Startbild passend.
                if (Build.VERSION.SDK_INT >= 31) {
                    context.getSystemService(UiModeManager::class.java)?.setApplicationNightMode(
                        when (mode) {
                            ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
                            ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
                            ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
                        },
                    )
                }
            },
        )
    }
}
