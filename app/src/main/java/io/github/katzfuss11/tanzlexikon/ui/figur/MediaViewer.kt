package io.github.katzfuss11.tanzlexikon.ui.figur

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.abs
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.runtime.remember
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
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
                // Geht die App in den Hintergrund, hält das Video an.
                val playerRef = remember { arrayOfNulls<ExoPlayer>(1) }
                LifecycleEventEffect(Lifecycle.Event.ON_STOP) { playerRef[0]?.pause() }
                AndroidView(
                    modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                    factory = { context ->
                        val player = ExoPlayer.Builder(context)
                            // Die Sprungknöpfe springen in beide Richtungen 5 Sekunden.
                            .setSeekBackIncrementMs(5_000)
                            .setSeekForwardIncrementMs(5_000)
                            // Andere Wiedergaben (Musik, Anruf) bekommen Vorrang bzw. werden angehalten.
                            .setAudioAttributes(
                                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
                                /* handleAudioFocus = */ true,
                            )
                            .build().apply {
                            setMediaItem(MediaItem.fromUri(uri))
                            prepare()
                            playWhenReady = true
                        }.also { playerRef[0] = it }
                        val playerView = PlayerView(context).apply {
                            this.player = player
                            // Beim Ziehen am Zeitregler läuft das Bild flüssig mit, damit man
                            // eine Stelle in der Figur genau ansteuern kann.
                            setTimeBarScrubbingEnabled(true)
                            setShowNextButton(false)
                            setShowPreviousButton(false)
                            controllerShowTimeoutMs = 4000
                        }
                        SwipeScrubLayout(context, playerView, player)
                    },
                    onRelease = { view -> view.release() },
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

/**
 * Legt sich über den Player: Wer im Bild waagerecht wischt, hält das Video an und spult fein mit
 * dem Finger mit (eine ganze Bildschirmbreite = [MS_PER_SCREEN_WIDTH]). Beim Loslassen läuft es
 * weiter, wenn es vorher lief. Antippen und der Zeitregler unten funktionieren wie gewohnt.
 */
@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
class SwipeScrubLayout(
    context: Context,
    private val playerView: PlayerView,
    private val player: ExoPlayer,
) : FrameLayout(context) {
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val controlsHeightPx = 120 * resources.displayMetrics.density
    private var downX = 0f
    private var downY = 0f
    private var startPosition = 0L
    private var wasPlaying = false
    private var scrubbing = false

    init {
        addView(playerView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                scrubbing = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.x - downX
                val dy = ev.y - downY
                // Den Bereich der Steuerleiste unten (Zeitregler) nicht anfassen.
                val onControls = playerView.isControllerFullyVisible && downY > height - controlsHeightPx
                if (!onControls && abs(dx) > touchSlop && abs(dx) > abs(dy)) {
                    startScrub()
                    return true
                }
            }
        }
        return false
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!scrubbing) return super.onTouchEvent(ev)
        when (ev.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                val offset = ((ev.x - downX) / width * MS_PER_SCREEN_WIDTH).toLong()
                val duration = player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                player.seekTo((startPosition + offset).coerceIn(0L, duration))
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> endScrub()
        }
        return true
    }

    private fun startScrub() {
        scrubbing = true
        wasPlaying = player.isPlaying
        player.pause()
        player.setScrubbingModeEnabled(true)
        startPosition = player.currentPosition
        // Die Steuerleiste zeigt dabei, wo man gerade ist.
        playerView.showController()
    }

    private fun endScrub() {
        scrubbing = false
        player.setScrubbingModeEnabled(false)
        if (wasPlaying) player.play()
    }

    fun release() {
        playerView.player = null
        player.release()
    }

    companion object {
        const val MS_PER_SCREEN_WIDTH = 4_000L
    }
}
