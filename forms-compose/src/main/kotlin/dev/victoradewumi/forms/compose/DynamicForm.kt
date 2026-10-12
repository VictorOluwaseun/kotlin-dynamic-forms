package dev.victoradewumi.forms.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.victoradewumi.forms.core.ChoiceOption
import dev.victoradewumi.forms.core.FieldDefinition
import dev.victoradewumi.forms.core.FieldType
import dev.victoradewumi.forms.core.FormState

/**
 * Stateless native renderer. The host owns scrolling, persistence, state collection and submission.
 * Override [fieldContent] to customize widgets while retaining the core's rules and validation.
 */
@Composable
public fun DynamicForm(
    state: FormState,
    onAnswerChange: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fieldContent: @Composable (FieldDefinition, String, String?, Boolean, (String) -> Unit) -> Unit =
        { field, value, error, active, change -> DefaultFormField(field, value, error, active, change) },
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        state.visibleFields.forEach { field ->
            key(field.id) {
                val error = state.errors[field.id]?.message.takeIf { state.showErrors }
                fieldContent(field, state.answers[field.id].orEmpty(), error, enabled) {
                    onAnswerChange(field.id, it)
                }
            }
        }
    }
}

/** Default Material inputs; required toggles use explicit yes/no choices, never implicit false. */
@Composable
public fun DefaultFormField(
    field: FieldDefinition,
    value: String,
    error: String?,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
) {
    val label = field.label + if (field.required) " *" else ""
    when (field.type) {
        FieldType.TEXT, FieldType.NUMBER -> OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it.take(4096)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            placeholder = { Text(field.placeholder) },
            supportingText = { Text(error ?: field.help) },
            isError = error != null,
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = when (field.type) {
                    FieldType.NUMBER -> KeyboardType.Decimal
                    else -> KeyboardType.Text
                },
            ),
        )
        FieldType.CHOICE -> ChoiceField(label, field.options, value, field.help, error, enabled, onValueChange)
        FieldType.TOGGLE -> ChoiceField(
            label, listOf(ChoiceOption("true", "Yes"), ChoiceOption("false", "No")),
            value, field.help, error, enabled, onValueChange,
        )
    }
}

@Composable
private fun ChoiceField(
    label: String,
    options: List<ChoiceOption>,
    value: String,
    help: String,
    error: String?,
    enabled: Boolean,
    onChange: (String) -> Unit,
) {
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        options.forEach { option ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(
                    selected = value == option.value,
                    enabled = enabled,
                    role = Role.RadioButton,
                    onClick = { onChange(option.value) },
                ).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = value == option.value, onClick = null, enabled = enabled)
                Text(option.label, Modifier.padding(start = 12.dp))
            }
        }
        Text(
            error ?: help,
            color = when (error) {
                null -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.error
            },
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
