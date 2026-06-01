package com.watnapp.buddhawajana.core.ui.state

fun <T> List<T>.toListUiState(): UiState<List<T>> =
    if (isEmpty()) UiState.Empty else UiState.Content(this)
