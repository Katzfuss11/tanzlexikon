package io.github.katzfuss11.tanzlexikon.lexikon

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.katzfuss11.tanzlexikon.data.MediaEntity
import io.github.katzfuss11.tanzlexikon.data.MediaKind
import io.github.katzfuss11.tanzlexikon.data.Section
import io.github.katzfuss11.tanzlexikon.data.TanzDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LexikonRepositoryTest {
    private lateinit var db: TanzDatabase
    private lateinit var repo: LexikonRepository
    private val day = 24L * 60 * 60 * 1000
    private val now = 1_800_000_000_000L

    @Before fun setUp() {
        db = TanzDatabase.build(ApplicationProvider.getApplicationContext<Context>(), inMemory = true)
        repo = LexikonRepository(db)
    }

    @After fun tearDown() = db.close()

    private suspend fun category(name: String) = repo.observeStart().first().single { it.name == name }
    private suspend fun dance(cat: String, name: String) = category(cat).dances.single { it.name == name }

    private fun media(figureId: Long, uri: String, kind: MediaKind = MediaKind.VIDEO, position: Int = 0) =
        MediaEntity(
            figureId = figureId, section = Section.GENERAL, kind = kind, uri = uri,
            displayName = null, mimeType = null, sizeBytes = null, position = position,
        )

    @Test fun deleteDance_withActiveFigure_returnsNotEmpty_andKeepsDance() = runTest {
        val rumba = dance("Latein", "Rumba").id
        repo.addFigure(rumba, "Fächer")
        assertEquals(DeleteResult.NOT_EMPTY, repo.deleteDance(rumba))
        assertEquals(rumba, dance("Latein", "Rumba").id)
    }

    @Test fun deleteDance_withOnlyTrashedFigures_deletes() = runTest {
        val rumba = dance("Latein", "Rumba").id
        repo.trashFigure(repo.addFigure(rumba, "Fächer"), now)
        assertEquals(DeleteResult.DELETED, repo.deleteDance(rumba))
        assertTrue(category("Latein").dances.none { it.name == "Rumba" })
    }

    @Test fun deleteCategory_withDances_returnsNotEmpty() = runTest {
        val latein = category("Latein").id
        assertEquals(DeleteResult.NOT_EMPTY, repo.deleteCategory(latein))
        assertEquals(latein, category("Latein").id)
    }

    @Test fun deleteCategory_empty_deletes() = runTest {
        val neu = repo.addCategory("Neu")
        assertEquals(DeleteResult.DELETED, repo.deleteCategory(neu))
        assertTrue(repo.observeStart().first().none { it.name == "Neu" })
    }

    @Test fun trashFigure_hidesFromDanceAndCounts_showsInTrash() = runTest {
        val rumba = dance("Latein", "Rumba").id
        val fig = repo.addFigure(rumba, "Fächer")
        assertEquals(1, dance("Latein", "Rumba").figureCount)
        repo.trashFigure(fig, now)
        assertTrue(repo.observeFigures(rumba).first().isEmpty())
        assertEquals(0, dance("Latein", "Rumba").figureCount)
        assertEquals(listOf(TrashedFigure(fig, "Fächer", "Rumba", now)), repo.observeTrash().first())
    }

    @Test fun restore_whenDanceStillExists_returnsSameDance() = runTest {
        val rumba = dance("Latein", "Rumba").id
        val fig = repo.addFigure(rumba, "Fächer")
        repo.trashFigure(fig, now)
        assertEquals(rumba, repo.restoreFigure(fig))
        assertEquals(listOf("Fächer"), repo.observeFigures(rumba).first().map { it.name })
        assertTrue(repo.observeTrash().first().isEmpty())
    }

    @Test fun restore_afterDanceAndCategoryDeleted_recreatesBothWithOldNames() = runTest {
        val latein = category("Latein")
        val fig = repo.addFigure(dance("Latein", "Rumba").id, "Fächer")
        repo.trashFigure(fig, now)
        latein.dances.forEach { assertEquals(DeleteResult.DELETED, repo.deleteDance(it.id)) }
        assertEquals(DeleteResult.DELETED, repo.deleteCategory(latein.id))

        val newDance = repo.restoreFigure(fig)
        val recreated = category("Latein")
        assertNotEquals(latein.id, recreated.id)
        assertEquals(listOf(DanceSummary(newDance, "Rumba", 1)), recreated.dances)
        assertEquals(listOf("Fächer"), repo.observeFigures(newDance).first().map { it.name })
    }

    @Test fun restore_matchesExistingDanceIgnoringCase() = runTest {
        val latein = category("Latein").id
        val rumba = dance("Latein", "Rumba").id
        val fig = repo.addFigure(rumba, "Fächer")
        repo.trashFigure(fig, now)
        repo.deleteDance(rumba)
        val lower = repo.addDance(latein, "rumba")

        assertEquals(lower, repo.restoreFigure(fig))
        assertEquals(1, category("Latein").dances.count { it.name.equals("rumba", ignoreCase = true) })
    }

    @Test fun purgeTrash_removesOnlyOlderThan30Days() = runTest {
        val rumba = dance("Latein", "Rumba").id
        val old = repo.addFigure(rumba, "Alt")
        val young = repo.addFigure(rumba, "Jung")
        repo.trashFigure(old, now - 30 * day - 1)
        repo.trashFigure(young, now - 29 * day)

        assertEquals(1, repo.purgeTrash(now))
        assertEquals(listOf("Jung"), repo.observeTrash().first().map { it.name })
        assertNull(db.dao().figure(old))
    }

    @Test fun saveFigure_replacesMediaAtomically_andNeverTouchesOthers() = runTest {
        val rumba = dance("Latein", "Rumba").id
        val a = repo.addFigure(rumba, "Fächer")
        val b = repo.addFigure(rumba, "Hockeystick")
        repo.saveFigure(a, "Fächer", "", "", "", listOf(media(a, "content://1"), media(a, "content://2", position = 1)))
        repo.saveFigure(b, "Hockeystick", "", "", "", listOf(media(b, "content://b")))

        val keep = repo.observeFigure(a).first()!!.media.first { it.uri == "content://1" }
        repo.saveFigure(
            a, "Fächer neu", "Allgemein", "Führen", "Folgen",
            listOf(keep, media(a, "https://youtu.be/x", MediaKind.YOUTUBE, position = 1)),
        )

        val detail = repo.observeFigure(a).first()!!
        assertEquals("Fächer neu", detail.figure.name)
        assertEquals(listOf("Allgemein", "Führen", "Folgen"),
            listOf(detail.figure.generalText, detail.figure.leaderText, detail.figure.followerText))
        assertEquals(listOf("content://1", "https://youtu.be/x"), detail.media.map { it.uri })
        assertEquals("Rumba", detail.danceName)
        assertEquals("Latein", detail.categoryName)
        assertEquals(listOf("content://b"), repo.observeFigure(b).first()!!.media.map { it.uri })
        assertEquals(FigureSummary(a, "Fächer neu", 1, 0, 1), repo.observeFigures(rumba).first().first())
    }

    @Test fun addFigure_onlyNameRequired_appendsAtEnd() = runTest {
        val rumba = dance("Latein", "Rumba").id
        val first = repo.addFigure(rumba, "Fächer")
        val second = repo.addFigure(rumba, "  Hockeystick ")
        assertEquals(listOf(first, second), repo.observeFigures(rumba).first().map { it.id })
        val fig = repo.observeFigure(second).first()!!.figure
        assertEquals("Hockeystick", fig.name)
        assertEquals("", fig.generalText)
        assertTrue(repo.observeFigure(second).first()!!.media.isEmpty())
    }

    @Test fun search_findsInNamesAndTexts_caseInsensitive_excludesTrashed() = runTest {
        val rumba = dance("Latein", "Rumba").id
        val byName = repo.addFigure(rumba, "Fächer")
        val byText = repo.addFigure(rumba, "Hockeystick")
        repo.saveFigure(byText, "Hockeystick", "", "Aus der Fächerposition führen", "", emptyList())
        val trashed = repo.addFigure(rumba, "Fächer rückwärts")
        repo.trashFigure(trashed, now)

        val hits = repo.search("fächer")
        assertEquals(setOf(byName, byText), hits.mapNotNull { it.figureId }.toSet())

        val danceHit = repo.search("RUMB").single()
        assertNull(danceHit.figureId)
        assertEquals(rumba, danceHit.danceId)
        assertTrue(repo.search("   ").isEmpty())
    }
}
