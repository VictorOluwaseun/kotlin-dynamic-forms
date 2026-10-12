package dev.victoradewumi.forms.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Versioned form contract; [revision] changes whenever a form's fields or rules change. */
@Serializable
public data class FormDefinition(
    val id: String,
    val title: String,
    val revision: Int,
    val fields: List<FieldDefinition>,
    val description: String = "",
    val schemaVersion: Int = 1,
)

/** Supported built-in input types. A boolean answer is the string "true" or "false". */
@Serializable
public enum class FieldType {
    @SerialName("text") TEXT,
    @SerialName("number") NUMBER,
    @SerialName("choice") CHOICE,
    @SerialName("toggle") TOGGLE,
}

/** Declarative field metadata. Text and number constraints are exclusive to their own types. */
@Serializable
public data class FieldDefinition(
    val id: String,
    val label: String,
    val type: FieldType,
    val required: Boolean = false,
    val help: String = "",
    val placeholder: String = "",
    val options: List<ChoiceOption> = emptyList(),
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val minValue: Double? = null,
    val maxValue: Double? = null,
    val visibleWhen: VisibilityRule? = null,
)

/** Stable submitted [value] and independently editable display [label]. */
@Serializable
public data class ChoiceOption(val value: String, val label: String)

/** Exact equality against another visible field; chained dependencies are supported. */
@Serializable
public data class VisibilityRule(val fieldId: String, val equals: String)

/** Machine-readable validation outcome with a default English message. */
public data class FieldError(val code: String, val message: String)

/** Renderer-independent state. Hidden answers are retained locally for reversible edits. */
public data class FormState(
    val answers: Map<String, String>,
    val visibleFields: List<FieldDefinition>,
    val errors: Map<String, FieldError>,
    val showErrors: Boolean = false,
) {
    public val isValid: Boolean get() = errors.isEmpty()
}

/** Validated payload. Only nonblank answers to currently visible fields are included. */
@Serializable
public data class FormSubmission(
    val formId: String,
    val revision: Int,
    val answers: Map<String, String>,
)

/** Submission succeeds only after all visible fields pass validation. */
public sealed interface SubmitResult {
    public data class Accepted(val submission: FormSubmission) : SubmitResult
    public data class Rejected(val errors: Map<String, FieldError>) : SubmitResult
}
