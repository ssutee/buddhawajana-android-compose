package com.watnapp.buddhawajana.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [BookEntity::class, AlbumEntity::class, AudioEntity::class, BookmarkEntity::class, ReadingProgressEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun albumDao(): AlbumDao
    abstract fun audioDao(): AudioDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun readingProgressDao(): ReadingProgressDao

    companion object {
        const val NAME = "buddhawajana.db"
    }
}
