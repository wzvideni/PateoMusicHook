package com.wzvideni.pateo.music

import android.app.Application
import android.content.Context.WINDOW_SERVICE
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

    lateinit var application: Application
    lateinit var mainViewModel: MainViewModel
    lateinit var mainDataStore: MainDataStore
    lateinit var floatingLyricsView: ComposeView
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
            if (floatingLyricsView.parent == null) {
                windowManager.addView(floatingLyricsView, lockedWindowParams)
//                windowManager.addView(floatingSettingsView, unlockedWindowParams)
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
                    floatingLyricsView = ComposeView(this).apply {
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

                            LaunchedEffect(Unit) {
                                mainDataStore.setLyricsSize(25f)
                                mainDataStore.setTranslationSize(25f)
                                mainDataStore.setOtherLyricsSize(24f)
                                mainDataStore.setLyricsColor(Color(0xFF01b425))
                                mainDataStore.setTranslationColor(Color(0xFF01b425))
                                mainDataStore.setLyricsWeight(FontWeight.Bold)
                                mainDataStore.setTranslationWeight(FontWeight.Bold)
                                mainDataStore.setOtherLyricsColor(Color(0xFFCC7B00))
                                mainDataStore.setLyricsVisibleLines(3)
                                mainDataStore.setLyricsLineSpacing(15.dp)
                            }
                            Box(modifier = Modifier.fillMaxSize()) {
                                Row(modifier = Modifier.fillMaxSize()) {
                                    LazyColumn(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(lyricsLineSpacing),
                                        modifier = Modifier
                                            .align(Alignment.Bottom)
                                            .padding(start = 70.dp, bottom = 180.dp)
                                    ) {
                                        items(
                                            items = visibleLyrics,
                                            key = { indexedValue ->
                                                val musicLyrics = indexedValue.value
                                                val millisecond = musicLyrics.millisecond
                                                val lyrics = musicLyrics.lyricsList.firstOrNull()
                                                val translation =
                                                    musicLyrics.lyricsList.lastOrNull()
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
                        }
                    }

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