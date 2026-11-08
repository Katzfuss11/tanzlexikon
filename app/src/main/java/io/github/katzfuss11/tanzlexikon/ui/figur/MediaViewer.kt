package io.github.katzfuss11.tanzlexikon.ui.figur

import android.net.Uri
import android.widget.FrameLayout
import android.widget.MediaController
import android.widget.VideoView
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
                        val video = VideoView(context)
                        FrameLayout(context).apply {
                            addView(
                                video,
                                FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.WRAP_CONTENT,
                                    android.view.Gravity.CENTER,
                                ),
                            )
                            val controller = MediaController(context)
                            controller.setAnchorView(this)
                            video.setMediaController(controller)
                            video.setVideoURI(uri)
                            video.setOnPreparedListener { video.start(); controller.show(3000) }
                        }
                    },
                    onRelease = { frame -> (frame.getChildAt(0) as? VideoView)?.stopPlayback() },
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
