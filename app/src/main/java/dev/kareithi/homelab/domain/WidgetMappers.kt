package dev.kareithi.homelab.domain

import dev.kareithi.homelab.data.model.CpuResponseDto
import dev.kareithi.homelab.data.model.DiskResponseDto
import dev.kareithi.homelab.data.model.JellyfinCountDto
import dev.kareithi.homelab.data.model.JellyfinSessionDto
import dev.kareithi.homelab.data.model.LidarrMissingDto
import dev.kareithi.homelab.data.model.MemoryResponseDto
import dev.kareithi.homelab.data.model.PingDto
import dev.kareithi.homelab.data.model.ProwlarrIndexerDto
import dev.kareithi.homelab.data.model.ProwlarrStatsDto
import dev.kareithi.homelab.data.model.QbtTorrentDto
import dev.kareithi.homelab.data.model.QueueStatusDto
import dev.kareithi.homelab.data.model.RadarrMovieDto
import dev.kareithi.homelab.data.model.SeerrCountDto
import dev.kareithi.homelab.data.model.SonarrQueueDto
import dev.kareithi.homelab.data.model.TotalRecordsDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import java.util.Locale

/** Thrown when a widget's response can't be turned into stats (error payload or bad shape). */
class WidgetException(message: String) : Exception(message)

/**
 * Turns raw Homepage proxy responses into display data, one mapper per widget type.
 * Pure Kotlin (no Android) so it is covered by plain JVM unit tests.
 */
object WidgetMappers {

    val LEECH_STATES = setOf("downloading", "stalledDL", "metaDL", "forcedDL", "queuedDL", "checkingDL")
    val SEED_STATES = setOf("uploading", "stalledUP", "forcedUP", "queuedUP")

    private val json: Json = HomelabJson

    /** Proxy endpoints to call for a widget type, or empty when the type is unsupported. */
    fun endpointsFor(type: String, version: Int?): List<String> = when (type.lowercase(Locale.ROOT)) {
        "jellyfin" -> if (version == 2) listOf("CountV2", "SessionsV2") else listOf("Count", "Sessions")
        "sonarr" -> listOf("wanted/missing", "queue", "series")
        "radarr" -> listOf("movie", "queue/status")
        "lidarr" -> listOf("wanted/missing", "queue/status", "artist")
        "prowlarr" -> listOf("indexer", "indexerstats")
        "seerr", "overseerr", "jellyseerr" -> listOf("request/count")
        "qbittorrent" -> listOf("torrents")
        else -> emptyList()
    }

    fun isSupported(type: String): Boolean = endpointsFor(type, null).isNotEmpty()

    /** Returns the message if the JSON has a top-level `error`, otherwise null. */
    fun errorMessage(element: JsonElement): String? {
        val obj = element as? JsonObject ?: return null
        val err = obj["error"] ?: return null
        return when (err) {
            is JsonNull -> null
            is JsonPrimitive -> err.contentOrNull?.takeIf { it.isNotBlank() } ?: "Error"
            is JsonObject -> {
                val msg = err["message"]
                when (msg) {
                    is JsonPrimitive -> msg.contentOrNull ?: "Error"
                    null, is JsonNull -> err.toString()
                    else -> msg.toString()
                }
            }
            else -> err.toString()
        }
    }

    /**
     * Maps all endpoint responses for one widget. [responses] is keyed by endpoint name as
     * returned by [endpointsFor].
     */
    fun map(type: String, version: Int?, responses: Map<String, JsonElement>): WidgetData {
        responses.values.forEach { el -> errorMessage(el)?.let { throw WidgetException(it) } }
        fun need(endpoint: String): JsonElement =
            responses[endpoint] ?: throw WidgetException("Missing $endpoint response")

        return when (type.lowercase(Locale.ROOT)) {
            "jellyfin" -> if (version == 2) jellyfin(need("CountV2"), need("SessionsV2"))
            else jellyfin(need("Count"), need("Sessions"))
            "sonarr" -> sonarr(need("wanted/missing"), need("queue"), need("series"))
            "radarr" -> radarr(need("movie"), need("queue/status"))
            "lidarr" -> lidarr(need("wanted/missing"), need("queue/status"), need("artist"))
            "prowlarr" -> prowlarr(need("indexer"), need("indexerstats"))
            "seerr", "overseerr", "jellyseerr" -> seerr(need("request/count"))
            "qbittorrent" -> qbittorrent(need("torrents"))
            else -> WidgetData()
        }
    }

