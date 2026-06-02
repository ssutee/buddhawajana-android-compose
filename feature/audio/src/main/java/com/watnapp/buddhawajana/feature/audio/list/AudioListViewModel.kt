package com.watnapp.buddhawajana.feature.audio.list

import androidx.lifecycle.viewModelScope
import com.watnapp.buddhawajana.core.data.download.DownloadState
import com.watnapp.buddhawajana.core.data.repo.AudioRepository
import com.watnapp.buddhawajana.core.data.repo.DownloadRepository
import com.watnapp.buddhawajana.core.model.Album
import com.watnapp.buddhawajana.core.model.Audio
import com.watnapp.buddhawajana.core.player.PlaybackController
import com.watnapp.buddhawajana.core.ui.BaseViewModel
import com.watnapp.buddhawajana.core.ui.state.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AudioListViewModel(
    private val albumId: String,
    private val repo: AudioRepository,
    private val controller: PlaybackController,
    private val downloads: DownloadRepository,
) : BaseViewModel() {

    private val query = MutableStateFlow("")
    val queryState: StateFlow<String> = query.asStateFlow()
    fun onSearch(q: String) { query.value = q }

    val nowPlaying = controller.nowPlaying

    val state: StateFlow<UiState<List<Audio>>> =
        combine(repo.stream(albumId), query) { audios, q ->
            val filtered = if (q.isBlank()) audios
                else audios.filter { it.title.contains(q.trim(), ignoreCase = true) }
            if (filtered.isEmpty()) UiState.Empty else UiState.Content(filtered)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    private val probing = mutableSetOf<String>()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        repo.refresh(albumId).onFailure { emitMessage("รีเฟรชล้มเหลว") }
    }

    /** Called when a row first appears; probes duration/size once per audio. */
    fun onRowVisible(audio: Audio) {
        if (audio.durationMs != null && audio.sizeBytes != null) return
        if (!probing.add(audio.id)) return
        viewModelScope.launch { repo.ensureMetadata(audio) }
    }

    /** Start the album as a queue at [index]. */
    fun play(album: Album, audios: List<Audio>, index: Int) {
        controller.setQueue(album, audios, index)
    }

    fun downloadState(audioId: String): Flow<DownloadState> = downloads.state(audioId)
    fun download(audio: Audio, album: Album) = downloads.enqueue(audio, album)
    fun cancelDownload(audioId: String) = downloads.cancel(audioId)
    fun deleteDownload(audioId: String) = viewModelScope.launch { downloads.delete(audioId) }
    fun downloadAll(album: Album, audios: List<Audio>) = viewModelScope.launch { downloads.enqueueAll(album, audios) }
}
