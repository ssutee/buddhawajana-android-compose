package com.watnapp.buddhawajana.core.data.mapper

import com.watnapp.buddhawajana.core.network.dto.BookDto
import org.junit.Assert.assertEquals
import org.junit.Test

class MappersTest {
    @Test
    fun `BookDto maps to entity then model preserving fields`() {
        val dto = BookDto(id = "7", name = "ตถาคต", sortOrder = "2", totalpage = "50", producer = "P", file = "b.pdf", cover = "c.png", category = null)
        val model = dto.toEntity().toModel()
        assertEquals("7", model.id)
        assertEquals("ตถาคต", model.title)
        assertEquals(2, model.orderNumber)
        assertEquals("b.pdf", model.fileUrl)
        assertEquals(50, model.totalPage)
    }

    @Test
    fun `BookDto category maps through entity to model`() {
        val dto = BookDto(id = "7", name = "ตถาคต", sortOrder = "2", totalpage = "50", producer = "P", file = "b.pdf", cover = "c.png", category = "พุทธวจน")
        val model = dto.toEntity().toModel()
        assertEquals("พุทธวจน", model.category)
    }

    @Test
    fun `AlbumDto string count parses to itemCount`() {
        val dto = com.watnapp.buddhawajana.core.network.dto.AlbumDto(
            id = "9", albumName = "A", albumCover = null, count = "63240",
        )
        assertEquals(63240, dto.toEntity().toModel().itemCount)
    }

    @Test
    fun `AlbumDto null or non-numeric count is zero`() {
        val nul = com.watnapp.buddhawajana.core.network.dto.AlbumDto("9", "A", null, null)
        val junk = com.watnapp.buddhawajana.core.network.dto.AlbumDto("9", "A", null, "  ")
        assertEquals(0, nul.toEntity().toModel().itemCount)
        assertEquals(0, junk.toEntity().toModel().itemCount)
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
