package io.github.katzfuss11.tanzlexikon.ui.start

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.katzfuss11.tanzlexikon.R
import io.github.katzfuss11.tanzlexikon.lexikon.CategoryWithDances
import io.github.katzfuss11.tanzlexikon.lexikon.DanceSummary
import io.github.katzfuss11.tanzlexikon.ui.NameDialog
import io.github.katzfuss11.tanzlexikon.ui.appViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** Welcher Dialog gerade offen ist. */
private sealed interface StartDialog {
    data object ChooseNew : StartDialog
    data object NewCategory : StartDialog
    data object NewDance : StartDialog
    data class RenameDance(val dance: DanceSummary) : StartDialog
    data class RenameCategory(val category: CategoryWithDances) : StartDialog
    data class MoveDance(val dance: DanceSummary, val fromCategoryId: Long) : StartDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreen(
    onOpenDance: (Long) -> Unit,
    onSearch: () -> Unit,
    onTrash: () -> Unit,
    onSettings: () -> Unit,
    onOpenFigure: (Long) -> Unit,
    vm: StartViewModel = appViewModel { StartViewModel(it.repository, it.mediaChecker::isAvailable) },
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showMissing by remember { mutableStateOf(false) }
    // Bei jeder Rückkehr auf die Startseite neu prüfen, ob Videos oder Bilder fehlen.
    LifecycleResumeEffect(Unit) {
        vm.refreshMissingMedia()
        onPauseOrDispose {}
    }
    val snackbar = remember { SnackbarHostState() }
    var dialog by remember { mutableStateOf<StartDialog?>(null) }
    var ordering by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    val messageText = state.message?.let { stringResource(it) }
    LaunchedEffect(messageText) {
        if (messageText != null) {
            snackbar.showSnackbar(messageText)
            vm.messageShown()
        }
    }

    // Im Ziehmodus wird eine lokale Kopie verschoben und erst bei "Fertig" gespeichert.
    val orderEntries = remember { mutableStateListOf<OrderEntry>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (ordering) R.string.menu_order else R.string.start_title)) },
                actions = {
                    if (ordering) {
                        TextButton(onClick = {
                            vm.applyOrder(orderEntries.toOrder())
                            ordering = false
                        }) { Text(stringResource(R.string.order_done)) }
                    } else {
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more))
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.menu_order)) }, onClick = {
                                    menuOpen = false
                                    orderEntries.clear()
                                    orderEntries.addAll(state.categories.toEntries())
                                    ordering = true
                                })
                                DropdownMenuItem(text = { Text(stringResource(R.string.menu_trash)) }, onClick = {
                                    menuOpen = false
                                    onTrash()
                                })
                                DropdownMenuItem(text = { Text(stringResource(R.string.menu_settings)) }, onClick = {
                                    menuOpen = false
                                    onSettings()
                                })
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (!ordering) {
                FloatingActionButton(onClick = { dialog = StartDialog.ChooseNew }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (ordering) {
                OrderList(orderEntries)
            } else {
                StartList(
                    categories = state.categories,
                    missingMediaCount = state.missingMediaCount,
                    onShowMissing = { showMissing = true },
                    onSearch = onSearch,
                    onOpenDance = onOpenDance,
                    onRenameDance = { dialog = StartDialog.RenameDance(it) },
                    onMoveDance = { d, c -> dialog = StartDialog.MoveDance(d, c) },
                    onDeleteDance = { vm.deleteDance(it.id) },
                    onRenameCategory = { dialog = StartDialog.RenameCategory(it) },
                    onDeleteCategory = { vm.deleteCategory(it.id) },
                )
            }
        }
    }

    if (showMissing) {
        AlertDialog(
            onDismissRequest = { showMissing = false },
            title = { Text(stringResource(R.string.missing_media_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.missing_media_text), style = MaterialTheme.typography.bodyMedium)
                    state.missingFigures.forEach { f ->
                        ListItem(
                            headlineContent = { Text(f.figureName) },
                            supportingContent = { Text(f.danceName) },
                            modifier = Modifier.clickable {
                                showMissing = false
                                onOpenFigure(f.figureId)
                            },
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showMissing = false }) { Text(stringResource(R.string.ok)) } },
        )
    }

    when (val d = dialog) {
        null -> Unit
        StartDialog.ChooseNew -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(stringResource(R.string.add)) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.new_dance)) },
                        modifier = Modifier.clickable { dialog = StartDialog.NewDance },
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.new_category)) },
                        modifier = Modifier.clickable { dialog = StartDialog.NewCategory },
                    )
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.cancel)) } },
        )
        StartDialog.NewCategory -> NameDialog(
            title = stringResource(R.string.new_category),
            onDismiss = { dialog = null },
            onConfirm = { vm.addCategory(it); dialog = null },
        )
        StartDialog.NewDance -> {
            var categoryId by rememberSaveable { mutableStateOf(state.categories.firstOrNull()?.id) }
            NameDialog(
                title = stringResource(R.string.new_dance),
                extra = {
                    CategoryChooser(state.categories, categoryId) { categoryId = it }
                },
                onDismiss = { dialog = null },
                onConfirm = { name ->
                    categoryId?.let { vm.addDance(it, name) }
                    dialog = null
                },
            )
        }
        is StartDialog.RenameDance -> NameDialog(
            title = stringResource(R.string.rename_dance),
            initial = d.dance.name,
            onDismiss = { dialog = null },
            onConfirm = { vm.renameDance(d.dance.id, it); dialog = null },
        )
        is StartDialog.RenameCategory -> NameDialog(
            title = stringResource(R.string.rename_category),
            initial = d.category.name,
            onDismiss = { dialog = null },
            onConfirm = { vm.renameCategory(d.category.id, it); dialog = null },
        )
        is StartDialog.MoveDance -> {
            var target by rememberSaveable { mutableStateOf<Long?>(d.fromCategoryId) }
            AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text(stringResource(R.string.move_to_category)) },
                text = { CategoryChooser(state.categories, target) { target = it } },
                confirmButton = {
                    TextButton(onClick = {
                        target?.takeIf { it != d.fromCategoryId }?.let { vm.moveDance(d.dance.id, it) }
                        dialog = null
                    }) { Text(stringResource(R.string.ok)) }
                },
                dismissButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.cancel)) } },
            )
        }
    }
}

