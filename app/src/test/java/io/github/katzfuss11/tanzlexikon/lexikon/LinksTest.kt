package io.github.katzfuss11.tanzlexikon.lexikon

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.katzfuss11.tanzlexikon.data.TanzDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Verweise: Gruppen über Tänze hinweg, einzelne Verbindungen im selben Tanz, Vorschläge. */
@RunWith(RobolectricTestRunner::class)
class LinksTest {
    private lateinit var db: TanzDatabase
    private lateinit var repo: LexikonRepository
    private val now = 1_800_000_000_000L

    @Before fun setUp() {
        db = TanzDatabase.build(ApplicationProvider.getApplicationContext<Context>(), inMemory = true)
        repo = LexikonRepository(db)
    }

    @After fun tearDown() = db.close()

    private suspend fun dance(name: String) = repo.observeStart().first().flatMap { it.dances }.single { it.name == name }.id
    private suspend fun fig(dance: String, name: String) = repo.addFigure(dance(dance), name)
    private suspend fun links(id: Long) = repo.observeLinks(id).first()
    private suspend fun FigureLinks.otherIds() = otherDances.map { it.id }
    private suspend fun FigureLinks.sameIds() = sameDance.map { it.id }
    private suspend fun linkRowCount(): Int =
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM figure_link").use { it.moveToFirst(); it.getInt(0) }

    /** Salsa, Rumba und Cha-Cha-Cha „Crossbody Lead“ in einer Gruppe. */
    private suspend fun crossbodyGroup(): Triple<Long, Long, Long> {
        val s = fig("Salsa", "Crossbody Lead")
        val r = fig("Rumba", "Crossbody Lead")
        repo.answerSuggestion(r, listOf(s), link = true)
        val c = fig("Cha-Cha-Cha", "Crossbody Lead")
        repo.answerSuggestion(c, listOf(s), link = true)
        return Triple(s, r, c)
    }

    @Test fun suggestion_findsSameNameInOtherDancesOnly() = runTest {
        val salsa = fig("Salsa", "Crossbody Lead")
        val rumba = fig("Rumba", "crossbody lead ")
        val salsa2 = fig("Salsa", "Crossbody Lead")
        repo.trashFigure(fig("Rumba", "Crossbody Lead"), now)
        val cha = fig("Cha-Cha-Cha", "Crossbody Lead")

        val forCha = repo.linkSuggestion(cha)!!
        assertEquals("Crossbody Lead", forCha.figureName)
        assertEquals(listOf(rumba, salsa, salsa2), forCha.candidates.map { it.id })
        // Das Doppel im selben Tanz ist kein Kandidat.
        assertEquals(listOf(cha, rumba), repo.linkSuggestion(salsa)!!.candidates.map { it.id })
    }

    @Test fun suggestion_afterNo_notAskedAgain_fromEitherSide() = runTest {
        val alt = fig("Salsa", "Crossbody Lead")
        val neu = fig("Rumba", "Crossbody Lead")

        repo.answerSuggestion(neu, listOf(alt), link = false)

        assertNull(repo.linkSuggestion(neu))
        assertNull(repo.linkSuggestion(alt))
        assertTrue(links(neu).otherDances.isEmpty())
    }

    @Test fun suggestion_afterRename_askedAgainForNewName() = runTest {
        val hammer = fig("Salsa", "Hammerlock")
        val b = fig("Rumba", "Crossbody Lead")
        repo.answerSuggestion(b, listOf(fig("Salsa", "Crossbody Lead")), link = false)

        repo.saveFigure(b, "Hammerlock", "", "", "", emptyList())

        assertEquals(listOf(hammer), repo.linkSuggestion(b)!!.candidates.map { it.id })
    }

    @Test fun answerYes_groupsAcrossDances_transitively() = runTest {
        val (_, r, _) = crossbodyGroup()

        val l = links(r)
        assertEquals(listOf("Cha-Cha-Cha", "Salsa"), l.otherDances.map { it.danceName })
        assertTrue(l.sameDance.isEmpty())
    }

