package com.watnapp.buddhawajana.vm

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.watnapp.buddhawajana.api.AudioJson
import com.watnapp.buddhawajana.entity.AudioEntity
import com.watnapp.buddhawajana.entity.getFile
import com.watnapp.buddhawajana.repository.AudioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.net.URI
import java.net.URL

class AudioViewModel(override val repo: AudioRepository): DownloadableViewModel<AudioEntity, AudioJson>(repo) {

    val selectedIndex = MutableStateFlow(-1)
    val selectedId = MutableStateFlow(0L)

    fun getItems(id: Long): Flow<List<AudioEntity>> = repo.getItems(id)

    fun refresh(owner: LifecycleOwner, id: Long) {
        viewModelScope.launch {
            isRefreshing.emit(true)
            viewModelScope.launch(Dispatchers.IO) {
                repo.refreshFromServer(owner, id) {
                    viewModelScope.launch {
                        isRefreshing.emit(false)
                    }
                }
            }
        }
    }

    var albumId: Long
        get() = repo.albumId
        set(value) { repo.albumId = value }

    override fun getFilePath(entity: AudioEntity, context: Context): String
        = entity.getFile(context).absolutePath

    override fun getFileUrl(entity: AudioEntity): String {
        var url = URL(entity.url)
        val uri = URI(
            url.protocol,
            url.userInfo,
            url.host,
            url.port,
            url.path,
            url.query,
            url.ref
        )
        url = uri.toURL()
        return url.toString()
    }

    override fun getFile(entity: AudioEntity, context: Context): File
        = entity.getFile(context)

    override fun setProgress(entity: AudioEntity, progress: Int) {
        entity.progress = progress
    }

    override fun setStatus(entity: AudioEntity, status: Status) {
        entity.status = status.value
    }

}