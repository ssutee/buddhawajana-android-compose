package com.watnapp.buddhawajana.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM book ORDER BY order_number")
    fun stream(): Flow<List<BookEntity>>

    @Insert(onConflict = REPLACE)
    suspend fun upsertAll(items: List<BookEntity>)

    @Query("SELECT COUNT(*) FROM book")
    suspend fun count(): Int
}
