package com.watnapp.buddhawajana.feature.audio.format

/** Milliseconds → "M:SS" (or "H:MM:SS" past an hour). Negatives clamp to "0:00". */
fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** Row subtitle: "M:SS · X MB", any subset, or "—" when nothing is known yet. */
fun formatRowMeta(durationMs: Long?, sizeBytes: Long?): String {
    val parts = listOfNotNull(
        durationMs?.let { formatTime(it) },
        sizeBytes?.let { "%d MB".format((it / 1_000_000.0).toLong().coerceAtLeast(0)) },
    )
    return if (parts.isEmpty()) "—" else parts.joinToString(" · ")
}
