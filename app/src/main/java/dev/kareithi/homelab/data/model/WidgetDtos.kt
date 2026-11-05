package dev.kareithi.homelab.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------------------------
// Jellyfin (Count / CountV2, Sessions / SessionsV2)
// ---------------------------------------------------------------------------------------------

@Serializable
data class JellyfinCountDto(
    @SerialName("MovieCount") val movieCount: Long = 0,
    @SerialName("SeriesCount") val seriesCount: Long = 0,
    @SerialName("EpisodeCount") val episodeCount: Long = 0,
    @SerialName("SongCount") val songCount: Long = 0,
    @SerialName("AlbumCount") val albumCount: Long = 0,
    @SerialName("ArtistCount") val artistCount: Long = 0,
)

@Serializable
data class JellyfinSessionDto(
    @SerialName("Id") val id: String? = null,
    @SerialName("UserName") val userName: String? = null,
    @SerialName("DeviceName") val deviceName: String? = null,
    @SerialName("Client") val client: String? = null,
    @SerialName("NowPlayingItem") val nowPlayingItem: JellyfinItemDto? = null,
    @SerialName("PlayState") val playState: JellyfinPlayStateDto? = null,
)

@Serializable
data class JellyfinItemDto(
    @SerialName("Name") val name: String? = null,
    @SerialName("SeriesName") val seriesName: String? = null,
    @SerialName("Type") val type: String? = null,
    @SerialName("ParentIndexNumber") val parentIndexNumber: Int? = null,
    @SerialName("IndexNumber") val indexNumber: Int? = null,
    @SerialName("RunTimeTicks") val runTimeTicks: Long? = null,
)

@Serializable
data class JellyfinPlayStateDto(
    @SerialName("PositionTicks") val positionTicks: Long? = null,
    @SerialName("IsPaused") val isPaused: Boolean = false,
)

// ---------------------------------------------------------------------------------------------
// *arr
// ---------------------------------------------------------------------------------------------

@Serializable
data class TotalRecordsDto(
    val totalRecords: Long = 0,
)

@Serializable
data class QueueStatusDto(
    val totalCount: Long = 0,
)

@Serializable
data class SonarrQueueDto(
    val totalRecords: Long = 0,
    val records: List<SonarrQueueRecordDto> = emptyList(),
)

@Serializable
data class SonarrQueueRecordDto(
    val title: String? = null,
    val status: String? = null,
    val trackedDownloadState: String? = null,
    val size: Double = 0.0,
    val sizeleft: Double = 0.0,
    val estimatedCompletionTime: String? = null,
)

@Serializable
data class RadarrMovieDto(
    val wanted: Long = 0,
    val missing: Long = 0,
    val have: Long = 0,
    val all: List<RadarrMovieItemDto> = emptyList(),
)

@Serializable
data class RadarrMovieItemDto(
    val title: String? = null,
    val year: Int? = null,
    val hasFile: Boolean = false,
    val monitored: Boolean = false,
    val isAvailable: Boolean = true,
)

@Serializable
data class LidarrMissingDto(
    val totalRecords: Long = 0,
    val records: List<LidarrAlbumDto> = emptyList(),
)

@Serializable
data class LidarrAlbumDto(
    val title: String? = null,
    val releaseDate: String? = null,
    val artist: LidarrArtistDto? = null,
)

@Serializable
data class LidarrArtistDto(
    val artistName: String? = null,
)

@Serializable
data class ProwlarrIndexerDto(
    val id: Int? = null,
    val name: String? = null,
    val enable: Boolean = false,
)

@Serializable
data class ProwlarrStatsDto(
    val indexers: List<ProwlarrIndexerStatDto> = emptyList(),
)

@Serializable
data class ProwlarrIndexerStatDto(
    val indexerId: Int? = null,
    val indexerName: String? = null,
    val numberOfGrabs: Long = 0,
    val numberOfQueries: Long = 0,
    val numberOfFailedGrabs: Long = 0,
    val numberOfFailedQueries: Long = 0,
)

// ---------------------------------------------------------------------------------------------
// Seerr / Overseerr / Jellyseerr
// ---------------------------------------------------------------------------------------------

@Serializable
data class SeerrCountDto(
    val total: Long = 0,
    val movie: Long = 0,
    val tv: Long = 0,
    val pending: Long = 0,
    val approved: Long = 0,
    val declined: Long = 0,
    val processing: Long = 0,
    val available: Long = 0,
    val completed: Long = 0,
)

// ---------------------------------------------------------------------------------------------
// qBittorrent
// ---------------------------------------------------------------------------------------------

@Serializable
data class QbtTorrentDto(
    val hash: String? = null,
    val name: String = "",
    val state: String = "",
    val dlspeed: Long = 0,
    val upspeed: Long = 0,
    val progress: Double = 0.0,
    val size: Long = 0,
    val eta: Long = 0,
    val category: String? = null,
)

// ---------------------------------------------------------------------------------------------
// Docker
// ---------------------------------------------------------------------------------------------

@Serializable
data class DockerStatusesDto(
    val statuses: Map<String, DockerStatusDto> = emptyMap(),
)

@Serializable
data class DockerStatusDto(
    val status: String? = null,
    val health: String? = null,
)

@Serializable
data class DockerStatsResponseDto(
    val stats: Map<String, DockerStatDto> = emptyMap(),
)

@Serializable
data class DockerStatDto(
    val cpu: Double? = null,
    val mem: Double? = null,
    val rx: Double? = null,
    val tx: Double? = null,
)

// ---------------------------------------------------------------------------------------------
// Resources info widget
// ---------------------------------------------------------------------------------------------

@Serializable
data class CpuResponseDto(val cpu: CpuDto? = null)

@Serializable
data class CpuDto(
    val usage: Double = 0.0,
    val load: Double = 0.0,
)

@Serializable
data class MemoryResponseDto(val memory: MemoryDto? = null)

@Serializable
data class MemoryDto(
    val total: Double = 0.0,
    val available: Double = 0.0,
    val used: Double = 0.0,
    val free: Double = 0.0,
)

@Serializable
data class DiskResponseDto(val drive: DriveDto? = null)

@Serializable
data class DriveDto(
    val size: Double = 0.0,
    val used: Double = 0.0,
    val available: Double = 0.0,
    val use: Double? = null,
    val mount: String? = null,
)

// ---------------------------------------------------------------------------------------------
// Ping / siteMonitor
// ---------------------------------------------------------------------------------------------

@Serializable
data class PingDto(
    val alive: Boolean? = null,
    val status: Int? = null,
    val latency: Double? = null,
)
