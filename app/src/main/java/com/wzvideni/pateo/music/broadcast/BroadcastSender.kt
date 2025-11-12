package com.wzvideni.pateo.music.broadcast

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object BroadcastSender {
    const val TASKER_PACKAGE = "net.dinglisch.android.taskerm"
    const val MODULE_PACKAGE = "com.wzvideni.pateo.music"

    // Broadcast actions for Tasker Intent Received
    const val ACTION_LYRICS_INFO = "com.example.music.LYRICS_INFO"
    const val ACTION_METADATA_CHANGED = "com.example.music.METADATA_CHANGED"
    const val ACTION_MQTT_MESSAGE = "com.example.mqtt.MESSAGE_RECEIVED"

    // Internal mirror actions to update module app UI with values from hook process
    const val ACTION_LYRICS_INFO_INTERNAL = "com.wzvideni.pateo.music.internal.LYRICS_INFO"
    const val ACTION_METADATA_CHANGED_INTERNAL = "com.wzvideni.pateo.music.internal.METADATA_CHANGED"
    const val ACTION_MQTT_MESSAGE_INTERNAL = "com.wzvideni.pateo.music.internal.MQTT_MESSAGE"

    // Overlay status coordination between mock overlay and hook
    const val ACTION_OVERLAY_STATUS = "com.wzvideni.pateo.music.MOCK_OVERLAY_STATUS"

    @Volatile
    private var overlayMockEnabled: Boolean = false

    fun updateOverlayStatus(enabled: Boolean) {
        overlayMockEnabled = enabled
    }

    fun isOverlayMockEnabled(): Boolean = overlayMockEnabled

    // 当前已发送的变量数据（用于 UI 调试展示）
    data class LyricsInfo(val lyricText: String, val nextLyric: String, val timestamp: Long)
    data class MetadataInfo(
        val artistName: String,
        val songName: String,
        val albumName: String,
        val coverUrl: String,
        val timestamp: Long
    )
    data class MqttInfo(val topic: String, val message: String, val timestamp: Long)

    private val _lastLyrics: MutableStateFlow<LyricsInfo?> = MutableStateFlow(null)
    private val _lastMetadata: MutableStateFlow<MetadataInfo?> = MutableStateFlow(null)
    private val _lastMqtt: MutableStateFlow<MqttInfo?> = MutableStateFlow(null)

    fun observeLastLyrics(): StateFlow<LyricsInfo?> = _lastLyrics
    fun observeLastMetadata(): StateFlow<MetadataInfo?> = _lastMetadata
    fun observeLastMqtt(): StateFlow<MqttInfo?> = _lastMqtt

    fun sendLyrics(context: Context, currentLyric: String?, nextLyric: String?) {
        val ts = System.currentTimeMillis()
        val next = nextLyric?.takeIf { it.isNotBlank() } ?: "end"
        val cur = currentLyric ?: ""
        val intent = Intent(ACTION_LYRICS_INFO).apply {
            setPackage(TASKER_PACKAGE)
            putExtra("lyric_text", cur)
            // 下一句为空则显示 "end"
            putExtra("next_lyric", next)
            putExtra("timestamp", ts)
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        }
        context.sendBroadcast(intent)
        // Mirror for module app UI
        val mirror = Intent(ACTION_LYRICS_INFO_INTERNAL).apply {
            setPackage(MODULE_PACKAGE)
            putExtra("lyric_text", cur)
            putExtra("next_lyric", next)
            putExtra("timestamp", ts)
        }
        context.sendBroadcast(mirror)
        _lastLyrics.value = LyricsInfo(cur, next, ts)
    }

    fun sendMetadata(
        context: Context,
        artist: String?,
        song: String?,
        album: String?,
        coverUrl: String?
    ) {
        val ts = System.currentTimeMillis()
        val intent = Intent(ACTION_METADATA_CHANGED).apply {
            setPackage(TASKER_PACKAGE)
            putExtra("artist_name", artist ?: "")
            putExtra("song_name", song ?: "")
            putExtra("album_name", album ?: "")
            putExtra("cover_url", coverUrl ?: "")
            putExtra("timestamp", ts)
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        }
        context.sendBroadcast(intent)
        // Mirror for module app UI
        val mirror = Intent(ACTION_METADATA_CHANGED_INTERNAL).apply {
            setPackage(MODULE_PACKAGE)
            putExtra("artist_name", artist ?: "")
            putExtra("song_name", song ?: "")
            putExtra("album_name", album ?: "")
            putExtra("cover_url", coverUrl ?: "")
            putExtra("timestamp", ts)
        }
        context.sendBroadcast(mirror)
        _lastMetadata.value = MetadataInfo(
            artistName = artist ?: "",
            songName = song ?: "",
            albumName = album ?: "",
            coverUrl = coverUrl ?: "",
            timestamp = ts
        )
    }

    fun sendMqtt(context: Context, topic: String, message: String, timestamp: Long = System.currentTimeMillis()) {
        val intent = Intent(ACTION_MQTT_MESSAGE).apply {
            setPackage(TASKER_PACKAGE)
            putExtra("mqtt_topic", topic)
            putExtra("mqtt_message", message)
            putExtra("timestamp", timestamp)
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        }
        context.sendBroadcast(intent)
        // Mirror for module app UI
        val mirror = Intent(ACTION_MQTT_MESSAGE_INTERNAL).apply {
            setPackage(MODULE_PACKAGE)
            putExtra("mqtt_topic", topic)
            putExtra("mqtt_message", message)
            putExtra("timestamp", timestamp)
        }
        context.sendBroadcast(mirror)
        _lastMqtt.value = MqttInfo(topic, message, timestamp)
    }

    fun sendOverlayStatus(context: Context, enabled: Boolean) {
        val intent = Intent(ACTION_OVERLAY_STATUS).apply {
            putExtra("enabled", enabled)
        }
        context.sendBroadcast(intent)
    }

    // Register a receiver in module app to keep UI flows updated when broadcasts originate from hook process
    // Also provide a public handler so manifest-declared receivers can reuse the same logic
    fun handleMirrorIntent(intent: Intent) {
        when (intent.action) {
            ACTION_LYRICS_INFO_INTERNAL -> {
                val cur = intent.getStringExtra("lyric_text") ?: ""
                val next = intent.getStringExtra("next_lyric") ?: "end"
                val ts = intent.getLongExtra("timestamp", System.currentTimeMillis())
                _lastLyrics.value = LyricsInfo(cur, next, ts)
            }
            ACTION_METADATA_CHANGED_INTERNAL -> {
                val artist = intent.getStringExtra("artist_name") ?: ""
                val song = intent.getStringExtra("song_name") ?: ""
                val album = intent.getStringExtra("album_name") ?: ""
                val cover = intent.getStringExtra("cover_url") ?: ""
                val ts = intent.getLongExtra("timestamp", System.currentTimeMillis())
                _lastMetadata.value = MetadataInfo(artist, song, album, cover, ts)
            }
            ACTION_MQTT_MESSAGE_INTERNAL -> {
                val topic = intent.getStringExtra("mqtt_topic") ?: ""
                val message = intent.getStringExtra("mqtt_message") ?: ""
                val ts = intent.getLongExtra("timestamp", System.currentTimeMillis())
                _lastMqtt.value = MqttInfo(topic, message, ts)
            }
        }
    }

    fun registerDebugMirrorReceiver(context: Context): android.content.BroadcastReceiver {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val action = intent?.action ?: return
                // Reuse shared handler
                handleMirrorIntent(intent)
            }
        }
        val filter = android.content.IntentFilter().apply {
            addAction(ACTION_LYRICS_INFO_INTERNAL)
            addAction(ACTION_METADATA_CHANGED_INTERNAL)
            addAction(ACTION_MQTT_MESSAGE_INTERNAL)
        }
        context.registerReceiver(receiver, filter)
        return receiver
    }

    fun unregisterDebugMirrorReceiver(context: Context, receiver: android.content.BroadcastReceiver?) {
        if (receiver != null) runCatching { context.unregisterReceiver(receiver) }
    }
}