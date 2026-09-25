package com.example.aura.model

enum class SongStatus {
    PUBLISHED,
    DRAFT,
    UNPUBLISHED
}

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String = "",
    val album: String = "",
    val albumId: String = "",
    val genre: String = "Electronic",
    val releaseDate: String = "2026",
    val description: String = "",
    val lyrics: String? = null,
    val durationMs: Long = 210000L,
    val audioUrl: String,
    val coverUrl: String,
    val downloadPermitted: Boolean = true,
    val copyrightNotice: String = "Licensed by AURA Music Protocol",
    val status: SongStatus = SongStatus.PUBLISHED,
    val playsCount: Long = 0L,
    val likesCount: Long = 0L,
    val isFeatured: Boolean = false,
    val isTrending: Boolean = false,
    val isNewRelease: Boolean = false,
    val isRecommended: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class Artist(
    val id: String,
    val name: String,
    val imageUrl: String,
    val bio: String,
    val genre: String,
    val monthlyListeners: Long = 250000L
)

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String,
    val coverUrl: String,
    val releaseYear: String,
    val genre: String,
    val trackIds: List<String> = emptyList()
)

data class Playlist(
    val id: String,
    val title: String,
    val description: String,
    val coverUrl: String,
    val createdBy: String,
    val creatorName: String,
    val isFeatured: Boolean = false,
    val songIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class HomeBanner(
    val id: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String,
    val tag: String,
    val targetSongId: String? = null
)

data class User(
    val id: String,
    val email: String,
    val username: String,
    val role: String = "USER", // "USER" or "ADMIN"
    val passwordHash: String,
    val profileImageUrl: String = "",
    val isSuspended: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class ContentReport(
    val id: String,
    val songId: String,
    val songTitle: String,
    val reporterEmail: String,
    val reason: String,
    val details: String,
    val type: String, // "REPORT" or "COPYRIGHT"
    val status: String = "PENDING", // "PENDING", "RESOLVED", "DISMISSED"
    val createdAt: Long = System.currentTimeMillis()
)

data class DownloadRecord(
    val id: String,
    val songId: String,
    val localFilePath: String,
    val downloadedAt: Long = System.currentTimeMillis()
)

data class ListeningStats(
    val totalUsers: Long,
    val totalSongs: Long,
    val publishedSongs: Long,
    val unpublishedSongs: Long,
    val totalPlaylists: Long,
    val totalReports: Long,
    val totalStreams: Long
)
