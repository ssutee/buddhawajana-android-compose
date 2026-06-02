package com.watnapp.buddhawajana.feature.books.reader

import androidx.lifecycle.viewModelScope
import com.watnapp.buddhawajana.core.data.download.BookFileStore
import com.watnapp.buddhawajana.core.data.download.DownloadProgress
import com.watnapp.buddhawajana.core.data.download.FileDownloader
import com.watnapp.buddhawajana.core.data.repo.BookRepository
import com.watnapp.buddhawajana.core.data.repo.BookmarkRepository
import com.watnapp.buddhawajana.core.data.repo.ReadingProgressRepository
import com.watnapp.buddhawajana.core.model.Bookmark
import com.watnapp.buddhawajana.core.ui.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

sealed interface ReaderState {
    data object Loading : ReaderState
    data class Downloading(val fraction: Float) : ReaderState
    data class Ready(val pdf: PdfHandle, val pageCount: Int, val startPage: Int) : ReaderState
    data class Error(val message: String) : ReaderState
}

class ReaderViewModel(
    private val bookId: Long,
    private val books: BookRepository,
    private val downloader: FileDownloader,
    private val files: BookFileStore,
    private val bookmarks: BookmarkRepository,
    private val progress: ReadingProgressRepository,
    private val openPdf: (File) -> PdfHandle,
) : BaseViewModel() {

    private val _state = MutableStateFlow<ReaderState>(ReaderState.Loading)
    val state: StateFlow<ReaderState> = _state.asStateFlow()

    val bookmarksFor = bookmarks.stream(bookId)

    private val _page = MutableStateFlow(0)
    val page: StateFlow<Int> = _page.asStateFlow()

    init { open() }

    private fun open() = viewModelScope.launch {
        try {
            if (!files.exists(bookId)) {
                val url = books.stream().first().firstOrNull { it.id.toLongOrNull() == bookId }?.fileUrl
                if (url.isNullOrEmpty()) { _state.value = ReaderState.Error("ไม่พบไฟล์"); return@launch }
                var failed = false
                downloader.download(url, files.file(bookId)).collect { p ->
                    when (p) {
                        is DownloadProgress.Progress -> _state.value = ReaderState.Downloading(p.fraction)
                        is DownloadProgress.Failed -> { _state.value = ReaderState.Error("ดาวน์โหลดล้มเหลว"); failed = true }
                        is DownloadProgress.Done -> {}
                    }
                }
                if (failed) return@launch
            }
            val pdf = openPdf(files.file(bookId))
            val start = (progress.get(bookId)?.page ?: 0).coerceIn(0, (pdf.pageCount - 1).coerceAtLeast(0))
            _page.value = start
            _state.value = ReaderState.Ready(pdf, pdf.pageCount, start)
        } catch (e: Throwable) {
            _state.value = ReaderState.Error("เปิดไฟล์ไม่ได้")
        }
    }

    fun onPageChanged(index: Int) {
        _page.value = index
        viewModelScope.launch { progress.save(bookId, index) }
    }

    fun addBookmark(page: Int) = viewModelScope.launch { bookmarks.add(bookId, page, "หน้า ${page + 1}") }
    fun deleteBookmark(b: Bookmark) = viewModelScope.launch { bookmarks.delete(b) }

    override fun onCleared() {
        super.onCleared()
        (_state.value as? ReaderState.Ready)?.pdf?.close()
    }
}
