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
import androidx.lifecycle.SavedStateHandle

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

    // Wie nach dem Beenden durch Android: nur die gesicherten Werte überleben, das ViewModel ist neu.
    private fun recreated(saved: SavedStateHandle): FigurViewModel {
        val restored = SavedStateHandle(saved.keys().associateWith { saved.get<Any>(it) })
        return FigurViewModel(repo, figureId, restored).also { v -> await { v.state.first { it.detail != null } } }
    }

    @Test fun draft_survivesRecreation() {
        val saved = SavedStateHandle()
        val first = FigurViewModel(repo, figureId, saved)
        await { first.state.first { it.detail != null } }
        first.startEditing()
        first.updateDraft { it.copy(generalText = "Nach der Stunde getippt 💃") }

        val second = recreated(saved)

        assertTrue(second.state.value.editing)
        assertEquals("Nach der Stunde getippt 💃", second.state.value.draft!!.generalText)
    }

    @Test fun recordedVideo_afterRecreation_landsInChosenSection() {
        val saved = SavedStateHandle()
        val first = FigurViewModel(repo, figureId, saved)
        await { first.state.first { it.detail != null } }
        first.startEditing()
        first.updateDraft { it.copy(leaderText = "Hand höher") }
        first.chooseMediaTarget(MediaTarget.Add(Section.LEADER))

        val second = recreated(saved)
        second.onMediaPicked(listOf(NewMedia(MediaKind.VIDEO, "content://media/external/video/media/7")))

        val draft = second.state.value.draft!!
        assertEquals("Hand höher", draft.leaderText)
        assertEquals(listOf(Section.LEADER to "content://media/external/video/media/7"), draft.media.map { it.section to it.uri })
    }

    @Test fun relink_afterRecreation_replacesTheChosenMedium() {
        val saved = SavedStateHandle()
        val first = FigurViewModel(repo, figureId, saved)
        await { first.state.first { it.detail != null } }
        first.startEditing()
        first.addMedia(Section.GENERAL, listOf(NewMedia(MediaKind.VIDEO, "content://alt/1"), NewMedia(MediaKind.VIDEO, "content://alt/2")))
        first.chooseMediaTarget(MediaTarget.Relink(index = 1))

        val second = recreated(saved)
        second.onMediaPicked(listOf(NewMedia(MediaKind.IMAGE, "content://neu/3")))

        assertEquals(listOf("content://alt/1", "content://neu/3"), second.state.value.draft!!.media.map { it.uri })
    }

    @Test fun mediaPicked_withoutDraft_startsEditingAndAttaches() {
        vm.chooseMediaTarget(MediaTarget.Add(Section.GENERAL))
        vm.onMediaPicked(listOf(NewMedia(MediaKind.VIDEO, "content://v/1")))

        assertTrue(vm.state.value.editing)
        assertEquals(listOf("content://v/1"), vm.state.value.draft!!.media.map { it.uri })
    }

    @Test fun hasChanges_onlyAfterRealChange() {
        vm.startEditing()
        assertFalse(vm.state.value.hasChanges)
        vm.updateDraft { it.copy(name = "Fächer neu") }
        assertTrue(vm.state.value.hasChanges)
        vm.updateDraft { it.copy(name = "Fächer") }
        assertFalse(vm.state.value.hasChanges)
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
        val old = vm.state.value.draft!!.media.indexOfFirst { it.uri == "content://alt" }

        vm.chooseMediaTarget(MediaTarget.Relink(old))
        vm.onMediaPicked(listOf(NewMedia(MediaKind.VIDEO, "content://neu", "neu.mp4", "video/mp4", 20)))
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
