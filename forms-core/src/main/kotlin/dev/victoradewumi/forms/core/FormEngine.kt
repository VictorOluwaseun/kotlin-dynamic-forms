package dev.victoradewumi.forms.core

/** Stateless evaluator usable from Android, JVM services, tests or a custom renderer. */
public class FormEngine(public val definition: FormDefinition) {
    private val ordered = SchemaValidator.validate(definition)
    private val knownIds = definition.fields.map { it.id }.toSet()

    /** Resolves visibility before validation, without losing hidden input from a local draft. */
    public fun evaluate(answers: Map<String, String>, showErrors: Boolean = false): FormState {
        val knownAnswers = answers.filterKeys { it in knownIds }
        val visibleIds = mutableSetOf<String>()
        ordered.forEach { field ->
            val rule = field.visibleWhen
            val visible = rule == null || (rule.fieldId in visibleIds && knownAnswers[rule.fieldId] == rule.equals)
            if (visible) visibleIds.add(field.id)
        }
        val visible = definition.fields.filter { it.id in visibleIds }
        val errors = visible.mapNotNull { field ->
            validate(field, knownAnswers[field.id].orEmpty())?.let { field.id to it }
        }.toMap()
        return FormState(knownAnswers, visible, errors, showErrors)
    }

    /** Revalidates and builds a payload that never leaks values from hidden fields. */
    public fun submit(answers: Map<String, String>): SubmitResult {
        val state = evaluate(answers)
        if (!state.isValid) return SubmitResult.Rejected(state.errors)
        val visibleIds = state.visibleFields.map { it.id }.toSet()
        val payload = state.answers.filter { (key, value) -> key in visibleIds && value.isNotBlank() }
        return SubmitResult.Accepted(FormSubmission(definition.id, definition.revision, payload))
    }

    private fun validate(field: FieldDefinition, value: String): FieldError? {
        if (value.isBlank()) return requiredError(field)
        if (value.length > 4096) return FieldError("too_long", "Use at most 4096 characters.")
        return when (field.type) {
            FieldType.TEXT -> validateText(field, value)
            FieldType.NUMBER -> validateNumber(field, value)
            FieldType.CHOICE -> choiceError(field, value)
            FieldType.TOGGLE -> toggleError(value)
        }
    }

    private fun requiredError(field: FieldDefinition): FieldError? =
        when (field.required) {
            true -> FieldError("required", "${field.label} is required.")
            false -> null
        }

    private fun validateText(field: FieldDefinition, value: String): FieldError? {
        val length = value.trim().length
        if (length < (field.minLength ?: 0)) return FieldError("too_short", "Use at least ${field.minLength} characters.")
        if (length > (field.maxLength ?: 4096)) return FieldError("too_long", "Use at most ${field.maxLength} characters.")
        return null
    }

    private fun validateNumber(field: FieldDefinition, value: String): FieldError? {
        // Bound parsing work and exclude exponent notation, NaN and Infinity from the wire format.
        if (!value.matches(Regex("-?[0-9]{1,32}(\\.[0-9]{1,16})?"))) return FieldError("number", "Enter a valid decimal number.")
        val number = value.toBigDecimal()
        if (field.minValue != null && number < field.minValue.toBigDecimal()) return FieldError("minimum", "Enter ${field.minValue} or more.")
        if (field.maxValue != null && number > field.maxValue.toBigDecimal()) return FieldError("maximum", "Enter ${field.maxValue} or less.")
        return null
    }

    private fun choiceError(field: FieldDefinition, value: String): FieldError? =
        when (field.options.any { it.value == value }) {
            true -> null
            false -> FieldError("option", "Choose one of the available options.")
        }

    private fun toggleError(value: String): FieldError? = when (value) {
        "true", "false" -> null
        else -> FieldError("boolean", "Choose yes or no.")
    }
}
