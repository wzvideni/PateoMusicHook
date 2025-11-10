package com.wzvideni.pateo.music

import android.content.Intent
import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import android.content.pm.PackageManager
import android.widget.Toast
import com.wzvideni.pateo.music.broadcast.BroadcastSender
import androidx.compose.ui.graphics.Color
import com.wzvideni.pateo.music.overlay.FloatingLyricsOverlay
import com.wzvideni.pateo.music.ui.MqttConsoleWindow
import com.wzvideni.pateo.music.ui.TaskerInfoWindow
import com.wzvideni.pateo.music.mqtt.MqttCenter
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private var isMockMode: Boolean = false
    private var overlayPermissionGranted by mutableStateOf(false)
    private var isOverlayActive by mutableStateOf(false)
    private var isTraccarRunning by mutableStateOf(false)
    private var autostartEnabled by mutableStateOf(false)
    private var showMqttConsole by mutableStateOf(false)
    private var showTaskerInfo by mutableStateOf(false)
    private var locationPermissionGranted by mutableStateOf(false)

    private var windowManager: WindowManager? = null
    private var floatingLyricsHandle: FloatingLyricsOverlay.Handle? = null
    private var mainViewModel: MainViewModel? = null
    private var mainDataStore: MainDataStore? = null
    private var debugMirrorReceiver: android.content.BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        isMockMode = resources.getBoolean(R.bool.is_mock_mode)

        if (isMockMode) {
            // 使用 Application 级的 WindowManager，避免某些 ROM 对 Activity 上下文的限制
            windowManager = application.getSystemService(WINDOW_SERVICE) as WindowManager
            mainViewModel = MainViewModel(application)
            mainDataStore = MainDataStore(application)
            val initialPosition = runBlocking {
                FloatingLyricsOverlay.OverlayPosition(
                    requireNotNull(mainDataStore).getOverlayPositionX(),
                    requireNotNull(mainDataStore).getOverlayPositionY()
                )
            }
            floatingLyricsHandle = FloatingLyricsOverlay.create(
                context = application,
                windowManager = requireNotNull(windowManager),
                mainViewModel = requireNotNull(mainViewModel),
                mainDataStore = requireNotNull(mainDataStore),
                isMockMode = true,
                initialPosition = initialPosition
            )
        }

        setContent {
            MockModeScreen(
                    isMockMode = isMockMode,
                    overlayPermissionGranted = overlayPermissionGranted,
                    isOverlayActive = isOverlayActive,
                    isTraccarRunning = isTraccarRunning,
                    autostartEnabled = autostartEnabled,
                    locationPermissionGranted = locationPermissionGranted,
                    onRequestOverlayPermission = ::openOverlaySettings,
                    onStartOverlay = ::attachFloatingLyrics,
                    onStopOverlay = ::detachFloatingLyrics,
                    onOpenTraccarConsole = { startActivity(Intent(this, com.wzvideni.traccar.ui.TraccarActivity::class.java)) },
                    onOpenMqttConsole = { showMqttConsole = true },
                    onOpenTaskerInfo = { showTaskerInfo = true },
                    showMqttConsole = showMqttConsole,
                    onCloseMqttConsole = { showMqttConsole = false },
                    showTaskerInfo = showTaskerInfo,
                    onCloseTaskerInfo = { showTaskerInfo = false },
                    onAutoGrantPermissions = ::autoGrantRequiredPermissions,
                    onOpenLocationPermissionSettings = ::onOpenLocationPermissionSettings
                )
        }

        // 注册内部镜像广播接收器：当 hook 端发送歌词/元数据时，同步更新本应用的变量调试 UI
        debugMirrorReceiver = BroadcastSender.registerDebugMirrorReceiver(this)
    }

    override fun onResume() {
        super.onResume()
        overlayPermissionGranted = Settings.canDrawOverlays(this)
        locationPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        isTraccarRunning = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean("status", false)
        autostartEnabled = runCatching {
            getSharedPreferences("autostart_prefs", MODE_PRIVATE).getBoolean("enabled", false)
        }.getOrElse { false }
        // 默认不自动启动模拟悬浮歌词，保持关闭状态，需手动点击按钮启动
    }

    override fun onDestroy() {
        detachFloatingLyrics()
        // 释放内部镜像广播接收器
        BroadcastSender.unregisterDebugMirrorReceiver(this, debugMirrorReceiver)
        debugMirrorReceiver = null
        super.onDestroy()
    }

    private fun runSuCommand(command: String): Boolean {
        return runCatching {
            val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val exit = proc.waitFor()
            exit == 0
        }.getOrElse { false }
    }

    // Root 一键授权：悬浮窗 + 定位权限
    private fun autoGrantRequiredPermissions() {
        val pkg = packageName
        val results = mutableListOf<String>()

        // 悬浮窗 (SYSTEM_ALERT_WINDOW) 使用 cmd appops 允许，兼容不同 ROM
        val overlayOk =
            runSuCommand("cmd appops set --user 0 ${pkg} SYSTEM_ALERT_WINDOW allow") ||
            runSuCommand("cmd appops set ${pkg} SYSTEM_ALERT_WINDOW allow") ||
            runSuCommand("cmd appops set --user 0 ${pkg} android:system_alert_window allow") ||
            runSuCommand("cmd appops set ${pkg} android:system_alert_window allow")
        results += "悬浮窗权限:${if (overlayOk) "已允许" else "失败"}"

        // 定位权限：授予 runtime 权限 + appops 允许
        val fineGrant = runSuCommand("pm grant ${pkg} android.permission.ACCESS_FINE_LOCATION")
        val coarseGrant = runSuCommand("pm grant ${pkg} android.permission.ACCESS_COARSE_LOCATION")
        val fineOps = runSuCommand("cmd appops set --user 0 ${pkg} ACCESS_FINE_LOCATION allow") ||
                runSuCommand("cmd appops set ${pkg} ACCESS_FINE_LOCATION allow")
        val coarseOps = runSuCommand("cmd appops set --user 0 ${pkg} ACCESS_COARSE_LOCATION allow") ||
                runSuCommand("cmd appops set ${pkg} ACCESS_COARSE_LOCATION allow")
        val locOk = (fineGrant || coarseGrant) || (fineOps || coarseOps)
        results += "定位权限:${if (locOk) "已允许" else "失败"}"

        // 刷新状态
        overlayPermissionGranted = Settings.canDrawOverlays(this)
        locationPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        Toast.makeText(this, results.joinToString(" · "), Toast.LENGTH_SHORT).show()
    }

    private fun attachFloatingLyrics() {
        if (!isMockMode) return
        val windowManager = windowManager ?: return
        val handle = floatingLyricsHandle ?: return
        if (!Settings.canDrawOverlays(this)) {
            overlayPermissionGranted = false
            Toast.makeText(this, "请先授予悬浮窗权限", Toast.LENGTH_SHORT).show()
            return
        }
        overlayPermissionGranted = true
        if (handle.view.parent == null) {
            runCatching {
                windowManager.addView(handle.view, handle.layoutParams)
                FloatingLyricsOverlay.updateLifecycleToResumed()
                // 移除启动提示的 Toast，保持静默
                isOverlayActive = true
                // 广播：标记模拟悬浮歌词已启用，通知 hook 端关闭数据广播
                BroadcastSender.updateOverlayStatus(true)
                BroadcastSender.sendOverlayStatus(this, true)
            }.onFailure { e ->
                val perm = Settings.canDrawOverlays(this)
                val sdk = android.os.Build.VERSION.SDK_INT
                val type = handle.layoutParams.type
                val flags = handle.layoutParams.flags
                Toast.makeText(this, "启动失败：${e.javaClass.simpleName}${e.message?.let { ": $it" } ?: ""}\nperm=${perm}, sdk=${sdk}, type=${type}, flags=${flags}", Toast.LENGTH_SHORT).show()
            }
        } else {
            // 移除重复激活时的 Toast，保持静默
            isOverlayActive = true
            BroadcastSender.updateOverlayStatus(true)
            BroadcastSender.sendOverlayStatus(this, true)
        }
    }

    private fun detachFloatingLyrics() {
        if (!isMockMode) return
        val windowManager = windowManager ?: return
        val handle = floatingLyricsHandle ?: return
        if (handle.view.parent != null) {
            windowManager.removeViewImmediate(handle.view)
            FloatingLyricsOverlay.updateLifecycleToDestroyed()
            // 移除关闭提示的 Toast，保持静默
        }
        isOverlayActive = false
        // 广播：标记模拟悬浮歌词已关闭，允许 hook 端恢复广播
        BroadcastSender.updateOverlayStatus(false)
        BroadcastSender.sendOverlayStatus(this, false)
    }

    private fun openOverlaySettings() {
        runCatching {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }
    }

    private fun onOpenLocationPermissionSettings() {
        runCatching {
            // Try app details first to prompt for permissions
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")
                )
            )
        }.onFailure {
            // Fallback to general location settings
            runCatching { startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
        }
    }
}

