package com.touchsi.buddhawajana

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class DownloadWorker(appContext: Context, workerParams:
    WorkerParameters): CoroutineWorker(appContext, workerParams) {

    companion object {
        const val Progress = "Progress"
        const val Status = "Status"
    }

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            val pdfUriInput = inputData.getString("PDF_URI")
                ?: return@withContext Result.failure()
            val bookFile = inputData.getString("BOOK_FILE")?.let { File(it) }
                ?: return@withContext Result.failure()

            if (bookFile.exists()) {
                bookFile.delete()
            }

            val url = URL(pdfUriInput)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept-Encoding", "identity")
            connection.connect()

            if (connection.responseCode in 200 .. 299) {
                val fileSize = connection.contentLength
                val inputStream = connection.inputStream
                val outputStream = FileOutputStream(bookFile)

                var bytesCopied = 0L
                var buffer = ByteArray(1024)
                var bytes = inputStream.read(buffer)
                while (bytes >= 0) {
                    bytesCopied += bytes
                    val progress = (bytesCopied.toFloat() / fileSize.toFloat() * 100).toInt()
                    setProgress(workDataOf(Progress to progress))
                    outputStream.write(buffer, 0, bytes)
                    bytes = inputStream.read(buffer)
                }
                outputStream.close()
                inputStream.close()
            } else {
                return@withContext Result.failure()
            }
            return@withContext Result.success()
        }
    }
}