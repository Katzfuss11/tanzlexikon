package io.github.katzfuss11.tanzlexikon.ui.empfang

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import io.github.katzfuss11.tanzlexikon.data.MediaKind
import io.github.katzfuss11.tanzlexikon.data.Section
import io.github.katzfuss11.tanzlexikon.data.TanzDatabase
import io.github.katzfuss11.tanzlexikon.lexikon.LexikonRepository
import io.github.katzfuss11.tanzlexikon.media.GalleryWriter
import io.github.katzfuss11.tanzlexikon.share.ImportTarget
import io.github.katzfuss11.tanzlexikon.testutil.FakeGalleryProvider
import io.github.katzfuss11.tanzlexikon.transfer.ArchCategory
import io.github.katzfuss11.tanzlexikon.transfer.ArchDance
import io.github.katzfuss11.tanzlexikon.transfer.ArchFigure
import io.github.katzfuss11.tanzlexikon.transfer.ArchMedia
import io.github.katzfuss11.tanzlexikon.transfer.ArchiveError
import io.github.katzfuss11.tanzlexikon.transfer.ArchiveFile
import io.github.katzfuss11.tanzlexikon.transfer.ArchiveKind
import io.github.katzfuss11.tanzlexikon.transfer.ArchiveManifest
import io.github.katzfuss11.tanzlexikon.transfer.ArchiveWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class EmpfangViewModelTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: TanzDatabase
    private lateinit var repo: LexikonRepository
    private lateinit var gallery: FakeGalleryProvider

    private fun <T> await(block: suspend () -> T): T = runBlocking { withTimeout(5_000) { block() } }

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        gallery = Robolectric.setupContentProvider(FakeGalleryProvider::class.java, "media")
        db = TanzDatabase.build(context, inMemory = true)
        repo = LexikonRepository(db)
    }

    @After fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private val video = ByteArray(3000) { (it % 97).toByte() }

    private fun archive(
        kind: ArchiveKind, category: String, dance: String, figure: String,
        linkGroup: Int? = null, askedName: String? = null,
    ): ByteArray {
        val manifest = ArchiveManifest(
            kind = kind, appVersion = "0.1.0",
            categories = listOf(
                ArchCategory(category, 0, listOf(ArchDance(dance, 0, listOf(
                    ArchFigure(
                        figure, 0, "Grundidee", "", "",
                        listOf(
                            ArchMedia(Section.GENERAL, MediaKind.VIDEO, 0, file = "media/1.mp4", displayName = "kreisel.mp4", mimeType = "video/mp4", sizeBytes = video.size.toLong()),
                            ArchMedia(Section.LEADER, MediaKind.YOUTUBE, 0, youtubeUrl = "https://youtu.be/dQw4w9WgXcQ"),
                        ),
                        linkGroup = linkGroup, askedName = askedName,
                    ),
                )))),
            ),
        )
        return ByteArrayOutputStream().also {
            ArchiveWriter().write(it, manifest, listOf(ArchiveFile("media/1.mp4", video.size.toLong()) { ByteArrayInputStream(video) })) { _, _ -> }
        }.toByteArray()
    }

    private fun viewModel(bytes: ByteArray): EmpfangViewModel {
        val vm = EmpfangViewModel(repo, GalleryWriter(context.contentResolver)) { ByteArrayInputStream(bytes) }
        await { vm.state.first { !it.loading } }
        return vm
    }

    private suspend fun danceNames() = repo.observeStart().first().flatMap { c -> c.dances.map { it.name } }

    @Test fun empfang_salsaFile_userChoosesCasino_addsFigureToCasino_salsaNotCreated() {
        val latein = await { repo.observeStart().first().single { it.name == "Latein" }.id }
        await { repo.deleteDance(repo.observeStart().first().single { it.name == "Latein" }.dances.single { it.name == "Salsa" }.id) }
        val casino = await { repo.addDance(latein, "Casino") }

        val vm = viewModel(archive(ArchiveKind.FIGURE, "Latein", "Salsa", "Kreisel"))
        assertEquals(ImportTarget.New("Latein", "Salsa"), vm.state.value.target)

        vm.chooseExisting(casino)
        vm.add()

        val id = await { vm.state.first { it.addedFigureId != null } }.addedFigureId!!
        val detail = await { repo.observeFigure(id).first() }!!
        assertEquals("Casino", detail.danceName)
        assertEquals("Kreisel", detail.figure.name)
        assertFalse(await { danceNames() }.contains("Salsa"))
        val v = detail.media.single { it.kind == MediaKind.VIDEO }
        assertArrayEquals(video, gallery.fileOf(Uri.parse(v.uri))!!.readBytes())
        assertEquals("https://youtu.be/dQw4w9WgXcQ", detail.media.single { it.kind == MediaKind.YOUTUBE }.uri)
    }

    @Test fun empfang_screenClosedWhileAdding_figureIsStillAdded() {
        val rumba = await { repo.observeStart().first().single { it.name == "Latein" }.dances.single { it.name == "Rumba" }.id }
        val bytes = archive(ArchiveKind.FIGURE, "Latein", "Rumba", "Kreisel")
        val calls = java.util.concurrent.atomic.AtomicInteger()
        val screenClosed = java.util.concurrent.CountDownLatch(1)
        val store = androidx.lifecycle.ViewModelStore()
        val vm = EmpfangViewModel(repo, GalleryWriter(context.contentResolver), workScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob())) {
            // Beim Hinzufügen (zweiter Aufruf) erst weiterlesen, wenn der Bildschirm zu ist.
            if (calls.incrementAndGet() > 1) screenClosed.await()
            ByteArrayInputStream(bytes)
        }
        androidx.lifecycle.ViewModelProvider(store, object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = vm as T
        })[EmpfangViewModel::class.java]
        await { vm.state.first { !it.loading } }

        vm.add()
        store.clear()
        screenClosed.countDown()

        assertEquals(listOf("Kreisel"), await { repo.observeFigures(rumba).first { it.isNotEmpty() } }.map { it.name })
    }

    @Test fun empfang_addedFigure_hasNoGroupAndIsNotAsked() {
        val vm = viewModel(archive(ArchiveKind.FIGURE, "Latein", "Rumba", "Kreisel", linkGroup = 3, askedName = "kreisel"))
        vm.add()

        val id = await { vm.state.first { it.addedFigureId != null } }.addedFigureId!!
        val figure = await { db.dao().figure(id) }!!
        assertNull(figure.linkGroup)
        assertNull(figure.askedName)
    }

    @Test fun empfang_duplicateName_flagsDuplicate_addStillCreatesSecondFigure() {
        val rumba = await { repo.observeStart().first().single { it.name == "Latein" }.dances.single { it.name == "Rumba" }.id }
        await { repo.addFigure(rumba, "Fächer") }

        val vm = viewModel(archive(ArchiveKind.FIGURE, "Latein", "Rumba", "fächer"))
        assertTrue(vm.state.value.duplicateName)
        vm.rename("Fächer (von Anna)")
        await { vm.state.first { !it.duplicateName } }
        vm.rename("fächer")
        await { vm.state.first { it.duplicateName } }
        vm.add()

        await { vm.state.first { it.addedFigureId != null } }
        assertEquals(listOf("Fächer", "fächer"), await { repo.observeFigures(rumba).first() }.map { it.name })
    }

    @Test fun empfang_backupZipOpened_showsNotAFigureError_addsNothing() {
        val vm = viewModel(archive(ArchiveKind.BACKUP, "Latein", "Rumba", "Fächer"))
        assertTrue(vm.state.value.notAFigure)
        vm.add()
        assertNull(vm.state.value.addedFigureId)
        assertTrue(gallery.inserted.isEmpty())
    }

    @Test fun empfang_corruptFile_showsError_addsNothing() {
        val good = archive(ArchiveKind.FIGURE, "Latein", "Rumba", "Fächer")
        val vm = viewModel(good.copyOf(120))
        assertEquals(ArchiveError.CORRUPT, vm.state.value.error)
        vm.add()
        assertNull(vm.state.value.addedFigureId)
        assertTrue(gallery.inserted.isEmpty())
    }
}
