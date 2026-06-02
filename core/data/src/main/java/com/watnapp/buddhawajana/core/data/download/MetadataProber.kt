package com.watnapp.buddhawajana.core.data.download

import android.media.MediaMetadataRetriever
import com.watnapp.buddhawajana.core.common.runCatchingCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Probes an audio URL for duration (ms) and size (bytes). Best-effort: a failure
 * yields null for that field. Heavy work runs on Dispatchers.IO.
 * Primary ctor takes lambdas for testability; the OkHttpClient secondary ctor wires
 * the real HEAD + MediaMetadataRetriever impls.
 */
class MetadataProber(
    private val headSize: suspend (url: String) -> Long?,
    private val readDuration: suspend (url: String) -> Long?,
) {
    suspend fun probe(url: String): Pair<Long?, Long?> = withContext(Dispatchers.IO) {
        val size = runCatchingCancellable { headSize(url) }.getOrNull()
        val dur = runCatchingCancellable { readDuration(url) }.getOrNull()
        dur to size
    }

    constructor(client: OkHttpClient) : this(
        headSize = { url ->
            client.newCall(Request.Builder().url(url).head().build()).execute().use { resp ->
                resp.header("Content-Length")?.toLongOrNull()
            }
        },
        readDuration = { url ->
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(url, HashMap())
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            } finally {
                retriever.release()
            }
        },
    )
}
