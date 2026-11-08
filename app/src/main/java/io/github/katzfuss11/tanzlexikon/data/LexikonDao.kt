package io.github.katzfuss11.tanzlexikon.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LexikonDao {
    @Query("SELECT * FROM category ORDER BY position, id")
    suspend fun allCategories(): List<CategoryEntity>

    @Query("SELECT * FROM dance WHERE categoryId = :categoryId ORDER BY position, id")
    suspend fun dancesOf(categoryId: Long): List<DanceEntity>

    @Insert suspend fun insertDance(dance: DanceEntity): Long

    @Query("DELETE FROM dance WHERE id = :id")
    suspend fun deleteDance(id: Long)

    @Insert suspend fun insertFigure(figure: FigureEntity): Long

    @Query("SELECT * FROM figure WHERE id = :id")
    suspend fun figure(id: Long): FigureEntity?

    @Query("DELETE FROM figure WHERE id = :id")
    suspend fun deleteFigure(id: Long)

    @Insert suspend fun insertMedia(media: MediaEntity): Long

    @Query("SELECT * FROM media WHERE figureId = :figureId ORDER BY section, position, id")
    suspend fun mediaOf(figureId: Long): List<MediaEntity>
}
