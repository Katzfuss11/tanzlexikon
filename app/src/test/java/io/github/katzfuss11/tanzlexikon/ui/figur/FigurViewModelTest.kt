package io.github.katzfuss11.tanzlexikon.ui.figur

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.katzfuss11.tanzlexikon.R
import io.github.katzfuss11.tanzlexikon.data.MediaEntity
import io.github.katzfuss11.tanzlexikon.data.MediaKind
import io.github.katzfuss11.tanzlexikon.data.Section
import io.github.katzfuss11.tanzlexikon.data.TanzDatabase
import io.github.katzfuss11.tanzlexikon.lexikon.LexikonRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class FigurViewModelTest {
    private lateinit var db: TanzDatabase
    private lateinit var repo: LexikonRepository
    private lateinit var vm: FigurViewModel
    private var figureId = 0L

    private fun <T> await(block: suspend () -> T): T = runBlocking { withTimeout(5_000) { block() } }

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = TanzDatabase.build(ApplicationProvider.getApplicationContext<Context>(), inMemory = true)
        repo = LexikonRepository(db)
        figureId = await {
            val rumba = repo.observeStart().first().single { it.name == "Latein" }.dances.single { it.name == "Rumba" }.id
            val id = repo.addFigure(rumba, "Fächer")
            repo.saveFigure(id, "Fächer", "Grundidee", "Führen", "", emptyList())
            id
        }
        vm = FigurViewModel(repo, figureId)
        await { vm.state.first { it.detail != null } }
    }

    @After fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test fun cancelEditing_discardsDraft_andDetailUnchanged() {
        vm.startEditing()
        vm.updateDraft { it.copy(leaderText = "Ganz anders") }
        assertEquals("Ganz anders", vm.state.value.draft!!.leaderText)

        vm.cancelEditing()

        val s = vm.state.value
        assertFalse(s.editing)
        assertNull(s.draft)
        assertEquals("Führen", s.detail!!.figure.leaderText)
        assertEquals("Führen", await { repo.observeFigure(figureId).first() }!!.figure.leaderText)
    }

    @Test fun save_withBlankName_keepsEditing_andShowsNameRequired() {
        vm.startEditing()
        vm.updateDraft { it.copy(name = "   ", leaderText = "Neu") }

        vm.save()

        val s = vm.state.value
        assertTrue(s.editing)
        assertEquals(R.string.name_required, s.error)
        assertEquals("Fächer", await { repo.observeFigure(figureId).first() }!!.figure.name)
    }

    @Test fun save_writesTextsAndMediaInOneGo() {
        vm.startEditing()
        val link = MediaEntity(
            figureId = figureId, section = Section.LEADER, kind = MediaKind.YOUTUBE, uri = "https://youtu.be/x",
            displayName = null, mimeType = null, sizeBytes = null, position = 0,
        )
        vm.updateDraft { it.copy(name = "Fächer (Rumba)", followerText = "Folgen", media = listOf(link)) }

        vm.save()

        val s = await { vm.state.first { !it.editing && it.detail?.figure?.name == "Fächer (Rumba)" } }
        assertNull(s.draft)
        assertNull(s.error)
        val f = s.detail!!.figure
        assertEquals(listOf("Grundidee", "Führen", "Folgen"), listOf(f.generalText, f.leaderText, f.followerText))
        assertEquals(listOf("https://youtu.be/x"), await { vm.state.first { it.detail!!.media.isNotEmpty() } }.detail!!.media.map { it.uri })
    }

    @Test fun mediaDraft_addRemoveRelink_onlyChangesDraftUntilSave() {
        vm.startEditing()
        vm.addMedia(Section.GENERAL, listOf(NewMedia(MediaKind.VIDEO, "content://alt", "alt.mp4", "video/mp4", 10)))
        vm.addMedia(Section.LEADER, listOf(NewMedia(MediaKind.YOUTUBE, "https://youtu.be/dQw4w9WgXcQ")))
        val old = vm.state.value.draft!!.media.first { it.uri == "content://alt" }

        vm.relinkMedia(old, NewMedia(MediaKind.VIDEO, "content://neu", "neu.mp4", "video/mp4", 20))
        val link = vm.state.value.draft!!.media.first { it.kind == MediaKind.YOUTUBE }
        vm.removeMedia(link)

        val draftMedia = vm.state.value.draft!!.media
        assertEquals(listOf("content://neu"), draftMedia.map { it.uri })
        assertEquals("neu.mp4", draftMedia.single().displayName)
        assertEquals(Section.GENERAL, draftMedia.single().section)
        assertTrue(await { repo.observeFigure(figureId).first() }!!.media.isEmpty())

        vm.save()
        val saved = await { vm.state.first { !it.editing && it.detail!!.media.isNotEmpty() } }.detail!!.media
        assertEquals(listOf(20L), saved.map { it.sizeBytes })
    }
}
