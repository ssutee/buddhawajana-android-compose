package com.watnapp.buddhawajana.core.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

abstract class BaseViewModel : ViewModel() {
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()   // one-shot snackbar events (e.g. silent refresh failures)

    protected suspend fun emitMessage(text: String) = _messages.send(text)
}
