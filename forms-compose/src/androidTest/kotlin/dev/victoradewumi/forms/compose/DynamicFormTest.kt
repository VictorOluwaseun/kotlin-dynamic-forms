package dev.victoradewumi.forms.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.victoradewumi.forms.core.*
import org.junit.Rule
import org.junit.Test

/** Device UI test confirms native interactions trigger rule evaluation and visibility changes. */
class DynamicFormTest {
    @get:Rule val compose = createComposeRule()

    @Test fun conditionalFieldAppearsAndDisappearsAfterSelection() {
        val engine = FormEngine(FormDefinition("f", "Form", 1, listOf(
            FieldDefinition("hazard", "Hazard?", FieldType.TOGGLE),
            FieldDefinition("details", "Hazard details", FieldType.TEXT, visibleWhen = VisibilityRule("hazard", "true")),
        )))
        val controller = FormController(engine)
        compose.setContent {
            MaterialTheme { DynamicForm(controller.state.collectAsState().value, controller::setAnswer) }
        }
        compose.onNodeWithText("Hazard details").assertDoesNotExist()
        compose.onNodeWithText("Yes").performClick()
        compose.onNodeWithText("Hazard details").assertIsDisplayed()
        compose.onNodeWithText("No").performClick()
        compose.onNodeWithText("Hazard details").assertDoesNotExist()
    }
}
