package com.watnapp.buddhawajana.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "audio",
    indices = [Index("audio_id"), Index("album_id")],
    foreignKeys = [
        ForeignKey(
            entity = AlbumEntity::class,
            parentColumns = ["album_id"],
            childColumns = ["album_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ]
)
data class AudioEntity(
    @PrimaryKey @ColumnInfo(name = "audio_id") var audioId: Long = 0,
    @ColumnInfo(name = "title") var title: String = "",
    @ColumnInfo(name = "url") var url: String = "",
    @ColumnInfo(name = "album_id") var albumId: Long = 0,
    @ColumnInfo(name = "updated_at") var updatedAt: Date = Date(0),
    @ColumnInfo(name = "status") var status: Int = 0,
    @ColumnInfo(name = "request_id") var requestId: Int = 0,
    @ColumnInfo(name = "progress") var progress: Int = 0,
    @ColumnInfo(name = "duration_ms") var durationMs: Long? = null,
    @ColumnInfo(name = "size_bytes") var sizeBytes: Long? = null,
) {
    @Ignore
    constructor() : this(0)
}
