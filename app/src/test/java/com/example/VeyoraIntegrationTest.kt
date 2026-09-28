package com.example

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
        assertTrue(VeyoraConfig.getBaseUrl().contains("c023f1e9-3baa-468a-9a92-4343182d94fb"))
    }

    @Test
    fun veyoraSongParser_parsesPostgrestJsonArrayCorrectly() {
        val sampleJson = """
            [
              {
                "id": "veyora_track_1",
                "title": "Quantum Echoes",
                "artist": "Nova Pulse",
                "album": "Celestial Drift",
                "genre": "Synthwave",
                "audio_url": "https://cdn.veyora.cloud/audio/quantum_echoes.mp3",
                "cover_url": "https://cdn.veyora.cloud/images/quantum_echoes.jpg",
                "duration_ms": 215000,
                "status": "PUBLISHED"
              },
              {
                "id": "veyora_track_2",
                "name": "Astral Breeze",
                "artist_name": "Lyra V",
                "file_url": "https://cdn.veyora.cloud/audio/astral_breeze.wav",
                "image_url": "https://cdn.veyora.cloud/images/astral_breeze.png",
                "duration": 190
              }
            ]
        """.trimIndent()

        val songs = VeyoraSongParser.parseSongList(sampleJson)
        assertEquals(2, songs.size)

        val first = songs[0]
        assertEquals("veyora_track_1", first.id)
        assertEquals("Quantum Echoes", first.title)
        assertEquals("Nova Pulse", first.artist)
        assertEquals("Celestial Drift", first.album)
        assertEquals("Synthwave", first.genre)
        assertEquals("https://cdn.veyora.cloud/audio/quantum_echoes.mp3", first.audioUrl)
        assertEquals("https://cdn.veyora.cloud/images/quantum_echoes.jpg", first.coverUrl)
        assertEquals(215000L, first.durationMs)

        val second = songs[1]
        assertEquals("veyora_track_2", second.id)
        assertEquals("Astral Breeze", second.title)
        assertEquals("Lyra V", second.artist)
        assertEquals("https://cdn.veyora.cloud/audio/astral_breeze.wav", second.audioUrl)
        assertEquals("https://cdn.veyora.cloud/images/astral_breeze.png", second.coverUrl)
        assertEquals(190000L, second.durationMs) // converted seconds to ms
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
}
