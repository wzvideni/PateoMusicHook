package com.wzvideni.pateo.music

import android.app.Application
import android.content.Context.WINDOW_SERVICE
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import com.highcapable.betterandroid.system.extension.tool.AndroidVersion
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.configs
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.factory.registerModuleAppActivities
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import com.wzvideni.pateo.music.expansion.checkDrawOverlays
import com.wzvideni.pateo.music.expansion.getValueOf
import com.wzvideni.pateo.music.expansion.toast
import com.wzvideni.pateo.music.overlay.FloatingLyricsOverlay
import kotlinx.coroutines.runBlocking

@InjectYukiHookWithXposed
class MainHookEntry : IYukiHookXposedInit {

    lateinit var application: Application
    lateinit var mainViewModel: MainViewModel
    lateinit var mainDataStore: MainDataStore
    lateinit var floatingLyricsHandle: FloatingLyricsOverlay.Handle
    lateinit var floatingSettingsView: ComposeView


    lateinit var windowManager: WindowManager

    override fun onInit() = configs {
        debugLog {
            isEnable = false
            isRecord = false
            elements(TAG, PRIORITY, PACKAGE_NAME, USER_ID)
        }
        isDebug = true
        isEnableModuleAppResourcesCache = true
        isEnableHookSharedPreferences = false
        isEnableDataChannel = true
    }

    fun addFloatingComposeView(application: Application) {
        if (application.checkDrawOverlays()) {
            val handle = floatingLyricsHandle
            if (handle.view.parent == null) {
                windowManager.addView(handle.view, handle.layoutParams)
//                windowManager.addView(floatingSettingsView, unlockedWindowParams)
                FloatingLyricsOverlay.updateLifecycleToResumed()
                application.toast("启动悬浮窗")
            }
        } else {
            application.toast("请授予悬浮窗权限")
        }
    }


    override fun onHook() = encase {
        // 装载需要 Hook 的 APP
        loadApp(name = "com.sgmw.lingos.music") {

            // 注册模块 Activity 代理
            onAppLifecycle {
                onCreate {
                    if (AndroidVersion.isAtLeast(AndroidVersion.N)) registerModuleAppActivities()
                    application = this
                    windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
                    mainViewModel = MainViewModel(this)
                    mainDataStore = MainDataStore(this)
                    val initialPosition = runBlocking {
                        FloatingLyricsOverlay.OverlayPosition(
                            mainDataStore.getOverlayPositionX(),
                            mainDataStore.getOverlayPositionY()
                        )
                    }
                    floatingLyricsHandle = FloatingLyricsOverlay.create(
                        context = this,
                        windowManager = windowManager,
                        mainViewModel = mainViewModel,
                        mainDataStore = mainDataStore,
                        isMockMode = false,
                        initialPosition = initialPosition
                    )

//                    floatingSettingsView = ComposeView(this).apply {
//                        setViewTreeLifecycleOwner(FloatingWindowLifecycleOwner)
//                        setViewTreeSavedStateRegistryOwner(FloatingWindowLifecycleOwner)
//                        setContent {
//                            var showColorPicker by remember { mutableStateOf(false) }
//                            var showStylePicker by remember { mutableStateOf(false) }
//                            val configuration = LocalConfiguration.current
//                            val density = LocalDensity.current
//
//                            val screenHeightPx =
//                                with(density) { configuration.screenHeightDp.dp.toPx() }
//
//                            var offsetX by remember { mutableFloatStateOf(0f) }
//                            var offsetY by remember { mutableFloatStateOf(screenHeightPx / 2f) } // 屏幕高度一半
//
//                            Row(
//                                modifier = Modifier
//                                    .offset { IntOffset(offsetX.toInt(), offsetY.toInt()) }
//                                    .pointerInput(Unit) {
//                                        detectDragGestures { change, dragAmount ->
//                                            change.consume()
//                                            offsetX += dragAmount.x
//                                            offsetY += dragAmount.y
//                                        }
//                                    }
//                                    .padding(horizontal = 15.dp)
//                                    .padding(bottom = 10.dp),
//                                verticalAlignment = Alignment.CenterVertically,
//                                horizontalArrangement = Arrangement.spacedBy(20.dp)
//                            ) {
//                                // 字体颜色选择
//                                Icon(
//                                    imageVector = Icons.Default.ColorLens,
//                                    contentDescription = null,
//                                    tint = BasicSelector.tintColor(showColorPicker),
//                                    modifier = Modifier
//                                        .size(50.dp)
//                                        .pointerInput(Unit) {
//                                            detectTapGestures {
//                                                showColorPicker = true
//                                            }
//                                        }
//                                )
//
//                                // 字体大小选择
//                                Icon(
//                                    imageVector = Icons.Default.TextFields,
//                                    contentDescription = null,
//                                    tint = BasicSelector.tintColor(showStylePicker),
//                                    modifier = Modifier
//                                        .size(50.dp)
//                                        .pointerInput(Unit) {
//                                            detectTapGestures {
//                                                showStylePicker = true
//                                            }
//                                        }
//                                )
//                            }
//
//                            if (showColorPicker) {
//                                FontColorDialog(mainDataStore = mainDataStore) {
//                                    showColorPicker = false
//                                }
//                            }
//
//                            if (showStylePicker) {
//                                FontStyleDialog(mainDataStore = mainDataStore) {
//                                    showStylePicker = false
//                                }
//                            }
//                        }
//                    }
                    addFloatingComposeView(this)
                }
            }

            "com.sgmw.lingos.sgmwmediamusic.qqmusic.player.EventCallback".toClass().resolve()
                .apply {
                    method {
                        name = "onSongInfoChanged"
                    }.hookAll {
                        after {
                            val songInfo = args[0] ?: return@after

                            YLog.debug("onSongInfoUpdate: $songInfo")
                            val songId = "$songInfo".getValueOf("songId")
                            val songMid = "$songInfo".getValueOf("songMid")
                            mainViewModel.setSongId(songId)
                            mainViewModel.setSongMid(songMid)
                            YLog.debug("songId: $songId")
                            YLog.debug("songMid: $songMid")

//                            application.toast("onSongInfoUpdate: $songInfo")
//                            application.toast("songId: $songId")
//                            application.toast("songMid: $songMid")
                        }
                    }

                    method {
                        name = "onProgressChanged"
                    }.hookAll {
                        after {
                            val currentTime = args[0] ?: return@after
                            val totalTime = args[1] ?: return@after
                            mainViewModel.setMusicPlayingPosition(
                                currentTime.toString().toLongOrNull() ?: 0
                            )
                            YLog.debug("onProgressChanged: $currentTime/$totalTime")
//                            application.toast("onProgressChanged: $currentTime/$totalTime")
                        }
                    }
                }
        }
    }
}
