package com.wzvideni.pateo.music.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.wzvideni.pateo.music.MainDataStore
import com.wzvideni.pateo.music.MainViewModel
import com.wzvideni.pateo.music.data.mockLyrics
import com.wzvideni.pateo.music.dialog.font_color.FontColorDialog
import com.wzvideni.pateo.music.lifecycle.FloatingWindowLifecycleOwner
import com.wzvideni.pateo.music.ui.theme.PateoMusicHookTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object FloatingLyricsOverlay {

    data class Handle(
        val view: ComposeView, val layoutParams: WindowManager.LayoutParams
    )

    data class OverlayPosition(val x: Int, val y: Int)

    fun create(
        context: Context,
        windowManager: WindowManager,
        mainViewModel: MainViewModel,
        mainDataStore: MainDataStore,
        isMockMode: Boolean,
        initialPosition: OverlayPosition,
    ): Handle {
        val layoutParams = createLayoutParams(initialPosition)
        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(FloatingWindowLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(FloatingWindowLifecycleOwner)
        }

        val dragController = FloatingLyricsDragController(
            context = context,
            windowManager = windowManager,
            view = composeView,
            layoutParams = layoutParams
        )

        composeView.setContent {
            PateoMusicHookTheme(dynamicColor = false) {
                FloatingLyricsContent(
                    mainViewModel = mainViewModel,
                    mainDataStore = mainDataStore,
                    isMockMode = isMockMode,
                    dragController = dragController
                )
            }
        }

        return Handle(composeView, layoutParams)
    }

    private fun createLayoutParams(position: OverlayPosition): WindowManager.LayoutParams {
        return if (Build.VERSION.SDK_INT != Build.VERSION_CODES.S) {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            )
        } else {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                alpha = 0.9f
            }
        }.apply {
            gravity = Gravity.TOP or Gravity.START
            x = position.x
            y = position.y
        }
    }

    fun updateLifecycleToResumed() {
        FloatingWindowLifecycleOwner.updateLifecycleState(Lifecycle.State.CREATED)
        FloatingWindowLifecycleOwner.updateLifecycleState(Lifecycle.State.STARTED)
        FloatingWindowLifecycleOwner.updateLifecycleState(Lifecycle.State.RESUMED)
    }

    fun updateLifecycleToDestroyed() {
        FloatingWindowLifecycleOwner.updateLifecycleState(Lifecycle.State.STARTED)
        FloatingWindowLifecycleOwner.updateLifecycleState(Lifecycle.State.CREATED)
    }
}

private class FloatingLyricsDragController(
    private val context: Context,
    private val windowManager: WindowManager,
    private val view: ComposeView,
    val layoutParams: WindowManager.LayoutParams
) {

    private var viewWidth: Int = 0
    private var viewHeight: Int = 0
    private var initialX: Int = 0
    private var initialY: Int = 0
    private var accumulatedDragX: Float = 0f
    private var accumulatedDragY: Float = 0f

    var currentPosition: FloatingLyricsOverlay.OverlayPosition =
        FloatingLyricsOverlay.OverlayPosition(layoutParams.x, layoutParams.y)
        private set

    fun onSizeChanged(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        val changed = width != viewWidth || height != viewHeight
        viewWidth = width
        viewHeight = height
        if (changed) {
            applyPosition(layoutParams.x, layoutParams.y)
        }
    }

    fun onDragStart() {
        initialX = layoutParams.x
        initialY = layoutParams.y
        accumulatedDragX = 0f
        accumulatedDragY = 0f
    }

    fun onDrag(dragAmount: Offset) {
        accumulatedDragX += dragAmount.x
        accumulatedDragY += dragAmount.y
        val targetX = initialX + accumulatedDragX.toInt()
        val targetY = initialY + accumulatedDragY.toInt()
        applyPosition(targetX, targetY)
    }

    fun onDragEnd(): FloatingLyricsOverlay.OverlayPosition = currentPosition

    private fun applyPosition(targetX: Int, targetY: Int) {
        val metrics = context.resources.displayMetrics
        val maxX = (metrics.widthPixels - viewWidth).coerceAtLeast(0)
        val maxY = (metrics.heightPixels - viewHeight).coerceAtLeast(0)
        val clampedX = targetX.coerceIn(0, maxX)
        val clampedY = targetY.coerceIn(0, maxY)
        if (layoutParams.x != clampedX || layoutParams.y != clampedY) {
            layoutParams.x = clampedX
            layoutParams.y = clampedY
            if (ViewCompat.isAttachedToWindow(view)) {
                windowManager.updateViewLayout(view, layoutParams)
            }
        }
        currentPosition = FloatingLyricsOverlay.OverlayPosition(layoutParams.x, layoutParams.y)
    }
}

