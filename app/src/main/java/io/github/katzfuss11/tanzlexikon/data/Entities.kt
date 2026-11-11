package io.github.katzfuss11.tanzlexikon.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import kotlinx.serialization.Serializable

/** Die drei Bereiche einer Figur. */
enum class Section { GENERAL, LEADER, FOLLOWER }

/** Art eines Mediums. Bei YOUTUBE steht in [MediaEntity.uri] die Original-URL. */
enum class MediaKind { VIDEO, IMAGE, YOUTUBE }

@Entity(tableName = "category")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val position: Int,
)

@Entity(
    tableName = "dance",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("categoryId")],
)
data class DanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val name: String,
    val position: Int,
)

/**
 * Eine Figur. Liegt sie im Papierkorb, ist [trashedAt] gesetzt; Tanz- und Kategoriename
 * werden gemerkt, damit beim Wiederherstellen der Tanz notfalls neu angelegt werden kann.
 */
@Entity(
    tableName = "figure",
    foreignKeys = [
        ForeignKey(
            entity = DanceEntity::class, parentColumns = ["id"], childColumns = ["danceId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("danceId")],
)
data class FigureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val danceId: Long?,
    val name: String,
    val position: Int,
    val generalText: String = "",
    val leaderText: String = "",
    val followerText: String = "",
    val trashedAt: Long? = null,
    val trashedDanceName: String? = null,
    val trashedCategoryName: String? = null,
    /** Gruppe „dieselbe Figur in anderen Tänzen“. null = in keiner Gruppe. */
    val linkGroup: Long? = null,
    /** Der (normalisierte) Name, für den schon einmal Verknüpfen vorgeschlagen wurde. */
    val askedName: String? = null,
)

/**
 * Eine direkte Verbindung zwischen zwei Figuren (vor allem im selben Tanz). Immer [figureA] < [figureB].
 * Wird eine Figur endgültig gelöscht, verschwinden ihre Verbindungen mit.
 */
@Entity(
    tableName = "figure_link",
    primaryKeys = ["figureA", "figureB"],
    foreignKeys = [
        ForeignKey(entity = FigureEntity::class, parentColumns = ["id"], childColumns = ["figureA"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = FigureEntity::class, parentColumns = ["id"], childColumns = ["figureB"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("figureB")],
)
data class FigureLinkEntity(val figureA: Long, val figureB: Long)

@Entity(
    tableName = "media",
    foreignKeys = [
        ForeignKey(
            entity = FigureEntity::class, parentColumns = ["id"], childColumns = ["figureId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("figureId")],
)
// Serializable, damit ein Entwurf mit seinen Medien das Beenden durch Android übersteht.
@Serializable
data class MediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val figureId: Long,
    val section: Section,
    val kind: MediaKind,
    val uri: String,
    val displayName: String?,
    val mimeType: String?,
    val sizeBytes: Long?,
    val position: Int,
)

/** Speichert die Enums als ihren Namen, damit eine geänderte Reihenfolge nichts kaputt macht. */
class Converters {
    @TypeConverter fun sectionToText(s: Section): String = s.name
    @TypeConverter fun textToSection(s: String): Section = Section.valueOf(s)
    @TypeConverter fun kindToText(k: MediaKind): String = k.name
    @TypeConverter fun textToKind(k: String): MediaKind = MediaKind.valueOf(k)
}
