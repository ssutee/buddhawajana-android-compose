package com.watnapp.buddhawajana.core.data.repo

import com.watnapp.buddhawajana.core.data.db.BookmarkDao
import com.watnapp.buddhawajana.core.data.db.BookmarkEntity
import com.watnapp.buddhawajana.core.data.mapper.toModel
import com.watnapp.buddhawajana.core.model.Bookmark
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BookmarkRepository(private val dao: BookmarkDao) {
    fun stream(bookId: Long): Flow<List<Bookmark>> = dao.stream(bookId).map { it.map(BookmarkEntity::toModel) }
    suspend fun add(bookId: Long, page: Int, note: String, now: Long = System.currentTimeMillis()) {
        dao.insert(BookmarkEntity(bookId = bookId, page = page, note = note, addedAt = now))
    }
    suspend fun delete(bookmark: Bookmark) {
        dao.delete(BookmarkEntity(id = bookmark.id, bookId = bookmark.bookId, page = bookmark.page, note = bookmark.note, addedAt = bookmark.addedAt))
    }
}
