package com.watnapp.buddhawajana.core.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.watnapp.buddhawajana.core.data.repo.PlaybackProgressRepository
import com.watnapp.buddhawajana.core.model.Album
import com.watnapp.buddhawajana.core.model.Audio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Binds a MediaController to PlaybackService, exposes player state as StateFlows, and
 * forwards commands. Persists/restores playback position via PlaybackProgressRepository.
 *
 * Device-verified (MediaController requires the running service); not unit-tested.
 */
class MediaPlaybackController(
    private val context: Context,
    private val progress: PlaybackProgressRepository,
    private val prefs: PlaybackPrefs,
) : PlaybackController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controller: MediaController? = null
    private var sleepJob: kotlinx.coroutines.Job? = null

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    override val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()
    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    private val _positionMs = MutableStateFlow(0L)
    override val positionMs: StateFlow<Long> = _positionMs.asStateFlow()
    private val _durationMs = MutableStateFlow(0L)
    override val durationMs: StateFlow<Long> = _durationMs.asStateFlow()
    private val _speed = MutableStateFlow(1.0f)
    override val speed: StateFlow<Float> = _speed.asStateFlow()
    private val _sleepTimer = MutableStateFlow<SleepTimerState>(SleepTimerState.Off)
    override val sleepTimer: StateFlow<SleepTimerState> = _sleepTimer.asStateFlow()

    init { connect() }

    private fun connect() {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            val c = future.get()
            controller = c
            scope.launch { prefs.speed.collect { rate -> _speed.value = rate; c.setPlaybackSpeed(rate) } }
            c.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    if (!isPlaying) saveProgress()
                }
                override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                    saveProgress()
                    updateNowPlaying()
                }
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED && _sleepTimer.value is SleepTimerState.EndOfTrack) {
                        c.pause(); _sleepTimer.value = SleepTimerState.Off
                    }
                }
            })
            updateNowPlaying()
            startTicker()
        }, MoreExecutors.directExecutor())
    }

    private fun startTicker() = scope.launch {
        var sinceSaveMs = 0L
        while (true) {
            val c = controller
            if (c != null && c.isPlaying) {
                _positionMs.value = c.currentPosition
                _durationMs.value = c.duration.coerceAtLeast(0L)
                sinceSaveMs += 500
                if (sinceSaveMs >= 10_000) { saveProgress(); sinceSaveMs = 0 }
            }
            delay(500)
        }
    }

    private fun updateNowPlaying() {
        val c = controller ?: return
        val item = c.currentMediaItem
        if (item == null) { _nowPlaying.value = null; return }
        val md = item.mediaMetadata
        _nowPlaying.value = NowPlaying(
            audioId = item.mediaId,
            albumId = md.extras?.getString("albumId") ?: "",
            title = md.title?.toString() ?: "",
            album = md.albumTitle?.toString() ?: "",
            artworkUrl = md.artworkUri?.toString(),
        )
    }

    private fun saveProgress() {
        val c = controller ?: return
        val id = c.currentMediaItem?.mediaId ?: return
        val pos = c.currentPosition
        scope.launch { progress.save(id, pos) }
    }

    override fun setQueue(album: Album, audios: List<Audio>, startIndex: Int) {
        val c = controller ?: return
        val items = audios.map { audio ->
            MediaItem.Builder()
                .setMediaId(audio.id)
                .setUri(Uri.parse(audio.url))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(audio.title)
                        .setAlbumTitle(album.title)
                        .setArtworkUri(album.coverUrl?.let(Uri::parse))
                        .setExtras(android.os.Bundle().apply { putString("albumId", album.id) })
                        .build(),
                )
                .build()
        }
        scope.launch {
            val resumeMs = progress.get(audios.getOrNull(startIndex)?.id ?: "")?.positionMs ?: 0L
            c.setMediaItems(items, startIndex, resumeMs)
            c.setPlaybackSpeed(_speed.value)
            c.prepare()
            c.play()
            updateNowPlaying()
        }
    }

    override fun playPause() { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
    override fun seekTo(ms: Long) { controller?.seekTo(ms.coerceAtLeast(0)) }
    override fun skip(deltaMs: Long) {
        val c = controller ?: return
        c.seekTo((c.currentPosition + deltaMs).coerceIn(0, c.duration.coerceAtLeast(0)))
    }
    override fun next() { controller?.seekToNextMediaItem() }
    override fun prev() { controller?.seekToPreviousMediaItem() }
    override fun setSpeed(rate: Float) {
        val r = PlaybackSpeed.clamp(rate)
        controller?.setPlaybackSpeed(r)
        _speed.value = r
        scope.launch { prefs.setSpeed(r) }
    }
    override fun setSleepTimer(state: SleepTimerState) {
        sleepJob?.cancel()
        _sleepTimer.value = state
        if (state is SleepTimerState.Duration) {
            sleepJob = scope.launch {
                var remaining = state.remainingMs
                while (!isExpired(remaining)) {
                    delay(1_000)
                    remaining = tickRemaining(remaining, 1_000)
                    _sleepTimer.value = SleepTimerState.Duration(remaining)
                }
                controller?.pause()
                _sleepTimer.value = SleepTimerState.Off
            }
        }
    }
    override fun stop() {
        saveProgress()
        sleepJob?.cancel()
        controller?.run { pause(); clearMediaItems() }
        _nowPlaying.value = null
        _sleepTimer.value = SleepTimerState.Off
    }
}
