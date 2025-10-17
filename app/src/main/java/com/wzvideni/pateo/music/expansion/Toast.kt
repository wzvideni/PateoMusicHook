package com.wzvideni.pateo.music.expansion

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import com.wzvideni.pateo.music.basic.runOnUiThread


fun Context.toast(message: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
    runOnUiThread {
        Toast.makeText(this, message, duration).show()
    }
}

fun Context.toast(@StringRes message: Int, duration: Int = Toast.LENGTH_SHORT) {
    runOnUiThread {
        Toast.makeText(this, getString(message), duration).show()
    }
}

fun Context.longToast(message: CharSequence, duration: Int = Toast.LENGTH_LONG) {
    runOnUiThread {
        Toast.makeText(this, message, duration).show()
    }
}

fun Context.longToast(@StringRes message: Int, duration: Int = Toast.LENGTH_LONG) {
    runOnUiThread {
        Toast.makeText(this, getString(message), duration).show()
    }
}