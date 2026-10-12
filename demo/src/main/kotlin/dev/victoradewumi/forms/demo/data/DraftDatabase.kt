package dev.victoradewumi.forms.demo.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase

/** One explicitly saved local draft per form revision. */
@Entity(tableName = "drafts", primaryKeys = ["formId", "revision"])
data class DraftEntity(val formId: String, val revision: Int, val answersJson: String)

/** All disk access is suspendable; callers never block the UI thread. */
@Dao
interface DraftDao {
    @Query("SELECT * FROM drafts WHERE formId = :formId AND revision = :revision LIMIT 1")
    suspend fun find(formId: String, revision: Int): DraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(draft: DraftEntity)

    @Query("DELETE FROM drafts WHERE formId = :formId AND revision = :revision")
    suspend fun delete(formId: String, revision: Int)
}

/** Versioned sample database. Exported schemas are committed for future migration tests. */
@Database(entities = [DraftEntity::class], version = 1, exportSchema = true)
abstract class DraftDatabase : RoomDatabase() {
    abstract fun drafts(): DraftDao
}
