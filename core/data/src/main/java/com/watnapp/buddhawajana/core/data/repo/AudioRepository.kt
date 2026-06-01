package com.watnapp.buddhawajana.core.data.repo

import com.watnapp.buddhawajana.core.common.runCatchingCancellable
import com.watnapp.buddhawajana.core.data.db.AudioDao
import com.watnapp.buddhawajana.core.data.mapper.toEntity
import com.watnapp.buddhawajana.core.data.mapper.toModel
import com.watnapp.buddhawajana.core.model.Audio
import com.watnapp.buddhawajana.core.network.AudioService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Cache-first repository for audios (per-album).
 *
 * Merge strategy (read-before-write):
 * - Fetch DTOs for the given albumId from the server.
 * - For each DTO, look up the existing entity by audioId.
 *   - If found: apply server content fields while KEEPING user-owned download
 *     columns (status, progress, requestId).
 *   - If not found: insert fresh entity (download columns default to 0).
 *
 * AudioRepository is not a generic Repository<T> because its stream/refresh are
 * scoped to a specific albumId.
 */
class AudioRepository(
    private val dao: AudioDao,
    private val service: AudioService,
) {
    fun stream(albumId: String): Flow<List<Audio>> =
        dao.stream(albumId.toLong()).map { entities ->
            entities.map { it.toModel() }
        }

    suspend fun refresh(albumId: String): Result<Unit> = runCatchingCancellable {
        val dtos = service.getAudios(albumId)
        val albumIdLong = albumId.toLong()
        val existing = dao.getAllOnce(albumIdLong).associateBy { it.audioId }

        val merged = dtos.map { dto ->
            val id = dto.id.toLong()
            val fresh = dto.toEntity(albumId)
            val current = existing[id]
            if (current != null) {
                // Preserve user-owned download state; apply server content fields.
                fresh.copy(
                    status = current.status,
                    progress = current.progress,
                    requestId = current.requestId,
                )
            } else {
                fresh
            }
        }
        dao.upsertAll(merged)
    }
}
