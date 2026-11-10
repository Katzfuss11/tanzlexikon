package io.github.katzfuss11.tanzlexikon

import android.app.Application
import android.content.Context
import android.net.Uri
import io.github.katzfuss11.tanzlexikon.share.FigureShare
import kotlinx.coroutines.flow.MutableStateFlow
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.video.VideoFrameDecoder
import io.github.katzfuss11.tanzlexikon.data.TanzDatabase
import io.github.katzfuss11.tanzlexikon.lexikon.LexikonRepository
import io.github.katzfuss11.tanzlexikon.media.GalleryWriter
import io.github.katzfuss11.tanzlexikon.media.MediaChecker
import io.github.katzfuss11.tanzlexikon.transfer.BackupService
import io.github.katzfuss11.tanzlexikon.ui.theme.ThemeSettings
import io.github.katzfuss11.tanzlexikon.update.UpdateChecker
import io.github.katzfuss11.tanzlexikon.update.UpdateInstaller
import io.github.katzfuss11.tanzlexikon.update.UpdateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Die Application-Klasse lebt so lange wie die App.
 * Hier werden Datenbank und Dienste einmalig erzeugt.
 */
class TanzlexikonApp : Application(), SingletonImageLoader.Factory {
    /** Für Arbeiten, die unabhängig von einem Bildschirm laufen. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: TanzDatabase by lazy { TanzDatabase.build(this) }
    val repository: LexikonRepository by lazy { LexikonRepository(database) }
    val mediaChecker: MediaChecker by lazy { MediaChecker(contentResolver) }
    val galleryWriter: GalleryWriter by lazy { GalleryWriter(contentResolver) }
    val backupService: BackupService by lazy { BackupService(this, repository, galleryWriter, mediaChecker) }
    val figureShare: FigureShare by lazy { FigureShare(this, mediaChecker) }
    val themeSettings: ThemeSettings by lazy { ThemeSettings.create(this) }
    val updateManager: UpdateManager by lazy {
        UpdateManager(checker = { UpdateChecker().check() }, installer = UpdateInstaller(this), scope = appScope)
    }

    /** Eine geöffnete oder geteilte .tanzfigur-Datei, die noch auf den Empfang-Bildschirm wartet. */
    val pendingImport = MutableStateFlow<Uri?>(null)

    override fun onCreate() {
        super.onCreate()
        // Was länger als 30 Tage im Papierkorb liegt, wird beim Start endgültig entfernt.
        appScope.launch { repository.purgeTrash(System.currentTimeMillis()) }
    }

    /** Bildlader für Vorschaubilder; kann auch ein Standbild aus Videos holen. */
    override fun newImageLoader(context: Context): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
}
