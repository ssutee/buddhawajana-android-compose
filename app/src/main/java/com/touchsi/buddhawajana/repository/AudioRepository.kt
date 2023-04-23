package com.touchsi.buddhawajana.repository

import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.touchsi.buddhawajana.AppDatabaseProvider
import com.touchsi.buddhawajana.api.AudioJson
import com.touchsi.buddhawajana.api.AudioService
import com.touchsi.buddhawajana.api.toEntity
import com.touchsi.buddhawajana.entity.AudioDao
import com.touchsi.buddhawajana.entity.AudioEntity
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class AudioRepository(databaseProvider: AppDatabaseProvider):
    Repository<AudioEntity, AudioJson>() {
    private var dao: AudioDao = databaseProvider.getDatabase().audioDao()
    private val audioService by lazy { AudioService.create() }
    var albumId: Long = 0

    override fun getItemsFromService(): Observable<List<AudioJson>> =
        audioService.getAllAudios(albumId)

    private fun getItemsFromService(id: Long): Observable<List<AudioJson>> {
        albumId = id
        return audioService.getAllAudios(id)
    }


    override fun getEntityId(entity: AudioEntity): Long = entity.audioId
    override fun getJsonId(json: AudioJson): Long = json.id
    override fun convertJsonToEntity(json: AudioJson): AudioEntity {
        var entity = json.toEntity()
        entity.albumId = albumId
        return entity
    }

    override suspend fun get(id: Long): AudioEntity = dao.get(id)
    override suspend fun insert(entity: AudioEntity) = dao.insert(entity)
    override suspend fun delete(entity: AudioEntity) = dao.delete(entity)
    override suspend fun update(entity: AudioEntity) = dao.update(entity)
    override fun shouldBeUpdated(entity: AudioEntity, json: AudioJson): Boolean {
        if (entity.url != json.file_url || entity.title != json.name) {
            return true
        }
        return false
    }
    override fun getItems(id: Long): Flow<List<AudioEntity>> = dao.list(id)
    fun getSimpleItems(id: Long): List<AudioEntity> = dao.simpleList(id)

    fun refreshFromServer(owner: LifecycleOwner,
                          id: Long,
                          onComplete:() -> Unit): Disposable {
        return getItemsFromService(id)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result ->
                    owner.lifecycleScope.launch {
                        updateItems(result, id = id)
                    }
                },
                { error ->
                    error.message?.let { Log.d("error", it) }
                },
                { onComplete.invoke() }
            )
    }


}