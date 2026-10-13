package dev.victoradewumi.forms.demo.presentation

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import dev.victoradewumi.forms.demo.MainActivity
import java.io.File
import org.junit.Rule
import org.junit.Test

class InspectionScreenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun savingShowsSummaryAndDraftRestoresAfterRelaunch() {
        val siteName = "Save flow check"
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasSetTextAction() and hasText("Site name *"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasSetTextAction() and hasText("Site name *"))
            .performScrollTo().performTextInput(siteName)
        compose.onNodeWithText("Save draft").performScrollTo().performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Draft saved").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Draft saved").assertIsDisplayed()
        compose.onNodeWithText(siteName).assertIsDisplayed()
        compose.onNodeWithText("Saved ", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Continue editing").assertIsDisplayed()
        compose.onNodeWithText("Site name *").assertDoesNotExist()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val outputDirectory = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
            ?.let { File(it) } ?: checkNotNull(context.getExternalFilesDir("verification"))
        outputDirectory.mkdirs()
        val screenshot = File(outputDirectory, "draft-saved.png")
        screenshot.outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }

        compose.onNodeWithText("Continue editing").performClick()
        compose.onNode(hasSetTextAction() and hasText("Site name *"))
            .performScrollTo().assertTextContains(siteName)
        compose.activityRule.scenario.close()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Saved draft restored").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Saved draft restored").assertIsDisplayed()
            compose.onNode(hasSetTextAction() and hasText("Site name *"))
                .performScrollTo().assertTextContains(siteName)
        }
    }
}
