package dev.kareithi.homelab.domain

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class WidgetMappersTest {

    private fun j(s: String): JsonElement = HomelabJson.parseToJsonElement(s)
    private fun stats(d: WidgetData) = d.stats.associate { it.label to it.value }

    @Test
    fun endpoints_perType() {
        assertEquals(listOf("CountV2", "SessionsV2"), WidgetMappers.endpointsFor("jellyfin", 2))
        assertEquals(listOf("Count", "Sessions"), WidgetMappers.endpointsFor("jellyfin", null))
        assertEquals(listOf("wanted/missing", "queue", "series"), WidgetMappers.endpointsFor("sonarr", null))
        assertEquals(listOf("request/count"), WidgetMappers.endpointsFor("jellyseerr", null))
        assertTrue(WidgetMappers.endpointsFor("uptimekuma", null).isEmpty())
        assertTrue(WidgetMappers.isSupported("qbittorrent"))
    }

    @Test
    fun errorMessage_detectsTopLevelError() {
        assertEquals("Unsupported service endpoint", WidgetMappers.errorMessage(j("""{"error":"Unsupported service endpoint"}""")))
        assertEquals("boom", WidgetMappers.errorMessage(j("""{"error":{"message":"boom"}}""")))
        assertNull(WidgetMappers.errorMessage(j("""{"totalRecords":3}""")))
        assertNull(WidgetMappers.errorMessage(j("""[{"error":"nested is fine"}]""")))
    }

    @Test
    fun map_throwsOnErrorResponse() {
        try {
            WidgetMappers.map("sonarr", null, mapOf(
                "wanted/missing" to j("""{"error":"Unsupported service endpoint"}"""),
                "queue" to j("""{"totalRecords":0,"records":[]}"""),
                "series" to j("[]"),
            ))
            fail("expected WidgetException")
        } catch (e: WidgetException) {
            assertEquals("Unsupported service endpoint", e.message)
        }
    }

    @Test
    fun unknownType_rendersWithoutStats() {
        val d = WidgetMappers.map("customapi", null, emptyMap())
        assertTrue(d.stats.isEmpty())
    }

    @Test
    fun jellyfin_countsAndNowPlaying() {
        val count = j("""{"MovieCount":412,"SeriesCount":63,"EpisodeCount":2154,"SongCount":8,"AlbumCount":2,"ArtistCount":1}""")
        val sessions = j("""
            [
              {"UserName":"brian","DeviceName":"Living Room TV",
               "NowPlayingItem":{"Name":"Pilot","SeriesName":"Severance","Type":"Episode","ParentIndexNumber":1,"IndexNumber":1,"RunTimeTicks":36000000000},
               "PlayState":{"PositionTicks":9000000000,"IsPaused":true}},
              {"UserName":"idle","DeviceName":"Phone"},
              {"UserName":"guest","DeviceName":"Web","NowPlayingItem":{"Name":"Dune","Type":"Movie","RunTimeTicks":0},"PlayState":{}}
            ]
        """)
        val d = WidgetMappers.map("jellyfin", 2, mapOf("CountV2" to count, "SessionsV2" to sessions))
        assertEquals(mapOf("Movies" to "412", "Series" to "63", "Episodes" to "2.2k", "Songs" to "8"), stats(d))
        assertEquals(2, d.sessions.size)

        val ep = d.sessions[0]
        assertEquals("brian", ep.user)
        assertEquals("Severance", ep.title)
        assertEquals("S01E01 · Pilot", ep.subtitle)
        assertEquals("Living Room TV", ep.device)
        assertEquals(0.25f, ep.progress, 0.0001f)
        assertTrue(ep.isPaused)
        assertEquals(900L, ep.positionSeconds)
        assertEquals(3600L, ep.runtimeSeconds)

        val movie = d.sessions[1]
        assertEquals("Dune", movie.title)
        assertNull(movie.subtitle)
        assertEquals(0f, movie.progress, 0f)
    }

    @Test
    fun jellyfin_v1UsesCountAndSessions() {
        val d = WidgetMappers.map("jellyfin", null, mapOf(
            "Count" to j("""{"MovieCount":1,"SeriesCount":2,"EpisodeCount":3,"SongCount":4}"""),
            "Sessions" to j("[]"),
        ))
        assertEquals("1", stats(d)["Movies"])
        assertEquals("4", stats(d)["Songs"])
    }

    @Test
    fun sonarr_wantedQueuedSeries() {
        val d = WidgetMappers.map("sonarr", null, mapOf(
            "wanted/missing" to j("""{"totalRecords":17}"""),
            "queue" to j("""{"totalRecords":2,"records":[
                {"title":"Show.S01E01.1080p","status":"downloading","size":1000,"sizeleft":250,"estimatedCompletionTime":"2026-01-01T12:00:00Z"},
                {"title":"Show.S01E02.1080p","status":"queued","size":0,"sizeleft":0}
            ]}"""),
            "series" to j("""[{"title":"a"},{"title":"b"},{"title":"c"}]"""),
        ))
        assertEquals(mapOf("Wanted" to "17", "Queued" to "2", "Series" to "3"), stats(d))
        assertEquals(2, d.queue.size)
        assertEquals(0.75f, d.queue[0].progress!!, 0.0001f)
        assertEquals("2026-01-01T12:00:00Z", d.queue[0].estimatedCompletion)
        assertNull(d.queue[1].progress)
        assertEquals(2L, d.queueTotal)
    }

    @Test
    fun radarr_statsAndMissingList() {
        val d = WidgetMappers.map("radarr", null, mapOf(
            "movie" to j("""{"wanted":3,"missing":2,"have":120,"all":[
                {"title":"Zeta","year":2024,"hasFile":false,"monitored":true,"isAvailable":true},
                {"title":"Alpha","year":2027,"hasFile":false,"monitored":true,"isAvailable":false},
                {"title":"Owned","hasFile":true,"monitored":true},
                {"title":"Unmonitored","hasFile":false,"monitored":false}
            ]}"""),
            "queue/status" to j("""{"totalCount":4}"""),
        ))
        assertEquals(mapOf("Wanted" to "3", "Missing" to "2", "Queued" to "4", "Movies" to "120"), stats(d))
        val rows = d.sections.single().rows
        assertEquals(listOf("Alpha", "Zeta"), rows.map { it.title })
        assertEquals("2027 · Not yet released", rows[0].subtitle)
    }

    @Test
    fun radarr_trimmedAllListsLibrary() {
        // What Homepage actually returns today: `all` is trimmed to {title, id}.
        val d = WidgetMappers.map("radarr", null, mapOf(
            "movie" to j("""{"wanted":14,"have":19,"missing":18,"all":[{"title":"Project Hail Mary","id":2},{"title":"Avengers: Doomsday","id":3}]}"""),
            "queue/status" to j("""{"totalCount":3,"count":0,"unknownCount":3}"""),
        ))
        assertEquals(mapOf("Wanted" to "14", "Missing" to "18", "Queued" to "3", "Movies" to "19"), stats(d))
        val section = d.sections.single()
        assertEquals("Movies in Radarr (2)", section.title)
        assertEquals(listOf("Avengers: Doomsday", "Project Hail Mary"), section.rows.map { it.title })
        assertNull(section.rows[0].subtitle)
    }

    @Test
    fun lidarr_wantedQueuedArtists() {
        val d = WidgetMappers.map("lidarr", null, mapOf(
            "wanted/missing" to j("""{"totalRecords":5,"records":[{"title":"OK Computer","releaseDate":"1997-05-21T00:00:00Z","artist":{"artistName":"Radiohead"}}]}"""),
            "queue/status" to j("""{"totalCount":1}"""),
            "artist" to j("""[{},{}]"""),
        ))
        assertEquals(mapOf("Wanted" to "5", "Queued" to "1", "Artists" to "2"), stats(d))
        assertEquals("Radiohead · 1997", d.sections.single().rows.single().subtitle)
    }

    @Test
    fun prowlarr_sumsAndEnabledCount() {
        val d = WidgetMappers.map("prowlarr", null, mapOf(
            "indexer" to j("""[{"id":1,"name":"A","enable":true},{"id":2,"name":"B","enable":false},{"id":3,"name":"C","enable":true}]"""),
            "indexerstats" to j("""{"indexers":[
                {"indexerName":"A","numberOfGrabs":10,"numberOfQueries":1500,"numberOfFailedGrabs":1,"numberOfFailedQueries":2},
                {"indexerName":"C","numberOfGrabs":5,"numberOfQueries":500,"numberOfFailedGrabs":0,"numberOfFailedQueries":3}
            ]}"""),
        ))
        assertEquals(mapOf("Indexers" to "2", "Grabs" to "15", "Queries" to "2k", "Fails" to "6"), stats(d))
        val table = d.sections.single()
        assertEquals(listOf("Grabs", "Queries", "Fails"), table.headers)
        assertEquals(listOf("10", "1.5k", "3"), table.rows.first().cells)
    }

    @Test
    fun seerr_usesCompletedWhenAvailableIsZero() {
        val sample = """{"total":40,"movie":25,"tv":15,"pending":2,"approved":5,"declined":1,"processing":3,"available":0,"completed":29}"""
        val d = WidgetMappers.map("seerr", null, mapOf("request/count" to j(sample)))
        assertEquals(mapOf("Pending" to "2", "Processing" to "3", "Approved" to "5", "Available" to "29"), stats(d))
        assertEquals(2L, d.pendingRequests)

        val d2 = WidgetMappers.map("overseerr", null, mapOf("request/count" to j("""{"pending":0,"available":7,"completed":9}""")))
        assertEquals("7", stats(d2)["Available"])
    }

    @Test
    fun qbittorrent_leechSeedAndSpeeds() {
        val d = WidgetMappers.map("qbittorrent", null, mapOf("torrents" to j("""[
            {"hash":"h1","name":"a","state":"downloading","dlspeed":4508876,"upspeed":0,"progress":0.5,"size":100,"eta":60,"category":"tv"},
            {"hash":"h2","name":"b","state":"stalledDL","dlspeed":0,"upspeed":0,"progress":0.1,"size":100,"eta":8640000},
            {"hash":"h3","name":"c","state":"uploading","dlspeed":0,"upspeed":524288,"progress":1,"size":100,"eta":0},
            {"hash":"h4","name":"d","state":"pausedUP","dlspeed":0,"upspeed":0,"progress":1,"size":100,"eta":0}
        ]""")))
        assertEquals(mapOf("Leech" to "2", "Seed" to "1", "Download" to "4.3 MB/s", "Upload" to "512 KB/s"), stats(d))
        assertEquals(4, d.torrents.size)
        assertEquals(listOf("h1", "h2", "h3"), d.torrents.filter { it.isActive }.map { it.hash })
        assertTrue(d.torrents.single { it.hash == "h4" }.isComplete)
    }

    @Test
    fun resources_memoryUsesTotalMinusAvailable() {
        val m = WidgetMappers.memory(j("""{"memory":{"total":16000000000,"available":12000000000,"used":15000000000,"free":500000000}}"""))!!
        assertEquals(4_000_000_000.0, m.usedBytes, 0.0)
        assertEquals(25.0, m.percent, 0.0001)
    }

    @Test
    fun resources_cpuAndDisk() {
        val cpu = WidgetMappers.cpu(j("""{"cpu":{"usage":13.5,"load":0.45}}"""))!!
        assertEquals(13.5, cpu.usagePercent, 0.0)
        val disk = WidgetMappers.disk(
            j("""{"drive":{"size":3078632652800,"used":546771058688,"available":2531861594112,"use":17.76,"mount":"/mnt/media"}}"""),
            "/mnt/media",
        )!!
        assertEquals(17.76, disk.usePercent, 0.0)
        assertEquals("2.8 TB", Format.bytes(disk.sizeBytes))
    }

    @Test
    fun resources_diskTargetStringOrList() {
        val single = j("""{"cpu":true,"memory":true,"disk":"/mnt/media"}""") as JsonObject
        assertEquals(listOf("/mnt/media"), WidgetMappers.diskTargets(single))
        assertTrue(WidgetMappers.optionEnabled(single, "cpu"))
        val list = j("""{"disk":["/","/mnt/media"]}""") as JsonObject
        assertEquals(listOf("/", "/mnt/media"), WidgetMappers.diskTargets(list))
        assertTrue(WidgetMappers.diskTargets(null).isEmpty())
        assertTrue(!WidgetMappers.optionEnabled(list, "cpu"))
    }

    @Test
    fun healthCheck_shapes() {
        assertEquals(HealthCheck(true, 12.5), WidgetMappers.healthCheck(true, j("""{"alive":true,"latency":12.5}""")))
        assertEquals(HealthCheck(true, 80.0), WidgetMappers.healthCheck(true, j("""{"status":200,"latency":80}""")))
        assertEquals(false, WidgetMappers.healthCheck(true, j("""{"status":503,"latency":80}""")).up)
        assertEquals(false, WidgetMappers.healthCheck(false, j("""{"error":"no ping"}""")).up)
    }

    @Test
    fun dockerLevels() {
        assertEquals(StatusLevel.OK, dockerLevel("running", "healthy"))
        assertEquals(StatusLevel.OK, dockerLevel("running", null))
        assertEquals(StatusLevel.WARN, dockerLevel("running", "unhealthy"))
        assertEquals(StatusLevel.WARN, dockerLevel("running", "starting"))
        assertEquals(StatusLevel.ERROR, dockerLevel("exited", null))
        assertEquals(StatusLevel.ERROR, dockerLevel("not found", null))
    }
}
