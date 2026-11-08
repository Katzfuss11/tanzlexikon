package io.github.katzfuss11.tanzlexikon.data

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import io.github.katzfuss11.tanzlexikon.lexikon.FigureSummary
import io.github.katzfuss11.tanzlexikon.lexikon.TrashedFigure
import kotlinx.coroutines.flow.Flow

/** Eine Zeile der Startseite: Kategorie mit (höchstens) einem Tanz. */
data class StartRow(
    val categoryId: Long,
    val categoryName: String,
    val danceId: Long?,
    val danceName: String?,
    val figureCount: Int,
)

/** Eine Figur mit den Namen von Tanz und Kategorie (null, wenn der Tanz fehlt). */
data class FigureRow(
    @Embedded val figure: FigureEntity,
    val danceName: String?,
    val categoryName: String?,
)

/** Ein Medium mit Figur- und Tanznamen. */
data class MediaRow(
    @Embedded val media: MediaEntity,
    val figureName: String,
    val danceName: String,
)

@Dao
interface LexikonDao {
    // ---- Kategorien ----
    @Query("SELECT * FROM category ORDER BY position, id")
    suspend fun allCategories(): List<CategoryEntity>

    @Query("SELECT * FROM category WHERE id = :id")
    suspend fun category(id: Long): CategoryEntity?

    @Insert suspend fun insertCategory(category: CategoryEntity): Long

    @Query("UPDATE category SET name = :name WHERE id = :id")
    suspend fun renameCategory(id: Long, name: String)

    @Query("UPDATE category SET position = :position WHERE id = :id")
    suspend fun setCategoryPosition(id: Long, position: Int)

    @Query("DELETE FROM category WHERE id = :id")
    suspend fun deleteCategory(id: Long)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM category")
    suspend fun nextCategoryPosition(): Int

    @Query(
        """
        SELECT c.id AS categoryId, c.name AS categoryName, d.id AS danceId, d.name AS danceName,
               (SELECT COUNT(*) FROM figure f WHERE f.danceId = d.id AND f.trashedAt IS NULL) AS figureCount
        FROM category c LEFT JOIN dance d ON d.categoryId = c.id
        ORDER BY c.position, c.id, d.position, d.id
        """,
    )
    fun observeStartRows(): Flow<List<StartRow>>

    // ---- Tänze ----
    @Query("SELECT * FROM dance WHERE categoryId = :categoryId ORDER BY position, id")
    suspend fun dancesOf(categoryId: Long): List<DanceEntity>

    @Query("SELECT * FROM dance ORDER BY position, id")
    suspend fun allDances(): List<DanceEntity>

    @Query("SELECT * FROM dance WHERE id = :id")
    suspend fun dance(id: Long): DanceEntity?

    @Insert suspend fun insertDance(dance: DanceEntity): Long

    @Query("UPDATE dance SET name = :name WHERE id = :id")
    suspend fun renameDance(id: Long, name: String)

    @Query("UPDATE dance SET categoryId = :categoryId, position = :position WHERE id = :id")
    suspend fun moveDance(id: Long, categoryId: Long, position: Int)

    @Query("UPDATE dance SET position = :position WHERE id = :id")
    suspend fun setDancePosition(id: Long, position: Int)

    @Query("DELETE FROM dance WHERE id = :id")
    suspend fun deleteDance(id: Long)

    @Query("SELECT COUNT(*) FROM dance WHERE categoryId = :categoryId")
    suspend fun danceCount(categoryId: Long): Int

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM dance WHERE categoryId = :categoryId")
    suspend fun nextDancePosition(categoryId: Long): Int

    // ---- Figuren ----
    @Insert suspend fun insertFigure(figure: FigureEntity): Long

    @Query("SELECT * FROM figure WHERE id = :id")
    suspend fun figure(id: Long): FigureEntity?

    @Query("SELECT * FROM figure WHERE trashedAt IS NULL")
    suspend fun activeFigures(): List<FigureEntity>

    @Query(
        """
        UPDATE figure SET name = :name, generalText = :generalText, leaderText = :leaderText,
               followerText = :followerText WHERE id = :id
        """,
    )
    suspend fun updateFigureTexts(id: Long, name: String, generalText: String, leaderText: String, followerText: String)

