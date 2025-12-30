package com.oneatom.onebrowser.ui.theme

import androidx.compose.ui.graphics.Color

// Pure Monochrome Palette - Dark Mode
val DarkBackground = Color(0xFF000000)
val DarkText = Color(0xFFFFFFFF)
val DarkMuted = Color(0xFF888888)
val DarkBorder = Color(0xFF222222)
val DarkHover = Color(0xFF111111)

// Pure Monochrome Palette - Light Mode
val LightBackground = Color(0xFFFFFFFF)
val LightText = Color(0xFF000000)
val LightMuted = Color(0xFF666666)
val LightBorder = Color(0xFFE0E0E0)
val LightHover = Color(0xFFF5F5F5)

// Glass Effects
object GlassColors {
    // Dark mode glass
    val DarkGlassBackground = Color(0x08FFFFFF) // rgba(255,255,255,0.03)
    val DarkGlassBorder = Color(0x14FFFFFF) // rgba(255,255,255,0.08)

    // Light mode glass
    val LightGlassBackground = Color(0x08000000) // rgba(0,0,0,0.03)
    val LightGlassBorder = Color(0x14000000) // rgba(0,0,0,0.08)

    // Scrollbar colors
    val DarkScrollbar = Color(0x33FFFFFF) // rgba(255,255,255,0.2)
    val LightScrollbar = Color(0x33000000) // rgba(0,0,0,0.2)
}

// Input field backgrounds
val DarkInputBackground = Color(0x0DFFFFFF) // ~5% white
val LightInputBackground = Color(0x0D000000) // ~5% black
