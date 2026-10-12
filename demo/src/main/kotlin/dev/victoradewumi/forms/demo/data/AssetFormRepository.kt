package dev.victoradewumi.forms.demo.data

import android.content.res.AssetManager
import dev.victoradewumi.forms.core.FormCodec
import dev.victoradewumi.forms.core.FormDefinition
import dev.victoradewumi.forms.demo.domain.FormRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Offline schema source; replace this adapter to fetch definitions from a backend. */
class AssetFormRepository(
    private val assets: AssetManager,
    private val codec: FormCodec,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : FormRepository {
    override suspend fun load(): FormDefinition = withContext(dispatcher) {
        assets.open("site-inspection.json").bufferedReader().use { codec.decode(it.readText()) }
    }
}
