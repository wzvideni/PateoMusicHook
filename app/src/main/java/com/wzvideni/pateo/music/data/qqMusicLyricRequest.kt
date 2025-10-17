package com.wzvideni.pateo.music.data

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.Response

// 根据QQ音乐ID请求歌词
suspend fun qqMusicLyricRequest(songMid: String): List<Lyric> =
    withContext(Dispatchers.IO) {
        // 根据音乐ID构建请求链接
        val url = "https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg" +
                "?songmid=$songMid" +
                "&g_tk=5381" +
                "&format=json"

        // 构建QQ音乐歌词请求
        val qqMusicLyricsRequest = Request.Builder()
            .url(url)
            .header("Referer", OkHttpClient.QQ_REFERER)
            .header("User-Agent", OkHttpClient.USER_AGENT)
            .build()

        // 执行请求并获取响应，使用use自动释放资源
        OkHttpClient.okHttpClient.newCall(qqMusicLyricsRequest).execute()
            .use { response: Response ->
                if (response.isSuccessful) {
                    response.body?.string()?.let {
                        // 使用jsonString构建jsonObject
                        val jsonObject = Gson().fromJson(it, JsonObject::class.java)
                        // 从jsonObject获取歌词和翻译分别按换行符分割后的列表
                        // 解密base64字符串为字节数组再转换为字符串
                        val lyricList =
                            jsonObject.get("lyric")?.asString?.let { lyric: String ->
                                String(Base64.decode(lyric, Base64.DEFAULT))
                            }?.split("\n")
                        val transList =
                            jsonObject.get("trans")?.asString?.let { trans: String ->
                                String(Base64.decode(trans, Base64.DEFAULT))
                            }?.split("\n")

                        // 把歌词列表和翻译列表相加组成行列表，如何其中有一个为空则不相加，返回不为空的那个列表
                        // 都为空就返回空列表
                        val linesList = if (lyricList != null && transList != null) {
                            lyricList + transList
                        } else lyricList ?: (transList ?: emptyList())

                        // 行列表不为空才构建歌词列表
                        if (linesList.isNotEmpty()) {
                            // 返回歌词列表
                            return@withContext buildLyricsList(linesList)
                        }
                    }
                }
            }
        return@withContext emptyList()
    }