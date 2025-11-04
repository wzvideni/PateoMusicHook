package com.wzvideni.pateo.music

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wzvideni.pateo.music.expansion.checkDrawOverlays
import com.wzvideni.pateo.music.expansion.toast
import com.wzvideni.pateo.music.overlay.FloatingLyricsOverlay
import com.wzvideni.pateo.music.ui.theme.PateoMusicHookTheme
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private var isMockMode: Boolean = false
    private var overlayPermissionGranted by mutableStateOf(false)
    private var isOverlayActive by mutableStateOf(false)

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
                    isOverlayActive = isOverlayActive,
                    onRequestOverlayPermission = ::openOverlaySettings,
                    onStartOverlay = ::attachFloatingLyrics,
                    onStopOverlay = ::detachFloatingLyrics
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        overlayPermissionGranted = checkDrawOverlays()
        if (isMockMode && overlayPermissionGranted && !isOverlayActive) {
            attachFloatingLyrics()
        }
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
}

@Composable
private fun MockModeScreen(
    isMockMode: Boolean,
    overlayPermissionGranted: Boolean,
    isOverlayActive: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onStartOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
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
                Text(
                    text = if (overlayPermissionGranted) "悬浮窗权限：已授予" else "悬浮窗权限：未授予",
                    fontWeight = FontWeight.Bold
                )
                Button(onClick = onRequestOverlayPermission) {
                    Text(text = "打开悬浮窗权限设置")
                }
                Button(
                    onClick = onStartOverlay,
                    enabled = overlayPermissionGranted
                ) {
                    Text(text = "启动模拟悬浮歌词")
                }
                Button(
                    onClick = onStopOverlay,
                    enabled = isOverlayActive
                ) {
                    Text(text = "关闭模拟悬浮歌词")
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
