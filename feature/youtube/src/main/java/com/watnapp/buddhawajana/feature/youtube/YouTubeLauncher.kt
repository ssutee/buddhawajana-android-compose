package com.watnapp.buddhawajana.feature.youtube

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Opens the Buddhawajana YouTube channel — the YouTube app if installed, else a browser. */
object YouTubeLauncher {
    const val CHANNEL_URL = "https://www.youtube.com/@BuddhawajanaReal"
    private const val YOUTUBE_PACKAGE = "com.google.android.youtube"

    fun open(context: Context) {
        val uri = Uri.parse(CHANNEL_URL)
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage(YOUTUBE_PACKAGE))
        } catch (e: ActivityNotFoundException) {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }
}
