package com.wzvideni.pateo.music

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri
import com.highcapable.yukihookapi.hook.log.YLog
import android.util.Log
import com.highcapable.yukihookapi.hook.xposed.application.ModuleApplication


class MainApplication : ModuleApplication() {

    val powerManager by lazy { getSystemService(Context.POWER_SERVICE) as android.os.PowerManager }

    override fun onCreate() {
        super.onCreate()

        // Align with Traccar client requirements
        System.setProperty("http.keepAliveDuration", (30 * 60 * 1000).toString())
        ensureTraccarNotificationChannel()

        // 自动启动 Traccar 跟踪服务（在已配置与已授权定位的前提下）
        try {
            com.wzvideni.pateo.music.traccar.TraccarAutoStarter.maybeStart(this)
        } catch (e: Exception) {
            Log.e("MainApplication", "TraccarAutoStarter.maybeStart failed: ${e.message}", e)
        }

        // 启用 MQTT 自动连接，保持长连
        try {
            com.wzvideni.pateo.music.mqtt.MqttCenter.manager.enableAutoConnect(this)
        } catch (e: Exception) {
            Log.e("MainApplication", "enableAutoConnect failed: ${e.message}", e)
        }

        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = "package:$packageName".toUri()
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("MainApplication", "request ignore battery optimization failed: ${e.message}", e)
            }
        }
        YLog.debug("I am running in module space")
    }

    private fun ensureTraccarNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "default" // org.traccar.client.MainApplication.PRIMARY_CHANNEL
            val channelName = getString(org.traccar.client.R.string.channel_default)
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}