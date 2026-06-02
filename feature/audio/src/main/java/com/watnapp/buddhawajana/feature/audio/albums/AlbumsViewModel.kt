package com.watnapp.buddhawajana.feature.audio.albums

import androidx.lifecycle.viewModelScope
import com.watnapp.buddhawajana.core.data.repo.AlbumRepository
import com.watnapp.buddhawajana.core.data.repo.FavoriteRepository
import com.watnapp.buddhawajana.core.model.Album
import com.watnapp.buddhawajana.core.ui.BaseViewModel
import com.watnapp.buddhawajana.core.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlbumsViewModel(
    private val repo: AlbumRepository,
    favorites: FavoriteRepository,
) : BaseViewModel() {
    private val query = MutableStateFlow("")
    val queryState: StateFlow<String> = query.asStateFlow()
    fun onSearch(q: String) { query.value = q }

    val state: StateFlow<UiState<List<Album>>> =
        combine(repo.stream(), query) { albums, q ->
            val filtered = if (q.isBlank()) albums
                else albums.filter { it.title.contains(q.trim(), ignoreCase = true) }
            if (filtered.isEmpty()) UiState.Empty else UiState.Content(filtered)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    val favoriteCount: StateFlow<Int> =
        favorites.favorites.map { it.size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        repo.refresh().onFailure { emitMessage("รีเฟรชล้มเหลว") }
    }
}
