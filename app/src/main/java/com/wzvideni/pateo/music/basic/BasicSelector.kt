package com.wzvideni.pateo.music.basic

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

object BasicSelector {
    @Composable
    fun color(selected: Boolean) = if (selected) PrimaryColor else Color.Unspecified

    @Composable
    fun fontWeight(selected: Boolean) = if (selected) FontWeight.Bold else FontWeight.Normal

    @Composable
    fun tintColor(selected: Boolean) = if (selected) PrimaryColor else Color.White

}
