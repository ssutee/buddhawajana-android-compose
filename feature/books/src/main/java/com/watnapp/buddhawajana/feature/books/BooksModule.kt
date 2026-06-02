package com.watnapp.buddhawajana.feature.books

import com.watnapp.buddhawajana.core.data.download.BookFileStore
import com.watnapp.buddhawajana.feature.books.list.BookListViewModel
import com.watnapp.buddhawajana.feature.books.reader.PdfDocument
import com.watnapp.buddhawajana.feature.books.reader.ReaderViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

val booksModule = module {
    viewModel { BookListViewModel(get(), downloaded = { id -> get<BookFileStore>().exists(id) }) }
    viewModel { (bookId: Long) ->
        ReaderViewModel(
            bookId = bookId,
            books = get(),
            downloader = get(),
            files = get(),
            bookmarks = get(),
            progress = get(),
            openPdf = { f -> PdfDocument.open(f) },
        )
    }
}
