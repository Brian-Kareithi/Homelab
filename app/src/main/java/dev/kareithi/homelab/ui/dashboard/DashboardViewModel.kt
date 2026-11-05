package dev.kareithi.homelab.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.kareithi.homelab.data.repo.CachedDashboard
import dev.kareithi.homelab.data.repo.resultOf
import dev.kareithi.homelab.data.repo.toUserMessage
import dev.kareithi.homelab.data.settings.AppSettings
import dev.kareithi.homelab.di.AppContainer
import dev.kareithi.homelab.domain.BookmarkGroupUi
import dev.kareithi.homelab.domain.ConnectionKind
import dev.kareithi.homelab.domain.DockerState
import dev.kareithi.homelab.domain.DockerStats
import dev.kareithi.homelab.domain.HealthCheck
import dev.kareithi.homelab.domain.NowPlaying
import dev.kareithi.homelab.domain.QueueItem
import dev.kareithi.homelab.domain.Resources
import dev.kareithi.homelab.domain.ServiceGroupUi
import dev.kareithi.homelab.domain.ServiceUi
import dev.kareithi.homelab.domain.TorrentItem
import dev.kareithi.homelab.domain.WidgetMappers
import dev.kareithi.homelab.domain.WidgetState
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

data class DashboardUiState(
    val groups: List<ServiceGroupUi> = emptyList(),
    val widgets: Map<String, WidgetState> = emptyMap(),
    val docker: Map<String, DockerState> = emptyMap(),
    val dockerStats: Map<String, DockerStats> = emptyMap(),
    val health: Map<String, HealthCheck> = emptyMap(),
    val resources: Resources? = null,
    val bookmarks: List<BookmarkGroupUi> = emptyList(),
    val connection: ConnectionKind? = null,
    val baseUrl: String? = null,
    val lastUpdated: Long? = null,
    /** Any refresh in flight (auto or manual). */
    val isRefreshing: Boolean = false,
    /** Only a user pull-to-refresh, so the pull indicator doesn't flash on every auto refresh. */
    val isUserRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val offlineMessage: String? = null,
    /** At least one refresh attempt has finished. */
    val initialAttemptDone: Boolean = false,
) {
    val hasData: Boolean get() = groups.isNotEmpty()

    /** First launch with nothing cached and the server unreachable. */
    val needsSetup: Boolean get() = initialAttemptDone && !hasData && isOffline

    val allServices: List<ServiceUi> get() = groups.flatMap { it.services }

    fun service(key: String): ServiceUi? = allServices.firstOrNull { it.key == key }

    val nowPlaying: List<NowPlaying>
        get() = widgets.values.flatMap { it.data?.sessions.orEmpty() }

    val queue: List<QueueItem>
        get() = widgets.values.flatMap { it.data?.queue.orEmpty() }

    /** Server-side queue total (may exceed [queue].size, which is one page). */
    val queueTotal: Long
        get() = widgets.values.sumOf { w -> w.data?.let { it.queueTotal ?: it.queue.size.toLong() } ?: 0L }

    val activeTorrents: List<TorrentItem>
        get() = widgets.values.flatMap { it.data?.torrents.orEmpty() }
            .filter { it.isActive }
            .sortedWith(compareBy<TorrentItem> { it.isComplete }.thenByDescending { it.downloadSpeed + it.uploadSpeed })
}

class DashboardViewModel(container: AppContainer) : ViewModel() {

    private val repo = container.repository
    private val resolver = container.resolver
    private val cache = container.cache
    private val store = container.settings

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    val settings: StateFlow<AppSettings> =
        store.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private var refreshJob: Job? = null
    private var lastStarted = 0L
    private val cacheLoaded = Job()

    init {
        viewModelScope.launch {
            cache.load()?.let { applyCache(it) }
            cacheLoaded.complete()
        }
        // Server settings changed: re-resolve and reload straight away.
        viewModelScope.launch {
            store.settings.map { it.urlSignature }.distinctUntilChanged().drop(1).collect {
                refresh(userInitiated = true, forceResolve = true)
            }
        }
    }

    /** Called by screens' auto-refresh loop; ignored if a refresh just started. */
    fun autoRefresh() {
        if (System.currentTimeMillis() - lastStarted < MIN_AUTO_GAP_MS) return
        refresh(userInitiated = false)
    }

    fun refresh(userInitiated: Boolean = true, forceResolve: Boolean = false) {
        if (refreshJob?.isActive == true) {
            if (userInitiated) _state.update { it.copy(isUserRefreshing = true) }
            return
        }
        lastStarted = System.currentTimeMillis()
        refreshJob = viewModelScope.launch {
            cacheLoaded.join()
            _state.update { it.copy(isRefreshing = true, isUserRefreshing = userInitiated) }
            try {
                load(forceResolve || userInitiated && _state.value.isOffline)
            } finally {
                _state.update { it.copy(isRefreshing = false, isUserRefreshing = false, initialAttemptDone = true) }
            }
        }
    }

