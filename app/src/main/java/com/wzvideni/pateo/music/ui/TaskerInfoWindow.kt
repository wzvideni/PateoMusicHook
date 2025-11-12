package com.wzvideni.pateo.music.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wzvideni.pateo.music.broadcast.BroadcastSender

@Composable
fun TaskerInfoWindow(onClose: () -> Unit) {
    val context = LocalContext.current

    fun copyToClipboard(label: String, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "已复制: $text", Toast.LENGTH_SHORT).show()
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 头部
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Tasker变量信息",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onClose) { Text("关闭") }
            }

            // 歌词广播说明
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "歌词广播", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.weight(1f))
                        Button(onClick = { copyToClipboard("Broadcast Action", "com.example.music.LYRICS_INFO") }) { Text("复制广播地址") }
                    }
                    Text(text = "动作: com.example.music.LYRICS_INFO", style = MaterialTheme.typography.bodyLarge)
                    val lyricsInfo = BroadcastSender.observeLastLyrics().collectAsState().value
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "%lyric_text — 当前原文歌词行（不包含翻译）", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text(text = "当前值: ${lyricsInfo?.lyricText ?: "暂无"}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "%next_lyric — 下一行原文歌词；无下一行时为 \"end\"", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text(text = "当前值: ${lyricsInfo?.nextLyric ?: "暂无"}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "%timestamp — 接收时间戳（毫秒）", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text(text = "当前值: ${lyricsInfo?.timestamp ?: "暂无"}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        }
                    }
                }
            }

            // 元数据广播说明
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "元数据广播", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.weight(1f))
                        Button(onClick = { copyToClipboard("Broadcast Action", "com.example.music.METADATA_CHANGED") }) { Text("复制广播地址") }
                    }
                    Text(text = "动作: com.example.music.METADATA_CHANGED", style = MaterialTheme.typography.bodyLarge)
                    val metadataInfo = BroadcastSender.observeLastMetadata().collectAsState().value
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "%artist_name — 歌手名称", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text(text = "当前值: ${metadataInfo?.artistName ?: "暂无"}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "%song_name — 歌曲名称", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text(text = "当前值: ${metadataInfo?.songName ?: "暂无"}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "%album_name — 专辑名称", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text(text = "当前值: ${metadataInfo?.albumName ?: "暂无"}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "%cover_url — 专辑封面图片地址", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text(text = "当前值: ${metadataInfo?.coverUrl ?: "暂无"}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "%timestamp — 接收时间戳（毫秒）", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text(text = "当前值: ${metadataInfo?.timestamp ?: "暂无"}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        }
                    }
                }
            }

            // MQTT 广播说明
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "MQTT 广播", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.weight(1f))
                        Button(onClick = { copyToClipboard("Broadcast Action", "com.example.mqtt.MESSAGE_RECEIVED") }) { Text("复制广播地址") }
                    }
                    Text(text = "动作: com.example.mqtt.MESSAGE_RECEIVED", style = MaterialTheme.typography.bodyLarge)
                    val mqttInfo = BroadcastSender.observeLastMqtt().collectAsState().value
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "%mqtt_topic — 接收的主题名称", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text(text = "当前值: ${mqttInfo?.topic ?: "暂无"}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "%mqtt_message — 消息内容文本", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text(text = "当前值: ${mqttInfo?.message ?: "暂无"}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "%timestamp — 接收时间戳（毫秒）", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text(text = "当前值: ${mqttInfo?.timestamp ?: "暂无"}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        }
                    }
                }
            }
        }
    }
}