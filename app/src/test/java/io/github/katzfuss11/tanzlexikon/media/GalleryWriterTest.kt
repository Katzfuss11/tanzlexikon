package io.github.katzfuss11.tanzlexikon.media

import android.content.Context
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import io.github.katzfuss11.tanzlexikon.testutil.FakeGalleryProvider
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import java.io.InputStream

@RunWith(RobolectricTestRunner::class)
class GalleryWriterTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var provider: FakeGalleryProvider

    @Before fun setUp() {
        provider = Robolectric.setupContentProvider(FakeGalleryProvider::class.java, "media")
    }

    @Test fun galleryWriter_sameNameAndSize_returnsExistingUri_withoutReading() {
        val existing = provider.add("fächer.mp4", ByteArray(1234))
        val failing = object : InputStream() {
            override fun read(): Int = throw AssertionError("Stream darf nicht gelesen werden")
        }
        val uri = GalleryWriter(context.contentResolver).save("fächer.mp4", "video/mp4", 1234, failing)
        assertEquals(existing.lastPathSegment, uri.lastPathSegment)
        assertEquals(0, provider.inserted.size)
    }

    @Test fun galleryWriter_sameNameOtherSize_insertsNewFileIntoTanzlexikonFolder() {
        val existing = provider.add("fächer.mp4", ByteArray(999))
        val bytes = ByteArray(1234) { it.toByte() }
        val uri = GalleryWriter(context.contentResolver).save("fächer.mp4", "video/mp4", 1234, bytes.inputStream())

        assertNotEquals(existing.lastPathSegment, uri.lastPathSegment)
        val values = provider.inserted.single()
        assertEquals("fächer.mp4", values.getAsString(MediaStore.MediaColumns.DISPLAY_NAME))
        assertEquals("Movies/Tanzlexikon", values.getAsString(MediaStore.MediaColumns.RELATIVE_PATH))
        assertEquals(0, provider.lastPending)
        assertArrayEquals(bytes, provider.fileOf(uri)!!.readBytes())
    }

    @Test fun newRecording_createsEmptyVideoInTanzlexikonFolder() {
        val uri = GalleryWriter(context.contentResolver).newRecording()

        val values = provider.inserted.single()
        assertEquals("video/mp4", values.getAsString(MediaStore.MediaColumns.MIME_TYPE))
        assertEquals("Movies/Tanzlexikon", values.getAsString(MediaStore.MediaColumns.RELATIVE_PATH))
        assertTrue(values.getAsString(MediaStore.MediaColumns.DISPLAY_NAME).matches(Regex("""Tanzlexikon_\d{8}_\d{6}\.mp4""")))
        assertNotNull(provider.fileOf(uri))
        assertFalse(GalleryWriter(context.contentResolver).hasContent(uri))
    }

    @Test fun hasContent_trueOnceTheCameraWroteIntoIt() {
        val writer = GalleryWriter(context.contentResolver)
        val uri = writer.newRecording()
        provider.fileOf(uri)!!.writeBytes(ByteArray(10))
        assertTrue(writer.hasContent(uri))
    }

    @Test fun discard_removesTheEntry() {
        val writer = GalleryWriter(context.contentResolver)
        val uri = writer.newRecording()
        writer.discard(uri)
        assertNull(provider.fileOf(uri))
        assertFalse(writer.hasContent(uri))
    }
}
