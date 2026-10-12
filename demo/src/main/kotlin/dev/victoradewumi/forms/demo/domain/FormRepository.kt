package dev.victoradewumi.forms.demo.domain

import dev.victoradewumi.forms.core.FormDefinition

/** Supplies a validated definition without coupling presentation to assets or a future API. */
interface FormRepository {
    suspend fun load(): FormDefinition
}
