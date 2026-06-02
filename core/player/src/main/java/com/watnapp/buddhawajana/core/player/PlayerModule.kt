package com.watnapp.buddhawajana.core.player

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val playerModule = module {
    single { PlaybackPrefs(androidContext()) }
    single<PlaybackController> { MediaPlaybackController(androidContext(), get(), get()) }
}
