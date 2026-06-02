package com.watnapp.buddhawajana.core.data.download

sealed interface DownloadProgress {
    data class Progress(val bytesDownloaded: Long, val bytesTotal: Long) : DownloadProgress {
        val fraction: Float get() = if (bytesTotal > 0) bytesDownloaded.toFloat() / bytesTotal else 0f
    }
    data object Done : DownloadProgress
    data class Failed(val error: Throwable) : DownloadProgress
}
