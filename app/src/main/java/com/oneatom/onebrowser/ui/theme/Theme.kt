package com.oneatom.onebrowser.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Dark color scheme - Pure monochrome
private val DarkColorScheme =
        darkColorScheme(
                primary = Color.White,
                onPrimary = Color.Black,
                primaryContainer = DarkHover,
                onPrimaryContainer = Color.White,
                secondary = DarkMuted,
                onSecondary = Color.Black,
                secondaryContainer = DarkHover,
                onSecondaryContainer = Color.White,
                tertiary = DarkMuted,
                onTertiary = Color.Black,
                background = DarkBackground,
                onBackground = DarkText,
                surface = DarkBackground,
                onSurface = DarkText,
                surfaceVariant = DarkHover,
                onSurfaceVariant = DarkMuted,
                outline = DarkBorder,
                outlineVariant = DarkBorder,
                inverseSurface = Color.White,
                inverseOnSurface = Color.Black,
                inversePrimary = Color.Black,
                surfaceTint = Color.White
        )

// Light color scheme - Pure monochrome
private val LightColorScheme =
        lightColorScheme(
                primary = Color.Black,
                onPrimary = Color.White,
                primaryContainer = LightHover,
                onPrimaryContainer = Color.Black,
                secondary = LightMuted,
                onSecondary = Color.White,
                secondaryContainer = LightHover,
                onSecondaryContainer = Color.Black,
                tertiary = LightMuted,
                onTertiary = Color.White,
                background = LightBackground,
                onBackground = LightText,
                surface = LightBackground,
                onSurface = LightText,
                surfaceVariant = LightHover,
                onSurfaceVariant = LightMuted,
                outline = LightBorder,
                outlineVariant = LightBorder,
                inverseSurface = Color.Black,
                inverseOnSurface = Color.White,
                inversePrimary = Color.White,
                surfaceTint = Color.Black
        )

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

@Composable
fun OneBrowserTheme(themeMode: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
    val darkTheme =
            when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Set status bar and navigation bar colors
            window.statusBarColor =
                    if (darkTheme) {
                        DarkBackground.toArgb()
                    } else {
                        LightBackground.toArgb()
                    }
            window.navigationBarColor =
                    if (darkTheme) {
                        DarkBackground.toArgb()
                    } else {
                        LightBackground.toArgb()
                    }

            // Set light/dark status bar icons
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

// Helper to check if current theme is dark
@Composable
fun isDarkTheme(themeMode: ThemeMode): Boolean {
    return when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
}
