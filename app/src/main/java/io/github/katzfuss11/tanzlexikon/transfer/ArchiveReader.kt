package io.github.katzfuss11.tanzlexikon.transfer

import kotlinx.serialization.SerializationException
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream

class ArchiveReader {
    /**
     * Liest ein Archiv: erst die Beschreibung, dann jede Datei über [onFile].
     * Wirft [ArchiveException], wenn die Datei kein Tanzlexikon-Archiv ist, aus einer neueren
     * App-Version stammt oder beschädigt ist.
     */
    fun read(input: InputStream, onFile: (path: String, data: InputStream) -> Unit): ArchiveManifest {
        try {
            val zip = ZipInputStream(input)
            val first = zip.nextEntry
            if (first == null || first.name != MANIFEST_ENTRY) throw ArchiveException(ArchiveError.NOT_TANZLEXIKON)

            val text = zip.readBytes().toString(Charsets.UTF_8)
            val version = archiveJson.parseToJsonElement(text).let {
                (it as? kotlinx.serialization.json.JsonObject)?.get("formatVersion")
                    ?.let { v -> (v as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() }
            } ?: throw ArchiveException(ArchiveError.CORRUPT)
            // Erst die Version prüfen: ein neueres Format könnte sich anders lesen.
            if (version > CURRENT_FORMAT) throw ArchiveException(ArchiveError.NEWER_FORMAT)
            val manifest = archiveJson.decodeFromString(ArchiveManifest.serializer(), text)

            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) continue
                if (!isSafePath(entry.name)) throw ArchiveException(ArchiveError.CORRUPT)
                try {
                    onFile(entry.name, Source(zip))
                } catch (e: SourceReadException) {
                    throw ArchiveException(ArchiveError.CORRUPT, e.cause)
                } catch (e: ArchiveException) {
                    throw e
                } catch (e: Exception) {
                    // Das Archiv war lesbar, aber das Ziel (Galerie, Speicher) hat abgelehnt.
                    throw ArchiveException(ArchiveError.WRITE_FAILED, e)
                }
            }
            return manifest
        } catch (e: ArchiveException) {
            throw e
        } catch (e: IOException) {
            throw ArchiveException(ArchiveError.CORRUPT, e)
        } catch (e: SerializationException) {
            throw ArchiveException(ArchiveError.CORRUPT, e)
        } catch (e: IllegalArgumentException) {
            throw ArchiveException(ArchiveError.CORRUPT, e)
        }
    }

    /** Kein Pfad darf aus dem Archiv herausführen. */
    private fun isSafePath(name: String): Boolean {
        val normalized = name.replace('\\', '/')
        if (normalized.startsWith("/") || normalized.contains(':')) return false
        return normalized.split('/').none { it == ".." }
    }

    private class SourceReadException(cause: IOException) : IOException(cause)

    /**
     * Gibt eine Datei aus dem ZIP weiter. Lesefehler werden markiert, damit sie sich von
     * Schreibfehlern beim Empfänger unterscheiden lassen. Der Empfänger kann das ZIP nicht schließen.
     */
    private class Source(inner: InputStream) : FilterInputStream(inner) {
        override fun read(): Int = guard { super.read() }
        override fun read(b: ByteArray, off: Int, len: Int): Int = guard { super.read(b, off, len) }
        override fun skip(n: Long): Long = guard { super.skip(n) }
        override fun close() = Unit

        private inline fun <T> guard(block: () -> T): T =
            try { block() } catch (e: IOException) { throw SourceReadException(e) }
    }
}