    // -----------------------------------------------------------------------------------------
    // Jellyfin
    // -----------------------------------------------------------------------------------------

    fun jellyfin(count: JsonElement, sessions: JsonElement): WidgetData {
        val c = decode<JellyfinCountDto>(count)
        val s = decode<List<JellyfinSessionDto>>(sessions)
        val playing = s.mapNotNull { nowPlaying(it, "Jellyfin") }
        return WidgetData(
            stats = listOf(
                Stat("Movies", Format.compact(c.movieCount)),
                Stat("Series", Format.compact(c.seriesCount)),
                Stat("Episodes", Format.compact(c.episodeCount)),
                Stat("Songs", Format.compact(c.songCount)),
            ),
            sessions = playing,
            sections = listOf(
                DetailSection(
                    title = "Library",
                    headers = listOf("Count"),
                    rows = listOf(
                        DetailRow("Movies", cells = listOf(Format.compact(c.movieCount))),
                        DetailRow("Series", cells = listOf(Format.compact(c.seriesCount))),
                        DetailRow("Episodes", cells = listOf(Format.compact(c.episodeCount))),
                        DetailRow("Songs", cells = listOf(Format.compact(c.songCount))),
                        DetailRow("Albums", cells = listOf(Format.compact(c.albumCount))),
                        DetailRow("Artists", cells = listOf(Format.compact(c.artistCount))),
                    ),
                ),
            ),
        )
    }

    fun nowPlaying(session: JellyfinSessionDto, source: String): NowPlaying? {
        val item = session.nowPlayingItem ?: return null
        val runtime = item.runTimeTicks ?: 0L
        val position = session.playState?.positionTicks ?: 0L
        val progress = if (runtime > 0) (position.toDouble() / runtime).toFloat().coerceIn(0f, 1f) else 0f
        val isEpisode = !item.seriesName.isNullOrBlank()
        val title: String
        val subtitle: String?
        if (isEpisode) {
            title = item.seriesName.orEmpty()
            val code = episodeCode(item.parentIndexNumber, item.indexNumber)
            subtitle = listOfNotNull(code, item.name).joinToString(" · ").ifBlank { null }
        } else {
            title = item.name ?: "Unknown"
            subtitle = null
        }
        return NowPlaying(
            user = session.userName ?: "Unknown user",
            title = title,
            subtitle = subtitle,
            device = session.deviceName ?: session.client,
            progress = progress,
            isPaused = session.playState?.isPaused == true,
            positionSeconds = Format.ticksToSeconds(position),
            runtimeSeconds = Format.ticksToSeconds(runtime),
            source = source,
        )
    }

    fun episodeCode(season: Int?, episode: Int?): String? {
        if (season == null && episode == null) return null
        val s = season?.let { String.format(Locale.US, "S%02d", it) }.orEmpty()
        val e = episode?.let { String.format(Locale.US, "E%02d", it) }.orEmpty()
        return s + e
    }

    // -----------------------------------------------------------------------------------------
    // Sonarr / Radarr / Lidarr / Prowlarr
    // -----------------------------------------------------------------------------------------

    fun sonarr(missing: JsonElement, queue: JsonElement, series: JsonElement): WidgetData {
        val wanted = decode<TotalRecordsDto>(missing).totalRecords
        val q = decode<SonarrQueueDto>(queue)
        val seriesCount = (series as? JsonArray)?.size ?: decode<List<JsonElement>>(series).size
        val items = q.records.map { r ->
            val progress = if (r.size > 0) ((r.size - r.sizeleft) / r.size).toFloat().coerceIn(0f, 1f) else null
            QueueItem(
                title = r.title ?: "Unknown",
                status = r.trackedDownloadState ?: r.status ?: "unknown",
                sizeBytes = r.size,
                progress = progress,
                estimatedCompletion = r.estimatedCompletionTime,
                source = "Sonarr",
            )
        }
        return WidgetData(
            stats = listOf(
                Stat("Wanted", Format.compact(wanted)),
                Stat("Queued", Format.compact(q.totalRecords)),
                Stat("Series", Format.compact(seriesCount.toLong())),
            ),
            queue = items,
        )
    }

