package com.example.app.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color.White,
    secondary = Color(0xFF26A69A),
    background = Color(0xFFF5F5F5),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF82B1FF),
    onPrimary = Color.Black,
    secondary = Color(0xFF80CBC4),
    background = Color(0xFF121212),
)

/**
 * Tema raiz de la app. Vive en :core:ui para que cualquier feature lo pueda
 * reusar sin duplicar la definicion de colores ni redeclarar Material3.
 */
@Composable
fun NotesAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
