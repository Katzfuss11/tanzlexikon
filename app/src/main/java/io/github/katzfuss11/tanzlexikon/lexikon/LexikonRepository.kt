package io.github.katzfuss11.tanzlexikon.lexikon

import androidx.room.withTransaction
import io.github.katzfuss11.tanzlexikon.data.CategoryEntity
import io.github.katzfuss11.tanzlexikon.data.DanceEntity
import io.github.katzfuss11.tanzlexikon.data.FigureEntity
import io.github.katzfuss11.tanzlexikon.data.MediaEntity
import io.github.katzfuss11.tanzlexikon.data.TanzDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Alles, was die Bildschirme mit dem Lexikon tun dürfen, an einer Stelle.
 * Hier stecken auch die Regeln: Tänze und Kategorien sind nur löschbar, wenn sie leer sind;
 * Figuren gehen in den Papierkorb und kommen beim Wiederherstellen wieder in ihren Tanz.
 */
class LexikonRepository(private val db: TanzDatabase) {
    private val dao = db.dao()

    // ---- Startseite ----

    fun observeStart(): Flow<List<CategoryWithDances>> = dao.observeStartRows().map { rows ->
        rows.groupBy { it.categoryId }.map { (id, group) ->
            CategoryWithDances(
                id = id,
                name = group.first().categoryName,
                dances = group.filter { it.danceId != null }
                    .map { DanceSummary(it.danceId!!, it.danceName.orEmpty(), it.figureCount) },
            )
        }
    }

    // ---- Kategorien ----

    suspend fun addCategory(name: String): Long =
        db.withTransaction { dao.insertCategory(CategoryEntity(name = name.trim(), position = dao.nextCategoryPosition())) }

    suspend fun renameCategory(id: Long, name: String) = dao.renameCategory(id, name.trim())

    suspend fun reorderCategories(orderedIds: List<Long>) = db.withTransaction {
        orderedIds.forEachIndexed { i, id -> dao.setCategoryPosition(id, i) }
    }

    suspend fun deleteCategory(id: Long): DeleteResult = db.withTransaction {
        if (dao.danceCount(id) > 0) {
            DeleteResult.NOT_EMPTY
        } else {
            dao.deleteCategory(id)
            DeleteResult.DELETED
        }
    }

    // ---- Tänze ----

    suspend fun addDance(categoryId: Long, name: String): Long = db.withTransaction {
        dao.insertDance(DanceEntity(categoryId = categoryId, name = name.trim(), position = dao.nextDancePosition(categoryId)))
    }

    suspend fun renameDance(id: Long, name: String) = dao.renameDance(id, name.trim())

    suspend fun moveDance(id: Long, categoryId: Long) = db.withTransaction {
        dao.moveDance(id, categoryId, dao.nextDancePosition(categoryId))
    }

    suspend fun reorderDances(categoryId: Long, orderedIds: List<Long>) = db.withTransaction {
        orderedIds.forEachIndexed { i, id -> dao.setDancePosition(id, i) }
    }

    /**
     * Setzt die Reihenfolge von Kategorien und Tänzen in einem Rutsch (eine Transaktion).
     * Steht ein Tanz unter einer anderen Kategorie als vorher, wechselt er dorthin.
     */
    suspend fun arrange(order: List<Pair<Long, List<Long>>>) = db.withTransaction {
        order.forEachIndexed { catPos, (categoryId, danceIds) ->
            dao.setCategoryPosition(categoryId, catPos)
            danceIds.forEachIndexed { pos, danceId -> dao.moveDance(danceId, categoryId, pos) }
        }
    }

    /** Figuren im Papierkorb zählen nicht: sie verlieren dann ihren Tanz und merken sich nur den Namen. */
    suspend fun deleteDance(id: Long): DeleteResult = db.withTransaction {
        if (dao.activeFigureCount(id) > 0) {
            DeleteResult.NOT_EMPTY
        } else {
            dao.deleteDance(id)
            DeleteResult.DELETED
        }
    }

    // ---- Figuren ----

    fun observeFigures(danceId: Long): Flow<List<FigureSummary>> = dao.observeFigureSummaries(danceId)

    suspend fun addFigure(danceId: Long, name: String): Long = db.withTransaction {
        dao.insertFigure(FigureEntity(danceId = danceId, name = name.trim(), position = dao.nextFigurePosition(danceId)))
    }

    suspend fun reorderFigures(danceId: Long, orderedIds: List<Long>) = db.withTransaction {
        orderedIds.forEachIndexed { i, id -> dao.setFigurePosition(id, i) }
    }

