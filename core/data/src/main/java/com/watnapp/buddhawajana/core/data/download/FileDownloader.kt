package com.watnapp.buddhawajana.core.data.download

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream

class FileDownloader(
    private val openStream: (url: String) -> Pair<Long, InputStream>,
) {
    constructor(client: OkHttpClient) : this(openStream = { url ->
        val resp = client.newCall(Request.Builder().url(url).build()).execute()
        if (!resp.isSuccessful) { resp.close(); error("HTTP ${resp.code}") }
        val body = resp.body ?: error("empty body")
        body.contentLength() to body.byteStream()
    })

    fun download(url: String, dest: File): Flow<DownloadProgress> = flow {
        val tmp = File(dest.parentFile, dest.name + ".part")
        dest.parentFile?.mkdirs()
        try {
            val (total, input) = openStream(url)
            input.use { source ->
                tmp.outputStream().use { sink ->
                    val buf = ByteArray(64 * 1024)
                    var downloaded = 0L
                    while (true) {
                        val n = source.read(buf)
                        if (n < 0) break
                        sink.write(buf, 0, n)
                        downloaded += n
                        emit(DownloadProgress.Progress(downloaded, total))
                    }
                }
            }
            if (!tmp.renameTo(dest)) { tmp.copyTo(dest, overwrite = true); tmp.delete() }
            emit(DownloadProgress.Done)
        } catch (c: kotlin.coroutines.cancellation.CancellationException) {
            tmp.delete(); throw c
        } catch (e: Throwable) {
            tmp.delete(); emit(DownloadProgress.Failed(e))
        }
    }
}
