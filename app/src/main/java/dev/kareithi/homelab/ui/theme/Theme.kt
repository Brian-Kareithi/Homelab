package dev.kareithi.homelab.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.kareithi.homelab.data.settings.ThemeMode
import dev.kareithi.homelab.domain.StatusLevel

/** The five brand colours, plus the few tones derived from them. */
object Palette {
    val BlueEnergy = Color(0xFF4392F1)
    val LavenderMist = Color(0xFFECE8EF)
    val Lavender = Color(0xFFE3EBFF)
    val AliceBlue = Color(0xFFE7F0FF)
    val Cinnabar = Color(0xFFDC493A)

    // Derived: text inks and the dark-mode navies share Blue Energy's hue (213°).
    val Ink = Color(0xFF12223A)
    val InkMuted = Color(0xFF5D6B84)
    val Paper = Color(0xFFFBFCFF)
    val PaperBorder = Color(0xFFD9E3F6)
    val Ember = Color(0xFFE8913A) // between Cinnabar and amber, for warnings
    val Iris = Color(0xFF8A6FDF) // Lavender Mist's hue (274°), saturated

    val Night = Color(0xFF0B1220)
    val NightTop = Color(0xFF101C33)
    val NightCard = Color(0xFF16233A)
    val NightBorder = Color(0xFF22324F)
    val NightMuted = Color(0xFF93A3BF)
}

@Immutable
data class StatusColors(
    val ok: Color = Palette.BlueEnergy,
    val warn: Color = Palette.Ember,
    val error: Color = Palette.Cinnabar,
    val lan: Color = Palette.BlueEnergy,
    val tailscale: Color = Palette.Iris,
) {
    fun of(level: StatusLevel): Color = when (level) {
        StatusLevel.OK -> ok
        StatusLevel.WARN -> warn
        StatusLevel.ERROR -> error
    }
}

/** Tokens Material's scheme doesn't model: card surfaces, background gradient, skeletons. */
@Immutable
data class HomelabColors(
    val card: Color,
    val cardBorder: Color,
    val statBlock: Color,
    val accent: Color,
    val bgTop: Color,
    val bgBottom: Color,
    val skeletonBase: Color,
    val skeletonHighlight: Color,
    val shadow: Color,
    val isDark: Boolean,
    val status: StatusColors = StatusColors(),
) {
    val accentSoft: Color get() = accent.copy(alpha = if (isDark) 0.18f else 0.12f)
}

private fun lightTokens(accent: Color) = HomelabColors(
    card = Palette.Paper,
    cardBorder = Palette.PaperBorder,
    statBlock = Palette.Lavender,
    accent = accent,
    bgTop = Palette.AliceBlue,
    bgBottom = Palette.LavenderMist,
    skeletonBase = Palette.Lavender,
    skeletonHighlight = Color.White.copy(alpha = 0.85f),
    shadow = accent.copy(alpha = 0.22f),
    isDark = false,
)

private fun darkTokens(accent: Color) = HomelabColors(
    card = Palette.NightCard,
    cardBorder = Palette.NightBorder,
    statBlock = Palette.Night.copy(alpha = 0.7f),
    accent = accent,
    bgTop = Palette.NightTop,
    bgBottom = Palette.Night,
    skeletonBase = Color(0xFF1D2B45),
    skeletonHighlight = Palette.AliceBlue.copy(alpha = 0.10f),
    shadow = Color.Black.copy(alpha = 0.5f),
    isDark = true,
)

val LocalHomelabColors = staticCompositionLocalOf { lightTokens(Palette.BlueEnergy) }

private fun onColorFor(c: Color): Color = if (c.luminance() > 0.5f) Palette.Ink else Color.White

private fun lightScheme(accent: Color): ColorScheme = lightColorScheme(
    primary = accent,
    onPrimary = onColorFor(accent),
    primaryContainer = Palette.Lavender,
    onPrimaryContainer = Palette.Ink,
    secondary = accent,
    onSecondary = onColorFor(accent),
    secondaryContainer = Palette.AliceBlue,
    onSecondaryContainer = Palette.Ink,
    tertiary = Palette.Cinnabar,
    onTertiary = Color.White,
    background = Palette.LavenderMist,
    onBackground = Palette.Ink,
    surface = Palette.LavenderMist,
    onSurface = Palette.Ink,
    surfaceVariant = Palette.AliceBlue,
    onSurfaceVariant = Palette.InkMuted,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Palette.Paper,
    surfaceContainer = Palette.Paper,
    surfaceContainerHigh = Palette.AliceBlue,
    surfaceContainerHighest = Palette.Lavender,
    outline = Palette.PaperBorder,
    outlineVariant = Palette.PaperBorder,
    error = Palette.Cinnabar,
    onError = Color.White,
)

private fun darkScheme(accent: Color): ColorScheme = darkColorScheme(
    primary = accent,
    onPrimary = onColorFor(accent),
    primaryContainer = lerp(Palette.NightCard, accent, 0.3f),
    onPrimaryContainer = Palette.AliceBlue,
    secondary = accent,
    onSecondary = onColorFor(accent),
    secondaryContainer = Palette.NightBorder,
    onSecondaryContainer = Palette.AliceBlue,
    tertiary = Palette.Cinnabar,
    onTertiary = Color.White,
    background = Palette.Night,
    onBackground = Palette.AliceBlue,
    surface = Palette.Night,
    onSurface = Palette.AliceBlue,
    surfaceVariant = Palette.NightCard,
    onSurfaceVariant = Palette.NightMuted,
    surfaceContainerLowest = Palette.Night,
    surfaceContainerLow = Palette.NightCard,
    surfaceContainer = Palette.NightCard,
    surfaceContainerHigh = Palette.NightCard,
    surfaceContainerHighest = Palette.NightBorder,
    outline = Palette.NightBorder,
    outlineVariant = Palette.NightBorder,
    error = Palette.Cinnabar,
    onError = Color.White,
)

private val HomelabTypography = Typography().let { t ->
    t.copy(
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = t.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = t.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        labelSmall = t.labelSmall.copy(letterSpacing = 0.6.sp, fontWeight = FontWeight.Medium),
    )
}

@Composable
fun HomelabTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    accent: Color = Palette.BlueEnergy,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val useDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val scheme = when {
        useDynamic && dark -> dynamicDarkColorScheme(LocalContext.current)
        useDynamic -> dynamicLightColorScheme(LocalContext.current)
        dark -> darkScheme(accent)
        else -> lightScheme(accent)
    }
    val tokens = when {
        useDynamic -> (if (dark) darkTokens(scheme.primary) else lightTokens(scheme.primary)).copy(
            card = scheme.surfaceContainer,
            cardBorder = scheme.outlineVariant,
            statBlock = scheme.surfaceContainerHighest.copy(alpha = 0.7f),
            bgTop = scheme.surfaceContainerLow,
            bgBottom = scheme.background,
        )
        dark -> darkTokens(accent)
        else -> lightTokens(accent)
    }
    CompositionLocalProvider(LocalHomelabColors provides tokens) {
        MaterialTheme(colorScheme = scheme, typography = HomelabTypography, content = content)
    }
}

object HomelabTheme {
    val colors: HomelabColors
        @Composable get() = LocalHomelabColors.current
}
