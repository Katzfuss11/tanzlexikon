package io.github.katzfuss11.tanzlexikon.ui.suche

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.katzfuss11.tanzlexikon.R
import io.github.katzfuss11.tanzlexikon.ui.BackButton
import io.github.katzfuss11.tanzlexikon.ui.appViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SucheScreen(
    onBack: () -> Unit,
    onOpenFigure: (Long) -> Unit,
    onOpenDance: (Long) -> Unit,
    vm: SucheViewModel = appViewModel { SucheViewModel(it.repository) },
) {
    val query by vm.query.collectAsStateWithLifecycle()
    val hits by vm.hits.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton(onBack) },
                title = {
                    TextField(
                        value = query,
                        onValueChange = { vm.query.value = it },
                        placeholder = { Text(stringResource(R.string.search_hint)) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                },
            )
        },
    ) { padding ->
        if (query.isNotBlank() && hits.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.TopCenter) {
                Text(stringResource(R.string.search_no_hits), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(hits, key = { "${it.figureId}-${it.danceId}" }) { hit ->
                val figureId = hit.figureId
                ListItem(
                    headlineContent = { Text(hit.title) },
                    supportingContent = {
                        Text(if (figureId == null) stringResource(R.string.search_dance_hit, hit.subtitle) else hit.subtitle)
                    },
                    modifier = Modifier.clickable {
                        if (figureId != null) onOpenFigure(figureId) else onOpenDance(hit.danceId)
                    },
                )
                HorizontalDivider(Modifier.padding(start = 16.dp))
            }
        }
    }
}
