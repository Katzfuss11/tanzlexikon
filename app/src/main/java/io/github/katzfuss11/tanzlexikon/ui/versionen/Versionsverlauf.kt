package io.github.katzfuss11.tanzlexikon.ui.versionen

import android.content.Context

/** Eine Version im Versionsverlauf. [date] fehlt, solange die Version noch nicht erschienen ist. */
data class VersionEntry(val version: String, val date: String?, val points: List<String>)

/** Datei in den Assets, die bei jeder neuen Version oben ergänzt wird. */
const val CHANGELOG_FILE = "versionsverlauf.md"

fun loadChangelog(context: Context): List<VersionEntry> =
    context.assets.open(CHANGELOG_FILE).bufferedReader(Charsets.UTF_8).use { parseChangelog(it.readText()) }

/**
 * Liest „## 0.1.3 · 12.11.2026“ als Kopf einer Version und „- …“ als Punkte; eingerückte Folgezeilen
 * gehören zum Punkt davor. Alles vor der ersten Version (Überschrift, Hinweise) wird übergangen.
 */
fun parseChangelog(text: String): List<VersionEntry> {
    val result = mutableListOf<VersionEntry>()
    var version: String? = null
    var date: String? = null
    val points = mutableListOf<String>()
    fun finish() {
        version?.let { result += VersionEntry(it, date, points.toList()) }
        points.clear()
    }
    for (raw in text.lines()) {
        val line = raw.trimEnd()
        when {
            line.startsWith("## ") -> {
                finish()
                val head = line.removePrefix("## ").split("·", limit = 2).map { it.trim() }
                version = head[0]
                date = head.getOrNull(1)?.takeIf { it.isNotEmpty() }
            }
            version == null -> Unit
            line.startsWith("- ") -> points += line.removePrefix("- ").trim()
            line.isNotBlank() && raw.first().isWhitespace() && points.isNotEmpty() ->
                points[points.lastIndex] = points.last() + " " + line.trim()
        }
    }
    finish()
    return result
}