/** Auswahl der Kategorie als aufklappbare Liste, damit der Dialog auch bei vielen Kategorien schlank bleibt. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChooser(categories: List<CategoryWithDances>, selected: Long?, onSelect: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.padding(top = 12.dp),
    ) {
        OutlinedTextField(
            value = categories.firstOrNull { it.id == selected }?.name.orEmpty(),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(R.string.category)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { c ->
                DropdownMenuItem(
                    text = { Text(c.name) },
                    onClick = {
                        onSelect(c.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StartList(
    categories: List<CategoryWithDances>,
    missingMediaCount: Int,
    onShowMissing: () -> Unit,
    onSearch: () -> Unit,
    onOpenDance: (Long) -> Unit,
    onRenameDance: (DanceSummary) -> Unit,
    onMoveDance: (DanceSummary, Long) -> Unit,
    onDeleteDance: (DanceSummary) -> Unit,
    onRenameCategory: (CategoryWithDances) -> Unit,
    onDeleteCategory: (CategoryWithDances) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        item(key = "search") { SearchField(onSearch) }
        if (missingMediaCount > 0) {
            item(key = "missing") {
                Surface(
                    onClick = onShowMissing,
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            pluralStringResource(R.plurals.missing_media_hint, missingMediaCount, missingMediaCount),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        }
        categories.forEach { category ->
            item(key = "c${category.id}") {
                var menu by remember { mutableStateOf(false) }
                Box {
                    Text(
                        category.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(onClick = {}, onLongClick = { menu = true })
                            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 6.dp),
                    )
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.rename)) }, onClick = {
                            menu = false; onRenameCategory(category)
                        })
                        DropdownMenuItem(text = { Text(stringResource(R.string.delete)) }, onClick = {
                            menu = false; onDeleteCategory(category)
                        })
                    }
                }
            }
            if (category.dances.isEmpty()) {
                item(key = "e${category.id}") {
                    Text(
                        stringResource(R.string.no_dances_yet),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            items(category.dances, key = { "d${it.id}" }) { dance ->
                var menu by remember { mutableStateOf(false) }
                Box {
                    ListItem(
                        headlineContent = { Text(dance.name) },
                        trailingContent = {
                            Text(
                                pluralStringResource(R.plurals.figure_count, dance.figureCount, dance.figureCount),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                        modifier = Modifier.combinedClickable(
                            onClick = { onOpenDance(dance.id) },
                            onLongClick = { menu = true },
                        ),
                    )
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.rename)) }, onClick = {
                            menu = false; onRenameDance(dance)
                        })
                        DropdownMenuItem(text = { Text(stringResource(R.string.move_to_category)) }, onClick = {
                            menu = false; onMoveDance(dance, category.id)
                        })
                        DropdownMenuItem(text = { Text(stringResource(R.string.delete)) }, onClick = {
                            menu = false; onDeleteDance(dance)
                        })
                    }
                }
                HorizontalDivider(Modifier.padding(start = 16.dp))
            }
        }
        item(key = "bottom") { Spacer(Modifier.size(88.dp)) }
    }
}

@Composable
private fun SearchField(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.search_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---- Ziehmodus ----

/** Ein Eintrag der Ziehliste: eine Kategorie-Überschrift oder ein Tanz. */
sealed interface OrderEntry {
    val key: String

