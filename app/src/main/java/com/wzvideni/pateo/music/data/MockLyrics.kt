package com.wzvideni.pateo.music.data

fun mockLyrics(): List<Lyric> = listOf(
    Lyric(
        timeline = "[00:00.00]",
        millisecond = 0,
        lyricsList = mutableListOf(
            "模拟播放开始",
            "Mock playback starts"
        )
    ),
    Lyric(
        timeline = "[00:05.00]",
        millisecond = 5000,
        lyricsList = mutableListOf(
            "车窗外的城市闪烁着光影",
            "City lights shimmer beyond the glass"
        )
    ),
    Lyric(
        timeline = "[00:10.00]",
        millisecond = 10_000,
        lyricsList = mutableListOf(
            "指尖跟随旋律轻敲方向盘",
            "Fingers drum softly to the beat"
        )
    ),
    Lyric(
        timeline = "[00:15.00]",
        millisecond = 15_000,
        lyricsList = mutableListOf(
            "心跳和节奏此刻重合",
            "Heartbeat syncs with every rhythm"
        )
    ),
    Lyric(
        timeline = "[00:20.00]",
        millisecond = 20_000,
        lyricsList = mutableListOf(
            "让旋律在空中自由漂浮",
            "Let the melody float through the air"
        )
    ),
    Lyric(
        timeline = "[00:25.00]",
        millisecond = 25_000,
        lyricsList = mutableListOf(
            "这是属于我们的旅途配乐",
            "This soundtrack belongs to our ride"
        )
    ),
    Lyric(
        timeline = "[00:30.00]",
        millisecond = 30_000,
        lyricsList = mutableListOf(
            "模拟播放循环结束",
            "Looping back to start"
        )
    )
)
