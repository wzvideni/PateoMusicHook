package com.wzvideni.pateo.music.traccar

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.webkit.URLUtil
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import org.traccar.client.MainFragment
import org.traccar.client.TrackingService

/**
 * 在应用进程启动时尝试自动启动 Traccar 跟踪服务：
 * - 仅当已配置设备ID与服务器URL且已授予定位权限时触发；
 * - 若偏好中 [status] 已为 true，则确保服务处于前台运行状态；
 * - 否则在满足条件时将 [status] 置为 true 并启动服务。
 */
object TraccarAutoStarter {

    fun maybeStart(context: Context) {
        // 预设默认偏好值，保证 KEY_* 存在
        PreferenceManager.setDefaultValues(context, org.traccar.client.R.xml.preferences, false)

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val alreadyOn = prefs.getBoolean(MainFragment.KEY_STATUS, false)

        val deviceId = prefs.getString(MainFragment.KEY_DEVICE, null)?.trim().orEmpty()
        val url = prefs.getString(MainFragment.KEY_URL, null)?.trim().orEmpty()
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val urlValid = isValidServerUrl(url)
        val canStart = deviceId.isNotBlank() && urlValid && fineGranted

        Log.i("TraccarAutoStarter", "check: deviceId=${deviceId.isNotBlank()}, urlValid=${urlValid}, fineGranted=${fineGranted}, alreadyOn=${alreadyOn}, url='${url}'")

        if (alreadyOn) {
            // 偏好标记已开启，确保服务前台运行
            ContextCompat.startForegroundService(context, Intent(context, TrackingService::class.java))
            return
        }

        if (canStart) {
            prefs.edit().putBoolean(MainFragment.KEY_STATUS, true).apply()
            ContextCompat.startForegroundService(context, Intent(context, TrackingService::class.java))
        }
    }

    private fun isValidServerUrl(userUrl: String): Boolean {
        if (!URLUtil.isValidUrl(userUrl)) return false
        if (!(URLUtil.isHttpUrl(userUrl) || URLUtil.isHttpsUrl(userUrl))) return false
        val port = try { android.net.Uri.parse(userUrl).port } catch (_: Exception) { -1 }
        return (port == -1 || port in 1..65535)
    }
}