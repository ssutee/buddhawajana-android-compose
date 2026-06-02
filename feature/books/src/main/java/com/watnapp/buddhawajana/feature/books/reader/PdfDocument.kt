package com.watnapp.buddhawajana.feature.books.reader

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.collection.LruCache
import java.io.File

class PdfDocument private constructor(
    private val pfd: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
) {
    val pageCount: Int get() = renderer.pageCount
    private val cache = object : LruCache<Int, Bitmap>(6) {
        override fun entryRemoved(evicted: Boolean, key: Int, oldValue: Bitmap, newValue: Bitmap?) {
            if (evicted && !oldValue.isRecycled) oldValue.recycle()
        }
    }

    @Synchronized
    fun renderPage(index: Int, targetWidthPx: Int): Bitmap {
        cache.get(index)?.let { if (!it.isRecycled) return it }
        renderer.openPage(index).use { page ->
            val scale = targetWidthPx.toFloat() / page.width
            val w = targetWidthPx
            val h = (page.height * scale).toInt().coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            cache.put(index, bmp)
            return bmp
        }
    }

    @Synchronized
    fun close() {
        cache.evictAll()
        renderer.close()
        pfd.close()
    }

    companion object {
        fun open(file: File): PdfDocument {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            return PdfDocument(pfd, PdfRenderer(pfd))
        }
    }
}
