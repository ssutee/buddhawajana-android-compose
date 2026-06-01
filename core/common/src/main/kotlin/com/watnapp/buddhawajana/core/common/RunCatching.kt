package com.watnapp.buddhawajana.core.common

import kotlin.coroutines.cancellation.CancellationException

/** Like [runCatching] but rethrows [CancellationException] so coroutine cancellation propagates. */
inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (c: CancellationException) {
        throw c
    } catch (e: Throwable) {
        Result.failure(e)
    }
