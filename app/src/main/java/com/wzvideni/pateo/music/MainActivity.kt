package com.wzvideni.pateo.music

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.wzvideni.pateo.music.expansion.checkDrawOverlays
import com.wzvideni.pateo.music.expansion.toast
import com.wzvideni.pateo.music.overlay.FloatingLyricsOverlay
import com.wzvideni.pateo.music.ui.theme.PateoMusicHookTheme
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private var isMockMode: Boolean = false
    private var overlayPermissionGranted by mutableStateOf(false)
    private var locationPermissionGranted by mutableStateOf(false)
    private var isOverlayActive by mutableStateOf(false)
    private var isTraccarRunning by mutableStateOf(false)
    private var isTaskerMockEnabled by mutableStateOf(true)
    private var autostartEnabled by mutableStateOf(false)

    private var windowManager: WindowManager? = null
    private var floatingLyricsHandle: FloatingLyricsOverlay.Handle? = null
    private var mainViewModel: MainViewModel? = null
    private var mainDataStore: MainDataStore? = null

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
            PateoMusicHookTheme {
                MockModeScreen(
                    isMockMode = isMockMode,
                    overlayPermissionGranted = overlayPermissionGranted,
                    locationPermissionGranted = locationPermissionGranted,
                    isOverlayActive = isOverlayActive,
                    isTraccarRunning = isTraccarRunning,
                    autostartEnabled = autostartEnabled,
                    taskerMockEnabled = isTaskerMockEnabled,
                    onRequestOverlayPermission = ::openOverlaySettings,
                    onOpenLocationPermissionSettings = ::openLocationPermissionSettings,
                    onStartOverlay = ::attachFloatingLyrics,
                    onStopOverlay = ::detachFloatingLyrics,
                    onOpenTraccarConsole = { startActivity(Intent(this, com.wzvideni.traccar.ui.TraccarActivity::class.java)) },
                    onToggleTaskerMock = {
                        isTaskerMockEnabled = !isTaskerMockEnabled
                        com.wzvideni.pateo.music.tasker.LyricsTaskerEvent.allowInMockMode = isTaskerMockEnabled
                        com.wzvideni.pateo.music.tasker.MetadataTaskerEvent.allowInMockMode = isTaskerMockEnabled
                        toast(if (isTaskerMockEnabled) "模拟模式将触发 Tasker 事件" else "模拟模式不再触发 Tasker 事件")
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        overlayPermissionGranted = checkDrawOverlays()
        locationPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        isTraccarRunning = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean("status", false)
        autostartEnabled = runCatching {
            getSharedPreferences("autostart_prefs", MODE_PRIVATE).getBoolean("enabled", false)
        }.getOrElse { false }
        com.wzvideni.pateo.music.tasker.LyricsTaskerEvent.allowInMockMode = isTaskerMockEnabled
        com.wzvideni.pateo.music.tasker.MetadataTaskerEvent.allowInMockMode = isTaskerMockEnabled
        // 默认不自动启动模拟悬浮歌词，保持关闭状态，需手动点击按钮启动
    }

    override fun onDestroy() {
        detachFloatingLyrics()
        super.onDestroy()
    }

    private fun attachFloatingLyrics() {
        if (!isMockMode) return
        val windowManager = windowManager ?: return
        val handle = floatingLyricsHandle ?: return
        if (!checkDrawOverlays()) {
            overlayPermissionGranted = false
            toast("请先授予悬浮窗权限")
            return
        }
        overlayPermissionGranted = true
        if (handle.view.parent == null) {
            runCatching {
                windowManager.addView(handle.view, handle.layoutParams)
                FloatingLyricsOverlay.updateLifecycleToResumed()
                if (!isOverlayActive) {
                    toast("启动模拟悬浮歌词")
                }
                isOverlayActive = true
            }.onFailure { e ->
                val perm = checkDrawOverlays()
                val sdk = android.os.Build.VERSION.SDK_INT
                val type = handle.layoutParams.type
                val flags = handle.layoutParams.flags
                toast("启动失败：${e.javaClass.simpleName}${e.message?.let { ": $it" } ?: ""}\nperm=${perm}, sdk=${sdk}, type=${type}, flags=${flags}")
            }
        } else {
            if (!isOverlayActive) {
                toast("启动模拟悬浮歌词")
            }
            isOverlayActive = true
        }
    }

    private fun detachFloatingLyrics() {
        if (!isMockMode) return
        val windowManager = windowManager ?: return
        val handle = floatingLyricsHandle ?: return
        if (handle.view.parent != null) {
            windowManager.removeViewImmediate(handle.view)
            FloatingLyricsOverlay.updateLifecycleToDestroyed()
            toast("关闭模拟悬浮歌词")
        }
        isOverlayActive = false
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

    private fun openLocationPermissionSettings() {
        runCatching {
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")
                )
            )
        }
    }
}

@Composable
private fun MockModeScreen(
    isMockMode: Boolean,
    overlayPermissionGranted: Boolean,
    locationPermissionGranted: Boolean,
    isOverlayActive: Boolean,
    isTraccarRunning: Boolean,
    autostartEnabled: Boolean,
    taskerMockEnabled: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onOpenLocationPermissionSettings: () -> Unit,
    onStartOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
    onOpenTraccarConsole: () -> Unit,
    onToggleTaskerMock: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isMockMode) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "当前为正常模式，请在 LSPosed 环境下体验完整功能。",
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
            return
        }

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
                    }
                }

                GridBlock(
                    modifier = Modifier.width(520.dp).heightIn(min = 320.dp, max = 320.dp),
                    title = "触发 Tasker 事件",
                    statusActive = taskerMockEnabled,
                    description = "默认开启。\n\n动作（Action）：\n- 歌词事件： com.wzvideni.pateo.music.LYRICS_CHANGED\n- 歌曲信息事件： com.wzvideni.pateo.music.SONGINFO_CHANGED\n\nSONGINFO_CHANGED 提供的变量：\n- %singer_name ：歌手名\n- %song_name ：歌名\n- %album_name ：专辑名\n- %album_pic ：专辑封面 URL\n\nLYRICS_CHANGED 提供的变量：\n- %lyric_text ：当前歌词文本（与 %lyric_current 相同，保留兼容）\n- %lyric_current ：当前行歌词文本\n- %lyric_second ：翻译/第二行歌词（可能不存在）",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ButtonWithStatusDot(
                            text = "切换开关",
                            onClick = onToggleTaskerMock,
                            active = taskerMockEnabled
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusDot(active = taskerMockEnabled)
                            Text(text = "LYRICS_CHANGED（歌词事件）跟随开关启用", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusDot(active = taskerMockEnabled)
                            Text(text = "SONGINFO_CHANGED（歌曲信息事件）跟随开关启用", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.size(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally)) {
                GridBlock(
                    modifier = Modifier.width(520.dp).heightIn(min = 320.dp, max = 320.dp),
                    title = "位置跟踪与自启",
                    statusActive = isTraccarRunning || autostartEnabled,
                    description = "Traccar 位置服务与开机自启动",
                ) {
                    val ctx = LocalContext.current
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ButtonWithStatusDot(
                            text = "打开位置跟踪控制台",
                            onClick = onOpenTraccarConsole,
                            active = isTraccarRunning
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
                    description = "模拟悬浮歌词用于调试，1920×1080 适配",
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
        Button(onClick = onClick) { Text(text = text) }
        Spacer(modifier = Modifier.width(space))
        StatusDot(active = active)
    }
}
