package com.touchsi.buddhawajana

import androidx.room.TypeConverter
import java.util.Date

object Converters {
    @TypeConverter
    @JvmStatic
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(value) }

    @TypeConverter
    @JvmStatic
    fun dateToTimestamp(date: Date?): Long? = date?.time
}