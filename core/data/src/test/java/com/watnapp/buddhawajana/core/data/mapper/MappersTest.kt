package com.watnapp.buddhawajana.core.data.mapper

import com.watnapp.buddhawajana.core.network.dto.BookDto
import org.junit.Assert.assertEquals
import org.junit.Test

class MappersTest {
    @Test
    fun `BookDto maps to entity then model preserving fields`() {
        val dto = BookDto(id = "7", name = "ตถาคต", sortOrder = 2, totalpage = 50, producer = "P", file = "b.pdf", cover = "c.png", category = null)
        val model = dto.toEntity().toModel()
        assertEquals("7", model.id)
        assertEquals("ตถาคต", model.title)
        assertEquals(2, model.orderNumber)
        assertEquals("b.pdf", model.fileUrl)
        assertEquals(50, model.totalPage)
    }

    @Test
    fun `BookDto category maps through entity to model`() {
        val dto = BookDto(id = "7", name = "ตถาคต", sortOrder = 2, totalpage = 50, producer = "P", file = "b.pdf", cover = "c.png", category = "พุทธวจน")
        val model = dto.toEntity().toModel()
        assertEquals("พุทธวจน", model.category)
    }

    @Test
    fun `DownloadEntity maps to model preserving fields`() {
        val e = com.watnapp.buddhawajana.core.data.download.DownloadEntity(
            audioId = "5", title = "T", url = "u", albumId = "9", albumTitle = "A",
            coverUrl = null, fileName = "5.mp3", sizeBytes = 1234L, completedAt = 99L,
        )
        val m = e.toModel()
        assertEquals("5", m.audioId)
        assertEquals("A", m.albumTitle)
        assertEquals(1234L, m.sizeBytes)
        assertEquals(99L, m.completedAt)
    }
}
