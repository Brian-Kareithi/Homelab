package dev.kareithi.homelab.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** One entry of `GET /api/services`. Homepage can nest groups inside groups. */
@Serializable
data class ServiceGroupDto(
    val name: String = "",
    val services: List<ServiceDto> = emptyList(),
    val groups: List<ServiceGroupDto> = emptyList(),
)

@Serializable
data class ServiceDto(
    val name: String = "",
    val href: String? = null,
    val description: String? = null,
    val icon: String? = null,
    val weight: Double = 0.0,
    val server: String? = null,
    val container: String? = null,
    /** Present (usually a hostname string) only when `ping:` is configured in services.yaml. */
    val ping: JsonElement? = null,
    /** Present (usually a URL string) only when `siteMonitor:` is configured in services.yaml. */
    val siteMonitor: JsonElement? = null,
    val widgets: List<WidgetDto> = emptyList(),
    /** Older Homepage versions expose a single `widget` instead of `widgets`. */
    val widget: WidgetDto? = null,
)

@Serializable
data class WidgetDto(
    val type: String = "",
    @SerialName("service_name") val serviceName: String? = null,
    @SerialName("service_group") val serviceGroup: String? = null,
    val index: Int = 0,
    val version: Int? = null,
)

/** One entry of `GET /api/widgets` (info widgets such as resources and search). */
@Serializable
data class InfoWidgetDto(
    val type: String = "",
    val options: JsonObject? = null,
)

@Serializable
data class BookmarkGroupDto(
    val name: String = "",
    val bookmarks: List<BookmarkDto> = emptyList(),
)

@Serializable
data class BookmarkDto(
    val name: String = "",
    val href: String = "",
    val description: String? = null,
    val abbr: String? = null,
    val icon: String? = null,
)
