package com.watnapp.buddhawajana.vm

import android.content.Context
import com.watnapp.buddhawajana.api.BookJson
import com.watnapp.buddhawajana.entity.BookEntity
import com.watnapp.buddhawajana.entity.getFile
import com.watnapp.buddhawajana.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.net.URI

class BookViewModel(repo: BookRepository): DownloadableViewModel<BookEntity, BookJson>(repo) {
    val firstLoading = MutableStateFlow(true)

    override fun getFilePath(entity: BookEntity, context: Context): String
        = entity.getFile(context).absolutePath

    override fun getFileUrl(entity: BookEntity): String
        = URI(entity.bookUrl).path

    override fun getFile(entity: BookEntity, context: Context): File
        = entity.getFile(context)

    override fun setProgress(entity: BookEntity, progress: Int) {
        entity.progress = progress
    }

    override fun setStatus(entity: BookEntity, status: Status) {
        entity.status = status.value
    }
}