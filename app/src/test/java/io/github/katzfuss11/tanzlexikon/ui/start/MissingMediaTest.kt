package io.github.katzfuss11.tanzlexikon.ui.start

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.katzfuss11.tanzlexikon.data.MediaEntity
import io.github.katzfuss11.tanzlexikon.data.MediaKind
import io.github.katzfuss11.tanzlexikon.data.Section
import io.github.katzfuss11.tanzlexikon.data.TanzDatabase
import io.github.katzfuss11.tanzlexikon.lexikon.FigureSummaryRef
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MissingMediaTest {
    private lateinit var db: TanzDatabase
    private lateinit var repo: LexikonRepository

    private fun <T> await(block: suspend () -> T): T = runBlocking { withTimeout(5_000) { block() } }

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = TanzDatabase.build(ApplicationProvider.getApplicationContext<Context>(), inMemory = true)
        repo = LexikonRepository(db)
    }

    @After fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun media(figureId: Long, uri: String, kind: MediaKind = MediaKind.VIDEO, position: Int = 0) = MediaEntity(
        figureId = figureId, section = Section.GENERAL, kind = kind, uri = uri,
        displayName = null, mimeType = null, sizeBytes = null, position = position,
    )

    @Test fun missingMediaCount_countsOnlyActiveFiguresMissingFiles() {
        await {
            val rumba = repo.observeStart().first().single { it.name == "Latein" }.dances.single { it.name == "Rumba" }.id
            val active = repo.addFigure(rumba, "Fächer")
            repo.saveFigure(
                active, "Fächer", "", "", "",
                listOf(
                    media(active, "content://weg/1"),
                    media(active, "content://da/2", position = 1),
                    media(active, "https://youtu.be/dQw4w9WgXcQ", MediaKind.YOUTUBE, position = 2),
                ),
            )
            val trashed = repo.addFigure(rumba, "Alt")
            repo.saveFigure(trashed, "Alt", "", "", "", listOf(media(trashed, "content://weg/3")))
            repo.trashFigure(trashed, 1L)
        }

        val vm = StartViewModel(repo, isAvailable = { !it.uri.startsWith("content://weg") })

        val state = await { vm.state.first { it.missingMediaCount > 0 } }
        assertEquals(1, state.missingMediaCount)
        assertEquals(listOf(FigureSummaryRef(state.missingFigures.single().figureId, "Fächer", "Rumba")), state.missingFigures)
    }
}
