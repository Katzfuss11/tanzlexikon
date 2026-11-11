package io.github.katzfuss11.tanzlexikon.ui.versionen

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.katzfuss11.tanzlexikon.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VersionsverlaufTest {
    @Test fun parse_readsVersionsDatesAndPoints() {
        val text = """
            # Versionsverlauf
            Kommentarzeile, wird ignoriert.

            ## 0.2.0
            - Neu: etwas
              das weitergeht
            - Zweiter Punkt

            ## 0.1.0 · 10.11.2026
            - Erste Version
        """.trimIndent()

        val versions = parseChangelog(text)

        assertEquals(
            listOf(
                VersionEntry("0.2.0", null, listOf("Neu: etwas das weitergeht", "Zweiter Punkt")),
                VersionEntry("0.1.0", "10.11.2026", listOf("Erste Version")),
            ),
            versions,
        )
    }

    /** Wer die Versionsnummer erhöht, muss auch den Versionsverlauf ergänzen – sonst schlägt dieser Test fehl. */
    @Test fun bundledChangelog_startsWithInstalledVersion_andEveryVersionHasPoints() {
        val versions = loadChangelog(ApplicationProvider.getApplicationContext<Context>())

        assertEquals(BuildConfig.VERSION_NAME, versions.first().version)
        assertTrue(versions.all { it.points.isNotEmpty() })
        assertEquals(versions.size, versions.map { it.version }.distinct().size)
    }
}
