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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.kareithi.homelab.data.settings.ThemeMode
import dev.kareithi.homelab.domain.StatusLevel

/** Homepage's slate palette (Tailwind). */
object Slate {
    val S50 = Color(0xFFF8FAFC)
    val S100 = Color(0xFFF1F5F9)
    val S200 = Color(0xFFE2E8F0)
    val S300 = Color(0xFFCBD5E1)
    val S400 = Color(0xFF94A3B8)
    val S500 = Color(0xFF64748B)
    val S600 = Color(0xFF475569)
    val S700 = Color(0xFF334155)
    val S800 = Color(0xFF1E293B)
    val S900 = Color(0xFF0F172A)
}

@Immutable
data class StatusColors(
    val ok: Color = Color(0xFF22C55E),
    val warn: Color = Color(0xFFF59E0B),
    val error: Color = Color(0xFFEF4444),
    val lan: Color = Color(0xFF22C55E),
    val tailscale: Color = Color(0xFF3B82F6),
) {
    fun of(level: StatusLevel): Color = when (level) {
        StatusLevel.OK -> ok
        StatusLevel.WARN -> warn
        StatusLevel.ERROR -> error
    }
}

/** Extra tokens that Material's scheme doesn't model directly. */
@Immutable
data class HomelabColors(
    val card: Color,
    val cardBorder: Color,
    val statBlock: Color,
    val accent: Color,
    val status: StatusColors = StatusColors(),
    val isDark: Boolean,
)

val LocalHomelabColors = staticCompositionLocalOf {
    HomelabColors(
        card = Slate.S800, cardBorder = Slate.S700, statBlock = Slate.S900.copy(alpha = 0.6f),
        accent = Color(0xFF06B6D4), isDark = true,
    )
}

private fun onColorFor(c: Color): Color = if (c.luminance() > 0.45f) Slate.S900 else Color.White

private fun darkScheme(accent: Color): ColorScheme = darkColorScheme(
    primary = accent,
    onPrimary = onColorFor(accent),
    primaryContainer = accent.copy(alpha = 0.25f).compositeOverSlate(Slate.S800),
    onPrimaryContainer = Color.White,
    secondary = accent,
    onSecondary = onColorFor(accent),
    secondaryContainer = Slate.S700,
    onSecondaryContainer = Color.White,
    background = Slate.S900,
    onBackground = Color.White,
    surface = Slate.S900,
    onSurface = Color.White,
    surfaceVariant = Slate.S800,
    onSurfaceVariant = Slate.S400,
    surfaceContainerLowest = Slate.S900,
    surfaceContainerLow = Slate.S800,
    surfaceContainer = Slate.S800,
    surfaceContainerHigh = Slate.S800,
    surfaceContainerHighest = Slate.S700,
    outline = Slate.S700,
    outlineVariant = Slate.S700,
    error = Color(0xFFF87171),
    onError = Slate.S900,
)

private fun lightScheme(accent: Color): ColorScheme = lightColorScheme(
    primary = accent,
    onPrimary = onColorFor(accent),
    primaryContainer = accent.copy(alpha = 0.18f).compositeOverSlate(Color.White),
    onPrimaryContainer = Slate.S900,
    secondary = accent,
    onSecondary = onColorFor(accent),
    secondaryContainer = Slate.S200,
    onSecondaryContainer = Slate.S900,
    background = Slate.S100,
    onBackground = Slate.S900,
    surface = Slate.S100,
    onSurface = Slate.S900,
    surfaceVariant = Color.White,
    onSurfaceVariant = Slate.S500,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Slate.S50,
    surfaceContainerHighest = Slate.S200,
    outline = Slate.S300,
    outlineVariant = Slate.S200,
    error = Color(0xFFDC2626),
    onError = Color.White,
)

private fun Color.compositeOverSlate(bg: Color): Color {
    val a = alpha
    return Color(
        red = red * a + bg.red * (1 - a),
        green = green * a + bg.green * (1 - a),
        blue = blue * a + bg.blue * (1 - a),
        alpha = 1f,
    )
}

private val HomelabTypography = Typography().let { t ->
    t.copy(
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = t.titleSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp),
        labelSmall = t.labelSmall.copy(letterSpacing = 0.3.sp),
    )
}

@Composable
fun HomelabTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    accent: Color = Color(0xFF06B6D4),
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
    val extra = when {
        useDynamic -> HomelabColors(
            card = scheme.surfaceContainer,
            cardBorder = scheme.outlineVariant,
            statBlock = scheme.surfaceContainerHighest.copy(alpha = 0.6f),
            accent = scheme.primary,
            isDark = dark,
        )
        dark -> HomelabColors(
            card = Slate.S800, cardBorder = Slate.S700, statBlock = Slate.S900.copy(alpha = 0.6f),
            accent = accent, isDark = true,
        )
        else -> HomelabColors(
            card = Color.White, cardBorder = Slate.S200, statBlock = Slate.S100,
            accent = accent, isDark = false,
        )
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalHomelabColors provides extra) {
        MaterialTheme(colorScheme = scheme, typography = HomelabTypography, content = content)
    }
}

object HomelabTheme {
    val colors: HomelabColors
        @Composable get() = LocalHomelabColors.current
}
