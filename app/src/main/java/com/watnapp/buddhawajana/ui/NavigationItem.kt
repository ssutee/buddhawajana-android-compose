package com.watnapp.buddhawajana.ui

import com.watnapp.buddhawajana.R

sealed class NavigationItem(var route: String, var icon: Int, var title: Int) {
    object Books : NavigationItem("books", R.drawable.ic_book, R.string.books)
    object Audio : NavigationItem("audio", R.drawable.ic_audio, R.string.audio)
    object Youtube : NavigationItem("youtube", R.drawable.ic_youtube, R.string.youtube)
}