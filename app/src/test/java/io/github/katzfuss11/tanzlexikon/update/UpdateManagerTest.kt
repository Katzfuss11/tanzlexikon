package io.github.katzfuss11.tanzlexikon.update

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateManagerTest {
    private val release = Release(Version(0, 2, 0), "v0.2.0", "https://example.org/a.apk")
    private val scope = CoroutineScope(UnconfinedTestDispatcher())

    private class FakeInstaller(var allowed: Boolean = true, val fail: Boolean = false) : ApkInstaller {
        val installed = mutableListOf<File>()
        var settingsOpened = 0
        override fun canInstall() = allowed
        override suspend fun download(release: Release, onProgress: (Long, Long) -> Unit): File {
            if (fail) throw java.io.IOException("weg")
            onProgress(5, 10)
            return File("x.apk")
        }
        override fun install(apk: File) { installed += apk }
        override fun openPermissionSettings() { settingsOpened++ }
    }

    private fun manager(result: UpdateResult, installer: ApkInstaller = FakeInstaller()) =
        UpdateManager(checker = { result }, installer = installer, scope = scope, current = "0.1.0")

    @Test fun startupCheck_failed_staysSilent() {
        val m = manager(UpdateResult.Failed)
        m.startupCheck()
        assertEquals(UpdateState.Idle, m.state.value)
    }

    @Test fun startupCheck_upToDate_staysSilent() {
        val m = manager(UpdateResult.UpToDate)
        m.startupCheck()
        assertEquals(UpdateState.Idle, m.state.value)
    }

    @Test fun startupCheck_available_offersOnlyOncePerProcess() {
        val m = manager(UpdateResult.Available(release))
        m.startupCheck()
        assertEquals(UpdateState.Offer(release), m.state.value)
        m.dismiss()
        m.startupCheck()
        assertEquals(UpdateState.Idle, m.state.value)
    }

    @Test fun manualCheck_reportsEveryOutcome() {
        assertEquals(UpdateState.CheckFailed, manager(UpdateResult.Failed).also { it.manualCheck() }.state.value)
        assertEquals(UpdateState.UpToDate("0.1.0"), manager(UpdateResult.UpToDate).also { it.manualCheck() }.state.value)
        assertEquals(UpdateState.Offer(release), manager(UpdateResult.Available(release)).also { it.manualCheck() }.state.value)
    }

    @Test fun download_withPermission_installs() {
        val installer = FakeInstaller()
        val m = manager(UpdateResult.Available(release), installer)
        m.download(release)
        assertEquals(listOf(File("x.apk")), installer.installed)
        assertEquals(UpdateState.Idle, m.state.value)
    }

    @Test fun download_withoutPermission_asksFirstThenInstallsAfterRetry() {
        val installer = FakeInstaller(allowed = false)
        val m = manager(UpdateResult.Available(release), installer)
        m.download(release)
        assertEquals(UpdateState.NeedPermission(release), m.state.value)
        assertTrue(installer.installed.isEmpty())
        m.openPermissionSettings()
        assertEquals(1, installer.settingsOpened)
        installer.allowed = true
        m.download(release)
        assertEquals(1, installer.installed.size)
    }

    @Test fun download_failure_reportsError() {
        val m = manager(UpdateResult.Available(release), FakeInstaller(fail = true))
        m.download(release)
        assertEquals(UpdateState.DownloadFailed, m.state.value)
    }
}
