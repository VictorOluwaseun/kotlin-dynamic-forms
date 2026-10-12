package dev.victoradewumi.forms.core

import org.junit.Assert.*
import org.junit.Test

/** Behavioral contract tests for the SDK, independent of Android and its UI toolkit. */
class FormEngineTest {
    private val hazard = FieldDefinition("hazard", "Hazard found", FieldType.TOGGLE, required = true)
    private val details = FieldDefinition(
        "details", "Details", FieldType.TEXT, required = true, minLength = 5,
        visibleWhen = VisibilityRule("hazard", "true"),
    )
    private fun engine(vararg fields: FieldDefinition) = FormEngine(FormDefinition("inspection", "Inspection", 1, fields.toList()))

    @Test fun `hidden required fields do not block submission`() {
        val result = engine(hazard, details).submit(mapOf("hazard" to "false"))
        assertTrue(result is SubmitResult.Accepted)
    }

    @Test fun `visible required fields block submission`() {
        val result = engine(hazard, details).submit(mapOf("hazard" to "true")) as SubmitResult.Rejected
        assertEquals("required", result.errors.getValue("details").code)
    }

    @Test fun `hidden answers are retained in state but excluded from payload`() {
        val engine = engine(hazard, details)
        val answers = mapOf("hazard" to "false", "details" to "previous sensitive detail")
        assertEquals(answers, engine.evaluate(answers).answers)
        assertEquals(mapOf("hazard" to "false"), (engine.submit(answers) as SubmitResult.Accepted).submission.answers)
    }

    @Test fun `hidden ancestors hide all descendants even with matching saved values`() {
        val third = FieldDefinition("action", "Action", FieldType.TEXT, required = true, visibleWhen = VisibilityRule("details", "danger"))
        val state = engine(hazard, details, third).evaluate(mapOf("hazard" to "false", "details" to "danger"))
        assertEquals(listOf("hazard"), state.visibleFields.map { it.id })
        assertTrue(state.isValid)
    }

    @Test fun `forward dependencies work while displayed field order remains stable`() {
        val state = engine(details, hazard).evaluate(mapOf("hazard" to "true"))
        assertEquals(listOf("details", "hazard"), state.visibleFields.map { it.id })
    }

    @Test fun `unknown answer ids cannot enter a submission`() {
        val result = engine(hazard).submit(mapOf("hazard" to "false", "admin" to "true")) as SubmitResult.Accepted
        assertFalse("admin" in result.submission.answers)
    }

    @Test fun `required boolean false is an explicit valid answer`() {
        assertTrue(engine(hazard).evaluate(mapOf("hazard" to "false")).isValid)
        assertFalse(engine(hazard).evaluate(emptyMap()).isValid)
    }

    @Test fun `invalid boolean is rejected`() {
        assertEquals("boolean", engine(hazard).evaluate(mapOf("hazard" to "yes")).errors["hazard"]?.code)
    }

    @Test fun `whitespace does not satisfy a required text field`() {
        val field = FieldDefinition("name", "Name", FieldType.TEXT, required = true)
        assertEquals("required", engine(field).evaluate(mapOf("name" to "   ")).errors["name"]?.code)
    }

    @Test fun `text length bounds are inclusive and evaluated on trimmed text`() {
        val field = FieldDefinition("name", "Name", FieldType.TEXT, minLength = 2, maxLength = 4)
        val engine = engine(field)
        assertTrue(engine.evaluate(mapOf("name" to " ab ")).isValid)
        assertTrue(engine.evaluate(mapOf("name" to "abcd")).isValid)
        assertEquals("too_short", engine.evaluate(mapOf("name" to "a")).errors["name"]?.code)
        assertEquals("too_long", engine.evaluate(mapOf("name" to "abcde")).errors["name"]?.code)
    }

