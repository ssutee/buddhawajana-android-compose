package com.watnapp.buddhawajana.feature.audio.downloads

import androidx.lifecycle.viewModelScope
import com.watnapp.buddhawajana.core.data.repo.DownloadRepository
import com.watnapp.buddhawajana.core.model.Album
import com.watnapp.buddhawajana.core.model.Audio
import com.watnapp.buddhawajana.core.model.Download
import com.watnapp.buddhawajana.core.player.PlaybackController
import com.watnapp.buddhawajana.core.ui.BaseViewModel
import com.watnapp.buddhawajana.core.ui.state.UiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DownloadsViewModel(
    private val downloads: DownloadRepository,
    private val controller: PlaybackController,
) : BaseViewModel() {

    val state: StateFlow<UiState<List<Download>>> =
        downloads.downloads
            .map { if (it.isEmpty()) UiState.Empty else UiState.Content(it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    fun play(d: Download) {
        controller.setQueue(
            Album(d.albumId, d.albumTitle, d.coverUrl, 0, 0),
            listOf(Audio(d.audioId, d.albumId, d.title, d.url)),
            0,
        )
    }

    fun delete(audioId: String) = viewModelScope.launch { downloads.delete(audioId) }
}
