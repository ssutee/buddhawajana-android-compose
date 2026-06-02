package com.watnapp.buddhawajana.feature.audio.albums

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.watnapp.buddhawajana.core.designsystem.component.CachedAsyncImage
import com.watnapp.buddhawajana.core.designsystem.component.EmptyStateView
import com.watnapp.buddhawajana.core.designsystem.component.ErrorView
import com.watnapp.buddhawajana.core.designsystem.component.LoadingView
import com.watnapp.buddhawajana.core.designsystem.theme.Spacing
import com.watnapp.buddhawajana.core.model.Album
import com.watnapp.buddhawajana.core.ui.state.UiState

@Composable
fun AlbumsScreen(
    state: UiState<List<Album>>,
    query: String,
    onSearch: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenAlbum: (Album) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = onSearch,
            singleLine = true, modifier = Modifier.fillMaxWidth().padding(Spacing.m),
            placeholder = { Text("ค้นหาชุดเสียง") },
        )
        when (state) {
            is UiState.Loading -> LoadingView()
            is UiState.Empty -> EmptyStateView("ไม่พบชุดเสียง")
            is UiState.Error -> ErrorView(state.message, onRefresh)
            is UiState.Content -> LazyVerticalGrid(
                columns = GridCells.Adaptive(110.dp),
                contentPadding = PaddingValues(Spacing.m),
                horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                items(state.data, key = { it.id }) { album ->
                    AlbumCell(album, onClick = { onOpenAlbum(album) })
                }
            }
        }
    }
}

@Composable
private fun AlbumCell(album: Album, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp))) {
            CachedAsyncImage(album.coverUrl, album.title, Modifier.fillMaxSize())
        }
        Text(album.title, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = Spacing.xs))
        if (album.itemCount > 0) {
            Text("${album.itemCount} ตอน", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
