package io.github.katzfuss11.tanzlexikon.update

/** Eine Versionsnummer wie 0.10.2. Verglichen wird Zahl für Zahl, nicht als Text. */
data class Version(val major: Int, val minor: Int, val patch: Int) : Comparable<Version> {
    override fun compareTo(other: Version): Int =
        compareValuesBy(this, other, Version::major, Version::minor, Version::patch)

    override fun toString() = "$major.$minor.$patch"
}

/** Liest "v1.2.3", "1.2" oder "1.2.0-beta". Ein führendes v und Zusätze nach - oder + werden ignoriert. */
fun parseVersion(s: String): Version? {
    val core = s.trim().removePrefix("v").removePrefix("V").substringBefore('-').substringBefore('+')
    val parts = core.split('.')
    if (parts.isEmpty() || parts.size > 3) return null
    val numbers = parts.map { it.toIntOrNull()?.takeIf { n -> n >= 0 } ?: return null }
    return Version(numbers[0], numbers.getOrElse(1) { 0 }, numbers.getOrElse(2) { 0 })
}
