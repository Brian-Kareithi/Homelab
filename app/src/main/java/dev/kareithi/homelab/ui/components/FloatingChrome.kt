package dev.kareithi.homelab.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.kareithi.homelab.ui.theme.HomelabTheme

enum class NavAction { MENU, BACK }

private val BarHeight: Dp = 60.dp
private val BarMargin: Dp = 12.dp
val FloatingBarShape = RoundedCornerShape(20.dp)

/** Rounded top bar that floats above the content with a margin and a shadow. */
@Composable
fun FloatingTopBar(
    title: String,
    navAction: NavAction,
    onNavAction: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = HomelabTheme.colors
    Surface(
        modifier = modifier.cardShadow(FloatingBarShape, 16.dp).fillMaxWidth().height(BarHeight),
        shape = FloatingBarShape,
        color = c.card.copy(alpha = 0.96f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, c.cardBorder),
    ) {
        Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(
                onClick = onNavAction,
                shape = RoundedCornerShape(14.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = c.accentSoft,
                    contentColor = c.accent,
                ),
            ) {
                Icon(
                    if (navAction == NavAction.MENU) Icons.Filled.Menu else Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (navAction == NavAction.MENU) "Open menu" else "Back",
                )
            }
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                subtitle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, content = actions)
            Spacer(Modifier.width(8.dp))
        }
    }
}

/**
 * Screen frame with a [FloatingTopBar]. Content draws edge to edge and scrolls underneath the
 * bar; [content] receives the padding needed to start below it and clear the navigation bar.
 * Pass [onRefresh] to get pull-to-refresh with the indicator placed just under the bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomelabScaffold(
    title: String,
    navAction: NavAction,
    onNavAction: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    isRefreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    content: @Composable BoxScope.(PaddingValues) -> Unit,
) {
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val barTop = statusTop + 8.dp
    val contentTop = barTop + BarHeight + BarMargin
    val contentPadding = PaddingValues(top = contentTop, bottom = navBottom + 24.dp)
    val c = HomelabTheme.colors
    val bg = c.bgTop

    Box(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(c.bgTop, c.bgBottom)))) {
        if (onRefresh != null) {
            val state = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                state = state,
                modifier = Modifier.fillMaxSize(),
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = state,
                        isRefreshing = isRefreshing,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = contentTop - 8.dp),
                    )
                },
            ) { content(contentPadding) }
        } else {
            content(contentPadding)
        }

        // Fade content out under the status bar so the floating bar stays readable.
        Box(
            Modifier
                .fillMaxWidth()
                .height(barTop + BarHeight / 2)
                .background(Brush.verticalGradient(listOf(bg, bg.copy(alpha = 0.9f), Color.Transparent))),
        )

        FloatingTopBar(
            title = title,
            subtitle = subtitle,
            navAction = navAction,
            onNavAction = onNavAction,
            actions = actions,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = barTop, start = BarMargin, end = BarMargin),
        )
    }
}