    data class Header(val id: Long, val name: String) : OrderEntry {
        override val key get() = "c$id"
    }

    data class Dance(val id: Long, val name: String) : OrderEntry {
        override val key get() = "d$id"
    }
}

fun List<CategoryWithDances>.toEntries(): List<OrderEntry> = flatMap { c ->
    listOf(OrderEntry.Header(c.id, c.name)) + c.dances.map { OrderEntry.Dance(it.id, it.name) }
}

/** Jeder Tanz gehört zur Überschrift über ihm. */
fun List<OrderEntry>.toOrder(): List<Pair<Long, List<Long>>> {
    val result = mutableListOf<Pair<Long, MutableList<Long>>>()
    forEach { e ->
        when (e) {
            is OrderEntry.Header -> result += e.id to mutableListOf()
            is OrderEntry.Dance -> result.lastOrNull()?.second?.add(e.id)
        }
    }
    return result
}

/** Verschiebt eine Kategorie mitsamt ihren Tänzen um einen Block nach oben (-1) oder unten (+1). */
private fun MutableList<OrderEntry>.moveCategoryBlock(headerIndex: Int, by: Int) {
    fun blockEnd(start: Int): Int {
        var i = start + 1
        while (i < size && this[i] !is OrderEntry.Header) i++
        return i
    }
    val headers = indices.filter { this[it] is OrderEntry.Header }
    val pos = headers.indexOf(headerIndex)
    val otherPos = pos + by
    if (pos < 0 || otherPos !in headers.indices) return
    val (first, second) = if (by < 0) headers[otherPos] to headerIndex else headerIndex to headers[otherPos]
    val firstBlock = subList(first, blockEnd(first)).toList()
    val secondBlock = subList(second, blockEnd(second)).toList()
    val end = blockEnd(second)
    subList(first, end).clear()
    addAll(first, secondBlock + firstBlock)
}

@Composable
private fun OrderList(entries: MutableList<OrderEntry>) {
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        // Nichts vor die erste Überschrift schieben, sonst hätte ein Tanz keine Kategorie.
        if (to.index == 0) return@rememberReorderableLazyListState
        entries.add(to.index, entries.removeAt(from.index))
    }
    Column(Modifier.fillMaxSize()) {
        Text(
            stringResource(R.string.order_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(entries, key = { it.key }) { entry ->
                ReorderableItem(reorderState, key = entry.key) { dragging ->
                    when (entry) {
                        is OrderEntry.Header -> Row(
                            Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                entry.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { entries.moveCategoryBlock(entries.indexOf(entry), -1) }) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = stringResource(R.string.move_up))
                            }
                            IconButton(onClick = { entries.moveCategoryBlock(entries.indexOf(entry), +1) }) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = stringResource(R.string.move_down))
                            }
                        }
                        is OrderEntry.Dance -> Surface(tonalElevation = if (dragging) 6.dp else 0.dp) {
                            ListItem(
                                headlineContent = { Text(entry.name) },
                                trailingContent = {
                                    Icon(
                                        Icons.Default.DragHandle,
                                        contentDescription = stringResource(R.string.drag_handle),
                                        modifier = Modifier.draggableHandle(),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
