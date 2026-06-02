package com.watnapp.buddhawajana.core.data.repo

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.watnapp.buddhawajana.core.data.download.AudioFileStore
import com.watnapp.buddhawajana.core.data.download.DownloadDao
import com.watnapp.buddhawajana.core.data.download.DownloadState
import com.watnapp.buddhawajana.core.data.download.DownloadWorker
import com.watnapp.buddhawajana.core.data.download.mapDownloadState
import com.watnapp.buddhawajana.core.data.mapper.toModel
import com.watnapp.buddhawajana.core.model.Album
import com.watnapp.buddhawajana.core.model.Audio
import com.watnapp.buddhawajana.core.model.Download
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class DownloadRepository(
    private val context: Context,
    private val dao: DownloadDao,
    private val files: AudioFileStore,
) {
    private val wm get() = WorkManager.getInstance(context)

    val downloads: Flow<List<Download>> = dao.stream().map { list -> list.map { it.toModel() } }

    fun enqueue(audio: Audio, album: Album) {
        val req = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(DownloadWorker.inputData(audio, album))
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .build()
        wm.enqueueUniqueWork(DownloadWorker.workName(audio.id), ExistingWorkPolicy.KEEP, req)
    }

    suspend fun enqueueAll(album: Album, audios: List<Audio>) {
        audios.forEach { if (!dao.existsOnce(it.id)) enqueue(it, album) }
    }

    fun cancel(audioId: String) { wm.cancelUniqueWork(DownloadWorker.workName(audioId)) }

    suspend fun delete(audioId: String) {
        cancel(audioId)
        files.delete(audioId)
        dao.deleteById(audioId)
    }

    fun state(audioId: String): Flow<DownloadState> =
        combine(
            dao.existsFlow(audioId),
            wm.getWorkInfosForUniqueWorkFlow(DownloadWorker.workName(audioId)),
        ) { downloaded, infos ->
            val info = infos.firstOrNull()
            mapDownloadState(
                downloaded = downloaded,
                fileExists = files.exists(audioId),
                infoState = info?.state,
                fraction = info?.progress?.getFloat(DownloadWorker.KEY_FRACTION, 0f) ?: 0f,
            )
        }
}
