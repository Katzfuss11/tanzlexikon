package io.github.katzfuss11.tanzlexikon.share

import io.github.katzfuss11.tanzlexikon.lexikon.CategoryWithDances

/** Wohin eine empfangene Figur einsortiert wird. */
sealed interface ImportTarget {
    data class Existing(val danceId: Long, val danceName: String, val categoryName: String) : ImportTarget
    data class New(val categoryName: String, val danceName: String) : ImportTarget
}

/**
 * Schlägt den Zieltanz vor: Gibt es beim Empfänger irgendwo einen Tanz mit diesem Namen
 * (ohne Rücksicht auf Groß-/Kleinschreibung), dann diesen. Sonst einen neuen Tanz, und zwar in der
 * Kategorie mit gleichem Namen in der Schreibweise des Empfängers.
 */
fun proposeTarget(existing: List<CategoryWithDances>, categoryName: String, danceName: String): ImportTarget {
    val dance = danceName.trim()
    existing.forEach { c ->
        c.dances.firstOrNull { it.name.trim().equals(dance, ignoreCase = true) }?.let {
            return ImportTarget.Existing(it.id, it.name, c.name)
        }
    }
    val category = existing.firstOrNull { it.name.trim().equals(categoryName.trim(), ignoreCase = true) }?.name
        ?: categoryName.trim()
    return ImportTarget.New(category, dance)
}
