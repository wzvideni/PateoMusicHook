package com.wzvideni.pateo.music.expansion

import android.os.Build
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

// 扩展函数：将时间戳 (毫秒) 转换为指定格式的时间字符串
fun Long.toFormattedDate(pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // Android 8.0 (API 26) 及以上，使用 DateTimeFormatter（线程安全）
        val formatter = DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.systemDefault())
        formatter.format(Instant.ofEpochMilli(this))
    } else {
        // 低于 Android 8.0 (API 26)，使用 SimpleDateFormat（非线程安全）
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        sdf.format(Date(this))
    }
}

fun getDateTime(pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
    return System.currentTimeMillis().toFormattedDate(pattern)
}

fun getLogWithDateTime(log: String, pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
    return "[${System.currentTimeMillis().toFormattedDate(pattern)}] $log"
}

fun getTime(pattern: String = "HH:mm:ss"): String {
    return System.currentTimeMillis().toFormattedDate(pattern)
}

fun getLogWithTime(log: String, pattern: String = "HH:mm:ss"): String {
    return "[${System.currentTimeMillis().toFormattedDate(pattern)}] $log"
}