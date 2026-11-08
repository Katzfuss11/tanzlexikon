package io.github.katzfuss11.tanzlexikon.transfer

import java.io.OutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ArchiveWriter {
    /**
     * Schreibt erst die Beschreibung, dann die Dateien. Die Dateien werden in kleinen Stücken
     * kopiert, damit auch große Videos nie ganz im Speicher liegen.
     * [onProgress] meldet die bisher kopierten Bytes und die Gesamtgröße der Dateien.
     */
    fun write(
        out: OutputStream,
        manifest: ArchiveManifest,
        files: List<ArchiveFile>,
        onProgress: (done: Long, total: Long) -> Unit,
    ) {
        val total = files.sumOf { it.size }
        var done = 0L
        val zip = ZipOutputStream(out)
        zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
        zip.write(archiveJson.encodeToString(ArchiveManifest.serializer(), manifest).toByteArray(Charsets.UTF_8))
        zip.closeEntry()

        // Videos und Bilder sind schon komprimiert; nochmal packen kostet nur Zeit.
        zip.setLevel(Deflater.NO_COMPRESSION)
        val buffer = ByteArray(64 * 1024)
        for (file in files) {
            zip.putNextEntry(ZipEntry(file.path))
            file.open().use { input ->
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    zip.write(buffer, 0, n)
                    done += n
                    onProgress(done, total)
                }
            }
            zip.closeEntry()
        }
        zip.finish()
        zip.flush()
        onProgress(total, total)
    }
}
