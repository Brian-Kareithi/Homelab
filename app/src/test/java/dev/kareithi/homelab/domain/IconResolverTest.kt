package dev.kareithi.homelab.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class IconResolverTest {

    @Test
    fun mdiIcon() {
        assertEquals(
            IconSource.Mdi("wifi-alert", "https://cdn.jsdelivr.net/npm/@mdi/svg@7.4.47/svg/wifi-alert.svg"),
            IconResolver.resolve("mdi-wifi-alert"),
        )
    }

    @Test
    fun mdiIconWithColorSuffix() {
        assertEquals(
            IconSource.Mdi("wifi-alert", "https://cdn.jsdelivr.net/npm/@mdi/svg@7.4.47/svg/wifi-alert.svg", "#f0d453"),
            IconResolver.resolve("mdi-wifi-alert-#f0d453"),
        )
    }

    @Test
    fun simpleIcon() {
        assertEquals(
            IconSource.Remote("https://cdn.jsdelivr.net/npm/simple-icons@13/icons/github.svg", tint = true),
            IconResolver.resolve("si-github"),
        )
    }

    @Test
    fun absoluteUrlUsedAsIs() {
        val url = "https://example.com/icon.png"
        assertEquals(IconSource.Remote(url), IconResolver.resolve(url))
        assertEquals(IconSource.Remote("http://dreezy0:8080/x.svg"), IconResolver.resolve("http://dreezy0:8080/x.svg"))
    }

    @Test
    fun fileNameWithExtension() {
        assertEquals(
            IconSource.Remote("https://cdn.jsdelivr.net/gh/homarr-labs/dashboard-icons/png/uptime-kuma.png"),
            IconResolver.resolve("uptime-kuma.png"),
        )
        assertEquals(
            IconSource.Remote("https://cdn.jsdelivr.net/gh/homarr-labs/dashboard-icons/svg/casaos.svg"),
            IconResolver.resolve("casaos.svg"),
        )
        assertEquals(
            IconSource.Remote("https://cdn.jsdelivr.net/gh/homarr-labs/dashboard-icons/webp/coolify.webp"),
            IconResolver.resolve("coolify.webp"),
        )
    }

    @Test
    fun bareName() {
        assertEquals(
            IconSource.Remote("https://cdn.jsdelivr.net/gh/homarr-labs/dashboard-icons/png/sonarr.png"),
            IconResolver.resolve("sonarr"),
        )
    }

    @Test
    fun homepageRelativePath() {
        assertEquals(
            IconSource.Remote("http://192.168.100.35:3000/icons/custom.png"),
            IconResolver.resolve("/icons/custom.png", "http://192.168.100.35:3000/"),
        )
        assertEquals(IconSource.None, IconResolver.resolve("/icons/custom.png", null))
    }

    @Test
    fun emptyFallsBackToLetter() {
        assertEquals(IconSource.None, IconResolver.resolve(null))
        assertEquals(IconSource.None, IconResolver.resolve("  "))
        assertEquals("S", IconResolver.initial("sonarr"))
        assertEquals("Q", IconResolver.initial(" qBittorrent"))
        assertEquals("?", IconResolver.initial(""))
    }
}
