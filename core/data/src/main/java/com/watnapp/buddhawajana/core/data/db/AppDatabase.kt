package com.watnapp.buddhawajana.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [BookEntity::class, AlbumEntity::class, AudioEntity::class, BookmarkEntity::class, ReadingProgressEntity::class, PlaybackProgressEntity::class, FavoriteEntity::class],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun albumDao(): AlbumDao
    abstract fun audioDao(): AudioDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun playbackProgressDao(): PlaybackProgressDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        const val NAME = "buddhawajana.db"

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE audio ADD COLUMN duration_ms INTEGER")
                db.execSQL("ALTER TABLE audio ADD COLUMN size_bytes INTEGER")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS playback_progress (" +
                        "audio_id TEXT NOT NULL PRIMARY KEY, " +
                        "position_ms INTEGER NOT NULL, " +
                        "updated_at INTEGER NOT NULL)"
                )
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS favorite (" +
                        "audio_id TEXT NOT NULL PRIMARY KEY, " +
                        "title TEXT NOT NULL, url TEXT NOT NULL, " +
                        "album_id TEXT NOT NULL, album_title TEXT NOT NULL, " +
                        "cover_url TEXT, added_at INTEGER NOT NULL)"
                )
            }
        }
    }
}
