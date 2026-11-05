package dev.kareithi.homelab.data.api

import dev.kareithi.homelab.data.model.BookmarkGroupDto
import dev.kareithi.homelab.data.model.DockerStatsResponseDto
import dev.kareithi.homelab.data.model.DockerStatusesDto
import dev.kareithi.homelab.data.model.InfoWidgetDto
import dev.kareithi.homelab.data.model.ServiceGroupDto
import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Homepage's own JSON API. The base URL given to Retrofit is a placeholder; the real
 * scheme/host/port are injected per request by [dev.kareithi.homelab.data.net.BaseUrlInterceptor].
 *
 * No service credentials ever pass through the app: Homepage's proxy adds them server-side.
 */
interface HomepageApi {

    @GET("api/services")
    suspend fun services(): List<ServiceGroupDto>

    /** Widget data proxy. Retrofit URL-encodes every query value. */
    @GET("api/services/proxy")
    suspend fun proxy(
        @Query("group") group: String,
        @Query("service") service: String,
        @Query("index") index: Int,
        @Query("endpoint") endpoint: String,
    ): JsonElement

    @GET("api/docker/statuses")
    suspend fun dockerStatuses(@Query("server") server: String): DockerStatusesDto

    @GET("api/docker/stats")
    suspend fun dockerStats(
        @Query("container") container: String,
        @Query("server") server: String,
    ): DockerStatsResponseDto

    @GET("api/widgets")
    suspend fun widgets(): List<InfoWidgetDto>

    @GET("api/widgets/resources")
    suspend fun resources(
        @Query("type") type: String,
        @Query("target") target: String? = null,
    ): JsonElement

    @GET("api/bookmarks")
    suspend fun bookmarks(): List<BookmarkGroupDto>

    @GET("api/ping")
    suspend fun ping(
        @Query("groupName") groupName: String,
        @Query("serviceName") serviceName: String,
    ): Response<JsonElement>

    @GET("api/siteMonitor")
    suspend fun siteMonitor(
        @Query("groupName") groupName: String,
        @Query("serviceName") serviceName: String,
    ): Response<JsonElement>
}
