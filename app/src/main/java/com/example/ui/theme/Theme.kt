package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ShieldGoldLight,
    onPrimary = Color(0xFF1E1700),
    primaryContainer = ShieldGoldDark,
    onPrimaryContainer = Color(0xFFFFE082),
    secondary = ShieldCyanAccent,
    onSecondary = Color(0xFF001F2A),
    secondaryContainer = Color(0xFF0369A1),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = ShieldEmeraldSafe,
    onTertiary = Color(0xFF00210E),
    tertiaryContainer = Color(0xFF047857),
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = DarkBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    error = ShieldRoseRisk,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = ShieldGoldDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF78350F),
    secondary = ShieldCyanAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    tertiary = ShieldEmeraldSafe,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF065F46),
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    error = ShieldRoseRisk,
    onError = Color.White
)

@Composable
fun ProjectShieldTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our handcrafted security theme by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
