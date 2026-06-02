package com.watnapp.buddhawajana.core.common

/** Upgrade a plain `http://` URL to `https://` and trim whitespace; leave other schemes as-is. */
fun String.toHttpsOrSelf(): String {
    val t = trim()
    return if (t.startsWith("http://")) "https://" + t.removePrefix("http://") else t
}
