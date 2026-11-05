package dev.kareithi.homelab.domain

/** Where a service icon should come from. Pure data so it can be unit-tested without Android. */
sealed interface IconSource {
    /** Material Design Icon. The UI uses a built-in Material icon when one maps, else [url]. */
    data class Mdi(val name: String, val url: String, val colorHex: String? = null) : IconSource

    /** Remote image. Monochrome sources (simple-icons) set [tint] so they show on dark cards. */
    data class Remote(val url: String, val tint: Boolean = false, val colorHex: String? = null) : IconSource

    /** Nothing usable: show the first-letter fallback. */
    data object None : IconSource
}

object IconResolver {
    const val MDI_BASE = "https://cdn.jsdelivr.net/npm/@mdi/svg@7.4.47/svg/"
    const val SIMPLE_ICONS_BASE = "https://cdn.jsdelivr.net/npm/simple-icons@13/icons/"
    const val DASHBOARD_ICONS_BASE = "https://cdn.jsdelivr.net/gh/homarr-labs/dashboard-icons/"
    const val SELFHST_BASE = "https://cdn.jsdelivr.net/gh/selfhst/icons/"

    private val IMAGE_EXTENSIONS = listOf("png", "svg", "webp")

    /** Homepage allows a colour suffix such as `mdi-wifi-alert-#f0d453`. */
    private val COLOR_SUFFIX = Regex("-(#[0-9a-fA-F]{3,8})$")

    /**
     * @param baseUrl Homepage base URL, used for icons served by Homepage itself (`/icons/foo.png`).
     */
    fun resolve(icon: String?, baseUrl: String? = null): IconSource {
        val raw = icon?.trim().orEmpty()
        if (raw.isEmpty()) return IconSource.None

        return when {
            raw.startsWith("mdi-") -> {
                val (name, color) = splitColor(raw.removePrefix("mdi-"))
                if (name.isEmpty()) IconSource.None
                else IconSource.Mdi(name = name, url = "$MDI_BASE$name.svg", colorHex = color)
            }

            raw.startsWith("si-") -> {
                val (name, color) = splitColor(raw.removePrefix("si-"))
                if (name.isEmpty()) IconSource.None
                else IconSource.Remote("$SIMPLE_ICONS_BASE$name.svg", tint = true, colorHex = color)
            }

            raw.startsWith("sh-") -> {
                val name = raw.removePrefix("sh-")
                val ext = extensionOf(name)
                if (ext != null) IconSource.Remote("$SELFHST_BASE$ext/$name")
                else IconSource.Remote("${SELFHST_BASE}png/$name.png")
            }

            raw.startsWith("http://", ignoreCase = true) ||
                raw.startsWith("https://", ignoreCase = true) -> IconSource.Remote(raw)

            raw.startsWith("/") -> {
                val base = baseUrl?.trimEnd('/')
                if (base.isNullOrEmpty()) IconSource.None else IconSource.Remote(base + raw)
            }

            else -> {
                val ext = extensionOf(raw)
                if (ext != null) IconSource.Remote("$DASHBOARD_ICONS_BASE$ext/$raw")
                else IconSource.Remote("${DASHBOARD_ICONS_BASE}png/$raw.png")
            }
        }
    }

    /** First letter for the fallback tile. */
    fun initial(name: String): String =
        name.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "?"

    private fun extensionOf(name: String): String? {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext.takeIf { it in IMAGE_EXTENSIONS }
    }

    private fun splitColor(name: String): Pair<String, String?> {
        val match = COLOR_SUFFIX.find(name) ?: return name to null
        return name.removeRange(match.range) to match.groupValues[1]
    }
}
