package com.wzvideni.pateo.music.mqtt

import android.util.Log
import android.content.Context
import com.wzvideni.pateo.music.broadcast.BroadcastSender
import com.wzvideni.pateo.music.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import javax.net.ssl.SSLSocketFactory
import java.nio.charset.Charset
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Simple MQTT manager wrapping Paho client.
 * Handles connect/disconnect, subscribe/unsubscribe, and publishes incoming messages to a flow.
 */
class MqttManager {

    private val scope = CoroutineScope(Dispatchers.IO)

    private var client: MqttClient? = null
    // Track subscriptions with QoS
    private val subscriptions = ConcurrentHashMap<String, Int>()
    // Topics desired by persisted settings, independent of UI
    private val wantedTopics = ConcurrentHashMap<String, Int>()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    // 保留最近消息用于控制台重开时回放（调整为 10 条）
    val messages = MutableSharedFlow<MqttMessageEvent>(
        replay = 10,
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val opMutex = Mutex()

    @Volatile
    private var lastError: String? = null

    // Auto-connect support
    @Volatile
    private var autoConnectStarted: Boolean = false
    @Volatile
    private var lastConnectAttemptMs: Long = 0L
    @Volatile
    private var currentConfig: MqttConfig? = null
    @Volatile
    private var appContext: Context? = null

    fun getLastError(): String? = lastError

    private fun formatError(t: Throwable?, phase: String): String {
        if (t == null) return "[$phase] 未知错误"
        val base = "${t.javaClass.simpleName}: ${t.message}"
        val lower = (t.message ?: "").lowercase()
        val hints = when {
            lower.contains("bad user") || lower.contains("not authorized") -> "检查用户名/密码或 Broker 访问权限"
            lower.contains("refused") || lower.contains("failed") -> "检查 Host/Port/TLS 设置及网络防火墙"
            lower.contains("ssl") || lower.contains("handshake") || lower.contains("certificate") -> "TLS 证书问题（自签名需信任或导入CA）"
            lower.contains("identifier") || lower.contains("clientid") || lower.contains("rejected") -> "clientId 不唯一或不合法"
            lower.contains("timeout") || lower.contains("keep alive") -> "网络抖动/NAT导致心跳异常，可调整 keepAlive"
            else -> "检查网络连通性、Broker 可用性、clientId 唯一性、凭证与TLS"
        }
        return "[$phase] ${base}；建议：${hints}"
    }

    suspend fun connect(config: MqttConfig) {
        _connectionState.value = ConnectionState.CONNECTING
        lastError = null
        withContext(Dispatchers.IO) {
            try {
                opMutex.withLock {
                    // 若已有客户端且配置一致，直接复用，避免同 clientId 重复并发连接被服务端踢线
                    val existing = client
                    if (existing != null) {
                        val schemeExisting = try { existing.serverURI?.startsWith("ssl://") == true } catch (_: Exception) { false }
                        val schemeNew = (config.port == 8883)
                        val sameId = try { existing.clientId == config.clientId } catch (_: Exception) { false }
                        val sameHostPort = try {
                            val uri = existing.serverURI ?: ""
                            val hp = uri.removePrefix("ssl://").removePrefix("tcp://")
                            val parts = hp.split(":")
                            parts.size == 2 && parts[0] == config.host && parts[1].toIntOrNull() == config.port
                        } catch (_: Exception) { false }
                        val sameCfg = (schemeExisting == schemeNew) && sameId && sameHostPort
                        val isConnected = try { existing.isConnected } catch (_: Exception) { false }
                        if (sameCfg && isConnected) {
        if (BuildConfig.DEBUG) Log.i("MQTT", "reusing existing connection: ${existing.serverURI} clientId=${existing.clientId}")
                            _connectionState.value = ConnectionState.CONNECTED
                            return@withLock
                        } else {
                            // 配置变更或旧连接未连通，先安全关闭旧客户端
                            try { existing.disconnect() } catch (_: Exception) {}
                            try { existing.close() } catch (_: Exception) {}
                            client = null
                        }
                    }
                    val scheme = if (config.port == 8883) "ssl" else "tcp"
                    val uri = "$scheme://${config.host}:${config.port}"
                    val effectiveClientId = config.clientId.takeIf { it.isNotBlank() } ?: fallbackClientId()
                    val mqttClient = MqttClient(uri, effectiveClientId, MemoryPersistence())
                    val options = MqttConnectOptions().apply {
                        // 使用持久会话，配合自动重连与重新订阅，提升连接持续性
                        isCleanSession = false
                        userName = config.username.takeIf { it.isNotBlank() }
                        password = config.password.takeIf { it.isNotBlank() }?.toCharArray()
                        isAutomaticReconnect = true
                        connectionTimeout = 10
                        keepAliveInterval = 20
                        mqttVersion = MqttConnectOptions.MQTT_VERSION_3_1_1
                        if (scheme == "ssl") {
                            try {
                                socketFactory = SSLSocketFactory.getDefault()
                            } catch (_: Exception) {}
                        }
                    }
        if (BuildConfig.DEBUG) Log.i("MQTT", "connecting uri=${uri}, clientId=${effectiveClientId}, tls=${scheme == "ssl"}")
                    mqttClient.setCallback(object : MqttCallbackExtended {
                        override fun connectionLost(cause: Throwable?) {
                            // 使用自动重连时将状态标记为“连接中”，避免 UI 频繁断开/连接闪烁
                            lastError = formatError(cause, "connectionLost")
        Log.w("MQTT", "connectionLost: ${lastError}")
                            _connectionState.value = ConnectionState.CONNECTING
                        }

                        override fun messageArrived(topic: String?, message: MqttMessage?) {
                            if (topic != null && message != null) {
                                val payload = try {
                                    message.payload.toString(Charset.forName("UTF-8"))
                                } catch (e: Exception) {
                                    // fallback
                                    String(message.payload)
                                }
                                scope.launch(Dispatchers.Default) {
                                    messages.tryEmit(
                                        MqttMessageEvent(
                                            timestamp = Date(),
                                            topic = topic,
                                            payload = payload
                                        )
                                    )
                                    // 已移除 Tasker 事件与 HookState 同步逻辑
                                    // 发送 MQTT 显式广播，供 Tasker 接收解析
                                    try {
                                        val ts = System.currentTimeMillis()
                                        appContext?.let { ctx ->
                                            BroadcastSender.sendMqtt(ctx, topic, payload, ts)
                                        }
                                    } catch (_: Exception) {}
                                }
                            }
                        }

                        override fun deliveryComplete(token: IMqttDeliveryToken?) {
                            // no-op (we don't publish from console)
                        }

                        override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                            _connectionState.value = ConnectionState.CONNECTED
        if (BuildConfig.DEBUG) Log.i("MQTT", "connectComplete reconnect=${reconnect}, serverURI=${serverURI}")
                            // re-subscribe after reconnect
                            scope.launch(Dispatchers.IO) {
                                subscriptions.forEach { (t, qos) ->
                                    try { mqttClient.subscribe(t, qos.coerceIn(0, 2)) } catch (_: Exception) {}
                                }
                                // ensure persisted topics are subscribed as well
                                wantedTopics.forEach { (t, qos) ->
                                    if (!subscriptions.containsKey(t)) {
                                        try {
                                            mqttClient.subscribe(t, qos.coerceIn(0, 2))
                                            subscriptions[t] = qos.coerceIn(0, 2)
                                        } catch (_: Exception) {}
                                    }
                                }
                            }
                        }
                    })
                    mqttClient.connect(options)
                    client = mqttClient
                }
            } catch (e: Exception) {
                lastError = formatError(e, "connect")
                Log.e("MQTT", "connect error: ${lastError}")
                _connectionState.value = ConnectionState.ERROR
            }
        }
    }

    suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            try {
                opMutex.withLock {
                    client?.let { c ->
                        try { c.disconnect() } catch (_: Exception) {}
                        try { c.close() } catch (_: Exception) {}
                    }
                    client = null
                    _connectionState.value = ConnectionState.DISCONNECTED
                }
            } catch (e: Exception) {
                lastError = e.message
                _connectionState.value = ConnectionState.ERROR
            }
        }
    }

    suspend fun subscribe(topic: String, qos: Int = 1) {
        if (topic.isBlank()) return
        val safeQos = qos.coerceIn(0, 2)
        withContext(Dispatchers.IO) {
            opMutex.withLock {
                subscriptions[topic] = safeQos
                try {
                    client?.subscribe(topic, safeQos)
                } catch (e: Exception) {
                    lastError = formatError(e, "subscribe")
                    Log.e("MQTT", "subscribe error: topic=${topic}, qos=${safeQos}, err=${lastError}")
                }
            }
        }
    }

    suspend fun unsubscribe(topic: String) {
        withContext(Dispatchers.IO) {
            opMutex.withLock {
                subscriptions.remove(topic)
                try {
                    client?.unsubscribe(topic)
                } catch (e: Exception) {
                    lastError = formatError(e, "unsubscribe")
                    Log.e("MQTT", "unsubscribe error: topic=${topic}, err=${lastError}")
                }
            }
        }
    }

    fun currentSubscriptions(): Set<String> = subscriptions.keys.toSet()

    private fun isConfigValid(cfg: MqttConfig): Boolean {
        // 放宽校验：仅要求 Host 与端口有效，clientId 可为空（连接时回退）
        return cfg.host.isNotBlank() && cfg.port > 0
    }

    private fun attemptConnectIfNeeded() {
        val cfg = currentConfig ?: return
        if (!isConfigValid(cfg)) return
        val currentClient = client
        val isConnected = try { currentClient?.isConnected ?: false } catch (_: Exception) { false }
        // 如果已有客户端实例，交由 Paho 自动重连处理，避免手动并发重连导致抖动
        if (currentClient != null) return
        if (isConnected || _connectionState.value == ConnectionState.CONNECTING) return
        val now = System.currentTimeMillis()
        if (now - lastConnectAttemptMs < 3000) return // 简单退避，避免高频尝试
        lastConnectAttemptMs = now
        scope.launch { connect(cfg) }
    }

    fun enableAutoConnect(context: android.content.Context) {
        if (autoConnectStarted) return
        autoConnectStarted = true
        appContext = context.applicationContext
        val store = MqttSettingsStore(context)
        // 跟随配置变化记录当前配置
        scope.launch {
            store.observeConfig()
                .distinctUntilChanged()
                .debounce(200)
                .collect { cfg ->
                    currentConfig = cfg
                    attemptConnectIfNeeded()
                }
        }
        // 跟随主题设置变化进行订阅/退订（无需打开控制台）
        scope.launch {
            store.observeTopicSpecs()
                .distinctUntilChanged()
                .collect { set ->
                    updateWantedTopics(set)
                }
        }
        // 连接状态变化时尝试保持连接
        scope.launch {
            connectionState
                .collect { st ->
                    if (st == ConnectionState.DISCONNECTED || st == ConnectionState.ERROR) {
                        attemptConnectIfNeeded()
                    }
                }
        }
    }

    private fun fallbackClientId(): String {
        val model = android.os.Build.MODEL?.takeIf { it.isNotBlank() } ?: "Android"
        val androidId = try {
            appContext?.let { android.provider.Settings.Secure.getString(it.contentResolver, android.provider.Settings.Secure.ANDROID_ID) }
        } catch (_: Exception) { null }
        val suffix = androidId?.takeLast(6)?.takeIf { it.isNotBlank() }
        return suffix?.let { "$model-$it" } ?: model
    }

    private fun updateWantedTopics(set: Set<MqttTopicSpec>) {
        val newMap = set.filter { it.topic.isNotBlank() }
            .associate { it.topic to it.qos.coerceIn(0, 2) }
        val toAdd = newMap.keys.minus(wantedTopics.keys)
        val toRemove = wantedTopics.keys.minus(newMap.keys)
        // Update wanted topics map
        wantedTopics.clear()
        wantedTopics.putAll(newMap)
        scope.launch(Dispatchers.IO) {
            // subscribe newly wanted topics
            toAdd.forEach { t ->
                val qos = newMap[t] ?: 1
                try { subscribe(t, qos) } catch (_: Exception) {}
            }
            // unsubscribe topics no longer wanted
            toRemove.forEach { t ->
                try { unsubscribe(t) } catch (_: Exception) {}
            }
        }
    }
}

object MqttCenter {
    val manager = MqttManager()
}