    fun radarr(movie: JsonElement, queueStatus: JsonElement): WidgetData {
        val m = decode<RadarrMovieDto>(movie)
        val queued = decode<QueueStatusDto>(queueStatus).totalCount
        val missingMovies = m.all
            .filter { it.monitored && !it.hasFile }
            .sortedBy { it.title?.lowercase(Locale.ROOT) }
            .map {
                DetailRow(
                    title = it.title ?: "Unknown",
                    subtitle = listOfNotNull(
                        it.year?.takeIf { y -> y > 0 }?.toString(),
                        if (it.isAvailable) "Available" else "Not yet released",
                    ).joinToString(" · "),
                )
            }
        return WidgetData(
            stats = listOf(
                Stat("Wanted", Format.compact(m.wanted)),
                Stat("Missing", Format.compact(m.missing)),
                Stat("Queued", Format.compact(queued)),
                Stat("Movies", Format.compact(m.have)),
            ),
            sections = if (missingMovies.isEmpty()) emptyList()
            else listOf(DetailSection("Missing movies (${missingMovies.size})", rows = missingMovies)),
        )
    }

    fun lidarr(missing: JsonElement, queueStatus: JsonElement, artists: JsonElement): WidgetData {
        val w = decode<LidarrMissingDto>(missing)
        val queued = decode<QueueStatusDto>(queueStatus).totalCount
        val artistCount = (artists as? JsonArray)?.size ?: decode<List<JsonElement>>(artists).size
        val albums = w.records.map {
            DetailRow(
                title = it.title ?: "Unknown album",
                subtitle = listOfNotNull(it.artist?.artistName, it.releaseDate?.take(4))
                    .joinToString(" · ").ifBlank { null },
            )
        }
        return WidgetData(
            stats = listOf(
                Stat("Wanted", Format.compact(w.totalRecords)),
                Stat("Queued", Format.compact(queued)),
                Stat("Artists", Format.compact(artistCount.toLong())),
            ),
            sections = if (albums.isEmpty()) emptyList()
            else listOf(DetailSection("Missing albums (${w.totalRecords})", rows = albums)),
        )
    }

    fun prowlarr(indexers: JsonElement, indexerStats: JsonElement): WidgetData {
        val list = decode<List<ProwlarrIndexerDto>>(indexers)
        val stats = decode<ProwlarrStatsDto>(indexerStats).indexers
        val grabs = stats.sumOf { it.numberOfGrabs }
        val queries = stats.sumOf { it.numberOfQueries }
        val fails = stats.sumOf { it.numberOfFailedGrabs + it.numberOfFailedQueries }
        val rows = stats
            .sortedByDescending { it.numberOfGrabs }
            .map {
                DetailRow(
                    title = it.indexerName ?: list.firstOrNull { i -> i.id == it.indexerId }?.name ?: "Indexer",
                    cells = listOf(
                        Format.compact(it.numberOfGrabs),
                        Format.compact(it.numberOfQueries),
                        Format.compact(it.numberOfFailedGrabs + it.numberOfFailedQueries),
                    ),
                )
            }
        return WidgetData(
            stats = listOf(
                Stat("Indexers", list.count { it.enable }.toString()),
                Stat("Grabs", Format.compact(grabs)),
                Stat("Queries", Format.compact(queries)),
                Stat("Fails", Format.compact(fails)),
            ),
            sections = if (rows.isEmpty()) emptyList()
            else listOf(DetailSection("Indexers", headers = listOf("Grabs", "Queries", "Fails"), rows = rows)),
        )
    }

    // -----------------------------------------------------------------------------------------
    // Seerr
    // -----------------------------------------------------------------------------------------

