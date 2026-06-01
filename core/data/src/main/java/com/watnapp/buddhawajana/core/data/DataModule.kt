package com.watnapp.buddhawajana.core.data

import androidx.room.Room
import com.watnapp.buddhawajana.core.data.db.AppDatabase
import com.watnapp.buddhawajana.core.data.repo.AlbumRepository
import com.watnapp.buddhawajana.core.data.repo.AudioRepository
import com.watnapp.buddhawajana.core.data.repo.BookRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, AppDatabase.NAME)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }
    single { get<AppDatabase>().bookDao() }
    single { get<AppDatabase>().albumDao() }
    single { get<AppDatabase>().audioDao() }
    single { BookRepository(get(), get()) }
    single { AlbumRepository(get(), get()) }
    single { AudioRepository(get(), get()) }
}