    fun toggleGroup(name: String) {
        viewModelScope.launch { store.toggleGroupCollapsed(name) }
    }

    private suspend fun load(forceResolve: Boolean) {
        val resolved = resolver.resolve(force = forceResolve)
        _state.update { it.copy(baseUrl = resolved.display) }

        val groupsResult = resultOf { repo.services() }
        val groups = groupsResult.getOrElse { e ->
            resolver.invalidate()
            _state.update {
                it.copy(isOffline = true, offlineMessage = e.toUserMessage(), connection = ConnectionKind.OFFLINE)
            }
            return
        }

        val services = groups.flatMap { it.services }
        val keys = services.map { it.key }.toSet()
        _state.update { s ->
            s.copy(
                groups = groups,
                connection = resolved.kind,
                isOffline = false,
                offlineMessage = null,
                widgets = services
                    .filter { it.widget != null && WidgetMappers.isSupported(it.widget.type) }
                    .associate { svc -> svc.key to (s.widgets[svc.key]?.copy(loading = true) ?: WidgetState(loading = true)) },
                docker = s.docker.filterKeys { it in keys },
                dockerStats = s.dockerStats.filterKeys { it in keys },
                health = s.health.filterKeys { it in keys },
            )
        }

        supervisorScope {
            val jobs = buildList {
                // Service widgets, all in parallel. One bad widget only marks its own card.
                services.filter { it.key in _state.value.widgets }.forEach { svc ->
                    add(async {
                        val result = repo.widgetData(svc.widget!!)
                        _state.update { s ->
                            val prev = s.widgets[svc.key]
                            val next = result.fold(
                                onSuccess = { WidgetState(data = it) },
                                onFailure = { WidgetState(data = prev?.data, error = it.toUserMessage()) },
                            )
                            s.copy(widgets = s.widgets + (svc.key to next))
                        }
                    })
                }

                // Docker status: one call per server.
                services.filter { it.hasDocker }.groupBy { it.server!! }.forEach { (server, svcs) ->
                    add(async {
                        val statuses = resultOf { repo.dockerStatuses(server) }
                        _state.update { s ->
                            s.copy(docker = s.docker + svcs.associate { svc ->
                                svc.key to statuses.fold(
                                    onSuccess = { it[svc.container!!] ?: DockerState("not found") },
                                    onFailure = { DockerState("unknown") },
                                )
                            })
                        }
                    })
                    svcs.forEach { svc ->
                        add(async {
                            resultOf { repo.dockerStats(server, svc.container!!) }.getOrNull()?.let { stats ->
                                _state.update { s -> s.copy(dockerStats = s.dockerStats + (svc.key to stats)) }
                            }
                        })
                    }
                }

                // Optional ping / siteMonitor.
                services.filter { it.hasHealthCheck }.forEach { svc ->
                    add(async {
                        val h = resultOf { repo.healthCheck(svc) }.getOrElse { HealthCheck(up = false) }
                        if (h != null) _state.update { s -> s.copy(health = s.health + (svc.key to h)) }
                    })
                }

                add(async {
                    resultOf { repo.resources() }.onSuccess { r -> _state.update { it.copy(resources = r) } }
                })
                add(async {
                    resultOf { repo.bookmarks() }.onSuccess { b -> _state.update { it.copy(bookmarks = b) } }
                })
            }
            jobs.awaitAll()
        }

        val now = System.currentTimeMillis()
        _state.update { it.copy(lastUpdated = now) }
        cache.save(snapshot(_state.value, now))
    }

    private fun applyCache(c: CachedDashboard) {
        _state.update {
            it.copy(
                groups = c.groups,
                widgets = c.widgets.mapValues { (_, d) -> WidgetState(data = d) },
                docker = c.docker,
                dockerStats = c.dockerStats,
                health = c.health,
                resources = c.resources,
                bookmarks = c.bookmarks,
                lastUpdated = c.savedAt,
            )
        }
    }

    private fun snapshot(s: DashboardUiState, now: Long) = CachedDashboard(
        savedAt = now,
        groups = s.groups,
        widgets = s.widgets.mapNotNull { (k, v) -> v.data?.let { k to it } }.toMap(),
        docker = s.docker,
        dockerStats = s.dockerStats,
        health = s.health,
        resources = s.resources,
        bookmarks = s.bookmarks,
    )

    private companion object {
        const val MIN_AUTO_GAP_MS = 3_000L
    }
}
