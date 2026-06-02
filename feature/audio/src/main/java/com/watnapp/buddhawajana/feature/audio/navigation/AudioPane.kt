package com.watnapp.buddhawajana.feature.audio.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.watnapp.buddhawajana.core.model.Album
import com.watnapp.buddhawajana.core.model.Audio
import com.watnapp.buddhawajana.core.ui.state.UiState
import com.watnapp.buddhawajana.feature.audio.albums.AlbumsScreen
import com.watnapp.buddhawajana.feature.audio.albums.AlbumsViewModel
import com.watnapp.buddhawajana.feature.audio.list.AudioListScreen
import com.watnapp.buddhawajana.feature.audio.list.AudioListViewModel
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable private data object AlbumsRoute
@Serializable private data class AudioListRoute(val albumId: String, val albumTitle: String)

/**
 * Browse pane for the AUDIO tab. Hosts albums → audio-list internally so the bottom
 * nav + mini-player stay visible. [onOpenPlayer] asks the app NavHost to push the
 * full-screen player after a queue has been set.
 */
@Composable
fun AudioPane(onOpenPlayer: () -> Unit, modifier: Modifier = Modifier) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = AlbumsRoute, modifier = modifier) {
        composable<AlbumsRoute> {
            val vm: AlbumsViewModel = koinViewModel()
            val state by vm.state.collectAsState()
            val query by vm.queryState.collectAsState()
            AlbumsScreen(
                state = state,
                query = query,
                onSearch = vm::onSearch,
                onRefresh = vm::refresh,
                onOpenAlbum = { album -> nav.navigate(AudioListRoute(album.id, album.title)) },
            )
        }
        composable<AudioListRoute> { entry ->
            val route = entry.toRoute<AudioListRoute>()
            val vm: AudioListViewModel = koinViewModel { parametersOf(route.albumId) }
            val state by vm.state.collectAsState()
            val query by vm.queryState.collectAsState()
            val now by vm.nowPlaying.collectAsState()
            val audios: List<Audio> = when (val s = state) {
                is UiState.Content -> s.data
                else -> emptyList()
            }
            AudioListScreen(
                state = state,
                query = query,
                playingAudioId = now?.audioId,
                onSearch = vm::onSearch,
                onRefresh = vm::refresh,
                onRowVisible = vm::onRowVisible,
                onPlay = { index ->
                    if (audios.isNotEmpty()) {
                        vm.play(Album(route.albumId, route.albumTitle, now?.artworkUrl, audios.size, 0), audios, index)
                        onOpenPlayer()
                    }
                },
            )
        }
    }
}