    fun observeFigure(id: Long): Flow<FigureDetail?> =
        combine(dao.observeFigureRow(id), dao.observeMedia(id)) { row, media ->
            row?.let {
                FigureDetail(
                    figure = it.figure,
                    danceName = it.danceName ?: it.figure.trashedDanceName.orEmpty(),
                    categoryName = it.categoryName ?: it.figure.trashedCategoryName.orEmpty(),
                    media = media,
                )
            }
        }

    /** Ersetzt Texte und die komplette Medienliste in einer Transaktion. Neue Medien haben id = 0. */
    suspend fun saveFigure(
        id: Long,
        name: String,
        generalText: String,
        leaderText: String,
        followerText: String,
        media: List<MediaEntity>,
    ) = db.withTransaction {
        dao.updateFigureTexts(id, name.trim(), generalText, leaderText, followerText)
        dao.deleteMediaOf(id)
        media.forEach { dao.insertMedia(it.copy(id = 0, figureId = id)) }
    }

    // ---- Papierkorb ----

    suspend fun trashFigure(id: Long, now: Long) = db.withTransaction {
        val figure = dao.figure(id) ?: return@withTransaction
        val dance = figure.danceId?.let { dao.dance(it) }
        val category = dance?.let { dao.category(it.categoryId) }
        dao.markTrashed(id, now, dance?.name, category?.name)
    }

    fun observeTrash(): Flow<List<TrashedFigure>> = dao.observeTrash()

    /** Gibt den Tanz zurück, in dem die Figur jetzt liegt. Fehlt er, wird er (samt Kategorie) neu angelegt. */
    suspend fun restoreFigure(id: Long): Long = db.withTransaction {
        val figure = requireNotNull(dao.figure(id)) { "Figur $id gibt es nicht" }
        val danceId = figure.danceId
            ?: findOrCreateDance(
                figure.trashedCategoryName?.takeIf { it.isNotBlank() } ?: FALLBACK_CATEGORY,
                figure.trashedDanceName?.takeIf { it.isNotBlank() } ?: FALLBACK_DANCE,
            )
        val position = if (figure.danceId == danceId) figure.position else dao.nextFigurePosition(danceId)
        dao.markRestored(id, danceId, position)
        danceId
    }

    suspend fun deleteFigurePermanently(id: Long) = dao.deleteFigure(id)

    /** Löscht, was länger als 30 Tage im Papierkorb liegt. Gibt die Anzahl zurück. */
    suspend fun purgeTrash(now: Long): Int = dao.deleteTrashedBefore(now - TRASH_RETENTION_MS)

    /** Sucht Kategorie und Tanz ohne Rücksicht auf Groß-/Kleinschreibung und legt Fehlendes am Ende an. */
    suspend fun findOrCreateDance(categoryName: String, danceName: String): Long = db.withTransaction {
        val catName = categoryName.trim()
        val dName = danceName.trim()
        val categoryId = dao.allCategories().firstOrNull { it.name.trim().equals(catName, ignoreCase = true) }?.id
            ?: dao.insertCategory(CategoryEntity(name = catName, position = dao.nextCategoryPosition()))
        dao.dancesOf(categoryId).firstOrNull { it.name.trim().equals(dName, ignoreCase = true) }?.id
            ?: dao.insertDance(DanceEntity(categoryId = categoryId, name = dName, position = dao.nextDancePosition(categoryId)))
    }

    // ---- Suche ----

    /**
     * Durchsucht Tanznamen, Figurennamen und die drei Texte. Gefiltert wird in Kotlin statt per SQL-LIKE,
     * weil SQLite Umlaute sonst nur mit gleicher Groß-/Kleinschreibung findet ("fächer" ≠ "Fächer").
     */
    suspend fun search(query: String): List<SearchHit> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val categories = dao.allCategories().associateBy { it.id }
        val dances = dao.allDances()
        val danceById = dances.associateBy { it.id }
        val danceHits = dances.filter { it.name.contains(q, ignoreCase = true) }
            .map { SearchHit(null, it.id, it.name, categories[it.categoryId]?.name.orEmpty()) }
        val figureHits = dao.activeFigures()
            .filter { f -> listOf(f.name, f.generalText, f.leaderText, f.followerText).any { it.contains(q, ignoreCase = true) } }
            .mapNotNull { f ->
                val dance = f.danceId?.let { danceById[it] } ?: return@mapNotNull null
                SearchHit(f.id, dance.id, f.name, dance.name)
            }
            .sortedBy { it.title.lowercase() }
        return danceHits + figureHits
    }

    private companion object {
        const val FALLBACK_CATEGORY = "Sonstige"
        const val FALLBACK_DANCE = "Ohne Tanz"
    }
}
