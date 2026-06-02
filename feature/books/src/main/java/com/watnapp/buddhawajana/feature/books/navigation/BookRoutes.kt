package com.watnapp.buddhawajana.feature.books.navigation

import kotlinx.serialization.Serializable

@Serializable data object BooksListRoute
@Serializable data class ReaderRoute(val bookId: Long)
