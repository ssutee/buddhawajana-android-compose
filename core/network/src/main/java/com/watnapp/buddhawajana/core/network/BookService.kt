package com.watnapp.buddhawajana.core.network

import com.watnapp.buddhawajana.core.network.dto.BookDto
import retrofit2.http.GET
import retrofit2.http.Query

interface BookService {
    @GET("api")
    suspend fun getBooks(
        @Query("token") token: String = "",
        @Query("method") method: String = "getitem",
    ): List<BookDto>
}
