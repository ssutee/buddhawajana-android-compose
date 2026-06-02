package com.watnapp.buddhawajana.core.data.repo

import com.watnapp.buddhawajana.core.common.runCatchingCancellable
import com.watnapp.buddhawajana.core.data.db.AudioDao
import com.watnapp.buddhawajana.core.data.download.MetadataProber
import com.watnapp.buddhawajana.core.data.mapper.toEntity
import com.watnapp.buddhawajana.core.data.mapper.toModel
import com.watnapp.buddhawajana.core.model.Audio
import com.watnapp.buddhawajana.core.network.AudioService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Cache-first repository for audios (per-album).
 * Merge strategy (read-before-write): apply server content fields while KEEPING
 * user/device-owned columns — download state (status, progress, requestId) AND
 * probed metadata (durationMs, sizeBytes).
 */
class AudioRepository(
    private val dao: AudioDao,
    private val service: AudioService,
    private val prober: MetadataProber? = null,
) {
    fun stream(albumId: String): Flow<List<Audio>> =
        dao.stream(albumId.toLong()).map { entities -> entities.map { it.toModel() } }

    suspend fun refresh(albumId: String): Result<Unit> = runCatchingCancellable {
        val dtos = service.getAudios(albumId)
        val albumIdLong = albumId.toLong()
        val existing = dao.getAllOnce(albumIdLong).associateBy { it.audioId }
        val merged = dtos.map { dto ->
            val fresh = dto.toEntity(albumId)
            val current = existing[dto.id.toLong()]
            if (current != null) {
                fresh.copy(
                    status = current.status,
                    progress = current.progress,
                    requestId = current.requestId,
                    durationMs = current.durationMs,
                    sizeBytes = current.sizeBytes,
                )
            } else fresh
        }
        dao.upsertAll(merged)
        if (dtos.isNotEmpty()) dao.deleteNotInAlbum(albumIdLong, dtos.map { it.id.toLong() })
    }

    /** Probe + persist duration/size for [audio] if not already known. No-op if probed or no prober. */
    suspend fun ensureMetadata(audio: Audio) {
        if (audio.durationMs != null && audio.sizeBytes != null) return
        val p = prober ?: return
        val (dur, size) = p.probe(audio.url)
        if (dur != null || size != null) {
            dao.updateMetadata(audio.id.toLong(), dur ?: audio.durationMs, size ?: audio.sizeBytes)
        }
    }
}
