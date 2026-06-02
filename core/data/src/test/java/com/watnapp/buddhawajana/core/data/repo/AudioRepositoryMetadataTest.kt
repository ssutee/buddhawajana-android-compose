package com.watnapp.buddhawajana.core.data.repo

import com.watnapp.buddhawajana.core.data.db.AudioDao
import com.watnapp.buddhawajana.core.data.db.AudioEntity
import com.watnapp.buddhawajana.core.network.AudioService
import com.watnapp.buddhawajana.core.network.dto.AlbumDto
import com.watnapp.buddhawajana.core.network.dto.AudioDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioRepositoryMetadataTest {
    private val stored = mutableListOf<AudioEntity>()
    private val dao = object : AudioDao {
        override fun stream(albumId: Long): Flow<List<AudioEntity>> = flowOf(stored.filter { it.albumId == albumId })
        override suspend fun getAllOnce(albumId: Long) = stored.filter { it.albumId == albumId }
        override suspend fun upsertAll(items: List<AudioEntity>) {
            items.forEach { e -> stored.removeAll { it.audioId == e.audioId }; stored.add(e) }
        }
        override suspend fun count() = stored.size
        override suspend fun updateMetadata(audioId: Long, durationMs: Long?, sizeBytes: Long?) {
            stored.replaceAll { if (it.audioId == audioId) it.copy(durationMs = durationMs, sizeBytes = sizeBytes) else it }
        }
    }
    private val service = object : AudioService {
        override suspend fun getAlbums(): List<AlbumDto> = emptyList()
        override suspend fun getAudios(albumId: String) = listOf(AudioDto(id = "1", name = "T2", fileUrl = "http://x/1.mp3"))
    }

    @Test fun `refresh keeps probed duration and size`() = runTest {
        stored.add(AudioEntity(audioId = 1, albumId = 9, title = "T1", url = "http://x/1.mp3", durationMs = 323_000, sizeBytes = 12_000_000))
        val repo = AudioRepository(dao, service)
        repo.refresh("9")
        val row = dao.getAllOnce(9).single()
        assertEquals("T2", row.title)
        assertEquals(323_000L, row.durationMs)
        assertEquals(12_000_000L, row.sizeBytes)
    }
}
