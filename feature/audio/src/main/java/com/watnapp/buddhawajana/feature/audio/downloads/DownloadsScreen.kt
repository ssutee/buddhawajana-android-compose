package com.watnapp.buddhawajana.feature.audio.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.watnapp.buddhawajana.core.designsystem.component.CachedAsyncImage
import com.watnapp.buddhawajana.core.designsystem.component.EmptyStateView
import com.watnapp.buddhawajana.core.designsystem.component.ErrorView
import com.watnapp.buddhawajana.core.designsystem.component.LoadingView
import com.watnapp.buddhawajana.core.model.Download
import com.watnapp.buddhawajana.core.ui.state.UiState
import com.watnapp.buddhawajana.feature.audio.format.formatRowMeta

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    state: UiState<List<Download>>,
    onPlay: (Download) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is UiState.Loading -> LoadingView()
        is UiState.Empty -> EmptyStateView("ยังไม่มีรายการดาวน์โหลด")
        is UiState.Error -> ErrorView(message = state.message, onRetry = { })
        is UiState.Content -> LazyColumn(modifier.fillMaxSize()) {
            items(state.data, key = { it.audioId }) { d ->
                val dismiss = rememberSwipeToDismissBoxState(
                    confirmValueChange = { v ->
                        if (v != SwipeToDismissBoxValue.Settled) { onRemove(d.audioId); true } else false
                    },
                )
                SwipeToDismissBox(
                    state = dismiss,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        Box(
                            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer)
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterEnd,
                        ) { Icon(Icons.Default.Delete, contentDescription = null) }
                    },
                ) {
                    ListItem(
                        leadingContent = {
                            CachedAsyncImage(d.coverUrl, d.title, Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)))
                        },
                        headlineContent = { Text(d.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(formatRowMeta(null, d.sizeBytes)) },
                        modifier = Modifier.clickable { onPlay(d) },
                    )
                }
                HorizontalDivider()
            }
        }
    }
}
