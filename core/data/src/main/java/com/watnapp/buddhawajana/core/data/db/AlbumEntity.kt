package com.watnapp.buddhawajana.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "album")
data class AlbumEntity(
    @PrimaryKey @ColumnInfo(name = "album_id") var albumId: Long = 0,
    @ColumnInfo(name = "title") var title: String = "",
    @ColumnInfo(name = "cover_url") var coverUrl: String = "",
    @ColumnInfo(name = "view_count") var viewCount: Long = 0,
    @ColumnInfo(name = "position") var position: Int = 0,
    @ColumnInfo(name = "updated_at") var updatedAt: Date = Date(0),
    @ColumnInfo(name = "item_count") var itemCount: Int = 0,
) {
    @Ignore
    constructor() : this(0)
}
