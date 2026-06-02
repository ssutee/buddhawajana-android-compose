package com.watnapp.buddhawajana.feature.books.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.watnapp.buddhawajana.feature.books.list.BookListScreen
import com.watnapp.buddhawajana.feature.books.list.BookListViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun BooksListPane(onOpenBook: (Long) -> Unit, modifier: Modifier = Modifier) {
    val vm: BookListViewModel = koinViewModel()
    val state by vm.state.collectAsState()
    val query by vm.queryState.collectAsState()
    BookListScreen(
        state = state, query = query,
        onSearch = { vm.onSearch(it) },
        onRefresh = vm::refresh, onOpen = onOpenBook,
        onDelete = { },
        modifier = modifier,
    )
}
