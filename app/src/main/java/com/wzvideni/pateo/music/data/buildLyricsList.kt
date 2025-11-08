package com.wzvideni.pateo.music.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

// 构建歌词列表
suspend fun buildLyricsList(
    linesList: List<String>,
): List<Lyric> = withContext(Dispatchers.IO) {
    val lyricsList = mutableListOf<Lyric>()
    // 以下变量皆是需要短时间内经常改变的，定义为常量创建的开销可能过大
    // 当前时间轴：00:00.00
    var timeline: String?
    // 分：毫秒形式
    var minutes: Int
    // 秒：毫秒形式
    var seconds: Int
    // 毫秒
    var millisecond: Int
    // 歌词
    var lyric: String
    // 是否存在重复时间轴
    var isExistRepeat: Boolean
    // 集合大小的索引
    var lastIndex: Int
    // 开始索引
    var beginIndex: Int
    // 结束索引
    var endIndex: Int
    // 中间索引
    var midIndex: Int
    // 中间索引歌词
    var midLyric: Lyric
    // 遍历行列表
    for (line in linesList) {
        // 通过正则表达式从每行里面提取出时间轴[00:00.00]
        timeline = Regex(pattern = "\\[\\d+:\\d+.\\d+]").find(line)?.value
        // 如果匹配成功，时间轴不为空
        if (timeline != null) {
            // 分割字符串的[1,3)区间为分，再转换为Int值的毫秒
            minutes = timeline.substring(1, 3).toInt() * 60 * 1000
            // 分割字符串[4,6)区间为秒，再转换为Int值的毫秒
            seconds = timeline.substring(4, 6).toInt() * 1000
            // 分割字符串[7,9)区间为百分之一秒 (centiseconds)，需转换为毫秒 (×10)
            millisecond = timeline.substring(7, 9).toInt() * 10 + minutes + seconds
            // 判断毫秒时间轴键是否已存在于歌词字典，存在则证明此次添加的为翻译，不存在则代表添加的是原歌词
            lyric = line.replace(timeline, "").trim()
            // 跳过写入歌词列表
            if (lyric == "//" || lyric == "") {
                continue
            }
            // 初始化是否存在重复时间轴为真
            isExistRepeat = true
            // 获取最后索引
            lastIndex = lyricsList.lastIndex
            // 获取开始索引
            beginIndex = 0
            // 获取结束索引
            endIndex = lastIndex
            // 二分查找是否有重复的时间轴
            while (lyricsList.isNotEmpty() && beginIndex <= endIndex) {
                // 获取中间索引
                midIndex = (beginIndex + endIndex) / 2
                // 获取中间索引歌词
                midLyric = lyricsList[midIndex]
                // 判断时间轴差是否在指定的范围内（某些歌词和翻译的时间轴对应不上，差几毫秒）
                if (abs(millisecond - midLyric.millisecond) <= 3) {
                    // 清空同时间轴多余歌词（QQ音乐有些歌曲刚开始的词曲信息和头一句歌词用同一个时间轴）
                    if (midLyric.lyricsList.size == 2) {
                        midLyric.lyricsList.clear()
                    }
                    midLyric.lyricsList.add(lyric)
                    isExistRepeat = false
                    break
                    // 否则判断当前遍历的歌词毫秒值是否小于中间索引歌词的毫秒时间轴，是就表示歌词位于前半部分
                } else if (millisecond < midLyric.millisecond) {
                    endIndex = midIndex - 1
                    // 否则判断当前遍历的歌词毫秒值是否大于中间索引歌词的毫秒时间轴，是就表示歌词位于后半部分
                } else {
                    beginIndex = midIndex + 1
                }
            }
            // 查找完歌词列表也不存在重复的时间轴的话就新建Lyrics类实例添加到歌词列表
            if (isExistRepeat) {
                lyricsList.add(Lyric(timeline, millisecond, mutableListOf(lyric)))
            }
        }
    }
    return@withContext lyricsList
}