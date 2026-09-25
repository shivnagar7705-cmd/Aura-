package com.aura.studio.model

data class StudioSong(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val artistId: String = "",
    val album: String = "",
    val albumId: String = "",
    val genre: String = "Pop",
    val releaseDate: String = "",
    val description: String = "",
    val lyrics: String = "",
    val durationMs: Long = 0L,
    val audioUrl: String = "",
    val coverUrl: String = "",
    val downloadPermitted: Boolean = true,
    val copyrightNotice: String = "© 2026 Aura Music. All rights reserved.",
    val status: String = "PUBLISHED", // "PUBLISHED" or "UNPUBLISHED" / "DRAFT"
    val playsCount: Long = 0L,
    val likesCount: Long = 0L,
    val isFeatured: Boolean = false,
    val isTrending: Boolean = false,
    val isNewRelease: Boolean = true,
    val isRecommended: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class StudioArtist(
    val id: String = "",
    val name: String = "",
    val imageUrl: String = "",
    val bio: String = "",
    val genre: String = "Pop",
    val monthlyListeners: Long = 0L
)

data class StudioAlbum(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val artistId: String = "",
    val coverUrl: String = "",
    val releaseYear: String = "2026",
    val genre: String = "Pop",
    val trackIds: List<String> = emptyList(),
    val status: String = "PUBLISHED"
)

data class StudioPlaylist(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val coverUrl: String = "",
    val createdBy: String = "admin",
    val creatorName: String = "Aura Editorial",
    val songIds: List<String> = emptyList(),
    val isPublic: Boolean = true,
    val isEditorial: Boolean = true
)

data class StudioUser(
    val id: String = "",
    val email: String = "",
    val username: String = "",
    val role: String = "USER", // "USER" or "ADMIN"
    val isSuspended: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class StudioAdminSession(
    val uid: String,
    val email: String,
    val fullName: String,
    val role: String,
    val token: String
)
