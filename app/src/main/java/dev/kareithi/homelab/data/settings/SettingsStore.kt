package dev.kareithi.homelab.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class UrlMode(val label: String) {
    AUTO("Auto"),
    LAN("LAN"),
    TAILSCALE("Tailscale"),
    CUSTOM("Custom"),
}

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    DARK("Dark"),
    LIGHT("Light"),
}

enum class NotificationKind(val key: String, val label: String, val description: String) {
    DOCKER("docker", "Container stopped", "A Docker container leaves the running state"),
    SERVER("server", "Server reachability", "Homepage becomes unreachable, then reachable again"),
    TORRENTS("torrents", "Torrent completed", "A qBittorrent download finishes"),
    REQUESTS("requests", "New Seerr request", "A request is waiting for approval"),
}

object Defaults {
    const val LAN_URL = "http://192.168.100.35:3000"
    const val TAILSCALE_URL = "http://100.100.173.21:3000"
    const val MAGIC_DNS_URL = "http://dreezy0:3000"
    const val TITLE = "Kareithi's Homelab"
    const val DESCRIPTION = "Movies, TV, Music & Downloads"
    const val ACCENT: Long = 0xFF4392F1
    const val REFRESH_SECONDS = 15
    val REFRESH_OPTIONS = listOf(5, 15, 30, 60, 0) // 0 = off
    val ACCENT_OPTIONS: List<Long> = listOf(
        0xFF4392F1, // Blue Energy
        0xFFDC493A, // Cinnabar
        0xFF8A6FDF, // Iris (Lavender Mist hue)
        0xFF2F9E9A, // Lagoon
        0xFFE8913A, // Ember
        0xFF12223A, // Ink
    )
}

data class AppSettings(
    val urlMode: UrlMode = UrlMode.TAILSCALE,
    val lanUrl: String = Defaults.LAN_URL,
    val tailscaleUrl: String = Defaults.TAILSCALE_URL,
    val customUrl: String = Defaults.MAGIC_DNS_URL,
    val refreshSeconds: Int = Defaults.REFRESH_SECONDS,
    val theme: ThemeMode = ThemeMode.LIGHT,
    val accent: Long = Defaults.ACCENT,
    val dynamicColor: Boolean = false,
    val title: String = Defaults.TITLE,
    val collapsedGroups: Set<String> = emptySet(),
    val notifications: Set<NotificationKind> = emptySet(),
) {
    /** Changes to any of these invalidate the resolved base URL. */
    val urlSignature: String get() = "$urlMode|$lanUrl|$tailscaleUrl|$customUrl"
}

/** What the background worker remembers between runs to detect transitions. */
data class WorkerState(
    val serverUp: Boolean? = null,
    val containerStates: Map<String, String> = emptyMap(),
    val incompleteTorrents: Set<String>? = null,
    val seerrPending: Long? = null,
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "homelab_settings")

class SettingsStore(context: Context) {

    private val store = context.applicationContext.settingsDataStore

    private object Keys {
        val URL_MODE = stringPreferencesKey("url_mode")
        val LAN_URL = stringPreferencesKey("lan_url")
        val TAILSCALE_URL = stringPreferencesKey("tailscale_url")
        val CUSTOM_URL = stringPreferencesKey("custom_url")
        val REFRESH = intPreferencesKey("refresh_seconds")
        val THEME = stringPreferencesKey("theme")
        val ACCENT = longPreferencesKey("accent")
        val DYNAMIC = booleanPreferencesKey("dynamic_color")
        val TITLE = stringPreferencesKey("title")
        val COLLAPSED = stringSetPreferencesKey("collapsed_groups")
        val NOTIFICATIONS = stringSetPreferencesKey("notifications")

        val W_SERVER_UP = intPreferencesKey("w_server_up") // -1 unknown, 0 down, 1 up
        val W_CONTAINERS = stringSetPreferencesKey("w_containers") // "key\u0000status"
        val W_TORRENTS = stringSetPreferencesKey("w_incomplete_torrents")
        val W_TORRENTS_KNOWN = booleanPreferencesKey("w_torrents_known")
        val W_SEERR = longPreferencesKey("w_seerr_pending") // -1 unknown
    }

