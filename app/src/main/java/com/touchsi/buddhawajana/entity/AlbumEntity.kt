package com.touchsi.buddhawajana.entity

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.*

@Entity(tableName = "album")
data class AlbumEntity(@PrimaryKey @ColumnInfo(name = "album_id") var albumId: Long = 0) {
    @Ignore
    constructor() : this(0)
    @ColumnInfo(name = "title") lateinit var title: String
    @ColumnInfo(name = "cover_url") lateinit var coverUrl: String
    @ColumnInfo(name = "view_count") var viewCount: Long = 0
    @ColumnInfo(name = "position") var position: Int = 0
    @ColumnInfo(name = "updated_at") lateinit var updatedAt: Date
    @ColumnInfo(name =  "item_count") var itemCount: Int = 0
}

@Dao
interface AlbumDao {

    @Query("SELECT * from album WHERE album_id = :albumId")
    fun get(albumId: Long): AlbumEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(albumEntity: AlbumEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(albumEntity: AlbumEntity)

    @Delete
    fun delete(albumEntity: AlbumEntity)

    @Query("SELECT * FROM album ORDER BY album_id DESC")
    fun list(): Flow<List<AlbumEntity>>
}