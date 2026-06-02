package com.watnapp.buddhawajana.core.player

import com.watnapp.buddhawajana.core.model.Album
import com.watnapp.buddhawajana.core.model.Audio
import kotlinx.coroutines.flow.StateFlow

const val SKIP_DELTA_MS = 15_000L

data class NowPlaying(
    val audioId: String,
    val albumId: String,
    val title: String,
    val album: String,
    val artworkUrl: String?,
    val url: String,
    val isLocal: Boolean,
)

sealed interface SleepTimerState {
    data object Off : SleepTimerState
    data class Duration(val remainingMs: Long) : SleepTimerState
    data object EndOfTrack : SleepTimerState
}

/** Decrement a sleep-timer remaining value, clamped at zero. Pure. */
fun tickRemaining(remainingMs: Long, deltaMs: Long): Long = (remainingMs - deltaMs).coerceAtLeast(0L)

/** True once a countdown has reached/passed zero. Pure. */
fun isExpired(remainingMs: Long): Boolean = remainingMs <= 0L

object PlaybackSpeed {
    val PRESETS = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    fun clamp(rate: Float): Float = PRESETS.minByOrNull { kotlin.math.abs(it - rate) } ?: 1.0f
}

/** Playback surface consumed by ViewModels and the mini-player. Media3-backed in production, fakeable in tests. */
interface PlaybackController {
    val nowPlaying: StateFlow<NowPlaying?>
    val isPlaying: StateFlow<Boolean>
    val positionMs: StateFlow<Long>
    val durationMs: StateFlow<Long>
    val speed: StateFlow<Float>
    val sleepTimer: StateFlow<SleepTimerState>

    fun setQueue(album: Album, audios: List<Audio>, startIndex: Int)
    fun playPause()
    fun seekTo(ms: Long)
    fun skip(deltaMs: Long)
    fun next()
    fun prev()
    fun setSpeed(rate: Float)
    fun setSleepTimer(state: SleepTimerState)
    fun stop()
}
