package com.watnapp.buddhawajana.core.data.repo

import com.watnapp.buddhawajana.core.data.db.FavoriteDao
import com.watnapp.buddhawajana.core.data.mapper.toEntity
import com.watnapp.buddhawajana.core.data.mapper.toModel
import com.watnapp.buddhawajana.core.model.Favorite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoriteRepository(private val dao: FavoriteDao) {
    val favorites: Flow<List<Favorite>> = dao.stream().map { list -> list.map { it.toModel() } }
    fun isFavorite(audioId: String): Flow<Boolean> = dao.isFavorite(audioId)
    suspend fun add(f: Favorite) = dao.insert(f.toEntity())
    suspend fun remove(audioId: String) = dao.deleteById(audioId)
    suspend fun toggle(f: Favorite) {
        if (dao.exists(f.audioId)) dao.deleteById(f.audioId) else dao.insert(f.toEntity())
    }
}
