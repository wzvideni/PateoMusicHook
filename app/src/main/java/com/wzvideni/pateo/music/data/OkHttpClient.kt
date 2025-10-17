package com.wzvideni.pateo.music.data

import okhttp3.OkHttpClient

class OkHttpClient {

    companion object {
        // 公共的OkHttpClient
        val okHttpClient: OkHttpClient = OkHttpClient()

        // 用户代理
        const val USER_AGENT =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/22B83"

        // 蓝奏云用户代理
        const val LAN_ZOU_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0"

        // QQ音乐Referer参数
        const val QQ_REFERER = "https://i.y.qq.com"

        // 网易云音乐Referer参数
        const val WYY_REFERER = "https://music.163.com"

        // 蓝奏云链接
        const val LAN_ZOU_URL = "https://wwur.lanzout.com/b01rs66mb"

        // 蓝奏云域名
        val lanZouDomain = Regex("https?://[^/]+").find(LAN_ZOU_URL)?.value

        // 蓝奏云密码
        const val LAN_ZOU_PWD = "xfgc"
    }
}