    private val data: Flow<Preferences> = store.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val settings: Flow<AppSettings> = data.map { p ->
        AppSettings(
            urlMode = enumOr(p[Keys.URL_MODE], UrlMode.TAILSCALE),
            lanUrl = p[Keys.LAN_URL] ?: Defaults.LAN_URL,
            tailscaleUrl = p[Keys.TAILSCALE_URL] ?: Defaults.TAILSCALE_URL,
            customUrl = p[Keys.CUSTOM_URL] ?: Defaults.MAGIC_DNS_URL,
            refreshSeconds = p[Keys.REFRESH] ?: Defaults.REFRESH_SECONDS,
            theme = enumOr(p[Keys.THEME], ThemeMode.LIGHT),
            accent = p[Keys.ACCENT] ?: Defaults.ACCENT,
            dynamicColor = p[Keys.DYNAMIC] ?: false,
            title = p[Keys.TITLE]?.takeIf { it.isNotBlank() } ?: Defaults.TITLE,
            collapsedGroups = p[Keys.COLLAPSED] ?: emptySet(),
            notifications = p[Keys.NOTIFICATIONS].orEmpty()
                .mapNotNull { k -> NotificationKind.entries.firstOrNull { it.key == k } }
                .toSet(),
        )
    }.distinctUntilChanged()

    suspend fun current(): AppSettings = settings.first()

    suspend fun setUrlMode(mode: UrlMode) = store.edit { it[Keys.URL_MODE] = mode.name }
    suspend fun setLanUrl(url: String) = store.edit { it[Keys.LAN_URL] = url.trim() }
    suspend fun setTailscaleUrl(url: String) = store.edit { it[Keys.TAILSCALE_URL] = url.trim() }
    suspend fun setCustomUrl(url: String) = store.edit { it[Keys.CUSTOM_URL] = url.trim() }
    suspend fun setRefreshSeconds(seconds: Int) = store.edit { it[Keys.REFRESH] = seconds }
    suspend fun setTheme(theme: ThemeMode) = store.edit { it[Keys.THEME] = theme.name }
    suspend fun setAccent(argb: Long) = store.edit { it[Keys.ACCENT] = argb }
    suspend fun setDynamicColor(enabled: Boolean) = store.edit { it[Keys.DYNAMIC] = enabled }
    suspend fun setTitle(title: String) = store.edit { it[Keys.TITLE] = title.trim() }

    suspend fun toggleGroupCollapsed(group: String) = store.edit {
        val current = it[Keys.COLLAPSED].orEmpty()
        it[Keys.COLLAPSED] = if (group in current) current - group else current + group
    }

    suspend fun setNotification(kind: NotificationKind, enabled: Boolean) = store.edit {
        val current = it[Keys.NOTIFICATIONS].orEmpty()
        it[Keys.NOTIFICATIONS] = if (enabled) current + kind.key else current - kind.key
    }

    // --- Worker state -------------------------------------------------------------------------

    suspend fun workerState(): WorkerState {
        val p = data.first()
        return WorkerState(
            serverUp = when (p[Keys.W_SERVER_UP]) {
                1 -> true
                0 -> false
                else -> null
            },
            containerStates = p[Keys.W_CONTAINERS].orEmpty().mapNotNull { entry ->
                val i = entry.indexOf(SEP)
                if (i <= 0) null else entry.substring(0, i) to entry.substring(i + 1)
            }.toMap(),
            incompleteTorrents = if (p[Keys.W_TORRENTS_KNOWN] == true) p[Keys.W_TORRENTS].orEmpty() else null,
            seerrPending = p[Keys.W_SEERR]?.takeIf { it >= 0 },
        )
    }

    suspend fun saveWorkerState(state: WorkerState) = store.edit {
        it[Keys.W_SERVER_UP] = when (state.serverUp) {
            true -> 1
            false -> 0
            null -> -1
        }
        it[Keys.W_CONTAINERS] = state.containerStates.map { (k, v) -> "$k$SEP$v" }.toSet()
        it[Keys.W_TORRENTS_KNOWN] = state.incompleteTorrents != null
        it[Keys.W_TORRENTS] = state.incompleteTorrents.orEmpty()
        it[Keys.W_SEERR] = state.seerrPending ?: -1L
    }

    private companion object {
        const val SEP = '\u0000'

        inline fun <reified E : Enum<E>> enumOr(name: String?, fallback: E): E =
            name?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: fallback
    }
}
