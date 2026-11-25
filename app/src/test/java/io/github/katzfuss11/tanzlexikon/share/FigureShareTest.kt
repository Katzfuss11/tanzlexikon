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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import android.app.Application
import android.content.Intent
import android.net.Uri

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
        val (manifest, files) = read(share { true }.writeFigureFile(d, withMedia = true, shrink = true) { _, _ -> })

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
        val (manifest, files) = read(share { false }.writeFigureFile(d, withMedia = true, shrink = true) { _, _ -> })

        val media = manifest.categories[0].dances[0].figures[0].media
        assertArrayEquals(video, files[media[0].file])
        assertEquals(video.size.toLong(), media[0].sizeBytes)
    }

    @Test fun shareAsFile_manifestHasNoLinkData() = runBlocking {
        val d = detail().let { it.copy(figure = it.figure.copy(linkGroup = 5, askedName = "fächer")) }
        val (manifest, _) = read(share { true }.writeFigureFile(d, withMedia = true, shrink = false) { _, _ -> })

        val f = manifest.categories[0].dances[0].figures[0]
        assertEquals(null, f.linkGroup)
        assertEquals(null, f.key)
        assertEquals(null, f.askedName)
        assertEquals(emptyList<Any>(), manifest.links)
    }

    @Test fun writeFigureFile_withoutShrink_packsOriginalUntouched() = runBlocking {
        val d = detail()
        val (manifest, files) = read(share { true }.writeFigureFile(d, withMedia = true, shrink = false) { _, _ -> })

        assertEquals(emptyList<String>(), shrunkSources)
        assertArrayEquals(video, files[manifest.categories[0].dances[0].figures[0].media[0].file])
    }

    @Test fun writeFigureFile_withoutMedia_keepsOnlyYouTubeLinks() = runBlocking {
        val d = detail()
        val (manifest, files) = read(share { true }.writeFigureFile(d, withMedia = false, shrink = true) { _, _ -> })

        val media = manifest.categories[0].dances[0].figures[0].media
        assertEquals(listOf(MediaKind.YOUTUBE), media.map { it.kind })
        assertEquals("https://youtu.be/dQw4w9WgXcQ", media[0].youtubeUrl)
        assertEquals(emptyMap<String, ByteArray>(), files)
        assertEquals(emptyList<String>(), shrunkSources)
    }

    private fun sentIntent(): Intent {
        val chooser = shadowOf(context as Application).nextStartedActivity
        @Suppress("DEPRECATION")
        return chooser.getParcelableExtra(Intent.EXTRA_INTENT)!!
    }

    @Test fun shareAsMessage_withoutMedia_sendsOnlyText() = runBlocking {
        share { true }.shareAsMessage(detail(), withMedia = false)

        val sent = sentIntent()
        assertEquals(Intent.ACTION_SEND, sent.action)
        assertEquals("text/plain", sent.type)
        assertNull(sent.extras?.get(Intent.EXTRA_STREAM))
        // Android legt den Text selbst als ClipData ab – Dateien dürfen darin aber keine stecken.
        val clip = sent.clipData
        assertTrue(clip == null || (0 until clip.itemCount).all { clip.getItemAt(it).uri == null })
        assertTrue(sent.getStringExtra(Intent.EXTRA_TEXT)!!.contains("https://youtu.be/dQw4w9WgXcQ"))
    }

    @Test fun shareAsMessage_withMedia_attachesVideoAndImage() = runBlocking {
        val d = detail()
        share { true }.shareAsMessage(d, withMedia = true)

        val sent = sentIntent()
        assertEquals(Intent.ACTION_SEND_MULTIPLE, sent.action)
        @Suppress("DEPRECATION")
        val streams = sent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)!!.map { it.toString() }
        assertEquals(listOf(d.media[0].uri, d.media[1].uri), streams)
    }

    @Test fun writeFigureFile_neverContainsNote() = runBlocking {
        val d = detail().let { it.copy(figure = it.figure.copy(noteText = "GEHEIM-123")) }
        for (withMedia in listOf(true, false)) {
            val file = share { true }.writeFigureFile(d, withMedia = withMedia, shrink = false) { _, _ -> }
            val (manifest, _) = read(file)
            assertEquals("", manifest.categories[0].dances[0].figures[0].noteText)
            val rawJson = java.util.zip.ZipFile(file).use { z -> z.getInputStream(z.getEntry("tanzlexikon.json")).readBytes().decodeToString() }
            assertFalse(rawJson.contains("GEHEIM-123"))
        }
    }
}
