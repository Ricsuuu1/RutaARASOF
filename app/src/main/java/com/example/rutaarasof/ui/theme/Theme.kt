package com.example.rutaarasof.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColorScheme = lightColorScheme(
    primary = RedPrimary,
    onPrimary = Color.White,
    primaryContainer = RedContainer,
    onPrimaryContainer = RedPrimary,

    secondary = RedSecondary,
    onSecondary = Color.White,
    secondaryContainer = RedContainer,
    onSecondaryContainer = RedPrimary,

    background = BackgroundLight, // Soft white/gray background
    surface = SurfaceCard,        // Crisp white cards
    onBackground = TextPrimary,   // Deep charcoal text
    onSurface = TextPrimary
)

@Composable
fun RutaARASOFTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Forcing the clean light harmony scheme so it stays gorgeous and readable
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}
