package com.watnapp.buddhawajana.core.network

import com.watnapp.buddhawajana.core.network.dto.BookDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class BookServiceContractTest {
    @Test
    fun `book json decodes into BookDto with snake_case mapping`() {
        val json = """[{"id":"1","name":"Test","sort_order":3,"totalpage":120,"producer":"X","file":"f.pdf","cover":"c.png"}]"""
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val type = Types.newParameterizedType(List::class.java, BookDto::class.java)
        val list: List<BookDto> = moshi.adapter<List<BookDto>>(type).fromJson(json)!!

        assertEquals("1", list[0].id)
        assertEquals(3, list[0].sortOrder)
        assertEquals(120, list[0].totalpage)
    }
}
