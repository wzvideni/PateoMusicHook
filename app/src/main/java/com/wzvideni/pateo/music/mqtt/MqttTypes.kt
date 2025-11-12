package com.wzvideni.pateo.music.mqtt

import java.util.Date

data class MqttConfig(
    val host: String = "",
    val port: Int = 1883,
    val username: String = "",
    val password: String = "",
    val clientId: String = ""
)

enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

data class MqttMessageEvent(
    val timestamp: Date,
    val topic: String,
    val payload: String
)

data class MqttTopicSpec(
    val topic: String,
    val qos: Int = 1
)