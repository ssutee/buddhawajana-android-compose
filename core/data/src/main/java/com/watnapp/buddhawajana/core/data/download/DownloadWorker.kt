package com.watnapp.buddhawajana.core.data.download

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.watnapp.buddhawajana.core.model.Album
import com.watnapp.buddhawajana.core.model.Audio
import okhttp3.OkHttpClient
import org.koin.core.context.GlobalContext
import kotlin.coroutines.cancellation.CancellationException

/** Streams one audio to AudioFileStore and records a DownloadEntity on success. */
class DownloadWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val audioId = inputData.getString(KEY_AUDIO_ID) ?: return Result.failure()
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val koin = GlobalContext.get()
        val client = koin.get<OkHttpClient>()
        val dao = koin.get<DownloadDao>()
        val files = koin.get<AudioFileStore>()
        val title = inputData.getString(KEY_TITLE) ?: ""
        return try {
            setForeground(foregroundInfo(audioId, title))
            FileDownloader(client).download(url, files.file(audioId)).collect { p ->
                when (p) {
                    is DownloadProgress.Progress -> setProgress(workDataOf(KEY_FRACTION to p.fraction))
                    is DownloadProgress.Failed -> throw p.error
                    DownloadProgress.Done -> {}
                }
            }
            val f = files.file(audioId)
            dao.insert(
                DownloadEntity(
                    audioId = audioId, title = title, url = url,
                    albumId = inputData.getString(KEY_ALBUM_ID) ?: "",
                    albumTitle = inputData.getString(KEY_ALBUM_TITLE) ?: "",
                    coverUrl = inputData.getString(KEY_COVER),
                    fileName = f.name, sizeBytes = f.length(),
                    completedAt = System.currentTimeMillis(),
                ),
            )
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun foregroundInfo(audioId: String, title: String): ForegroundInfo {
        val notif = NotificationCompat.Builder(applicationContext, CHANNEL)
            .setContentTitle("กำลังดาวน์โหลด")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()
        val id = audioId.hashCode()
        return if (Build.VERSION.SDK_INT >= 34) {
            ForegroundInfo(id, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(id, notif)
        }
    }

    companion object {
        const val CHANNEL = "downloads"
        const val KEY_AUDIO_ID = "audioId"
        const val KEY_URL = "url"
        const val KEY_TITLE = "title"
        const val KEY_ALBUM_ID = "albumId"
        const val KEY_ALBUM_TITLE = "albumTitle"
        const val KEY_COVER = "cover"
        const val KEY_FRACTION = "fraction"

        fun inputData(audio: Audio, album: Album) = workDataOf(
            KEY_AUDIO_ID to audio.id,
            KEY_URL to audio.url,
            KEY_TITLE to audio.title,
            KEY_ALBUM_ID to album.id,
            KEY_ALBUM_TITLE to album.title,
            KEY_COVER to album.coverUrl,
        )

        fun workName(audioId: String) = "download_$audioId"
    }
}
