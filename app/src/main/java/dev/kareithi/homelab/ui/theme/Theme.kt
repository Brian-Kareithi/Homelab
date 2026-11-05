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
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
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

    // Light mode inks share Blue Energy's hue (213°).
    val Ink = Color(0xFF12223A)
    val InkMuted = Color(0xFF55627A)
    val Paper = Color(0xFFFBFCFF)
    val PaperBorder = Color(0xFFD9E3F6)
    val PaperOutline = Color(0xFF7B89A3)
    val Ember = Color(0xFFE8913A) // between Cinnabar and amber, for warnings
    val Iris = Color(0xFF8A6FDF) // Lavender Mist's hue (274°), saturated

    // Dark mode: true black canvas, near-black surfaces with a trace of the brand hue.
    val Black = Color(0xFF000000)
    val NightTop = Color(0xFF05080E)
    val NightCard = Color(0xFF0E121A)
    val NightBlock = Color(0xFF161B25)
    val NightBorder = Color(0xFF1F2633)
    val NightOutline = Color(0xFF66718A)
    val NightText = Color(0xFFEFF4FD) // Alice Blue, a touch brighter on black
    val NightMuted = Color(0xFF9AA6BC)
}

/**
 * Status colours. Fills (dots, gauges, bars) only need 3:1; the matching text tones are
 * adjusted to 4.5:1 against the pill or card they sit on.
 */
@Immutable
data class StatusColors(
    val ok: Color,
    val warn: Color,
    val error: Color,
    val lan: Color,
    val tailscale: Color,
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
    /** Accent for graphics (arcs, bars, icons): at least 3:1 on cards. */
    val accent: Color,
    /** Accent as text on cards and soft accent chips: at least 4.5:1. */
    val accentText: Color,
    /** Solid accent surface (selected drawer item, buttons) and the content drawn on it. */
    val accentFill: Color,
    val onAccentFill: Color,
    val bgTop: Color,
    val bgBottom: Color,
    val text: Color,
    val textMuted: Color,
    val skeletonBase: Color,
    val skeletonHighlight: Color,
    val shadow: Color,
    val isDark: Boolean,
    val status: StatusColors,
) {
    val accentSoft: Color get() = accent.copy(alpha = if (isDark) 0.20f else 0.12f)

    /** A tinted pill/chip background for [color] over a card. */
    fun tint(color: Color, alpha: Float = if (isDark) 0.18f else 0.13f): Color =
        color.copy(alpha = alpha).compositeOver(card)

    /** [color] adjusted to be legible as text on [background] (defaults to its own tint). */
    fun textOn(color: Color, background: Color = tint(color)): Color = color.readableOn(background)
}

val LocalHomelabColors = staticCompositionLocalOf { buildTheme(dark = false, accent = Palette.BlueEnergy).second }

/**
 * Builds the Material scheme and Homelab tokens. Pure (no Android calls) so the contrast
 * guarantees are unit-tested. Pass [dynamic] to derive everything from a wallpaper scheme.
 */
fun buildTheme(dark: Boolean, accent: Color, dynamic: ColorScheme? = null): Pair<ColorScheme, HomelabColors> =
    if (dynamic != null) dynamicTheme(dark, dynamic) else brandTheme(dark, accent)

