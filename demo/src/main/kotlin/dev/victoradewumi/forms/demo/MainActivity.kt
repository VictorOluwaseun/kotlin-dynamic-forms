package dev.victoradewumi.forms.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.victoradewumi.forms.demo.presentation.FormViewModel
import dev.victoradewumi.forms.demo.presentation.InspectionScreen

/** Thin Android entry point; all interaction state is owned by the retained ViewModel. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as FormsApplication
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(FormViewModel::class.java))
                @Suppress("UNCHECKED_CAST")
                return FormViewModel(app.forms, app.drafts, app.codec) as T
            }
        }
        setContent {
            val dark = isSystemInDarkTheme()
            val colors = when (dark) {
                true -> darkColorScheme(primary = Color(0xFF81D5C6))
                false -> lightColorScheme(primary = Color(0xFF146C60), surface = Color(0xFFF8FAF8))
            }
            MaterialTheme(colorScheme = colors) {
                val model: FormViewModel = viewModel(factory = factory)
                InspectionScreen(
                    state = model.state.collectAsStateWithLifecycle().value,
                    onAnswer = model::change,
                    onSave = model::saveDraft,
                    onClear = model::clearDraft,
                    onReview = model::previewSubmission,
                    onDismiss = model::dismissPreview,
                )
            }
        }
    }
}
