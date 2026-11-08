package io.github.katzfuss11.tanzlexikon.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import io.github.katzfuss11.tanzlexikon.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Was der UpdateManager zum Installieren braucht. */
interface ApkInstaller {
    /** Darf die App Installationen anstoßen? ("Unbekannte Apps installieren") */
    fun canInstall(): Boolean
    suspend fun download(release: Release, onProgress: (Long, Long) -> Unit): File
    fun install(apk: File)
    fun openPermissionSettings()
}

class UpdateInstaller(private val context: Context) : ApkInstaller {
    override fun canInstall() = context.packageManager.canRequestPackageInstalls()

    /** Lädt nach cache/updates/tanzlexikon-<tag>.apk; erst als .part, damit nie eine halbe APK übrig bleibt. */
    override suspend fun download(release: Release, onProgress: (Long, Long) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val name = "tanzlexikon-" + release.tag.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val part = File(dir, "$name.part")
        val target = File(dir, "$name.apk")
        val c = URL(release.apkUrl).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 10_000
            c.readTimeout = 30_000
            c.setRequestProperty("User-Agent", "Tanzlexikon-App")
            if (c.responseCode != 200) throw IOException("HTTP ${c.responseCode}")
            val total = c.contentLengthLong
            var done = 0L
            c.inputStream.use { input ->
                part.outputStream().use { out ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        done += n
                        onProgress(done, total)
                    }
                }
            }
        } finally {
            c.disconnect()
        }
        if (!part.renameTo(target)) throw IOException("APK nicht gespeichert")
        target
    }

    override fun install(apk: File) {
        val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.files", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    override fun openPermissionSettings() {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${BuildConfig.APPLICATION_ID}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
