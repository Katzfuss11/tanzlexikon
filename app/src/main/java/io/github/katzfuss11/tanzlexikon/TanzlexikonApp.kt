package io.github.katzfuss11.tanzlexikon

import android.app.Application
import io.github.katzfuss11.tanzlexikon.data.TanzDatabase

/**
 * Die Application-Klasse lebt so lange wie die App.
 * Hier werden Datenbank und Dienste einmalig erzeugt.
 */
class TanzlexikonApp : Application() {
    val database: TanzDatabase by lazy { TanzDatabase.build(this) }
}
