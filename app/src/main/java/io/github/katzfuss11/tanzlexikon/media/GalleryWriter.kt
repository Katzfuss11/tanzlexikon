package io.github.katzfuss11.tanzlexikon.media

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Legt Videos und Bilder aus einer Sicherung oder einer geteilten Figur zurück in die Galerie,
 * in den Ordner "Movies/Tanzlexikon" bzw. "Pictures/Tanzlexikon", und bereitet neue Aufnahmen vor.
 */
class GalleryWriter(private val resolver: ContentResolver) {

    /**
     * Gibt es schon eine Datei mit gleichem Namen und gleicher Größe, wird sie wiederverwendet
     * und [data] gar nicht gelesen. Sonst wird die Datei neu angelegt.
     */
    fun save(displayName: String, mimeType: String, size: Long, data: InputStream): Uri {
        val isImage = mimeType.startsWith("image/")
        val collection = collection(isImage)
        findExisting(collection, displayName, size)?.let { return it }
        return insert(collection, displayName, mimeType, isImage, data)
    }

    /**
     * Legt einen leeren Video-Eintrag in "Movies/Tanzlexikon" an, in den die Kamera-App die
     * Aufnahme schreibt. Weil der Eintrag der App gehört, darf sie das Video später ohne
     * Galerie-Berechtigung lesen. Bricht die Aufnahme ab, mit [discard] wieder entfernen.
     */
    fun newRecording(now: Date = Date()): Uri {
        val name = "Tanzlexikon_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(now) + ".mp4"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, folder(isImage = false))
        }
        return resolver.insert(collection(isImage = false), values) ?: throw IOException("Galerie hat die Datei abgelehnt")
    }

    /** Ob in der Datei schon etwas steht (die Kamera also wirklich aufgenommen hat). */
    fun hasContent(uri: Uri): Boolean =
        runCatching { resolver.openFileDescriptor(uri, "r")?.use { it.statSize > 0 } ?: false }.getOrDefault(false)

    fun discard(uri: Uri) {
        runCatching { resolver.delete(uri, null, null) }
    }

    private fun collection(isImage: Boolean): Uri =
        if (isImage) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        else MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

    private fun findExisting(collection: Uri, displayName: String, size: Long): Uri? =
        resolver.query(
            collection,
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.SIZE} = ?",
            arrayOf(displayName, size.toString()),
            null,
        )?.use { c -> if (c.moveToFirst()) ContentUris.withAppendedId(collection, c.getLong(0)) else null }

    private fun folder(isImage: Boolean) =
        (if (isImage) Environment.DIRECTORY_PICTURES else Environment.DIRECTORY_MOVIES) + "/Tanzlexikon"

    private fun insert(collection: Uri, displayName: String, mimeType: String, isImage: Boolean, data: InputStream): Uri {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, folder(isImage))
            // Solange die Datei noch geschrieben wird, sehen andere Apps sie nicht.
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: throw IOException("Galerie hat die Datei abgelehnt")
        try {
            val out = resolver.openOutputStream(uri) ?: throw IOException("Datei lässt sich nicht schreiben")
            out.use { data.copyTo(it, 64 * 1024) }
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            return uri
        } catch (e: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            throw e
        }
    }
}
