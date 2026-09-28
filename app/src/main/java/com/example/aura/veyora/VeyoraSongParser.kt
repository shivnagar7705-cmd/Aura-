package com.example.aura.veyora

import android.util.Log
import com.example.aura.model.Song
import com.example.aura.model.SongStatus
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

object VeyoraSongParser {

    private const val TAG = "VeyoraSongParser"

    /**
     * Parses a raw JSON response (either JSONArray or JSONObject containing songs array)
     * from Veyora Cloud into a list of domain [Song] instances.
     */
    fun parseSongList(jsonString: String): List<Song> {
        val trimmed = jsonString.trim()
        val songs = mutableListOf<Song>()

        try {
            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    parseSong(item, i)?.let { songs.add(it) }
                }
            } else if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)
                val array = root.optJSONArray("songs")
                    ?: root.optJSONArray("data")
                    ?: root.optJSONArray("tracks")
                    ?: root.optJSONArray("items")
                    ?: root.optJSONArray("results")

                if (array != null) {
                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        parseSong(item, i)?.let { songs.add(it) }
                    }
                } else if (root.has("song") && root.optJSONObject("song") != null) {
                    parseSong(root.getJSONObject("song"), 0)?.let { songs.add(it) }
                } else if (root.has("title") || root.has("audio_url") || root.has("audioUrl")) {
                    // Single song object
                    parseSong(root, 0)?.let { songs.add(it) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Veyora Cloud JSON: ${e.message}", e)
        }

        return songs
    }

    private fun parseSong(obj: JSONObject, index: Int): Song? {
        val id = obj.optString("id").ifBlank {
            obj.optString("song_id").ifBlank {
                obj.optString("uuid").ifBlank { "veyora_${UUID.randomUUID()}" }
            }
        }

        val title = obj.optString("title").ifBlank {
            obj.optString("name").ifBlank {
                obj.optString("song_name").ifBlank {
                    obj.optString("track_name").ifBlank { "Track #${index + 1}" }
                }
            }
        }

        val artist = obj.optString("artist").ifBlank {
            obj.optString("artist_name").ifBlank {
                obj.optString("author").ifBlank {
                    obj.optString("creator").ifBlank { "Veyora Artist" }
                }
            }
        }

        val album = obj.optString("album").ifBlank {
            obj.optString("album_name").ifBlank { "Single" }
        }

        val genre = obj.optString("genre").ifBlank {
            obj.optString("category").ifBlank { "Electronic" }
        }

        // Real Veyora Cloud DB field is "audio_url" (or camelCase "audioUrl")
        val rawAudio = obj.optString("audio_url").ifBlank {
            obj.optString("audioUrl").ifBlank {
                obj.optString("url").ifBlank {
                    obj.optString("file_url").ifBlank {
                        obj.optString("media_url").ifBlank {
                            obj.optString("stream_url").ifBlank { "" }
                        }
                    }
                }
            }
        }

        // Real Veyora Cloud DB field is "cover_url" (or camelCase "coverUrl")
        val rawCover = obj.optString("cover_url").ifBlank {
            obj.optString("coverUrl").ifBlank {
                obj.optString("image_url").ifBlank {
                    obj.optString("artwork_url").ifBlank {
                        obj.optString("cover_image").ifBlank {
                            obj.optString("thumbnail").ifBlank { "" }
                        }
                    }
                }
            }
        }

        val baseUrl = VeyoraConfig.getBaseUrl().trimEnd('/')

        val normalizedAudio = when {
            rawAudio.isBlank() -> ""
            rawAudio.startsWith("http://") || rawAudio.startsWith("https://") -> rawAudio
            rawAudio.startsWith("/") -> "$baseUrl$rawAudio"
            else -> "$baseUrl/$rawAudio"
        }

        val normalizedCover = when {
            rawCover.isBlank() -> ""
            rawCover.startsWith("http://") || rawCover.startsWith("https://") -> rawCover
            rawCover.startsWith("/") -> "$baseUrl$rawCover"
            else -> "$baseUrl/$rawCover"
        }

        // Songs without playable audio cannot be streamed
        if (normalizedAudio.isBlank()) {
            Log.w(TAG, "Skipping song '$title' because audio_url is empty")
            return null
        }

        val rawDuration = obj.optLong("duration_ms", 0L).let {
            if (it > 0) it else obj.optLong("duration", 0L)
        }
        val durationMs = when {
            rawDuration in 1..999 -> rawDuration * 1000
            rawDuration > 0 -> rawDuration
            else -> 210000L // Default duration: 3 mins 30 secs
        }

        val lyrics = obj.optString("lyrics").takeIf { it.isNotBlank() }
        val description = obj.optString("description").ifBlank { "Streamed from Veyora Cloud" }
        val releaseDate = obj.optString("release_date").ifBlank { "2026" }

        val statusStr = obj.optString("status").uppercase()
        val status = when {
            statusStr == "DRAFT" -> SongStatus.DRAFT
            statusStr == "UNPUBLISHED" -> SongStatus.UNPUBLISHED
            else -> SongStatus.PUBLISHED
        }

        val createdAtEpoch = try {
            val rawCreated = obj.optString("created_at")
            if (rawCreated.isNotBlank()) {
                Instant.parse(rawCreated).toEpochMilli()
            } else {
                System.currentTimeMillis()
            }
        } catch (_: Exception) {
            obj.optLong("created_at", System.currentTimeMillis())
        }

        return Song(
            id = id,
            title = title,
            artist = artist,
            artistId = obj.optString("artist_id", ""),
            album = album,
            albumId = obj.optString("album_id", ""),
            genre = genre,
            releaseDate = releaseDate,
            description = description,
            lyrics = lyrics,
            durationMs = durationMs,
            audioUrl = normalizedAudio,
            coverUrl = normalizedCover,
            downloadPermitted = obj.optBoolean("download_permitted", true),
            copyrightNotice = obj.optString("copyright_notice", "© Veyora Cloud • Test App 6"),
            status = status,
            playsCount = obj.optLong("plays_count", 0L),
            likesCount = obj.optLong("likes_count", 0L),
            isFeatured = obj.optBoolean("is_featured", false),
            isTrending = obj.optBoolean("is_trending", true),
            isNewRelease = obj.optBoolean("is_new_release", true),
            isRecommended = obj.optBoolean("is_recommended", true),
            createdAt = createdAtEpoch,
            updatedAt = System.currentTimeMillis()
        )
    }
}
