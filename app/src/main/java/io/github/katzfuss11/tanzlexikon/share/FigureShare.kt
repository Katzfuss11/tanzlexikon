package io.github.katzfuss11.tanzlexikon.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import io.github.katzfuss11.tanzlexikon.BuildConfig
import io.github.katzfuss11.tanzlexikon.R
import io.github.katzfuss11.tanzlexikon.data.MediaKind
import io.github.katzfuss11.tanzlexikon.lexikon.FigureDetail
import io.github.katzfuss11.tanzlexikon.lexikon.fileExtension
import io.github.katzfuss11.tanzlexikon.media.MediaChecker
import io.github.katzfuss11.tanzlexikon.transfer.ArchCategory
import io.github.katzfuss11.tanzlexikon.transfer.ArchDance
import io.github.katzfuss11.tanzlexikon.transfer.ArchFigure
import io.github.katzfuss11.tanzlexikon.transfer.ArchMedia
import io.github.katzfuss11.tanzlexikon.transfer.ArchiveFile
import io.github.katzfuss11.tanzlexikon.transfer.ArchiveKind
import io.github.katzfuss11.tanzlexikon.transfer.ArchiveManifest
import io.github.katzfuss11.tanzlexikon.transfer.ArchiveWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** Teilt eine Figur als Nachricht (Text + Anhänge) oder als .tanzfigur-Datei. */
class FigureShare(
    private val context: Context,
    private val mediaChecker: MediaChecker,
    private val archiveWriter: ArchiveWriter = ArchiveWriter(),
) {
    private val labels
        get() = ShareLabels(
            context.getString(R.string.section_general),
            context.getString(R.string.section_leader),
            context.getString(R.string.section_follower),
        )

    /** Text mit YouTube-Links, dazu die eigenen Videos und Bilder, die noch da sind. */
    suspend fun shareAsMessage(d: FigureDetail) {
        val attachments = withContext(Dispatchers.IO) {
            d.media.filter { it.kind != MediaKind.YOUTUBE && mediaChecker.isAvailable(it) }.map { Uri.parse(it.uri) }
        }
        val text = buildShareText(d, labels)
        val intent = when (attachments.size) {
            0 -> Intent(Intent.ACTION_SEND).apply { type = "text/plain" }
            1 -> Intent(Intent.ACTION_SEND).apply {
                type = typeFor(d)
                putExtra(Intent.EXTRA_STREAM, attachments.single())
            }
            else -> Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = typeFor(d)
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(attachments))
            }
        }.apply {
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, d.figure.name)
            if (attachments.isNotEmpty()) {
                clipData = ClipData.newRawUri(null, attachments.first()).also { clip ->
                    attachments.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        start(intent)
    }

    /** Packt die Figur samt Videos und Bildern in eine .tanzfigur-Datei und öffnet das Teilen-Menü. */
    suspend fun shareAsFile(d: FigureDetail, onProgress: (Long, Long) -> Unit = { _, _ -> }) {
        val file = withContext(Dispatchers.IO) { writeFigureFile(d, onProgress) }
        val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.files", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        start(intent)
    }

    private fun writeFigureFile(d: FigureDetail, onProgress: (Long, Long) -> Unit): File {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        // Alte Dateien von früherem Teilen wegräumen.
        dir.listFiles()?.forEach { it.delete() }
        val files = mutableListOf<ArchiveFile>()
        val media = d.media.map { m ->
            val path = if (m.kind != MediaKind.YOUTUBE && mediaChecker.isAvailable(m)) {
                val p = "media/${files.size + 1}.${fileExtension(m)}"
                val size = m.sizeBytes ?: runCatching {
                    context.contentResolver.openFileDescriptor(Uri.parse(m.uri), "r")?.use { it.statSize }
                }.getOrNull() ?: 0L
                files += ArchiveFile(p, size) {
                    context.contentResolver.openInputStream(Uri.parse(m.uri)) ?: throw IOException("nicht lesbar")
                }
                p
            } else {
                null
            }
            ArchMedia(
                section = m.section, kind = m.kind, position = m.position,
                youtubeUrl = m.uri.takeIf { m.kind == MediaKind.YOUTUBE },
                file = path, displayName = m.displayName, mimeType = m.mimeType, sizeBytes = m.sizeBytes,
            )
        }
        val f = d.figure
        val manifest = ArchiveManifest(
            kind = ArchiveKind.FIGURE,
            appVersion = BuildConfig.VERSION_NAME,
            categories = listOf(
                ArchCategory(d.categoryName, 0, listOf(ArchDance(d.danceName, 0, listOf(
                    ArchFigure(f.name, 0, f.generalText, f.leaderText, f.followerText, media),
                )))),
            ),
        )
        val target = File(dir, safeFileName(f.name) + ".tanzfigur")
        target.outputStream().use { archiveWriter.write(it, manifest, files, onProgress) }
        return target
    }

    private fun typeFor(d: FigureDetail): String {
        val kinds = d.media.filter { it.kind != MediaKind.YOUTUBE }.map { it.kind }.toSet()
        return when (kinds) {
            setOf(MediaKind.VIDEO) -> "video/*"
            setOf(MediaKind.IMAGE) -> "image/*"
            else -> "*/*"
        }
    }

    private fun start(intent: Intent) {
        val chooser = Intent.createChooser(intent, context.getString(R.string.share)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}

/** Macht aus einem Figurnamen einen Dateinamen, der überall funktioniert. */
fun safeFileName(name: String): String =
    name.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_").trim().trim('.').take(60).ifBlank { "Figur" }
