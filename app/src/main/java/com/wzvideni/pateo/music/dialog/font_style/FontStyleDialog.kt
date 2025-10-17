package com.wzvideni.pateo.music.dialog.font_style

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wzvideni.pateo.music.MainDataStore
import com.wzvideni.pateo.music.basic.BasicNumberSlider
import com.wzvideni.pateo.music.basic.BasicSelector
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontStyleDialog(
    mainDataStore: MainDataStore,
    onDismissRequest: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val lyricsWeight by mainDataStore.lyricsWeight.collectAsStateWithLifecycle(
        MainDataStore.defaultLyricsWeight
    )
    val translationWeight by mainDataStore.translationWeight.collectAsStateWithLifecycle(
        MainDataStore.defaultTranslationWeight
    )
    val lyricsSize by mainDataStore.lyricsSize.collectAsStateWithLifecycle(MainDataStore.defaultLyricsSize)
    val translationSize by mainDataStore.translationSize.collectAsStateWithLifecycle(
        MainDataStore.defaultTranslationSize
    )
    val otherLyricsSize by mainDataStore.otherLyricsSize.collectAsStateWithLifecycle(
        MainDataStore.defaultOtherLyricsSize
    )
    val otherLyricsWeight by mainDataStore.otherLyricsWeight.collectAsStateWithLifecycle(
        MainDataStore.defaultOtherLyricsWeight
    )


    val lyricsVisibleLines by mainDataStore.lyricsVisibleLines.collectAsStateWithLifecycle(
        MainDataStore.defaultLyricsVisibleLines
    )
    val lyricsLineSpacing by mainDataStore.lyricsLineSpacing.collectAsStateWithLifecycle(
        MainDataStore.defaultLyricsLineSpacing
    )

    var tabIndex by remember { mutableIntStateOf(0) }

    val tabs by remember { mutableStateOf(listOf("歌词", "翻译", "其他")) }


    Dialog(onDismissRequest = onDismissRequest) {

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
                        FontWeightPicker(text = "歌词粗细：", weight = lyricsWeight) { weight ->
                            coroutineScope.launch {
                                mainDataStore.setLyricsWeight(weight)
                            }
                        }
                        BasicNumberSlider(
                            text = "歌词大小：",
                            value = lyricsSize.value,
                            valueRange = 10f..30f
                        ) { size ->
                            coroutineScope.launch {
                                mainDataStore.setLyricsSize(size)
                            }
                        }
                    }

                    // 翻译
                    1 -> {
                        FontWeightPicker(text = "翻译粗细：", weight = translationWeight) { weight ->
                            coroutineScope.launch {
                                mainDataStore.setTranslationWeight(weight)
                            }
                        }

                        BasicNumberSlider(
                            text = "翻译大小：",
                            value = translationSize.value,
                            valueRange = 10f..30f
                        ) { size ->
                            coroutineScope.launch {
                                mainDataStore.setTranslationSize(size)
                            }
                        }
                    }

                    // 其他
                    2 -> {
                        FontWeightPicker(text = "其他粗细：", weight = otherLyricsWeight) { weight ->
                            coroutineScope.launch {
                                mainDataStore.setOtherLyricsWeight(weight)
                            }
                        }

                        BasicNumberSlider(
                            text = "其他大小：",
                            value = otherLyricsSize.value,
                            valueRange = 10f..30f
                        ) { size ->
                            coroutineScope.launch {
                                mainDataStore.setOtherLyricsSize(size)
                            }
                        }
                    }
                }

                BasicNumberSlider(
                    text = "歌词行数：",
                    value = lyricsVisibleLines.toFloat(),
                    valueRange = 1f..10f
                ) { lines ->
                    coroutineScope.launch {
                        mainDataStore.setLyricsVisibleLines(lines.toInt())
                    }
                }

                BasicNumberSlider(
                    text = "歌词间距：",
                    value = lyricsLineSpacing.value,
                    valueRange = 0f..30f
                ) { spacing ->
                    coroutineScope.launch {
                        mainDataStore.setLyricsLineSpacing(spacing.dp)
                    }
                }
            }
        }
    }
}