package dev.victoradewumi.forms.demo.data

import dev.victoradewumi.forms.core.FormCodec
import dev.victoradewumi.forms.demo.domain.DraftRepository

/** Adapts Room entities to the domain contract; database details stop at this boundary. */
class RoomDraftRepository(private val dao: DraftDao, private val codec: FormCodec) : DraftRepository {
    override suspend fun load(formId: String, revision: Int): Map<String, String>? {
        val entity = dao.find(formId, revision) ?: return null
        return codec.decodeAnswers(entity.answersJson)
    }

    override suspend fun save(formId: String, revision: Int, answers: Map<String, String>) {
        dao.upsert(DraftEntity(formId, revision, codec.encodeAnswers(answers)))
    }

    override suspend fun delete(formId: String, revision: Int) = dao.delete(formId, revision)
}
