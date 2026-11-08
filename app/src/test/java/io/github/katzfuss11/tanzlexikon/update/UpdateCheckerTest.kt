package io.github.katzfuss11.tanzlexikon.update

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateCheckerTest {
    private fun json(tag: String, assets: String = """{"name": "tanzlexikon-$tag.apk", "browser_download_url": "https://example.org/$tag.apk"}""") = """
        {
          "tag_name": "$tag",
          "name": "Version $tag",
          "body": "Neu: Teilen",
          "assets": [
            {"name": "notes.txt", "browser_download_url": "https://example.org/notes.txt"},
            $assets
          ]
        }
    """.trimIndent()

    @Test fun parseLatestRelease_readsTagAndApkUrl() {
        val r = parseLatestRelease(json("v0.2.0"))!!
        assertEquals("v0.2.0", r.tag)
        assertEquals(Version(0, 2, 0), r.version)
        assertEquals("https://example.org/v0.2.0.apk", r.apkUrl)
    }

    @Test fun parseLatestRelease_withoutApk_returnsNull() {
        assertNull(parseLatestRelease(json("v0.2.0", assets = """{"name": "a.zip", "browser_download_url": "https://example.org/a.zip"}""")))
    }

    @Test fun parseLatestRelease_notJson_returnsNull() {
        assertNull(parseLatestRelease("<html>nope</html>"))
    }

    @Test fun check_newerRemote_returnsAvailable() = runTest {
        val result = UpdateChecker(fetch = { json("v0.2.0") }, current = "0.1.0").check()
        assertEquals("v0.2.0", (result as UpdateResult.Available).release.tag)
    }

    @Test fun check_sameVersion_returnsUpToDate() = runTest {
        assertEquals(UpdateResult.UpToDate, UpdateChecker(fetch = { json("v0.1.0") }, current = "0.1.0").check())
    }

    @Test fun check_olderRemote_returnsUpToDate() = runTest {
        assertEquals(UpdateResult.UpToDate, UpdateChecker(fetch = { json("v0.0.9") }, current = "0.1.0").check())
    }

    @Test fun check_fetchReturnsNull_returnsFailed_noThrow() = runTest {
        assertEquals(UpdateResult.Failed, UpdateChecker(fetch = { null }, current = "0.1.0").check())
    }

    @Test fun check_fetchThrows_returnsFailed() = runTest {
        assertEquals(UpdateResult.Failed, UpdateChecker(fetch = { throw java.io.IOException("offline") }, current = "0.1.0").check())
    }

    @Test fun check_unparsableTag_returnsFailed() = runTest {
        assertEquals(UpdateResult.Failed, UpdateChecker(fetch = { json("neueste") }, current = "0.1.0").check())
    }
}
