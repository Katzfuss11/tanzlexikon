package io.github.katzfuss11.tanzlexikon.ui.figur

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
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

/** Damit die App einen Verweis auf die Galerie auch nach einem Neustart noch lesen darf. */
private fun keepAccess(context: Context, uri: Uri) {
    try {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (_: Exception) {
        // Nicht jede Quelle erlaubt das (z. B. die Kamera). Dann gilt die Galerie-Berechtigung.
    }
}

private fun galleryPermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= 34 ->
        arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
    Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_IMAGES)
    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
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

    val capture = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && uri != null) {
            keepAccess(context, uri)
            callback(listOf(readNewMedia(context, uri).copy(kind = MediaKind.VIDEO)))
        }
    }
    fun startCamera() {
        // Ohne EXTRA_OUTPUT: die Kamera-App legt das Video selbst in der Galerie ab.
        try {
            capture.launch(Intent(MediaStore.ACTION_VIDEO_CAPTURE))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, noCamera, Toast.LENGTH_LONG).show()
        }
    }
    // Erst die Leseberechtigung erfragen, damit das Video auch später noch abspielbar ist.
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { startCamera() }

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
            recordVideo = { permission.launch(galleryPermissions()) },
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
