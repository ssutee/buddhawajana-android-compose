package com.watnapp.buddhawajana.feature.books

import com.watnapp.buddhawajana.core.data.download.BookFileStore
import com.watnapp.buddhawajana.feature.books.list.BookListViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val booksModule = module {
    viewModel { BookListViewModel(get(), downloaded = { id -> get<BookFileStore>().exists(id) }) }
}
