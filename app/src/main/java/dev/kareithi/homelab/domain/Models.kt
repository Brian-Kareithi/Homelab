package dev.kareithi.homelab.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The single JSON configuration used for every Homepage response and the offline cache. */
val HomelabJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    explicitNulls = false
    encodeDefaults = true
}

@Serializable
data class ServiceGroupUi(
    val name: String,
    val services: List<ServiceUi>,
)

@Serializable
data class ServiceUi(
    val group: String,
    val name: String,
    val href: String? = null,
    val description: String? = null,
    val icon: String? = null,
    val server: String? = null,
    val container: String? = null,
    val widget: WidgetRef? = null,
    val hasPing: Boolean = false,
    val hasSiteMonitor: Boolean = false,
) {
    val key: String get() = serviceKey(group, name)
    val hasDocker: Boolean get() = !server.isNullOrBlank() && !container.isNullOrBlank()
    val hasHealthCheck: Boolean get() = hasPing || hasSiteMonitor
}

fun serviceKey(group: String, name: String): String = "$group/$name"

/** Identifies one service widget for the proxy endpoint. */
@Serializable
data class WidgetRef(
    val type: String,
    val group: String,
    val service: String,
    val index: Int = 0,
    val version: Int? = null,
)

/** A "stat block": big number with a small label underneath, as on Homepage. */
@Serializable
data class Stat(val label: String, val value: String)

@Serializable
data class NowPlaying(
    val user: String,
    val title: String,
    val subtitle: String? = null,
    val device: String? = null,
    val progress: Float = 0f,
    val isPaused: Boolean = false,
    val positionSeconds: Long = 0,
    val runtimeSeconds: Long = 0,
    val source: String = "",
)

@Serializable
data class QueueItem(
    val title: String,
    val status: String,
    val sizeBytes: Double = 0.0,
    val progress: Float? = null,
    /** ISO-8601 instant, formatted relative to "now" at render time. */
    val estimatedCompletion: String? = null,
    val source: String = "",
)

@Serializable
data class TorrentItem(
    val hash: String,
    val name: String,
    val state: String,
    val progress: Float,
    val downloadSpeed: Long,
    val uploadSpeed: Long,
    val sizeBytes: Long,
    val etaSeconds: Long,
    val category: String? = null,
) {
    val isComplete: Boolean get() = progress >= 1f

    /** Something is actually happening with it right now. */
    val isActive: Boolean
        get() = downloadSpeed > 0 || uploadSpeed > 0 || (!isComplete && state in WidgetMappers.LEECH_STATES)
}

/** A titled list (or table when [headers] is non-empty) shown on the Service Detail screen. */
@Serializable
data class DetailSection(
    val title: String,
    val headers: List<String> = emptyList(),
    val rows: List<DetailRow> = emptyList(),
)

@Serializable
data class DetailRow(
    val title: String,
    val subtitle: String? = null,
    val cells: List<String> = emptyList(),
    val progress: Float? = null,
)

/** Everything a widget produced, already mapped for display. */
@Serializable
data class WidgetData(
    val stats: List<Stat> = emptyList(),
    val sessions: List<NowPlaying> = emptyList(),
    val queue: List<QueueItem> = emptyList(),
    /** Total queue size on the server; Homepage's proxy only returns the first page of records. */
    val queueTotal: Long? = null,
    val torrents: List<TorrentItem> = emptyList(),
    val sections: List<DetailSection> = emptyList(),
    /** Seerr pending-request count, used by notifications. */
    val pendingRequests: Long? = null,
)

/** Per-card widget state. Data is kept while refreshing so the card doesn't flicker. */
data class WidgetState(
    val data: WidgetData? = null,
    val loading: Boolean = false,
    val error: String? = null,
)

enum class StatusLevel { OK, WARN, ERROR }

@Serializable
data class DockerState(
    val status: String,
    val health: String? = null,
) {
    val level: StatusLevel get() = dockerLevel(status, health)
    val label: String
        get() = when {
            status.equals("running", true) && !health.isNullOrBlank() -> health
            else -> status
        }
}

fun dockerLevel(status: String, health: String?): StatusLevel {
    val s = status.lowercase()
    val h = health?.lowercase()
    return when {
        s == "running" && (h == null || h == "" || h == "healthy" || h == "none") -> StatusLevel.OK
        s == "running" -> StatusLevel.WARN // starting / unhealthy
        s == "starting" || s == "restarting" || s == "created" || s == "paused" -> StatusLevel.WARN
        s.startsWith("partial") -> StatusLevel.WARN
        else -> StatusLevel.ERROR // exited, dead, not found, unknown
    }
}

@Serializable
data class DockerStats(
    val cpuPercent: Double? = null,
    val memoryBytes: Double? = null,
    val rxBytes: Double? = null,
    val txBytes: Double? = null,
)

@Serializable
data class HealthCheck(
    val up: Boolean,
    val latencyMs: Double? = null,
)

@Serializable
data class CpuInfo(val usagePercent: Double, val load: Double)

@Serializable
data class MemoryInfo(val totalBytes: Double, val usedBytes: Double) {
    val percent: Double get() = if (totalBytes > 0) usedBytes / totalBytes * 100.0 else 0.0
}

@Serializable
data class DiskInfo(
    val target: String,
    val sizeBytes: Double,
    val usedBytes: Double,
    val usePercent: Double,
)

@Serializable
data class Resources(
    val cpu: CpuInfo? = null,
    val memory: MemoryInfo? = null,
    val disks: List<DiskInfo> = emptyList(),
) {
    val isEmpty: Boolean get() = cpu == null && memory == null && disks.isEmpty()
}

@Serializable
data class BookmarkGroupUi(
    val name: String,
    val bookmarks: List<BookmarkUi>,
)

@Serializable
data class BookmarkUi(
    val name: String,
    val href: String,
    val description: String? = null,
    val abbr: String? = null,
    val icon: String? = null,
)

enum class ConnectionKind { LAN, TAILSCALE, CUSTOM, OFFLINE }
