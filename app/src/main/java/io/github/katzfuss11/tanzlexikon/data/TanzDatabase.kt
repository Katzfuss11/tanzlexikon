package io.github.katzfuss11.tanzlexikon.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CategoryEntity::class, DanceEntity::class, FigureEntity::class, MediaEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class TanzDatabase : RoomDatabase() {
    abstract fun dao(): LexikonDao

    companion object {
        const val FILE_NAME = "tanzlexikon.db"

        fun build(context: Context, inMemory: Boolean = false): TanzDatabase {
            val builder = if (inMemory) {
                Room.inMemoryDatabaseBuilder(context, TanzDatabase::class.java)
            } else {
                Room.databaseBuilder(context, TanzDatabase::class.java, FILE_NAME)
            }
            return builder.addCallback(SeedCallback).build()
        }
    }

    /** Läuft nur beim allerersten Anlegen der Datenbank, innerhalb derselben Transaktion. */
    private object SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            SEED.forEachIndexed { catPos, (category, dances) ->
                db.execSQL("INSERT INTO category (name, position) VALUES (?, ?)", arrayOf<Any>(category, catPos))
                dances.forEachIndexed { dancePos, dance ->
                    db.execSQL(
                        "INSERT INTO dance (categoryId, name, position) " +
                            "VALUES ((SELECT id FROM category WHERE position = ?), ?, ?)",
                        arrayOf<Any>(catPos, dance, dancePos),
                    )
                }
            }
        }
    }
}
