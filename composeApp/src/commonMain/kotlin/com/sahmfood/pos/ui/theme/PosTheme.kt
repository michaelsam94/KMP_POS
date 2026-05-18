package com.sahmfood.pos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PrimaryGreen  = Color(0xFF1B8A5A)
private val OnPrimary     = Color(0xFFFFFFFF)
private val Secondary     = Color(0xFFF5A623)
private val Surface       = Color(0xFFF8F9FA)
private val OnSurface     = Color(0xFF1C1C1E)
private val Background    = Color(0xFFFFFFFF)
private val Error         = Color(0xFFD32F2F)

private val LightColors = lightColorScheme(
    primary         = PrimaryGreen,
    onPrimary       = OnPrimary,
    secondary       = Secondary,
    surface         = Surface,
    onSurface       = OnSurface,
    background      = Background,
    onBackground    = OnSurface,
    error           = Error
)

private val DarkColors = darkColorScheme(
    primary         = Color(0xFF34C77B),
    onPrimary       = Color(0xFF003825),
    secondary       = Secondary,
    surface         = Color(0xFF1C1C1E),
    onSurface       = Color(0xFFECECEC),
    background      = Color(0xFF121212),
    error           = Color(0xFFEF5350)
)

@Composable
fun PosTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content     = content
    )
}
