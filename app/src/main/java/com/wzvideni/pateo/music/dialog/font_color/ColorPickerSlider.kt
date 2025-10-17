package com.wzvideni.floatinglyrics.ui.dialog.font_color

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wzvideni.pateo.music.expansion.defaultLocale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
inline fun ColorPickerSlider(
    pairValue: Pair<String, Color>,
    value: Number,
    valueRange: ClosedFloatingPointRange<Float>,
    crossinline onValueChange: (String) -> Unit
) {
    val format by remember { derivedStateOf { if (value is Float) "%.2f" else "%.0f" } }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "${pairValue.first}:")

        Text(
            text = String.format(
                locale = defaultLocale,
                format = format,
                value.toFloat()
            ),
            modifier = Modifier
                .width(40.dp),
            textAlign = TextAlign.Center
        )

        Slider(
            value = value.toFloat(),
            valueRange = valueRange,
            onValueChange = { floatValue ->
                onValueChange(
                    String.format(
                        locale = defaultLocale,
                        format = format,
                        floatValue
                    )
                )
            },
            thumb = {
                Spacer(
                    Modifier
                        .size(20.dp)
                        .background(pairValue.second, CircleShape)
                )
            },
            track = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(SliderDefaults.colors().inactiveTrackColor)
                )
            }
        )
    }
}