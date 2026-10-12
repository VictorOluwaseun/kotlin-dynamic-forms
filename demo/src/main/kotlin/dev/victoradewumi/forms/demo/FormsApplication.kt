package dev.victoradewumi.forms.demo

import android.app.Application
import androidx.room.Room
import dev.victoradewumi.forms.core.FormCodec
import dev.victoradewumi.forms.demo.data.AssetFormRepository
import dev.victoradewumi.forms.demo.data.DraftDatabase
import dev.victoradewumi.forms.demo.data.RoomDraftRepository

/** Composition root: creates concrete storage once and injects repository interfaces into the UI. */
class FormsApplication : Application() {
    val codec by lazy { FormCodec() }
    private val database by lazy {
        Room.databaseBuilder(this, DraftDatabase::class.java, "form-drafts.db").build()
    }
    val drafts by lazy { RoomDraftRepository(database.drafts(), codec) }
    val forms by lazy { AssetFormRepository(assets, codec) }
}
