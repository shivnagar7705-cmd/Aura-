package com.example.aura.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.aura.ads.AuraAdBanner
import com.example.aura.model.Artist
import com.example.aura.model.HomeBanner
import com.example.aura.model.Song
import com.example.aura.model.User
import com.example.aura.ui.components.SongArtworkThumbnail
import com.example.aura.ui.components.SongDynamicPalette
import com.example.aura.ui.components.getSongDynamicPalette
import com.example.ui.theme.*
import java.util.Calendar

@Composable
fun HomeScreen(
    currentUser: User?,
    banners: List<HomeBanner>,
    publishedSongs: List<Song>,
    recentlyPlayed: List<Song>,
    artists: List<Artist>,
    onSongSelected: (Song, List<Song>) -> Unit,
    onSongDetailsClick: (Song) -> Unit,
    onArtistClick: (Artist) -> Unit,
    currentPlayingSongId: String? = null,
    isPlaying: Boolean = false,
    currentSong: Song? = null
) {
    // Dynamic Greeting based on time of day
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    val newReleases = remember(publishedSongs) { publishedSongs.filter { it.isNewRelease } }
    val trendingSongs = remember(publishedSongs) { publishedSongs.filter { it.isTrending } }
    val recommendedSongs = remember(publishedSongs) { publishedSongs.filter { it.isRecommended } }

    val activeSong = remember(currentPlayingSongId, currentSong, publishedSongs) {
        currentSong ?: publishedSongs.find { it.id == currentPlayingSongId }
    }
    val currentPalette = remember(activeSong?.id, activeSong?.title) {
        getSongDynamicPalette(activeSong)
    }

    val animatedTopBg by androidx.compose.animation.animateColorAsState(
        targetValue = if (activeSong != null && isPlaying) currentPalette.topColor.copy(alpha = 0.35f) else Color.Transparent,
        animationSpec = androidx.compose.animation.core.tween(800),
        label = "home_top_bg"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraBackground)
    ) {
        // Atmospheric dynamic color glow responding to currently playing track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            animatedTopBg,
                            Color.Transparent
                        )
                    )
                )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_screen_feed"),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
        // Greeting Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 14.dp)
            ) {
                Text(
                    text = "$greeting${if (currentUser != null) ", ${currentUser.username}" else ""}",
                    color = AuraTextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Future Sounds & AI Music • Lossless Audio",
                    color = AuraCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Featured Banners Carousel
        if (banners.isNotEmpty()) {
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(banners, key = { it.id }) { banner ->
                        FeaturedBannerCard(
                            banner = banner,
                            onBannerClick = {
                                if (banner.targetSongId != null) {
                                    val targetSong = publishedSongs.find { it.id == banner.targetSongId }
                                    if (targetSong != null) {
                                        onSongSelected(targetSong, publishedSongs)
                                    }
                                }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Recently Played
        if (recentlyPlayed.isNotEmpty()) {
            item {
                SectionHeader(title = "Recently Played")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(recentlyPlayed, key = { it.id }) { song ->
                        SongHorizontalCard(
                            song = song,
                            onPlay = { onSongSelected(song, recentlyPlayed) },
                            onClick = { onSongDetailsClick(song) },
                            isCurrentPlaying = (song.id == currentPlayingSongId && isPlaying)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // New Releases
        if (newReleases.isNotEmpty()) {
            item {
                SectionHeader(title = "New Releases")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(newReleases, key = { it.id }) { song ->
                        SongHorizontalCard(
                            song = song,
                            onPlay = { onSongSelected(song, newReleases) },
                            onClick = { onSongDetailsClick(song) },
                            isCurrentPlaying = (song.id == currentPlayingSongId && isPlaying)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Non-intrusive AdMob Banner
        item {
            AuraAdBanner(modifier = Modifier.padding(vertical = 4.dp))
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Trending Now
        if (trendingSongs.isNotEmpty()) {
            item {
                SectionHeader(title = "Trending Now")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(trendingSongs, key = { it.id }) { song ->
                        SongHorizontalCard(
                            song = song,
                            onPlay = { onSongSelected(song, trendingSongs) },
                            onClick = { onSongDetailsClick(song) },
                            isCurrentPlaying = (song.id == currentPlayingSongId && isPlaying)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Popular Artists
        if (artists.isNotEmpty()) {
            item {
                SectionHeader(title = "Popular Artists")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(artists, key = { it.id }) { artist ->
                        ArtistCircleItem(
                            artist = artist,
                            onClick = { onArtistClick(artist) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Recommended for You (Vertical Grid Cards)
        if (recommendedSongs.isNotEmpty()) {
            item {
                SectionHeader(title = "Recommended for You")
            }
            items(recommendedSongs, key = { it.id }) { song ->
                SongRowItem(
                    song = song,
                    onPlay = { onSongSelected(song, recommendedSongs) },
                    onClick = { onSongDetailsClick(song) },
                    isCurrentPlaying = (song.id == currentPlayingSongId && isPlaying),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
        }
    }
}
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = AuraTextPrimary,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun FeaturedBannerCard(
    banner: HomeBanner,
    onBannerClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(310.dp)
            .height(150.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF19213D),
                        Color(0xFF0F1322)
                    )
                )
            )
            .border(1.dp, AuraCardBorder, RoundedCornerShape(20.dp))
            .clickable { onBannerClick() }
    ) {
        AsyncImage(
            model = banner.imageUrl,
            contentDescription = banner.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.45f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(AuraCyan.copy(alpha = 0.25f))
                    .border(0.5.dp, AuraCyan, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = banner.tag,
                    color = AuraCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = banner.title,
                color = AuraTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = banner.subtitle,
                color = AuraTextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SongHorizontalCard(
    song: Song,
    onPlay: () -> Unit,
    onClick: () -> Unit,
    isCurrentPlaying: Boolean = false
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, AuraCardBorder, RoundedCornerShape(16.dp))
        ) {
            SongArtworkThumbnail(
                song = song,
                isCurrentlyPlaying = isCurrentPlaying,
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(16.dp),
                beatsSize = 22.dp
            )

            // Play icon overlay button - Pure Solid White with Black Icon
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(34.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onPlay() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCurrentPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isCurrentPlaying) "Pause" else "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = song.title,
            color = AuraTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist,
            color = AuraTextSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ArtistCircleItem(
    artist: Artist,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(90.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = artist.imageUrl,
            contentDescription = artist.name,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .border(1.5.dp, AuraViolet.copy(alpha = 0.5f), CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = artist.name,
            color = AuraTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SongRowItem(
    song: Song,
    onPlay: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCurrentPlaying: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AuraSurface)
            .border(1.dp, AuraCardBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SongArtworkThumbnail(
            song = song,
            isCurrentlyPlaying = isCurrentPlaying,
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(8.dp),
            beatsSize = 16.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = AuraTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${song.artist} • ${song.genre}",
                color = AuraTextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // White Play Button with Black Icon
        IconButton(
            onClick = onPlay,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White)
        ) {
            Icon(
                imageVector = if (isCurrentPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isCurrentPlaying) "Pause" else "Play",
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
