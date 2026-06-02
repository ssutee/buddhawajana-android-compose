package com.watnapp.buddhawajana.feature.audio

import com.watnapp.buddhawajana.feature.audio.albums.AlbumsViewModel
import com.watnapp.buddhawajana.feature.audio.favorites.FavoritesViewModel
import com.watnapp.buddhawajana.feature.audio.list.AudioListViewModel
import com.watnapp.buddhawajana.feature.audio.player.PlayerViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val audioModule = module {
    viewModel { AlbumsViewModel(get(), get()) }
    viewModel { (albumId: String) -> AudioListViewModel(albumId, get(), get()) }
    viewModel { PlayerViewModel(get(), get(), get()) }
    viewModel { FavoritesViewModel(get(), get()) }
}
