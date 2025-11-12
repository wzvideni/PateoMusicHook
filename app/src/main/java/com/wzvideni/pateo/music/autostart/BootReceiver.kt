package com.wzvideni.pateo.music.autostart

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.wzvideni.pateo.music.BuildConfig
import com.wzvideni.pateo.music.mqtt.MqttCenter
import com.wzvideni.pateo.music.traccar.TraccarAutoStarter
import com.wzvideni.pateo.music.accessibility.AccessibilityMonitorService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val supported = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_LOCKED_BOOT_COMPLETED
        )
        if (action !in supported) return

        // 先行尝试启动后台服务：MQTT 自动连接与 Traccar 前台服务（满足条件时）
        runCatching {
            MqttCenter.manager.enableAutoConnect(context)
        }.onFailure { Log.w("BootReceiver", "Enable MQTT auto connect at boot failed: ${it.message}") }
        runCatching {
            TraccarAutoStarter.maybeStart(context)
        }.onFailure { Log.w("BootReceiver", "Start Traccar at boot failed: ${it.message}") }

        // 启动无障碍持续监控（若启用）
        runCatching { AccessibilityMonitorService.startIfEnabled(context) }


        runCatching {
            // 使用设备受保护存储以支持 LOCKED_BOOT_COMPLETED
            val deviceCtx = context.createDeviceProtectedStorageContext()
            val dprefs = deviceCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            val enabled = dprefs.getBoolean(KEY_ENABLED, false) || prefs.getBoolean(KEY_ENABLED, false)
            if (!enabled) return

            val pkg = dprefs.getString(KEY_PACKAGE, null) ?: prefs.getString(KEY_PACKAGE, null)
            val component = dprefs.getString(KEY_COMPONENT, null) ?: prefs.getString(KEY_COMPONENT, null)
            if (pkg.isNullOrBlank()) return

            val launchIntent = when {
                !component.isNullOrBlank() -> Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(ComponentName(pkg, component))
                else -> context.packageManager.getLaunchIntentForPackage(pkg)
            }

            val intentToStart = launchIntent ?: run {
                // 兜底：查询该包的 LAUNCHER Activity
                val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(pkg)
                val activities = context.packageManager.queryIntentActivities(query, 0)
                val ri = activities.firstOrNull() ?: return
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(ComponentName(ri.activityInfo.packageName, ri.activityInfo.name))
            }

            intentToStart.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            context.startActivity(intentToStart)
        }.onFailure { e ->
            Log.e("BootReceiver", "Autostart failed", e)
        }
    }

    companion object {
        private const val PREFS_NAME = "autostart_prefs"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_PACKAGE = "package"
        private const val KEY_COMPONENT = "component"
    }
}