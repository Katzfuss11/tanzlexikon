package io.github.katzfuss11.tanzlexikon.media

import java.net.URI

private val VIDEO_ID = Regex("[A-Za-z0-9_-]{11}")

/**
 * Liest die Video-ID aus einem YouTube-Link, z. B. aus
 * `youtube.com/watch?v=…`, `youtu.be/…`, `youtube.com/shorts/…` oder `m.youtube.com/…`,
 * mit oder ohne `https://` und mit zusätzlichen Parametern. Alles andere ergibt null.
 */
fun youtubeVideoId(url: String): String? {
    val text = url.trim()
    if (text.isEmpty() || text.any { it.isWhitespace() }) return null
    val uri = runCatching { URI(if ("://" in text) text else "https://$text") }.getOrNull() ?: return null
    val host = uri.host?.lowercase()?.removePrefix("www.")?.removePrefix("m.") ?: return null
    val segments = uri.path.orEmpty().split('/').filter { it.isNotEmpty() }
    val id = when (host) {
        "youtu.be" -> segments.firstOrNull()
        "youtube.com", "music.youtube.com" -> when (segments.firstOrNull()) {
            "watch" -> uri.rawQuery.orEmpty().split('&')
                .firstOrNull { it.startsWith("v=") }?.removePrefix("v=")
            "shorts", "embed", "live" -> segments.getOrNull(1)
            else -> null
        }
        else -> null
    }
    return id?.takeIf { VIDEO_ID.matches(it) }
}

fun youtubeThumbnailUrl(id: String) = "https://img.youtube.com/vi/$id/hqdefault.jpg"
