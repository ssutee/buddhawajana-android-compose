package com.watnapp.buddhawajana.core.data.repo

import com.watnapp.buddhawajana.core.data.db.AudioDao
import com.watnapp.buddhawajana.core.data.db.AudioEntity
import com.watnapp.buddhawajana.core.data.download.MetadataProber
import com.watnapp.buddhawajana.core.model.Audio
import com.watnapp.buddhawajana.core.network.AudioService
import com.watnapp.buddhawajana.core.network.dto.AlbumDto
import com.watnapp.buddhawajana.core.network.dto.AudioDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        override suspend fun deleteNotInAlbum(albumId: Long, ids: List<Long>) {
            stored.removeAll { it.albumId == albumId && it.audioId !in ids }
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

    @Test fun `ensureMetadata probes and persists when missing`() = runTest {
        stored.add(AudioEntity(audioId = 2, albumId = 9, title = "T", url = "http://x/2.mp3"))
        val prober = MetadataProber(headSize = { 7_000_000L }, readDuration = { 60_000L })
        val repo = AudioRepository(dao, service, prober)
        repo.ensureMetadata(Audio("2", "9", "T", "http://x/2.mp3"))
        val row = dao.getAllOnce(9).single { it.audioId == 2L }
        assertEquals(60_000L, row.durationMs)
        assertEquals(7_000_000L, row.sizeBytes)
    }

    @Test fun `ensureMetadata no-ops when already fully probed`() = runTest {
        var probed = false
        val prober = MetadataProber(headSize = { probed = true; 1L }, readDuration = { probed = true; 1L })
        val repo = AudioRepository(dao, service, prober)
        repo.ensureMetadata(Audio("2", "9", "T", "http://x/2.mp3", durationMs = 1, sizeBytes = 2))
        assertFalse(probed)
    }

    @Test fun `ensureMetadata no-ops when no prober`() = runTest {
        stored.add(AudioEntity(audioId = 3, albumId = 9, title = "T", url = "http://x/3.mp3"))
        val repo = AudioRepository(dao, service)
        repo.ensureMetadata(Audio("3", "9", "T", "http://x/3.mp3"))
        val row = dao.getAllOnce(9).single { it.audioId == 3L }
        assertNull(row.durationMs)
    }

    @Test fun `refresh removes audios the server no longer returns`() = runTest {
        stored.add(AudioEntity(audioId = 1, albumId = 9, title = "keep", url = "u1"))
        stored.add(AudioEntity(audioId = 99, albumId = 9, title = "stale", url = "u99"))
        AudioRepository(dao, service).refresh("9") // service returns only id=1
        assertEquals(listOf(1L), dao.getAllOnce(9).map { it.audioId })
    }
}
