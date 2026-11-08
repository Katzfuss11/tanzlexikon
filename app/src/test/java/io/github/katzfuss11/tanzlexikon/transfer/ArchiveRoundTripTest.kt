package io.github.katzfuss11.tanzlexikon.transfer

import io.github.katzfuss11.tanzlexikon.data.MediaKind
import io.github.katzfuss11.tanzlexikon.data.Section
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.random.Random

class ArchiveRoundTripTest {
    private fun manifest(text: String = "Grundidee", media: List<ArchMedia> = emptyList(), version: Int = CURRENT_FORMAT) =
        ArchiveManifest(
            formatVersion = version,
            kind = ArchiveKind.BACKUP,
            appVersion = "0.1.0",
            categories = listOf(
                ArchCategory(
                    "Latein", 0,
                    listOf(ArchDance("Rumba", 2, listOf(ArchFigure("Fächer", 0, text, "Führen", "", media)))),
                ),
            ),
        )

    private fun write(m: ArchiveManifest, files: List<ArchiveFile> = emptyList(), progress: (Long, Long) -> Unit = { _, _ -> }) =
        ByteArrayOutputStream().also { ArchiveWriter().write(it, m, files, progress) }.toByteArray()

    private fun expectError(expected: ArchiveError, bytes: ByteArray) {
        try {
            ArchiveReader().read(ByteArrayInputStream(bytes)) { _, _ -> }
            fail("Fehler $expected erwartet")
        } catch (e: ArchiveException) {
            assertEquals(expected, e.reason)
        }
    }

    @Test fun roundTrip_preservesManifestAndFileBytes() {
        val small = Random(1).nextBytes(1024)
        val big = Random(2).nextBytes(3 * 1024 * 1024)
        val media = listOf(
            ArchMedia(Section.GENERAL, MediaKind.VIDEO, 0, file = "media/1.mp4", displayName = "a.mp4", mimeType = "video/mp4", sizeBytes = big.size.toLong()),
            ArchMedia(Section.LEADER, MediaKind.IMAGE, 0, file = "media/2.jpg", displayName = "b.jpg", mimeType = "image/jpeg", sizeBytes = small.size.toLong()),
            ArchMedia(Section.LEADER, MediaKind.YOUTUBE, 1, youtubeUrl = "https://youtu.be/dQw4w9WgXcQ"),
            ArchMedia(Section.FOLLOWER, MediaKind.VIDEO, 0, displayName = "weg.mp4"),
        )
        val m = manifest(media = media)
        var last = 0L to 0L
        val bytes = write(
            m,
            listOf(
                ArchiveFile("media/1.mp4", big.size.toLong()) { ByteArrayInputStream(big) },
                ArchiveFile("media/2.jpg", small.size.toLong()) { ByteArrayInputStream(small) },
            ),
        ) { done, total -> last = done to total }
        assertEquals(last.second, last.first)
        assertEquals((big.size + small.size).toLong(), last.second)

        val files = mutableMapOf<String, ByteArray>()
        val read = ArchiveReader().read(ByteArrayInputStream(bytes)) { path, data -> files[path] = data.readBytes() }
        assertEquals(m, read)
        assertArrayEquals(big, files["media/1.mp4"])
        assertArrayEquals(small, files["media/2.jpg"])
    }

    @Test fun roundTrip_keepsUmlautsEmojiAndLongText() {
        val text = "Schulter an Schulter – Übergang 💃" + "x".repeat(5000)
        val read = ArchiveReader().read(ByteArrayInputStream(write(manifest(text = text)))) { _, _ -> }
        assertEquals(text, read.categories[0].dances[0].figures[0].generalText)
    }

    @Test fun read_randomZipWithoutManifest_throwsNotTanzlexikon() {
        val zip = ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { z ->
                z.putNextEntry(ZipEntry("urlaub.jpg")); z.write(byteArrayOf(1, 2, 3)); z.closeEntry()
            }
        }.toByteArray()
        expectError(ArchiveError.NOT_TANZLEXIKON, zip)
        expectError(ArchiveError.NOT_TANZLEXIKON, "Hallo, das ist kein ZIP".toByteArray())
    }

    @Test fun read_truncatedFile_throwsCorrupt() {
        val bytes = write(manifest(text = "y".repeat(10_000)))
        expectError(ArchiveError.CORRUPT, bytes.copyOf(100))
    }

    @Test fun read_formatVersion2_throwsNewerFormat() {
        expectError(ArchiveError.NEWER_FORMAT, write(manifest(version = 2)))
    }

    @Test fun read_pathTraversal_throwsCorrupt() {
        val good = write(manifest())
        val zip = ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { z ->
                ZipInputStream(ByteArrayInputStream(good)).use { zin ->
                    val e = zin.nextEntry!!
                    z.putNextEntry(ZipEntry(e.name)); z.write(zin.readBytes()); z.closeEntry()
                }
                z.putNextEntry(ZipEntry("../evil")); z.write(byteArrayOf(1)); z.closeEntry()
            }
        }.toByteArray()
        expectError(ArchiveError.CORRUPT, zip)
    }
}
