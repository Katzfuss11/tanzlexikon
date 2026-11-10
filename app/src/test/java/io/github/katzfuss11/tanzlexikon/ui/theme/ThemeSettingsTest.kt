package io.github.katzfuss11.tanzlexikon.ui.theme

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThemeSettingsTest {
    private val prefs = ApplicationProvider.getApplicationContext<Context>()
        .getSharedPreferences("test_theme", Context.MODE_PRIVATE)
        .also { it.edit().clear().commit() }

    @Test fun default_followsPhone() {
        assertEquals(ThemeMode.SYSTEM, ThemeSettings(prefs).mode.value)
    }

    @Test fun choice_isKeptForNextStart() {
        ThemeSettings(prefs).set(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, ThemeSettings(prefs).mode.value)
    }

    @Test fun choice_updatesFlowImmediately() {
        val s = ThemeSettings(prefs)
        s.set(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, s.mode.value)
    }

    @Test fun unknownStoredValue_fallsBackToPhone() {
        prefs.edit().putString("theme_mode", "LILA").commit()
        assertEquals(ThemeMode.SYSTEM, ThemeSettings(prefs).mode.value)
    }

    @Test fun choice_isPassedToSystem_onStartAndOnChange() {
        prefs.edit().putString("theme_mode", "DARK").commit()
        val applied = mutableListOf<ThemeMode>()
        val s = ThemeSettings(prefs, applyToSystem = { applied += it })
        s.set(ThemeMode.LIGHT)
        assertEquals(listOf(ThemeMode.DARK, ThemeMode.LIGHT), applied)
    }
}
