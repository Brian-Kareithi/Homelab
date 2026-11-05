package dev.kareithi.homelab.ui.theme

import androidx.compose.ui.graphics.Color
import dev.kareithi.homelab.data.settings.Defaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * Every text/background pairing the UI uses must meet WCAG AA (4.5:1) and every graphic
 * (gauge arcs, dots, icons) 3:1, in light and dark mode, for every accent the user can pick.
 */
class ThemeContrastTest {

    private val failures = mutableListOf<String>()

    private fun check(name: String, fg: Color, bg: Color, min: Float) {
        val ratio = contrastRatio(fg, bg)
        if (ratio < min) failures += String.format(Locale.US, "%s: %.2f < %.1f", name, ratio, min)
    }

    private fun verify(label: String, dark: Boolean, accent: Color) {
        val (scheme, c) = buildTheme(dark, accent)
        val p = "$label/${if (dark) "dark" else "light"}"

        // Body text on every surface it appears on.
        listOf("card" to c.card, "statBlock" to c.statBlock, "bgTop" to c.bgTop, "bgBottom" to c.bgBottom).forEach { (n, bg) ->
            check("$p text on $n", c.text, bg, Contrast.TEXT)
        }
        check("$p muted on card", c.textMuted, c.card, Contrast.TEXT)
        check("$p muted on statBlock", c.textMuted, c.statBlock, Contrast.TEXT)
        check("$p onSurfaceVariant on card", scheme.onSurfaceVariant, c.card, Contrast.TEXT)

        // Accent: graphics, text on cards and chips, solid fill with content.
        check("$p accent graphic on card", c.accent, c.card, Contrast.GRAPHIC)
        check("$p accentText on card", c.accentText, c.card, Contrast.TEXT)
        check("$p chip text", c.textOn(c.accent), c.tint(c.accent), Contrast.TEXT)
        check("$p onAccentFill", c.onAccentFill, c.accentFill, Contrast.TEXT)
        check("$p onPrimary", scheme.onPrimary, scheme.primary, Contrast.TEXT)

        // Status pills: dot vs card, text vs tinted pill.
        listOf("ok" to c.status.ok, "warn" to c.status.warn, "error" to c.status.error,
            "lan" to c.status.lan, "tailscale" to c.status.tailscale).forEach { (n, color) ->
            check("$p $n dot", color, c.card, Contrast.GRAPHIC)
            check("$p $n pill text", c.textOn(color), c.tint(color), Contrast.TEXT)
            check("$p $n text on card", c.textOn(color, c.card), c.card, Contrast.TEXT)
        }
        check("$p error text", scheme.error, c.card, Contrast.TEXT)
        check("$p outline", scheme.outline, c.card, Contrast.GRAPHIC)
    }

    @Test
    fun allAccents_lightAndDark_meetWcagAA() {
        Defaults.ACCENT_OPTIONS.forEach { argb ->
            val name = String.format(Locale.US, "#%06X", argb and 0xFFFFFF)
            verify(name, dark = false, accent = Color(argb))
            verify(name, dark = true, accent = Color(argb))
        }
        assertTrue("Contrast failures:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun darkMode_usesTrueBlackBackground() {
        val (scheme, c) = buildTheme(dark = true, accent = Color(Defaults.ACCENT))
        assertEquals(Color.Black, scheme.background)
        assertEquals(Color.Black, scheme.surface)
        assertEquals(Color.Black, c.bgBottom)
    }

    @Test
    fun brandBlue_isKeptWhereAlreadyReadable() {
        // Blue Energy passes 3:1 on both card colours, so arcs/dots use the exact brand colour.
        assertEquals(Palette.BlueEnergy, buildTheme(false, Palette.BlueEnergy).second.accent)
        assertEquals(Palette.BlueEnergy, buildTheme(true, Palette.BlueEnergy).second.accent)
    }

    @Test
    fun dynamicScheme_isAppliedAndStaysReadable() {
        // A pastel-ish wallpaper scheme like Android would generate.
        val base = androidx.compose.material3.darkColorScheme(
            primary = Color(0xFFB4C5FF),
            onPrimary = Color(0xFF1B2D60),
            tertiary = Color(0xFFE2BADA),
            surfaceContainer = Color(0xFF1E1F25),
            surfaceContainerHigh = Color(0xFF282A2F),
            surfaceContainerHighest = Color(0xFF33353A),
            onSurface = Color(0xFFE3E2E9),
            onSurfaceVariant = Color(0xFFC5C6D0),
            outlineVariant = Color(0xFF45464F),
        )
        val (scheme, c) = buildTheme(dark = true, accent = Palette.BlueEnergy, dynamic = base)
        assertEquals(Color.Black, scheme.background)
        assertEquals(base.surfaceContainer, c.card)
        assertEquals(base.primary, c.accentFill)
        check("dyn text", c.text, c.card, Contrast.TEXT)
        check("dyn muted", c.textMuted, c.statBlock, Contrast.TEXT)
        check("dyn accentText", c.accentText, c.card, Contrast.TEXT)
        check("dyn onAccentFill", c.onAccentFill, c.accentFill, Contrast.TEXT)
        listOf(c.status.ok, c.status.warn, c.status.error, c.status.tailscale).forEachIndexed { i, s ->
            check("dyn status $i pill", c.textOn(s), c.tint(s), Contrast.TEXT)
        }
        assertTrue("Contrast failures:\n" + failures.joinToString("\n"), failures.isEmpty())
    }
}
