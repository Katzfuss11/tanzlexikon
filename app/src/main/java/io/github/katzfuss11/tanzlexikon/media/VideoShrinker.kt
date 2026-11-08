package io.github.katzfuss11.tanzlexikon.media

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

/** Rechnet ein Video in eine kleinere Fassung um. Gibt false zurück, wenn das nicht geklappt hat. */
fun interface VideoShrinker {
    suspend fun shrink(source: Uri, target: File, onProgress: (Float) -> Unit): Boolean
}

/**
 * Verkleinert mit Media3 Transformer auf 720p (kürzere Seite) und etwa 3 Mbit/s.
 * HDR-Aufnahmen werden dabei in normale Farben umgerechnet, damit jedes Handy sie abspielen kann.
 */
@OptIn(UnstableApi::class)
class TransformerShrinker(private val context: Context) : VideoShrinker {
    override suspend fun shrink(source: Uri, target: File, onProgress: (Float) -> Unit): Boolean =
        // Transformer muss auf einem Thread mit Looper laufen; der Hauptthread ist dafür gedacht.
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                val handler = Handler(Looper.getMainLooper())
                lateinit var poll: Runnable
                val transformer = Transformer.Builder(context)
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .setEncoderFactory(
                        DefaultEncoderFactory.Builder(context)
                            .setRequestedVideoEncoderSettings(VideoEncoderSettings.Builder().setBitrate(BITRATE).build())
                            .build(),
                    )
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            handler.removeCallbacks(poll)
                            if (cont.isActive) cont.resume(true)
                        }

                        override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                            handler.removeCallbacks(poll)
                            target.delete()
                            if (cont.isActive) cont.resume(false)
                        }
                    })
                    .build()
                val item = EditedMediaItem.Builder(MediaItem.fromUri(source))
                    .setEffects(Effects(listOf(), listOf(Presentation.createForShortSide(SHORT_SIDE))))
                    .build()
                val composition = Composition.Builder(EditedMediaItemSequence.withAudioAndVideoFrom(listOf(item)))
                    .setHdrMode(Composition.HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_OPEN_GL)
                    .build()
                val holder = ProgressHolder()
                poll = Runnable {
                    if (transformer.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) onProgress(holder.progress / 100f)
                    handler.postDelayed(poll, 250)
                }
                cont.invokeOnCancellation {
                    handler.post {
                        handler.removeCallbacks(poll)
                        transformer.cancel()
                        target.delete()
                    }
                }
                try {
                    transformer.start(composition, target.absolutePath)
                    handler.post(poll)
                } catch (e: Exception) {
                    target.delete()
                    cont.resume(false)
                }
            }
        }

    private companion object {
        const val SHORT_SIDE = 720
        const val BITRATE = 3_000_000
    }
}
