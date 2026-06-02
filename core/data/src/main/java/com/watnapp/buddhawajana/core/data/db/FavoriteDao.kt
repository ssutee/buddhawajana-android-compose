package com.watnapp.buddhawajana.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite ORDER BY added_at DESC")
    fun stream(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite WHERE audio_id = :audioId)")
    fun isFavorite(audioId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite WHERE audio_id = :audioId)")
    suspend fun exists(audioId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(e: FavoriteEntity)

    @Query("DELETE FROM favorite WHERE audio_id = :audioId")
    suspend fun deleteById(audioId: String)
}
