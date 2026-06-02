package com.watnapp.buddhawajana.feature.audio.albums

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
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
    favoriteCount: Int,
    onSearch: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenAlbum: (Album) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = onSearch,
            singleLine = true, modifier = Modifier.fillMaxWidth().padding(Spacing.m),
            placeholder = { Text("ค้นหาอัลบั้ม") },
        )
        if (query.isBlank()) {
            FavoritesCard(count = favoriteCount, onClick = onOpenFavorites)
            HorizontalDivider()
        }
        when (state) {
            is UiState.Loading -> LoadingView()
            is UiState.Empty -> EmptyStateView("ไม่พบอัลบั้ม")
            is UiState.Error -> ErrorView(state.message, onRefresh)
            is UiState.Content -> LazyColumn(Modifier.fillMaxSize()) {
                items(state.data, key = { it.id }) { album ->
                    AlbumRow(album, onClick = { onOpenAlbum(album) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoritesCard(count: Int, onClick: () -> Unit) {
    ListItem(
        leadingContent = {
            Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        headlineContent = { Text("รายการโปรด") },
        trailingContent = {
            if (count > 0) Text("$count", style = MaterialTheme.typography.bodyMedium)
            else Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumRow(album: Album, onClick: () -> Unit) {
    ListItem(
        leadingContent = {
            CachedAsyncImage(
                album.coverUrl,
                album.title,
                Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
            )
        },
        headlineContent = {
            Text(album.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        trailingContent = {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
