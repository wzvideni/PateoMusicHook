package com.wzvideni.pateo.music

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.xposed.application.ModuleApplication


class MainApplication : ModuleApplication() {

    val powerManager by lazy { getSystemService(Context.POWER_SERVICE) as android.os.PowerManager }

    override fun onCreate() {
        super.onCreate()
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
}