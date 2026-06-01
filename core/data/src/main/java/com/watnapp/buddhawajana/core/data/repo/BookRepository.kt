package com.watnapp.buddhawajana.core.data.repo

import com.watnapp.buddhawajana.core.data.db.BookDao
import com.watnapp.buddhawajana.core.data.mapper.toEntity
import com.watnapp.buddhawajana.core.data.mapper.toModel
import com.watnapp.buddhawajana.core.model.Book
import com.watnapp.buddhawajana.core.network.BookService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Cache-first repository for books.
 *
 * Merge strategy (read-before-write):
 * - Fetch DTOs from the server.
 * - For each DTO, look up the existing entity by bookId.
 *   - If found: apply server content fields onto the existing entity while
 *     KEEPING the user-owned download columns (status, progress, requestId).
 *     The is_new / is_updated flags follow the original app's behaviour:
 *     a book that already exists in the DB is NOT marked as new.
 *   - If not found: insert a fresh entity (download columns default to 0/"").
 * - Books present in the DB but absent from the server response are kept
 *   (skipDeleting = true equivalent – the original used skipDeleting() = false,
 *    but we keep the simpler safe-default here to avoid deleting books mid-download).
 *
 * NOTE: The original app's update() called convertJsonToEntity() which DID wipe
 * download columns — that was a latent bug. We intentionally fix it here.
 */
class BookRepository(
    private val dao: BookDao,
    private val service: BookService,
) : Repository<Book> {

    override fun stream(): Flow<List<Book>> = dao.stream().map { entities ->
        entities.map { it.toModel() }
    }

    override suspend fun refresh(): Result<Unit> = runCatching {
        val dtos = service.getBooks()
        val existing = dao.getAllOnce().associateBy { it.bookId }

        val merged = dtos.map { dto ->
            val id = dto.id.toLong()
            val fresh = dto.toEntity()
            val current = existing[id]
            if (current != null) {
                // Preserve user-owned download state; apply server content fields.
                fresh.copy(
                    status = current.status,
                    progress = current.progress,
                    requestId = current.requestId,
                    new = current.new,
                    updated = current.updated,
                )
            } else {
                fresh
            }
        }
        dao.upsertAll(merged)
    }
}