    fun seerr(count: JsonElement): WidgetData {
        val c = decode<SeerrCountDto>(count)
        val available = if (c.available == 0L) c.completed else c.available
        val all = listOf(
            "Total" to c.total, "Movies" to c.movie, "TV" to c.tv, "Pending" to c.pending,
            "Approved" to c.approved, "Declined" to c.declined, "Processing" to c.processing,
            "Available" to c.available, "Completed" to c.completed,
        )
        return WidgetData(
            stats = listOf(
                Stat("Pending", Format.compact(c.pending)),
                Stat("Processing", Format.compact(c.processing)),
                Stat("Approved", Format.compact(c.approved)),
                Stat("Available", Format.compact(available)),
            ),
            sections = listOf(
                DetailSection(
                    title = "Requests",
                    headers = listOf("Count"),
                    rows = all.map { (label, v) -> DetailRow(label, cells = listOf(Format.compact(v))) },
                ),
            ),
            pendingRequests = c.pending,
        )
    }

    // -----------------------------------------------------------------------------------------
    // qBittorrent
    // -----------------------------------------------------------------------------------------

    fun qbittorrent(torrents: JsonElement): WidgetData {
        val list = decode<List<QbtTorrentDto>>(torrents)
        val leech = list.count { it.state in LEECH_STATES }
        val seed = list.count { it.state in SEED_STATES }
        val dl = list.sumOf { it.dlspeed }
        val ul = list.sumOf { it.upspeed }
        return WidgetData(
            stats = listOf(
                Stat("Leech", leech.toString()),
                Stat("Seed", seed.toString()),
                Stat("Download", Format.speed(dl)),
                Stat("Upload", Format.speed(ul)),
            ),
            torrents = list.map {
                TorrentItem(
                    hash = it.hash ?: it.name,
                    name = it.name,
                    state = it.state,
                    progress = it.progress.toFloat().coerceIn(0f, 1f),
                    downloadSpeed = it.dlspeed,
                    uploadSpeed = it.upspeed,
                    sizeBytes = it.size,
                    etaSeconds = it.eta,
                    category = it.category?.takeIf { c -> c.isNotBlank() },
                )
            },
        )
    }

    // -----------------------------------------------------------------------------------------
    // Resources info widget + health checks
    // -----------------------------------------------------------------------------------------

    /** `options.disk` may be a single path or a list of paths. */
    fun diskTargets(options: JsonObject?): List<String> = when (val disk = options?.get("disk")) {
        null, is JsonNull -> emptyList()
        is JsonPrimitive -> listOfNotNull(disk.contentOrNull?.takeIf { it.isNotBlank() && disk.booleanOrNull == null })
        is JsonArray -> disk.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.takeIf { s -> s.isNotBlank() } }
        else -> emptyList()
    }

    /** True for `true` / `"true"`; Homepage options are booleans for cpu/memory. */
    fun optionEnabled(options: JsonObject?, key: String): Boolean {
        val v = options?.get(key) as? JsonPrimitive ?: return false
        return v.booleanOrNull ?: (v.contentOrNull?.equals("true", ignoreCase = true) == true)
    }

    fun cpu(element: JsonElement): CpuInfo? =
        decode<CpuResponseDto>(element).cpu?.let { CpuInfo(it.usage, it.load) }

    /** Used RAM = total − available (`used` includes cache). */
    fun memory(element: JsonElement): MemoryInfo? =
        decode<MemoryResponseDto>(element).memory?.let {
            MemoryInfo(totalBytes = it.total, usedBytes = (it.total - it.available).coerceAtLeast(0.0))
        }

    fun disk(element: JsonElement, target: String): DiskInfo? =
        decode<DiskResponseDto>(element).drive?.let {
            val pct = it.use ?: if (it.size > 0) it.used / it.size * 100.0 else 0.0
            DiskInfo(target = it.mount ?: target, sizeBytes = it.size, usedBytes = it.used, usePercent = pct)
        }

    fun healthCheck(httpOk: Boolean, element: JsonElement?): HealthCheck {
        if (element == null) return HealthCheck(up = false)
        if (errorMessage(element) != null) return HealthCheck(up = false)
        val p = runCatching { decode<PingDto>(element) }.getOrNull() ?: return HealthCheck(up = httpOk)
        val up = when {
            p.alive != null -> p.alive
            p.status != null -> p.status in 200..399
            else -> httpOk
        }
        return HealthCheck(up = up, latencyMs = p.latency)
    }

    private inline fun <reified T> decode(element: JsonElement): T = try {
        json.decodeFromJsonElement<T>(element)
    } catch (e: IllegalArgumentException) {
        // SerializationException extends IllegalArgumentException.
        throw WidgetException("Unexpected response")
    }
}
