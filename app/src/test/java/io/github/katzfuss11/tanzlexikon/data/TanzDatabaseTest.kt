package io.github.katzfuss11.tanzlexikon.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TanzDatabaseTest {
    private lateinit var db: TanzDatabase

    @Before fun setUp() {
        db = TanzDatabase.build(ApplicationProvider.getApplicationContext<Context>(), inMemory = true)
    }

    @After fun tearDown() = db.close()

    @Test fun freshDatabaseContainsSeedInOrder() = runTest {
        val cats = db.dao().allCategories()
        assertEquals(listOf("Standard", "Latein", "Sonstige"), cats.map { it.name })
        assertEquals(
            listOf("Langsamer Walzer", "Tango", "Wiener Walzer", "Slowfox", "Quickstep"),
            db.dao().dancesOf(cats[0].id).map { it.name },
        )
        assertEquals(
            listOf("Cha-Cha-Cha", "Samba", "Rumba", "Paso Doble", "Jive", "Salsa"),
            db.dao().dancesOf(cats[1].id).map { it.name },
        )
        assertEquals(listOf("Discofox", "Argentinischer Tango"), db.dao().dancesOf(cats[2].id).map { it.name })
    }

    @Test fun deletingFigureCascadesMediaButDanceDeleteNullsFigure() = runTest {
        val dao = db.dao()
        val catId = dao.allCategories().first().id
        val rumbaId = dao.insertDance(DanceEntity(categoryId = catId, name = "Testtanz", position = 99))

        val figId = dao.insertFigure(FigureEntity(danceId = rumbaId, name = "Fächer", position = 0))
        dao.insertMedia(
            MediaEntity(
                figureId = figId, section = Section.GENERAL, kind = MediaKind.YOUTUBE,
                uri = "https://youtu.be/abc", displayName = null, mimeType = null, sizeBytes = null, position = 0,
            ),
        )
        assertEquals(1, dao.mediaOf(figId).size)
        dao.deleteFigure(figId)
        assertTrue(dao.mediaOf(figId).isEmpty())

        val otherDance = dao.insertDance(DanceEntity(categoryId = catId, name = "Zweiter", position = 100))
        val fig2 = dao.insertFigure(FigureEntity(danceId = otherDance, name = "Hockeystick", position = 0))
        dao.deleteDance(otherDance)
        assertNull(dao.figure(fig2)!!.danceId)
    }
}
