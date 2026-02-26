package com.pdrehab.handwritinglab.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val scheme = lightColorScheme(
    primary = Color(0xFF522B47),
    secondary = Color(0xFF8DAA9D),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    background = Color.White,
    onBackground = Color.Black
)

@Composable
fun PDHLTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}