package com.example.aura.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.aura.backend.AuraSharedBackend
import com.example.aura.model.Playlist
import com.example.aura.model.Song
import com.example.aura.sharing.AuraShareHelper
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailsScreen(
    playlist: Playlist,
    allSongs: List<Song>,
    backend: AuraSharedBackend,
    userId: String?,
    onBack: () -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onSongClick: (Song) -> Unit,
    onPlaylistDeleted: () -> Unit
) {
    val context = LocalContext.current
    val playlistSongs = remember(playlist.songIds, allSongs) {
        playlist.songIds.mapNotNull { id -> allSongs.find { it.id == id } }
    }

    var showRenameDialog by remember { mutableStateOf(false) }
    var currentTitle by remember { mutableStateOf(playlist.title) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentTitle, color = AuraTextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AuraTextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { AuraShareHelper.sharePlaylist(context, playlist) }) {
                        Icon(Icons.Outlined.Share, contentDescription = "Share", tint = AuraTextPrimary)
                    }
                    if (userId == playlist.createdBy) {
                        IconButton(onClick = { showRenameDialog = true }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Rename", tint = AuraTextPrimary)
                        }
                        IconButton(
                            onClick = {
                                backend.deletePlaylist(playlist.id)
                                Toast.makeText(context, "Playlist deleted", Toast.LENGTH_SHORT).show()
                                onPlaylistDeleted()
                            }
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = AuraPink)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AuraBackground)
            )
        },
        containerColor = AuraBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))

                // Header Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, AuraCardBorder, RoundedCornerShape(16.dp))
                    ) {
                        if (playlist.coverUrl.isNotEmpty()) {
                            AsyncImage(
                                model = playlist.coverUrl,
                                contentDescription = currentTitle,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(AuraSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.QueueMusic, contentDescription = null, tint = AuraCyan, modifier = Modifier.size(44.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentTitle,
                            color = AuraTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Created by ${playlist.creatorName}",
                            color = AuraCyan,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${playlistSongs.size} tracks",
                            color = AuraTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Play All Button
                if (playlistSongs.isNotEmpty()) {
                    Button(
                        onClick = { onPlayAll(playlistSongs) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AuraCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Play All", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            if (playlistSongs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("This playlist is empty.", color = AuraTextMuted, fontSize = 14.sp)
                    }
                }
            } else {
                items(playlistSongs, key = { it.id }) { song ->
                    SongRowItem(
                        song = song,
                        onPlay = { onPlayAll(listOf(song)) },
                        onClick = { onSongClick(song) },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(currentTitle) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Playlist", color = AuraTextPrimary) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            backend.renamePlaylist(playlist.id, newName)
                            currentTitle = newName
                        }
                        showRenameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraCyan, contentColor = Color.Black)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = AuraTextMuted)
                }
            },
            containerColor = AuraSurfaceVariant
        )
    }
}