private fun brandTheme(dark: Boolean, rawAccent: Color): Pair<ColorScheme, HomelabColors> {
    val card = if (dark) Palette.NightCard else Palette.Paper
    val block = if (dark) Palette.NightBlock else Palette.Lavender
    val text = if (dark) Palette.NightText else Palette.Ink
    // Muted text must hold up on the darker of card / stat block.
    val muted = (if (dark) Palette.NightMuted else Palette.InkMuted).readableOn(block).readableOn(card)

    // Graphics accent (arcs, dots, icons sit on cards): any user-picked colour stays visible,
    // e.g. Ink on black gets lifted, while Blue Energy already passes and is kept exact.
    val accent = rawAccent.readableOn(card, Contrast.GRAPHIC)
    val accentFill = if (dark) accent else accent.readableOn(Color.White) // darker so white text passes
    val onAccentFill = if (dark) bestContentOn(accentFill) else Color.White
    val accentText = accent.readableOn(accent.copy(alpha = if (dark) 0.20f else 0.12f).compositeOver(card))
        .readableOn(card)

    val status = StatusColors(
        ok = accent,
        warn = Palette.Ember.readableOn(card, Contrast.GRAPHIC),
        error = Palette.Cinnabar.readableOn(card, Contrast.GRAPHIC),
        lan = accent,
        tailscale = Palette.Iris.readableOn(card, Contrast.GRAPHIC),
    )
    val error = Palette.Cinnabar.readableOn(card)

    val scheme = if (dark) {
        darkColorScheme(
            primary = accentFill,
            onPrimary = onAccentFill,
            primaryContainer = lerp(card, accent, 0.28f),
            onPrimaryContainer = text,
            secondary = accentFill,
            onSecondary = onAccentFill,
            secondaryContainer = lerp(Palette.NightBorder, accent, 0.22f),
            onSecondaryContainer = text,
            tertiary = error,
            onTertiary = Palette.Black,
            background = Palette.Black,
            onBackground = text,
            surface = Palette.Black,
            onSurface = text,
            surfaceVariant = card,
            onSurfaceVariant = muted,
            surfaceContainerLowest = Palette.Black,
            surfaceContainerLow = card,
            surfaceContainer = card,
            surfaceContainerHigh = block,
            surfaceContainerHighest = Palette.NightBorder,
            inverseSurface = text,
            inverseOnSurface = Palette.Black,
            outline = Palette.NightOutline,
            outlineVariant = Palette.NightBorder,
            error = error,
            onError = Palette.Black,
            scrim = Palette.Black,
        )
    } else {
        lightColorScheme(
            primary = accentFill,
            onPrimary = onAccentFill,
            primaryContainer = Palette.Lavender,
            onPrimaryContainer = text,
            secondary = accentFill,
            onSecondary = onAccentFill,
            secondaryContainer = Palette.Lavender,
            onSecondaryContainer = text,
            tertiary = error,
            onTertiary = Color.White,
            background = Palette.LavenderMist,
            onBackground = text,
            surface = Palette.LavenderMist,
            onSurface = text,
            surfaceVariant = Palette.AliceBlue,
            onSurfaceVariant = muted,
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = card,
            surfaceContainer = card,
            surfaceContainerHigh = Palette.AliceBlue,
            surfaceContainerHighest = block,
            outline = Palette.PaperOutline,
            outlineVariant = Palette.PaperBorder,
            error = error,
            onError = Color.White,
        )
    }

    val tokens = HomelabColors(
        card = card,
        cardBorder = if (dark) Palette.NightBorder else Palette.PaperBorder,
        statBlock = block,
        accent = accent,
        accentText = accentText,
        accentFill = accentFill,
        onAccentFill = onAccentFill,
        bgTop = if (dark) Palette.NightTop else Palette.AliceBlue,
        bgBottom = if (dark) Palette.Black else Palette.LavenderMist,
        text = text,
        textMuted = muted,
        skeletonBase = if (dark) Palette.NightBlock else Palette.Lavender,
        skeletonHighlight = if (dark) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.85f),
        shadow = if (dark) Color.Black else accent.copy(alpha = 0.22f),
        isDark = dark,
        status = status,
    )
    return scheme to tokens
}

/**
 * Wallpaper-based theme (Android 12+). Every surface, accent and status tone comes from the
 * system scheme; dark mode still sits on true black, with the tinted surfaces as cards.
 */
private fun dynamicTheme(dark: Boolean, base: ColorScheme): Pair<ColorScheme, HomelabColors> {
    val scheme = if (dark) {
        base.copy(
            background = Palette.Black,
            surface = Palette.Black,
            surfaceContainerLowest = Palette.Black,
        )
    } else {
        base
    }
    val card = if (dark) scheme.surfaceContainer else scheme.surfaceContainerLowest
    val block = scheme.surfaceContainerHigh
    val muted = scheme.onSurfaceVariant.readableOn(block).readableOn(card)
    val accent = scheme.primary.readableOn(card, Contrast.GRAPHIC)
    // Harmonise the fixed status hues toward the wallpaper so they don't clash.
    fun harmonise(c: Color) = lerp(c, scheme.primary, 0.18f).readableOn(card, Contrast.GRAPHIC)

    val tokens = HomelabColors(
        card = card,
        cardBorder = scheme.outlineVariant,
        statBlock = block,
        accent = accent,
        accentText = scheme.primary.readableOn(accent.copy(alpha = if (dark) 0.20f else 0.12f).compositeOver(card))
            .readableOn(card),
        accentFill = scheme.primary,
        onAccentFill = scheme.onPrimary.readableOn(scheme.primary),
        bgTop = if (dark) lerp(Palette.Black, scheme.primary, 0.04f) else scheme.surfaceContainerLow,
        bgBottom = if (dark) Palette.Black else scheme.surfaceContainer,
        text = scheme.onSurface,
        textMuted = muted,
        skeletonBase = scheme.surfaceContainerHighest,
        skeletonHighlight = if (dark) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.7f),
        shadow = if (dark) Color.Black else scheme.primary.copy(alpha = 0.18f),
        isDark = dark,
        status = StatusColors(
            ok = accent,
            warn = harmonise(Palette.Ember),
            error = scheme.error.readableOn(card, Contrast.GRAPHIC),
            lan = accent,
            tailscale = scheme.tertiary.readableOn(card, Contrast.GRAPHIC),
        ),
    )
    return scheme.copy(onSurfaceVariant = muted) to tokens
}

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
    val context = LocalContext.current
    val useDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val dynamicScheme = when {
        !useDynamic -> null
        dark -> dynamicDarkColorScheme(context)
        else -> dynamicLightColorScheme(context)
    }
    val (scheme, tokens) = remember(dark, accent, dynamicScheme) { buildTheme(dark, accent, dynamicScheme) }
    CompositionLocalProvider(LocalHomelabColors provides tokens) {
        MaterialTheme(colorScheme = scheme, typography = HomelabTypography, content = content)
    }
}

object HomelabTheme {
    val colors: HomelabColors
        @Composable get() = LocalHomelabColors.current
}
