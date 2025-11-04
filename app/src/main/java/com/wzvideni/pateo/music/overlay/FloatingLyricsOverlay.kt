package com.wzvideni.pateo.music.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
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
import com.wzvideni.pateo.music.lifecycle.FloatingWindowLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object FloatingLyricsOverlay {

    data class Handle(
        val view: ComposeView,
        val layoutParams: WindowManager.LayoutParams
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
            FloatingLyricsContent(
                mainViewModel = mainViewModel,
                mainDataStore = mainDataStore,
                isMockMode = isMockMode,
                dragController = dragController
            )
        }

        return Handle(composeView, layoutParams)
    }

    private fun createLayoutParams(position: OverlayPosition): WindowManager.LayoutParams {
        return if (Build.VERSION.SDK_INT != Build.VERSION_CODES.S) {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            )
        } else {
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
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

    val visibleLyrics by remember {
        derivedStateOf {
            val current = musicLyricsIndex
            val next = current + 1
            val lastIndex = musicLyricsList.lastIndex
            musicLyricsList.withIndex().filter { indexed ->
                indexed.index == current || (next <= lastIndex && indexed.index == next)
            }
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
        mainDataStore.setLyricsLineSpacing(8.dp)
    }

    LaunchedEffect(isMockMode) {
        if (isMockMode) {
            val mockLyrics = mockLyrics()
            mainViewModel.setMusicLyricsList(mockLyrics)
            mainViewModel.setMusicPlayingPosition(0L)
            while (isActive) {
                mockLyrics.forEach { lyric ->
                    mainViewModel.setMusicPlayingPosition(lyric.millisecond.toLong())
                    delay(2200)
                }
            }
        }
    }

    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .wrapContentSize(Alignment.Center)
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        dragController.onDragStart()
                    },
                    onDrag = { change, dragAmount ->
                        change.consumeAllChanges()
                        dragController.onDrag(dragAmount)
                    },
                    onDragEnd = {
                        val position = dragController.onDragEnd()
                        coroutineScope.launch {
                            mainDataStore.setOverlayPosition(position.x, position.y)
                        }
                    },
                    onDragCancel = {
                        val position = dragController.onDragEnd()
                        coroutineScope.launch {
                            mainDataStore.setOverlayPosition(position.x, position.y)
                        }
                    }
                )
            }
            .onGloballyPositioned { coordinates ->
                dragController.onSizeChanged(coordinates.size.width, coordinates.size.height)
            }
            .padding(bottom = 100.dp)
    ) {
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
