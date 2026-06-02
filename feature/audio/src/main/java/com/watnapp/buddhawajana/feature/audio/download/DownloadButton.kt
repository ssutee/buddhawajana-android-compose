package com.watnapp.buddhawajana.feature.audio.download

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
        DownloadState.Downloaded -> {
            var confirm by remember { mutableStateOf(false) }
            IconButton(onClick = { confirm = true }, modifier = modifier) {
                Icon(Icons.Default.CheckCircle, contentDescription = "ลบไฟล์", tint = MaterialTheme.colorScheme.primary)
            }
            if (confirm) {
                AlertDialog(
                    onDismissRequest = { confirm = false },
                    title = { Text("ลบไฟล์ที่ดาวน์โหลด?") },
                    confirmButton = { TextButton(onClick = { confirm = false; onDelete() }) { Text("ลบ") } },
                    dismissButton = { TextButton(onClick = { confirm = false }) { Text("ยกเลิก") } },
                )
            }
        }
        DownloadState.Failed ->
            IconButton(onClick = onDownload, modifier = modifier) {
                Icon(Icons.Default.ErrorOutline, contentDescription = "ลองใหม่", tint = MaterialTheme.colorScheme.error)
            }
    }
}
