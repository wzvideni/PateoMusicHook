package com.wzvideni.pateo.music.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.wzvideni.pateo.music.mqtt.ConnectionState
import com.wzvideni.pateo.music.mqtt.MqttCenter
import com.wzvideni.pateo.music.mqtt.MqttConfig
import com.wzvideni.pateo.music.mqtt.MqttMessageEvent
import com.wzvideni.pateo.music.mqtt.MqttTopicSpec
import com.wzvideni.pateo.music.mqtt.MqttSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun MqttConsoleWindow(onClose: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val store = remember { MqttSettingsStore(context) }

    var config by remember { mutableStateOf(MqttConfig()) }
    var topicInput by remember { mutableStateOf("") }
    var topicQos by remember { mutableStateOf(1) }
    val topics = remember { mutableStateListOf<MqttTopicSpec>() }
    val messages = remember { mutableStateListOf<MqttMessageEvent>() }

    val manager = MqttCenter.manager
    var connectionState by remember { mutableStateOf(ConnectionState.DISCONNECTED) }
    var lastError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        // Load persisted config and topics
        store.observeConfig().collectLatest { cfg -> config = cfg }
    }
    LaunchedEffect(Unit) {
        store.observeTopicSpecs().collectLatest { set ->
            topics.clear(); topics.addAll(set.sortedBy { it.topic })
        }
    }
    LaunchedEffect(Unit) {
        launch(Dispatchers.Main) {
            manager.messages.collectLatest { evt ->
                messages.add(evt)
                // Limit to last 200 to keep UI smooth
                while (messages.size > 200) messages.removeAt(0)
            }
        }
    }
    LaunchedEffect(Unit) {
        launch(Dispatchers.Main) {
            manager.connectionState.collectLatest { st ->
                connectionState = st
                lastError = manager.getLastError()
            }
        }
    }

    // Auto-subscribe persisted topics when connected, avoid duplicates
    LaunchedEffect(connectionState, topics) {
        if (connectionState == ConnectionState.CONNECTED && topics.isNotEmpty()) {
            val current = manager.currentSubscriptions()
            topics.filter { it.topic !in current }.forEach { spec ->
                launch { manager.subscribe(spec.topic, spec.qos) }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header and status
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "MQTT 接收控制台", style = MaterialTheme.typography.titleMedium)
                val statusColor = when (connectionState) {
                    ConnectionState.CONNECTED -> Color(0xFF4CAF50)
                    ConnectionState.CONNECTING -> Color(0xFFFFA000)
                    ConnectionState.ERROR -> Color(0xFFE53935)
                    ConnectionState.DISCONNECTED -> Color(0xFF9E9E9E)
                }
                Box(Modifier.size(10.dp).background(statusColor, RoundedCornerShape(50)))
                if (lastError != null) Text(text = "错误: ${lastError}", color = Color(0xFFE53935))
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onClose) { Text("关闭") }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = config.host, onValueChange = { config = config.copy(host = it) }, label = { Text("Host") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = config.port.toString(), onValueChange = {
                            val p = it.toIntOrNull() ?: 0
                            config = config.copy(port = p)
                        }, label = { Text("Port") }, modifier = Modifier.width(120.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = config.username, onValueChange = { config = config.copy(username = it) }, label = { Text("Username") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = config.password, onValueChange = { config = config.copy(password = it) }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = config.clientId, onValueChange = { config = config.copy(clientId = it) }, label = { Text("ClientID") }, modifier = Modifier.fillMaxWidth().height(56.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            // validate required
                            val ok = config.host.isNotBlank() && config.port > 0 && config.username.isNotBlank() && config.password.isNotBlank() && config.clientId.isNotBlank()
                            if (!ok) return@Button
                            coroutineScope.launch { store.saveConfig(config) }
                        }) { Text("保存配置") }
                        Button(onClick = { coroutineScope.launch { manager.connect(config) } }, enabled = connectionState != ConnectionState.CONNECTED) { Text("连接") }
                        Button(onClick = { coroutineScope.launch { manager.disconnect() } }, enabled = connectionState == ConnectionState.CONNECTED || connectionState == ConnectionState.ERROR) { Text("断开") }
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = topicInput, onValueChange = { topicInput = it }, label = { Text("添加订阅主题") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(
                            value = topicQos.toString(),
                            onValueChange = {
                                topicQos = it.toIntOrNull()?.coerceIn(0, 2) ?: topicQos
                            },
                            label = { Text("QoS(0-2)") },
                            modifier = Modifier.width(100.dp)
                        )
                        Button(onClick = {
                            val t = topicInput.trim()
                            val qos = topicQos.coerceIn(0, 2)
                            if (t.isNotBlank()) {
                                coroutineScope.launch {
                                    store.addTopic(t, qos)
                                    manager.subscribe(t, qos)
                                }
                                topicInput = ""
                                topicQos = 1
                            }
                        }) { Text("添加") }
                        TextButton(onClick = {
                            coroutineScope.launch {
                                // Unsubscribe all current topics immediately and clear persisted topics
                                MqttCenter.manager.currentSubscriptions().forEach { t ->
                                    launch { MqttCenter.manager.unsubscribe(t) }
                                }
                                store.clearTopics()
                            }
                        }) { Text("清空主题") }
                    }
                    // Topics list
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        topics.forEach { spec ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "${spec.topic} (QoS ${spec.qos})", modifier = Modifier.weight(1f))
                                TextButton(onClick = { coroutineScope.launch { store.removeTopic(spec.topic); manager.unsubscribe(spec.topic) } }) { Text("删除") }
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                val listState = rememberLazyListState()
                LaunchedEffect(messages.size) {
                    if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
                }
                Column(Modifier.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "消息接收展示", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { messages.clear() }) { Text("清空消息") }
                    }
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        items(messages) { m ->
                            val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(m.timestamp)
                            Text(text = "[${ts}] ${m.topic}: ${m.payload}")
                        }
                    }
                }
            }
        }
    }
}

// 全屏版本移除悬浮窗拖拽/缩放，使用填充背景的页面布局