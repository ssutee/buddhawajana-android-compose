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

    @Query("SELECT * FROM book")
    suspend fun getAllOnce(): List<BookEntity>

    @Insert(onConflict = REPLACE)
    suspend fun upsertAll(items: List<BookEntity>)

    /** Reconcile: drop cached books the server no longer lists. */
    @Query("DELETE FROM book WHERE book_id NOT IN (:ids)")
    suspend fun deleteNotIn(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM book")
    suspend fun count(): Int
}
