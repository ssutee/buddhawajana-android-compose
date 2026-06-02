package com.watnapp.buddhawajana.core.data.repo

import com.watnapp.buddhawajana.core.data.db.ReadingProgressDao
import com.watnapp.buddhawajana.core.data.db.ReadingProgressEntity
import com.watnapp.buddhawajana.core.data.mapper.toModel
import com.watnapp.buddhawajana.core.model.ReadingProgress

class ReadingProgressRepository(private val dao: ReadingProgressDao) {
    suspend fun get(bookId: Long): ReadingProgress? = dao.get(bookId)?.toModel()
    suspend fun save(bookId: Long, page: Int, now: Long = System.currentTimeMillis()) {
        dao.upsert(ReadingProgressEntity(bookId = bookId, page = page, updatedAt = now))
    }
}
