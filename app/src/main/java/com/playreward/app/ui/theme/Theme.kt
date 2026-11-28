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
    val cardSheenGradient: Brush,
    val isDark: Boolean
)

val LightPlayRewardColors = PlayRewardColors(
    canvas = Color(0xFFF3F4FA),
    card = Color(0xFFFFFFFF),
    cardBorder = Color(0xFFE3E7F5),
    inputBg = Color(0xFFF0F2FA),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF64748B),
    primaryIndigo = Color(0xFF4F46E5),
    primaryViolet = Color(0xFF7C3AED),
    darkCta = Color(0xFF0F172A),
    lavenderSoft = Color(0xFFEEF2FF),
    amberSoft = Color(0xFFFEF3C7),
    amberAccent = Color(0xFFF59E0B),
    roseSoft = Color(0xFFFEF2F2),
    roseBorder = Color(0xFFFECACA),
    roseAccent = Color(0xFFEF4444),
    mintSoft = Color(0xFFECFDF5),
    mintAccent = Color(0xFF10B981),
    cyanAccent = Color(0xFF06B6D4),
    heroGradient = Brush.linearGradient(listOf(Color(0xFF4F46E5), Color(0xFF7C3AED), Color(0xFF9333EA))),
    megaGradient = Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFF97316), Color(0xFFEC4899))),
    superGradient = Brush.linearGradient(listOf(Color(0xFF4F46E5), Color(0xFF6366F1), Color(0xFF8B5CF6))),
    warmBannerGradient = Brush.linearGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFFEDD5))),
    cardSheenGradient = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFFAFBFD))),
    isDark = false
)

val DarkPlayRewardColors = PlayRewardColors(
    canvas = Color(0xFF090C16),
    card = Color(0xFF13182B),
    cardBorder = Color(0xFF253052),
    inputBg = Color(0xFF1B223A),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    primaryIndigo = Color(0xFF7382FF),
    primaryViolet = Color(0xFFA78BFA),
    darkCta = Color(0xFFF8FAFC),
    lavenderSoft = Color(0xFF1E2647),
    amberSoft = Color(0xFF332610),
    amberAccent = Color(0xFFFBBF24),
    roseSoft = Color(0xFF361720),
    roseBorder = Color(0xFF7F1D1D),
    roseAccent = Color(0xFFF87171),
    mintSoft = Color(0xFF102E26),
    mintAccent = Color(0xFF34D399),
    cyanAccent = Color(0xFF22D3EE),
    heroGradient = Brush.linearGradient(listOf(Color(0xFF4F46E5), Color(0xFF7C3AED), Color(0xFF9333EA))),
    megaGradient = Brush.linearGradient(listOf(Color(0xFFEA580C), Color(0xFFF97316), Color(0xFFE11D48))),
    superGradient = Brush.linearGradient(listOf(Color(0xFF4338CA), Color(0xFF6366F1), Color(0xFF8B5CF6))),
    warmBannerGradient = Brush.linearGradient(listOf(Color(0xFF2A1F12), Color(0xFF382414))),
    cardSheenGradient = Brush.verticalGradient(listOf(Color(0xFF171E36), Color(0xFF121729))),
    isDark = true
)

val LocalPlayRewardColors = staticCompositionLocalOf { LightPlayRewardColors }

private val LightM3Scheme = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    secondary = Color(0xFF7C3AED),
    background = Color(0xFFF3F4FA),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    error = Color(0xFFEF4444)
)

private val DarkM3Scheme = darkColorScheme(
    primary = Color(0xFF7382FF),
    onPrimary = Color.White,
    secondary = Color(0xFFA78BFA),
    background = Color(0xFF090C16),
    surface = Color(0xFF13182B),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    error = Color(0xFFF87171)
)

private val PlayRewardTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp, lineHeight = 26.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.5.sp, lineHeight = 20.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, lineHeight = 17.sp),
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
