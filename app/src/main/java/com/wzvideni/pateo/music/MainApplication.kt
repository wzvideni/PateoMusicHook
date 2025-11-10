package com.wzvideni.pateo.music

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.xposed.application.ModuleApplication
import com.wzvideni.pateo.music.mqtt.MqttCenter


class MainApplication : ModuleApplication() {

    val powerManager by lazy { getSystemService(Context.POWER_SERVICE) as android.os.PowerManager }

    override fun onCreate() {
        super.onCreate()

        // Align with Traccar client requirements
        System.setProperty("http.keepAliveDuration", (30 * 60 * 1000).toString())
        ensureTraccarNotificationChannel()

        // 初始化 MQTT 自动连接与主题订阅，使控制台未打开时也能接收消息并更新“变量调试”
        try {
            MqttCenter.manager.enableAutoConnect(this)
        } catch (_: Exception) {}

        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = "package:$packageName".toUri()
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
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