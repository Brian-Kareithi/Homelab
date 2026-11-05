package dev.kareithi.homelab.data.net

import dev.kareithi.homelab.data.settings.AppSettings
import dev.kareithi.homelab.data.settings.Defaults
import dev.kareithi.homelab.data.settings.SettingsStore
import dev.kareithi.homelab.data.settings.UrlMode
import dev.kareithi.homelab.domain.ConnectionKind
import dev.kareithi.homelab.domain.HomelabJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import okhttp3.Dispatcher
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ResolvedUrl(
    val url: HttpUrl,
    val kind: ConnectionKind,
    val signature: String,
) {
    /** Base URL without a trailing slash, for display and for Homepage-relative icons. */
    val display: String get() = url.toString().trimEnd('/')
}

sealed interface ConnectionTestResult {
    data class Success(val groups: Int, val services: Int) : ConnectionTestResult
    data class Failure(val message: String) : ConnectionTestResult
}

object UrlUtils {
    /** Accepts "192.168.1.2:3000", "http://host:3000/", etc. Returns null when unusable. */
    fun normalize(raw: String): HttpUrl? {
        val trimmed = raw.trim().trimEnd('/')
        if (trimmed.isEmpty()) return null
        val withScheme = if (trimmed.contains("://")) trimmed else "http://$trimmed"
        val parsed = withScheme.toHttpUrlOrNull() ?: return null
        return if (parsed.encodedPath.endsWith("/")) parsed else parsed.newBuilder().addPathSegment("").build()
    }
}

/**
 * Decides which Homepage base URL to use.
 *
 * In Auto mode the LAN address is probed with a 1.5 s timeout; if it doesn't answer, Tailscale is
 * used. The decision is cached and re-evaluated after [AUTO_TTL_MS], when the network changes
 * ([invalidate]) or when the settings change.
 */
class BaseUrlResolver(
    baseClient: OkHttpClient,
    private val settings: SettingsStore,
) {
    private val probeClient = baseClient.newBuilder()
        .dispatcher(Dispatcher())
        .connectTimeout(PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .readTimeout(PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .callTimeout(PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .build()

    private val testClient = baseClient.newBuilder()
        .dispatcher(Dispatcher())
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    private val mutex = Mutex()

    @Volatile
    private var cached: ResolvedUrl? = null

    @Volatile
    private var cachedAt: Long = 0L

    private val _current = MutableStateFlow<ResolvedUrl?>(null)
    val current: StateFlow<ResolvedUrl?> = _current.asStateFlow()

    fun currentOrNull(): ResolvedUrl? = cached

    /** Forces the next [resolve] to re-evaluate (e.g. after a network change). */
    fun invalidate() {
        cachedAt = 0L
    }

    suspend fun resolve(force: Boolean = false): ResolvedUrl = mutex.withLock {
        val s = settings.current()
        val c = cached
        val stillValid = c != null && !force && c.signature == s.urlSignature &&
            (s.urlMode != UrlMode.AUTO || System.currentTimeMillis() - cachedAt < AUTO_TTL_MS)
        if (stillValid && c != null) {
            c
        } else {
            val resolved = compute(s)
            cached = resolved
            cachedAt = System.currentTimeMillis()
            _current.value = resolved
            resolved
        }
    }

    private suspend fun compute(s: AppSettings): ResolvedUrl {
        val tailscale = UrlUtils.normalize(s.tailscaleUrl) ?: UrlUtils.normalize(Defaults.TAILSCALE_URL)!!
        return when (s.urlMode) {
            UrlMode.LAN -> ResolvedUrl(
                UrlUtils.normalize(s.lanUrl) ?: tailscale, ConnectionKind.LAN, s.urlSignature,
            )
            UrlMode.TAILSCALE -> ResolvedUrl(tailscale, ConnectionKind.TAILSCALE, s.urlSignature)
            UrlMode.CUSTOM -> ResolvedUrl(
                UrlUtils.normalize(s.customUrl) ?: tailscale, ConnectionKind.CUSTOM, s.urlSignature,
            )
            UrlMode.AUTO -> {
                val lan = UrlUtils.normalize(s.lanUrl)
                if (lan != null && probe(lan)) ResolvedUrl(lan, ConnectionKind.LAN, s.urlSignature)
                else ResolvedUrl(tailscale, ConnectionKind.TAILSCALE, s.urlSignature)
            }
        }
    }

    /** True when Homepage answers on [base] within 1.5 s. */
    suspend fun probe(base: HttpUrl): Boolean = withContext(Dispatchers.IO) {
        val url = base.resolve("api/widgets") ?: return@withContext false
        try {
            probeClient.newCall(Request.Builder().url(url).get().build()).execute().use { it.isSuccessful }
        } catch (_: IOException) {
            false
        }
    }

    /** Settings "Test connection": hits `/api/services` and checks it is a Homepage response. */
    suspend fun test(rawUrl: String): ConnectionTestResult = withContext(Dispatchers.IO) {
        val base = UrlUtils.normalize(rawUrl)
            ?: return@withContext ConnectionTestResult.Failure("Invalid URL")
        val url = base.resolve("api/services")
            ?: return@withContext ConnectionTestResult.Failure("Invalid URL")
        try {
            testClient.newCall(Request.Builder().url(url).get().build()).execute().use { resp ->
                if (!resp.isSuccessful) return@use ConnectionTestResult.Failure("HTTP ${resp.code}")
                val body = resp.body?.string().orEmpty()
                val arr = runCatching { HomelabJson.parseToJsonElement(body) }.getOrNull() as? JsonArray
                    ?: return@use ConnectionTestResult.Failure("Reachable, but not a Homepage API response")
                val services = arr.sumOf { g ->
                    ((g as? kotlinx.serialization.json.JsonObject)?.get("services") as? JsonArray)?.size ?: 0
                }
                ConnectionTestResult.Success(groups = arr.size, services = services)
            }
        } catch (e: IOException) {
            ConnectionTestResult.Failure(e.message?.takeIf { it.isNotBlank() } ?: "Unreachable")
        }
    }

    companion object {
        const val PROBE_TIMEOUT_MS = 1_500L
        const val AUTO_TTL_MS = 2 * 60_000L
    }
}
