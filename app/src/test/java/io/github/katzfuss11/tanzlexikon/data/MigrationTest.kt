package io.github.katzfuss11.tanzlexikon.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Prüft das Update von Schema 1 (v0.1.2) auf die aktuelle Version: alte Daten bleiben unverändert.
 * Die alte Datenbank wird genau so angelegt, wie Room sie laut schemas/…/1.json erzeugt hat.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val fileName = "migration-test.db"
    private val dbFile = context.getDatabasePath(fileName)

    @After fun tearDown() {
        context.deleteDatabase(fileName)
    }

    private fun createVersion1Database() {
        val schema = Json.parseToJsonElement(
            File("schemas/io.github.katzfuss11.tanzlexikon.data.TanzDatabase/1.json").readText(),
        ).jsonObject["database"]!!.jsonObject
        dbFile.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { db ->
            schema["entities"]!!.jsonArray.map { it.jsonObject }.forEach { entity ->
                val table = entity["tableName"]!!.jsonPrimitive.content
                db.execSQL(entity.sql("createSql", table))
                entity["indices"]?.jsonArray?.forEach { db.execSQL(it.jsonObject.sql("createSql", table)) }
            }
            schema["setupQueries"]!!.jsonArray.forEach { db.execSQL(it.jsonPrimitive.content) }
            db.execSQL("INSERT INTO category (id, name, position) VALUES (1, 'Latein', 0)")
            db.execSQL("INSERT INTO dance (id, categoryId, name, position) VALUES (1, 1, 'Rumba', 0)")
            db.execSQL(
                "INSERT INTO figure (id, danceId, name, position, generalText, leaderText, followerText) " +
                    "VALUES (1, 1, 'Fächer', 0, 'Grundidee 💃', 'Führen', '')",
            )
            db.execSQL(
                "INSERT INTO media (figureId, section, kind, uri, position) " +
                    "VALUES (1, 'GENERAL', 'YOUTUBE', 'https://youtu.be/dQw4w9WgXcQ', 0)",
            )
            db.version = 1
        }
    }

    private fun JsonObject.sql(key: String, table: String) = this[key]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table)

    @Test fun migrate1To2_keepsAllData_noLinks() = runBlocking {
        createVersion1Database()

        val db = TanzDatabase.build(context, fileName = fileName)
        try {
            val figure = db.dao().figure(1)!!
            assertEquals("Fächer", figure.name)
            assertEquals("Grundidee 💃", figure.generalText)
            assertEquals("Führen", figure.leaderText)
            assertNull(figure.linkGroup)
            assertNull(figure.askedName)
            assertEquals(1, db.dao().mediaOf(1).size)
            db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM figure_link").use { c ->
                c.moveToFirst()
                assertEquals(0, c.getInt(0))
            }
        } finally {
            db.close()
        }
    }
}
