package com.wzvideni.pateo.music.startup

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.wzvideni.pateo.music.mqtt.MqttCenter
import com.wzvideni.pateo.music.traccar.TraccarAutoStarter

/**
 * 接收来自 Hook 侧的内部启动广播，在模块 App 进程中启动需要的后端服务：
 * - Traccar 前台定位服务（在已配置且权限允许的情况下）
 * - MQTT 自动连接（用于接收与镜像调试变量）
 */
class LsposedStartupReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_START_SERVICES = "com.wzvideni.pateo.music.START_SERVICES"
        private const val TAG = "LsposedStartupReceiver"
        private const val TRACCAR_CHANNEL_ID = "org.traccar.client.default"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "onReceive action=${intent.action}")
        val action = intent.action
        if (action != ACTION_START_SERVICES) return
        Log.i(TAG, "Handling ACTION_START_SERVICES")

        runCatching {
            ensureTraccarNotificationChannel(context)
        }.onFailure { Log.w(TAG, "ensureTraccarNotificationChannel failed: ${it.message}") }

        runCatching {
            // 启动 Traccar（只有在配置完整且权限允许时才会启动）
            TraccarAutoStarter.maybeStart(context)
        }.onFailure { Log.e(TAG, "Start Traccar failed", it) }

        runCatching {
            // 确保 MQTT 自动连接开启（幂等）
            MqttCenter.manager.enableAutoConnect(context)
        }.onFailure { Log.e(TAG, "Enable MQTT auto connect failed", it) }
    }

    private fun ensureTraccarNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ContextCompat.getSystemService(context, NotificationManager::class.java) ?: return
        val existing = nm.getNotificationChannel(TRACCAR_CHANNEL_ID)
        if (existing != null) return

        val name = try {
            // 尝试使用 Traccar 客户端的字符串资源，如不可用则兜底
            val resId = context.resources.getIdentifier(
                "channel_default",
                "string",
                context.packageName
            )
            if (resId != 0) context.getString(resId) else "Tracking"
        } catch (e: Throwable) {
            "Tracking"
        }
        val channel = NotificationChannel(
            TRACCAR_CHANNEL_ID,
            name,
            NotificationManager.IMPORTANCE_LOW
        )
        nm.createNotificationChannel(channel)
    }
}