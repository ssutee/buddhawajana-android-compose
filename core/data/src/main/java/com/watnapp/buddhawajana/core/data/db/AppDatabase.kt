package com.watnapp.buddhawajana.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [BookEntity::class, AlbumEntity::class, AudioEntity::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun albumDao(): AlbumDao
    abstract fun audioDao(): AudioDao

    companion object {
        const val NAME = "watna-compose.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `book_new` (
                        `book_id` INTEGER NOT NULL,
                        `progress` INTEGER NOT NULL,
                        `status` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `cover_url` TEXT NOT NULL,
                        `book_url` TEXT NOT NULL,
                        `order_number` INTEGER NOT NULL,
                        `is_new` INTEGER NOT NULL,
                        `is_updated` INTEGER NOT NULL,
                        `detail` TEXT NOT NULL,
                        `total_page` INTEGER NOT NULL,
                        `producer` TEXT NOT NULL,
                        `version` TEXT NOT NULL,
                        `request_id` TEXT NOT NULL,
                        `added_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        PRIMARY KEY(`book_id`)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `album_new` (
                        `album_id` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `cover_url` TEXT NOT NULL,
                        `view_count` INTEGER NOT NULL,
                        `position` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        `item_count` INTEGER NOT NULL,
                        PRIMARY KEY(`album_id`)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `audio_backup_1_2` (
                        `audio_id` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `url` TEXT NOT NULL,
                        `album_id` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        `status` INTEGER NOT NULL,
                        `request_id` INTEGER NOT NULL,
                        `progress` INTEGER NOT NULL,
                        PRIMARY KEY(`audio_id`)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT INTO `book_new` (
                        `book_id`,
                        `progress`,
                        `status`,
                        `title`,
                        `cover_url`,
                        `book_url`,
                        `order_number`,
                        `is_new`,
                        `is_updated`,
                        `detail`,
                        `total_page`,
                        `producer`,
                        `version`,
                        `request_id`,
                        `added_at`,
                        `updated_at`
                    )
                    SELECT
                        `book_id`,
                        `progress`,
                        `status`,
                        COALESCE(`title`, ''),
                        COALESCE(`cover_url`, ''),
                        COALESCE(`book_url`, ''),
                        `order_number`,
                        1,
                        `is_updated`,
                        COALESCE(`detail`, ''),
                        `total_page`,
                        `producer`,
                        `version`,
                        `request_id`,
                        COALESCE(`added_at`, 0),
                        COALESCE(`updated_at`, 0)
                    FROM `book`
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT INTO `album_new` (
                        `album_id`,
                        `title`,
                        `cover_url`,
                        `view_count`,
                        `position`,
                        `updated_at`,
                        `item_count`
                    )
                    SELECT
                        `album_id`,
                        COALESCE(`title`, ''),
                        COALESCE(`cover_url`, ''),
                        `view_count`,
                        `position`,
                        COALESCE(`updated_at`, 0),
                        `item_count`
                    FROM `album`
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT INTO `audio_backup_1_2` (
                        `audio_id`,
                        `title`,
                        `url`,
                        `album_id`,
                        `updated_at`,
                        `status`,
                        `request_id`,
                        `progress`
                    )
                    SELECT
                        `audio_id`,
                        COALESCE(`title`, ''),
                        COALESCE(`url`, ''),
                        `album_id`,
                        COALESCE(`updated_at`, 0),
                        `status`,
                        `request_id`,
                        `progress`
                    FROM `audio`
                    """.trimIndent()
                )
                database.execSQL("DROP TABLE `audio`")
                database.execSQL("DROP TABLE `book`")
                database.execSQL("DROP TABLE `album`")
                database.execSQL("ALTER TABLE `book_new` RENAME TO `book`")
                database.execSQL("ALTER TABLE `album_new` RENAME TO `album`")
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `audio` (
                        `audio_id` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `url` TEXT NOT NULL,
                        `album_id` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        `status` INTEGER NOT NULL,
                        `request_id` INTEGER NOT NULL,
                        `progress` INTEGER NOT NULL,
                        PRIMARY KEY(`audio_id`),
                        FOREIGN KEY(`album_id`) REFERENCES `album`(`album_id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT INTO `audio` (
                        `audio_id`,
                        `title`,
                        `url`,
                        `album_id`,
                        `updated_at`,
                        `status`,
                        `request_id`,
                        `progress`
                    )
                    SELECT
                        `audio_id`,
                        `title`,
                        `url`,
                        `album_id`,
                        `updated_at`,
                        `status`,
                        `request_id`,
                        `progress`
                    FROM `audio_backup_1_2`
                    WHERE `album_id` IN (SELECT `album_id` FROM `album`)
                    """.trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_audio_audio_id` ON `audio` (`audio_id`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_audio_album_id` ON `audio` (`album_id`)")
                database.execSQL("DROP TABLE `audio_backup_1_2`")
            }
        }
    }
}
