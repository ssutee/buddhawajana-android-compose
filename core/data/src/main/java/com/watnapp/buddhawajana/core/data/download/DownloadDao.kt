package com.watnapp.buddhawajana.core.data.download

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download ORDER BY completed_at DESC")
    fun stream(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM download WHERE audio_id = :id")
    suspend fun get(id: String): DownloadEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM download WHERE audio_id = :id)")
    fun existsFlow(id: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM download WHERE audio_id = :id)")
    suspend fun existsOnce(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(e: DownloadEntity)

    @Query("DELETE FROM download WHERE audio_id = :id")
    suspend fun deleteById(id: String)
}
