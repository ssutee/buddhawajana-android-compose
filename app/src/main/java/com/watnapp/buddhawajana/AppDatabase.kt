package com.watnapp.buddhawajana

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.watnapp.buddhawajana.entity.AlbumDao
import com.watnapp.buddhawajana.entity.AlbumEntity
import com.watnapp.buddhawajana.entity.AudioDao
import com.watnapp.buddhawajana.entity.AudioEntity
import com.watnapp.buddhawajana.entity.BookDao
import com.watnapp.buddhawajana.entity.BookEntity

@Database(entities = [BookEntity::class, AlbumEntity::class, AudioEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun albumDao(): AlbumDao
    abstract fun audioDao(): AudioDao
}

class AppDatabaseProvider(private var context: Context) {
    private var instance: AppDatabase? = null
    fun getDatabase(): AppDatabase {
        if (instance == null) {
            synchronized(AppDatabase::class.java) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context,
                            AppDatabase::class.java,
                            "watna-compose.db")
                            .build()
                }
            }
        }
        return instance!!
    }
}