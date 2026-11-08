package io.github.katzfuss11.tanzlexikon.ui.tanz

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.katzfuss11.tanzlexikon.lexikon.FigureSummary
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MediaSummaryTest {
    @Test fun mediaSummaryText_variants() {
        val res = ApplicationProvider.getApplicationContext<Context>().resources
        fun text(v: Int, i: Int, l: Int) = mediaSummaryText(res, FigureSummary(1, "X", v, i, l))
        assertEquals("nur Text", text(0, 0, 0))
        assertEquals("2 Videos", text(2, 0, 0))
        assertEquals("1 Video, 1 Link", text(1, 0, 1))
        assertEquals("1 Bild", text(0, 1, 0))
        assertEquals("3 Bilder, 2 Links", text(0, 3, 2))
    }
}
