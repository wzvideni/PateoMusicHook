package com.wzvideni.pateo.music.expansion

import java.util.Locale

fun String.getValueOf(field: String): String? {
    val regex = Regex("""$field=([^,)]*)""")
    val match = regex.find(this)
    return match?.groupValues?.get(1)?.trim()?.takeIf { it != "null" && it.isNotEmpty() }
}

val defaultLocale: Locale by lazy { Locale.getDefault() }

