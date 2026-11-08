package io.github.katzfuss11.tanzlexikon.media

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.InputStream

@RunWith(RobolectricTestRunner::class)
class GalleryWriterTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var provider: FakeGalleryProvider

    @Before fun setUp() {
        provider = Robolectric.setupContentProvider(FakeGalleryProvider::class.java, "media")
    }

    @Test fun galleryWriter_sameNameAndSize_returnsExistingUri_withoutReading() {
        provider.existing += Triple(42L, "fächer.mp4", 1234L)
        val failing = object : InputStream() {
            override fun read(): Int = throw AssertionError("Stream darf nicht gelesen werden")
        }
        val uri = GalleryWriter(context.contentResolver).save("fächer.mp4", "video/mp4", 1234, failing)
        assertEquals(42L, ContentUris.parseId(uri))
        assertEquals(0, provider.inserted.size)
    }

    @Test fun galleryWriter_sameNameOtherSize_insertsNewFileIntoTanzlexikonFolder() {
        provider.existing += Triple(42L, "fächer.mp4", 999L)
        val bytes = ByteArray(1234) { it.toByte() }
        val uri = GalleryWriter(context.contentResolver).save("fächer.mp4", "video/mp4", 1234, bytes.inputStream())

        assertEquals(FakeGalleryProvider.NEW_ID, ContentUris.parseId(uri))
        val values = provider.inserted.single()
        assertEquals("fächer.mp4", values.getAsString(MediaStore.MediaColumns.DISPLAY_NAME))
        assertEquals("Movies/Tanzlexikon", values.getAsString(MediaStore.MediaColumns.RELATIVE_PATH))
        assertEquals(0, provider.lastPending)
        assertArrayEquals(bytes, provider.written!!.readBytes())
    }
}

/** Eine winzige Galerie zum Testen: kennt vorhandene Dateien, nimmt neue entgegen. */
class FakeGalleryProvider : ContentProvider() {
    companion object { const val NEW_ID = 100L }

    val existing = mutableListOf<Triple<Long, String, Long>>()
    val inserted = mutableListOf<ContentValues>()
    var lastPending: Int? = null
    var written: File? = null

    override fun onCreate() = true

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, sort: String?): Cursor {
        val cursor = MatrixCursor(arrayOf(MediaStore.MediaColumns._ID))
        val name = args?.getOrNull(0)
        val size = args?.getOrNull(1)?.toLongOrNull()
        existing.filter { it.second == name && it.third == size }.forEach { cursor.addRow(arrayOf(it.first)) }
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        inserted += ContentValues(values)
        return ContentUris.withAppendedId(uri, NEW_ID)
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val f = written ?: File.createTempFile("gallery", ".bin", context!!.cacheDir).also { written = it }
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.parseMode(mode))
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?): Int {
        values?.getAsInteger(MediaStore.MediaColumns.IS_PENDING)?.let { lastPending = it }
        return 1
    }

    override fun getType(uri: Uri): String? = null
    override fun delete(uri: Uri, selection: String?, args: Array<out String>?) = 0
}
