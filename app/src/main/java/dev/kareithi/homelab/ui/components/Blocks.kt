package dev.kareithi.homelab.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.kareithi.homelab.domain.HealthCheck
import dev.kareithi.homelab.domain.Stat
import dev.kareithi.homelab.domain.StatusLevel
import dev.kareithi.homelab.ui.theme.HomelabTheme
import java.util.Locale

val CardShape = RoundedCornerShape(12.dp)

/** Homepage-style card: slate surface, 12dp corners, 1dp border. */
@Composable
fun HomelabCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val c = HomelabTheme.colors
    Surface(
        modifier = modifier,
        shape = CardShape,
        color = c.card,
        border = BorderStroke(1.dp, c.cardBorder),
        content = content,
    )
}

/** Row of 2–4 stat blocks: big number, small label. */
@Composable
fun StatRow(stats: List<Stat>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        stats.forEach { StatBlock(it, Modifier.weight(1f)) }
    }
}

@Composable
fun StatBlock(stat: Stat, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(HomelabTheme.colors.statBlock)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stat.value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(
            stat.label.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Placeholder stat row while a widget loads for the first time. */
@Composable
fun ShimmerStatRow(count: Int = 3, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) {
            Box(
                Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .shimmer(),
            )
        }
    }
}

/** Animated shimmer background. */
fun Modifier.shimmer(): Modifier = composed {
    val base = HomelabTheme.colors.statBlock
    val highlight = if (HomelabTheme.colors.isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.7f)
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -400f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerX",
    )
    background(base).background(
        Brush.linearGradient(
            colors = listOf(Color.Transparent, highlight, Color.Transparent),
            start = Offset(x, 0f),
            end = Offset(x + 400f, 200f),
        ),
    )
}

/** Small coloured pill with a dot, e.g. Docker "running". */
@Composable
fun StatusPill(text: String, level: StatusLevel, modifier: Modifier = Modifier) {
    val color = HomelabTheme.colors.status.of(level)
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Text(text.lowercase(Locale.getDefault()), style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
    }
}

/** Latency chip for services with `ping` / `siteMonitor`. */
@Composable
fun HealthPill(health: HealthCheck, modifier: Modifier = Modifier) {
    val text = when {
        !health.up -> "down"
        health.latencyMs != null -> String.format(Locale.US, "%.0f ms", health.latencyMs)
        else -> "up"
    }
    StatusPill(text, if (health.up) StatusLevel.OK else StatusLevel.ERROR, modifier)
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    HomelabCard(modifier.fillMaxWidth()) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
fun ErrorLine(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = HomelabTheme.colors.status.error,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}
