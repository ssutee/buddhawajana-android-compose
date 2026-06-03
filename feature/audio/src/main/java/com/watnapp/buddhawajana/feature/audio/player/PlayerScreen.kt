package com.watnapp.buddhawajana.feature.audio.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.content.Intent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.watnapp.buddhawajana.core.designsystem.component.CachedAsyncImage
import com.watnapp.buddhawajana.core.player.PlaybackSpeed
import com.watnapp.buddhawajana.core.player.SleepTimerState
import com.watnapp.buddhawajana.feature.audio.format.formatTime

@Composable
fun PlayerScreen(vm: PlayerViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val now by vm.nowPlaying.collectAsState()
    val isPlaying by vm.isPlaying.collectAsState()
    val position by vm.positionMs.collectAsState()
    val duration by vm.durationMs.collectAsState()

    // Adaptive layout: a single Column stacks art + controls vertically, which only fits when the
    // viewport is tall (portrait). In landscape the square art (sized by width) would overflow the
    // short height and push the controls off-screen, so split into art | controls side-by-side.
    BoxWithConstraints(modifier.fillMaxSize()) {
        if (maxWidth < maxHeight) {
            // PORTRAIT
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                Row(Modifier.fillMaxWidth()) { MinimizeButton(onBack) }
                Artwork(now?.artworkUrl, now?.title, Modifier.fillMaxWidth(0.8f).aspectRatio(1f))
                TrackInfo(now?.title, now?.album)
                SeekSection(vm, position, duration)
                TransportRow(vm, isPlaying)
                SpeedAndSleepRow(vm)
                ActionRow(vm)
            }
        } else {
            // LANDSCAPE — art on the left, scrollable controls on the right.
            Row(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Row(Modifier.fillMaxWidth()) { MinimizeButton(onBack) }
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        // fillMaxSize + aspectRatio(1f) => largest square fitting the box (min side),
                        // so the art never overflows regardless of the box's width/height.
                        Artwork(now?.artworkUrl, now?.title, Modifier.fillMaxSize().aspectRatio(1f))
                    }
                }
                Column(
                    Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                ) {
                    TrackInfo(now?.title, now?.album)
                    SeekSection(vm, position, duration)
                    TransportRow(vm, isPlaying)
                    SpeedAndSleepRow(vm)
                    ActionRow(vm)
                }
            }
        }
    }
}

@Composable
private fun MinimizeButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) { Icon(Icons.Default.KeyboardArrowDown, "ย่อ") }
}

@Composable
private fun Artwork(url: String?, contentDescription: String?, modifier: Modifier) {
    CachedAsyncImage(url = url, contentDescription = contentDescription, modifier = modifier)
}

@Composable
private fun TrackInfo(title: String?, album: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title ?: "", style = MaterialTheme.typography.titleLarge)
        Text(album ?: "", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SeekSection(vm: PlayerViewModel, position: Long, duration: Long) {
    var dragging by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(0f) }
    val shown = if (dragging) draft else position.toFloat()
    Column(Modifier.fillMaxWidth()) {
        Slider(
            value = shown,
            valueRange = 0f..(duration.toFloat().coerceAtLeast(1f)),
            onValueChange = { dragging = true; draft = it },
            onValueChangeFinished = { if (dragging) { vm.seekTo(draft.toLong()); dragging = false } },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(shown.toLong()))
            Text(formatTime(duration))
        }
    }
}

@Composable
private fun TransportRow(vm: PlayerViewModel, isPlaying: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(onClick = vm::prev) { Icon(Icons.Default.SkipPrevious, "ก่อนหน้า") }
        IconButton(onClick = vm::skipBack) { Icon(Icons.Default.FastRewind, "ถอย 15 วิ") }
        IconButton(onClick = vm::playPause) {
            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "เล่น/หยุด")
        }
        IconButton(onClick = vm::skipForward) { Icon(Icons.Default.FastForward, "เดิน 15 วิ") }
        IconButton(onClick = vm::next) { Icon(Icons.Default.SkipNext, "ถัดไป") }
    }
}

@Composable
private fun ActionRow(vm: PlayerViewModel) {
    val isFavorite by vm.isFavorite.collectAsState()
    val now by vm.nowPlaying.collectAsState()
    val downloadState by vm.downloadState.collectAsState()
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        IconButton(onClick = vm::toggleFavorite, enabled = now != null) {
            Icon(
                if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "รายการโปรด",
            )
        }
        com.watnapp.buddhawajana.feature.audio.download.DownloadButton(
            state = downloadState,
            onDownload = vm::download,
            onCancel = vm::cancelDownload,
            onDelete = vm::deleteDownload,
        )
        IconButton(
            onClick = {
                now?.url?.takeIf { it.isNotEmpty() }?.let { url ->
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, url)
                    }
                    context.startActivity(Intent.createChooser(send, null))
                }
            },
            enabled = now != null,
        ) {
            Icon(Icons.Default.Share, contentDescription = "แชร์")
        }
    }
}

@Composable
private fun SpeedAndSleepRow(vm: PlayerViewModel) {
    val speed by vm.speed.collectAsState()
    val sleep by vm.sleepTimer.collectAsState()
    var speedOpen by remember { mutableStateOf(false) }
    var sleepOpen by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box {
            TextButton(onClick = { speedOpen = true }) { Text(speedLabel(speed)) }
            DropdownMenu(expanded = speedOpen, onDismissRequest = { speedOpen = false }) {
                PlaybackSpeed.PRESETS.forEach { r ->
                    DropdownMenuItem(text = { Text(speedLabel(r)) }, onClick = { vm.setSpeed(r); speedOpen = false })
                }
            }
        }
        Box {
            val label = when (val s = sleep) {
                is SleepTimerState.Duration -> formatTime(s.remainingMs)
                SleepTimerState.EndOfTrack -> "จบตอน"
                SleepTimerState.Off -> "ตั้งเวลา"
            }
            TextButton(onClick = { sleepOpen = true }) { Text(label) }
            DropdownMenu(expanded = sleepOpen, onDismissRequest = { sleepOpen = false }) {
                listOf(15, 30, 45, 60).forEach { min ->
                    DropdownMenuItem(
                        text = { Text("$min นาที") },
                        onClick = { vm.setSleepTimer(SleepTimerState.Duration(min * 60_000L)); sleepOpen = false },
                    )
                }
                DropdownMenuItem(text = { Text("จบตอนนี้") }, onClick = { vm.setSleepTimer(SleepTimerState.EndOfTrack); sleepOpen = false })
                DropdownMenuItem(text = { Text("ปิด") }, onClick = { vm.setSleepTimer(SleepTimerState.Off); sleepOpen = false })
            }
        }
    }
}

private fun speedLabel(rate: Float): String =
    if (rate % 1f == 0f) "${rate.toInt()}x" else "${rate}x"
