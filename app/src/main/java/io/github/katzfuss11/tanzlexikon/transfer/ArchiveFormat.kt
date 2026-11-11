package io.github.katzfuss11.tanzlexikon.transfer

import io.github.katzfuss11.tanzlexikon.data.MediaKind
import io.github.katzfuss11.tanzlexikon.data.Section
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream

/*
 * Das Dateiformat für Sicherungen (.zip) und geteilte Figuren (.tanzfigur).
 * Beides ist ein ZIP: zuerst die Beschreibung "tanzlexikon.json", danach die Medien unter "media/".
 */

const val MANIFEST_ENTRY = "tanzlexikon.json"
const val CURRENT_FORMAT = 1

enum class ArchiveKind { BACKUP, FIGURE }

@Serializable
data class ArchiveManifest(
    val formatVersion: Int = CURRENT_FORMAT,
    val kind: ArchiveKind,
    val appVersion: String,
    val categories: List<ArchCategory>,
    /** Direkte Verbindungen zwischen Figuren (über [ArchFigure.key]). Nur in Sicherungen. */
    val links: List<ArchLink> = emptyList(),
)

@Serializable
data class ArchCategory(val name: String, val position: Int, val dances: List<ArchDance>)

@Serializable
data class ArchDance(val name: String, val position: Int, val figures: List<ArchFigure>)

@Serializable
data class ArchFigure(
    val name: String,
    val position: Int,
    val generalText: String,
    val leaderText: String,
    val followerText: String,
    val media: List<ArchMedia>,
    /** Nummer der Figur in dieser Sicherung, für [ArchiveManifest.links]. Nur in Sicherungen. */
    val key: Int? = null,
    /** Gruppe „dieselbe Figur in anderen Tänzen“ (Nummer nur innerhalb dieser Sicherung). */
    val linkGroup: Int? = null,
    /** Name, für den schon Verknüpfen vorgeschlagen wurde. */
    val askedName: String? = null,
)

/**
 * Ein Medium. [file] ist der Pfad im ZIP (`media/<Nummer>.<Endung>`). Fehlt die Datei schon beim
 * Exportieren, bleibt [file] leer und das Medium kommt als „nicht gefunden“ wieder an.
 */
@Serializable
data class ArchMedia(
    val section: Section,
    val kind: MediaKind,
    val position: Int,
    val youtubeUrl: String? = null,
    val file: String? = null,
    val displayName: String? = null,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
)

/** Eine Datei, die ins Archiv soll. Sie wird erst beim Schreiben geöffnet. */
data class ArchiveFile(val path: String, val size: Long, val open: () -> InputStream)

enum class ArchiveError {
    NOT_TANZLEXIKON, NEWER_FORMAT, CORRUPT,

    /** Das Archiv ist in Ordnung, aber das Ziel (Galerie, Speicher) hat das Schreiben abgelehnt. */
    WRITE_FAILED,
}

class ArchiveException(val reason: ArchiveError, cause: Throwable? = null) : Exception(reason.name, cause)

internal val archiveJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** Eine direkte Verbindung zwischen den Figuren mit den Schlüsseln [a] und [b]. */
@Serializable
data class ArchLink(val a: Int, val b: Int)
