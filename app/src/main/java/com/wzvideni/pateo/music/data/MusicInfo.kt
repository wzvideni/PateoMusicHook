package com.wzvideni.pateo.music.data

import androidx.compose.runtime.Stable


// 音乐信息
@Stable
data class MusicInfo(
    // QQ音乐的专辑信息为专辑ID
    // 网易云音乐的专辑信息为专辑图片链接
    val albumInfo: String,
    val musicId: String,
    val title: String,
    val singerString: String,
    val album: String,
)