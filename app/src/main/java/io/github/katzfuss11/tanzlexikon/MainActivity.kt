package io.github.katzfuss11.tanzlexikon

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.katzfuss11.tanzlexikon.ui.TanzlexikonNavHost
import io.github.katzfuss11.tanzlexikon.ui.theme.TanzlexikonTheme
import io.github.katzfuss11.tanzlexikon.ui.theme.isDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleIncoming(intent)
        val themeSettings = (application as TanzlexikonApp).themeSettings
        setContent {
            val mode by themeSettings.mode.collectAsStateWithLifecycle()
            val dark = mode.isDark()
            // Statusleiste und Navigationsleiste: helle Symbole auf dunkel, dunkle auf hell.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark },
                )
                onDispose {}
            }
            TanzlexikonTheme(dark = dark) {
                TanzlexikonNavHost()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncoming(intent)
    }

    /** Wurde eine Datei geöffnet oder geteilt, merkt sich die App sie für den Empfang-Bildschirm. */
    private fun handleIncoming(intent: Intent?) {
        val uri: Uri? = when (intent?.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND ->
                if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                else @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM)
            else -> null
        }
        if (uri != null) (application as TanzlexikonApp).pendingImport.value = uri
    }

    private companion object {
        // Dieselben halbdurchsichtigen Hintergründe der Navigationsleiste wie bei enableEdgeToEdge() ohne Angaben.
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
