package com.wzvideni.pateo.music.data

import com.google.gson.JsonArray

// 构建歌手字符串
fun buildSignerString(singerArray: JsonArray): String {
    // 歌手字符串
    var singerString = ""
    // 遍历歌手数组组建歌手字符串
    for (singer in singerArray) {
        // 将singer对应的JSON字符串转换为JsonObject
        val singerObject = singer.asJsonObject
        // 获取单个歌手
        val artist = singerObject.get("name").asString
        // 组建歌手字符串
        singerString = if (singerString == "") {
            artist
        } else {
            "${singerString}、$artist"
        }
    }
    return singerString
}
