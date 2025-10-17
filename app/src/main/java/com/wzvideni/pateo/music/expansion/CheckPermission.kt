package com.wzvideni.pateo.music.expansion

import android.content.Context
import android.provider.Settings

fun Context.checkDrawOverlays(): Boolean {
    return Settings.canDrawOverlays(this)
}
