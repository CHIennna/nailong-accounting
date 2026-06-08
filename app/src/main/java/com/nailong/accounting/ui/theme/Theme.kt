package com.nailong.accounting.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BrandYellow = Color(0xFFF7C948)
private val TextDark = Color(0xFF24211A)
private val SurfaceWarm = Color(0xFFFFFBF0)

private val LightColors: ColorScheme = lightColorScheme(
    primary = BrandYellow,
    onPrimary = TextDark,
    background = SurfaceWarm,
    onBackground = TextDark,
    surface = Color.White,
    onSurface = TextDark,
)

@Composable
fun NailongAccountingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
