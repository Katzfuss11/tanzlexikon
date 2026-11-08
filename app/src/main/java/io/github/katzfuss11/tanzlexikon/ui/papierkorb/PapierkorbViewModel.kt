package io.github.katzfuss11.tanzlexikon.ui.papierkorb

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.katzfuss11.tanzlexikon.lexikon.LexikonRepository
import io.github.katzfuss11.tanzlexikon.lexikon.TRASH_RETENTION_MS
import io.github.katzfuss11.tanzlexikon.lexikon.TrashedFigure
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val DAY_MS = 24L * 60 * 60 * 1000

/** Wie viele Tage eine Figur noch im Papierkorb bleibt (nie weniger als 0). */
fun daysLeft(now: Long, trashedAt: Long): Int =
    ((TRASH_RETENTION_MS / DAY_MS) - (now - trashedAt) / DAY_MS).coerceAtLeast(0).toInt()

class PapierkorbViewModel(private val repo: LexikonRepository) : ViewModel() {
    val trash: StateFlow<List<TrashedFigure>?> = repo.observeTrash()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun restore(id: Long) {
        viewModelScope.launch { repo.restoreFigure(id) }
    }

    fun deleteForever(id: Long) {
        viewModelScope.launch { repo.deleteFigurePermanently(id) }
    }
}
