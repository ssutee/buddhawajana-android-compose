package com.watnapp.buddhawajana.feature.books.reader

import android.graphics.Bitmap

class FakePdf : PdfHandle {
    override val pageCount = 3
    override fun renderPage(index: Int, targetWidthPx: Int): Bitmap = throw UnsupportedOperationException()
    override fun close() {}
}
