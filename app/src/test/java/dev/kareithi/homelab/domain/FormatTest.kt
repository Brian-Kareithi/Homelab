package dev.kareithi.homelab.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class FormatTest {

    @Test
    fun bytes() {
        assertEquals("0 B", Format.bytes(0.0))
        assertEquals("512 B", Format.bytes(512.0))
        assertEquals("1.2 GB", Format.bytes(1.2 * 1024 * 1024 * 1024))
        assertEquals("2.8 TB", Format.bytes(2.8 * 1024.0 * 1024 * 1024 * 1024))
        assertEquals("160 MB", Format.bytes(167_735_296L))
    }

    @Test
    fun speeds() {
        assertEquals("0 B/s", Format.speed(0L))
        assertEquals("512 KB/s", Format.speed(524_288L))
        assertEquals("4.3 MB/s", Format.speed(4_508_876L))
    }

    @Test
    fun compactNumbers() {
        assertEquals("999", Format.compact(999))
        assertEquals("2.1k", Format.compact(2_100))
        assertEquals("2k", Format.compact(2_000))
        assertEquals("1.3M", Format.compact(1_250_000))
    }

    @Test
    fun jellyfinTicks() {
        assertEquals(1L, Format.ticksToSeconds(10_000_000L))
        assertEquals(3600L, Format.ticksToSeconds(36_000_000_000L))
        assertEquals("1:00:00", Format.duration(3600))
        assertEquals("4:05", Format.duration(245))
    }

    @Test
    fun relativeTime() {
        val now = Instant.parse("2026-01-01T12:00:00Z")
        assertEquals("3 min ago", Format.relative(now.minusSeconds(180), now))
        assertEquals("in 12 min", Format.relative(now.plusSeconds(720), now))
        assertEquals("just now", Format.relative(now.minusSeconds(10), now))
        assertEquals("in 2 h", Format.relative(now.plusSeconds(7200), now))
        assertEquals("3 d ago", Format.relative(now.minusSeconds(3 * 86_400), now))
    }

    @Test
    fun eta() {
        assertEquals("∞", Format.eta(Format.QBT_INFINITE_ETA))
        assertEquals("45 s", Format.eta(45))
        assertEquals("12 min", Format.eta(720))
        assertEquals("1 h 5 min", Format.eta(3900))
    }

    @Test
    fun parseInstant() {
        assertEquals(Instant.parse("2026-01-01T12:00:00Z"), Format.parseInstant("2026-01-01T12:00:00Z"))
        assertEquals(null, Format.parseInstant("not a date"))
    }
}
