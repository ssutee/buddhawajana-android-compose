package com.watnapp.buddhawajana.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioDao {
    @Query("SELECT * FROM audio WHERE album_id = :albumId ORDER BY audio_id")
    fun stream(albumId: Long): Flow<List<AudioEntity>>

    @Query("SELECT * FROM audio WHERE album_id = :albumId")
    suspend fun getAllOnce(albumId: Long): List<AudioEntity>

    @Insert(onConflict = REPLACE)
    suspend fun upsertAll(items: List<AudioEntity>)

    @Query("SELECT COUNT(*) FROM audio")
    suspend fun count(): Int

    @Query("UPDATE audio SET duration_ms = :durationMs, size_bytes = :sizeBytes WHERE audio_id = :audioId")
    suspend fun updateMetadata(audioId: Long, durationMs: Long?, sizeBytes: Long?)

    /** Reconcile: drop cached audios in [albumId] the server no longer lists. */
    @Query("DELETE FROM audio WHERE album_id = :albumId AND audio_id NOT IN (:ids)")
    suspend fun deleteNotInAlbum(albumId: Long, ids: List<Long>)
}
