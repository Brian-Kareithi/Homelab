package dev.kareithi.homelab.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.kareithi.homelab.ui.theme.HomelabTheme

/** One clock for every skeleton on screen, so the shimmer sweeps across them as a single wave. */
private val LocalShimmerClock = staticCompositionLocalOf<State<Float>?> { null }

@Composable
fun ShimmerHost(content: @Composable () -> Unit) {
    val transition = rememberInfiniteTransition(label = "shimmerClock")
    val progress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerProgress",
    )
    CompositionLocalProvider(LocalShimmerClock provides progress, content = content)
}

/**
 * Skeleton fill with a diagonal highlight. The highlight is positioned in screen coordinates,
 * so neighbouring skeletons light up in sequence instead of pulsing independently.
 * Only the draw phase reads the clock: no recomposition per frame.
 */
fun Modifier.shimmer(): Modifier = composed {
    val c = HomelabTheme.colors
    val clock = LocalShimmerClock.current ?: run {
        val t = rememberInfiniteTransition(label = "shimmerLocal")
        t.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "p")
    }
    val screenWidth = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val base = c.skeletonBase
    val highlight = c.skeletonHighlight

    onGloballyPositioned { origin = it.positionInRoot() }
        .drawBehind {
            drawRect(base)
            val band = 280.dp.toPx()
            val sweepX = -band + clock.value * (screenWidth + band * 2)
            val localX = sweepX - origin.x + origin.y * 0.25f
            drawRect(
                Brush.linearGradient(
                    colors = listOf(Color.Transparent, highlight, Color.Transparent),
                    start = Offset(localX - band / 2, 0f),
                    end = Offset(localX + band / 2, band * 0.35f),
                ),
            )
        }
}

@Composable
fun SkeletonBlock(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(10.dp)) {
    Box(modifier.clip(shape).shimmer())
}

@Composable
fun SkeletonLine(width: Dp, height: Dp = 12.dp, modifier: Modifier = Modifier) {
    SkeletonBlock(modifier.size(width = width, height = height), RoundedCornerShape(50))
}

@Composable
fun PillSkeleton(modifier: Modifier = Modifier, width: Dp = 72.dp) {
    SkeletonBlock(modifier.size(width = width, height = 24.dp), RoundedCornerShape(50))
}

/** Mirrors [dev.kareithi.homelab.ui.dashboard.ServiceCard]: icon tile, two text lines, stat row. */
@Composable
fun ServiceCardSkeleton(modifier: Modifier = Modifier, stats: Int = 3) {
    HomelabCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkeletonBlock(Modifier.size(48.dp), RoundedCornerShape(14.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    SkeletonLine(width = 120.dp, height = 14.dp)
                    Spacer(Modifier.height(8.dp))
                    SkeletonLine(width = 84.dp, height = 10.dp)
                }
            }
            if (stats > 0) {
                Spacer(Modifier.height(12.dp))
                ShimmerStatRow(stats)
            }
        }
    }
}

/** Mirrors the CPU / RAM / Disk gauge strip. */
@Composable
fun ResourcesSkeleton(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(3) {
            HomelabCard(Modifier.weight(1f)) {
                Column(
                    Modifier.padding(vertical = 14.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SkeletonBlock(Modifier.size(64.dp), CircleShape)
                    Spacer(Modifier.height(10.dp))
                    SkeletonLine(width = 40.dp, height = 12.dp)
                    Spacer(Modifier.height(6.dp))
                    SkeletonLine(width = 64.dp, height = 8.dp)
                }
            }
        }
    }
}

@Composable
fun GroupHeaderSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkeletonBlock(Modifier.size(width = 4.dp, height = 14.dp), RoundedCornerShape(2.dp))
        Spacer(Modifier.width(8.dp))
        SkeletonLine(width = 90.dp, height = 12.dp)
        Spacer(Modifier.weight(1f))
        PillSkeleton(width = 32.dp)
    }
}

/** Generic list row: leading tile, title, subtitle and optional progress bar. */
@Composable
fun ListRowSkeleton(modifier: Modifier = Modifier, leading: Boolean = true, progress: Boolean = false) {
    HomelabCard(modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (leading) {
                SkeletonBlock(Modifier.size(40.dp), RoundedCornerShape(12.dp))
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                SkeletonLine(width = 160.dp, height = 13.dp)
                Spacer(Modifier.height(8.dp))
                SkeletonLine(width = 110.dp, height = 10.dp)
                if (progress) {
                    Spacer(Modifier.height(10.dp))
                    SkeletonBlock(Modifier.fillMaxWidth().height(5.dp), RoundedCornerShape(50))
                }
            }
        }
    }
}

/** Placeholder for a settings card (title row plus a few control lines). */
@Composable
fun SettingsCardSkeleton(modifier: Modifier = Modifier, lines: Int = 3) {
    HomelabCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(lines) { i ->
                SkeletonBlock(
                    Modifier.fillMaxWidth(if (i == 0) 1f else 0.85f).height(if (i == 0) 40.dp else 52.dp),
                    RoundedCornerShape(12.dp),
                )
            }
        }
    }
}
