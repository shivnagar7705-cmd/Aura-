package com.example.aura.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aura.backend.AuraSharedBackend
import com.example.aura.model.*
import com.example.aura.player.AuraAudioPlayer
import com.example.aura.ui.components.AuraFullPlayer
import com.example.aura.ui.components.AuraImageLogo
import com.example.aura.ui.components.AuraLogoEmblem
import com.example.aura.ui.components.AuraMiniPlayer
import com.example.aura.ui.screens.*
import com.example.ui.theme.*

enum class AuraScreenTab {
    HOME,
    SEARCH,
    LIBRARY,
    PROFILE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuraApp() {
    val context = LocalContext.current
    val backend = remember { AuraSharedBackend.getInstance(context) }
    val player = remember { AuraAudioPlayer.getInstance(context) }

    // State flows from backend
    val publishedSongs by backend.publishedSongs.collectAsState()
    val allSongs by backend.allSongs.collectAsState()
    val banners by backend.banners.collectAsState()
    val currentUser by backend.currentUser.collectAsState()

    // State flows from player
    val currentSong by player.currentSong.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    val currentPositionMs by player.currentPositionMs.collectAsState()
    val durationMs by player.durationMs.collectAsState()
    val isShuffle by player.isShuffle.collectAsState()
    val repeatMode by player.repeatMode.collectAsState()

    // Navigation & UI State
    var hasCompletedSplash by remember { mutableStateOf(false) }
    var currentTab by remember { mutableStateOf(AuraScreenTab.HOME) }
    var isFullPlayerVisible by remember { mutableStateOf(false) }

    // Detail Screen Navigation States
    var selectedSongDetails by remember { mutableStateOf<Song?>(null) }
    var selectedPlaylistDetails by remember { mutableStateOf<Playlist?>(null) }
    var selectedArtistDetails by remember { mutableStateOf<Artist?>(null) }
    var selectedAlbumDetails by remember { mutableStateOf<Album?>(null) }
    var isSettingsVisible by remember { mutableStateOf(false) }
    var legalDocToShow by remember { mutableStateOf<Pair<String, String>?>(null) } // Title, Type

    // Dialogs
    var songToReport by remember { mutableStateOf<Song?>(null) }
    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    // Derived collections
    val userPlaylists = remember(currentUser, publishedSongs) {
        currentUser?.let { backend.getUserPlaylists(it.id) } ?: emptyList()
    }
    val likedSongs = remember(currentUser, publishedSongs) {
        currentUser?.let { backend.getLikedSongs(it.id) } ?: emptyList()
    }
    val recentlyPlayed = remember(currentUser, publishedSongs) {
        currentUser?.let { backend.getRecentlyPlayed(it.id) } ?: emptyList()
    }
    val downloadedSongs = remember(currentUser, publishedSongs) {
        currentUser?.let { backend.getDownloadedSongs(it.id) } ?: emptyList()
    }
    val artists = remember(publishedSongs) { backend.getAllArtists() }
    val albums = remember(publishedSongs) { backend.getAllAlbums() }

    AuraTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AuraBackground)
        ) {
            if (!hasCompletedSplash) {
                SplashScreen(
                    onAnimationFinished = {
                        hasCompletedSplash = true
                    }
                )
            } else if (currentUser == null) {
                // If not authenticated, display clean Auth screen
                AuthScreen(
                    backend = backend,
                    onAuthSuccess = {
                        Toast.makeText(context, "Welcome to AURA", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                // Main Authenticated Experience
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AuraImageLogo(size = 32.dp, animated = true)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "AURA",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 17.sp,
                                            letterSpacing = 2.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "FUTURE SOUNDS & AI MUSIC",
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.2.sp,
                                            color = AuraTextSecondary
                                        )
                                    }
                                }
                            },
                            actions = {
                                IconButton(onClick = { isSettingsVisible = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = AuraTextSecondary
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = AuraBackground.copy(alpha = 0.95f),
                                titleContentColor = AuraTextPrimary
                            )
                        )
                    },
                    bottomBar = {
                        Column {
                            // Mini Player - sits neatly above bottom navigation bar
                            val progressFraction = if (durationMs > 0) {
                                currentPositionMs.toFloat() / durationMs.toFloat()
                            } else 0f

                            AuraMiniPlayer(
                                song = currentSong,
                                isPlaying = isPlaying,
                                progressFraction = progressFraction,
                                onTogglePlay = { player.togglePlayPause() },
                                onSkipNext = { player.skipNext() },
                                onOpenFullPlayer = { isFullPlayerVisible = true }
                            )

                            // Bottom Navigation (Strictly 4 items: Home, Search, Library, Profile)
                            NavigationBar(
                                containerColor = Color(0xFF0C1022),
                                contentColor = AuraTextPrimary,
                                tonalElevation = 0.dp,
                                modifier = Modifier
                                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                                    .testTag("aura_bottom_navigation")
                            ) {
                                NavigationBarItem(
                                    selected = currentTab == AuraScreenTab.HOME,
                                    onClick = {
                                        currentTab = AuraScreenTab.HOME
                                        selectedSongDetails = null
                                        isSettingsVisible = false
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentTab == AuraScreenTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                            contentDescription = "Home"
                                        )
                                    },
                                    label = { Text("Home", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = AuraCyan,
                                        selectedTextColor = AuraCyan,
                                        unselectedIconColor = AuraTextSecondary,
                                        unselectedTextColor = AuraTextSecondary,
                                        indicatorColor = AuraCyan.copy(alpha = 0.15f)
                                    )
                                )

                                NavigationBarItem(
                                    selected = currentTab == AuraScreenTab.SEARCH,
                                    onClick = {
                                        currentTab = AuraScreenTab.SEARCH
                                        selectedSongDetails = null
                                        isSettingsVisible = false
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Search"
                                        )
                                    },
                                    label = { Text("Search", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = AuraCyan,
                                        selectedTextColor = AuraCyan,
                                        unselectedIconColor = AuraTextSecondary,
                                        unselectedTextColor = AuraTextSecondary,
                                        indicatorColor = AuraCyan.copy(alpha = 0.15f)
                                    )
                                )

                                NavigationBarItem(
                                    selected = currentTab == AuraScreenTab.LIBRARY,
                                    onClick = {
                                        currentTab = AuraScreenTab.LIBRARY
                                        selectedSongDetails = null
                                        isSettingsVisible = false
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentTab == AuraScreenTab.LIBRARY) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic,
                                            contentDescription = "Library"
                                        )
                                    },
                                    label = { Text("Library", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = AuraCyan,
                                        selectedTextColor = AuraCyan,
                                        unselectedIconColor = AuraTextSecondary,
                                        unselectedTextColor = AuraTextSecondary,
                                        indicatorColor = AuraCyan.copy(alpha = 0.15f)
                                    )
                                )

                                NavigationBarItem(
                                    selected = currentTab == AuraScreenTab.PROFILE,
                                    onClick = {
                                        currentTab = AuraScreenTab.PROFILE
                                        selectedSongDetails = null
                                        isSettingsVisible = false
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (currentTab == AuraScreenTab.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                                            contentDescription = "Profile"
                                        )
                                    },
                                    label = { Text("Profile", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = AuraCyan,
                                        selectedTextColor = AuraCyan,
                                        unselectedIconColor = AuraTextSecondary,
                                        unselectedTextColor = AuraTextSecondary,
                                        indicatorColor = AuraCyan.copy(alpha = 0.15f)
                                    )
                                )
                            }
                        }
                    },
                    containerColor = AuraBackground
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            AuraScreenTab.HOME -> {
                                HomeScreen(
                                    currentUser = currentUser,
                                    banners = banners,
                                    publishedSongs = publishedSongs,
                                    recentlyPlayed = recentlyPlayed,
                                    artists = artists,
                                    onSongSelected = { song, queue ->
                                        player.playSong(song, queue)
                                    },
                                    onSongDetailsClick = { song ->
                                        selectedSongDetails = song
                                    },
                                    onArtistClick = { artist ->
                                        selectedArtistDetails = artist
                                    },
                                    currentPlayingSongId = currentSong?.id,
                                    isPlaying = isPlaying,
                                    currentSong = currentSong
                                )
                            }

                            AuraScreenTab.SEARCH -> {
                                SearchScreen(
                                    publishedSongs = publishedSongs,
                                    artists = artists,
                                    albums = albums,
                                    playlists = userPlaylists + backend.getFeaturedPlaylists(),
                                    onSongSelected = { song, queue ->
                                        player.playSong(song, queue)
                                    },
                                    onSongDetailsClick = { song ->
                                        selectedSongDetails = song
                                    },
                                    onArtistClick = { artist ->
                                        selectedArtistDetails = artist
                                    },
                                    onAlbumClick = { album ->
                                        selectedAlbumDetails = album
                                    },
                                    onPlaylistClick = { playlist ->
                                        selectedPlaylistDetails = playlist
                                    }
                                )
                            }

                            AuraScreenTab.LIBRARY -> {
                                LibraryScreen(
                                    likedSongsCount = likedSongs.size,
                                    playlistsCount = userPlaylists.size,
                                    albumsCount = albums.size,
                                    artistsCount = artists.size,
                                    downloadsCount = downloadedSongs.size,
                                    recentlyPlayedCount = recentlyPlayed.size,
                                    onOpenLikedSongs = {
                                        selectedPlaylistDetails = Playlist(
                                            id = "pl_liked",
                                            title = "Liked Songs",
                                            description = "Your personal collection of loved tracks",
                                            coverUrl = "",
                                            createdBy = currentUser?.id ?: "",
                                            creatorName = currentUser?.username ?: "You",
                                            isFeatured = false,
                                            songIds = likedSongs.map { it.id },
                                            createdAt = System.currentTimeMillis()
                                        )
                                    },
                                    onOpenPlaylists = {
                                        showCreatePlaylistDialog = true
                                    },
                                    onOpenAlbums = {
                                        if (albums.isNotEmpty()) selectedAlbumDetails = albums.first()
                                    },
                                    onOpenArtists = {
                                        if (artists.isNotEmpty()) selectedArtistDetails = artists.first()
                                    },
                                    onOpenDownloads = {
                                        selectedPlaylistDetails = Playlist(
                                            id = "pl_downloads",
                                            title = "Downloaded Tracks",
                                            description = "Offline audio tracks saved on device",
                                            coverUrl = "",
                                            createdBy = currentUser?.id ?: "",
                                            creatorName = "Offline Cache",
                                            isFeatured = false,
                                            songIds = downloadedSongs.map { it.id },
                                            createdAt = System.currentTimeMillis()
                                        )
                                    },
                                    onOpenRecentlyPlayed = {
                                        selectedPlaylistDetails = Playlist(
                                            id = "pl_history",
                                            title = "Recently Played",
                                            description = "Recent playback history on AURA",
                                            coverUrl = "",
                                            createdBy = currentUser?.id ?: "",
                                            creatorName = "History",
                                            isFeatured = false,
                                            songIds = recentlyPlayed.map { it.id },
                                            createdAt = System.currentTimeMillis()
                                        )
                                    }
                                )
                            }

                            AuraScreenTab.PROFILE -> {
                                ProfileScreen(
                                    user = currentUser,
                                    likedCount = likedSongs.size,
                                    playlistCount = userPlaylists.size,
                                    downloadsCount = downloadedSongs.size,
                                    onOpenSettings = { isSettingsVisible = true },
                                    onOpenEditProfile = {
                                        Toast.makeText(context, "Profile editing available in Settings", Toast.LENGTH_SHORT).show()
                                    },
                                    onLogout = {
                                        backend.logoutUser()
                                        Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }

                        // Overlay Sub-screens
                        if (selectedSongDetails != null) {
                            SongDetailsScreen(
                                song = selectedSongDetails!!,
                                backend = backend,
                                userId = currentUser?.id,
                                onBack = { selectedSongDetails = null },
                                onPlay = {
                                    player.playSong(selectedSongDetails!!, publishedSongs)
                                },
                                onAddToPlaylist = {
                                    songToAddToPlaylist = selectedSongDetails
                                },
                                onReportContent = { song ->
                                    songToReport = song
                                }
                            )
                        }

                        if (selectedPlaylistDetails != null) {
                            PlaylistDetailsScreen(
                                playlist = selectedPlaylistDetails!!,
                                allSongs = allSongs,
                                backend = backend,
                                userId = currentUser?.id,
                                onBack = { selectedPlaylistDetails = null },
                                onPlayAll = { songs ->
                                    player.playAll(songs)
                                },
                                onSongClick = { song ->
                                    selectedSongDetails = song
                                },
                                onPlaylistDeleted = {
                                    selectedPlaylistDetails = null
                                }
                            )
                        }

                        if (selectedArtistDetails != null) {
                            ArtistDetailsScreen(
                                artist = selectedArtistDetails!!,
                                allSongs = publishedSongs,
                                onBack = { selectedArtistDetails = null },
                                onPlayAll = { songs ->
                                    player.playAll(songs)
                                },
                                onSongClick = { song ->
                                    selectedSongDetails = song
                                }
                            )
                        }

                        if (selectedAlbumDetails != null) {
                            AlbumDetailsScreen(
                                album = selectedAlbumDetails!!,
                                allSongs = publishedSongs,
                                onBack = { selectedAlbumDetails = null },
                                onPlayAll = { songs ->
                                    player.playAll(songs)
                                },
                                onSongClick = { song ->
                                    selectedSongDetails = song
                                }
                            )
                        }

                        if (isSettingsVisible) {
                            SettingsScreen(
                                user = currentUser,
                                backend = backend,
                                player = player,
                                onBack = { isSettingsVisible = false },
                                onOpenLegalDoc = { title, type ->
                                    legalDocToShow = Pair(title, type)
                                },
                                onOpenReportDialog = {
                                    songToReport = currentSong
                                },
                                onLogout = {
                                    isSettingsVisible = false
                                    backend.logoutUser()
                                },
                                onAccountDeleted = {
                                    isSettingsVisible = false
                                }
                            )
                        }

                        if (legalDocToShow != null) {
                            LegalDocScreen(
                                title = legalDocToShow!!.first,
                                type = legalDocToShow!!.second,
                                onBack = { legalDocToShow = null }
                            )
                        }
                    }
                }

                // Full Screen Player Modal (can be opened from anywhere by tapping mini player)
                val isLiked = remember(currentSong, currentUser) {
                    if (currentSong != null && currentUser != null) {
                        backend.isSongLiked(currentUser!!.id, currentSong!!.id)
                    } else false
                }

                AuraFullPlayer(
                    visible = isFullPlayerVisible,
                    song = currentSong,
                    isPlaying = isPlaying,
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    isShuffle = isShuffle,
                    repeatMode = repeatMode,
                    isLiked = isLiked,
                    onClose = { isFullPlayerVisible = false },
                    onTogglePlay = { player.togglePlayPause() },
                    onSeek = { targetMs -> player.seekTo(targetMs) },
                    onSkipNext = { player.skipNext() },
                    onSkipPrevious = { player.skipPrevious() },
                    onToggleShuffle = { player.toggleShuffle() },
                    onCycleRepeat = { player.cycleRepeatMode() },
                    onToggleLike = {
                        if (currentSong != null && currentUser != null) {
                            backend.toggleLike(currentUser!!.id, currentSong!!.id)
                        }
                    },
                    onAddToPlaylist = {
                        songToAddToPlaylist = currentSong
                    },
                    onDownload = {
                        if (currentSong != null && currentUser != null) {
                            val res = backend.downloadSong(currentUser!!.id, currentSong!!)
                            res.onSuccess {
                                Toast.makeText(context, "Track saved for offline listening", Toast.LENGTH_SHORT).show()
                            }.onFailure {
                                Toast.makeText(context, it.message ?: "Download failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )

                // Dialog: Add to Playlist
                if (songToAddToPlaylist != null) {
                    AlertDialog(
                        onDismissRequest = { songToAddToPlaylist = null },
                        title = { Text("Add to Playlist", color = AuraTextPrimary) },
                        text = {
                            Column {
                                if (userPlaylists.isEmpty()) {
                                    Text("No playlists yet. Create your first playlist below!", color = AuraTextSecondary)
                                } else {
                                    LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                                        items(userPlaylists) { pl ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        backend.addSongToPlaylist(pl.id, songToAddToPlaylist!!.id)
                                                        Toast.makeText(context, "Added to ${pl.title}", Toast.LENGTH_SHORT).show()
                                                        songToAddToPlaylist = null
                                                    }
                                                    .padding(vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = AuraCyan)
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(pl.title, color = AuraTextPrimary, fontSize = 14.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showCreatePlaylistDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AuraCyan, contentColor = Color.Black)
                            ) {
                                Text("New Playlist")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { songToAddToPlaylist = null }) {
                                Text("Close", color = AuraTextMuted)
                            }
                        },
                        containerColor = AuraSurfaceVariant
                    )
                }

                // Dialog: Create Playlist
                if (showCreatePlaylistDialog) {
                    var plTitle by remember { mutableStateOf("") }
                    var plDesc by remember { mutableStateOf("") }

                    AlertDialog(
                        onDismissRequest = { showCreatePlaylistDialog = false },
                        title = { Text("New Playlist", color = AuraTextPrimary) },
                        text = {
                            Column {
                                OutlinedTextField(
                                    value = plTitle,
                                    onValueChange = { plTitle = it },
                                    label = { Text("Playlist Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = plDesc,
                                    onValueChange = { plDesc = it },
                                    label = { Text("Description (Optional)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (plTitle.isNotBlank() && currentUser != null) {
                                        val newPl = backend.createPlaylist(
                                            userId = currentUser!!.id,
                                            userName = currentUser!!.username,
                                            title = plTitle,
                                            description = plDesc,
                                            coverUrl = ""
                                        )
                                        if (songToAddToPlaylist != null) {
                                            backend.addSongToPlaylist(newPl.id, songToAddToPlaylist!!.id)
                                            songToAddToPlaylist = null
                                        }
                                        Toast.makeText(context, "Playlist created", Toast.LENGTH_SHORT).show()
                                        showCreatePlaylistDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AuraCyan, contentColor = Color.Black)
                            ) {
                                Text("Create")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCreatePlaylistDialog = false }) {
                                Text("Cancel", color = AuraTextMuted)
                            }
                        },
                        containerColor = AuraSurfaceVariant
                    )
                }

                // Dialog: Content Moderation & Copyright Report
                if (songToReport != null) {
                    ContentReportDialog(
                        song = songToReport,
                        backend = backend,
                        userEmail = currentUser?.email,
                        onDismiss = { songToReport = null }
                    )
                }
            }
        }
    }
}
