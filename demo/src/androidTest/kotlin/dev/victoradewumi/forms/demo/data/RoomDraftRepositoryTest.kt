package dev.victoradewumi.forms.demo.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.victoradewumi.forms.core.FormCodec
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Device integration test for exact revision isolation and Room persistence semantics. */
@RunWith(AndroidJUnit4::class)
class RoomDraftRepositoryTest {
    private lateinit var db: DraftDatabase
    private lateinit var repository: RoomDraftRepository

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DraftDatabase::class.java).build()
        repository = RoomDraftRepository(db.drafts(), FormCodec())
    }
    @After fun tearDown() = db.close()

    @Test fun saveRestoreOverwriteAndDeleteAreScopedToRevision() = runTest {
        repository.save("site", 1, mapOf("name" to "First"))
        repository.save("site", 2, mapOf("name" to "Second"))
        repository.save("site", 1, mapOf("name" to "Updated"))
        assertEquals("Updated", repository.load("site", 1)?.get("name"))
        assertEquals("Second", repository.load("site", 2)?.get("name"))
        assertNull(repository.load("site", 3))
        repository.delete("site", 1)
        assertNull(repository.load("site", 1))
        assertNotNull(repository.load("site", 2))
    }
}
