package dev.kareithi.homelab.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

/** WCAG 2.x contrast thresholds. */
object Contrast {
    /** Normal-size text. */
    const val TEXT = 4.5f

    /** Icons, gauge arcs, component boundaries and large text. */
    const val GRAPHIC = 3f
}

/** WCAG contrast ratio between two opaque colours (1..21). */
fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return max(la, lb) / min(la, lb)
}

/**
 * Returns this colour, or the closest shade of it (moved toward white on dark backgrounds,
 * toward black on light ones) that reaches [minRatio] against [background].
 * Keeps the hue recognisable while guaranteeing legibility.
 */
fun Color.readableOn(background: Color, minRatio: Float = Contrast.TEXT): Color {
    val bg = background.compositeOver(Color.Black).copy(alpha = 1f)
    val fg = this.compositeOver(bg).copy(alpha = 1f)
    if (contrastRatio(fg, bg) >= minRatio) return fg
    val target = if (bg.luminance() < 0.4f) Color.White else Color.Black
    var t = 0f
    while (t < 1f) {
        t += 0.04f
        val candidate = lerp(fg, target, t.coerceAtMost(1f))
        if (contrastRatio(candidate, bg) >= minRatio) return candidate
    }
    return target
}

/** White or black, whichever reads better on [fill]. */
fun bestContentOn(fill: Color): Color =
    if (contrastRatio(Color.White, fill) >= contrastRatio(Color.Black, fill)) Color.White else Color.Black