    @Query("UPDATE figure SET position = :position WHERE id = :id")
    suspend fun setFigurePosition(id: Long, position: Int)

    @Query(
        """
        UPDATE figure SET trashedAt = :trashedAt, trashedDanceName = :danceName,
               trashedCategoryName = :categoryName WHERE id = :id
        """,
    )
    suspend fun markTrashed(id: Long, trashedAt: Long, danceName: String?, categoryName: String?)

    @Query(
        """
        UPDATE figure SET danceId = :danceId, position = :position, trashedAt = NULL,
               trashedDanceName = NULL, trashedCategoryName = NULL WHERE id = :id
        """,
    )
    suspend fun markRestored(id: Long, danceId: Long, position: Int)

    @Query("DELETE FROM figure WHERE id = :id")
    suspend fun deleteFigure(id: Long)

    @Query("DELETE FROM figure WHERE trashedAt IS NOT NULL AND trashedAt < :cutoff")
    suspend fun deleteTrashedBefore(cutoff: Long): Int

    @Query("SELECT COUNT(*) FROM figure WHERE danceId = :danceId AND trashedAt IS NULL")
    suspend fun activeFigureCount(danceId: Long): Int

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM figure WHERE danceId = :danceId")
    suspend fun nextFigurePosition(danceId: Long): Int

    @Query(
        """
        SELECT f.id, f.name,
               (SELECT COUNT(*) FROM media m WHERE m.figureId = f.id AND m.kind = 'VIDEO') AS videoCount,
               (SELECT COUNT(*) FROM media m WHERE m.figureId = f.id AND m.kind = 'IMAGE') AS imageCount,
               (SELECT COUNT(*) FROM media m WHERE m.figureId = f.id AND m.kind = 'YOUTUBE') AS linkCount
        FROM figure f WHERE f.danceId = :danceId AND f.trashedAt IS NULL
        ORDER BY f.position, f.id
        """,
    )
    fun observeFigureSummaries(danceId: Long): Flow<List<FigureSummary>>

    @Query(
        """
        SELECT f.*, d.name AS danceName, c.name AS categoryName
        FROM figure f LEFT JOIN dance d ON d.id = f.danceId LEFT JOIN category c ON c.id = d.categoryId
        WHERE f.id = :id
        """,
    )
    fun observeFigureRow(id: Long): Flow<FigureRow?>

    @Query(
        """
        SELECT id, name, COALESCE(trashedDanceName, '') AS danceName, trashedAt
        FROM figure WHERE trashedAt IS NOT NULL ORDER BY trashedAt DESC, id DESC
        """,
    )
    fun observeTrash(): Flow<List<TrashedFigure>>

    // ---- Medien ----
    @Insert suspend fun insertMedia(media: MediaEntity): Long

    @Query("SELECT * FROM media WHERE figureId = :figureId ORDER BY section, position, id")
    suspend fun mediaOf(figureId: Long): List<MediaEntity>

    @Query("SELECT * FROM media WHERE figureId = :figureId ORDER BY section, position, id")
    fun observeMedia(figureId: Long): Flow<List<MediaEntity>>

    @Query("DELETE FROM media WHERE figureId = :figureId")
    suspend fun deleteMediaOf(figureId: Long)

    // ---- Für Export und Import ----
    @Query("SELECT * FROM figure WHERE danceId = :danceId AND trashedAt IS NULL ORDER BY position, id")
    suspend fun activeFiguresOf(danceId: Long): List<FigureEntity>

    @Query("DELETE FROM media") suspend fun deleteAllMedia()
    @Query("DELETE FROM figure") suspend fun deleteAllFigures()
    @Query("DELETE FROM dance") suspend fun deleteAllDances()
    @Query("DELETE FROM category") suspend fun deleteAllCategories()

    @Query(
        """
        SELECT m.*, f.name AS figureName, COALESCE(d.name, '') AS danceName
        FROM media m JOIN figure f ON f.id = m.figureId LEFT JOIN dance d ON d.id = f.danceId
        WHERE f.trashedAt IS NULL ORDER BY d.name, f.name, m.position
        """,
    )
    suspend fun activeMediaRows(): List<MediaRow>
}
