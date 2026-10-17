package com.example.qwant.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Светлая схема: светлый алюминий + графитовый текст, акцент — «расплав».
 */
private val LightColorScheme = lightColorScheme(
    primary = FurnaceOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCA),
    onPrimaryContainer = Color(0xFF3A1000),
    secondary = SteelBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7DEE5),
    onSecondaryContainer = Color(0xFF141C22),
    tertiary = SteelBlue,
    onTertiary = Color.White,
    background = AluminumSurface,
    onBackground = Graphite,
    surface = AluminumSurface,
    onSurface = Graphite,
    surfaceVariant = AluminumSurfaceVariant,
    onSurfaceVariant = GraphiteSoft,
    outline = AluminumOutline,
    error = SignalRed,
)

/**
 * Тёмная схема: графит цеха + раскалённый металл, акцент — «расплав».
 */
private val DarkColorScheme = darkColorScheme(
    primary = MoltenOrange,
    onPrimary = Color(0xFF3A1000),
    primaryContainer = MoltenOrangeContainer,
    onPrimaryContainer = Color(0xFFFFDBCA),
    secondary = SteelBlueDark,
    onSecondary = Color(0xFF1B2A34),
    secondaryContainer = GraphiteSurfaceVariant,
    onSecondaryContainer = Color(0xFFD7DEE5),
    tertiary = SteelBlueDark,
    onTertiary = Color(0xFF1B2A34),
    background = GraphiteBackground,
    onBackground = Color(0xFFE6EAED),
    surface = GraphiteSurface,
    onSurface = Color(0xFFE6EAED),
    surfaceVariant = GraphiteSurfaceVariant,
    onSurfaceVariant = Color(0xFFAEB8C0),
    outline = GraphiteOutline,
    error = Color(0xFFFF6B6B),
)

/**
 * Тема приложения. Фирменные цвета включены всегда — динамические цвета
 * системы отключены, чтобы приложение выглядело одинаково на всех планшетах
 * предприятия.
 */
@Composable
fun QwantTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
