package io.github.katzfuss11.tanzlexikon.ui.figur

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.katzfuss11.tanzlexikon.R
import io.github.katzfuss11.tanzlexikon.data.MediaEntity
import io.github.katzfuss11.tanzlexikon.data.MediaKind
import io.github.katzfuss11.tanzlexikon.data.Section
import io.github.katzfuss11.tanzlexikon.lexikon.FigureDetail
import io.github.katzfuss11.tanzlexikon.lexikon.LexikonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Die Arbeitskopie beim Bearbeiten. Erst "Speichern" schreibt sie in die Datenbank. */
data class FigurDraft(
    val name: String,
    val generalText: String,
    val leaderText: String,
    val followerText: String,
    val media: List<MediaEntity>,
)

data class FigurState(
    val detail: FigureDetail?,
    val editing: Boolean,
    val draft: FigurDraft?,
    @StringRes val error: Int? = null,
)

private data class EditState(val draft: FigurDraft? = null, @StringRes val error: Int? = null)

class FigurViewModel(private val repo: LexikonRepository, private val figureId: Long) : ViewModel() {
    private val edit = MutableStateFlow(EditState())

    val state: StateFlow<FigurState> = combine(repo.observeFigure(figureId), edit) { detail, e ->
        FigurState(detail = detail, editing = e.draft != null, draft = e.draft, error = e.error)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, FigurState(null, editing = false, draft = null))

    fun startEditing() {
        val d = state.value.detail ?: return
        edit.value = EditState(
            FigurDraft(d.figure.name, d.figure.generalText, d.figure.leaderText, d.figure.followerText, d.media),
        )
    }

    fun updateDraft(transform: (FigurDraft) -> FigurDraft) =
        edit.update { e -> e.draft?.let { e.copy(draft = transform(it)) } ?: e }

    fun cancelEditing() {
        edit.value = EditState()
    }

    /** Hängt neue Medien an einen Bereich an (nur im Entwurf). */
    fun addMedia(section: Section, items: List<NewMedia>) = updateDraft { d ->
        d.copy(
            media = d.media + items.map {
                MediaEntity(
                    figureId = figureId, section = section, kind = it.kind, uri = it.uri,
                    displayName = it.displayName, mimeType = it.mimeType, sizeBytes = it.sizeBytes, position = 0,
                )
            },
        )
    }

    /** Entfernt ein Medium aus dem Entwurf. Die Datei in der Galerie bleibt unberührt. */
    fun removeMedia(media: MediaEntity) = updateDraft { d -> d.copy(media = d.media.filterNot { it === media }) }

    /** Ersetzt die Datei eines Mediums, z. B. wenn die alte nicht mehr gefunden wird. */
    fun relinkMedia(media: MediaEntity, replacement: NewMedia) = updateDraft { d ->
        d.copy(
            media = d.media.map {
                if (it !== media) it
                else it.copy(
                    kind = replacement.kind, uri = replacement.uri, displayName = replacement.displayName,
                    mimeType = replacement.mimeType, sizeBytes = replacement.sizeBytes,
                )
            },
        )
    }

    fun save() {
        val draft = edit.value.draft ?: return
        if (draft.name.isBlank()) {
            edit.update { it.copy(error = R.string.name_required) }
            return
        }
        // Positionen je Bereich in der Reihenfolge des Entwurfs neu durchzählen.
        val media = draft.media.groupBy { it.section }.values.flatMap { list ->
            list.mapIndexed { i, m -> m.copy(position = i) }
        }
        viewModelScope.launch {
            repo.saveFigure(figureId, draft.name, draft.generalText, draft.leaderText, draft.followerText, media)
            edit.value = EditState()
        }
    }

    fun errorShown() = edit.update { it.copy(error = null) }

    fun trash(now: Long) {
        viewModelScope.launch { repo.trashFigure(figureId, now) }
    }
}

/** Ein frisch aufgenommenes oder ausgewähltes Medium, bevor es einer Figur gehört. */
data class NewMedia(
    val kind: MediaKind,
    val uri: String,
    val displayName: String? = null,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
)
