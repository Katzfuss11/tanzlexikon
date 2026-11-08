package io.github.katzfuss11.tanzlexikon.transfer

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.katzfuss11.tanzlexikon.data.MediaEntity
import io.github.katzfuss11.tanzlexikon.data.MediaKind
import io.github.katzfuss11.tanzlexikon.data.Section
import io.github.katzfuss11.tanzlexikon.data.TanzDatabase
import io.github.katzfuss11.tanzlexikon.lexikon.LexikonRepository
import io.github.katzfuss11.tanzlexikon.media.GalleryWriter
import io.github.katzfuss11.tanzlexikon.media.MediaChecker
import io.github.katzfuss11.tanzlexikon.testutil.FakeGalleryProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import android.provider.MediaStore

@RunWith(RobolectricTestRunner::class)
class BackupServiceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var gallery: FakeGalleryProvider
    private val dbs = mutableListOf<TanzDatabase>()
    private val emojiText = "Schulter an Schulter – Übergang 💃"

    @Before fun setUp() {
        gallery = Robolectric.setupContentProvider(FakeGalleryProvider::class.java, "media")
    }

    @After fun tearDown() = dbs.forEach { it.close() }

    private fun newRepo() = LexikonRepository(TanzDatabase.build(context, inMemory = true).also { dbs += it })

    private fun service(repo: LexikonRepository) = BackupService(
        context, repo, GalleryWriter(context.contentResolver), MediaChecker(context.contentResolver),
        availableBytes = { Long.MAX_VALUE },
    )

    private suspend fun rumba(repo: LexikonRepository) =
        repo.observeStart().first().single { it.name == "Latein" }.dances.single { it.name == "Rumba" }.id

    private fun media(figureId: Long, uri: String, kind: MediaKind, section: Section = Section.GENERAL, name: String? = null, size: Long? = null) =
        MediaEntity(figureId = figureId, section = section, kind = kind, uri = uri, displayName = name,
            mimeType = if (kind == MediaKind.VIDEO) "video/mp4" else null, sizeBytes = size, position = 0)

    private suspend fun export(repo: LexikonRepository): ByteArray =
        ByteArrayOutputStream().also { service(repo).export(it) { _, _ -> } }.toByteArray()

    @Test fun exportThenImport_onFreshDb_restoresStructureTextsAndMedia() = runTest {
        val video = ByteArray(5000) { (it % 251).toByte() }
        val videoUri = gallery.add("fächer.mp4", video)
        val source = newRepo()
        source.addCategory("Hochzeit")
        val fig = source.addFigure(rumba(source), "Fächer")
        source.saveFigure(
            fig, "Fächer", emojiText, "Führen", "",
            listOf(
                media(fig, videoUri.toString(), MediaKind.VIDEO, name = "fächer.mp4", size = video.size.toLong()),
                media(fig, "https://youtu.be/dQw4w9WgXcQ", MediaKind.YOUTUBE, Section.LEADER),
            ),
        )
        val bytes = export(source)
        // Die Originaldatei ist auf dem "neuen Handy" nicht da.
        gallery.items.clear()

        val target = newRepo()
        val outcome = service(target).import({ ByteArrayInputStream(bytes) }) { _, _ -> }

        assertTrue(outcome is ImportOutcome.Success)
        val start = target.observeStart().first()
        assertEquals(listOf("Standard", "Latein", "Sonstige", "Hochzeit"), start.map { it.name })
        val newFig = target.observeFigures(rumba(target)).first().single()
        val detail = target.observeFigure(newFig.id).first()!!
        assertEquals(emojiText, detail.figure.generalText)
        assertEquals("Führen", detail.figure.leaderText)
        val restoredVideo = detail.media.single { it.kind == MediaKind.VIDEO }
        assertArrayEquals(video, gallery.fileOf(android.net.Uri.parse(restoredVideo.uri))!!.readBytes())
        assertEquals("https://youtu.be/dQw4w9WgXcQ", detail.media.single { it.kind == MediaKind.YOUTUBE }.uri)
    }

    @Test fun import_invalidFile_leavesDatabaseUntouched() = runTest {
        val repo = newRepo()
        repo.addFigure(rumba(repo), "Fächer")
        val outcome = service(repo).import({ ByteArrayInputStream("kein Archiv".toByteArray()) }) { _, _ -> }

        assertEquals(ImportOutcome.Failed(ArchiveError.NOT_TANZLEXIKON), outcome)
        assertEquals(listOf("Fächer"), repo.observeFigures(rumba(repo)).first().map { it.name })
        assertTrue(gallery.inserted.isEmpty())
    }

    @Test fun import_createsSafetyExportBeforeReplacing() = runTest {
        val other = newRepo()
        other.addFigure(rumba(other), "Neu")
        val archive = export(other)

        val repo = newRepo()
        repo.addFigure(rumba(repo), "Alt")
        service(repo).import({ ByteArrayInputStream(archive) }) { _, _ -> }

        assertEquals(listOf("Neu"), repo.observeFigures(rumba(repo)).first().map { it.name })
        val safetyName = gallery.inserted.map { it.getAsString(MediaStore.MediaColumns.DISPLAY_NAME) }
            .single { it.startsWith("tanzlexikon-vor-import-") }
        assertTrue(safetyName.endsWith(".zip"))
        val safetyFile = gallery.items.single { it.name == safetyName }.file
        val manifest = ArchiveReader().read(safetyFile.inputStream()) { _, _ -> }
        val names = manifest.categories.flatMap { c -> c.dances.flatMap { d -> d.figures.map { it.name } } }
        assertEquals(listOf("Alt"), names)
    }

    @Test fun export_excludesTrashedFigures() = runTest {
        val repo = newRepo()
        repo.addFigure(rumba(repo), "Bleibt")
        repo.trashFigure(repo.addFigure(rumba(repo), "Gelöscht"), 1L)

        val manifest = ArchiveReader().read(ByteArrayInputStream(export(repo))) { _, _ -> }
        val names = manifest.categories.flatMap { c -> c.dances.flatMap { d -> d.figures.map { it.name } } }
        assertEquals(listOf("Bleibt"), names)
        assertEquals(ArchiveKind.BACKUP, manifest.kind)
    }

    @Test fun hasEnoughSpace_requires50MbMargin() {
        val mb = 1024L * 1024
        val s = service(newRepo())
        assertFalse(s.hasEnoughSpace(required = 100 * mb, available = 150 * mb - 1))
        assertTrue(s.hasEnoughSpace(required = 100 * mb, available = 150 * mb))
    }
}
