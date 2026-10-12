package dev.victoradewumi.forms.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** StateFlow adapter around the pure engine. Storage and networking stay in the host app. */
public class FormController(
    private val engine: FormEngine,
    initialAnswers: Map<String, String> = emptyMap(),
) {
    private val mutableState = MutableStateFlow(engine.evaluate(initialAnswers))
    public val state: StateFlow<FormState> = mutableState.asStateFlow()

    /** Applies a field edit atomically; unknown field IDs are rejected as programming errors. */
    public fun setAnswer(fieldId: String, value: String) {
        require(engine.definition.fields.any { it.id == fieldId }) { "Unknown field: $fieldId" }
        mutableState.update { old -> engine.evaluate(old.answers + (fieldId to value), old.showErrors) }
    }

    /** Clears answers and validation presentation, without affecting persisted host data. */
    public fun reset(): Unit = mutableState.update { engine.evaluate(emptyMap()) }

    /** Reveals validation errors and validates an immutable snapshot for submission. */
    public fun submit(): SubmitResult {
        mutableState.update { it.copy(showErrors = true) }
        return engine.submit(mutableState.value.answers)
    }
}
