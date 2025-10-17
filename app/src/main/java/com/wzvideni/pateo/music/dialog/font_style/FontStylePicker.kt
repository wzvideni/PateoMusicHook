package com.wzvideni.pateo.music.dialog.font_style

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.wzvideni.pateo.music.basic.BasicSelector

@Composable
inline fun FontStylePicker(
    text: String,
    style: FontStyle,
    crossinline onClick: (style: FontStyle) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = text)
        Row(
            modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "常规",
                fontStyle = FontStyle.Normal,
                color = BasicSelector.color(style == FontStyle.Normal),
                modifier = Modifier.clickable {
                    onClick(FontStyle.Normal)
                }
            )
            Text(
                text = "斜体",
                fontStyle = FontStyle.Italic,
                color = BasicSelector.color(style == FontStyle.Italic),
                modifier = Modifier.clickable {
                    onClick(FontStyle.Italic)
                }
            )
        }
    }
}