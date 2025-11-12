package com.wzvideni.pateo.music

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.view.WindowManager
import com.highcapable.betterandroid.system.extension.tool.AndroidVersion
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.configs
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.factory.registerModuleAppActivities
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import com.wzvideni.pateo.music.broadcast.BroadcastSender
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
    companion object {
        private val launchedOnce = java.util.concurrent.atomic.AtomicBoolean(false)
        private val postLaunchedOnce = java.util.concurrent.atomic.AtomicBoolean(false)
    }

    lateinit var windowManager: WindowManager

    override fun onInit() = configs {
        debugLog {
            isEnable = false
            isRecord = false
            elements(TAG, PRIORITY, PACKAGE_NAME, USER_ID)
        }
        isDebug = true
        isEnableModuleAppResourcesCache = true
        isEnableHookSharedPreferences = true
        isEnableDataChannel = true
    }

    fun addFloatingComposeView(application: Application) {
        if (application.checkDrawOverlays()) {
            val handle = floatingLyricsHandle
            if (handle.view.parent == null) {
                windowManager.addView(handle.view, handle.layoutParams)
                FloatingLyricsOverlay.updateLifecycleToResumed()
                application.toast("启动悬浮窗")
            }
        } else {
            application.toast("请授予悬浮窗权限")
        }
    }

    fun launchMyApp(context: Context) {
        try {
            val intent = Intent()
            intent.setClassName("com.wzvideni.pateo.music", "com.wzvideni.pateo.music.MainActivity")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Throwable) {
            YLog.debug("启动失败: ${e.message}")
        }
    }
    override fun onHook() = encase {
        // 装载需要 Hook 的 APP
        loadApp(name = "com.sgmw.lingos.launcher") {
            // 注册模块 Activity 代理
            onAppLifecycle {
                onCreate {
                    if (AndroidVersion.isAtLeast(AndroidVersion.N)) registerModuleAppActivities()
                    launchMyApp(application)
                }
            }
        }
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
                    addFloatingComposeView(this)

                    // 取消旧版“自动打开界面设置”逻辑：不再在 Hook 侧自动弹出模块 UI
                    YLog.debug("Auto UI launch from hook-side is disabled by design")

                    // 监听来自模块 App 的模拟悬浮歌词状态广播，用于在 hook 端决定是否发送歌词/元数据广播
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                            if (intent?.action == BroadcastSender.ACTION_OVERLAY_STATUS) {
                                val enabled = intent.getBooleanExtra("enabled", false)
                                BroadcastSender.updateOverlayStatus(enabled)
                            }
                        }
                    }
                    registerReceiver(receiver, IntentFilter(BroadcastSender.ACTION_OVERLAY_STATUS))
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
                            val songName = "$songInfo".getValueOf("songName")
                            val singerName = "$songInfo".getValueOf("singerName")
                            val albumName = "$songInfo".getValueOf("albumName")
                            val albumPic = "$songInfo".getValueOf("albumPic500x500")
                            mainViewModel.setSongId(songId)
                            mainViewModel.setSongMid(songMid)
                            mainViewModel.setSongName(songName)
                            mainViewModel.setSingerName(singerName)
                            mainViewModel.setAlbumName(albumName)
                            mainViewModel.setAlbumPic(albumPic)

                            // 在 hook 端发送元数据广播（若未启用模拟悬浮歌词）
                            if (!BroadcastSender.isOverlayMockEnabled()) {
                                BroadcastSender.sendMetadata(
                                    application,
                                    singerName,
                                    songName,
                                    albumName,
                                    albumPic
                                )
                            }

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
                            // 歌词广播由 ViewModel 的歌词索引更新逻辑触发，这里无需重复发送
                        }
                    }
                }
        }
    }
}
