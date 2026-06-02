package com.watnapp.buddhawajana

import android.app.Application
import com.watnapp.buddhawajana.core.data.dataModule
import com.watnapp.buddhawajana.core.network.networkModule
import com.watnapp.buddhawajana.core.player.playerModule
import com.watnapp.buddhawajana.feature.audio.audioModule
import com.watnapp.buddhawajana.feature.books.booksModule
import com.watnapp.buddhawajana.repository.AlbumRepository
import com.watnapp.buddhawajana.repository.AudioRepository
import com.watnapp.buddhawajana.repository.BookRepository
import com.watnapp.buddhawajana.vm.BookViewModel
import com.watnapp.buddhawajana.vm.AlbumViewModel
import com.watnapp.buddhawajana.vm.AudioViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            // networkModule: provides OkHttpClient/Moshi/Retrofit (new core:network).
            // dataModule: provides the new AppDatabase ("buddhawajana.db") — a DIFFERENT
            //   Kotlin class (com.watnapp.buddhawajana.core.data.db.AppDatabase) from the
            //   legacy AppDatabase ("watna-compose.db") in appModule, so no Koin
            //   duplicate-type conflict.
            // booksModule: provides BookListViewModel and ReaderViewModel for :feature:books.
            // appModule: legacy Audio/YouTube DI (kept until Task 11 migration).
            modules(networkModule, dataModule, playerModule, audioModule, booksModule, appModule)
        }
    }
}

val appModule = module {
    single { AppDatabaseProvider(get()) }

    single { BookRepository(get()) }
    viewModel { BookViewModel(get()) }

    single { AlbumRepository(get()) }
    viewModel { AlbumViewModel(get()) }

    single { AudioRepository(get()) }
    viewModel { AudioViewModel(get()) }
}
