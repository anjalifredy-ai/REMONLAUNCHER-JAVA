package com.remom.launcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RemomDarkColorScheme = darkColorScheme(
    primary = RemomBluePrimary,
    onPrimary = Color.White,
    primaryContainer = RemomBlueDark,
    onPrimaryContainer = RemomOnSurface,
    secondary = RemomAccent,
    onSecondary = Color.Black,
    background = RemomBackground,
    onBackground = RemomOnSurface,
    surface = RemomSurface,
    onSurface = RemomOnSurface,
    error = RemomError,
    onError = Color.White
)

@Composable
fun RemomLauncherTheme(
    darkTheme: Boolean = true, // Always prefer dark blue theme
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RemomDarkColorScheme,
        content = content
    )
}
