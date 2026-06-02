package com.watnapp.buddhawajana.core.player

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val playerModule = module {
    single { PlaybackPrefs(androidContext()) }
    single<PlaybackController> {
        MediaPlaybackController(
            androidContext(), get(), get(),
            localFile = { id ->
                val files = get<com.watnapp.buddhawajana.core.data.download.AudioFileStore>()
                if (files.exists(id)) files.file(id) else null
            },
        )
    }
}
