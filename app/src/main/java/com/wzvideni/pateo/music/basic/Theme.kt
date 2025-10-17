package com.wzvideni.pateo.music.basic

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color


val PrimaryColor: Color
    @Composable
    get() = MaterialTheme.colorScheme.primary


val LocalContentColor: Color
    @Composable
    get() = LocalContentColor.current