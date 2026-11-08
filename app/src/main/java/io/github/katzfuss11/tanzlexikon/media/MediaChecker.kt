package io.github.katzfuss11.tanzlexikon.media

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import io.github.katzfuss11.tanzlexikon.data.MediaEntity
import io.github.katzfuss11.tanzlexikon.data.MediaKind

/** Prüft, ob ein Video oder Bild noch in der Galerie liegt. YouTube-Links gelten immer als vorhanden. */
class MediaChecker(private val resolver: ContentResolver) {
    fun isAvailable(m: MediaEntity): Boolean {
        if (m.kind == MediaKind.YOUTUBE) return true
        return try {
            val uri = Uri.parse(m.uri)
            // Eine Datei im Galerie-Papierkorb lässt sich noch öffnen, erscheint bei einer normalen
            // Abfrage aber nicht mehr. Für uns ist sie damit weg.
            if (uri.authority == MediaStore.AUTHORITY && !listedInGallery(uri)) return false
            resolver.openFileDescriptor(uri, "r")?.use { true } ?: false
        } catch (e: Exception) {
            // Gelöscht, Berechtigung weg, kaputte Adresse: alles heißt "nicht gefunden".
            false
        }
    }

    private fun listedInGallery(uri: Uri): Boolean =
        resolver.query(uri, arrayOf(MediaStore.MediaColumns._ID), null, null, null)?.use { it.moveToFirst() } ?: false
}
