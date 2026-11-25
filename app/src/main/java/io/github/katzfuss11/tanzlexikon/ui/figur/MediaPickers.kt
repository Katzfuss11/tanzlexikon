package io.github.katzfuss11.tanzlexikon.ui.figur

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.katzfuss11.tanzlexikon.R
import io.github.katzfuss11.tanzlexikon.data.MediaKind
import io.github.katzfuss11.tanzlexikon.media.GalleryWriter
import io.github.katzfuss11.tanzlexikon.media.youtubeVideoId

/** Liest Name, Typ und Größe einer Datei aus der Galerie. */
fun readNewMedia(context: Context, uri: Uri): NewMedia {
    val resolver = context.contentResolver
    val mime = runCatching { resolver.getType(uri) }.getOrNull()
    var name: String? = null
    var size: Long? = null
    runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val n = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val s = c.getColumnIndex(OpenableColumns.SIZE)
                if (n >= 0 && !c.isNull(n)) name = c.getString(n)
                if (s >= 0 && !c.isNull(s)) size = c.getLong(s)
            }
        }
    }
    val kind = if (mime?.startsWith("image/") == true) MediaKind.IMAGE else MediaKind.VIDEO
    return NewMedia(kind, uri.toString(), name, mime, size)
}

/**
 * Damit die App eine im Photo Picker gewählte Datei auch nach einem Neustart noch lesen darf.
 * Eine Galerie-Berechtigung braucht die App dafür nicht.
 */
private fun keepAccess(context: Context, uri: Uri) {
    try {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (_: Exception) {
        // Nicht jede Quelle erlaubt das. Dann bleibt der Zugriff bis zum Neustart der App.
    }
}

/** Die drei Wege, ein Medium hinzuzufügen. */
class MediaAdder(
    val recordVideo: () -> Unit,
    val pickFromGallery: () -> Unit,
    val pickOne: () -> Unit,
)

/**
 * @param onPicked bekommt die neuen Medien (bei [MediaAdder.pickOne] genau eines).
 */
@Composable
fun rememberMediaAdder(onPicked: (List<NewMedia>) -> Unit): MediaAdder {
    val context = LocalContext.current
    val callback by rememberUpdatedState(onPicked)
    val noCamera = stringResource(R.string.no_camera)

    val gallery = remember { GalleryWriter(context.contentResolver) }
    // Der Eintrag, in den die Kamera gerade schreibt. Gespeichert, falls Android die App
    // während der Aufnahme beendet.
    var recording by rememberSaveable { mutableStateOf<String?>(null) }

    val capture = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val own = recording?.let(Uri::parse)
        recording = null
        val returned = result.data?.data
        when {
            result.resultCode != Activity.RESULT_OK -> own?.let(gallery::discard)
            own != null && gallery.hasContent(own) ->
                callback(listOf(readNewMedia(context, own).copy(kind = MediaKind.VIDEO)))
            returned != null -> {
                // Manche Kamera-Apps übergehen EXTRA_OUTPUT und speichern selbst. Dann bleibt
                // das Video nur lesbar, solange Android den Zugriff darauf nicht zurücknimmt.
                own?.let(gallery::discard)
                keepAccess(context, returned)
                callback(listOf(readNewMedia(context, returned).copy(kind = MediaKind.VIDEO)))
            }
            else -> own?.let(gallery::discard)
        }
    }
    fun startCamera() {
        // Die Kamera schreibt in einen Eintrag, der der App gehört. So kann sie das Video
        // ohne Galerie-Berechtigung abspielen, auch nach einem Neustart.
        val target = try {
            gallery.newRecording()
        } catch (_: Exception) {
            Toast.makeText(context, noCamera, Toast.LENGTH_LONG).show()
            return
        }
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            .putExtra(MediaStore.EXTRA_OUTPUT, target)
            .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            recording = target.toString()
            capture.launch(intent)
        } catch (_: ActivityNotFoundException) {
            recording = null
            gallery.discard(target)
            Toast.makeText(context, noCamera, Toast.LENGTH_LONG).show()
        }
    }

    val pickMany = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { keepAccess(context, it) }
            callback(uris.map { readNewMedia(context, it) })
        }
    }
    val pickSingle = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            keepAccess(context, uri)
            callback(listOf(readNewMedia(context, uri)))
        }
    }
    val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)

    return remember {
        MediaAdder(
            recordVideo = { startCamera() },
            pickFromGallery = { pickMany.launch(request) },
            pickOne = { pickSingle.launch(request) },
        )
    }
}

/** Dialog für einen YouTube-Link. Liest die Zwischenablage nicht von selbst aus. */
@Composable
fun YouTubeDialog(onDismiss: () -> Unit, onConfirm: (NewMedia) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var invalid by rememberSaveable { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.youtube_link)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it; invalid = false },
                placeholder = { Text(stringResource(R.string.youtube_hint)) },
                isError = invalid,
                supportingText = { if (invalid) Text(stringResource(R.string.not_a_youtube_link)) },
                singleLine = true,
                modifier = Modifier.focusRequester(focus),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (youtubeVideoId(text) == null) invalid = true
                else onConfirm(NewMedia(MediaKind.YOUTUBE, text.trim()))
            }, enabled = text.isNotBlank()) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
