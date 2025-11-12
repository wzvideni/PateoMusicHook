package com.wzvideni.pateo.music.mqtt

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.wzvideni.pateo.music.mqtt.MqttTopicSpec
import android.provider.Settings

private val Context.mqttDataStore by preferencesDataStore(name = "mqtt_settings")

object MqttKeys {
    val HOST = stringPreferencesKey("host")
    val PORT = intPreferencesKey("port")
    val USERNAME = stringPreferencesKey("username")
    val PASSWORD = stringPreferencesKey("password")
    val CLIENT_ID = stringPreferencesKey("client_id")
    val TOPICS = stringSetPreferencesKey("topics")
}

class MqttSettingsStore(private val context: Context) {

    fun observeConfig(): Flow<MqttConfig> = context.mqttDataStore.data.map { prefs: Preferences ->
        MqttConfig(
            host = prefs[MqttKeys.HOST] ?: "",
            port = prefs[MqttKeys.PORT] ?: 1883,
            username = prefs[MqttKeys.USERNAME] ?: "",
            password = prefs[MqttKeys.PASSWORD] ?: "",
            // 默认使用设备信息生成稳定的 clientId，避免未填写时阻塞自动连接
            clientId = prefs[MqttKeys.CLIENT_ID] ?: defaultClientId()
        )
    }

    fun observeTopics(): Flow<Set<String>> = context.mqttDataStore.data.map { prefs ->
        // 兼容旧版本仅存储主题字符串
        prefs[MqttKeys.TOPICS] ?: emptySet()
    }

    fun observeTopicSpecs(): Flow<Set<MqttTopicSpec>> = context.mqttDataStore.data.map { prefs ->
        val set = prefs[MqttKeys.TOPICS] ?: emptySet()
        set.map { entry ->
            val idx = entry.lastIndexOf('|')
            if (idx > 0) {
                val topic = entry.substring(0, idx)
                val qosStr = entry.substring(idx + 1)
                val qos = qosStr.toIntOrNull()?.coerceIn(0, 2) ?: 1
                MqttTopicSpec(topic = topic, qos = qos)
            } else {
                // 旧格式：只有主题，无 QoS
                MqttTopicSpec(topic = entry, qos = 1)
            }
        }.toSet()
    }

    suspend fun saveConfig(config: MqttConfig) {
        context.mqttDataStore.edit { prefs ->
            prefs[MqttKeys.HOST] = config.host
            prefs[MqttKeys.PORT] = config.port
            prefs[MqttKeys.USERNAME] = config.username
            prefs[MqttKeys.PASSWORD] = config.password
            prefs[MqttKeys.CLIENT_ID] = config.clientId
        }
    }

    suspend fun addTopic(topic: String, qos: Int) {
        val safeQos = qos.coerceIn(0, 2)
        context.mqttDataStore.edit { prefs ->
            val set = prefs[MqttKeys.TOPICS]?.toMutableSet() ?: mutableSetOf()
            // 移除可能已有的同名主题（不论旧格式还是带 QoS 格式）
            val filtered = set.filterNot { entry ->
                val idx = entry.lastIndexOf('|')
                val existingTopic = if (idx > 0) entry.substring(0, idx) else entry
                existingTopic == topic
            }.toMutableSet()
            if (topic.isNotBlank()) filtered.add("$topic|$safeQos")
            prefs[MqttKeys.TOPICS] = filtered
        }
    }

    suspend fun removeTopic(topic: String) {
        context.mqttDataStore.edit { prefs ->
            val set = prefs[MqttKeys.TOPICS]?.toMutableSet() ?: mutableSetOf()
            val filtered = set.filterNot { entry ->
                val idx = entry.lastIndexOf('|')
                val existingTopic = if (idx > 0) entry.substring(0, idx) else entry
                existingTopic == topic
            }.toSet()
            prefs[MqttKeys.TOPICS] = filtered
        }
    }

    suspend fun clearTopics() {
        context.mqttDataStore.edit { prefs ->
            prefs[MqttKeys.TOPICS] = emptySet()
        }
    }

    private fun defaultClientId(): String {
        val model = android.os.Build.MODEL?.takeIf { it.isNotBlank() } ?: "Android"
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (_: Exception) { "" }
        val suffix = androidId?.takeLast(6)?.takeIf { it.isNotBlank() }
        return suffix?.let { "$model-$it" } ?: model
    }
}