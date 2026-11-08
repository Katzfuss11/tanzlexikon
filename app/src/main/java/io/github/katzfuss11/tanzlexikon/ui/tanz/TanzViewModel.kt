package io.github.katzfuss11.tanzlexikon.ui.tanz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.katzfuss11.tanzlexikon.lexikon.FigureSummary
import io.github.katzfuss11.tanzlexikon.lexikon.LexikonRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TanzViewModel(private val repo: LexikonRepository, private val danceId: Long) : ViewModel() {
    val danceName: StateFlow<String> = repo.observeStart()
        .map { cats -> cats.flatMap { it.dances }.firstOrNull { it.id == danceId }?.name.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    /** null, solange noch nichts geladen ist. */
    val figures: StateFlow<List<FigureSummary>?> = repo.observeFigures(danceId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun addFigure(name: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch { onCreated(repo.addFigure(danceId, name)) }
    }

    fun reorder(orderedIds: List<Long>) {
        viewModelScope.launch { repo.reorderFigures(danceId, orderedIds) }
    }

    fun trash(id: Long) {
        viewModelScope.launch { repo.trashFigure(id, System.currentTimeMillis()) }
    }

    fun restore(id: Long) {
        viewModelScope.launch { repo.restoreFigure(id) }
    }
}
