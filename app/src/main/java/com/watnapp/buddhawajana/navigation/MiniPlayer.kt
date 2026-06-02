package com.watnapp.buddhawajana.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.watnapp.buddhawajana.core.designsystem.component.CachedAsyncImage
import com.watnapp.buddhawajana.core.player.PlaybackController

@Composable
fun MiniPlayer(controller: PlaybackController, onExpand: () -> Unit, modifier: Modifier = Modifier) {
    val now by controller.nowPlaying.collectAsState()
    val isPlaying by controller.isPlaying.collectAsState()
    val position by controller.positionMs.collectAsState()
    val duration by controller.durationMs.collectAsState()
    val track = now ?: return

    Surface(tonalElevation = 3.dp, modifier = modifier.fillMaxWidth()) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onExpand).padding(8.dp),
            ) {
                CachedAsyncImage(url = track.artworkUrl, contentDescription = track.title, modifier = Modifier.size(40.dp))
                Text(
                    track.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )
                IconButton(onClick = controller::playPause) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "เล่น/หยุด")
                }
                IconButton(onClick = controller::stop) { Icon(Icons.Default.Close, "ปิด") }
            }
            LinearProgressIndicator(
                progress = { if (duration > 0) position.toFloat() / duration else 0f },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
