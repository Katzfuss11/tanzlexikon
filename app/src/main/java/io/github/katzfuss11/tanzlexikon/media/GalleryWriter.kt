package io.github.katzfuss11.tanzlexikon.media

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Legt Videos und Bilder aus einer Sicherung oder einer geteilten Figur zurück in die Galerie,
 * in den Ordner "Movies/Tanzlexikon" bzw. "Pictures/Tanzlexikon".
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
        return if (Build.VERSION.SDK_INT >= 29) insertModern(collection, displayName, mimeType, isImage, data)
        else insertLegacy(collection, displayName, mimeType, isImage, data)
    }

    private fun collection(isImage: Boolean): Uri = when {
        Build.VERSION.SDK_INT >= 29 && isImage -> MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        Build.VERSION.SDK_INT >= 29 -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        isImage -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        else -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    }

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

    private fun insertModern(collection: Uri, displayName: String, mimeType: String, isImage: Boolean, data: InputStream): Uri {
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

    @Suppress("DEPRECATION")
    private fun insertLegacy(collection: Uri, displayName: String, mimeType: String, isImage: Boolean, data: InputStream): Uri {
        val dir = File(Environment.getExternalStorageDirectory(), folder(isImage)).apply { mkdirs() }
        var target = File(dir, displayName)
        var i = 1
        while (target.exists()) target = File(dir, "${target.nameWithoutExtension.substringBefore(" (")} (${i++}).${target.extension}")
        target.outputStream().use { data.copyTo(it, 64 * 1024) }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, target.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.DATA, target.absolutePath)
            put(MediaStore.MediaColumns.SIZE, target.length())
        }
        return resolver.insert(collection, values) ?: throw IOException("Galerie hat die Datei abgelehnt")
    }
}
