package com.watnapp.buddhawajana.core.data.download

import androidx.work.WorkInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadStateTest {
    @Test fun `downloaded when row and file present`() =
        assertEquals(DownloadState.Downloaded, mapDownloadState(true, true, WorkInfo.State.RUNNING, 0.5f))

    @Test fun `row without file falls through to work state`() =
        assertEquals(DownloadState.Downloading(0.5f), mapDownloadState(true, false, WorkInfo.State.RUNNING, 0.5f))

    @Test fun `enqueued is Queued`() =
        assertEquals(DownloadState.Queued, mapDownloadState(false, false, WorkInfo.State.ENQUEUED, 0f))

    @Test fun `running is Downloading with fraction`() =
        assertEquals(DownloadState.Downloading(0.3f), mapDownloadState(false, false, WorkInfo.State.RUNNING, 0.3f))

    @Test fun `failed is Failed`() =
        assertEquals(DownloadState.Failed, mapDownloadState(false, false, WorkInfo.State.FAILED, 0f))

    @Test fun `no work and no file is NotDownloaded`() =
        assertEquals(DownloadState.NotDownloaded, mapDownloadState(false, false, null, 0f))

    @Test fun `succeeded work but no row yet is NotDownloaded`() =
        assertEquals(DownloadState.NotDownloaded, mapDownloadState(false, false, WorkInfo.State.SUCCEEDED, 0f))
}
