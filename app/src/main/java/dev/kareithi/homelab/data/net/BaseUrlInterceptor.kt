package dev.kareithi.homelab.data.net

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Rewrites every Retrofit request onto the currently resolved Homepage base URL (scheme, host,
 * port and any path prefix), so switching LAN / Tailscale / Custom never rebuilds Retrofit.
 */
class BaseUrlInterceptor(private val resolver: BaseUrlResolver) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // Normally already resolved by the caller; OkHttp threads may block on the first call.
        val base = (resolver.currentOrNull() ?: runBlocking { resolver.resolve() }).url
        val original = chain.request()
        val prefix = base.encodedPath.trimEnd('/')
        val newUrl = original.url.newBuilder()
            .scheme(base.scheme)
            .host(base.host)
            .port(base.port)
            .encodedPath(prefix + original.url.encodedPath)
            .build()
        return chain.proceed(original.newBuilder().url(newUrl).build())
    }
}
