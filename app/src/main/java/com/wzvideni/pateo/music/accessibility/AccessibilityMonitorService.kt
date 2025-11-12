package com.wzvideni.pateo.music.accessibility

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.provider.Settings.Secure
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * 持续监控无障碍状态的前台服务：
 * - 监听 Settings.Secure.enabled_accessibility_services / accessibility_enabled 变化
 * - 当被关闭或缺少已锁定服务时，尝试自动重新开启（root/WRITE_SECURE_SETTINGS）
 * - 失败时弹出提示并可引导到系统设置
 */
class AccessibilityMonitorService : Service() {

    private var observer: ContentObserver? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTI_ID, buildNotification())
        registerObservers()
        // 启动时立即校验一次
        runCatching { checkAndFix() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 前台常驻，监听即可
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { contentResolver.unregisterContentObserver(observer!!) }
        observer = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun registerObservers() {
        val handler = Handler(Looper.getMainLooper())
        observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                runCatching { checkAndFix() }
            }
        }
        val uri1 = Secure.getUriFor(Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val uri2 = Secure.getUriFor(Secure.ACCESSIBILITY_ENABLED)
        contentResolver.registerContentObserver(uri1, false, observer!!)
        contentResolver.registerContentObserver(uri2, false, observer!!)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val ch = NotificationChannel(CHANNEL_ID, "无障碍监控", NotificationManager.IMPORTANCE_MIN)
            ch.setShowBadge(false)
            nm.createNotificationChannel(ch)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("正在监控无障碍服务")
            .setContentText("确保锁定的服务未被关闭")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun checkAndFix() {
        val prefs = getSharedPreferences("accessibility_prefs", MODE_PRIVATE)
        val locked = (prefs.getStringSet("locked_service_ids", emptySet()) ?: emptySet()).toMutableSet()
        val legacy = prefs.getString("locked_service_id", "") ?: ""
        if (locked.isEmpty() && legacy.isNotBlank()) locked.add(legacy)
        val autoEnable = prefs.getBoolean("auto_enable", true)
        if (locked.isEmpty()) return

        val current = Secure.getString(contentResolver, Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        val accEnabled = Secure.getInt(contentResolver, Secure.ACCESSIBILITY_ENABLED, 0)
        val enabledSet = current.split(":").filter { it.isNotBlank() }.map { it.trim() }.toSet()
        val allEnabled = accEnabled == 1 && locked.all { enabledSet.contains(it) }
        if (allEnabled) return
        if (!autoEnable) return

        // 尝试自动开启，并保留已有项
        val union = enabledSet.toMutableSet().apply { addAll(locked) }
        val newList = union.joinToString(":")
        val ok1 = runSuCommand("cmd settings put secure enabled_accessibility_services ${newList}") ||
                runSuCommand("settings put secure enabled_accessibility_services ${newList}")
        val ok2 = if (accEnabled != 1) {
            runSuCommand("cmd settings put secure accessibility_enabled 1") || runSuCommand("settings put secure accessibility_enabled 1")
        } else true

        val nowCurrent = Secure.getString(contentResolver, Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        val nowAccEnabled = Secure.getInt(contentResolver, Secure.ACCESSIBILITY_ENABLED, 0)
        val nowEnabledSet = nowCurrent.split(":").filter { it.isNotBlank() }.map { it.trim() }.toSet()
        val success = ok1 && ok2 && nowAccEnabled == 1 && locked.all { nowEnabledSet.contains(it) }
        if (!success) {
            Toast.makeText(this, "未能自动开启所有无障碍服务，请在系统设置中手动开启。", Toast.LENGTH_SHORT).show()
        }
    }

    private fun runSuCommand(command: String): Boolean {
        return runCatching {
            val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val exit = proc.waitFor()
            exit == 0
        }.getOrElse { false }
    }

    companion object {
        private const val CHANNEL_ID = "ACC_MONITOR"
        private const val NOTI_ID = 0xA110

        fun startIfEnabled(context: Context) {
            val prefs = context.getSharedPreferences("accessibility_prefs", MODE_PRIVATE)
            val monitor = prefs.getBoolean("monitor_enabled", true)
            if (!monitor) return
            ContextCompat.startForegroundService(context, Intent(context, AccessibilityMonitorService::class.java))
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, AccessibilityMonitorService::class.java)) }
        }
    }
}