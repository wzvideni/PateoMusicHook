package com.wzvideni.pateo.music.expansion

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.lang.String.format

fun Color.toLongValue(): Long = this.value.toLong()
fun Long.toComposeColor(): Color = Color(this.toULong())

fun String.toComposeColor(): Color {
    val cleanHex = this.removePrefix("#")
    val fullHex = if (cleanHex.length == 6) "FF$cleanHex" else cleanHex
    return Color(fullHex.toLong(16).toInt())
}

fun Color.toHexString(): String {
    return format(defaultLocale, "#%08X", this.toArgb())
}