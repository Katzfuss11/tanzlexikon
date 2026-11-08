package io.github.katzfuss11.tanzlexikon

import android.app.Application
import io.github.katzfuss11.tanzlexikon.data.TanzDatabase
import io.github.katzfuss11.tanzlexikon.lexikon.LexikonRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Die Application-Klasse lebt so lange wie die App.
 * Hier werden Datenbank und Dienste einmalig erzeugt.
 */
class TanzlexikonApp : Application() {
    /** Für Arbeiten, die unabhängig von einem Bildschirm laufen. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: TanzDatabase by lazy { TanzDatabase.build(this) }
    val repository: LexikonRepository by lazy { LexikonRepository(database) }

    override fun onCreate() {
        super.onCreate()
        // Was länger als 30 Tage im Papierkorb liegt, wird beim Start endgültig entfernt.
        appScope.launch { repository.purgeTrash(System.currentTimeMillis()) }
    }
}
