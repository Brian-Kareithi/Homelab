package dev.kareithi.homelab.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.kareithi.homelab.data.net.ConnectionTestResult
import dev.kareithi.homelab.data.settings.AppSettings
import dev.kareithi.homelab.data.settings.NotificationKind
import dev.kareithi.homelab.data.settings.ThemeMode
import dev.kareithi.homelab.data.settings.UrlMode
import dev.kareithi.homelab.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class UrlField { LAN, TAILSCALE, CUSTOM }

sealed interface TestState {
    data object Running : TestState
    data class Done(val result: ConnectionTestResult) : TestState
}

class SettingsViewModel(
    container: AppContainer,
    @Suppress("unused") private val app: Application,
) : ViewModel() {

    private val store = container.settings
    private val resolver = container.resolver

    /** Null until DataStore has been read once, so fields never flash defaults. */
    val settings: StateFlow<AppSettings?> =
        store.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _tests = MutableStateFlow<Map<UrlField, TestState>>(emptyMap())
    val tests: StateFlow<Map<UrlField, TestState>> = _tests.asStateFlow()

    fun setUrlMode(mode: UrlMode) = launch { store.setUrlMode(mode) }

    fun saveUrls(lan: String, tailscale: String, custom: String) = launch {
        store.setLanUrl(lan)
        store.setTailscaleUrl(tailscale)
        store.setCustomUrl(custom)
    }

    /** Tests the URLs that matter for [mode], using the values currently typed in. */
    fun test(mode: UrlMode, lan: String, tailscale: String, custom: String) {
        val targets = when (mode) {
            UrlMode.AUTO -> listOf(UrlField.LAN to lan, UrlField.TAILSCALE to tailscale)
            UrlMode.LAN -> listOf(UrlField.LAN to lan)
            UrlMode.TAILSCALE -> listOf(UrlField.TAILSCALE to tailscale)
            UrlMode.CUSTOM -> listOf(UrlField.CUSTOM to custom)
        }
        _tests.value = targets.associate { it.first to TestState.Running }
        targets.forEach { (field, url) ->
            viewModelScope.launch {
                val result = resolver.test(url)
                _tests.update { it + (field to TestState.Done(result)) }
            }
        }
    }

    fun clearTests() {
        _tests.value = emptyMap()
    }

    fun setRefreshSeconds(seconds: Int) = launch { store.setRefreshSeconds(seconds) }
    fun setTheme(theme: ThemeMode) = launch { store.setTheme(theme) }
    fun setAccent(argb: Long) = launch { store.setAccent(argb) }
    fun setDynamicColor(enabled: Boolean) = launch { store.setDynamicColor(enabled) }
    fun setTitle(title: String) = launch { store.setTitle(title) }

    fun setNotification(kind: NotificationKind, enabled: Boolean) = launch {
        store.setNotification(kind, enabled)
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
