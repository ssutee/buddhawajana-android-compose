package com.watnapp.buddhawajana.core.data.repo

import com.watnapp.buddhawajana.core.data.db.AlbumDao
import com.watnapp.buddhawajana.core.data.mapper.toEntity
import com.watnapp.buddhawajana.core.data.mapper.toModel
import com.watnapp.buddhawajana.core.model.Album
import com.watnapp.buddhawajana.core.network.AudioService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Cache-first repository for albums.
 *
 * Albums have no user-owned mutable state (no download columns), so a plain
 * replace upsert is safe.
 */
class AlbumRepository(
    private val dao: AlbumDao,
    private val service: AudioService,
) : Repository<Album> {

    override fun stream(): Flow<List<Album>> = dao.stream().map { entities ->
        entities.map { it.toModel() }
    }

    override suspend fun refresh(): Result<Unit> = runCatching {
        val dtos = service.getAlbums()
        val entities = dtos.map { it.toEntity() }
        dao.upsertAll(entities)
    }
}
