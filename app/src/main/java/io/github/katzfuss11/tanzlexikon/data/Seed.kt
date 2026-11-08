package io.github.katzfuss11.tanzlexikon.data

/**
 * Die Grundliste, mit der eine neue Datenbank startet. Sie ist nur ein Vorschlag:
 * alles lässt sich in der App umbenennen, umsortieren, ergänzen oder löschen.
 * Die Reihenfolge hier wird zur Reihenfolge in der App.
 */
val SEED: List<Pair<String, List<String>>> = listOf(
    "Standard" to listOf("Langsamer Walzer", "Tango", "Wiener Walzer", "Slowfox", "Quickstep"),
    "Latein" to listOf("Cha-Cha-Cha", "Samba", "Rumba", "Paso Doble", "Jive", "Salsa"),
    "Sonstige" to listOf("Discofox", "Argentinischer Tango"),
)
