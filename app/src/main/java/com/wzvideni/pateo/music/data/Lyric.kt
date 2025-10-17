package com.wzvideni.pateo.music.data

import androidx.compose.runtime.Stable

@Stable
data class Lyric(
    val timeline: String,
    val millisecond: Int,
    val lyricsList: MutableList<String>,
)