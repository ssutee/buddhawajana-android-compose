package com.watnapp.buddhawajana.feature.books.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
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
import com.watnapp.buddhawajana.core.designsystem.theme.Spacing
import com.watnapp.buddhawajana.core.ui.state.UiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BookListScreen(
    state: UiState<List<BookUi>>,
    query: String,
    onSearch: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpen: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = onSearch,
            singleLine = true, modifier = Modifier.fillMaxWidth().padding(Spacing.m),
            placeholder = { Text("ค้นหาหนังสือ") },
        )
        when (state) {
            is UiState.Loading -> LoadingView()
            is UiState.Empty -> EmptyStateView("ไม่พบหนังสือ")
            is UiState.Error -> ErrorView(state.message, onRefresh)
            is UiState.Content -> LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                contentPadding = PaddingValues(Spacing.m),
                horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                items(state.data, key = { it.book.id }) { item ->
                    BookCell(item, onClick = { onOpen(item.book.id.toLong()) }, onLongClick = { onDelete(item.book.id.toLong()) })
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookCell(item: BookUi, onClick: () -> Unit, onLongClick: () -> Unit) {
    Column(Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(12.dp))) {
            CachedAsyncImage(item.book.coverUrl, item.book.title, Modifier.fillMaxSize())
            item.book.category?.let {
                Surface(color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f), modifier = Modifier.align(Alignment.TopStart).padding(6.dp), shape = RoundedCornerShape(6.dp)) {
                    Text(it, color = MaterialTheme.colorScheme.inverseOnSurface, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Icon(
                if (item.downloaded) Icons.Default.CheckCircle else Icons.Default.Download,
                contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
            )
        }
        Text(item.book.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = Spacing.xs))
    }
}
