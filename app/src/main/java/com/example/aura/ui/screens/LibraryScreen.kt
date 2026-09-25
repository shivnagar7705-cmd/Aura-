package com.example.aura.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aura.model.Song
import com.example.ui.theme.*

@Composable
fun LibraryScreen(
    likedSongsCount: Int,
    playlistsCount: Int,
    albumsCount: Int,
    artistsCount: Int,
    downloadsCount: Int,
    recentlyPlayedCount: Int,
    onOpenLikedSongs: () -> Unit,
    onOpenPlaylists: () -> Unit,
    onOpenAlbums: () -> Unit,
    onOpenArtists: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenRecentlyPlayed: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraBackground)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .testTag("library_screen"),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your Library",
                color = AuraTextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Collections, offline music, and favorites",
                color = AuraCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Liked Songs Tile
        item {
            LibraryTile(
                title = "Liked Songs",
                subtitle = "$likedSongsCount tracks",
                icon = Icons.Outlined.Favorite,
                iconColor = AuraPink,
                onClick = onOpenLikedSongs,
                modifier = Modifier.testTag("library_tile_liked_songs")
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // My Playlists Tile
        item {
            LibraryTile(
                title = "My Playlists",
                subtitle = "$playlistsCount playlists created",
                icon = Icons.Outlined.QueueMusic,
                iconColor = AuraCyan,
                onClick = onOpenPlaylists,
                modifier = Modifier.testTag("library_tile_playlists")
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Downloads (Offline Mode)
        item {
            LibraryTile(
                title = "Downloads",
                subtitle = "$downloadsCount tracks saved offline (Permitted audio)",
                icon = Icons.Outlined.Download,
                iconColor = AuraMint,
                onClick = onOpenDownloads,
                modifier = Modifier.testTag("library_tile_downloads")
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Recently Played
        item {
            LibraryTile(
                title = "Recently Played",
                subtitle = "$recentlyPlayedCount tracks in history",
                icon = Icons.Outlined.History,
                iconColor = AuraViolet,
                onClick = onOpenRecentlyPlayed,
                modifier = Modifier.testTag("library_tile_recently_played")
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Albums
        item {
            LibraryTile(
                title = "Albums",
                subtitle = "$albumsCount full albums",
                icon = Icons.Outlined.Album,
                iconColor = Color(0xFFFFB703),
                onClick = onOpenAlbums,
                modifier = Modifier.testTag("library_tile_albums")
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Artists
        item {
            LibraryTile(
                title = "Artists",
                subtitle = "$artistsCount followed creators",
                icon = Icons.Outlined.People,
                iconColor = Color(0xFF64B5F6),
                onClick = onOpenArtists,
                modifier = Modifier.testTag("library_tile_artists")
            )
        }
    }
}

@Composable
fun LibraryTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AuraSurface)
            .border(1.dp, AuraCardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = AuraTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = AuraTextSecondary,
                fontSize = 13.sp
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = AuraTextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}
