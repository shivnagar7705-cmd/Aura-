package com.example.aura.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.aura.backend.AuraSharedBackend
import com.example.aura.model.Song
import com.example.aura.sharing.AuraShareHelper
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDetailsScreen(
    song: Song,
    backend: AuraSharedBackend,
    userId: String?,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onReportContent: (Song) -> Unit
) {
    val context = LocalContext.current
    var isLiked by remember { mutableStateOf(userId?.let { backend.isSongLiked(it, song.id) } ?: false) }
    var isDownloaded by remember { mutableStateOf(userId?.let { backend.isSongDownloaded(it, song.id) } ?: false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Song Details", color = AuraTextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AuraTextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { AuraShareHelper.shareSong(context, song) }) {
                        Icon(Icons.Outlined.Share, contentDescription = "Share", tint = AuraTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AuraBackground)
            )
        },
        containerColor = AuraBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Cover Art
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.5.dp, AuraCardBorder, RoundedCornerShape(20.dp))
            ) {
                AsyncImage(
                    model = song.coverUrl,
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Title & Artist
            Text(
                text = song.title,
                color = AuraTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${song.artist} • ${song.album}",
                color = AuraCyan,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Play Button
            Button(
                onClick = onPlay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .testTag("song_details_play_button"),
                colors = ButtonDefaults.buttonColors(containerColor = AuraCyan, contentColor = Color.Black)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Play Track", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Row: Like, Add to Playlist, Download, Copy Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like
                IconButton(
                    onClick = {
                        if (userId != null) {
                            isLiked = backend.toggleLike(userId, song.id)
                        } else {
                            Toast.makeText(context, "Please log in to like tracks", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) AuraPink else AuraTextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Add to Playlist
                IconButton(onClick = onAddToPlaylist) {
                    Icon(
                        imageVector = Icons.Outlined.BookmarkAdd,
                        contentDescription = "Add to Playlist",
                        tint = AuraTextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Download
                if (song.downloadPermitted) {
                    IconButton(
                        onClick = {
                            if (userId != null) {
                                if (isDownloaded) {
                                    backend.removeDownload(userId, song.id)
                                    isDownloaded = false
                                    Toast.makeText(context, "Download removed", Toast.LENGTH_SHORT).show()
                                } else {
                                    val res = backend.downloadSong(userId, song)
                                    res.onSuccess {
                                        isDownloaded = true
                                        Toast.makeText(context, "Downloaded for offline playback", Toast.LENGTH_SHORT).show()
                                    }.onFailure {
                                        Toast.makeText(context, it.message ?: "Download failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isDownloaded) Icons.Default.CheckCircle else Icons.Outlined.Download,
                            contentDescription = "Download",
                            tint = if (isDownloaded) AuraMint else AuraTextSecondary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Copy Link
                IconButton(
                    onClick = {
                        AuraShareHelper.copyToClipboard(
                            context,
                            AuraShareHelper.getSongShareUrl(song.id)
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Link",
                        tint = AuraTextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Metadata Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AuraSurface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(1.dp, brush = androidx.compose.ui.graphics.SolidColor(AuraCardBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("TRACK INFORMATION", color = AuraCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    InfoRow(label = "Genre", value = song.genre)
                    InfoRow(label = "Release Year", value = song.releaseDate)
                    InfoRow(label = "Streams", value = "${song.playsCount} plays")
                    InfoRow(label = "Offline Download", value = if (song.downloadPermitted) "Licensed for Offline" else "Streaming Only")

                    if (song.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = song.description,
                            color = AuraTextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Copyright Badge & Notice
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101322)),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(1.dp, brush = androidx.compose.ui.graphics.SolidColor(AuraCardBorder))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Copyright, contentDescription = null, tint = AuraTextMuted, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("LEGAL & COPYRIGHT", color = AuraTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = song.copyrightNotice.ifEmpty { "© 2026 AURA Music. All rights reserved by respective owners." },
                        color = AuraTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Report Content Button
            TextButton(
                onClick = { onReportContent(song) },
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF6584))
            ) {
                Icon(Icons.Outlined.Report, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Report Content or Copyright Issue", fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = AuraTextMuted, fontSize = 13.sp)
        Text(value, color = AuraTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
