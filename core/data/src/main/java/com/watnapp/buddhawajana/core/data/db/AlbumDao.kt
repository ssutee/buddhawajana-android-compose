package com.watnapp.buddhawajana.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {
    @Query("SELECT * FROM album ORDER BY album_id DESC")
    fun stream(): Flow<List<AlbumEntity>>

    @Insert(onConflict = REPLACE)
    suspend fun upsertAll(items: List<AlbumEntity>)

    /** Reconcile: drop cached albums the server no longer lists. Cascades to their audios. */
    @Query("DELETE FROM album WHERE album_id NOT IN (:ids)")
    suspend fun deleteNotIn(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM album")
    suspend fun count(): Int
}
