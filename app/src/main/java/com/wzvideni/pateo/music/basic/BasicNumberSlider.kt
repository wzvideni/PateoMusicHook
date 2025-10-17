package com.wzvideni.pateo.music.basic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly
import com.wzvideni.pateo.music.expansion.toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
inline fun BasicNumberSlider(
    text: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    crossinline onValueChange: (value: Float) -> Unit
) {
    val context = LocalContext.current
    var inputValue by remember(value) { mutableStateOf("${value.toInt()}") }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = text)
        BasicTextField(
            value = inputValue,
            modifier = Modifier.width(20.dp),
            textStyle = TextStyle(color = PrimaryColor),
            cursorBrush = SolidColor(PrimaryColor),
            onValueChange = { value ->
                inputValue = value
                if (value.isNotEmpty() && value.isDigitsOnly()) {
                    onValueChange(value.toInt().toFloat())
                } else {
                    context.toast("只允许包含数字")
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Slider(
            value = value,
            valueRange = valueRange,
            onValueChange = { floatValue ->
                onValueChange(floatValue.toInt().toFloat())
            },
            thumb = {
                Spacer(
                    Modifier
                        .size(20.dp)
                        .background(PrimaryColor, CircleShape)
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