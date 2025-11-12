package com.wzvideni.pateo.music.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Manifest-declared receiver to mirror hook-side broadcasts into module app
 * so Tasker 变量信息窗口能在应用未打开 Activity 时也获取到最新值。
 */
class DebugMirrorReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null) {
            BroadcastSender.handleMirrorIntent(intent)
        }
    }
}