package com.touchsi.buddhawajana.repository

import com.touchsi.buddhawajana.AppDatabaseProvider
import com.touchsi.buddhawajana.api.AlbumJson
import com.touchsi.buddhawajana.api.AudioService
import com.touchsi.buddhawajana.api.toEntity
import com.touchsi.buddhawajana.entity.AlbumDao
import com.touchsi.buddhawajana.entity.AlbumEntity
import io.reactivex.Observable
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class AlbumRepository(databaseProvider: AppDatabaseProvider):
    Repository<AlbumEntity, AlbumJson>() {
    private var dao: AlbumDao = databaseProvider.getDatabase().albumDao()
    private val audioService by lazy { AudioService.create() }

    override fun getItemsFromService(): Observable<List<AlbumJson>> = audioService.getAllAlbums()
    override fun getEntityId(entity: AlbumEntity): Long = entity.albumId
    override fun getJsonId(json: AlbumJson): Long = json.id
    override fun convertJsonToEntity(json: AlbumJson): AlbumEntity = json.toEntity()

    override suspend fun get(id: Long): AlbumEntity = dao.get(id)
    override fun getItems(id: Long): Flow<List<AlbumEntity>> = dao.list()
    override suspend fun insert(entity: AlbumEntity) = dao.insert(entity)
    override suspend fun delete(entity: AlbumEntity) = dao.delete(entity)
    override suspend fun update(entity: AlbumEntity) = dao.update(entity)
    override fun shouldBeUpdated(entity: AlbumEntity, json: AlbumJson): Boolean {
        val updatedAt = SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.UK).parse(json.created)
        if (entity.updatedAt.before(updatedAt) || entity.title != json.album_name
            || entity.coverUrl != json.album_cover || entity.viewCount != json.count) {
            return true
        }
        return false
    }
}