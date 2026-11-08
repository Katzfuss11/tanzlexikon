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
import java.io.File
import org.robolectric.Robolectric
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

    /**
     * Eine Galerie-Datei im Papierkorb lässt sich noch öffnen, taucht bei einer normalen Abfrage aber
     * nicht mehr auf. Sie muss als "nicht gefunden" gelten.
     */
    @Test fun mediaChecker_fileInGalleryTrash_returnsFalse() {
        Robolectric.setupContentProvider(FakeMediaProvider::class.java, "media")
        assertTrue(checker.isAvailable(media(MediaKind.VIDEO, "content://media/external/video/media/1")))
        assertFalse(checker.isAvailable(media(MediaKind.VIDEO, "content://media/external/video/media/${FakeMediaProvider.TRASHED_ID}")))
    }
}

/** Tut so, als wäre es die Galerie: Datei 7 liegt im Papierkorb (öffnen geht, Abfrage liefert nichts). */
class FakeMediaProvider : ContentProvider() {
    companion object { const val TRASHED_ID = 7L }

    override fun onCreate() = true
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, sort: String?): Cursor {
        val cursor = MatrixCursor(arrayOf(MediaStore.MediaColumns._ID))
        val id = ContentUris.parseId(uri)
        if (id != TRASHED_ID) cursor.addRow(arrayOf(id))
        return cursor
    }
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val f = File.createTempFile("video", ".mp4", context!!.cacheDir)
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)
    }
    override fun getType(uri: Uri) = "video/mp4"
    override fun insert(uri: Uri, values: ContentValues?) = null
    override fun delete(uri: Uri, selection: String?, args: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?) = 0
}
