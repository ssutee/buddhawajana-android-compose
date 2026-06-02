package com.watnapp.buddhawajana.core.data.repo

import app.cash.turbine.test
import com.watnapp.buddhawajana.core.data.db.BookDao
import com.watnapp.buddhawajana.core.data.db.BookEntity
import com.watnapp.buddhawajana.core.network.BookService
import com.watnapp.buddhawajana.core.network.dto.BookDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fake in-memory BookDao backed by MutableStateFlow.
 * - stream() emits the current list on every change.
 * - getAllOnce() returns the current list snapshot.
 * - upsertAll() merges by bookId (REPLACE semantics).
 */
private class FakeBookDao : BookDao {
    private val _state = MutableStateFlow<List<BookEntity>>(emptyList())

    override fun stream(): Flow<List<BookEntity>> = _state

    override suspend fun getAllOnce(): List<BookEntity> = _state.value

    override suspend fun upsertAll(items: List<BookEntity>) {
        val current = _state.value.associateBy { it.bookId }.toMutableMap()
        items.forEach { current[it.bookId] = it }
        _state.value = current.values.toList()
    }

    override suspend fun count(): Int = _state.value.size

    fun seed(vararg entities: BookEntity) {
        _state.value = entities.toList()
    }
}

class BookRepositoryTest {

    // (a) refresh fetches from service, upserts, and stream() re-emits mapped models
    @Test
    fun `refresh fetches from service and stream emits mapped models`() = runTest {
        val dao = FakeBookDao()
        val service = mockk<BookService>()
        coEvery { service.getBooks() } returns listOf(
            BookDto(id = "1", name = "The Way", sortOrder = 1, totalpage = 100, producer = "P", file = "f.pdf", cover = "c.png", category = null)
        )

        val repo = BookRepository(dao, service)

        repo.stream().test {
            // initial empty emission
            assertEquals(emptyList<Any>(), awaitItem())

            val result = repo.refresh()
            assertTrue(result.isSuccess)

            val books = awaitItem()
            assertEquals(1, books.size)
            assertEquals("1", books[0].id)
            assertEquals("The Way", books[0].title)
        }
    }

    // (b) refresh returns Result.failure when the service throws
    @Test
    fun `refresh returns failure when service throws`() = runTest {
        val dao = FakeBookDao()
        val service = mockk<BookService>()
        coEvery { service.getBooks() } throws RuntimeException("Network error")

        val repo = BookRepository(dao, service)
        val result = repo.refresh()

        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }

    // (c) CRITICAL: a book with status/progress preserved after refresh
    @Test
    fun `refresh preserves existing download status and progress for books`() = runTest {
        val dao = FakeBookDao()
        // Pre-seed a book with download state
        dao.seed(
            BookEntity(
                bookId = 1L,
                title = "Old Title",
                coverUrl = "old_cover.png",
                bookUrl = "old.pdf",
                position = 1,
                status = 2,       // downloading
                progress = 65,    // 65% done
                requestId = "req-abc",
                new = false,
                updated = false,
            )
        )

        val service = mockk<BookService>()
        coEvery { service.getBooks() } returns listOf(
            BookDto(id = "1", name = "New Title", sortOrder = 1, totalpage = 200, producer = "P", file = "new.pdf", cover = "new_cover.png", category = null)
        )

        val repo = BookRepository(dao, service)
        val result = repo.refresh()
        assertTrue("refresh should succeed", result.isSuccess)

        // Check that the entity in the DAO still has the original status/progress/requestId
        val entities = dao.getAllOnce()
        assertEquals(1, entities.size)
        val entity = entities[0]
        assertEquals("Server title should be updated", "New Title", entity.title)
        assertEquals("new.pdf", entity.bookUrl)
        assertEquals("Download status MUST be preserved", 2, entity.status)
        assertEquals("Download progress MUST be preserved", 65, entity.progress)
        assertEquals("Download requestId MUST be preserved", "req-abc", entity.requestId)
    }

    // (d) new books (not in DB) are inserted with status=0 (no download state to preserve)
    @Test
    fun `refresh inserts new books with default download state`() = runTest {
        val dao = FakeBookDao()
        // DB is empty
        val service = mockk<BookService>()
        coEvery { service.getBooks() } returns listOf(
            BookDto(id = "42", name = "New Book", sortOrder = 5, totalpage = 50, producer = "P", file = "b.pdf", cover = "c.png", category = null)
        )

        val repo = BookRepository(dao, service)
        repo.refresh()

        val entities = dao.getAllOnce()
        assertEquals(1, entities.size)
        assertEquals(42L, entities[0].bookId)
        assertEquals(0, entities[0].status)
        assertEquals(0, entities[0].progress)
        assertEquals("", entities[0].requestId)
    }
}
