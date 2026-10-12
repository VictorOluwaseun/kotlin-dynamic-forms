package dev.victoradewumi.forms.demo

import dev.victoradewumi.forms.core.FormCodec
import dev.victoradewumi.forms.core.FormEngine
import dev.victoradewumi.forms.core.SubmitResult
import java.io.File
import org.junit.Assert.*
import org.junit.Test

/** Keeps the shipped JSON asset and the SDK contract compatible. */
class SampleContractTest {
    @Test fun `bundled inspection schema accepts a complete routine inspection`() {
        val source = File("src/main/assets/site-inspection.json").readText()
        val engine = FormEngine(FormCodec().decode(source))
        val result = engine.submit(mapOf(
            "site_name" to "Lekki service centre",
            "inspector" to "Sample inspector",
            "inspection_type" to "routine",
            "hazard_found" to "false",
            "people_on_site" to "0",
            "previous_reference" to "hidden-reference",
        )) as SubmitResult.Accepted
        assertEquals("site-inspection", result.submission.formId)
        assertEquals(1, result.submission.revision)
        assertFalse("previous_reference" in result.submission.answers)
    }
}
