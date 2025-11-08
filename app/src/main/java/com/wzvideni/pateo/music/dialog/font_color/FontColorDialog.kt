package com.wzvideni.pateo.music.dialog.font_color

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wzvideni.pateo.music.MainDataStore
import com.wzvideni.pateo.music.basic.BasicSelector
import kotlinx.coroutines.launch


@Composable
fun FontColorDialog(
    mainDataStore: MainDataStore,
) {
    val coroutineScope = rememberCoroutineScope()
    val lyricsColor by mainDataStore.lyricsColor.collectAsStateWithLifecycle(MainDataStore.defaultLyricsColor)
    val translationColor by mainDataStore.translationColor.collectAsStateWithLifecycle(
        MainDataStore.defaultTranslationColor
    )
    val otherLyricsColor by mainDataStore.otherLyricsColor.collectAsStateWithLifecycle(
        MainDataStore.defaultOtherLyricsColor
    )

    // 用于内嵌快速调节
    val lyricsVisibleLines by mainDataStore.lyricsVisibleLines.collectAsStateWithLifecycle(
        MainDataStore.defaultLyricsVisibleLines
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
    val lyricsLineSpacing by mainDataStore.lyricsLineSpacing.collectAsStateWithLifecycle(
        MainDataStore.defaultLyricsLineSpacing
    )
    var tabIndex by remember { mutableIntStateOf(0) }

    val tabs by remember { mutableStateOf(listOf("歌词", "翻译", "其他")) }


    OutlinedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            SecondaryTabRow(
                selectedTabIndex = tabIndex,
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = tabIndex == index,
                        onClick = {
                            tabIndex = index
                        },
                        text = {
                            Text(
                                text = tab,
                                color = BasicSelector.color(tabIndex == index),
                                fontWeight = BasicSelector.fontWeight(tabIndex == index)
                            )
                        }
                    )
                }
            }

            when (tabIndex) {
                // 歌词
                0 -> {
                    ColorPicker(
                        color = lyricsColor
                    ) { color ->
                        coroutineScope.launch {
                            mainDataStore.setLyricsColor(color)
                        }
                    }

                    // 快速调节：可见行数
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "可见行数：$lyricsVisibleLines")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                coroutineScope.launch { mainDataStore.setLyricsVisibleLines(2) }
                            }, enabled = lyricsVisibleLines != 2) { Text("2排") }
                            OutlinedButton(onClick = {
                                coroutineScope.launch { mainDataStore.setLyricsVisibleLines(3) }
                            }, enabled = lyricsVisibleLines != 3) { Text("3排") }
                            OutlinedButton(onClick = {
                                coroutineScope.launch { mainDataStore.setLyricsVisibleLines(5) }
                            }, enabled = lyricsVisibleLines != 5) { Text("5排") }
                        }
                    }

                    // 快速调节：歌词字体大小
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "歌词大小：${lyricsSize.value.toInt()}sp")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                val minSp = 10f
                                val maxSp = 30f
                                val newSize = (lyricsSize.value + 1f).coerceIn(minSp, maxSp)
                                coroutineScope.launch { mainDataStore.setLyricsSize(newSize) }
                            }) { Text("A＋") }
                            OutlinedButton(onClick = {
                                val minSp = 10f
                                val maxSp = 30f
                                val newSize = (lyricsSize.value - 1f).coerceIn(minSp, maxSp)
                                coroutineScope.launch { mainDataStore.setLyricsSize(newSize) }
                            }) { Text("A－") }
                        }
                    }

                    // 快速调节：当前句与下一句的间距
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "行间距：${lyricsLineSpacing.value.toInt()}dp")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                val minDp = 0f
                                val maxDp = 24f
                                val newSpacing = (lyricsLineSpacing.value + 1f).coerceIn(minDp, maxDp)
                                coroutineScope.launch { mainDataStore.setLyricsLineSpacing(newSpacing.dp) }
                            }) { Text("＋间距") }
                            OutlinedButton(onClick = {
                                val minDp = 0f
                                val maxDp = 24f
                                val newSpacing = (lyricsLineSpacing.value - 1f).coerceIn(minDp, maxDp)
                                coroutineScope.launch { mainDataStore.setLyricsLineSpacing(newSpacing.dp) }
                            }) { Text("－间距") }
                        }
                    }
                }

                // 翻译
                1 -> {
                    ColorPicker(
                        color = translationColor
                    ) { color ->
                        coroutineScope.launch {
                            mainDataStore.setTranslationColor(color)
                        }
                    }

                    // 快速调节：翻译字体大小
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "翻译大小：${translationSize.value.toInt()}sp")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                val minSp = 10f
                                val maxSp = 30f
                                val newSize = (translationSize.value + 1f).coerceIn(minSp, maxSp)
                                coroutineScope.launch { mainDataStore.setTranslationSize(newSize) }
                            }) { Text("A＋") }
                            OutlinedButton(onClick = {
                                val minSp = 10f
                                val maxSp = 30f
                                val newSize = (translationSize.value - 1f).coerceIn(minSp, maxSp)
                                coroutineScope.launch { mainDataStore.setTranslationSize(newSize) }
                            }) { Text("A－") }
                        }
                    }
                }

                // 其他
                2 -> {
                    ColorPicker(
                        color = otherLyricsColor
                    ) { color ->
                        coroutineScope.launch {
                            mainDataStore.setOtherLyricsColor(color)
                        }
                    }

                    // 快速调节：其他歌词字体大小
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "其他大小：${otherLyricsSize.value.toInt()}sp")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                val minSp = 10f
                                val maxSp = 30f
                                val newSize = (otherLyricsSize.value + 1f).coerceIn(minSp, maxSp)
                                coroutineScope.launch { mainDataStore.setOtherLyricsSize(newSize) }
                            }) { Text("A＋") }
                            OutlinedButton(onClick = {
                                val minSp = 10f
                                val maxSp = 30f
                                val newSize = (otherLyricsSize.value - 1f).coerceIn(minSp, maxSp)
                                coroutineScope.launch { mainDataStore.setOtherLyricsSize(newSize) }
                            }) { Text("A－") }
                        }
                    }
                }
            }
        }
    }
}