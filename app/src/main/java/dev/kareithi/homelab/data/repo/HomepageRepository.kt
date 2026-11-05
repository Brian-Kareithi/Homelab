package dev.kareithi.homelab.data.repo

import dev.kareithi.homelab.data.api.HomepageApi
import dev.kareithi.homelab.data.model.ServiceDto
import dev.kareithi.homelab.data.model.ServiceGroupDto
import dev.kareithi.homelab.domain.BookmarkGroupUi
import dev.kareithi.homelab.domain.BookmarkUi
import dev.kareithi.homelab.domain.DockerState
import dev.kareithi.homelab.domain.DockerStats
import dev.kareithi.homelab.domain.HealthCheck
import dev.kareithi.homelab.domain.HomelabJson
import dev.kareithi.homelab.domain.Resources
import dev.kareithi.homelab.domain.ServiceGroupUi
import dev.kareithi.homelab.domain.ServiceUi
import dev.kareithi.homelab.domain.WidgetData
import dev.kareithi.homelab.domain.WidgetException
import dev.kareithi.homelab.domain.WidgetMappers
import dev.kareithi.homelab.domain.WidgetRef
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import retrofit2.HttpException
import java.io.IOException

/** Runs [block], turning failures into a [Result] but never swallowing coroutine cancellation. */
suspend inline fun <T> resultOf(crossinline block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}

/** Short human-readable message for an error line on a card. */
fun Throwable.toUserMessage(): String = when (this) {
    is WidgetException -> message ?: "Error"
    is HttpException -> "HTTP ${code()}"
    is SerializationException -> "Unexpected response"
    is IOException -> message?.takeIf { it.isNotBlank() }?.let { "Unreachable: $it" } ?: "Unreachable"
    else -> message ?: javaClass.simpleName
}

/**
 * Everything the app knows comes through here, from one Homepage instance. Each call is
 * independent so the caller can fetch in parallel and one failure never affects the others.
 */
class HomepageRepository(private val api: HomepageApi) {

    // --- Services ---------------------------------------------------------------------------

    suspend fun services(): List<ServiceGroupUi> = api.services().flatMap(::flatten)

    private fun flatten(group: ServiceGroupDto): List<ServiceGroupUi> {
        val own = ServiceGroupUi(
            name = group.name,
            services = group.services
                .withIndex()
                .sortedWith(compareBy({ it.value.weight }, { it.index }))
                .map { toUi(group.name, it.value) },
        )
        val nested = group.groups.flatMap(::flatten)
        return (if (own.services.isNotEmpty() || nested.isEmpty()) listOf(own) else emptyList()) + nested
    }

    private fun toUi(groupName: String, s: ServiceDto): ServiceUi {
        val w = s.widgets.firstOrNull() ?: s.widget
        return ServiceUi(
            group = groupName,
            name = s.name,
            href = s.href?.takeIf { it.isNotBlank() },
            description = s.description?.takeIf { it.isNotBlank() },
            icon = s.icon,
            server = s.server?.takeIf { it.isNotBlank() },
            container = s.container?.takeIf { it.isNotBlank() },
            widget = w?.takeIf { it.type.isNotBlank() }?.let {
                WidgetRef(
                    type = it.type,
                    group = it.serviceGroup ?: groupName,
                    service = it.serviceName ?: s.name,
                    index = it.index,
                    version = it.version,
                )
            },
            hasPing = isConfigured(s.ping),
            hasSiteMonitor = isConfigured(s.siteMonitor),
        )
    }

    private fun isConfigured(e: JsonElement?): Boolean = when (e) {
        null, is JsonNull -> false
        is JsonPrimitive -> e.booleanOrNull ?: !e.contentOrNull.isNullOrBlank()
        else -> true
    }

    // --- Widgets ----------------------------------------------------------------------------

