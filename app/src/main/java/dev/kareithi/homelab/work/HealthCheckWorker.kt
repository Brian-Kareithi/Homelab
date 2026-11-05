package dev.kareithi.homelab.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.kareithi.homelab.HomelabApp
import dev.kareithi.homelab.data.repo.resultOf
import dev.kareithi.homelab.data.settings.NotificationKind
import dev.kareithi.homelab.data.settings.WorkerState
import dev.kareithi.homelab.domain.ServiceUi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import java.util.concurrent.TimeUnit

/**
 * Periodic background check (every 15 min). Compares the current state with what it saw last
 * time and notifies on transitions only; the first run just records a baseline.
 */
class HealthCheckWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as HomelabApp).container
        val store = container.settings
        val repo = container.repository
        val enabled = store.current().notifications
        if (enabled.isEmpty()) return Result.success()

        val prev = store.workerState()
        container.resolver.resolve(force = true)
        val servicesResult = resultOf { repo.services() }
        val up = servicesResult.isSuccess

        if (NotificationKind.SERVER in enabled) {
            if (prev.serverUp == true && !up) {
                notify(NotificationKind.SERVER, "server", "Homepage is unreachable", "Your homelab dashboard stopped responding.")
            } else if (prev.serverUp == false && up) {
                notify(NotificationKind.SERVER, "server", "Homepage is back online", "Your homelab dashboard is reachable again.")
            }
        }

        if (!up) {
            // Keep the other baselines; they'll be compared again once the server is back.
            store.saveWorkerState(prev.copy(serverUp = false))
            return Result.success()
        }

        val services = servicesResult.getOrThrow().flatMap { it.services }
        val next = supervisorScope {
            val docker = async {
                if (NotificationKind.DOCKER in enabled) checkDocker(services, prev) else emptyMap()
            }
            val torrents = async {
                if (NotificationKind.TORRENTS in enabled) checkTorrents(services, prev) else null
            }
            val pending = async {
                if (NotificationKind.REQUESTS in enabled) checkRequests(services, prev) else null
            }
            listOf(docker, torrents, pending).awaitAll()
            WorkerState(
                serverUp = true,
                containerStates = docker.await(),
                incompleteTorrents = torrents.await(),
                seerrPending = pending.await(),
            )
        }
        store.saveWorkerState(next)
        return Result.success()
    }

    /** Notifies when a container that was running is now anything else. Returns new states. */
    private suspend fun checkDocker(services: List<ServiceUi>, prev: WorkerState): Map<String, String> {
        val repo = (applicationContext as HomelabApp).container.repository
        val result = mutableMapOf<String, String>()
        services.filter { it.hasDocker }.groupBy { it.server!! }.forEach { (server, svcs) ->
            val statuses = resultOf { repo.dockerStatuses(server) }.getOrNull() ?: run {
                // Docker API failed: keep previous values instead of reporting everything as stopped.
                svcs.forEach { s -> prev.containerStates[s.key]?.let { result[s.key] = it } }
                return@forEach
            }
            svcs.forEach { svc ->
                val now = statuses[svc.container!!]?.status ?: "not found"
                result[svc.key] = now
                val before = prev.containerStates[svc.key]
                if (before == "running" && now != "running") {
                    notify(
                        NotificationKind.DOCKER,
                        "docker:${svc.key}",
                        "${svc.name} is $now",
                        "Container ${svc.container} on ${svc.server} is no longer running.",
                    )
                }
            }
        }
        return result
    }

    /** Notifies for torrents whose progress went from < 1 to 1. Returns the incomplete set. */
    private suspend fun checkTorrents(services: List<ServiceUi>, prev: WorkerState): Set<String>? {
        val repo = (applicationContext as HomelabApp).container.repository
        val qbt = services.filter { it.widget?.type.equals("qbittorrent", ignoreCase = true) }
        if (qbt.isEmpty()) return emptySet()
        val incomplete = mutableSetOf<String>()
        var anySuccess = false
        qbt.forEach { svc ->
            val data = repo.widgetData(svc.widget!!).getOrNull() ?: return@forEach
            anySuccess = true
            data.torrents.forEach { t ->
                val id = "${svc.key}|${t.hash}"
                if (!t.isComplete) {
                    incomplete += id
                } else if (prev.incompleteTorrents?.contains(id) == true) {
                    notify(NotificationKind.TORRENTS, "torrent:$id", "Download complete", t.name)
                }
            }
        }
        return if (anySuccess) incomplete else prev.incompleteTorrents
    }

    /** Notifies when the Seerr pending count goes up. Returns the current count. */
    private suspend fun checkRequests(services: List<ServiceUi>, prev: WorkerState): Long? {
        val repo = (applicationContext as HomelabApp).container.repository
        val seerr = services.filter {
            it.widget?.type?.lowercase() in setOf("seerr", "overseerr", "jellyseerr")
        }
        if (seerr.isEmpty()) return null
        val counts = seerr.mapNotNull { repo.widgetData(it.widget!!).getOrNull()?.pendingRequests }
        if (counts.isEmpty()) return prev.seerrPending
        val pending = counts.sum()
        val before = prev.seerrPending
        if (before != null && pending > before) {
            val added = pending - before
            notify(
                NotificationKind.REQUESTS,
                "requests",
                if (added == 1L) "New request waiting" else "$added new requests waiting",
                "$pending request${if (pending == 1L) "" else "s"} pending approval in ${seerr.first().name}.",
            )
        }
        return pending
    }

    private fun notify(kind: NotificationKind, tag: String, title: String, text: String) =
        Notifications.post(applicationContext, kind, tag, title, text)

    companion object {
        private const val UNIQUE_NAME = "homelab-health-check"

        /** Schedules the periodic check when any notification is enabled, cancels it otherwise. */
        fun sync(context: Context, anyEnabled: Boolean) {
            val wm = WorkManager.getInstance(context)
            if (!anyEnabled) {
                wm.cancelUniqueWork(UNIQUE_NAME)
                return
            }
            val request = PeriodicWorkRequestBuilder<HealthCheckWorker>(15, TimeUnit.MINUTES)
                // No network constraint: "server unreachable" must still be detected.
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
                .build()
            wm.enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