@Composable
private fun MockModeScreen(
    isMockMode: Boolean,
    overlayPermissionGranted: Boolean,
    isOverlayActive: Boolean,
    isTraccarRunning: Boolean,
    autostartEnabled: Boolean,
    locationPermissionGranted: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onStartOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
    onOpenTraccarConsole: () -> Unit,
    onOpenMqttConsole: () -> Unit,
    onOpenTaskerInfo: () -> Unit,
    showMqttConsole: Boolean,
    onCloseMqttConsole: () -> Unit,
    showTaskerInfo: Boolean,
    onCloseTaskerInfo: () -> Unit,
    onAutoGrantPermissions: () -> Unit,
    onOpenLocationPermissionSettings: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        if (!isMockMode) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "当前为正常模式，请在 LSPosed 环境下体验完整功能。",
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // 2x2 网格，卡片居中且不占满全屏
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally)) {
                    GridBlock(
                        modifier = Modifier.width(520.dp).heightIn(min = 320.dp, max = 320.dp),
                        title = "打开权限",
                        statusActive = overlayPermissionGranted && locationPermissionGranted,
                        description = "授予悬浮窗与定位权限，确保界面与位置服务正常",
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ButtonWithStatusDot(
                                text = "悬浮窗权限",
                                onClick = onRequestOverlayPermission,
                                active = overlayPermissionGranted
                            )
                            ButtonWithStatusDot(
                                text = "定位权限",
                                onClick = onOpenLocationPermissionSettings,
                                active = locationPermissionGranted
                            )
                            ButtonWithStatusDot(
                                text = "一键开启",
                                onClick = onAutoGrantPermissions,
                                active = overlayPermissionGranted && locationPermissionGranted
                            )
                        }
                    }

                    GridBlock(
                        modifier = Modifier.width(520.dp).heightIn(min = 320.dp, max = 320.dp),
                        title = "Tasker变量信息",
                        statusActive = showTaskerInfo,
                        description = "查看歌词/元数据/MQTT 广播地址与变量说明，支持复制",
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ButtonWithStatusDot(
                                text = if (showTaskerInfo) "关闭Tasker变量信息" else "开启Tasker变量信息",
                                onClick = { if (showTaskerInfo) onCloseTaskerInfo() else onOpenTaskerInfo() },
                                active = showTaskerInfo
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.size(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally)) {
                    GridBlock(
                        modifier = Modifier.width(520.dp).heightIn(min = 320.dp, max = 320.dp),
                        title = "软件工具",
                        statusActive = isTraccarRunning || autostartEnabled,
                        description = "包含 traccar 跟踪控制台、MQTT 接收控制台与开机自启动设置",
                    ) {
                        val ctx = LocalContext.current
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ButtonWithStatusDot(
                                text = "TRACCAR跟踪控制台",
                                onClick = onOpenTraccarConsole,
                                active = isTraccarRunning
                            )
                            ButtonWithStatusDot(
                                text = "MQTT接收控制台",
                                onClick = onOpenMqttConsole,
                                active = (MqttCenter.manager.connectionState.value == com.wzvideni.pateo.music.mqtt.ConnectionState.CONNECTED)
                            )
                            ButtonWithStatusDot(
                                text = "开机自启动设置",
                                onClick = { ctx.startActivity(Intent(ctx, com.wzvideni.pateo.music.autostart.AutoStartSettingsActivity::class.java)) },
                                active = autostartEnabled
                            )
                        }
                    }

                    GridBlock(
                        modifier = Modifier.width(520.dp).heightIn(min = 320.dp, max = 320.dp),
                        title = "调试",
                        statusActive = isOverlayActive,
                        description = "模拟悬浮歌词用于调试",
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { if (!isOverlayActive) onStartOverlay() else onStopOverlay() },
                                enabled = overlayPermissionGranted,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isOverlayActive)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (!isOverlayActive)
                                        MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) { Text(text = if (!isOverlayActive) "启动模拟悬浮歌词" else "关闭模拟悬浮歌词") }
                            Spacer(modifier = Modifier.width(8.dp))
                            StatusDot(active = isOverlayActive)
                        }
                        Spacer(modifier = Modifier.size(12.dp))
                        if (isOverlayActive) {
                            Text(
                                text = "模拟悬浮歌词已启动，可切换应用查看显示效果。",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        // 顶层覆盖渲染控制台
        if (showMqttConsole) {
            MqttConsoleWindow(onClose = onCloseMqttConsole)
        }
        if (showTaskerInfo) {
            TaskerInfoWindow(onClose = onCloseTaskerInfo)
        }
    }
}

@Composable
private fun StatusDot(active: Boolean) {
    val dotSize: Dp = with(LocalDensity.current) { 8f.toDp() }
    val color = if (active) Color(0xFF4CAF50) else Color(0xFFE53935)
    Box(
        modifier = Modifier
            .size(dotSize)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun GridBlock(
    modifier: Modifier = Modifier,
    title: String,
    statusActive: Boolean,
    description: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                StatusDot(active = statusActive)
            }
            Text(text = description, style = MaterialTheme.typography.bodySmall)
            content()
        }
    }
}

@Composable
private fun ButtonWithStatusDot(text: String, onClick: () -> Unit, active: Boolean) {
    val space: Dp = with(LocalDensity.current) { 4f.toDp() }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = onClick, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text(text = text) }
        Spacer(modifier = Modifier.width(space))
        StatusDot(active = active)
    }
}
