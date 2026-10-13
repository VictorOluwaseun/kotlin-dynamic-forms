package dev.victoradewumi.forms.demo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.victoradewumi.forms.core.FormCodec
import dev.victoradewumi.forms.core.FormController
import dev.victoradewumi.forms.core.FormDefinition
import dev.victoradewumi.forms.core.FormEngine
import dev.victoradewumi.forms.core.FormState
import dev.victoradewumi.forms.core.SubmitResult
import dev.victoradewumi.forms.demo.domain.DraftRepository
import dev.victoradewumi.forms.demo.domain.FormRepository
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SavedDraftSummary(val siteName: String, val savedAt: Instant)

/** UI state includes explicit disk status so a successful save is never implied before it completes. */
data class DemoState(
    val loading: Boolean = true,
    val definition: FormDefinition? = null,
    val form: FormState? = null,
    val busy: Boolean = false,
    val dirty: Boolean = false,
    val message: String = "",
    val payload: String? = null,
    val failure: String? = null,
    val savedDraft: SavedDraftSummary? = null,
)

/** Coordinates SDK state through repository boundaries; it knows nothing about Room or assets. */
class FormViewModel(
    private val forms: FormRepository,
    private val drafts: DraftRepository,
    private val codec: FormCodec,
    private val clock: Clock = Clock.systemUTC(),
) : ViewModel() {
    private val mutableState = MutableStateFlow(DemoState())
    val state = mutableState.asStateFlow()
    private var controller: FormController? = null

    init { load() }

    private fun load() {
        viewModelScope.launch {
            try {
                val definition = forms.load()
                val restored = drafts.load(definition.id, definition.revision)
                val session = FormController(FormEngine(definition), restored.orEmpty())
                controller = session
                mutableState.value = DemoState(
                    loading = false, definition = definition, form = session.state.value,
                    message = if (restored == null) "Ready for a new inspection" else "Saved draft restored",
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.value = DemoState(loading = false, failure = "Unable to load this form or draft. Close and reopen the app to retry.")
            }
        }
    }

    /** Editing is paused during disk actions to prevent overwriting newer input with an older save. */
    fun change(fieldId: String, value: String) {
        if (state.value.busy) return
        val session = controller ?: return
        session.setAnswer(fieldId, value)
        mutableState.update { it.copy(form = session.state.value, dirty = true, payload = null, message = "Unsaved changes") }
    }

    /** Persists exactly the current snapshot, including invalid and hidden answers. */
    fun saveDraft() {
        val definition = state.value.definition ?: return
        val answers = state.value.form?.answers ?: return
        performDiskAction(
            "Draft saved on this device",
            summary = {
                SavedDraftSummary(
                    siteName = answers["site_name"]?.trim().orEmpty().ifBlank { "Untitled inspection" },
                    savedAt = clock.instant(),
                )
            },
        ) { drafts.save(definition.id, definition.revision, answers) }
    }

    fun continueEditing() { mutableState.update { it.copy(savedDraft = null) } }

    /** Deletes the persisted record before clearing UI state, so failures keep the current draft. */
    fun clearDraft() {
        val definition = state.value.definition ?: return
        performDiskAction("Draft cleared") {
            drafts.delete(definition.id, definition.revision)
            controller?.reset()
        }
    }

    /** Validates and displays the JSON that a real host would send; no network request is made. */
    fun previewSubmission() {
        if (state.value.busy) return
        val session = controller ?: return
        val result = session.submit()
        mutableState.update {
            it.copy(
                form = session.state.value,
                payload = (result as? SubmitResult.Accepted)?.let { accepted -> codec.encodeSubmission(accepted.submission) },
                message = when (result) {
                    is SubmitResult.Accepted -> "Valid payload ready to review"
                    is SubmitResult.Rejected -> "Please complete the highlighted fields"
                },
            )
        }
    }

    fun dismissPreview() { mutableState.update { it.copy(payload = null) } }

    private fun performDiskAction(
        success: String,
        summary: (() -> SavedDraftSummary)? = null,
        action: suspend () -> Unit,
    ) {
        if (state.value.busy) return
        mutableState.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                action()
                val savedDraft = summary?.invoke()
                mutableState.update {
                    it.copy(form = controller?.state?.value, busy = false, dirty = false, payload = null, message = success, savedDraft = savedDraft)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.update { it.copy(busy = false, message = "Storage action failed. Your current input is still on screen; please retry.") }
            }
        }
    }
}
