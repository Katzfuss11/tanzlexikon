package io.github.katzfuss11.tanzlexikon.transfer

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import io.github.katzfuss11.tanzlexikon.BuildConfig
import io.github.katzfuss11.tanzlexikon.data.MediaEntity
import io.github.katzfuss11.tanzlexikon.data.MediaKind
import io.github.katzfuss11.tanzlexikon.lexikon.LexikonRepository
import io.github.katzfuss11.tanzlexikon.lexikon.MISSING_URI
import io.github.katzfuss11.tanzlexikon.media.GalleryWriter
import io.github.katzfuss11.tanzlexikon.media.MediaChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface ImportOutcome {
    data class Success(val figures: Int, val missingMedia: Int) : ImportOutcome
    data class Failed(val error: ArchiveError) : ImportOutcome

    /** Die Datei ist eine einzelne geteilte Figur, keine Sicherung. */
    data object WrongKind : ImportOutcome
    data class NotEnoughSpace(val requiredBytes: Long) : ImportOutcome
}

/**
 * Sicherung des ganzen Bestands als ZIP und Wiederherstellung daraus.
 * Beim Import wird die Datenbank erst angefasst, wenn alle Videos und Bilder in der Galerie liegen.
 */
class BackupService(
    private val context: Context,
    private val repository: LexikonRepository,
    private val galleryWriter: GalleryWriter,
    private val mediaChecker: MediaChecker,
    private val availableBytes: () -> Long = { StatFs(Environment.getExternalStorageDirectory().path).availableBytes },
) {
    private val resolver = context.contentResolver

    suspend fun export(target: OutputStream, onProgress: (Long, Long) -> Unit) = withContext(Dispatchers.IO) {
        val snap = repository.snapshot(mediaChecker::isAvailable)
        val files = snap.files.map { (path, m) -> ArchiveFile(path, sizeOf(m)) { open(m) } }
        val manifest = ArchiveManifest(kind = ArchiveKind.BACKUP, appVersion = BuildConfig.VERSION_NAME, categories = snap.categories)
        ArchiveWriter().write(target, manifest, files, onProgress)
    }

    suspend fun import(source: () -> InputStream, onProgress: (Long, Long) -> Unit): ImportOutcome = withContext(Dispatchers.IO) {
        // 1. Beschreibung lesen und prüfen. Bei einem Fehler bleibt alles, wie es ist.
        val manifest = try {
            source().use { ArchiveReader().read(it) { _, _ -> } }
        } catch (e: ArchiveException) {
            return@withContext ImportOutcome.Failed(e.reason)
        }
        if (manifest.kind != ArchiveKind.BACKUP) return@withContext ImportOutcome.WrongKind

        // 2. Reicht der Platz für die Videos und Bilder und für die Sicherheitskopie des jetzigen Stands?
        val withFile = manifest.categories.flatMap { c -> c.dances.flatMap { d -> d.figures.flatMap { it.media } } }
            .filter { it.file != null }
        val required = withFile.sumOf { it.sizeBytes ?: 0L }
        val safetyCopy = repository.snapshot(mediaChecker::isAvailable).files.sumOf { (_, m) -> sizeOf(m) }
        if (!hasEnoughSpace(required + safetyCopy, availableBytes())) {
            return@withContext ImportOutcome.NotEnoughSpace(required + safetyCopy)
        }

        // 3. Den jetzigen Stand sichern, bevor etwas überschrieben wird.
        safetyExport(System.currentTimeMillis())

        // 4. Videos und Bilder in die Galerie legen.
        val byPath = withFile.associateBy { it.file!! }
        val uris = mutableMapOf<String, String>()
        var done = 0L
        try {
            source().use { input ->
                ArchiveReader().read(input) { path, data ->
                    val m = byPath[path] ?: return@read
                    val counting = CountingStream(data) { n -> done += n; onProgress(done, required) }
                    val uri = galleryWriter.save(
                        displayName = m.displayName ?: "tanzlexikon-${path.substringAfterLast('/')}",
                        mimeType = m.mimeType ?: if (m.kind == MediaKind.IMAGE) "image/jpeg" else "video/mp4",
                        size = m.sizeBytes ?: -1L,
                        data = counting,
                    )
                    uris[path] = uri.toString()
                }
            }
        } catch (e: ArchiveException) {
            return@withContext ImportOutcome.Failed(e.reason)
        }

        // 5. Erst jetzt die Datenbank ersetzen.
        var missing = 0
        repository.replaceAll(manifest) { m ->
            when {
                m.kind == MediaKind.YOUTUBE -> m.youtubeUrl ?: MISSING_URI
                m.file != null && uris[m.file] != null -> uris.getValue(m.file)
                else -> MISSING_URI.also { missing++ }
            }
        }
        onProgress(required, required)
        val figures = manifest.categories.sumOf { c -> c.dances.sumOf { it.figures.size } }
        ImportOutcome.Success(figures, missing)
    }

    /** Sichert den jetzigen Stand in den Download-Ordner, Unterordner "Tanzlexikon". */
    suspend fun safetyExport(now: Long): Uri = withContext(Dispatchers.IO) {
        val name = "tanzlexikon-vor-import-" + SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.ROOT).format(Date(now)) + ".zip"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Tanzlexikon")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Sicherung im Download-Ordner nicht möglich")
        resolver.openOutputStream(uri)!!.use { export(it) { _, _ -> } }
        resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        uri
    }

    /** Mindestens 50 MB sollen nach dem Import noch frei sein. */
    fun hasEnoughSpace(required: Long, available: Long): Boolean = available >= required + 50L * 1024 * 1024

    private fun open(m: MediaEntity): InputStream =
        resolver.openInputStream(Uri.parse(m.uri)) ?: throw IOException("Datei nicht lesbar: ${m.uri}")

    private fun sizeOf(m: MediaEntity): Long =
        m.sizeBytes ?: runCatching { resolver.openFileDescriptor(Uri.parse(m.uri), "r")?.use { it.statSize } }.getOrNull() ?: 0L

    /** Zählt mit, wie viel schon gelesen wurde (für den Fortschrittsbalken). */
    private class CountingStream(inner: InputStream, private val onRead: (Long) -> Unit) : FilterInputStream(inner) {
        override fun read(): Int = super.read().also { if (it >= 0) onRead(1) }
        override fun read(b: ByteArray, off: Int, len: Int): Int = super.read(b, off, len).also { if (it > 0) onRead(it.toLong()) }
    }
}
