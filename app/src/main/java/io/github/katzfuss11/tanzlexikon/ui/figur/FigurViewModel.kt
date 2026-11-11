package io.github.katzfuss11.tanzlexikon.ui.figur

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.katzfuss11.tanzlexikon.R
import io.github.katzfuss11.tanzlexikon.data.MediaEntity
import io.github.katzfuss11.tanzlexikon.data.MediaKind
import io.github.katzfuss11.tanzlexikon.data.Section
import io.github.katzfuss11.tanzlexikon.lexikon.FigureDetail
import io.github.katzfuss11.tanzlexikon.lexikon.LexikonRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Die Arbeitskopie beim Bearbeiten. Erst "Speichern" schreibt sie in die Datenbank. */
@Serializable
data class FigurDraft(
    val name: String,
    val generalText: String,
    val leaderText: String,
    val followerText: String,
    val media: List<MediaEntity>,
)

/** Wohin das nächste aufgenommene oder ausgewählte Medium gehört. */
@Serializable
sealed interface MediaTarget {
    /** In einen Bereich anhängen. */
    @Serializable data class Add(val section: Section) : MediaTarget

    /** Als Ersatz für das Medium an dieser Stelle im Entwurf (z. B. wenn es nicht mehr gefunden wird). */
    @Serializable data class Relink(val index: Int) : MediaTarget
}

data class FigurState(
    val detail: FigureDetail?,
    val editing: Boolean,
    val draft: FigurDraft?,
    @StringRes val error: Int? = null,
    /** Der Entwurf weicht vom gespeicherten Stand ab. */
    val hasChanges: Boolean = false,
)

private data class EditState(val draft: FigurDraft? = null, @StringRes val error: Int? = null)

/**
 * Der Entwurf und das Ziel des nächsten Mediums liegen auch im [saved]-Zustand. So überstehen sie es,
 * wenn Android die App beendet, während die Kamera läuft.
 */
class FigurViewModel(
    private val repo: LexikonRepository,
    private val figureId: Long,
    private val saved: SavedStateHandle = SavedStateHandle(),
    /** Für Schreibvorgänge, die auch nach dem Verlassen des Bildschirms fertig werden müssen. */
    private val workScope: CoroutineScope? = null,
) : ViewModel() {
    private val edit = MutableStateFlow(EditState(draft = saved.get<String>(KEY_DRAFT)?.let(::decodeDraft)))

    val state: StateFlow<FigurState> = combine(repo.observeFigure(figureId), edit) { detail, e ->
        FigurState(
            detail = detail, editing = e.draft != null, draft = e.draft, error = e.error,
            hasChanges = e.draft != null && detail != null && e.draft != draftOf(detail),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, FigurState(null, editing = false, draft = null))

    init {
        viewModelScope.launch {
            edit.collect { e -> saved[KEY_DRAFT] = e.draft?.let { json.encodeToString(FigurDraft.serializer(), it) } }
        }
    }

    fun startEditing() {
        val d = state.value.detail ?: return
        edit.value = EditState(draftOf(d))
    }

    fun updateDraft(transform: (FigurDraft) -> FigurDraft) =
        edit.update { e -> e.draft?.let { e.copy(draft = transform(it)) } ?: e }

    fun cancelEditing() {
        edit.value = EditState()
    }

    /** Merkt sich, wohin das gleich ausgewählte Medium gehört. */
    fun chooseMediaTarget(target: MediaTarget) {
        saved[KEY_TARGET] = json.encodeToString(MediaTarget.serializer(), target)
    }

    /** Ein aufgenommenes oder ausgewähltes Medium ist da: dorthin legen, wo [chooseMediaTarget] es haben wollte. */
    fun onMediaPicked(items: List<NewMedia>) {
        val target = saved.get<String>(KEY_TARGET)?.let { json.decodeFromString(MediaTarget.serializer(), it) } ?: return
        saved.remove<String>(KEY_TARGET)
        if (items.isEmpty()) return
        // Sollte der Entwurf fehlen, das Medium trotzdem nicht verlieren.
        if (edit.value.draft == null) startEditing()
        when (target) {
            is MediaTarget.Add -> addMedia(target.section, items)
            is MediaTarget.Relink -> relinkAt(target.index, items.first())
        }
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

    /** Ersetzt die Datei des Mediums an Stelle [index] im Entwurf. */
    private fun relinkAt(index: Int, replacement: NewMedia) = updateDraft { d ->
        d.copy(
            media = d.media.mapIndexed { i, it ->
                if (i != index) it
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

    /** Läuft im [workScope] weiter, weil der Bildschirm direkt danach geschlossen wird. */
    fun trash(now: Long) {
        (workScope ?: viewModelScope).launch { repo.trashFigure(figureId, now) }
    }

    private companion object {
        const val KEY_DRAFT = "entwurf"
        const val KEY_TARGET = "medienziel"
        val json = Json { ignoreUnknownKeys = true }

        fun draftOf(d: FigureDetail) =
            FigurDraft(d.figure.name, d.figure.generalText, d.figure.leaderText, d.figure.followerText, d.media)

        fun decodeDraft(text: String): FigurDraft? =
            runCatching { json.decodeFromString(FigurDraft.serializer(), text) }.getOrNull()
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
