package com.lchat.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Esquema oscuro — la app es dark-only por diseño
private val LChatColorScheme = darkColorScheme(
    primary = NeuAccent,
    onPrimary = TextOnAccent,
    background = NeuBackground,
    onBackground = TextPrimary,
    surface = NeuSurface,
    onSurface = TextPrimary,
    surfaceVariant = NeuSurface,
    onSurfaceVariant = TextSecondary,
    error = NeuAccent,
    onError = TextOnAccent
)

@Composable
fun LChatTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = NeuBackground.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = LChatColorScheme,
        typography = LChatTypography,
        shapes = LChatShapes,
        content = content
    )
}