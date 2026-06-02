package com.watnapp.buddhawajana.core.data.repo

import app.cash.turbine.test
import com.watnapp.buddhawajana.core.data.db.BookmarkDao
import com.watnapp.buddhawajana.core.data.db.BookmarkEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class BookmarkRepositoryTest {
    private val store = MutableStateFlow<List<BookmarkEntity>>(emptyList())
    private var nextId = 1L
    private val dao = object : BookmarkDao {
        override fun stream(bookId: Long) = store.map { it.filter { b -> b.bookId == bookId }.sortedBy { b -> b.page } }
        override suspend fun insert(bookmark: BookmarkEntity): Long {
            val withId = bookmark.copy(id = nextId++); store.update { it + withId }; return withId.id
        }
        override suspend fun delete(bookmark: BookmarkEntity) = store.update { it.filterNot { b -> b.id == bookmark.id } }
    }

    @Test
    fun `add then stream returns bookmark as model`() = runTest {
        val repo = BookmarkRepository(dao)
        repo.add(bookId = 5, page = 12, note = "หน้า 12")
        repo.stream(5).test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals(12, list[0].page)
            assertEquals("หน้า 12", list[0].note)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
