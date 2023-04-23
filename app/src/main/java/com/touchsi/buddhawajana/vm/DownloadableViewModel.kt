package com.touchsi.buddhawajana.vm

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.touchsi.buddhawajana.api.FileService
import com.touchsi.buddhawajana.repository.Repository
import com.touchsi.buddhawajana.ui.model.FileDownloadScreenState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import java.io.File

abstract class DownloadableViewModel<E, J>(open val repo: Repository<E, J>):
    BaseViewModel<E,J>(repo) {
    var state by mutableStateOf<FileDownloadScreenState>(FileDownloadScreenState.Idle)
        private set

    private val _service: FileService = FileService.create()

    abstract fun getFilePath(entity: E, context: Context): String
    abstract fun getFileUrl(entity: E): String
    abstract fun getFile(entity: E, context: Context): File
    abstract fun setProgress(entity: E, progress: Int)
    abstract fun setStatus(entity: E, status: Status)

    fun deleteFile(entity: E, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            setProgress(entity, 0)
            setStatus(entity, Status.IDLE)

            if (getFile(entity, context).exists()) {
                getFile(entity, context).delete()
            }
            repo.update(entity)
        }
    }

    suspend fun downloadFileFlow(entity: E, context: Context): Flow<Int> =
        flow {
            _service.downloadFile(getFileUrl(entity))
                .saveFile(getFilePath(entity, context))
                .collect { downloadState ->
                    state = when (downloadState) {
                        is DownloadState.Downloading -> {
                            setProgress(entity, downloadState.progress)
                            setStatus(entity, Status.DOWNLOADING)
                            repo.update(entity)
                            emit(downloadState.progress)
                            FileDownloadScreenState.Downloading(progress = downloadState.progress)
                        }
                        is DownloadState.Failed -> {
                            setStatus(entity, Status.FAILED)
                            repo.update(entity)
                            FileDownloadScreenState.Failed(error = downloadState.error)
                        }
                        DownloadState.Finished -> {
                            setStatus(entity, Status.FINISHED)
                            repo.update(entity)
                            FileDownloadScreenState.Downloaded
                        }
                    }
                }
        }

    fun downloadFile(entity: E, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _service.downloadFile(getFileUrl(entity))
                .saveFile(getFilePath(entity, context))
                .collect { downloadState ->
                    state = when (downloadState) {
                        is DownloadState.Downloading -> {
                            setProgress(entity, downloadState.progress)
                            setStatus(entity, Status.DOWNLOADING)
                            repo.update(entity)
                            FileDownloadScreenState.Downloading(progress = downloadState.progress)
                        }
                        is DownloadState.Failed -> {
                            setStatus(entity, Status.FAILED)
                            repo.update(entity)
                            FileDownloadScreenState.Failed(error = downloadState.error)
                        }
                        DownloadState.Finished -> {
                            setStatus(entity, Status.FINISHED)
                            repo.update(entity)
                            FileDownloadScreenState.Downloaded
                        }
                    }
                }
        }
    }

    enum class Status(val value: Int) {
        IDLE(0),
        DOWNLOADING(1),
        FINISHED(2),
        FAILED(3);

        companion object {
            infix fun from(value: Int): Status? = Status.values().firstOrNull { it.value == value }
            private val map = Status.values().associateBy { it.value }
            operator fun get(value: Int) = map[value]
        }
    }

    sealed class DownloadState {
        data class Downloading(val progress: Int) : DownloadState()
        object Finished : DownloadState()
        data class Failed(val error: Throwable? = null) : DownloadState()
    }

    private fun ResponseBody.saveFile(filePath: String): Flow<DownloadState> {
        return flow {
            emit(DownloadState.Downloading(0))
            val destinationFile = File(filePath)
            try {
                byteStream().use { inputStream ->
                    destinationFile.outputStream().use { outputStream ->
                        val totalBytes = contentLength()
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var progressBytes = 0L

                        var bytes = inputStream.read(buffer)
                        while (bytes >= 0) {
                            outputStream.write(buffer, 0, bytes)
                            progressBytes += bytes
                            bytes = inputStream.read(buffer)
                            emit(DownloadState.Downloading(((progressBytes * 100) / totalBytes).toInt()))
                        }
                    }
                }
                emit(DownloadState.Finished)
            } catch (e: Exception) {
                Log.d("DownloadError", e.toString())
                emit(DownloadState.Failed(e))
            }
        }.flowOn(Dispatchers.IO).distinctUntilChanged()
    }
}