package com.watnapp.buddhawajana

import android.app.Application
import com.watnapp.buddhawajana.core.network.networkModule
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
            // dataModule is intentionally NOT included here: both the legacy appModule and
            // core:data's dataModule define a single AppDatabase("watna-compose.db"), which
            // would cause a Koin duplicate-definition conflict. The legacy appModule owns
            // the DB for now; Task 11 will migrate the legacy screens to core:data and
            // then switch to dataModule exclusively.
            modules(networkModule, appModule)
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