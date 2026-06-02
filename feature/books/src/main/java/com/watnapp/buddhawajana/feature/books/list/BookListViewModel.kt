package com.watnapp.buddhawajana.feature.books.list

import androidx.lifecycle.viewModelScope
import com.watnapp.buddhawajana.core.data.repo.BookRepository
import com.watnapp.buddhawajana.core.model.Book
import com.watnapp.buddhawajana.core.ui.BaseViewModel
import com.watnapp.buddhawajana.core.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BookUi(val book: Book, val downloaded: Boolean)

class BookListViewModel(
    private val repo: BookRepository,
    private val downloaded: (Long) -> Boolean,
) : BaseViewModel() {

    private val query = MutableStateFlow("")
    fun onSearch(q: String) { query.value = q }

    val state: StateFlow<UiState<List<BookUi>>> =
        combine(repo.stream(), query) { books, q ->
            val filtered = if (q.isBlank()) books
                else books.filter { it.title.contains(q.trim(), ignoreCase = true) }
            if (filtered.isEmpty()) UiState.Empty
            else UiState.Content(filtered.map { BookUi(it, downloaded(it.id.toLongOrNull() ?: -1)) })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        repo.refresh().onFailure { emitMessage("รีเฟรชล้มเหลว") }
    }

    fun delete(id: Long, onDeleted: () -> Unit = {}) { /* hook for grid delete via BookFileStore; wired in BT10 */ }
}