    @Test fun group_hidesSameDanceMembers_untilDirectlyLinked() = runTest {
        val (s, r, c) = crossbodyGroup()
        val d = fig("Rumba", "Crossbody Lead mit Drehung")

        repo.saveLinks(s, otherDanceIds = links(s).otherIds() + d, sameDanceIds = emptyList())
        assertEquals(listOf(c, r, d), links(s).otherIds())
        assertEquals(listOf(c, s), links(r).otherIds())
        assertTrue(links(r).sameDance.isEmpty())

        repo.saveLinks(r, otherDanceIds = listOf(c, s), sameDanceIds = listOf(d))
        assertEquals(listOf(d), links(r).sameIds())
        assertEquals(listOf(r), links(d).sameIds())
        assertEquals(listOf(c, s), links(d).otherIds())
    }

    @Test fun sameDanceLink_isNotTransitive() = runTest {
        val f = fig("Rumba", "Fächer")
        val h = fig("Rumba", "Hockeystick")
        val n = fig("Rumba", "New York")

        repo.saveLinks(f, emptyList(), listOf(h))
        repo.saveLinks(h, emptyList(), listOf(f, n))

        assertEquals(listOf("Hockeystick"), links(f).sameDance.map { it.name })
        assertEquals(listOf("Fächer", "New York"), links(h).sameDance.map { it.name })
        assertTrue(links(f).otherDances.isEmpty())
    }

    @Test fun saveLinks_removeFromOther_takesEntryOutOfGroup_keepsItsSameDanceLinks() = runTest {
        val (s, r, c) = crossbodyGroup()
        val r2 = fig("Rumba", "Crossbody Lead mit Drehung")
        repo.saveLinks(r, otherDanceIds = listOf(c, s), sameDanceIds = listOf(r2))

        repo.saveLinks(s, otherDanceIds = listOf(c), sameDanceIds = emptyList())

        assertEquals(listOf(c), links(s).otherIds())
        assertEquals(listOf(s), links(c).otherIds())
        assertTrue(links(r).otherDances.isEmpty())
        assertEquals(listOf(r2), links(r).sameIds())
        assertNull(repo.linkSuggestion(r))
    }

    @Test fun saveLinks_removeSame_deletesOnlyThatLink_bothSidesAsked() = runTest {
        val f = fig("Rumba", "Fächer")
        val h = fig("Rumba", "Hockeystick")
        val n = fig("Rumba", "New York")
        repo.saveLinks(f, emptyList(), listOf(h, n))

        repo.saveLinks(f, emptyList(), listOf(n))

        assertEquals(listOf(n), links(f).sameIds())
        assertTrue(links(h).sameDance.isEmpty())
        assertEquals("fächer", db.dao().figure(f)!!.askedName)
        assertEquals("hockeystick", db.dao().figure(h)!!.askedName)
    }

    @Test fun trash_hidesFromSeeAlso_restoreBringsBack_inCurrentDance() = runTest {
        val s = fig("Salsa", "Crossbody Lead")
        val r = fig("Rumba", "Crossbody Lead")
        repo.answerSuggestion(r, listOf(s), link = true)

        repo.trashFigure(r, now)
        assertTrue(links(s).otherDances.isEmpty())
        assertEquals(DeleteResult.DELETED, repo.deleteDance(dance("Rumba")))

        repo.restoreFigure(r)

        val back = links(s).otherDances.single()
        assertEquals(r, back.id)
        assertEquals("Rumba", back.danceName)
        assertEquals(dance("Rumba"), back.danceId)
    }

    @Test fun purge_removesDirectLinks() = runTest {
        val f = fig("Rumba", "Fächer")
        val h = fig("Rumba", "Hockeystick")
        repo.saveLinks(f, emptyList(), listOf(h))
        assertEquals(1, linkRowCount())

        repo.deleteFigurePermanently(h)

        assertEquals(0, linkRowCount())
        assertTrue(links(f).sameDance.isEmpty())
    }

    @Test fun allFigures_sortedGerman_thenByDanceOrder() = runTest {
        fig("Salsa", "Zorro")
        fig("Salsa", "Crossbody Lead")
        fig("Cha-Cha-Cha", "crossbody lead")
        fig("Rumba", "Basic")
        fig("Jive", "Ärmel")
        repo.trashFigure(fig("Rumba", "Weg"), now)

        val all = repo.observeAllFigures().first()

        assertEquals(listOf("Ärmel", "Basic", "crossbody lead", "Crossbody Lead", "Zorro"), all.map { it.name })
        assertEquals(listOf("Cha-Cha-Cha", "Salsa"), all.filter { normalizeName(it.name) == "crossbody lead" }.map { it.danceName })
    }
}
