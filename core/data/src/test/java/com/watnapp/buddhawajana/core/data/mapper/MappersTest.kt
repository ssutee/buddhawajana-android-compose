package com.watnapp.buddhawajana.core.data.mapper

import com.watnapp.buddhawajana.core.network.dto.BookDto
import org.junit.Assert.assertEquals
import org.junit.Test

class MappersTest {
    @Test
    fun `BookDto maps to entity then model preserving fields`() {
        val dto = BookDto(id = "7", name = "ตถาคต", sortOrder = 2, totalpage = 50, producer = "P", file = "b.pdf", cover = "c.png")
        val model = dto.toEntity().toModel()
        assertEquals("7", model.id)
        assertEquals("ตถาคต", model.title)
        assertEquals(2, model.orderNumber)
        assertEquals("b.pdf", model.fileUrl)
        assertEquals(50, model.totalPage)
    }
}
