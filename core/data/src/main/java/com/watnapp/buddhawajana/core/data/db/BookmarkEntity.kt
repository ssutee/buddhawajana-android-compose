package com.watnapp.buddhawajana.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "bookmark", indices = [Index("book_id")])
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "book_id") val bookId: Long,
    @ColumnInfo(name = "page") val page: Int,
    @ColumnInfo(name = "note") val note: String,
    @ColumnInfo(name = "added_at") val addedAt: Long,
)
