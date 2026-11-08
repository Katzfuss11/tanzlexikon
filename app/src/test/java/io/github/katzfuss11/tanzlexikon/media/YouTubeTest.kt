package io.github.katzfuss11.tanzlexikon.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YouTubeTest {
    @Test fun recognisesCommonForms() {
        assertEquals("dQw4w9WgXcQ", youtubeVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=42s"))
        assertEquals("dQw4w9WgXcQ", youtubeVideoId("youtu.be/dQw4w9WgXcQ?si=abc"))
        assertEquals("dQw4w9WgXcQ", youtubeVideoId("https://youtube.com/shorts/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", youtubeVideoId("https://m.youtube.com/watch?feature=share&v=dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", youtubeVideoId("  http://youtube.com/watch?v=dQw4w9WgXcQ  "))
    }

    @Test fun rejectsEverythingElse() {
        assertNull(youtubeVideoId("https://vimeo.com/123"))
        assertNull(youtubeVideoId(""))
        assertNull(youtubeVideoId("https://www.youtube.com/"))
        assertNull(youtubeVideoId("https://notyoutube.com/watch?v=dQw4w9WgXcQ"))
        assertNull(youtubeVideoId("Fächer Rumba"))
    }

    @Test fun thumbnailUrl() {
        assertEquals("https://img.youtube.com/vi/abc/hqdefault.jpg", youtubeThumbnailUrl("abc"))
    }
}
