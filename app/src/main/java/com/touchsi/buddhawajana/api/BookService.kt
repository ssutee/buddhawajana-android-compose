package com.touchsi.buddhawajana.api

import com.touchsi.buddhawajana.entity.BookEntity
import io.reactivex.Observable
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.*

data class BookJson(var id: Long = 0) {
    lateinit var name: String
    var sort_order: Int  = 0
    var totalpage: Int = 0
    lateinit var producer: String
    lateinit var description: String
    lateinit var date_added: String
    lateinit var file: String
    lateinit var cover: String
    lateinit var printed_date: String
}

fun BookJson.toEntity(): BookEntity {
    var book = BookEntity()
    book.bookId = this.id
    book.title = this.name
    book.position = this.sort_order
    book.pages = this.totalpage
    book.detail = this.description
    book.addedAt = SimpleDateFormat("yyyy-MM-dd", Locale.UK).parse(this.date_added)!!
    book.updatedAt = SimpleDateFormat("yyyy-MM-dd", Locale.UK).parse(this.date_added)!!
    book.bookUrl = this.file
    book.coverUrl = this.cover
    book.version = this.printed_date
    return book
}

interface BookService {
    @GET("api")
    fun getAllBooks(@Query("token") token: String,
                    @Query("token_secret") secret: String,
                    @Query("method") method: String): Observable<List<BookJson>>

    companion object {
        fun create(): BookService {
            val retrofit = Retrofit.Builder()
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .baseUrl("http://etipitaka.org/ebookshop/oauth/")
                    .build()
            return retrofit.create(BookService::class.java)
        }
    }
}

