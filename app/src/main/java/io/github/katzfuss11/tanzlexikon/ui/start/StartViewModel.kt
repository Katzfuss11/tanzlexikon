package io.github.katzfuss11.tanzlexikon.ui.start

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.katzfuss11.tanzlexikon.R
import io.github.katzfuss11.tanzlexikon.data.MediaEntity
import io.github.katzfuss11.tanzlexikon.lexikon.FigureSummaryRef
import io.github.katzfuss11.tanzlexikon.lexikon.CategoryWithDances
import io.github.katzfuss11.tanzlexikon.lexikon.DeleteResult
import io.github.katzfuss11.tanzlexikon.lexikon.LexikonRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StartState(
    val categories: List<CategoryWithDances>,
    val missingMediaCount: Int = 0,
    @StringRes val message: Int? = null,
    /** Figuren mit mindestens einem fehlenden Medium, für den Hinweis-Dialog. */
    val missingFigures: List<FigureSummaryRef> = emptyList(),
)

private data class Missing(val count: Int = 0, val figures: List<FigureSummaryRef> = emptyList())

/**
 * @param isAvailable prüft, ob die Datei eines Mediums noch da ist. null = nicht prüfen.
 */
class StartViewModel(
    private val repo: LexikonRepository,
    private val isAvailable: ((MediaEntity) -> Boolean)? = null,
) : ViewModel() {
    private val message = MutableStateFlow<Int?>(null)
    private val missing = MutableStateFlow(Missing())

    val state: StateFlow<StartState> = combine(repo.observeStart(), message, missing) { cats, msg, miss ->
        StartState(categories = cats, missingMediaCount = miss.count, message = msg, missingFigures = miss.figures)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, StartState(emptyList()))

    init {
        refreshMissingMedia()
    }

    /** Prüft im Hintergrund alle Medien. Wird beim Start und bei der Rückkehr auf die Startseite aufgerufen. */
    fun refreshMissingMedia() {
        val check = isAvailable ?: return
        viewModelScope.launch {
            missing.value = withContext(Dispatchers.IO) {
                val gone = repo.allActiveMedia().filterNot { (m, _) -> check(m) }
                Missing(gone.size, gone.map { it.second }.distinctBy { it.figureId })
            }
        }
    }

    fun messageShown() = message.update { null }

    fun addCategory(name: String) = launch { repo.addCategory(name) }
    fun addDance(categoryId: Long, name: String) = launch { repo.addDance(categoryId, name) }
    fun renameCategory(id: Long, name: String) = launch { repo.renameCategory(id, name) }
    fun renameDance(id: Long, name: String) = launch { repo.renameDance(id, name) }
    fun moveDance(id: Long, categoryId: Long) = launch { repo.moveDance(id, categoryId) }

    fun deleteDance(id: Long) = launch {
        if (repo.deleteDance(id) == DeleteResult.NOT_EMPTY) message.value = R.string.delete_dance_not_empty
    }

    fun deleteCategory(id: Long) = launch {
        if (repo.deleteCategory(id) == DeleteResult.NOT_EMPTY) message.value = R.string.delete_category_not_empty
    }

    /**
     * Übernimmt die Reihenfolge aus dem Ziehmodus: Kategorien in der Reihenfolge der Liste,
     * darin die Tänze. Ein Tanz, der unter eine andere Kategorie gezogen wurde, wechselt dorthin.
     */
    fun applyOrder(order: List<Pair<Long, List<Long>>>) = launch { repo.arrange(order) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
