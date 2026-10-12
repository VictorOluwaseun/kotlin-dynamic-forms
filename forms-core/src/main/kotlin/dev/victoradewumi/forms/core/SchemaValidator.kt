package dev.victoradewumi.forms.core

/** Validates schema invariants and establishes a nonrecursive dependency evaluation order. */
internal object SchemaValidator {
    fun validate(form: FormDefinition): List<FieldDefinition> {
        require(form.schemaVersion == 1) { "Unsupported schemaVersion: ${form.schemaVersion}" }
        require(form.id.isNotBlank() && form.title.isNotBlank()) { "Form ID and title are required." }
        require(form.revision > 0) { "Form revision must be positive." }
        require(form.fields.size in 1..200) { "A form must contain 1–200 fields." }
        val byId = form.fields.associateBy { it.id }
        require(byId.size == form.fields.size) { "Field IDs must be unique." }
        form.fields.forEach { validateField(it, byId) }
        return dependencyOrder(form.fields)
    }

    private fun validateField(field: FieldDefinition, byId: Map<String, FieldDefinition>) {
        require(field.id.matches(Regex("[a-zA-Z][a-zA-Z0-9_]{0,63}"))) { "Invalid field ID: ${field.id}" }
        require(field.label.isNotBlank()) { "Field ${field.id} requires a label." }
        require(field.minLength == null || field.minLength >= 0) { "minLength must be nonnegative." }
        require(field.maxLength == null || field.maxLength in 1..4096) { "maxLength must be 1–4096." }
        require((field.minLength ?: 0) <= (field.maxLength ?: 4096)) { "Invalid text length bounds." }
        require(field.minValue == null || field.minValue.isFinite()) { "minValue must be finite." }
        require(field.maxValue == null || field.maxValue.isFinite()) { "maxValue must be finite." }
        require((field.minValue ?: -Double.MAX_VALUE) <= (field.maxValue ?: Double.MAX_VALUE)) { "Invalid number bounds." }
        require(field.type == FieldType.TEXT || (field.minLength == null && field.maxLength == null)) { "Text bounds require a text field." }
        require(field.type == FieldType.NUMBER || (field.minValue == null && field.maxValue == null)) { "Number bounds require a number field." }
        require(field.type == FieldType.CHOICE || field.options.isEmpty()) { "Options require a choice field." }
        require(field.type != FieldType.CHOICE || field.options.isNotEmpty()) { "Choice fields need options." }
        require(field.options.map { it.value }.distinct().size == field.options.size) { "Option values must be unique." }
        require(field.options.all { it.value.isNotBlank() && it.label.isNotBlank() }) { "Option values and labels cannot be blank." }
        val rule = field.visibleWhen ?: return
        require(rule.fieldId != field.id) { "A field cannot depend on itself." }
        require(rule.fieldId in byId) { "Unknown visibility dependency: ${rule.fieldId}" }
        val parent = byId.getValue(rule.fieldId)
        require(parent.type != FieldType.CHOICE || parent.options.any { it.value == rule.equals }) { "Visibility rule references an unknown option." }
        require(parent.type != FieldType.TOGGLE || rule.equals in setOf("true", "false")) { "Toggle conditions must use true or false." }
    }

    private fun dependencyOrder(fields: List<FieldDefinition>): List<FieldDefinition> {
        val pending = fields.toMutableList()
        val ordered = mutableListOf<FieldDefinition>()
        val resolved = mutableSetOf<String>()
        while (pending.isNotEmpty()) {
            val ready = pending.filter { it.visibleWhen?.fieldId == null || it.visibleWhen.fieldId in resolved }
            require(ready.isNotEmpty()) { "Visibility dependencies contain a cycle." }
            ordered.addAll(ready)
            resolved.addAll(ready.map { it.id })
            pending.removeAll(ready.toSet())
        }
        return ordered
    }
}
