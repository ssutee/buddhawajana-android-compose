package com.watnapp.buddhawajana.core.data.download

import android.content.Context
import android.os.Environment
import java.io.File

class BookFileStore(private val context: Context) {
    private fun dir(): File =
        File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "buddhawajana/books").apply { mkdirs() }
    fun file(bookId: Long): File = File(dir(), "$bookId.pdf")
    fun exists(bookId: Long): Boolean = file(bookId).let { it.exists() && it.length() > 0 }
    fun delete(bookId: Long): Boolean = file(bookId).delete()
}
