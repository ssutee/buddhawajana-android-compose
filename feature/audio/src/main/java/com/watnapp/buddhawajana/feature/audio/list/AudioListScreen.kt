package com.watnapp.buddhawajana.feature.audio.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.watnapp.buddhawajana.core.data.download.DownloadState
import com.watnapp.buddhawajana.core.designsystem.component.EmptyStateView
import com.watnapp.buddhawajana.core.designsystem.component.ErrorView
import com.watnapp.buddhawajana.core.designsystem.component.LoadingView
import com.watnapp.buddhawajana.core.designsystem.theme.Spacing
import com.watnapp.buddhawajana.core.model.Audio
import com.watnapp.buddhawajana.core.ui.state.UiState
import com.watnapp.buddhawajana.feature.audio.format.formatRowMeta
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioListScreen(
    state: UiState<List<Audio>>,
    query: String,
    playingAudioId: String?,
    onSearch: (String) -> Unit,
    onRefresh: () -> Unit,
    onRowVisible: (Audio) -> Unit,
    onPlay: (Int) -> Unit,
    downloadStateFor: (String) -> Flow<DownloadState>,
    onDownload: (Audio) -> Unit,
    onCancelDownload: (String) -> Unit,
    onDeleteDownload: (String) -> Unit,
    onDownloadAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = onSearch,
            singleLine = true, modifier = Modifier.fillMaxWidth().padding(Spacing.m),
            placeholder = { Text("ค้นหาเสียง") },
        )
        when (state) {
            is UiState.Loading -> LoadingView()
            is UiState.Empty -> EmptyStateView("ไม่พบไฟล์เสียง")
            is UiState.Error -> ErrorView(state.message, onRefresh)
            is UiState.Content -> Column(Modifier.fillMaxSize()) {
                androidx.compose.material3.TextButton(
                    onClick = onDownloadAll,
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) { Text("ดาวน์โหลดทั้งหมด") }
                LazyColumn(Modifier.fillMaxSize()) {
                    itemsIndexed(state.data, key = { _, a -> a.id }) { index, audio ->
                        LaunchedEffect(audio.id) { onRowVisible(audio) }
                        val dl by remember(audio.id) { downloadStateFor(audio.id) }
                            .collectAsState(initial = DownloadState.NotDownloaded)
                        ListItem(
                            headlineContent = {
                                Text(
                                    audio.title,
                                    color = if (audio.id == playingAudioId) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface,
                                )
                            },
                            supportingContent = { Text(formatRowMeta(audio.durationMs, audio.sizeBytes)) },
                            trailingContent = {
                                com.watnapp.buddhawajana.feature.audio.download.DownloadButton(
                                    state = dl,
                                    onDownload = { onDownload(audio) },
                                    onCancel = { onCancelDownload(audio.id) },
                                    onDelete = { onDeleteDownload(audio.id) },
                                )
                            },
                            modifier = Modifier.clickable { onPlay(index) },
                        )
                    }
                }
            }
        }
    }
}
