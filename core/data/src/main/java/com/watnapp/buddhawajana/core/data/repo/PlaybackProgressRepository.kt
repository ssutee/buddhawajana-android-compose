package com.watnapp.buddhawajana.core.data.repo

import com.watnapp.buddhawajana.core.data.db.PlaybackProgressDao
import com.watnapp.buddhawajana.core.data.db.PlaybackProgressEntity
import com.watnapp.buddhawajana.core.data.mapper.toModel
import com.watnapp.buddhawajana.core.model.PlaybackProgress

class PlaybackProgressRepository(private val dao: PlaybackProgressDao) {
    suspend fun get(audioId: String): PlaybackProgress? = dao.get(audioId)?.toModel()
    suspend fun save(audioId: String, positionMs: Long, now: Long = System.currentTimeMillis()) {
        dao.upsert(PlaybackProgressEntity(audioId = audioId, positionMs = positionMs, updatedAt = now))
    }
}
