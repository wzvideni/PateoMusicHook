package com.wzvideni.pateo.music

import android.app.Application
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.Context.WINDOW_SERVICE
import android.view.WindowManager
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
import com.wzvideni.pateo.music.broadcast.BroadcastSender
import com.wzvideni.pateo.music.overlay.FloatingLyricsOverlay
import com.wzvideni.pateo.music.startup.LsposedStartupReceiver
import de.robv.android.xposed.XSharedPreferences
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
                    addFloatingComposeView(this)

                    // 简单方式：当 Hook 侧加载到目标音乐 App，按用户设置决定是否打开模块界面（一次性）
                    runCatching {
                        val prefs = XSharedPreferences("com.wzvideni.pateo.music", "auto_launch_prefs").apply { reload() }
                        val allowLaunch = prefs.getBoolean("enabled", false)
                        if (allowLaunch && launchedOnce.compareAndSet(false, true)) {
                            val pm = application.packageManager
                            val launchIntent = pm.getLaunchIntentForPackage("com.wzvideni.pateo.music")
                            val intent = launchIntent ?: android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                                addCategory(android.content.Intent.CATEGORY_LAUNCHER)
                                // 显式定位到模块的入口 Activity（别名 Home 指向 MainActivity）
                                setClassName("com.wzvideni.pateo.music", "com.wzvideni.pateo.music.MainActivity")
                            }
                            intent.addFlags(
                                android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                android.content.Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                            )
                            application.startActivity(intent)
                            YLog.debug("Launched module UI from hook side (user-enabled)")

                            // 弹出后一次性延迟执行用户选定的应用
                            val postEnabled = prefs.getBoolean("post_enabled", false)
                            val postPkg = prefs.getString("post_package", null)
                            val postComp = prefs.getString("post_component", null)
                            if (postEnabled && postLaunchedOnce.compareAndSet(false, true) && !postPkg.isNullOrBlank()) {
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    runCatching {
                                        val postIntent = pm.getLaunchIntentForPackage(postPkg)
                                            ?: if (!postComp.isNullOrBlank()) android.content.Intent().setClassName(postPkg, postComp) else null
                                        if (postIntent != null) {
                                            postIntent.addFlags(
                                                android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                                android.content.Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                                            )
                                            application.startActivity(postIntent)
                                            YLog.debug("Post app launched once after UI: pkg=$postPkg comp=$postComp")
                                        } else {
                                            YLog.warn("Post app not resolvable: pkg=$postPkg comp=$postComp")
                                        }
                                    }.onFailure { e ->
                                        YLog.error("Failed to launch post app: ${e.message}")
                                    }
                                }, 500)
                            }
                        } else {
                            YLog.debug("Auto-launch disabled by user; skip opening UI")
                        }
                    }.onFailure {
                        YLog.warn("Read auto-launch prefs failed: ${it.message}")
                    }

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
