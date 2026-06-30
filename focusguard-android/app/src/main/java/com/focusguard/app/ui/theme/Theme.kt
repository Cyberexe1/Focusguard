package com.focusguard.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── Light theme — White · Light Blue · Light Violet ───────────────────────────

data class FocusGuardColors(
    // Canvas
    val background: Color              = Color(0xFFF5F6FF),   // soft lavender-white
    val backgroundSecondary: Color     = Color(0xFFEEF2FF),   // slightly deeper tint

    // Surfaces
    val surface: Color                 = Color(0xFFFFFFFF),   // pure white cards
    val surfaceVariant: Color          = Color(0xFFF0F4FF),   // blue-tinted surface
    val surfaceContainerLow: Color     = Color(0xFFF8F9FF),
    val surfaceContainer: Color        = Color(0xFFEEF2FF),
    val surfaceContainerHigh: Color    = Color(0xFFE4EBFF),
    val surfaceContainerHighest: Color = Color(0xFFD8E3FF),
    val surfaceGlass: Color            = Color(0xFFFFFFFF).copy(alpha = 0.80f),

    // Brand — violet + blue
    val primary: Color                 = Color(0xFF6366F1),   // indigo-violet
    val primaryVariant: Color          = Color(0xFF818CF8),   // light indigo
    val primaryLight: Color            = Color(0xFFE0E7FF),   // very light indigo
    val inversePrimary: Color          = Color(0xFF4F46E5),   // pressed deep indigo
    val onPrimary: Color               = Color(0xFFFFFFFF),
    val primaryContainer: Color        = Color(0xFFEEF2FF),
    val onPrimaryContainer: Color      = Color(0xFF3730A3),

    // Secondary — sky blue
    val secondary: Color               = Color(0xFF38BDF8),   // sky blue
    val secondaryVariant: Color        = Color(0xFF7DD3FC),   // light sky
    val secondaryLight: Color          = Color(0xFFE0F2FE),   // very light blue
    val secondaryContainer: Color      = Color(0xFFE0F2FE),
    val onSecondaryContainer: Color    = Color(0xFF0369A1),

    // Text
    val onBackground: Color            = Color(0xFF1E1B4B),   // deep indigo-black
    val onSurface: Color               = Color(0xFF1E1B4B),
    val onSurfaceVariant: Color        = Color(0xFF6B7280),   // medium grey
    val onSurfaceMuted: Color          = Color(0xFF9CA3AF),   // light grey
    val outline: Color                 = Color(0xFFE5E7EB),   // light border
    val outlineVariant: Color          = Color(0xFFF3F4F6),

    // Functional
    val error: Color                   = Color(0xFFEF4444),
    val errorContainer: Color          = Color(0xFFFEE2E2),
    val warning: Color                 = Color(0xFFF59E0B),
    val warningContainer: Color        = Color(0xFFFEF3C7),
    val success: Color                 = Color(0xFF10B981),
    val successContainer: Color        = Color(0xFFD1FAE5),

    // Tertiary
    val tertiary: Color                = Color(0xFFA78BFA),   // soft violet
    val tertiaryContainer: Color       = Color(0xFFEDE9FE),

    // Shadows (subtle on light theme)
    val shadowPrimary: Color           = Color(0xFF6366F1).copy(alpha = 0.15f),
    val shadowCard: Color              = Color(0xFF6366F1).copy(alpha = 0.08f),
    val shadowBlue: Color              = Color(0xFF38BDF8).copy(alpha = 0.15f),
)

// ── Gradient helpers ──────────────────────────────────────────────────────────
object FgGradients {
    // Background — very subtle lavender-white gradient
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFFF5F6FF), Color(0xFFEEF2FF), Color(0xFFF0F4FF)),
    )

    // Primary pill — indigo to violet
    val primaryClay = Brush.linearGradient(
        colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)),
    )

    // Blue pill — sky to blue
    val blueClay = Brush.linearGradient(
        colors = listOf(Color(0xFF38BDF8), Color(0xFF0EA5E9)),
    )

    // Success
    val successClay = Brush.linearGradient(
        colors = listOf(Color(0xFF34D399), Color(0xFF10B981)),
    )

    // Error
    val errorClay = Brush.linearGradient(
        colors = listOf(Color(0xFFF87171), Color(0xFFEF4444)),
    )

    // Warning
    val warningClay = Brush.linearGradient(
        colors = listOf(Color(0xFFFBBF24), Color(0xFFF59E0B)),
    )

    // Pink/violet accent
    val pinkClay = Brush.linearGradient(
        colors = listOf(Color(0xFFA78BFA), Color(0xFF8B5CF6)),
    )

    // Card header — light indigo tint
    val cardHeader = Brush.linearGradient(
        colors = listOf(Color(0xFFEEF2FF), Color(0xFFE0E7FF)),
    )

    // Hero card — indigo gradient
    val heroBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF6366F1), Color(0xFF4F46E5)),
    )

    // Blue hero
    val blueHeroBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7)),
    )

    // Subtle card border
    val glassBorder = Brush.linearGradient(
        colors = listOf(Color(0xFFE0E7FF), Color(0xFFEEF2FF)),
    )
}

val LocalFocusGuardColors = staticCompositionLocalOf { FocusGuardColors() }

object FocusGuardTheme {
    val colors: FocusGuardColors
        @Composable get() = LocalFocusGuardColors.current
}

@Composable
fun FocusGuardTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalFocusGuardColors provides FocusGuardColors(),
        content = content,
    )
}
