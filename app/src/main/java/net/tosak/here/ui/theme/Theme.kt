package net.tosak.here.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val EmberColorScheme = darkColorScheme(
    primary          = EmberAccent,
    onPrimary        = EmberBg,
    secondary        = EmberFg,
    onSecondary      = EmberBg,
    background       = EmberBg,
    onBackground     = EmberFg,
    surface          = EmberSurface,
    onSurface        = EmberFg,
    surfaceVariant   = EmberSurface,
    onSurfaceVariant = EmberMuted,
    outline          = EmberBorder,
    error            = EmberAccent,
    onError          = EmberBg,
)

@Composable
fun HereTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EmberColorScheme,
        typography  = Typography,
        content     = content,
    )
}