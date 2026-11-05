package dev.kareithi.homelab.data.repo

import dev.kareithi.homelab.domain.BookmarkGroupUi
import dev.kareithi.homelab.domain.DockerState
import dev.kareithi.homelab.domain.DockerStats
import dev.kareithi.homelab.domain.HealthCheck
import dev.kareithi.homelab.domain.HomelabJson
import dev.kareithi.homelab.domain.Resources
import dev.kareithi.homelab.domain.ServiceGroupUi
import dev.kareithi.homelab.domain.WidgetData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File

/** The last successful snapshot, so the app shows something useful while offline. */
@Serializable
data class CachedDashboard(
    val savedAt: Long,
    val groups: List<ServiceGroupUi> = emptyList(),
    val widgets: Map<String, WidgetData> = emptyMap(),
    val docker: Map<String, DockerState> = emptyMap(),
    val dockerStats: Map<String, DockerStats> = emptyMap(),
    val health: Map<String, HealthCheck> = emptyMap(),
    val resources: Resources? = null,
    val bookmarks: List<BookmarkGroupUi> = emptyList(),
)

/** In-memory copy plus a JSON file in `cacheDir`. */
class DashboardCache(private val file: File) {

    @Volatile
    private var memory: CachedDashboard? = null

    suspend fun load(): CachedDashboard? {
        memory?.let { return it }
        return withContext(Dispatchers.IO) {
            if (!file.exists()) return@withContext null
            runCatching { HomelabJson.decodeFromString(CachedDashboard.serializer(), file.readText()) }
                .getOrNull()
                ?.also { memory = it }
        }
    }

    suspend fun save(snapshot: CachedDashboard) {
        memory = snapshot
        withContext(Dispatchers.IO) {
            runCatching {
                val tmp = File(file.parentFile, file.name + ".tmp")
                tmp.writeText(HomelabJson.encodeToString(CachedDashboard.serializer(), snapshot))
                if (!tmp.renameTo(file)) {
                    file.delete()
                    tmp.renameTo(file)
                }
            }
        }
    }
}
