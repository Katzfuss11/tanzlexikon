package io.github.katzfuss11.tanzlexikon.update

import io.github.katzfuss11.tanzlexikon.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL

/** Das neueste Release auf GitHub. */
data class Release(val version: Version, val tag: String, val apkUrl: String)

sealed interface UpdateResult {
    data class Available(val release: Release) : UpdateResult
    data object UpToDate : UpdateResult
    /** Kein Internet, Repo privat oder Antwort unbrauchbar. */
    data object Failed : UpdateResult
}

const val LATEST_RELEASE_URL = "https://api.github.com/repos/Katzfuss11/tanzlexikon/releases/latest"

/** Liest Tag und APK-Anhang aus der GitHub-Antwort. Ohne APK oder mit unlesbarer Version: null. */
fun parseLatestRelease(json: String): Release? = runCatching {
    val root = Json.parseToJsonElement(json).jsonObject
    val tag = root["tag_name"]!!.jsonPrimitive.content
    val version = parseVersion(tag) ?: return null
    val apk = root["assets"]!!.jsonArray
        .map { it.jsonObject }
        .firstOrNull { it["name"]?.jsonPrimitive?.content?.endsWith(".apk", ignoreCase = true) == true }
        ?: return null
    Release(version, tag, apk["browser_download_url"]!!.jsonPrimitive.content)
}.getOrNull()

/** Fragt GitHub, ob es eine neuere Version gibt. Wirft nie; Probleme ergeben [UpdateResult.Failed]. */
class UpdateChecker(
    private val fetch: suspend (url: String) -> String? = ::httpGet,
    private val current: String = BuildConfig.VERSION_NAME,
) {
    suspend fun check(): UpdateResult {
        val body = try { fetch(LATEST_RELEASE_URL) } catch (e: Exception) { null } ?: return UpdateResult.Failed
        val release = parseLatestRelease(body) ?: return UpdateResult.Failed
        val installed = parseVersion(current) ?: return UpdateResult.Failed
        return if (release.version > installed) UpdateResult.Available(release) else UpdateResult.UpToDate
    }
}

/** Holt eine Seite als Text. Bei Fehlern oder einem Status außer 200 (z. B. 404 bei privatem Repo): null. */
suspend fun httpGet(url: String): String? = withContext(Dispatchers.IO) {
    try {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 10_000
            c.readTimeout = 10_000
            c.setRequestProperty("Accept", "application/vnd.github+json")
            c.setRequestProperty("User-Agent", "Tanzlexikon-App")
            if (c.responseCode != 200) null else c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    } catch (e: Exception) {
        null
    }
}
