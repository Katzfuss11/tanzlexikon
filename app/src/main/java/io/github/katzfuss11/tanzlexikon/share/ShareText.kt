package io.github.katzfuss11.tanzlexikon.share

import io.github.katzfuss11.tanzlexikon.data.MediaKind
import io.github.katzfuss11.tanzlexikon.data.Section
import io.github.katzfuss11.tanzlexikon.lexikon.FigureDetail

/** Die Überschriften der drei Bereiche, aus den App-Texten. */
data class ShareLabels(val general: String, val leader: String, val follower: String)

/**
 * Der Text, wenn eine Figur als Nachricht geteilt wird: Tanz und Figur, dann je Bereich der Text
 * und die YouTube-Links. Leere Bereiche fallen weg.
 */
fun buildShareText(d: FigureDetail, labels: ShareLabels): String {
    val f = d.figure
    val sections = listOf(
        Triple(Section.GENERAL, labels.general, f.generalText),
        Triple(Section.LEADER, labels.leader, f.leaderText),
        Triple(Section.FOLLOWER, labels.follower, f.followerText),
    )
    val blocks = sections.mapNotNull { (section, label, text) ->
        val links = d.media.filter { it.section == section && it.kind == MediaKind.YOUTUBE }.map { it.uri }
        val lines = listOfNotNull(text.trim().takeIf { it.isNotEmpty() }) + links
        if (lines.isEmpty()) null else "$label:\n" + lines.joinToString("\n")
    }
    val title = listOf(d.danceName, f.name).filter { it.isNotBlank() }.joinToString(" – ")
    return (listOf(title) + blocks).joinToString("\n\n") + "\n\nGeteilt mit dem Tanzlexikon"
}
