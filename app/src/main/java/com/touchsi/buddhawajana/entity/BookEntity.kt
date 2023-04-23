package com.touchsi.buddhawajana.entity

import androidx.room.*
import android.content.Context
import android.os.Environment
import androidx.room.OnConflictStrategy.IGNORE
import com.touchsi.buddhawajana.StorageHelper
import com.touchsi.buddhawajana.vm.DownloadableViewModel
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.*

@Entity(tableName = "book")
data class BookEntity(
    @PrimaryKey @ColumnInfo(name = "book_id") var bookId: Long = 0,
    @ColumnInfo(name = "progress") var progress: Int = 0,
    @ColumnInfo(name = "status") var status: Int = DownloadableViewModel.Status.IDLE.value
) {
    constructor() : this(0, 0, 0)

    @ColumnInfo(name = "title") lateinit var title: String
    @ColumnInfo(name = "cover_url") lateinit var coverUrl: String
    @ColumnInfo(name = "book_url") lateinit var bookUrl: String
    @ColumnInfo(name = "order_number") var position: Int = 0
    @ColumnInfo(name = "is_new") var new: Boolean = true
    @ColumnInfo(name = "is_updated") var updated: Boolean = false
    @ColumnInfo(name = "detail") lateinit var detail: String
    @ColumnInfo(name = "total_page") var pages: Int = 0
    @ColumnInfo(name = "producer") var producer: String = ""
    @ColumnInfo(name = "version") var version: String = ""
    @ColumnInfo(name = "request_id") var requestId: String = ""
    @ColumnInfo(name = "added_at") lateinit var addedAt: Date
    @ColumnInfo(name = "updated_at") lateinit var updatedAt: Date
}

fun BookEntity.getFile(context: Context): File {
    val storage = if (StorageHelper.isExternalStorageReadableAndWritable())
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) else context.filesDir.absoluteFile
    val rootDir = File(storage,
            "/buddhawajana/books/")
    if (!rootDir.exists()) {
        rootDir.mkdirs()
    }
    return File(rootDir, "${this.bookId}.pdf")
}

@Dao
interface BookDao {
    @Query("SELECT * from book WHERE book_id = :bookId")
    fun get(bookId: Long): BookEntity

    @Insert(onConflict = IGNORE)
    fun insert(bookEntity: BookEntity)

    @Update(onConflict = IGNORE)
    fun update(bookEntity: BookEntity)

    @Query("UPDATE book SET progress = :progress WHERE book_id = :bookId")
    fun updateProgress(bookId: Long, progress: Int)

    @Query("UPDATE book SET status = :status WHERE book_id = :bookId")
    fun updateStatus(bookId: Long, status: Int)

    @Delete
    fun delete(bookEntity: BookEntity)

    @Query("SELECT * from book ORDER BY order_number")
    fun list(): Flow<List<BookEntity>>
}

