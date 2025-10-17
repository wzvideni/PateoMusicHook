package com.wzvideni.pateo.music

import android.app.Application
import android.content.Context.WINDOW_SERVICE
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
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
import com.wzvideni.pateo.music.expansion.lockedWindowParams
import com.wzvideni.pateo.music.expansion.toast
import com.wzvideni.pateo.music.lifecycle.FloatingWindowLifecycleOwner

@InjectYukiHookWithXposed
class MainHookEntry : IYukiHookXposedInit {

    companion object {
        lateinit var application: Application
        lateinit var mainViewModel: MainViewModel
        lateinit var mainDataStore: MainDataStore
        lateinit var floatingComposeView: ComposeView
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
        isEnableHookSharedPreferences = false
        isEnableDataChannel = true
    }

    fun addFloatingComposeView(application: Application) {
        if (application.checkDrawOverlays()) {
            if (floatingComposeView.parent == null) {
                windowManager.addView(floatingComposeView, lockedWindowParams)
                FloatingWindowLifecycleOwner.updateLifecycleState(Lifecycle.State.CREATED)
                FloatingWindowLifecycleOwner.updateLifecycleState(Lifecycle.State.STARTED)
                FloatingWindowLifecycleOwner.updateLifecycleState(Lifecycle.State.RESUMED)
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
                    floatingComposeView = ComposeView(this).apply {
                        setViewTreeLifecycleOwner(FloatingWindowLifecycleOwner)
                        setViewTreeSavedStateRegistryOwner(FloatingWindowLifecycleOwner)
                        setContent {
                            val musicLyricsList by mainViewModel.musicLyricsList.collectAsStateWithLifecycle()
                            val musicLyricsIndex by mainViewModel.musicLyricsIndex.collectAsStateWithLifecycle()

                            val lyricsVisibleLines by mainDataStore.lyricsVisibleLines.collectAsStateWithLifecycle(
                                MainDataStore.defaultLyricsVisibleLines
                            )
                            val lyricsLineSpacing by mainDataStore.lyricsLineSpacing.collectAsStateWithLifecycle(
                                MainDataStore.defaultLyricsLineSpacing
                            )
                            val lyricsColor by mainDataStore.lyricsColor.collectAsStateWithLifecycle(
                                MainDataStore.defaultLyricsColor
                            )
                            val translationColor by mainDataStore.translationColor.collectAsStateWithLifecycle(
                                MainDataStore.defaultTranslationColor
                            )
                            val otherLyricsColor by mainDataStore.otherLyricsColor.collectAsStateWithLifecycle(
                                MainDataStore.defaultOtherLyricsColor
                            )
                            val lyricsSize by mainDataStore.lyricsSize.collectAsStateWithLifecycle(
                                MainDataStore.defaultLyricsSize
                            )
                            val translationSize by mainDataStore.translationSize.collectAsStateWithLifecycle(
                                MainDataStore.defaultTranslationSize
                            )
                            val otherLyricsSize by mainDataStore.otherLyricsSize.collectAsStateWithLifecycle(
                                MainDataStore.defaultOtherLyricsSize
                            )
                            val lyricsWeight by mainDataStore.lyricsWeight.collectAsStateWithLifecycle(
                                MainDataStore.defaultLyricsWeight
                            )
                            val translationWeight by mainDataStore.translationWeight.collectAsStateWithLifecycle(
                                MainDataStore.defaultTranslationWeight
                            )
                            val otherLyricsWeight by mainDataStore.otherLyricsWeight.collectAsStateWithLifecycle(
                                MainDataStore.defaultOtherLyricsWeight
                            )

                            val visibleLyricsRange by remember {
                                derivedStateOf {
                                    musicLyricsIndex - lyricsVisibleLines / 2..musicLyricsIndex + lyricsVisibleLines / 2
                                }
                            }

                            val visibleLyrics by remember {
                                derivedStateOf {
                                    musicLyricsList.withIndex()
                                        .filter { it.index in visibleLyricsRange }
                                }
                            }

                            LazyColumn(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(lyricsLineSpacing)
                            ) {
                                items(
                                    items = visibleLyrics,
                                    key = { indexedValue ->
                                        val musicLyrics = indexedValue.value
                                        val millisecond = musicLyrics.millisecond
                                        val lyrics = musicLyrics.lyricsList.firstOrNull()
                                        val translation = musicLyrics.lyricsList.lastOrNull()
                                        "${millisecond}${lyrics}${translation}"
                                    }
                                ) { indexedValue ->
                                    val index = indexedValue.index
                                    val musicLyrics = indexedValue.value
                                    val lyrics = musicLyrics.lyricsList.getOrNull(0)
                                    val translation = musicLyrics.lyricsList.getOrNull(1)

                                    if (lyrics != null) {
                                        if (musicLyricsIndex == index) {
                                            Text(
                                                text = lyrics,
                                                color = lyricsColor,
                                                fontSize = lyricsSize,
                                                fontWeight = lyricsWeight,
                                                textAlign = TextAlign.Center
                                            )
                                        } else {
                                            Text(
                                                text = lyrics,
                                                color = otherLyricsColor,
                                                fontSize = otherLyricsSize,
                                                fontWeight = otherLyricsWeight,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }

                                    if (translation != null) {
                                        if (musicLyricsIndex == index) {
                                            Text(
                                                text = translation,
                                                color = translationColor,
                                                fontSize = translationSize,
                                                fontWeight = translationWeight,
                                                textAlign = TextAlign.Center
                                            )
                                        } else {
                                            Text(
                                                text = translation,
                                                color = otherLyricsColor,
                                                fontSize = otherLyricsSize,
                                                fontWeight = otherLyricsWeight,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
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