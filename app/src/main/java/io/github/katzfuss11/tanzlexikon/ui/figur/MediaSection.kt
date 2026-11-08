package io.github.katzfuss11.tanzlexikon.ui.figur

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import io.github.katzfuss11.tanzlexikon.R
import io.github.katzfuss11.tanzlexikon.data.MediaEntity
import io.github.katzfuss11.tanzlexikon.data.MediaKind
import io.github.katzfuss11.tanzlexikon.media.MediaChecker
import io.github.katzfuss11.tanzlexikon.media.youtubeThumbnailUrl
import io.github.katzfuss11.tanzlexikon.media.youtubeVideoId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Die Medien eines Bereichs als zweispaltiges Raster.
 * Im Bearbeitungsmodus haben die Kacheln ein ✕ und fehlende Dateien „Neu verknüpfen“.
 */
@Composable
fun MediaSection(
    media: List<MediaEntity>,
    editing: Boolean,
    onRemove: (MediaEntity) -> Unit = {},
    onRelink: (MediaEntity) -> Unit = {},
) {
    if (media.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        media.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { m ->
                    Box(Modifier.weight(1f)) { MediaTile(m, editing, onRemove, onRelink) }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MediaTile(
    m: MediaEntity,
    editing: Boolean,
    onRemove: (MediaEntity) -> Unit,
    onRelink: (MediaEntity) -> Unit,
) {
    val context = LocalContext.current
    // Ob die Datei noch da ist, wird abseits der Oberfläche geprüft.
    val available by produceState<Boolean?>(initialValue = null, m.uri, m.kind) {
        value = withContext(Dispatchers.IO) { MediaChecker(context.contentResolver).isAvailable(m) }
    }
    val shape = MaterialTheme.shapes.small
    var viewing by remember { mutableStateOf(false) }
    if (viewing) MediaViewer(m, onClose = { viewing = false })
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(enabled = available == true) {
                    // Eigene Videos und Bilder zeigt die App selbst; nur YouTube geht nach außen.
                    if (m.kind == MediaKind.YOUTUBE) openYouTube(context, m) else viewing = true
                },
        ) {
            if (available == false) {
                Column(
                    Modifier.fillMaxSize().padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Default.BrokenImage, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text(
                        stringResource(R.string.media_missing),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error,
                    )
                    if (editing) {
                        TextButton(onClick = { onRelink(m) }) { Text(stringResource(R.string.relink)) }
                    }
                }
            } else if (available == true) {
                val model: Any? = when (m.kind) {
                    MediaKind.YOUTUBE -> youtubeVideoId(m.uri)?.let { youtubeThumbnailUrl(it) }
                    else -> Uri.parse(m.uri)
                }
                SubcomposeAsyncImage(
                    model = model,
                    contentDescription = m.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    error = { Placeholder(m.kind) },
                    loading = { Placeholder(m.kind) },
                )
                if (m.kind != MediaKind.IMAGE) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.55f),
                        modifier = Modifier.align(Alignment.Center).size(40.dp),
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.play),
                            tint = Color.White,
                            modifier = Modifier.padding(6.dp),
                        )
                    }
                }
            }
            if (editing) {
                IconButton(
                    onClick = { onRemove(m) },
                    modifier = Modifier.align(Alignment.TopEnd).size(36.dp),
                ) {
                    Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.55f)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.remove_media),
                            tint = Color.White,
                            modifier = Modifier.padding(4.dp),
                        )
                    }
                }
            }
        }
        val caption = if (m.kind == MediaKind.YOUTUBE) "YouTube" else m.displayName
        if (!caption.isNullOrBlank()) {
            Text(
                caption,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun Placeholder(kind: MediaKind) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            if (kind == MediaKind.YOUTUBE) "YouTube" else "",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Öffnet einen YouTube-Link in der YouTube-App oder im Browser. */
private fun openYouTube(context: Context, m: MediaEntity) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(m.uri)))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.no_app_to_open, Toast.LENGTH_LONG).show()
    }
}
