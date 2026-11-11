package io.github.katzfuss11.tanzlexikon.ui.empfang

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.katzfuss11.tanzlexikon.data.MediaKind
import io.github.katzfuss11.tanzlexikon.lexikon.CategoryWithDances
import io.github.katzfuss11.tanzlexikon.lexikon.LexikonRepository
import io.github.katzfuss11.tanzlexikon.lexikon.MISSING_URI
import io.github.katzfuss11.tanzlexikon.media.GalleryWriter
import io.github.katzfuss11.tanzlexikon.share.ImportTarget
import io.github.katzfuss11.tanzlexikon.share.proposeTarget
import io.github.katzfuss11.tanzlexikon.transfer.ArchFigure
import io.github.katzfuss11.tanzlexikon.transfer.ArchiveError
import io.github.katzfuss11.tanzlexikon.transfer.ArchiveException
import io.github.katzfuss11.tanzlexikon.transfer.ArchiveKind
import io.github.katzfuss11.tanzlexikon.transfer.ArchiveReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

data class EmpfangState(
    val loading: Boolean = true,
    val figure: ArchFigure? = null,
    val target: ImportTarget? = null,
    val figureName: String = "",
    /** Im Zieltanz gibt es schon eine Figur mit diesem Namen. */
    val duplicateName: Boolean = false,
    val error: ArchiveError? = null,
    /** Die Datei ist ein Tanzlexikon-Archiv, aber eine Sicherung statt einer einzelnen Figur. */
    val notAFigure: Boolean = false,
    val adding: Boolean = false,
    val addedFigureId: Long? = null,
    val categories: List<CategoryWithDances> = emptyList(),
)

/**
 * Empfang einer geteilten Figur. Liest zuerst nur die Beschreibung für die Vorschau;
 * die Videos und Bilder werden erst beim Hinzufügen aus der Datei geholt.
 */
class EmpfangViewModel(
    private val repo: LexikonRepository,
    private val galleryWriter: GalleryWriter,
    /** Hier läuft das Hinzufügen, damit es auch fertig wird, wenn der Bildschirm schon zu ist. */
    private val workScope: CoroutineScope? = null,
    private val source: () -> InputStream,
) : ViewModel() {
    private val _state = MutableStateFlow(EmpfangState())
    val state: StateFlow<EmpfangState> = _state.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val categories = repo.observeStart().first()
        val manifest = try {
            withContext(Dispatchers.IO) { source().use { ArchiveReader().read(it) { _, _ -> } } }
        } catch (e: ArchiveException) {
            _state.value = EmpfangState(loading = false, error = e.reason, categories = categories)
            return
        } catch (e: Exception) {
            _state.value = EmpfangState(loading = false, error = ArchiveError.CORRUPT, categories = categories)
            return
        }
        val category = manifest.categories.singleOrNull()
        val dance = category?.dances?.singleOrNull()
        val figure = dance?.figures?.singleOrNull()
        if (manifest.kind != ArchiveKind.FIGURE || figure == null) {
            _state.value = EmpfangState(loading = false, notAFigure = true, categories = categories)
            return
        }
        val target = proposeTarget(categories, category.name, dance.name)
        // Erst prüfen, dann zeigen: so erscheint der Hinweis auf einen doppelten Namen gleich mit.
        _state.value = EmpfangState(
            loading = false, figure = figure, target = target, figureName = figure.name, categories = categories,
            duplicateName = isDuplicate(target, figure.name),
        )
    }

    fun chooseExisting(danceId: Long) {
        val s = _state.value
        val category = s.categories.firstOrNull { c -> c.dances.any { it.id == danceId } } ?: return
        val dance = category.dances.first { it.id == danceId }
        _state.update { it.copy(target = ImportTarget.Existing(dance.id, dance.name, category.name)) }
        viewModelScope.launch { refreshDuplicate() }
    }

    fun chooseNew(categoryName: String, danceName: String) {
        // Gibt es den Tanz doch schon, wird er genommen (gleiche Regel wie beim Vorschlag).
        _state.update { it.copy(target = proposeTarget(it.categories.filter { c -> c.name.equals(categoryName.trim(), true) }, categoryName, danceName)) }
        viewModelScope.launch { refreshDuplicate() }
    }

    fun rename(name: String) {
        _state.update { it.copy(figureName = name) }
        viewModelScope.launch { refreshDuplicate() }
    }

    private suspend fun refreshDuplicate() {
        val s = _state.value
        val duplicate = isDuplicate(s.target, s.figureName)
        _state.update { it.copy(duplicateName = duplicate) }
    }

    private suspend fun isDuplicate(target: ImportTarget?, name: String): Boolean {
        val existing = target as? ImportTarget.Existing ?: return false
        return repo.observeFigures(existing.danceId).first().any { it.name.trim().equals(name.trim(), ignoreCase = true) }
    }

    /** Legt die Figur an (Tanz bei Bedarf neu), holt die Medien in die Galerie. Überschreibt nie. */
    fun add() {
        val s = _state.value
        val figure = s.figure ?: return
        val target = s.target ?: return
        if (s.adding || s.addedFigureId != null || s.figureName.isBlank()) return
        _state.update { it.copy(adding = true) }
        (workScope ?: viewModelScope).launch {
            try {
                val id = withContext(Dispatchers.IO) {
                    val byPath = figure.media.filter { it.file != null }.associateBy { it.file!! }
                    val uris = mutableMapOf<String, String>()
                    source().use { input ->
                        ArchiveReader().read(input) { path, data ->
                            val m = byPath[path] ?: return@read
                            uris[path] = galleryWriter.save(
                                displayName = m.displayName ?: "tanzlexikon-${path.substringAfterLast('/')}",
                                mimeType = m.mimeType ?: if (m.kind == MediaKind.IMAGE) "image/jpeg" else "video/mp4",
                                size = m.sizeBytes ?: -1L,
                                data = data,
                            ).toString()
                        }
                    }
                    val danceId = when (target) {
                        is ImportTarget.Existing -> target.danceId
                        is ImportTarget.New -> repo.findOrCreateDance(target.categoryName, target.danceName)
                    }
                    repo.importFigure(danceId, figure.copy(name = s.figureName.trim())) { m ->
                        when {
                            m.kind == MediaKind.YOUTUBE -> m.youtubeUrl ?: MISSING_URI
                            m.file != null -> uris[m.file] ?: MISSING_URI
                            else -> MISSING_URI
                        }
                    }
                }
                _state.update { it.copy(adding = false, addedFigureId = id) }
            } catch (e: ArchiveException) {
                _state.update { it.copy(adding = false, error = e.reason) }
            } catch (e: Exception) {
                _state.update { it.copy(adding = false, error = ArchiveError.CORRUPT) }
            }
        }
    }
}
