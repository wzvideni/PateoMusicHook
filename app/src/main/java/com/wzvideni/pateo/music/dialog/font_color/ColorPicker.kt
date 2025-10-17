package com.wzvideni.pateo.music.dialog.font_color

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import com.wzvideni.floatinglyrics.ui.dialog.font_color.ColorPickerSlider
import com.wzvideni.pateo.music.basic.PrimaryColor
import com.wzvideni.pateo.music.expansion.defaultLocale
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import java.lang.String.format

@OptIn(FlowPreview::class)
@Composable
inline fun ColorPicker(color: Color, crossinline onValueChange: (color: Color) -> Unit) {

    val colorRange by remember { mutableStateOf(0f..255f) }
    val alphaRange by remember { mutableStateOf(0f..1f) }

    var redInt by remember { mutableIntStateOf((color.red * 255).toInt()) }
    var greenInt by remember { mutableIntStateOf((color.green * 255).toInt()) }
    var blueInt by remember { mutableIntStateOf((color.blue * 255).toInt()) }
    var alpha by remember { mutableFloatStateOf(color.alpha) }
    val alphaInt by remember {
        derivedStateOf {
            ((alpha.fastCoerceIn(
                0f,
                1f
            ) * 255f) + 0.5f).toInt()
        }
    }

    val argb by remember {
        derivedStateOf {
            ((alphaInt and 0xFF) shl 24) or
                    ((redInt and 0xFF) shl 16) or
                    ((greenInt and 0xFF) shl 8) or
                    (blueInt and 0xFF)
        }
    }

    val argbHex by remember {
        derivedStateOf {
            format(defaultLocale, "#%08X", argb)
        }
    }

    val newColor by remember {
        derivedStateOf {
            Color(argb)
        }
    }

    LaunchedEffect(color) {
        redInt = (color.red * 255).toInt()
        greenInt = (color.green * 255).toInt()
        blueInt = (color.blue * 255).toInt()
        alpha = color.alpha
    }

    LaunchedEffect(Unit) {
        snapshotFlow { newColor }
            .debounce(100)
            .distinctUntilChanged()
            .collect { color ->
                onValueChange(color)
            }
    }


    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Hex:")
            Text(
                text = argbHex,
                modifier = Modifier
                    .weight(1f),
                textAlign = TextAlign.Center
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(newColor, RoundedCornerShape(5.dp))
            )
        }

        ColorPickerSlider(
            Pair("R", Color.Red),
            value = redInt,
            valueRange = colorRange,
            onValueChange = { redInt = it.toInt() })
        ColorPickerSlider(
            Pair("G", Color.Green),
            value = greenInt,
            valueRange = colorRange,
            onValueChange = { greenInt = it.toInt() })
        ColorPickerSlider(
            Pair("B", Color.Blue),
            value = blueInt,
            valueRange = colorRange,
            onValueChange = { blueInt = it.toInt() })
        ColorPickerSlider(
            Pair("A", PrimaryColor),
            value = alpha,
            valueRange = alphaRange,
            onValueChange = { alpha = it.toFloat() })
    }

}