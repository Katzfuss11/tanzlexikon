package io.github.katzfuss11.tanzlexikon.share

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.katzfuss11.tanzlexikon.data.FigureEntity
import io.github.katzfuss11.tanzlexikon.data.MediaEntity
import io.github.katzfuss11.tanzlexikon.data.MediaKind
import io.github.katzfuss11.tanzlexikon.data.Section
import io.github.katzfuss11.tanzlexikon.lexikon.FigureDetail
import io.github.katzfuss11.tanzlexikon.media.MediaChecker
import io.github.katzfuss11.tanzlexikon.media.VideoShrinker
import io.github.katzfuss11.tanzlexikon.testutil.FakeGalleryProvider
import io.github.katzfuss11.tanzlexikon.transfer.ArchiveReader
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FigureShareTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var gallery: FakeGalleryProvider
    private val video = ByteArray(8000) { (it % 251).toByte() }
    private val image = ByteArray(3000) { (it % 13).toByte() }
    private val small = "verkleinert".toByteArray()
    private val shrunkSources = mutableListOf<String>()

    @Before fun setUp() {
        gallery = Robolectric.setupContentProvider(FakeGalleryProvider::class.java, "media")
    }

    private fun detail(): FigureDetail {
        val v = gallery.add("tanz.mp4", video)
        val i = gallery.add("foto.jpg", image, collection = "images")
        fun m(kind: MediaKind, uri: String, name: String?, size: Long?, mime: String?, pos: Int) =
            MediaEntity(figureId = 1, section = Section.GENERAL, kind = kind, uri = uri, displayName = name,
                mimeType = mime, sizeBytes = size, position = pos)
        return FigureDetail(
            FigureEntity(id = 1, danceId = 1, name = "Fächer", position = 0),
            "Rumba", "Latein",
            listOf(
                m(MediaKind.VIDEO, v.toString(), "tanz.mp4", video.size.toLong(), "video/mp4", 0),
                m(MediaKind.IMAGE, i.toString(), "foto.jpg", image.size.toLong(), "image/jpeg", 1),
                m(MediaKind.YOUTUBE, "https://youtu.be/dQw4w9WgXcQ", null, null, null, 2),
            ),
        )
    }

    private fun share(result: (ByteArray) -> Boolean) = FigureShare(
        context, MediaChecker(context.contentResolver),
        shrinker = VideoShrinker { source, target, _ ->
            shrunkSources += source.toString()
            val bytes = small.takeIf { result(it) }
            if (bytes != null) target.writeBytes(bytes)
            bytes != null
        },
        minShrinkBytes = 0,
    )

    private fun read(file: java.io.File): Pair<io.github.katzfuss11.tanzlexikon.transfer.ArchiveManifest, Map<String, ByteArray>> {
        val files = mutableMapOf<String, ByteArray>()
        val manifest = file.inputStream().use { ArchiveReader().read(it) { p, d -> files[p] = d.readBytes() } }
        return manifest to files
    }

    @Test fun writeFigureFile_withShrink_packsSmallVideoAndOriginalImage() = runBlocking {
        val d = detail()
        val (manifest, files) = read(share { true }.writeFigureFile(d, shrink = true) { _, _ -> })

        assertEquals(listOf(d.media[0].uri), shrunkSources)
        val media = manifest.categories[0].dances[0].figures[0].media
        assertArrayEquals(small, files[media[0].file])
        assertEquals(small.size.toLong(), media[0].sizeBytes)
        assertEquals("video/mp4", media[0].mimeType)
        assertArrayEquals(image, files[media[1].file])
        assertEquals("https://youtu.be/dQw4w9WgXcQ", media[2].youtubeUrl)
    }

    @Test fun writeFigureFile_shrinkFails_packsOriginal() = runBlocking {
        val d = detail()
        val (manifest, files) = read(share { false }.writeFigureFile(d, shrink = true) { _, _ -> })

        val media = manifest.categories[0].dances[0].figures[0].media
        assertArrayEquals(video, files[media[0].file])
        assertEquals(video.size.toLong(), media[0].sizeBytes)
    }

    @Test fun shareAsFile_manifestHasNoLinkData() = runBlocking {
        val d = detail().let { it.copy(figure = it.figure.copy(linkGroup = 5, askedName = "fächer")) }
        val (manifest, _) = read(share { true }.writeFigureFile(d, shrink = false) { _, _ -> })

        val f = manifest.categories[0].dances[0].figures[0]
        assertEquals(null, f.linkGroup)
        assertEquals(null, f.key)
        assertEquals(null, f.askedName)
        assertEquals(emptyList<Any>(), manifest.links)
    }

    @Test fun writeFigureFile_withoutShrink_packsOriginalUntouched() = runBlocking {
        val d = detail()
        val (manifest, files) = read(share { true }.writeFigureFile(d, shrink = false) { _, _ -> })

        assertEquals(emptyList<String>(), shrunkSources)
        assertArrayEquals(video, files[manifest.categories[0].dances[0].figures[0].media[0].file])
    }
}
