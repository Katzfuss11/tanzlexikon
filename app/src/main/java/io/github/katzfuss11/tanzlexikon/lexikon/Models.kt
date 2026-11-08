package io.github.katzfuss11.tanzlexikon.lexikon

import io.github.katzfuss11.tanzlexikon.data.FigureEntity
import io.github.katzfuss11.tanzlexikon.data.MediaEntity

/** Wie lange eine gelöschte Figur im Papierkorb bleibt: 30 Tage. */
const val TRASH_RETENTION_MS = 30L * 24 * 60 * 60 * 1000

data class DanceSummary(val id: Long, val name: String, val figureCount: Int)

data class CategoryWithDances(val id: Long, val name: String, val dances: List<DanceSummary>)

data class FigureSummary(
    val id: Long,
    val name: String,
    val videoCount: Int,
    val imageCount: Int,
    val linkCount: Int,
)

data class FigureDetail(
    val figure: FigureEntity,
    val danceName: String,
    val categoryName: String,
    val media: List<MediaEntity>,
)

data class TrashedFigure(val id: Long, val name: String, val danceName: String, val trashedAt: Long)

/** Ein Suchtreffer. Bei einem Tanz-Treffer ist [figureId] null. */
data class SearchHit(val figureId: Long?, val danceId: Long, val title: String, val subtitle: String)

enum class DeleteResult { DELETED, NOT_EMPTY }

/** Verweis auf eine Figur, z. B. für die Liste der Figuren mit fehlenden Medien. */
data class FigureSummaryRef(val figureId: Long, val figureName: String, val danceName: String)

/** Der Bestand für ein Archiv: die Beschreibung und welche Galerie-Datei unter welchem Pfad ins ZIP soll. */
data class ArchiveSnapshot(
    val categories: List<io.github.katzfuss11.tanzlexikon.transfer.ArchCategory>,
    val files: List<Pair<String, MediaEntity>>,
)

/** Platzhalter-Adresse für ein Medium, dessen Datei schon beim Export fehlte. */
const val MISSING_URI = "missing:"

/** Dateiendung für ein Medium im Archiv, aus dem Namen oder dem Typ. */
fun fileExtension(m: MediaEntity): String {
    m.displayName?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() && it.length <= 5 }?.let { return it.lowercase() }
    return when (m.mimeType) {
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/heic" -> "heic"
        "video/mp4" -> "mp4"
        "video/3gpp" -> "3gp"
        "video/webm" -> "webm"
        "video/quicktime" -> "mov"
        else -> if (m.kind == io.github.katzfuss11.tanzlexikon.data.MediaKind.IMAGE) "jpg" else "mp4"
    }
}
