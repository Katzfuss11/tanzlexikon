package io.github.katzfuss11.tanzlexikon.testutil

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import java.io.File

/**
 * Eine kleine Galerie für Tests (Authority "media"): hält Dateien, findet sie nach Name und Größe,
 * nimmt neue entgegen. Mit Robolectric.setupContentProvider(FakeGalleryProvider::class.java, "media").
 */
class FakeGalleryProvider : ContentProvider() {
    data class Item(val id: Long, val name: String, val file: File)

    val items = mutableListOf<Item>()
    val inserted = mutableListOf<ContentValues>()
    var lastPending: Int? = null

    /** Wenn true, lehnt die Galerie neue Videos ab (wie bei vollem Speicher). */
    var failVideoInserts = false
    private var nextId = 100L

    override fun onCreate() = true

    /** Legt eine Datei in die Galerie und gibt ihre Adresse zurück. */
    fun add(name: String, bytes: ByteArray, collection: String = "video"): Uri {
        val id = nextId++
        val f = File.createTempFile("gallery", ".bin", context!!.cacheDir).apply { writeBytes(bytes) }
        items += Item(id, name, f)
        return Uri.parse("content://media/external/$collection/media/$id")
    }

    fun fileOf(uri: Uri): File? = items.firstOrNull { it.id == ContentUris.parseId(uri) }?.file

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, sort: String?): Cursor {
        val cursor = MatrixCursor(arrayOf(MediaStore.MediaColumns._ID))
        val id = uri.lastPathSegment?.toLongOrNull()
        if (id != null) {
            items.filter { it.id == id }.forEach { cursor.addRow(arrayOf(it.id)) }
        } else {
            val name = args?.getOrNull(0)
            val size = args?.getOrNull(1)?.toLongOrNull()
            items.filter { it.name == name && it.file.length() == size }.forEach { cursor.addRow(arrayOf(it.id)) }
        }
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        if (failVideoInserts && uri.pathSegments.contains("video")) throw java.io.IOException("Speicher voll")
        inserted += ContentValues(values)
        val id = nextId++
        val f = File.createTempFile("gallery", ".bin", context!!.cacheDir)
        items += Item(id, values?.getAsString(MediaStore.MediaColumns.DISPLAY_NAME).orEmpty(), f)
        return ContentUris.withAppendedId(uri, id)
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val f = fileOf(uri) ?: throw java.io.FileNotFoundException(uri.toString())
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.parseMode(mode))
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?): Int {
        values?.getAsInteger(MediaStore.MediaColumns.IS_PENDING)?.let { lastPending = it }
        return 1
    }

    override fun getType(uri: Uri): String? = null
    override fun delete(uri: Uri, selection: String?, args: Array<out String>?): Int {
        val removed = items.removeAll { it.id == ContentUris.parseId(uri) }
        return if (removed) 1 else 0
    }
}