    @Test fun `numeric boundaries accept zero and negative decimal values`() {
        val field = FieldDefinition("temperature", "Temperature", FieldType.NUMBER, minValue = -2.5, maxValue = 3.0)
        val engine = engine(field)
        listOf("-2.5", "0", "3.0").forEach { assertTrue(engine.evaluate(mapOf("temperature" to it)).isValid) }
        assertEquals("minimum", engine.evaluate(mapOf("temperature" to "-2.6")).errors["temperature"]?.code)
        assertEquals("maximum", engine.evaluate(mapOf("temperature" to "3.1")).errors["temperature"]?.code)
    }

    @Test fun `nonfinite exponent and malformed numbers are rejected`() {
        val engine = engine(FieldDefinition("value", "Value", FieldType.NUMBER))
        listOf("NaN", "Infinity", "1e999999999", "1,000", "2..1", "-", "9".repeat(100)).forEach {
            assertEquals(it, "number", engine.evaluate(mapOf("value" to it)).errors["value"]?.code)
        }
    }

    @Test fun `forged choices are rejected`() {
        val field = FieldDefinition("status", "Status", FieldType.CHOICE, options = listOf(ChoiceOption("open", "Open")))
        assertEquals("option", engine(field).evaluate(mapOf("status" to "administrator")).errors["status"]?.code)
    }

    @Test fun `blank optional values are omitted from payload`() {
        val engine = engine(FieldDefinition("note", "Note", FieldType.TEXT))
        assertTrue((engine.submit(mapOf("note" to "  ")) as SubmitResult.Accepted).submission.answers.isEmpty())
    }

    @Test fun `duplicate field ids fail before rendering`() {
        assertThrows(IllegalArgumentException::class.java) { engine(hazard, hazard) }
    }

    @Test fun `unknown visibility dependencies fail before rendering`() {
        assertThrows(IllegalArgumentException::class.java) { engine(details) }
    }

    @Test fun `dependency cycles are rejected`() {
        val a = details.copy(id = "a", visibleWhen = VisibilityRule("b", "yes"))
        val b = details.copy(id = "b", visibleWhen = VisibilityRule("a", "yes"))
        assertThrows(IllegalArgumentException::class.java) { engine(a, b) }
    }

    @Test fun `self references are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { engine(details.copy(visibleWhen = VisibilityRule("details", "yes"))) }
    }

    @Test fun `schema version is enforced`() {
        assertThrows(IllegalArgumentException::class.java) { FormEngine(FormDefinition("a", "A", 1, listOf(hazard), schemaVersion = 2)) }
    }

    @Test fun `invalid constraints and empty options are rejected`() {
        val invalid = listOf(
            FieldDefinition("a", "A", FieldType.TEXT, minLength = 5, maxLength = 2),
            FieldDefinition("a", "A", FieldType.NUMBER, minValue = 5.0, maxValue = 2.0),
            FieldDefinition("a", "A", FieldType.CHOICE),
            FieldDefinition("a", "A", FieldType.NUMBER, maxValue = Double.POSITIVE_INFINITY),
            FieldDefinition("a", "A", FieldType.TEXT, minValue = 2.0),
        )
        invalid.forEach { field -> assertThrows(IllegalArgumentException::class.java) { engine(field) } }
    }

    @Test fun `controller reveals errors on submit and recomputes them after edits`() {
        val controller = FormController(engine(hazard, details))
        assertFalse(controller.state.value.showErrors)
        assertTrue(controller.submit() is SubmitResult.Rejected)
        assertTrue(controller.state.value.showErrors)
        controller.setAnswer("hazard", "false")
        assertTrue(controller.state.value.isValid)
        controller.reset()
        assertFalse(controller.state.value.showErrors)
        assertTrue(controller.state.value.answers.isEmpty())
    }

    @Test fun `strict codec rejects unsupported properties`() {
        val json = """{"id":"a","title":"A","revision":1,"fields":[],"execute":"script"}"""
        assertThrows(IllegalArgumentException::class.java) { FormCodec().decode(json) }
    }

    @Test fun `answer codec preserves unicode quotes and newlines`() {
        val answers = mapOf("note" to "Lagos — \"ready\"\n✓")
        assertEquals(answers, FormCodec().decodeAnswers(FormCodec().encodeAnswers(answers)))
    }
}
