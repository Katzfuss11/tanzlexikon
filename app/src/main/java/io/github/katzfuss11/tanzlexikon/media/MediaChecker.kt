package io.github.katzfuss11.tanzlexikon.media

import android.content.ContentResolver
import android.net.Uri
import io.github.katzfuss11.tanzlexikon.data.MediaEntity
import io.github.katzfuss11.tanzlexikon.data.MediaKind

/** Prüft, ob ein Video oder Bild noch in der Galerie liegt. YouTube-Links gelten immer als vorhanden. */
class MediaChecker(private val resolver: ContentResolver) {
    fun isAvailable(m: MediaEntity): Boolean {
        if (m.kind == MediaKind.YOUTUBE) return true
        return try {
            resolver.openFileDescriptor(Uri.parse(m.uri), "r")?.use { true } ?: false
        } catch (e: Exception) {
            // Gelöscht, Berechtigung weg, kaputte Adresse: alles heißt "nicht gefunden".
            false
        }
    }
}
