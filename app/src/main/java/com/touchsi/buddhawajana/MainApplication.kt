package com.touchsi.buddhawajana

import android.app.Application
import com.touchsi.buddhawajana.repository.AlbumRepository
import com.touchsi.buddhawajana.repository.AudioRepository
import com.touchsi.buddhawajana.repository.BookRepository
import com.touchsi.buddhawajana.vm.BookViewModel
import com.touchsi.buddhawajana.vm.AlbumViewModel
import com.touchsi.buddhawajana.vm.AudioViewModel
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
            modules(appModule)
        }
    }
}

val appModule = module {
    single { AppDatabaseProvider(get())}

    single { BookRepository(get()) }
    viewModel { BookViewModel(get()) }

    single { AlbumRepository(get()) }
    viewModel { AlbumViewModel(get()) }

    single { AudioRepository(get()) }
    viewModel { AudioViewModel(get()) }
}