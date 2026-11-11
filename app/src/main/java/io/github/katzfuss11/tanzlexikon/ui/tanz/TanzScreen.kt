package io.github.katzfuss11.tanzlexikon.ui.tanz

import android.content.res.Resources
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.katzfuss11.tanzlexikon.R
import io.github.katzfuss11.tanzlexikon.lexikon.FigureSummary
import io.github.katzfuss11.tanzlexikon.ui.BackButton
import io.github.katzfuss11.tanzlexikon.ui.NameDialog
import io.github.katzfuss11.tanzlexikon.ui.appViewModel
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** Kurzbeschreibung der Medien einer Figur, z. B. „1 Video, 1 Link“ oder „nur Text“. */
fun mediaSummaryText(res: Resources, s: FigureSummary): String {
    val parts = buildList {
        if (s.videoCount > 0) add(res.getQuantityString(R.plurals.media_videos, s.videoCount, s.videoCount))
        if (s.imageCount > 0) add(res.getQuantityString(R.plurals.media_images, s.imageCount, s.imageCount))
        if (s.linkCount > 0) add(res.getQuantityString(R.plurals.media_links, s.linkCount, s.linkCount))
    }
    return if (parts.isEmpty()) res.getString(R.string.media_text_only) else parts.joinToString(", ")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TanzScreen(
    danceId: Long,
    onBack: () -> Unit,
    onOpenFigure: (Long) -> Unit,
    onFigureCreated: (Long) -> Unit = onOpenFigure,
    vm: TanzViewModel = appViewModel(key = "tanz$danceId") { TanzViewModel(it.repository, danceId) },
) {
    val name by vm.danceName.collectAsStateWithLifecycle()
    val figures by vm.figures.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var adding by remember { mutableStateOf(false) }
    val movedText = stringResource(R.string.moved_to_trash)
    val undoText = stringResource(R.string.undo)

    fun trash(id: Long) {
        vm.trash(id)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(movedText, actionLabel = undoText, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) vm.restore(id)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(name) }, navigationIcon = { BackButton(onBack) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { adding = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_figure))
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val list = figures
            when {
                list == null -> Unit
                list.isEmpty() -> Text(
                    stringResource(R.string.no_figures_yet),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
                else -> FigureList(list, onOpenFigure, onTrash = ::trash, onReorder = vm::reorder)
            }
        }
    }

    if (adding) {
        NameDialog(
            title = stringResource(R.string.new_figure),
            onDismiss = { adding = false },
            onConfirm = { figureName ->
                adding = false
                vm.addFigure(figureName, onFigureCreated)
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FigureList(
    figures: List<FigureSummary>,
    onOpen: (Long) -> Unit,
    onTrash: (Long) -> Unit,
    onReorder: (List<Long>) -> Unit,
) {
    // Lokale Kopie, damit das Ziehen flüssig ist; gespeichert wird beim Loslassen.
    val items = remember { mutableStateListOf<FigureSummary>() }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(figures) {
        if (!dragging) {
            items.clear()
            items.addAll(figures)
        }
    }
    val res = LocalResources.current
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        items.add(to.index, items.removeAt(from.index))
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        items(items, key = { it.id }) { figure ->
            ReorderableItem(reorderState, key = figure.id) { isDragging ->
                var menu by remember { mutableStateOf(false) }
                Surface(tonalElevation = if (isDragging) 6.dp else 0.dp) {
                        Box {
                            ListItem(
                                headlineContent = { Text(figure.name) },
                                supportingContent = { Text(mediaSummaryText(res, figure)) },
                                trailingContent = {
                                    Icon(
                                        Icons.Default.DragHandle,
                                        contentDescription = stringResource(R.string.drag_handle),
                                        modifier = Modifier.draggableHandle(
                                            onDragStarted = { dragging = true },
                                            onDragStopped = {
                                                dragging = false
                                                onReorder(items.map { it.id })
                                            },
                                        ),
                                    )
                                },
                                modifier = Modifier.combinedClickable(
                                    onClick = { onOpen(figure.id) },
                                    onLongClick = { menu = true },
                                ),
                            )
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.to_trash)) }, onClick = {
                                    menu = false
                                    onTrash(figure.id)
                                })
                            }
                        }
                }
                HorizontalDivider(Modifier.padding(start = 16.dp))
            }
        }
        item { Spacer(Modifier.size(88.dp)) }
    }
}
