package com.watnapp.buddhawajana

import android.app.Application
import com.watnapp.buddhawajana.core.data.dataModule
import com.watnapp.buddhawajana.core.network.networkModule
import com.watnapp.buddhawajana.core.player.playerModule
import com.watnapp.buddhawajana.feature.audio.audioModule
import com.watnapp.buddhawajana.feature.books.booksModule
import com.watnapp.buddhawajana.repository.BookRepository
import com.watnapp.buddhawajana.vm.BookViewModel
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
            // playerModule + audioModule: Media3 playback engine + :feature:audio.
            // appModule: legacy YouTube + Book DI (Audio has migrated to :feature:audio).
            modules(networkModule, dataModule, playerModule, audioModule, booksModule, appModule)
        }
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            val mgr = getSystemService(android.app.NotificationManager::class.java)
            mgr.createNotificationChannel(
                android.app.NotificationChannel(
                    "downloads",
                    "ดาวน์โหลด",
                    android.app.NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
    }
}

val appModule = module {
    single { AppDatabaseProvider(get()) }

    single { BookRepository(get()) }
    viewModel { BookViewModel(get()) }

}
