package com.playreward.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class PlayRewardColors(
    val canvas: Color,
    val card: Color,
    val cardBorder: Color,
    val inputBg: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val primaryIndigo: Color,
    val primaryViolet: Color,
    val darkCta: Color,
    val lavenderSoft: Color,
    val amberSoft: Color,
    val amberAccent: Color,
    val roseSoft: Color,
    val roseBorder: Color,
    val roseAccent: Color,
    val mintSoft: Color,
    val mintAccent: Color,
    val cyanAccent: Color,
    val heroGradient: Brush,
    val megaGradient: Brush,
    val superGradient: Brush,
    val warmBannerGradient: Brush,
    val isDark: Boolean
)

val LightPlayRewardColors = PlayRewardColors(
    canvas = Color(0xFFF4F5FA),
    card = Color(0xFFFFFFFF),
    cardBorder = Color(0xFFE8EAF2),
    inputBg = Color(0xFFF1F3F9),
    textPrimary = Color(0xFF111827),
    textSecondary = Color(0xFF6B7280),
    primaryIndigo = Color(0xFF5B6AF0),
    primaryViolet = Color(0xFF8B5CF6),
    darkCta = Color(0xFF111827),
    lavenderSoft = Color(0xFFEEF2FF),
    amberSoft = Color(0xFFFEF3C7),
    amberAccent = Color(0xFFF59E0B),
    roseSoft = Color(0xFFFEF2F2),
    roseBorder = Color(0xFFFECACA),
    roseAccent = Color(0xFFEF4444),
    mintSoft = Color(0xFFECFDF5),
    mintAccent = Color(0xFF10B981),
    cyanAccent = Color(0xFF06B6D4),
    heroGradient = Brush.linearGradient(listOf(Color(0xFF5B6AF0), Color(0xFF8B5CF6))),
    megaGradient = Brush.linearGradient(listOf(Color(0xFFFBBF24), Color(0xFFF97316), Color(0xFFFB7185))),
    superGradient = Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF818CF8), Color(0xFFA78BFA))),
    warmBannerGradient = Brush.linearGradient(listOf(Color(0xFFFFF7ED), Color(0xFFFFEDD5))),
    isDark = false
)

val DarkPlayRewardColors = PlayRewardColors(
    canvas = Color(0xFF0F111A),
    card = Color(0xFF1A1D2E),
    cardBorder = Color(0xFF2B3048),
    inputBg = Color(0xFF23273D),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    primaryIndigo = Color(0xFF6C7BFF),
    primaryViolet = Color(0xFFA78BFA),
    darkCta = Color(0xFFF8FAFC),
    lavenderSoft = Color(0xFF252A4A),
    amberSoft = Color(0xFF3A2D12),
    amberAccent = Color(0xFFFBBF24),
    roseSoft = Color(0xFF3A1A22),
    roseBorder = Color(0xFF7F1D1D),
    roseAccent = Color(0xFFF87171),
    mintSoft = Color(0xFF13332B),
    mintAccent = Color(0xFF34D399),
    cyanAccent = Color(0xFF22D3EE),
    heroGradient = Brush.linearGradient(listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))),
    megaGradient = Brush.linearGradient(listOf(Color(0xFFD97706), Color(0xFFEA580C), Color(0xFFE11D48))),
    superGradient = Brush.linearGradient(listOf(Color(0xFF4F46E5), Color(0xFF6366F1), Color(0xFF8B5CF6))),
    warmBannerGradient = Brush.linearGradient(listOf(Color(0xFF2B2216), Color(0xFF382716))),
    isDark = true
)

val LocalPlayRewardColors = staticCompositionLocalOf { LightPlayRewardColors }

private val LightM3Scheme = lightColorScheme(
    primary = Color(0xFF5B6AF0),
    onPrimary = Color.White,
    secondary = Color(0xFF8B5CF6),
    background = Color(0xFFF4F5FA),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF111827),
    onSurface = Color(0xFF111827),
    error = Color(0xFFEF4444)
)

private val DarkM3Scheme = darkColorScheme(
    primary = Color(0xFF6C7BFF),
    onPrimary = Color.White,
    secondary = Color(0xFFA78BFA),
    background = Color(0xFF0F111A),
    surface = Color(0xFF1A1D2E),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    error = Color(0xFFF87171)
)

private val PlayRewardTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 13.5.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.5.sp, lineHeight = 15.sp)
)

enum class ThemeMode { LIGHT, DARK, SYSTEM }

@Composable
fun PlayRewardTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val customColors = if (useDark) DarkPlayRewardColors else LightPlayRewardColors
    val m3Scheme = if (useDark) DarkM3Scheme else LightM3Scheme

    CompositionLocalProvider(LocalPlayRewardColors provides customColors) {
        MaterialTheme(
            colorScheme = m3Scheme,
            typography = PlayRewardTypography,
            content = content
        )
    }
}

object PRTheme {
    val colors: PlayRewardColors
        @Composable get() = LocalPlayRewardColors.current
}
