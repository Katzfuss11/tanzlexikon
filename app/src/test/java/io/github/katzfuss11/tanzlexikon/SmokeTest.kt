package io.github.katzfuss11.tanzlexikon

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SmokeTest {
    @Test
    fun appNameIsTanzlexikon() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Tanzlexikon", ctx.getString(R.string.app_name))
    }
}
