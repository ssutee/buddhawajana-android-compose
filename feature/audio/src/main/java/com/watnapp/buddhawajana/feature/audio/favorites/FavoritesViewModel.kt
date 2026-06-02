package com.watnapp.buddhawajana.feature.audio.favorites

import androidx.lifecycle.viewModelScope
import com.watnapp.buddhawajana.core.data.repo.FavoriteRepository
import com.watnapp.buddhawajana.core.model.Album
import com.watnapp.buddhawajana.core.model.Audio
import com.watnapp.buddhawajana.core.model.Favorite
import com.watnapp.buddhawajana.core.player.PlaybackController
import com.watnapp.buddhawajana.core.ui.BaseViewModel
import com.watnapp.buddhawajana.core.ui.state.UiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val favorites: FavoriteRepository,
    private val controller: PlaybackController,
) : BaseViewModel() {

    val state: StateFlow<UiState<List<Favorite>>> =
        favorites.favorites
            .map { if (it.isEmpty()) UiState.Empty else UiState.Content(it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    fun play(f: Favorite) {
        controller.setQueue(
            Album(f.albumId, f.albumTitle, f.coverUrl, 0, 0),
            listOf(Audio(f.audioId, f.albumId, f.title, f.url)),
            0,
        )
    }

    fun remove(audioId: String) = viewModelScope.launch { favorites.remove(audioId) }
}
