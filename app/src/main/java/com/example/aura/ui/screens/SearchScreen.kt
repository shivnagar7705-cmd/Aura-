package com.example.aura.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aura.model.Album
import com.example.aura.model.Artist
import com.example.aura.model.Playlist
import com.example.aura.model.Song
import com.example.ui.theme.*

enum class SearchFilter {
    ALL,
    SONGS,
    ARTISTS,
    ALBUMS,
    PLAYLISTS
}

@Composable
fun SearchScreen(
    publishedSongs: List<Song>,
    artists: List<Artist>,
    albums: List<Album>,
    playlists: List<Playlist>,
    onSongSelected: (Song, List<Song>) -> Unit,
    onSongDetailsClick: (Song) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onPlaylistClick: (Playlist) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(SearchFilter.ALL) }

    val filteredSongs = remember(searchQuery, publishedSongs) {
        if (searchQuery.isBlank()) emptyList()
        else publishedSongs.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true) ||
            it.genre.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredArtists = remember(searchQuery, artists) {
        if (searchQuery.isBlank()) emptyList()
        else artists.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val filteredAlbums = remember(searchQuery, albums) {
        if (searchQuery.isBlank()) emptyList()
        else albums.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredPlaylists = remember(searchQuery, playlists) {
        if (searchQuery.isBlank()) emptyList()
        else playlists.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    val hasResults = filteredSongs.isNotEmpty() || filteredArtists.isNotEmpty() ||
            filteredAlbums.isNotEmpty() || filteredPlaylists.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraBackground)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .testTag("search_screen")
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Search",
            color = AuraTextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Search Input Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search songs, artists, albums...", color = AuraTextMuted) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = AuraCyan)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = AuraTextSecondary)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AuraCyan,
                unfocusedBorderColor = AuraCardBorder,
                focusedTextColor = AuraTextPrimary,
                unfocusedTextColor = AuraTextPrimary,
                focusedContainerColor = AuraSurface,
                unfocusedContainerColor = AuraSurface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_input_field")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Chips Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(SearchFilter.values()) { filter ->
                val isSelected = selectedFilter == filter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) AuraCyan else AuraSurface)
                        .border(1.dp, if (isSelected) AuraCyan else AuraCardBorder, RoundedCornerShape(20.dp))
                        .clickable { selectedFilter = filter }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = filter.name.lowercase().replaceFirstChar { it.uppercase() },
                        color = if (isSelected) Color.Black else AuraTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Content or Empty / No Results State
        if (searchQuery.isBlank()) {
            // Initial browse state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 120.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = AuraCyan.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Find What Moves You",
                        color = AuraTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Search by song title, artist, album, or genre",
                        color = AuraTextMuted,
                        fontSize = 13.sp
                    )
                }
            }
        } else if (!hasResults) {
            // No results state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 120.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = AuraPink.copy(alpha = 0.5f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No results found for \"$searchQuery\"",
                        color = AuraTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Check your spelling or explore new artists",
                        color = AuraTextMuted,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            // Search Results List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Songs section
                if ((selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.SONGS) && filteredSongs.isNotEmpty()) {
                    item {
                        Text(
                            text = "Songs (${filteredSongs.size})",
                            color = AuraCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(filteredSongs, key = { it.id }) { song ->
                        SongRowItem(
                            song = song,
                            onPlay = { onSongSelected(song, filteredSongs) },
                            onClick = { onSongDetailsClick(song) }
                        )
                    }
                }

                // Artists section
                if ((selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.ARTISTS) && filteredArtists.isNotEmpty()) {
                    item {
                        Text(
                            text = "Artists (${filteredArtists.size})",
                            color = AuraCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                        )
                    }
                    items(filteredArtists, key = { it.id }) { artist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AuraSurface)
                                .border(1.dp, AuraCardBorder, RoundedCornerShape(12.dp))
                                .clickable { onArtistClick(artist) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = artist.name,
                                color = AuraTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = artist.genre,
                                color = AuraTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Albums section
                if ((selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.ALBUMS) && filteredAlbums.isNotEmpty()) {
                    item {
                        Text(
                            text = "Albums (${filteredAlbums.size})",
                            color = AuraCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                        )
                    }
                    items(filteredAlbums, key = { it.id }) { album ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AuraSurface)
                                .border(1.dp, AuraCardBorder, RoundedCornerShape(12.dp))
                                .clickable { onAlbumClick(album) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = album.title,
                                    color = AuraTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${album.artist} • ${album.releaseYear}",
                                    color = AuraTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Playlists section
                if ((selectedFilter == SearchFilter.ALL || selectedFilter == SearchFilter.PLAYLISTS) && filteredPlaylists.isNotEmpty()) {
                    item {
                        Text(
                            text = "Playlists (${filteredPlaylists.size})",
                            color = AuraCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                        )
                    }
                    items(filteredPlaylists, key = { it.id }) { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AuraSurface)
                                .border(1.dp, AuraCardBorder, RoundedCornerShape(12.dp))
                                .clickable { onPlaylistClick(playlist) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist.title,
                                    color = AuraTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${playlist.songIds.size} songs • By ${playlist.creatorName}",
                                    color = AuraTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
