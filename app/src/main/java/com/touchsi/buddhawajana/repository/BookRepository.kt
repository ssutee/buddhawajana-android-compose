package com.touchsi.buddhawajana.repository

import com.touchsi.buddhawajana.AppDatabaseProvider
import com.touchsi.buddhawajana.api.BookJson
import com.touchsi.buddhawajana.api.BookService
import com.touchsi.buddhawajana.api.toEntity
import com.touchsi.buddhawajana.entity.BookDao
import com.touchsi.buddhawajana.entity.BookEntity
import io.reactivex.Observable
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class BookRepository(databaseProvider: AppDatabaseProvider):
        Repository<BookEntity, BookJson>() {
    private var dao: BookDao = databaseProvider.getDatabase().bookDao()
    private val bookService by lazy { BookService.create() }

    override fun getItemsFromService(): Observable<List<BookJson>>
            = bookService.getAllBooks("68e5acb6f3be4b6fc21b751d7aa2acb0e2606193",
                "895bb2cb6995102747a7e5585150284556a79c0b", "getitem")
    override fun getEntityId(entity: BookEntity): Long = entity.bookId
    override fun getJsonId(json: BookJson): Long = json.id
    override fun convertJsonToEntity(json: BookJson): BookEntity = json.toEntity()

    override suspend fun get(id: Long): BookEntity = dao.get(id)
    override fun getItems(id: Long): Flow<List<BookEntity>> = dao.list()
    override suspend fun insert(entity: BookEntity) = dao.insert(entity)
    override suspend fun delete(entity: BookEntity) = dao.delete(entity)
    override suspend fun update(entity: BookEntity) = dao.update(entity)
    override fun shouldBeUpdated(entity: BookEntity, json: BookJson): Boolean {
        val addedAt = SimpleDateFormat("yyyy-MM-dd", Locale.UK).parse(json.date_added)
        if (entity.bookUrl != json.file || entity.coverUrl != json.cover ||
                entity.title != json.name || entity.addedAt.before(addedAt) ||
                entity.position != json.sort_order) {
            return true
        }
        return false
    }
}