package io.github.katzfuss11.tanzlexikon.ui.start

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.katzfuss11.tanzlexikon.R
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class StartViewModelTest {
    private lateinit var db: TanzDatabase
    private lateinit var repo: LexikonRepository
    private lateinit var vm: StartViewModel

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = TanzDatabase.build(ApplicationProvider.getApplicationContext<Context>(), inMemory = true)
        repo = LexikonRepository(db)
        vm = StartViewModel(repo)
    }

    @After fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun <T> await(block: suspend () -> T): T = runBlocking { withTimeout(5_000) { block() } }

    private fun rumbaId(): Long = await {
        vm.state.first { it.categories.isNotEmpty() }.categories
            .single { it.name == "Latein" }.dances.single { it.name == "Rumba" }.id
    }

    @Test fun deleteDanceWithFigures_setsNotEmptyMessage_andDanceStays() {
        val rumba = rumbaId()
        await { repo.addFigure(rumba, "Fächer") }

        vm.deleteDance(rumba)

        val state = await { vm.state.first { it.message != null } }
        assertEquals(R.string.delete_dance_not_empty, state.message)
        assertTrue(state.categories.single { it.name == "Latein" }.dances.any { it.id == rumba })

        vm.messageShown()
        assertNull(vm.state.value.message)
    }

    @Test fun applyOrder_danceDraggedBelowOtherHeader_changesCategory_andCategoriesReorder() {
        rumbaId()
        val entries = vm.state.value.categories.toEntries().toMutableList()
        // Rumba direkt unter die Überschrift "Sonstige" ziehen.
        val rumba = entries.first { it is OrderEntry.Dance && it.name == "Rumba" }
        entries.remove(rumba)
        entries.add(entries.indexOfFirst { it is OrderEntry.Header && it.name == "Sonstige" } + 1, rumba)
        // Und "Sonstige" nach ganz oben (Kategorienreihenfolge umdrehen).
        val order = entries.toOrder().reversed()

        vm.applyOrder(order)

        val state = await {
            vm.state.first { s -> s.categories.first().name == "Sonstige" && s.categories.first().dances.size == 3 }
        }
        assertEquals(listOf("Sonstige", "Latein", "Standard"), state.categories.map { it.name })
        assertEquals(listOf("Rumba", "Discofox", "Argentinischer Tango"), state.categories[0].dances.map { it.name })
        assertTrue(state.categories[1].dances.none { it.name == "Rumba" })
    }

    @Test fun deleteEmptyDance_removesWithoutConfirmation() {
        val rumba = rumbaId()

        vm.deleteDance(rumba)

        val state = await {
            vm.state.first { s -> s.categories.single { it.name == "Latein" }.dances.none { it.id == rumba } }
        }
        assertNull(state.message)
    }
}
