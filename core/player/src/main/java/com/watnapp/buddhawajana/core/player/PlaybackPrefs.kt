package com.watnapp.buddhawajana.core.player

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.playbackDataStore: DataStore<Preferences> by preferencesDataStore("playback_prefs")

class PlaybackPrefs(private val context: Context) {
    private val speedKey = floatPreferencesKey("speed")

    val speed: Flow<Float> = context.playbackDataStore.data.map { it[speedKey] ?: 1.0f }

    suspend fun setSpeed(rate: Float) {
        context.playbackDataStore.edit { it[speedKey] = PlaybackSpeed.clamp(rate) }
    }
}
