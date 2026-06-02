package com.watnapp.buddhawajana.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [BookEntity::class, AlbumEntity::class, AudioEntity::class, BookmarkEntity::class, ReadingProgressEntity::class, PlaybackProgressEntity::class],
    version = 2,
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
    }
}
