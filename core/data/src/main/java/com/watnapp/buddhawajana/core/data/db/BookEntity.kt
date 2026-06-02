package com.watnapp.buddhawajana.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "book")
data class BookEntity(
    @PrimaryKey @ColumnInfo(name = "book_id") var bookId: Long = 0,
    @ColumnInfo(name = "progress") var progress: Int = 0,
    @ColumnInfo(name = "status") var status: Int = 0,
    @ColumnInfo(name = "title") var title: String = "",
    @ColumnInfo(name = "cover_url") var coverUrl: String = "",
    @ColumnInfo(name = "book_url") var bookUrl: String = "",
    @ColumnInfo(name = "order_number") var position: Int = 0,
    @ColumnInfo(name = "is_new") var new: Boolean = true,
    @ColumnInfo(name = "is_updated") var updated: Boolean = false,
    @ColumnInfo(name = "detail") var detail: String = "",
    @ColumnInfo(name = "total_page") var pages: Int = 0,
    @ColumnInfo(name = "producer") var producer: String = "",
    @ColumnInfo(name = "category") var category: String = "",
    @ColumnInfo(name = "version") var version: String = "",
    @ColumnInfo(name = "request_id") var requestId: String = "",
    @ColumnInfo(name = "added_at") var addedAt: Date = Date(0),
    @ColumnInfo(name = "updated_at") var updatedAt: Date = Date(0),
) {
    @Ignore
    constructor() : this(0, 0, 0)
}