@Composable
private fun FloatingLyricsContent(
    mainViewModel: MainViewModel,
    mainDataStore: MainDataStore,
    isMockMode: Boolean,
    dragController: FloatingLyricsDragController,
) {
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

    // 获取歌曲信息
    val songName by mainViewModel.songName.collectAsStateWithLifecycle()
    val singerName by mainViewModel.singerName.collectAsStateWithLifecycle()
    val albumName by mainViewModel.albumName.collectAsStateWithLifecycle()
    val albumPic by mainViewModel.albumPic.collectAsStateWithLifecycle()

    // 使用完整列表 + 平滑滚动，让滚动更自然
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val centerOffset = remember(lyricsVisibleLines) { (lyricsVisibleLines - 1) / 2 }
    val approxLyricLinePx = remember(lyricsSize, otherLyricsSize, density) {
        with(density) { kotlin.math.max(lyricsSize.toPx(), otherLyricsSize.toPx()) * 1.6f }
    }
    val approxTranslationLinePx = remember(translationSize, otherLyricsSize, density) {
        with(density) { kotlin.math.max(translationSize.toPx(), otherLyricsSize.toPx()) * 1.6f }
    }
    var measuredLyricCurrentPx by remember { mutableStateOf<Float?>(null) }
    var measuredLyricOtherPx by remember { mutableStateOf<Float?>(null) }
    var measuredTranslationCurrentPx by remember { mutableStateOf<Float?>(null) }
    var measuredTranslationOtherPx by remember { mutableStateOf<Float?>(null) }
    val containerHeightDp = remember(
        musicLyricsList,
        musicLyricsIndex,
        lyricsVisibleLines,
        lyricsLineSpacing,
        approxLyricLinePx,
        approxTranslationLinePx,
        measuredLyricCurrentPx,
        measuredLyricOtherPx,
        measuredTranslationCurrentPx,
        measuredTranslationOtherPx,
        density
    ) {
        val startIndex = (musicLyricsIndex - centerOffset).coerceAtLeast(0)
        var heightPx = 0f
        var rowsAccum = 0
        val spacingPx = with(density) { lyricsLineSpacing.toPx() }
        val lyricCurrentPx = measuredLyricCurrentPx ?: approxLyricLinePx
        val lyricOtherPx = measuredLyricOtherPx ?: approxLyricLinePx
        val transCurrentPx = measuredTranslationCurrentPx ?: approxTranslationLinePx
        val transOtherPx = measuredTranslationOtherPx ?: approxTranslationLinePx
        var i = startIndex
        while (i <= musicLyricsList.lastIndex && rowsAccum < lyricsVisibleLines) {
            val item = musicLyricsList[i]
            val hasLyric = item.lyricsList.getOrNull(0) != null
            val hasTrans = item.lyricsList.getOrNull(1) != null
            val rowHeight =
                (if (hasLyric) {
                    if (i == musicLyricsIndex) lyricCurrentPx else lyricOtherPx
                } else 0f) +
                (if (hasTrans) {
                    if (i == musicLyricsIndex) transCurrentPx else transOtherPx
                } else 0f)

            heightPx += rowHeight
            if (i == musicLyricsIndex && rowsAccum < lyricsVisibleLines - 1) {
                heightPx += spacingPx
            }
            rowsAccum++
            i++
        }
        with(density) { heightPx.toDp() }
    }

    // 移除每次初始化强制覆盖设置，改为完全使用 DataStore 持久化值

    LaunchedEffect(isMockMode) {
        if (isMockMode) {
            val mockLyrics = mockLyrics()
            mainViewModel.setMusicLyricsList(mockLyrics)
            mainViewModel.setMusicPlayingPosition(0L)
            mainViewModel.setSongName("偏爱 (My devotion)")
            mainViewModel.setSingerName("黄星")
            mainViewModel.setAlbumName("Beloved (挚爱)")
            mainViewModel.setAlbumPic("http://y.gtimg.cn/music/photo_new/T002R500x500M000001J55Pj0QwUjw_1.jpg")
            while (isActive) {
                mockLyrics.forEach { lyric ->
                    mainViewModel.setMusicPlayingPosition(lyric.millisecond.toLong())
                    delay(2200)
                }
            }
        }
    }

    val coroutineScope = rememberCoroutineScope()
    var controlsVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .wrapContentSize(Alignment.Center)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        controlsVisible = !controlsVisible
                    })
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(onDragStart = {
                    dragController.onDragStart()
                }, onDrag = { change, dragAmount ->
                    change.consumeAllChanges()
                    dragController.onDrag(dragAmount)
                }, onDragEnd = {
                    val position = dragController.onDragEnd()
                    coroutineScope.launch {
                        mainDataStore.setOverlayPosition(position.x, position.y)
                    }
                }, onDragCancel = {
                    val position = dragController.onDragEnd()
                    coroutineScope.launch {
                        mainDataStore.setOverlayPosition(position.x, position.y)
                    }
                })
            }
            .onGloballyPositioned { coordinates ->
                dragController.onSizeChanged(coordinates.size.width, coordinates.size.height)
            }
            .padding(bottom = 0.dp)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = controlsVisible,
                enter = slideInVertically { it / 2 } + fadeIn(),
                exit = slideOutVertically { it / 2 } + fadeOut()) {


                Column(modifier = Modifier.fillMaxWidth()) {
                    FontColorDialog(mainDataStore = mainDataStore)
                }

            }

            // 平滑滚动到当前歌词附近位置（居中偏移）
            LaunchedEffect(musicLyricsIndex, lyricsVisibleLines) {
                val startIndex = (musicLyricsIndex - centerOffset).coerceAtLeast(0)
                val lastStart =
                    (musicLyricsList.lastIndex - lyricsVisibleLines + 1).coerceAtLeast(0)
                val targetIndex = startIndex.coerceAtMost(lastStart)
                val approxRowPx = (approxLyricLinePx + approxTranslationLinePx)
                val scrollOffsetPx = (approxRowPx * centerOffset).toInt()
                listState.animateScrollToItem(targetIndex, scrollOffsetPx)
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .heightIn(max = containerHeightDp),
                userScrollEnabled = false,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(
                    items = musicLyricsList,
                    key = { _, musicLyrics ->
                        val millisecond = musicLyrics.millisecond
                        val lyrics = musicLyrics.lyricsList.firstOrNull()
                        val translation = musicLyrics.lyricsList.lastOrNull()
                        "${millisecond}${lyrics}${translation}"
                    }
                ) { index, musicLyrics ->
                    val lyrics = musicLyrics.lyricsList.getOrNull(0)
                    val translation = musicLyrics.lyricsList.getOrNull(1)

                    val bottomPad = if (musicLyricsIndex == index) lyricsLineSpacing else 0.dp
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = bottomPad)) {
                        if (lyrics != null) {
                            if (musicLyricsIndex == index) {
                                Text(
                                    text = lyrics,
                                    color = lyricsColor,
                                    fontSize = lyricsSize,
                                    fontWeight = lyricsWeight,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                    onTextLayout = { measuredLyricCurrentPx = it.size.height.toFloat() }
                                )
                            } else {
                                Text(
                                    text = lyrics,
                                    color = otherLyricsColor,
                                    fontSize = otherLyricsSize,
                                    fontWeight = otherLyricsWeight,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                    onTextLayout = { measuredLyricOtherPx = it.size.height.toFloat() }
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
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                    onTextLayout = { measuredTranslationCurrentPx = it.size.height.toFloat() }
                                )
                            } else {
                                Text(
                                    text = translation,
                                    color = otherLyricsColor,
                                    fontSize = otherLyricsSize,
                                    fontWeight = otherLyricsWeight,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                    onTextLayout = { measuredTranslationOtherPx = it.size.height.toFloat() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
