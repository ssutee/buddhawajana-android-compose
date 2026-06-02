package com.watnapp.buddhawajana.feature.audio.download

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.watnapp.buddhawajana.core.data.download.DownloadState

@Composable
fun DownloadButton(
    state: DownloadState,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        DownloadState.NotDownloaded ->
            IconButton(onClick = onDownload, modifier = modifier) {
                Icon(Icons.Default.Download, contentDescription = "ดาวน์โหลด")
            }
        DownloadState.Queued ->
            IconButton(onClick = onCancel, modifier = modifier) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            }
        is DownloadState.Downloading ->
            IconButton(onClick = onCancel, modifier = modifier) {
                CircularProgressIndicator(progress = { state.fraction }, modifier = Modifier.size(20.dp))
            }
        DownloadState.Downloaded ->
            IconButton(onClick = onDelete, modifier = modifier) {
                Icon(Icons.Default.CheckCircle, contentDescription = "ลบไฟล์", tint = MaterialTheme.colorScheme.primary)
            }
        DownloadState.Failed ->
            IconButton(onClick = onDownload, modifier = modifier) {
                Icon(Icons.Default.ErrorOutline, contentDescription = "ลองใหม่", tint = MaterialTheme.colorScheme.error)
            }
    }
}
