package com.watnapp.buddhawajana

import android.app.Dialog
import android.content.DialogInterface
import androidx.annotation.LayoutRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.github.rupinderjeet.kprogresshud.KProgressHUD

fun ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(context).inflate(layoutRes, this, attachToRoot)
}

fun KProgressHUD.setOnCancelListener(listener: DialogInterface.OnCancelListener) {
    javaClass.getDeclaredField("mProgressDialog").let {
        it.isAccessible = true
        val mProgressDialog = it.get(this) as Dialog
        mProgressDialog.setOnCancelListener(listener)
    }
}

