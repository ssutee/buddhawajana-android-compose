package com.watnapp.buddhawajana.feature.audio.player

import androidx.lifecycle.viewModelScope
import com.watnapp.buddhawajana.core.data.repo.FavoriteRepository
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
