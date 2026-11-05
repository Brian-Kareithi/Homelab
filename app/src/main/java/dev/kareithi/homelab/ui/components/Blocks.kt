package dev.kareithi.homelab.ui.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.kareithi.homelab.domain.HealthCheck
import dev.kareithi.homelab.domain.Stat
import dev.kareithi.homelab.domain.StatusLevel
import dev.kareithi.homelab.ui.theme.HomelabTheme
import java.util.Locale

val CardShape = RoundedCornerShape(18.dp)
private val BlockShape = RoundedCornerShape(12.dp)

/**
 * Soft, accent-tinted shadow in light mode (cards appear to float over the gradient);
 * flat with a border in dark mode where shadows don't read.
 */
@Composable
fun Modifier.cardShadow(shape: Shape = CardShape, elevation: Dp = 10.dp): Modifier {
    val c = HomelabTheme.colors
    return if (c.isDark) this else this.shadow(elevation, shape, clip = false, ambientColor = c.shadow, spotColor = c.shadow)
}

/** The app's card: Paper surface, 18dp corners, 1dp border, tinted shadow. */
@Composable
fun HomelabCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val c = HomelabTheme.colors
    Surface(
        // Shadow goes first so callers' clip()/clickable() don't cut it off.
        modifier = Modifier.cardShadow().then(modifier),
        shape = CardShape,
        color = c.card,
        border = BorderStroke(1.dp, c.cardBorder),
        content = content,
    )
}

/** Row of 2–4 stat blocks: big number, small label. */
@Composable
fun StatRow(stats: List<Stat>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        stats.forEach { StatBlock(it, Modifier.weight(1f)) }
    }
}

@Composable
fun StatBlock(stat: Stat, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(BlockShape)
            .background(HomelabTheme.colors.statBlock)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stat.value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(1.dp))
        Text(
            stat.label.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Skeleton for a stat row while a widget loads for the first time. */
@Composable
fun ShimmerStatRow(count: Int = 3, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) {
            Column(
                Modifier
                    .weight(1f)
                    .clip(BlockShape)
                    .background(HomelabTheme.colors.statBlock)
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SkeletonLine(width = 34.dp, height = 14.dp)
                Spacer(Modifier.height(6.dp))
                SkeletonLine(width = 48.dp, height = 8.dp)
            }
        }
    }
}

/** Small coloured pill with a dot, e.g. Docker "running". */
@Composable
fun StatusPill(text: String, level: StatusLevel, modifier: Modifier = Modifier) {
    val c = HomelabTheme.colors
    val color = c.status.of(level)
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(c.tint(color))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text.lowercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = c.textOn(color),
            maxLines = 1,
        )
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

/** Section label: small accent bar, uppercase tracking, optional trailing value. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccentBar()
        Spacer(Modifier.width(8.dp))
        Text(
            text.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelLarge,
            letterSpacing = 1.2.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) CountChip(trailing)
    }
}

@Composable
fun AccentBar(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(width = 4.dp, height = 14.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(HomelabTheme.colors.accent),
    )
}

@Composable
fun CountChip(text: String, modifier: Modifier = Modifier) {
    val c = HomelabTheme.colors
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = c.textOn(c.accent),
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(c.tint(c.accent))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    HomelabCard(modifier.fillMaxWidth()) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
        )
    }
}

@Composable
fun ErrorLine(text: String, modifier: Modifier = Modifier) {
    val c = HomelabTheme.colors
    val red = c.textOn(c.status.error, c.card)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = red, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = red,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
