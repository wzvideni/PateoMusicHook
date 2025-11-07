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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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

    private var windowManager: WindowManager? = null
    private var floatingLyricsHandle: FloatingLyricsOverlay.Handle? = null
    private var mainViewModel: MainViewModel? = null
    private var mainDataStore: MainDataStore? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        isMockMode = resources.getBoolean(R.bool.is_mock_mode)

        if (isMockMode) {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
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
                    onRequestOverlayPermission = ::openOverlaySettings,
                    onOpenLocationPermissionSettings = ::openLocationPermissionSettings,
                    onStartOverlay = ::attachFloatingLyrics,
                    onStopOverlay = ::detachFloatingLyrics,
                    onOpenTraccarConsole = { startActivity(Intent(this, com.wzvideni.traccar.ui.TraccarActivity::class.java)) }
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
            windowManager.addView(handle.view, handle.layoutParams)
            FloatingLyricsOverlay.updateLifecycleToResumed()
        }
        if (!isOverlayActive) {
            toast("启动模拟悬浮歌词")
        }
        isOverlayActive = true
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
    onRequestOverlayPermission: () -> Unit,
    onOpenLocationPermissionSettings: () -> Unit,
    onStartOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
    onOpenTraccarConsole: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!isMockMode) {
            Text(
                text = "当前为正常模式，请在 LSPosed 环境下体验完整功能。",
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ButtonWithStatusDot(
                    text = "打开悬浮窗权限设置",
                    onClick = onRequestOverlayPermission,
                    active = overlayPermissionGranted
                )
                ButtonWithStatusDot(
                    text = "打开定位权限设置",
                    onClick = onOpenLocationPermissionSettings,
                    active = locationPermissionGranted
                )
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
                ) {
                    Text(text = if (!isOverlayActive) "启动模拟悬浮歌词" else "关闭模拟悬浮歌词")
                }
                ButtonWithStatusDot(
                    text = "打开位置跟踪控制台",
                    onClick = onOpenTraccarConsole,
                    active = isTraccarRunning
                )
                val ctx = LocalContext.current
                Button(onClick = { ctx.startActivity(Intent(ctx, com.wzvideni.pateo.music.autostart.AutoStartSettingsActivity::class.java)) }) {
                    Text(text = "开机自启动设置")
                }
                if (isOverlayActive) {
                    Text(
                        text = "模拟悬浮歌词已启动，可切换应用查看显示效果。",
                        textAlign = TextAlign.Center
                    )
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
private fun ButtonWithStatusDot(text: String, onClick: () -> Unit, active: Boolean) {
    val space: Dp = with(LocalDensity.current) { 4f.toDp() }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = onClick) { Text(text = text) }
        Spacer(modifier = Modifier.width(space))
        StatusDot(active = active)
    }
}