    /** Fetches every endpoint the widget needs in parallel, then maps them to display data. */
    suspend fun widgetData(ref: WidgetRef): Result<WidgetData> = resultOf {
        val endpoints = WidgetMappers.endpointsFor(ref.type, ref.version)
        if (endpoints.isEmpty()) return@resultOf WidgetData()
        val responses = coroutineScope {
            endpoints.map { ep -> async { ep to proxy(ref, ep) } }.awaitAll().toMap()
        }
        WidgetMappers.map(ref.type, ref.version, responses)
    }

    private suspend fun proxy(ref: WidgetRef, endpoint: String): JsonElement {
        val element = try {
            api.proxy(ref.group, ref.service, ref.index, endpoint)
        } catch (e: HttpException) {
            val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
            val parsed = body?.let { runCatching { HomelabJson.parseToJsonElement(it) }.getOrNull() }
            throw WidgetException(parsed?.let(WidgetMappers::errorMessage) ?: "HTTP ${e.code()}")
        }
        WidgetMappers.errorMessage(element)?.let { throw WidgetException(it) }
        return element
    }

    // --- Docker -----------------------------------------------------------------------------

    /** Status per container name on a Docker server. */
    suspend fun dockerStatuses(server: String): Map<String, DockerState> =
        api.dockerStatuses(server).statuses.mapValues { (_, v) ->
            DockerState(status = v.status ?: "unknown", health = v.health)
        }

    suspend fun dockerStats(server: String, container: String): DockerStats? {
        val stats = api.dockerStats(container, server).stats
        val s = stats[container] ?: stats.values.singleOrNull() ?: return null
        return DockerStats(cpuPercent = s.cpu, memoryBytes = s.mem, rxBytes = s.rx, txBytes = s.tx)
    }

    // --- Resources info widget --------------------------------------------------------------

    /** Null when Homepage has no resources widget configured. */
    suspend fun resources(): Resources? = supervisorScope {
        val configs = api.widgets().filter { it.type.equals("resources", ignoreCase = true) }
        if (configs.isEmpty()) return@supervisorScope null
        val wantCpu = configs.any { WidgetMappers.optionEnabled(it.options, "cpu") }
        val wantMem = configs.any { WidgetMappers.optionEnabled(it.options, "memory") }
        val targets = configs.flatMap { WidgetMappers.diskTargets(it.options) }.distinct()

        val cpu = async {
            if (wantCpu) resultOf { WidgetMappers.cpu(api.resources("cpu")) }.getOrNull() else null
        }
        val mem = async {
            if (wantMem) resultOf { WidgetMappers.memory(api.resources("memory")) }.getOrNull() else null
        }
        val disks = targets.map { t ->
            async { resultOf { WidgetMappers.disk(api.resources("disk", t), t) }.getOrNull() }
        }
        Resources(cpu = cpu.await(), memory = mem.await(), disks = disks.awaitAll().filterNotNull())
    }

    // --- Bookmarks --------------------------------------------------------------------------

    suspend fun bookmarks(): List<BookmarkGroupUi> = api.bookmarks().map { g ->
        BookmarkGroupUi(
            name = g.name,
            bookmarks = g.bookmarks.filter { it.href.isNotBlank() }.map {
                BookmarkUi(
                    name = it.name,
                    href = it.href,
                    description = it.description?.takeIf { d -> d.isNotBlank() },
                    abbr = it.abbr,
                    icon = it.icon,
                )
            },
        )
    }

    // --- Health checks ----------------------------------------------------------------------

    /** Only meaningful when the service has `ping` or `siteMonitor` set; otherwise null. */
    suspend fun healthCheck(service: ServiceUi): HealthCheck? {
        val response = when {
            service.hasSiteMonitor -> api.siteMonitor(service.group, service.name)
            service.hasPing -> api.ping(service.group, service.name)
            else -> return null
        }
        val body = response.body() ?: runCatching {
            response.errorBody()?.string()?.let { HomelabJson.parseToJsonElement(it) }
        }.getOrNull()
        return WidgetMappers.healthCheck(response.isSuccessful, body)
    }
}
