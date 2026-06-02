package com.watnapp.buddhawajana.core.data.download

import androidx.work.WorkInfo

sealed interface DownloadState {
    data object NotDownloaded : DownloadState
    data object Queued : DownloadState
    data class Downloading(val fraction: Float) : DownloadState
    data object Downloaded : DownloadState
    data object Failed : DownloadState
}

/** Merge persistent (entity+file) and transient (WorkInfo) signals into one state. Pure. */
fun mapDownloadState(
    downloaded: Boolean,
    fileExists: Boolean,
    infoState: WorkInfo.State?,
    fraction: Float,
): DownloadState =
    if (downloaded && fileExists) DownloadState.Downloaded
    else when (infoState) {
        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> DownloadState.Queued
        WorkInfo.State.RUNNING -> DownloadState.Downloading(fraction)
        WorkInfo.State.FAILED -> DownloadState.Failed
        else -> DownloadState.NotDownloaded
    }
