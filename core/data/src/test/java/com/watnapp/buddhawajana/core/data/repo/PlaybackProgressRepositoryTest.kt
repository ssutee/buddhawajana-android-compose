package com.watnapp.buddhawajana.core.data.repo

import com.watnapp.buddhawajana.core.data.db.PlaybackProgressDao
import com.watnapp.buddhawajana.core.data.db.PlaybackProgressEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackProgressRepositoryTest {
    private val store = HashMap<String, PlaybackProgressEntity>()
    private val dao = object : PlaybackProgressDao {
        override suspend fun get(audioId: String) = store[audioId]
        override suspend fun upsert(progress: PlaybackProgressEntity) { store[progress.audioId] = progress }
    }

    @Test fun `save then get round-trips position`() = runTest {
        val repo = PlaybackProgressRepository(dao)
        assertNull(repo.get("5"))
        repo.save("5", 42_000L, now = 100L)
        val p = repo.get("5")!!
        assertEquals(42_000L, p.positionMs)
        assertEquals(100L, p.updatedAt)
    }

    @Test fun `save overwrites prior position`() = runTest {
        val repo = PlaybackProgressRepository(dao)
        repo.save("5", 1_000L, now = 1L)
        repo.save("5", 9_000L, now = 2L)
        assertEquals(9_000L, repo.get("5")!!.positionMs)
    }
}
