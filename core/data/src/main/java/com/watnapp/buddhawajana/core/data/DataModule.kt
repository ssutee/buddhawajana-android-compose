package com.watnapp.buddhawajana.core.data

import androidx.room.Room
import com.watnapp.buddhawajana.core.data.db.AppDatabase
import com.watnapp.buddhawajana.core.data.download.BookFileStore
import com.watnapp.buddhawajana.core.data.download.FileDownloader
import com.watnapp.buddhawajana.core.data.repo.AlbumRepository
import com.watnapp.buddhawajana.core.data.repo.AudioRepository
import com.watnapp.buddhawajana.core.data.repo.BookmarkRepository
import com.watnapp.buddhawajana.core.data.repo.BookRepository
import com.watnapp.buddhawajana.core.data.repo.ReadingProgressRepository
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, AppDatabase.NAME)
            .build()
    }
    single { get<AppDatabase>().bookDao() }
    single { get<AppDatabase>().albumDao() }
    single { get<AppDatabase>().audioDao() }
    single { get<AppDatabase>().bookmarkDao() }
    single { get<AppDatabase>().readingProgressDao() }
    single { BookRepository(get(), get()) }
    single { AlbumRepository(get(), get()) }
    single { AudioRepository(get(), get()) }
    single { BookmarkRepository(get()) }
    single { ReadingProgressRepository(get()) }
    single { FileDownloader(get<OkHttpClient>()) }
    single { BookFileStore(androidContext()) }
}
