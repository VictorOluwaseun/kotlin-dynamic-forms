package dev.victoradewumi.forms.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Strict JSON boundary: unknown properties/types and malformed schemas fail before rendering. */
public class FormCodec {
    private val json = Json { prettyPrint = true }

    /** Decodes and validates the complete definition, including its dependency graph. */
    public fun decode(source: String): FormDefinition {
        require(source.length <= 1_000_000) { "Form definition exceeds the size limit." }
        return json.decodeFromString<FormDefinition>(source).also { FormEngine(it) }
    }

    /** Produces a portable payload for the host application's transport layer. */
    public fun encodeSubmission(submission: FormSubmission): String = json.encodeToString(submission)

    /** Serializes raw draft answers; unlike submission, these may contain hidden or invalid input. */
    public fun encodeAnswers(answers: Map<String, String>): String = json.encodeToString(answers)

    /** Restores raw input; the engine filters unknown field IDs when creating state. */
    public fun decodeAnswers(source: String): Map<String, String> = json.decodeFromString(source)
}
