package io.github.katzfuss11.tanzlexikon.ui.figur

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import io.github.katzfuss11.tanzlexikon.R
import io.github.katzfuss11.tanzlexikon.data.MediaEntity
import io.github.katzfuss11.tanzlexikon.data.MediaKind

/**
 * Zeigt genau ein Video oder Bild im Vollbild, innerhalb der App.
 * So gibt es kein Weiterblättern durch die übrige Galerie.
 */
@Composable
fun MediaViewer(media: MediaEntity, onClose: () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            val uri = Uri.parse(media.uri)
            if (media.kind == MediaKind.IMAGE) {
                AsyncImage(
                    model = uri,
                    contentDescription = media.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                    factory = { context ->
                        val player = ExoPlayer.Builder(context)
                            // Die Sprungknöpfe springen in beide Richtungen 5 Sekunden.
                            .setSeekBackIncrementMs(5_000)
                            .setSeekForwardIncrementMs(5_000)
                            .build().apply {
                            setMediaItem(MediaItem.fromUri(uri))
                            prepare()
                            playWhenReady = true
                        }
                        PlayerView(context).apply {
                            this.player = player
                            // Beim Ziehen am Zeitregler läuft das Bild flüssig mit, damit man
                            // eine Stelle in der Figur genau ansteuern kann.
                            setTimeBarScrubbingEnabled(true)
                            setShowNextButton(false)
                            setShowPreviousButton(false)
                            controllerShowTimeoutMs = 4000
                        }
                    },
                    onRelease = { view ->
                        view.player?.release()
                        view.player = null
                    },
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd).safeDrawingPadding().padding(8.dp),
            ) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.back), tint = Color.White)
            }
        }
    }
}
