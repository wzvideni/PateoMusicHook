package com.wzvideni.pateo.music.dialog.font_color

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
    onDismissRequest: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val lyricsColor by mainDataStore.lyricsColor.collectAsStateWithLifecycle(MainDataStore.defaultLyricsColor)
    val translationColor by mainDataStore.translationColor.collectAsStateWithLifecycle(
        MainDataStore.defaultTranslationColor
    )
    val otherLyricsColor by mainDataStore.otherLyricsColor.collectAsStateWithLifecycle(
        MainDataStore.defaultOtherLyricsColor
    )
    var tabIndex by remember { mutableIntStateOf(0) }

    val tabs by remember { mutableStateOf(listOf("歌词", "翻译", "其他")) }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                onDismissRequest()
            }, contentAlignment = Alignment.Center
    ) {

        OutlinedCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
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
                    }
                }
            }
        }
    }
}