package dev.victoradewumi.forms.demo.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.victoradewumi.forms.compose.DynamicForm

/** A functional showcase: conditional native fields, explicit offline saves and payload inspection. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionScreen(
    state: DemoState,
    onAnswer: (String, String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    onReview: () -> Unit,
    onDismiss: () -> Unit,
) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    Scaffold(topBar = { TopAppBar(title = { Text("Dynamic Forms") }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (state.loading) CircularProgressIndicator()
            state.failure?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.definition?.let { definition ->
                Text("OFFLINE INSPECTION", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(definition.title, style = MaterialTheme.typography.headlineMedium)
                Text(definition.description, style = MaterialTheme.typography.bodyLarge)
                Text(state.message, style = MaterialTheme.typography.labelLarge)
                state.form?.let { form -> DynamicForm(form, onAnswer, enabled = !state.busy) }
                if (state.form?.showErrors == true && !state.form.isValid) {
                    Text("Complete the highlighted fields above to continue.", color = MaterialTheme.colorScheme.error)
                }
                Button(onClick = onReview, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                    Text("Review submission")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onSave, enabled = !state.busy, modifier = Modifier.weight(1f)) {
                        Text(if (state.busy) "Working…" else "Save draft")
                    }
                    TextButton(onClick = { confirmClear = true }, enabled = !state.busy) { Text("Clear") }
                }
                Text("Save your draft before leaving. Review creates a local JSON preview; nothing is sent.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("Clear this draft?") },
        text = { Text("This removes the saved draft and all current answers.") },
        confirmButton = { TextButton(onClick = { confirmClear = false; onClear() }) { Text("Clear draft") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Keep draft") } },
    )
    state.payload?.let { json ->
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Submission preview") },
            text = { SelectionContainer { Text(json, Modifier.verticalScroll(rememberScrollState()), fontFamily = FontFamily.Monospace) } },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        )
    }
}
