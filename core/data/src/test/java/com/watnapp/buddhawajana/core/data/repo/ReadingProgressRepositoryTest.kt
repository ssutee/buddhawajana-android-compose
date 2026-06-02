package com.watnapp.buddhawajana.core.data.repo

import com.watnapp.buddhawajana.core.data.db.ReadingProgressDao
import com.watnapp.buddhawajana.core.data.db.ReadingProgressEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReadingProgressRepositoryTest {
    private val map = HashMap<Long, ReadingProgressEntity>()
    private val dao = object : ReadingProgressDao {
        override suspend fun get(bookId: Long) = map[bookId]
        override suspend fun upsert(progress: ReadingProgressEntity) { map[progress.bookId] = progress }
    }

    @Test
    fun `save then get returns page`() = runTest {
        val repo = ReadingProgressRepository(dao)
        assertNull(repo.get(9))
        repo.save(bookId = 9, page = 33, now = 1000L)
        assertEquals(33, repo.get(9)?.page)
    }
}
