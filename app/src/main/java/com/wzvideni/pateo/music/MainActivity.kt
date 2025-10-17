package com.wzvideni.pateo.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wzvideni.pateo.music.basic.BasicSelector
import com.wzvideni.pateo.music.dialog.font_color.FontColorDialog
import com.wzvideni.pateo.music.dialog.font_style.FontStyleDialog
import com.wzvideni.pateo.music.ui.theme.PateoMusicHookTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val mainDataStore by lazy { MainDataStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PateoMusicHookTheme {

                val coroutineScope = rememberCoroutineScope()
                val autoSearch by mainDataStore.autoSearch.collectAsStateWithLifecycle(MainDataStore.defaultAutoSearch)

                var showColorPicker by remember { mutableStateOf(false) }
                var showStylePicker by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                            .padding(horizontal = 15.dp)
                            .padding(bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // 自动搜索
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = BasicSelector.tintColor(autoSearch),
                            modifier = Modifier
                                .size(50.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures {
                                        coroutineScope.launch {
                                            mainDataStore.setAutoSearch(!autoSearch)
                                        }
                                    }
                                }
                        )

                        // 字体颜色选择
                        Icon(
                            imageVector = Icons.Default.ColorLens,
                            contentDescription = null,
                            tint = BasicSelector.tintColor(showColorPicker),
                            modifier = Modifier
                                .size(50.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures {
                                        showColorPicker = true
                                    }
                                }
                        )

                        // 字体大小选择
                        Icon(
                            imageVector = Icons.Default.TextFields,
                            contentDescription = null,
                            tint = BasicSelector.tintColor(showStylePicker),
                            modifier = Modifier
                                .size(50.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures {
                                        showStylePicker = true
                                    }
                                }
                        )
                    }
                }

                if (showColorPicker) {
                    FontColorDialog(mainDataStore = mainDataStore) {
                        showColorPicker = false
                    }
                }

                if (showStylePicker) {
                    FontStyleDialog(mainDataStore = mainDataStore) {
                        showStylePicker = false
                    }
                }
            }
        }
    }
}