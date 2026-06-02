package com.watnapp.buddhawajana.feature.audio.player

import androidx.lifecycle.viewModelScope
import com.watnapp.buddhawajana.core.data.download.DownloadState
import com.watnapp.buddhawajana.core.data.repo.DownloadRepository
import com.watnapp.buddhawajana.core.data.repo.FavoriteRepository
import com.watnapp.buddhawajana.core.model.Album
import com.watnapp.buddhawajana.core.model.Audio
import com.watnapp.buddhawajana.core.model.Favorite
import com.watnapp.buddhawajana.core.player.NowPlaying
import com.watnapp.buddhawajana.core.player.PlaybackController
import com.watnapp.buddhawajana.core.player.SKIP_DELTA_MS
import com.watnapp.buddhawajana.core.player.SleepTimerState
import com.watnapp.buddhawajana.core.ui.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val controller: PlaybackController,
    private val favorites: FavoriteRepository,
    private val downloads: DownloadRepository,
) : BaseViewModel() {
    val nowPlaying = controller.nowPlaying
    val isPlaying = controller.isPlaying
    val positionMs = controller.positionMs
    val durationMs = controller.durationMs
    val speed = controller.speed
    val sleepTimer = controller.sleepTimer

    @OptIn(ExperimentalCoroutinesApi::class)
    val isFavorite: StateFlow<Boolean> =
        controller.nowPlaying.flatMapLatest { np ->
            if (np == null) flowOf(false) else favorites.isFavorite(np.audioId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val downloadState: StateFlow<DownloadState> =
        controller.nowPlaying.flatMapLatest { np ->
            if (np == null) flowOf(DownloadState.NotDownloaded) else downloads.state(np.audioId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadState.NotDownloaded)

    fun download() {
        val np = controller.nowPlaying.value ?: return
        if (np.url.isEmpty()) return
        downloads.enqueue(
            Audio(np.audioId, np.albumId, np.title, np.url),
            Album(np.albumId, np.album, np.artworkUrl, 0, 0),
        )
    }
    fun cancelDownload() { controller.nowPlaying.value?.let { downloads.cancel(it.audioId) } }
    fun deleteDownload() = viewModelScope.launch {
        controller.nowPlaying.value?.let { downloads.delete(it.audioId) }
    }

    fun playPause() = controller.playPause()
    fun seekTo(ms: Long) = controller.seekTo(ms)
    fun skipForward() = controller.skip(SKIP_DELTA_MS)
    fun skipBack() = controller.skip(-SKIP_DELTA_MS)
    fun next() = controller.next()
    fun prev() = controller.prev()
    fun setSpeed(rate: Float) = controller.setSpeed(rate)
    fun setSleepTimer(state: SleepTimerState) = controller.setSleepTimer(state)

    fun toggleFavorite() = viewModelScope.launch {
        val np = controller.nowPlaying.value ?: return@launch
        if (np.url.isEmpty()) return@launch
        favorites.toggle(np.toFavorite(System.currentTimeMillis()))
    }
}

internal fun NowPlaying.toFavorite(addedAt: Long) =
    Favorite(audioId, title, url, albumId, album, artworkUrl, addedAt)
