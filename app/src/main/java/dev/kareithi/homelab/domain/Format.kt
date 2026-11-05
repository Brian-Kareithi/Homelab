package dev.kareithi.homelab.domain

import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.abs

object Format {

    private val UNITS = arrayOf("B", "KB", "MB", "GB", "TB", "PB", "EB")

    /** qBittorrent reports this ETA for "infinite". */
    const val QBT_INFINITE_ETA = 8_640_000L

    /** 1024-based: "512 B", "1.2 GB", "2.8 TB". Below 100 one decimal, otherwise none. */
    fun bytes(value: Double): String {
        if (value.isNaN() || value <= 0.0) return "0 B"
        if (value < 1024.0) return "${value.toLong()} B"
        var v = value
        var unit = 0
        while (v >= 1024.0 && unit < UNITS.lastIndex) {
            v /= 1024.0
            unit++
        }
        return "${decimal(v)} ${UNITS[unit]}"
    }

    fun bytes(value: Long): String = bytes(value.toDouble())

    /** "0 B/s", "512 KB/s", "4.3 MB/s". */
    fun speed(bytesPerSecond: Double): String = bytes(bytesPerSecond) + "/s"

    fun speed(bytesPerSecond: Long): String = speed(bytesPerSecond.toDouble())

    /** 999 -> "999", 2100 -> "2.1k", 2000 -> "2k", 1_250_000 -> "1.3M". */
    fun compact(n: Long): String {
        val a = abs(n)
        return when {
            a < 1_000 -> n.toString()
            a < 1_000_000 -> trimZero(n / 1_000.0) + "k"
            a < 1_000_000_000 -> trimZero(n / 1_000_000.0) + "M"
            else -> trimZero(n / 1_000_000_000.0) + "B"
        }
    }

    fun percent(value: Double): String = String.format(Locale.US, "%.0f%%", value)

    /** Jellyfin: 1 tick = 100 ns. */
    fun ticksToSeconds(ticks: Long): Long = ticks / 10_000_000L

    /** 65 -> "1:05", 3725 -> "1:02:05". */
    fun duration(seconds: Long): String {
        val s = seconds.coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, sec)
        else String.format(Locale.US, "%d:%02d", m, sec)
    }

    /** Remaining time for qBittorrent: "∞", "45 s", "12 min", "3 h 5 min", "2 d". */
    fun eta(seconds: Long): String = when {
        seconds < 0 || seconds >= QBT_INFINITE_ETA -> "∞"
        seconds < 60 -> "$seconds s"
        seconds < 3600 -> "${seconds / 60} min"
        seconds < 86_400 -> {
            val m = (seconds % 3600) / 60
            if (m == 0L) "${seconds / 3600} h" else "${seconds / 3600} h $m min"
        }
        else -> "${seconds / 86_400} d"
    }

    /** "just now", "3 min ago", "in 12 min", "2 h ago", "in 3 d". */
    fun relative(target: Instant, now: Instant): String {
        val diff = Duration.between(now, target).seconds
        val future = diff > 0
        val a = abs(diff)
        val unit = when {
            a < 60 -> return if (future) "in <1 min" else "just now"
            a < 3600 -> "${a / 60} min"
            a < 86_400 -> "${a / 3600} h"
            else -> "${a / 86_400} d"
        }
        return if (future) "in $unit" else "$unit ago"
    }

    fun relative(epochMillis: Long, nowMillis: Long): String =
        relative(Instant.ofEpochMilli(epochMillis), Instant.ofEpochMilli(nowMillis))

    /** Parses an ISO-8601 instant (Sonarr's `estimatedCompletionTime`), or null if malformed. */
    fun parseInstant(iso: String?): Instant? {
        if (iso.isNullOrBlank()) return null
        return try {
            Instant.parse(iso)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun decimal(v: Double): String =
        if (v >= 100) String.format(Locale.US, "%.0f", v) else String.format(Locale.US, "%.1f", v)

    private fun trimZero(v: Double): String {
        val s = String.format(Locale.US, "%.1f", v)
        return if (s.endsWith(".0")) s.dropLast(2) else s
    }
}
