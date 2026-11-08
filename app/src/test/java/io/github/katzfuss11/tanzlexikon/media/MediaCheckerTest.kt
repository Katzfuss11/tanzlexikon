package io.github.katzfuss11.tanzlexikon.media

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.katzfuss11.tanzlexikon.data.MediaEntity
import io.github.katzfuss11.tanzlexikon.data.MediaKind
import io.github.katzfuss11.tanzlexikon.data.Section
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MediaCheckerTest {
    private val checker = MediaChecker(ApplicationProvider.getApplicationContext<Context>().contentResolver)

    private fun media(kind: MediaKind, uri: String) = MediaEntity(
        figureId = 1, section = Section.GENERAL, kind = kind, uri = uri,
        displayName = null, mimeType = null, sizeBytes = null, position = 0,
    )

    @Test fun mediaChecker_missingContentUri_returnsFalse_noCrash() {
        assertFalse(checker.isAvailable(media(MediaKind.VIDEO, "content://media/external/video/media/999999")))
        assertFalse(checker.isAvailable(media(MediaKind.IMAGE, "kaputt")))
    }

    @Test fun mediaChecker_youtube_alwaysTrue() {
        assertTrue(checker.isAvailable(media(MediaKind.YOUTUBE, "https://youtu.be/dQw4w9WgXcQ")))
    }
}
