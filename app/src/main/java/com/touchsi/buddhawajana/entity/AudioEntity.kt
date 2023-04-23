package com.touchsi.buddhawajana.entity

import android.content.Context
import android.os.Environment
import androidx.room.*
import com.touchsi.buddhawajana.StorageHelper
import com.touchsi.buddhawajana.vm.DownloadableViewModel
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.*

@Entity(tableName = "audio",
    indices = [Index("audio_id"), Index("album_id")],
    foreignKeys = [(ForeignKey(entity = AlbumEntity::class,
        parentColumns = arrayOf("album_id"),
        childColumns = arrayOf("album_id"),
        onDelete = ForeignKey.CASCADE))])
data class AudioEntity(@PrimaryKey
                       @ColumnInfo(name = "audio_id") var audioId: Long = 0) {
    @Ignore
    constructor(): this(0)
    @ColumnInfo(name = "title") lateinit var title: String
    @ColumnInfo(name = "url") lateinit var url: String
    @ColumnInfo(name = "album_id") var albumId: Long = 0
    @ColumnInfo(name = "updated_at") lateinit var updatedAt: Date
    @ColumnInfo(name = "status") var status: Int = DownloadableViewModel.Status.IDLE.value
    @ColumnInfo(name = "request_id") var requestId: Int = 0
    @ColumnInfo(name = "progress") var progress: Int = 0
}

fun AudioEntity.getFile(context: Context): File {
    var storage = if (StorageHelper.isExternalStorageReadableAndWritable())
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) else context.filesDir.absoluteFile
    var rootDir = File(storage,
        "/buddhawajana/audios/${this.albumId}/")
    if (!rootDir.exists()) {
        rootDir.mkdirs()
    }
    return File(rootDir, "${this.audioId}.mp3")
}

@Dao
interface AudioDao {
    @Query("SELECT * from audio WHERE audio_id = :audioId")
    fun get(audioId: Long): AudioEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: AudioEntity)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(entity: AudioEntity)

    @Query("UPDATE audio SET progress = :progress WHERE request_id = :requestId")
    fun updateProgress(requestId: Int, progress: Int)

    @Query("UPDATE audio SET status = :status WHERE request_id = :requestId")
    fun updateStatus(requestId: Int, status: Int)

    @Delete
    fun delete(entity: AudioEntity)

    @Query("SELECT * from audio WHERE album_id = :albumId ORDER BY audio_id")
    fun list(albumId: Long): Flow<List<AudioEntity>>

    @Query("SELECT * from audio WHERE album_id = :albumId ORDER BY audio_id")
    fun simpleList(albumId: Long): List<AudioEntity>
}