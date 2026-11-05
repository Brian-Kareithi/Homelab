package dev.kareithi.homelab.di

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dev.kareithi.homelab.data.api.HomepageApi
import dev.kareithi.homelab.data.net.BaseUrlInterceptor
import dev.kareithi.homelab.data.net.BaseUrlResolver
import dev.kareithi.homelab.data.repo.DashboardCache
import dev.kareithi.homelab.data.repo.HomepageRepository
import dev.kareithi.homelab.data.settings.SettingsStore
import dev.kareithi.homelab.domain.HomelabJson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Manual dependency container (one instance, owned by [dev.kareithi.homelab.HomelabApp]).
 * Chosen over Hilt to keep the build free of annotation processing.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val settings = SettingsStore(appContext)

    /** Shared by Retrofit, the URL probe and Coil (icons). */
    val baseClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    val resolver = BaseUrlResolver(baseClient, settings)

    private val apiClient: OkHttpClient = baseClient.newBuilder()
        .addInterceptor(BaseUrlInterceptor(resolver))
        .build()

    val api: HomepageApi = Retrofit.Builder()
        // Placeholder: BaseUrlInterceptor swaps in the real scheme/host/port on every request.
        .baseUrl("http://homepage.invalid/")
        .client(apiClient)
        .addConverterFactory(HomelabJson.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(HomepageApi::class.java)

    val repository = HomepageRepository(api)

    val cache = DashboardCache(File(appContext.cacheDir, "dashboard_cache.json"))

    init {
        // Re-run the Auto LAN→Tailscale decision whenever the device switches networks.
        val cm = appContext.getSystemService(ConnectivityManager::class.java)
        cm?.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = resolver.invalidate()
            override fun onLost(network: Network) = resolver.invalidate()
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                resolver.invalidate()
            }
        })
    }
}
