package com.wzvideni.pateo.music.dialog.font_style

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wzvideni.pateo.music.basic.BasicSelector

@Composable
inline fun FontWeightPicker(
    text: String,
    weight: FontWeight,
    crossinline onClick: (weight: FontWeight) -> Unit
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
                text = "细体",
                fontWeight = FontWeight.Light,
                color = BasicSelector.color(weight == FontWeight.Light),
                modifier = Modifier.clickable {
                    onClick(FontWeight.Light)
                }
            )
            Text(
                text = "常规",
                fontWeight = FontWeight.Normal,
                color = BasicSelector.color(weight == FontWeight.Normal),
                modifier = Modifier.clickable {
                    onClick(FontWeight.Normal)
                }
            )
            Text(
                text = "中等",
                fontWeight = FontWeight.Medium,
                color = BasicSelector.color(weight == FontWeight.Medium),
                modifier = Modifier.clickable {
                    onClick(FontWeight.Medium)
                }
            )
            Text(
                text = "加粗",
                fontWeight = FontWeight.Bold,
                color = BasicSelector.color(weight == FontWeight.Bold),
                modifier = Modifier.clickable {
                    onClick(FontWeight.Bold)
                }
            )
        }
    }
}