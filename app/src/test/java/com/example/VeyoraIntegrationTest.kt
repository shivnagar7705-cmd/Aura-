package com.example

import com.example.aura.model.SongStatus
import com.example.aura.veyora.VeyoraConfig
import com.example.aura.veyora.VeyoraSongParser
import org.junit.Assert.*
import org.junit.Test

class VeyoraIntegrationTest {

    @Test
    fun veyoraConfig_hasExpectedProjectIdAndName() {
        assertEquals("c023f1e9-3baa-468a-9a92-4343182d94fb", VeyoraConfig.DEFAULT_PROJECT_ID)
        assertEquals("Test App 6", VeyoraConfig.PROJECT_NAME)
        assertEquals("c023f1e9-3baa-468a-9a92-4343182d94fb", VeyoraConfig.getProjectId())
        assertTrue(VeyoraConfig.getBaseUrl().contains("veyora-cloud.onrender.com"))
    }

    @Test
    fun veyoraSongParser_parsesRealVeyoraCloudJsonCorrectly() {
        val sampleJson = """
            [
              {
                "id": "4ecc6046-e649-45fa-b7ef-7bc3a73923a0",
                "created_at": "2026-09-28T13:31:18.038343+00:00",
                "project_id": "c023f1e9-3baa-468a-9a92-4343182d94fb",
                "title": "Midnight Reverie",
                "artist": "Aura Echo",
                "audio_url": "https://veyora-cloud.onrender.com/uploads/c023f1e9/midnight.mp3",
                "cover_url": "https://veyora-cloud.onrender.com/uploads/c023f1e9/midnight_cover.jpg",
                "status": "published"
              },
              {
                "id": "7bdd3125-d721-42cb-98da-8cc4e82012c4",
                "created_at": "2026-09-27T10:15:00.000000+00:00",
                "project_id": "c023f1e9-3baa-468a-9a92-4343182d94fb",
                "title": "Solar Flares",
                "artist": "Nova Pulse",
                "audio_url": "https://veyora-cloud.onrender.com/uploads/c023f1e9/solar.mp3",
                "cover_url": "https://veyora-cloud.onrender.com/uploads/c023f1e9/solar.png",
                "status": "published"
              }
            ]
        """.trimIndent()

        val songs = VeyoraSongParser.parseSongList(sampleJson)
        assertEquals(2, songs.size)

        val first = songs[0]
        assertEquals("4ecc6046-e649-45fa-b7ef-7bc3a73923a0", first.id)
        assertEquals("Midnight Reverie", first.title)
        assertEquals("Aura Echo", first.artist)
        assertEquals("https://veyora-cloud.onrender.com/uploads/c023f1e9/midnight.mp3", first.audioUrl)
        assertEquals("https://veyora-cloud.onrender.com/uploads/c023f1e9/midnight_cover.jpg", first.coverUrl)
        assertEquals(SongStatus.PUBLISHED, first.status)
        assertTrue(first.durationMs > 0)
        assertTrue(first.createdAt > 0)

        val second = songs[1]
        assertEquals("7bdd3125-d721-42cb-98da-8cc4e82012c4", second.id)
        assertEquals("Solar Flares", second.title)
        assertEquals("Nova Pulse", second.artist)
        assertEquals("https://veyora-cloud.onrender.com/uploads/c023f1e9/solar.mp3", second.audioUrl)
        assertEquals("https://veyora-cloud.onrender.com/uploads/c023f1e9/solar.png", second.coverUrl)
        assertEquals(SongStatus.PUBLISHED, second.status)
    }

    @Test
    fun veyoraSongParser_filtersOutItemsWithoutAudioUrl() {
        val json = """
            [
              {
                "id": "invalid_track",
                "title": "Silent Track",
                "artist": "Unknown"
              }
            ]
        """.trimIndent()

        val songs = VeyoraSongParser.parseSongList(json)
        assertEquals(0, songs.size)
    }

    @Test
    fun veyoraConfig_demoCredentialsAreValid() {
        assertEquals("c74ba593-c43e-4c7a-b7af-b191a6038876", VeyoraConfig.DEMO_PROJECT_ID)
        assertEquals("vk_a72cbfe5c912f4d273d0f2c4cf989c5e0fd4", VeyoraConfig.DEMO_API_KEY)
    }
}
