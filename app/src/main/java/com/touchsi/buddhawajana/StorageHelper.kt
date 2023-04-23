package com.touchsi.buddhawajana

import android.os.Environment

class StorageHelper {
    companion object {
        private var externalStorageReadable: Boolean = false
        private var externalStorageWritable: Boolean = false

        fun isExternalStorageReadableAndWritable(): Boolean {
            checkStorage()
            return externalStorageReadable && externalStorageWritable
        }

        private fun checkStorage() {
            when(Environment.getExternalStorageState()) {
                Environment.MEDIA_MOUNTED -> {
                    externalStorageReadable = true
                    externalStorageWritable = true
                }
                Environment.MEDIA_MOUNTED_READ_ONLY -> {
                    externalStorageReadable = true
                    externalStorageWritable = false
                }
                else -> {
                    externalStorageReadable = false
                    externalStorageWritable = false
                }
            }
        }

    }
}