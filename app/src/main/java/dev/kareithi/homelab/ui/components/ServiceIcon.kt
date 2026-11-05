package dev.kareithi.homelab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import dev.kareithi.homelab.domain.IconResolver
import dev.kareithi.homelab.domain.IconSource
import dev.kareithi.homelab.ui.theme.HomelabTheme

/** Material equivalents for common `mdi-` names; everything else loads the MDI SVG. */
private val MDI_TO_MATERIAL: Map<String, ImageVector> = mapOf(
    "wifi" to Icons.Filled.Wifi,
    "server" to Icons.Filled.Dns,
    "server-network" to Icons.Filled.Dns,
    "dns" to Icons.Filled.Dns,
    "home" to Icons.Filled.Home,
    "download" to Icons.Filled.Download,
    "movie" to Icons.Filled.Movie,
    "filmstrip" to Icons.Filled.Movie,
    "television" to Icons.Filled.Tv,
    "television-classic" to Icons.Filled.Tv,
    "music" to Icons.Filled.MusicNote,
    "music-note" to Icons.Filled.MusicNote,
    "cloud" to Icons.Filled.Cloud,
    "database" to Icons.Filled.Storage,
    "harddisk" to Icons.Filled.Storage,
    "nas" to Icons.Filled.Storage,
    "router" to Icons.Filled.Router,
    "router-wireless" to Icons.Filled.Router,
    "shield" to Icons.Filled.Shield,
    "shield-check" to Icons.Filled.Security,
    "security" to Icons.Filled.Security,
    "cog" to Icons.Filled.Settings,
    "monitor" to Icons.Filled.Monitor,
    "monitor-dashboard" to Icons.Filled.Dashboard,
    "view-dashboard" to Icons.Filled.Dashboard,
    "chart-line" to Icons.AutoMirrored.Filled.ShowChart,
    "web" to Icons.Filled.Language,
    "earth" to Icons.Filled.Public,
    "bookmark" to Icons.Filled.Bookmark,
    "book" to Icons.Filled.Book,
    "calendar" to Icons.Filled.CalendarMonth,
    "lock" to Icons.Filled.Lock,
    "email" to Icons.Filled.Email,
    "folder" to Icons.Filled.Folder,
    "camera" to Icons.Filled.CameraAlt,
    "cctv" to Icons.Filled.Videocam,
    "image" to Icons.Filled.Photo,
    "printer" to Icons.Filled.Print,
    "lightbulb" to Icons.Filled.Lightbulb,
    "thermometer" to Icons.Filled.Thermostat,
    "speedometer" to Icons.Filled.Speed,
    "magnify" to Icons.Filled.Search,
    "bell" to Icons.Filled.Notifications,
    "memory" to Icons.Filled.Memory,
    "chip" to Icons.Filled.Memory,
    "console" to Icons.Filled.Terminal,
    "code-braces" to Icons.Filled.Code,
    "backup-restore" to Icons.Filled.Backup,
    "sync" to Icons.Filled.Sync,
)

private fun parseHex(hex: String?): Color? {
    if (hex == null) return null
    val h = hex.removePrefix("#")
    val full = when (h.length) {
        3 -> h.map { "$it$it" }.joinToString("")
        6, 8 -> h
        else -> return null
    }
    val v = full.toLongOrNull(16) ?: return null
    return if (full.length == 6) Color(0xFF000000 or v) else Color(v)
}

/**
 * Service / bookmark icon following Homepage's resolution rules, with a rounded
 * first-letter tile when nothing loads.
 */
@Composable
fun ServiceIcon(
    icon: String?,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    baseUrl: String? = null,
) {
    val source = remember(icon, baseUrl) { IconResolver.resolve(icon, baseUrl) }
    val accent = HomelabTheme.colors.accent
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        when (source) {
            is IconSource.Mdi -> {
                val tint = parseHex(source.colorHex) ?: accent
                val vector = MDI_TO_MATERIAL[source.name]
                if (vector != null) {
                    Icon(vector, contentDescription = name, tint = tint, modifier = Modifier.fillMaxSize().padding(size * 0.08f))
                } else {
                    RemoteIcon(source.url, name, size, tint)
                }
            }
            is IconSource.Remote -> RemoteIcon(
                url = source.url,
                name = name,
                size = size,
                tint = if (source.tint) parseHex(source.colorHex) ?: accent else null,
            )
            IconSource.None -> LetterTile(name, size)
        }
    }
}

@Composable
private fun RemoteIcon(url: String, name: String, size: Dp, tint: Color?) {
    val context = LocalContext.current
    val request = remember(url) { ImageRequest.Builder(context).data(url).build() }
    SubcomposeAsyncImage(
        model = request,
        contentDescription = name,
        contentScale = ContentScale.Fit,
        colorFilter = tint?.let { ColorFilter.tint(it) },
        modifier = Modifier.size(size),
        loading = { SkeletonBlock(Modifier.size(size), RoundedCornerShape(size * 0.25f)) },
        error = { LetterTile(name, size) },
    )
}

@Composable
fun LetterTile(name: String, size: Dp, modifier: Modifier = Modifier) {
    val c = HomelabTheme.colors
    val accent = c.accent
    val tileBg = c.tint(accent)
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.25f))
            .background(tileBg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = IconResolver.initial(name),
            color = c.textOn(accent, tileBg),
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.45f).sp,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
