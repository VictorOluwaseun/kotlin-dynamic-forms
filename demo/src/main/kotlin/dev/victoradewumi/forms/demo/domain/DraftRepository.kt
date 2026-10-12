package dev.victoradewumi.forms.demo.domain

/** Storage contract; revision matching prevents stale answers being applied to changed forms. */
interface DraftRepository {
    suspend fun load(formId: String, revision: Int): Map<String, String>?
    suspend fun save(formId: String, revision: Int, answers: Map<String, String>)
    suspend fun delete(formId: String, revision: Int)
}
