package io.github.katzfuss11.tanzlexikon.share

import io.github.katzfuss11.tanzlexikon.data.FigureEntity
import io.github.katzfuss11.tanzlexikon.data.MediaEntity
import io.github.katzfuss11.tanzlexikon.data.MediaKind
import io.github.katzfuss11.tanzlexikon.data.Section
import io.github.katzfuss11.tanzlexikon.lexikon.FigureDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareTextTest {
    private val labels = ShareLabels("Allgemein", "Leader", "Follower")

    private fun media(section: Section, kind: MediaKind, uri: String) = MediaEntity(
        figureId = 1, section = section, kind = kind, uri = uri,
        displayName = null, mimeType = null, sizeBytes = null, position = 0,
    )

    @Test fun shareText_omitsEmptySections_andListsYoutubeUnderItsSection() {
        val d = FigureDetail(
            figure = FigureEntity(id = 1, danceId = 1, name = "Fächer", position = 0, generalText = "Grundidee", leaderText = "", followerText = ""),
            danceName = "Rumba",
            categoryName = "Latein",
            media = listOf(
                media(Section.LEADER, MediaKind.YOUTUBE, "https://youtu.be/dQw4w9WgXcQ"),
                media(Section.GENERAL, MediaKind.VIDEO, "content://media/external/video/media/1"),
            ),
        )
        val expected = "Rumba – Fächer\n\n" +
            "Allgemein:\nGrundidee\n\n" +
            "Leader:\nhttps://youtu.be/dQw4w9WgXcQ\n\n" +
            "Geteilt mit dem Tanzlexikon"
        assertEquals(expected, buildShareText(d, labels))
    }

    @Test fun shareText_keepsUmlautsAndEmoji() {
        val d = FigureDetail(
            figure = FigureEntity(id = 1, danceId = 1, name = "Schulterdrehung 💃", position = 0, followerText = "Über die Schulter – ölig"),
            danceName = "Wiener Walzer",
            categoryName = "Standard",
            media = emptyList(),
        )
        val text = buildShareText(d, labels)
        assertTrue(text.startsWith("Wiener Walzer – Schulterdrehung 💃\n\nFollower:\nÜber die Schulter – ölig"))
    }
}
