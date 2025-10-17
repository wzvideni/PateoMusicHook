package com.wzvideni.pateo.music.basic

import android.content.Context
import android.os.Handler
import android.os.Looper

fun Context.runOnUiThread(action: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        action()
    } else {
        Handler(Looper.getMainLooper()).post(action)
    }
}
