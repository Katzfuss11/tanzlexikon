package io.github.katzfuss11.tanzlexikon.transfer

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.katzfuss11.tanzlexikon.data.MediaEntity
import io.github.katzfuss11.tanzlexikon.data.MediaKind
import io.github.katzfuss11.tanzlexikon.data.Section
import io.github.katzfuss11.tanzlexikon.data.TanzDatabase
import io.github.katzfuss11.tanzlexikon.lexikon.FigureLinks
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

    @Test fun import_galleryWriteFails_reportsWriteFailed_andLeavesDatabaseUntouched() = runTest {
        val video = ByteArray(5000) { 7 }
        val source = newRepo()
        val fig = source.addFigure(rumba(source), "Neu")
        source.saveFigure(fig, "Neu", "", "", "", listOf(media(fig, gallery.add("neu.mp4", video).toString(), MediaKind.VIDEO, name = "neu.mp4", size = 5000)))
        val archive = export(source)
        gallery.items.clear()
        gallery.failVideoInserts = true

        val repo = newRepo()
        repo.addFigure(rumba(repo), "Alt")
        val outcome = service(repo).import({ ByteArrayInputStream(archive) }) { _, _ -> }

        assertEquals(ImportOutcome.Failed(ArchiveError.WRITE_FAILED), outcome)
        assertEquals(listOf("Alt"), repo.observeFigures(rumba(repo)).first().map { it.name })
    }

    @Test fun import_spaceCheck_includesSafetyCopyOfCurrentMedia() = runTest {
        val mb = 1024L * 1024
        val source = newRepo()
        val fig = source.addFigure(rumba(source), "Neu")
        source.saveFigure(fig, "Neu", "", "", "", listOf(media(fig, gallery.add("neu.mp4", ByteArray(3000)).toString(), MediaKind.VIDEO, name = "neu.mp4", size = 3000)))
        val archive = export(source)

        val repo = newRepo()
        val old = repo.addFigure(rumba(repo), "Alt")
        repo.saveFigure(old, "Alt", "", "", "", listOf(media(old, gallery.add("alt.mp4", ByteArray(2000)).toString(), MediaKind.VIDEO, name = "alt.mp4", size = 2000)))
        val tight = BackupService(context, repo, GalleryWriter(context.contentResolver), MediaChecker(context.contentResolver),
            availableBytes = { 50 * mb + 3000 + 1999 })

        val outcome = tight.import({ ByteArrayInputStream(archive) }) { _, _ -> }

        assertEquals(ImportOutcome.NotEnoughSpace(5000), outcome)
        assertEquals(listOf("Alt"), repo.observeFigures(rumba(repo)).first().map { it.name })
    }

    @Test fun exportThenImport_keepsGroupsLinksAndAskedName() = runTest {
        val source = newRepo()
        suspend fun fig(dance: String, name: String) =
            source.addFigure(source.observeStart().first().flatMap { it.dances }.single { it.name == dance }.id, name)
        val s = fig("Salsa", "Crossbody Lead")
        val r = fig("Rumba", "Crossbody Lead")
        source.answerSuggestion(r, listOf(s), link = true)
        val f = fig("Rumba", "Fächer")
        val h = fig("Rumba", "Hockeystick")
        source.saveLinks(f, emptyList(), listOf(h))
        val solo = fig("Jive", "Kick")
        source.answerSuggestion(solo, listOf(fig("Samba", "Kick")), link = false)
        source.trashFigure(fig("Rumba", "Weg"), 1L)
        val bytes = export(source)

        val target = newRepo()
        assertTrue(service(target).import({ ByteArrayInputStream(bytes) }) { _, _ -> } is ImportOutcome.Success)

        val byName = target.observeAllFigures().first().groupBy { it.name to it.danceName }
        fun id(name: String, dance: String) = byName.getValue(name to dance).single().id
        suspend fun links(id: Long) = target.observeLinks(id).first()
        assertEquals(listOf("Salsa"), links(id("Crossbody Lead", "Rumba")).otherDances.map { it.danceName })
        assertEquals(listOf("Rumba"), links(id("Crossbody Lead", "Salsa")).otherDances.map { it.danceName })
        assertEquals(listOf("Hockeystick"), links(id("Fächer", "Rumba")).sameDance.map { it.name })
        assertEquals(null, target.linkSuggestion(id("Kick", "Jive")))
        assertEquals(null, target.linkSuggestion(id("Kick", "Samba")))
    }

    @Test fun import_oldBackupWithoutLinkFields_works() = runTest {
        // So sah eine Sicherung aus v0.1.2 aus: ohne key, linkGroup, askedName und links.
        val json = """{"formatVersion":1,"kind":"BACKUP","appVersion":"0.1.2","categories":[{"name":"Latein","position":0,
            "dances":[{"name":"Rumba","position":0,"figures":[{"name":"Fächer","position":0,"generalText":"Grundidee",
            "leaderText":"","followerText":"","media":[]}]}]}]}"""
        val zip = ByteArrayOutputStream().also { out ->
            java.util.zip.ZipOutputStream(out).use { z ->
                z.putNextEntry(java.util.zip.ZipEntry(MANIFEST_ENTRY)); z.write(json.toByteArray()); z.closeEntry()
            }
        }.toByteArray()

        val repo = newRepo()
        val outcome = service(repo).import({ ByteArrayInputStream(zip) }) { _, _ -> }

        assertEquals(ImportOutcome.Success(1, 0), outcome)
        val fig = repo.observeAllFigures().first().single()
        assertEquals("Fächer", fig.name)
        assertEquals(FigureLinks(), repo.observeLinks(fig.id).first())
    }

    @Test fun hasEnoughSpace_requires50MbMargin() {
        val mb = 1024L * 1024
        val s = service(newRepo())
        assertFalse(s.hasEnoughSpace(required = 100 * mb, available = 150 * mb - 1))
        assertTrue(s.hasEnoughSpace(required = 100 * mb, available = 150 * mb))
    }
}
