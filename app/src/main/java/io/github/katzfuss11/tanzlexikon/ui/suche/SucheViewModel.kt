package io.github.katzfuss11.tanzlexikon.ui.suche

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.katzfuss11.tanzlexikon.lexikon.LexikonRepository
import io.github.katzfuss11.tanzlexikon.lexikon.SearchHit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SucheViewModel(private val repo: LexikonRepository) : ViewModel() {
    val query = MutableStateFlow("")

    /** Treffer erscheinen beim Tippen, 200 ms nach dem letzten Anschlag. */
    val hits: StateFlow<List<SearchHit>> = query
        .debounce(200)
        .mapLatest { repo.search(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
