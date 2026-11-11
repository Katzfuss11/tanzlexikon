package io.github.katzfuss11.tanzlexikon.ui.start

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StartViewSettingsTest {
    private val prefs = ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("test-start", Context.MODE_PRIVATE)

    @Before fun setUp() = prefs.edit().clear().commit().let { }

    @Test fun default_isByDance() {
        assertEquals(StartView.BY_DANCE, StartViewSettings(prefs).view.value)
    }

    @Test fun set_isRememberedAcrossInstances() {
        StartViewSettings(prefs).set(StartView.ALL_FIGURES)
        assertEquals(StartView.ALL_FIGURES, StartViewSettings(prefs).view.value)
    }
}
