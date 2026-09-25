package com.aura.studio.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aura.studio.backend.AuraStudioBackend
import com.aura.studio.model.*
import kotlinx.coroutines.launch

// ==========================================
// 1. ADMIN AUTHENTICATION SCREEN
// ==========================================

@Composable
fun StudioAuthScreen(backend: AuraStudioBackend) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = StudioSurface),
            border = BorderStroke(1.dp, StudioCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Official AURA Emblem with live glowing audio wave
                AuraStudioImageLogo(
                    size = 80.dp,
                    animated = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "AURA STUDIO",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )

                Text(
                    text = "ARTIST & CATALOG MANAGER",
                    color = StudioAmberLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                if (errorMessage != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0x33E53935)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFFF8A80),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = null },
                    label = { Text("Admin Email") },
                    placeholder = { Text("admin@aura.music") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = StudioAmber) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_email_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    label = { Text("Admin Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = StudioAmber) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_password_input")
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            errorMessage = "Please enter both admin email and password"
                            return@Button
                        }
                        isSubmitting = true
                        errorMessage = null
                        coroutineScope.launch {
                            val result = backend.login(email.trim(), password)
                            isSubmitting = false
                            result.onSuccess {
                                Toast.makeText(context, "Welcome ${it.fullName}", Toast.LENGTH_SHORT).show()
                            }.onFailure { e ->
                                errorMessage = e.message ?: "Authentication failed. Admin role required."
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("admin_login_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StudioAmber, contentColor = Color.Black),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.Black, strokeWidth = 2.dp)
                    } else {
                        Text("Sign In to Studio", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        email = "shivnagar7705@gmail.com"
                        password = "AdminPassword2026!"
                        isSubmitting = true
                        errorMessage = null
                        coroutineScope.launch {
                            val result = backend.login("shivnagar7705@gmail.com", "AdminPassword2026!")
                            isSubmitting = false
                            result.onSuccess {
                                Toast.makeText(context, "Welcome ${it.fullName}", Toast.LENGTH_SHORT).show()
                            }.onFailure { e ->
                                errorMessage = e.message ?: "Authentication failed."
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, StudioAmber.copy(alpha = 0.5f))
                ) {
                    Text("⚡ Quick Login as Owner (Shivnagar)", color = StudioAmberLight, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Role-based verification enforced. All actions logged.",
                    color = StudioTextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// ==========================================
// 2. DASHBOARD SCREEN
// ==========================================

@Composable
fun StudioDashboardScreen(
    backend: AuraStudioBackend,
    onNavigateTab: (StudioTab) -> Unit
) {
    val songs by backend.songs.collectAsState()
    val artists by backend.artists.collectAsState()
    val albums by backend.albums.collectAsState()
    val users by backend.users.collectAsState()

    val publishedCount = remember(songs) { songs.count { it.status == "PUBLISHED" } }
    val draftCount = remember(songs) { songs.count { it.status != "PUBLISHED" } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Studio Overview",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = StudioTextPrimary
            )
            Text(
                text = "Live catalog and system statistics",
                fontSize = 13.sp,
                color = StudioTextSecondary
            )
        }

        // Stats grid
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(title = "Total Songs", count = songs.size.toString(), color = StudioAmber, modifier = Modifier.weight(1f))
                StatCard(title = "Published", count = publishedCount.toString(), color = StudioGreen, modifier = Modifier.weight(1f))
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(title = "Unpublished", count = draftCount.toString(), color = StudioRed, modifier = Modifier.weight(1f))
                StatCard(title = "Artists", count = artists.size.toString(), color = Color(0xFF64B5F6), modifier = Modifier.weight(1f))
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(title = "Albums", count = albums.size.toString(), color = Color(0xFFBA68C8), modifier = Modifier.weight(1f))
                StatCard(title = "Users", count = users.size.toString(), color = Color(0xFFFFB74D), modifier = Modifier.weight(1f))
            }
        }

        // Quick Actions
        item {
            Text(
                text = "Quick Management",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = StudioTextPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateTab(StudioTab.UPLOAD) },
                colors = CardDefaults.cardColors(containerColor = StudioSurface),
                border = BorderStroke(1.dp, StudioCardBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(StudioAmber),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, tint = Color.Black)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Upload New Song", color = StudioTextPrimary, fontWeight = FontWeight.Bold)
                        Text("Upload MP3/WAV, artwork & publish directly to Aura Music", color = StudioTextSecondary, fontSize = 12.sp)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = StudioAmber)
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateTab(StudioTab.SONGS) },
                colors = CardDefaults.cardColors(containerColor = StudioSurface),
                border = BorderStroke(1.dp, StudioCardBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF64B5F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = Color.Black)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Manage Music Catalog", color = StudioTextPrimary, fontWeight = FontWeight.Bold)
                        Text("Publish, unpublish, edit metadata or delete songs", color = StudioTextSecondary, fontSize = 12.sp)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = StudioAmber)
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, count: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StudioSurface),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = StudioTextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(count, color = color, fontSize = 26.sp, fontWeight = FontWeight.Black)
        }
    }
}

// ==========================================
// 3. SONG MANAGEMENT SCREEN
// ==========================================

@Composable
fun StudioSongsScreen(backend: AuraStudioBackend) {
    val songs by backend.songs.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf("All") }
    var songToDelete by remember { mutableStateOf<StudioSong?>(null) }
    var songToEdit by remember { mutableStateOf<StudioSong?>(null) }

    val filteredSongs = remember(songs, searchQuery, selectedGenre) {
        songs.filter { song ->
            val matchSearch = searchQuery.isBlank() ||
                    song.title.contains(searchQuery, ignoreCase = true) ||
                    song.artist.contains(searchQuery, ignoreCase = true)
            val matchGenre = selectedGenre == "All" || song.genre.equals(selectedGenre, ignoreCase = true)
            matchSearch && matchGenre
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search songs or artists...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = StudioAmber) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredSongs, key = { it.id }) { song ->
                SongManagementCard(
                    song = song,
                    onTogglePublish = {
                        val newStatus = if (song.status == "PUBLISHED") "UNPUBLISHED" else "PUBLISHED"
                        coroutineScope.launch {
                            backend.toggleSongPublishStatus(song.id, newStatus).onSuccess {
                                Toast.makeText(context, "Song set to $newStatus", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onEdit = { songToEdit = song },
                    onDelete = { songToDelete = song }
                )
            }
        }
    }

    if (songToDelete != null) {
        AlertDialog(
            onDismissRequest = { songToDelete = null },
            title = { Text("Delete Song?", color = StudioTextPrimary) },
            text = { Text("Are you sure you want to permanently delete '${songToDelete?.title}' from the cloud catalog?", color = StudioTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        val song = songToDelete ?: return@Button
                        coroutineScope.launch {
                            backend.deleteSong(song.id).onSuccess {
                                Toast.makeText(context, "Song deleted", Toast.LENGTH_SHORT).show()
                                songToDelete = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { songToDelete = null }) {
                    Text("Cancel", color = StudioTextSecondary)
                }
            },
            containerColor = StudioSurfaceVariant
        )
    }

    if (songToEdit != null) {
        EditSongDialog(
            song = songToEdit!!,
            onDismiss = { songToEdit = null },
            onSave = { updatedSong ->
                coroutineScope.launch {
                    backend.createOrUpdateSong(updatedSong).onSuccess {
                        Toast.makeText(context, "Song metadata updated", Toast.LENGTH_SHORT).show()
                        songToEdit = null
                    }
                }
            }
        )
    }
}

@Composable
fun SongManagementCard(
    song: StudioSong,
    onTogglePublish: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StudioSurface),
        border = BorderStroke(1.dp, StudioCardBorder)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.DarkGray),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, color = StudioTextPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, color = StudioTextSecondary, fontSize = 12.sp, maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isPub = song.status == "PUBLISHED"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isPub) Color(0x334CAF50) else Color(0x33E53935))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isPub) "PUBLISHED" else "UNPUBLISHED",
                            color = if (isPub) StudioGreen else StudioRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(song.genre, color = StudioAmberLight, fontSize = 10.sp)
                }
            }

            IconButton(onClick = onTogglePublish) {
                Icon(
                    imageVector = if (song.status == "PUBLISHED") Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Toggle status",
                    tint = if (song.status == "PUBLISHED") StudioGreen else StudioRed
                )
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit metadata", tint = StudioAmber)
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StudioRed)
            }
        }
    }
}

// ==========================================
// 4. UPLOAD SONG SCREEN
// ==========================================

@Composable
fun StudioUploadSongScreen(
    backend: AuraStudioBackend,
    onUploaded: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var album by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("Pop") }
    var description by remember { mutableStateOf("") }
    var lyrics by remember { mutableStateOf("") }
    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var audioFileName by remember { mutableStateOf("") }
    var coverUri by remember { mutableStateOf<Uri?>(null) }
    var isPublishImmediately by remember { mutableStateOf(true) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadStatus by remember { mutableStateOf("") }
    var showRulesDialog by remember { mutableStateOf(false) }

    if (showRulesDialog) {
        FirebaseRulesGuideDialog(
            backend = backend,
            onDismiss = { showRulesDialog = false }
        )
    }

    // Audio file picker
    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            audioUri = uri
            audioFileName = uri.lastPathSegment ?: "selected_audio.mp3"
        }
    }

    // Cover image picker
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) coverUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Upload New Music", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = StudioTextPrimary)
                Text("Add master audio & cover art to the live catalog", fontSize = 12.sp, color = StudioTextSecondary)
            }
            OutlinedButton(
                onClick = { showRulesDialog = true },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, StudioAmber.copy(alpha = 0.6f)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.CloudQueue, contentDescription = null, tint = StudioAmberLight, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Firebase Rules", fontSize = 11.sp, color = StudioAmberLight, fontWeight = FontWeight.Bold)
            }
        }

        // Audio File Card
        Card(
            modifier = Modifier.fillMaxWidth().clickable { audioPicker.launch("audio/*") },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = StudioSurface),
            border = BorderStroke(1.dp, if (audioUri != null) StudioGreen else StudioCardBorder)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AudioFile,
                    contentDescription = null,
                    tint = if (audioUri != null) StudioGreen else StudioAmber,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (audioUri != null) "Audio File Selected" else "Select Master Audio (MP3/WAV)",
                        color = StudioTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (audioUri != null) audioFileName else "Tap to choose audio file from storage",
                        color = StudioTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Cover Art Card
        Card(
            modifier = Modifier.fillMaxWidth().clickable {
                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = StudioSurface),
            border = BorderStroke(1.dp, if (coverUri != null) StudioGreen else StudioCardBorder)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (coverUri != null) {
                    AsyncImage(
                        model = coverUri,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Image, contentDescription = null, tint = StudioAmber, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (coverUri != null) "Cover Artwork Selected" else "Select Cover Artwork",
                        color = StudioTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (coverUri != null) "Ready to upload" else "Tap to choose image (JPG/PNG)",
                        color = StudioTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Song Title *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = artist,
            onValueChange = { artist = it },
            label = { Text("Artist Name *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = album,
            onValueChange = { album = it },
            label = { Text("Album Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = genre,
            onValueChange = { genre = it },
            label = { Text("Genre / Category") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description / Credits") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = lyrics,
            onValueChange = { lyrics = it },
            label = { Text("Lyrics") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = isPublishImmediately,
                onCheckedChange = { isPublishImmediately = it },
                colors = SwitchDefaults.colors(checkedThumbColor = StudioAmber, checkedTrackColor = Color(0x66DE9E36))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Publish to Aura Music Immediately", color = StudioTextPrimary, fontWeight = FontWeight.SemiBold)
                Text("When active, all Aura Music users will see this song instantly", color = StudioTextSecondary, fontSize = 11.sp)
            }
        }

        if (uploadStatus.isNotBlank()) {
            val isError = uploadStatus.contains("failed", ignoreCase = true) || uploadStatus.contains("denied", ignoreCase = true)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (isError) Color(0x33E53935) else Color(0x22DE9E36)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        uploadStatus,
                        color = if (isError) Color(0xFFFF8A80) else StudioAmberLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (isError && (uploadStatus.contains("permission", ignoreCase = true) || uploadStatus.contains("PERMISSION_DENIED", ignoreCase = true))) {
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(
                            onClick = { showRulesDialog = true },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("👉 Click here to View & Copy Required Firebase Rules", color = StudioAmberLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                if (title.isBlank() || artist.isBlank()) {
                    Toast.makeText(context, "Title and Artist are required", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                isUploading = true
                uploadStatus = "Preparing cloud upload..."

                coroutineScope.launch {
                    var audioUrl = ""
                    var coverUrl = ""

                    // 1. Upload audio track if selected
                    if (audioUri != null) {
                        uploadStatus = "1/3 Uploading audio track (${audioFileName})..."
                        val audioResult = backend.uploadAudioFile(audioUri!!, audioFileName)
                        if (audioResult.isFailure) {
                            isUploading = false
                            val err = audioResult.exceptionOrNull()?.message ?: "Audio upload failed"
                            uploadStatus = "Audio upload failed: $err"
                            if (err.contains("permission", ignoreCase = true) || err.contains("denied", ignoreCase = true)) {
                                showRulesDialog = true
                            }
                            return@launch
                        }
                        audioUrl = audioResult.getOrNull() ?: ""
                    }

                    // 2. Upload cover artwork if selected
                    if (coverUri != null) {
                        uploadStatus = "2/3 Uploading cover artwork..."
                        val coverResult = backend.uploadCoverImage(coverUri!!, "cover_${System.currentTimeMillis()}.jpg")
                        if (coverResult.isFailure) {
                            val err = coverResult.exceptionOrNull()?.message ?: "Cover image upload failed"
                            uploadStatus = "Warning: Cover upload failed ($err). Proceeding with audio..."
                        } else {
                            coverUrl = coverResult.getOrNull() ?: ""
                        }
                    }

                    // 3. Save song metadata to live Firestore catalog
                    uploadStatus = "3/3 Publishing song to live Aura Music catalog..."
                    val newSong = StudioSong(
                        id = "",
                        title = title.trim(),
                        artist = artist.trim(),
                        album = album.trim(),
                        genre = genre.trim(),
                        description = description.trim(),
                        lyrics = lyrics.trim(),
                        audioUrl = audioUrl,
                        coverUrl = coverUrl,
                        status = if (isPublishImmediately) "PUBLISHED" else "UNPUBLISHED",
                        releaseDate = "2026",
                        isNewRelease = true
                    )

                    val saveResult = backend.createOrUpdateSong(newSong)
                    isUploading = false
                    if (saveResult.isSuccess) {
                        uploadStatus = "✓ Song successfully published to Aura Music catalog!"
                        Toast.makeText(context, "Song successfully saved and catalog updated!", Toast.LENGTH_LONG).show()
                        onUploaded()
                    } else {
                        val errMsg = saveResult.exceptionOrNull()?.message ?: "Save operation failed"
                        uploadStatus = "Save failed: $errMsg"
                        if (errMsg.contains("PERMISSION_DENIED", ignoreCase = true) || errMsg.contains("permission", ignoreCase = true)) {
                            showRulesDialog = true
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = StudioAmber, contentColor = Color.Black),
            enabled = !isUploading
        ) {
            if (isUploading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black, strokeWidth = 2.dp)
            } else {
                Text("Upload & Save Song", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

// ==========================================
// FIREBASE RULES & CLOUD GUIDE DIALOG
// ==========================================

@Composable
fun FirebaseRulesGuideDialog(
    backend: AuraStudioBackend,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val firestoreRules = remember { backend.getRequiredFirestoreRules() }
    val storageRules = remember { backend.getRequiredStorageRules() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudDone, contentDescription = null, tint = StudioAmber)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Firebase Rules Setup", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = StudioTextPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Project: aura-music-b07a4\nAuth Status: ${backend.getAuthStatusDescription()}",
                    color = StudioAmberLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "To allow song uploads and live catalog updates, Cloud Firestore and Firebase Storage security rules must allow access.",
                    color = StudioTextSecondary,
                    fontSize = 12.sp
                )

                HorizontalDivider(color = StudioCardBorder)

                Text("Step 1: Firestore Database Rules", fontWeight = FontWeight.Bold, color = StudioTextPrimary, fontSize = 13.sp)
                Text("Go to Firebase Console → Firestore Database → Rules tab, paste this and click Publish:", color = StudioTextSecondary, fontSize = 11.sp)

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E1E1E),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = firestoreRules,
                        color = Color(0xFFA5D6A7),
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Button(
                    onClick = {
                        clipboard.setText(AnnotatedString(firestoreRules))
                        Toast.makeText(context, "Firestore rules copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioAmber, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Firestore Rules", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(color = StudioCardBorder)

                Text("Step 2: Firebase Storage Rules", fontWeight = FontWeight.Bold, color = StudioTextPrimary, fontSize = 13.sp)
                Text("Go to Firebase Console → Storage → Rules tab, paste this and click Publish:", color = StudioTextSecondary, fontSize = 11.sp)

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E1E1E),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = storageRules,
                        color = Color(0xFFA5D6A7),
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Button(
                    onClick = {
                        clipboard.setText(AnnotatedString(storageRules))
                        Toast.makeText(context, "Storage rules copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioAmber, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Storage Rules", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = StudioAmber, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = StudioSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

// ==========================================
// 5. ARTISTS MANAGEMENT SCREEN
// ==========================================

@Composable
fun StudioArtistsScreen(backend: AuraStudioBackend) {
    val artists by backend.artists.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Artist Management", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = StudioTextPrimary)
            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = StudioAmber, contentColor = Color.Black)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Artist")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(artists, key = { it.id }) { artist ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = StudioSurface),
                    border = BorderStroke(1.dp, StudioCardBorder)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = artist.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.DarkGray),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(artist.name, color = StudioTextPrimary, fontWeight = FontWeight.Bold)
                            Text(artist.genre, color = StudioAmberLight, fontSize = 12.sp)
                            Text(artist.bio, color = StudioTextSecondary, fontSize = 11.sp, maxLines = 1)
                        }
                        IconButton(onClick = {
                            coroutineScope.launch {
                                backend.deleteArtist(artist.id).onSuccess {
                                    Toast.makeText(context, "Artist removed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = StudioRed)
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        var genre by remember { mutableStateOf("Pop") }
        var bio by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New Artist", color = StudioTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Artist Name") }, singleLine = true)
                    OutlinedTextField(value = genre, onValueChange = { genre = it }, label = { Text("Genre") }, singleLine = true)
                    OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Biography") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isBlank()) return@Button
                        coroutineScope.launch {
                            val newArtist = StudioArtist(id = "", name = name.trim(), genre = genre.trim(), bio = bio.trim())
                            backend.createOrUpdateArtist(newArtist).onSuccess {
                                showCreateDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioAmber, contentColor = Color.Black)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel", color = StudioTextSecondary) }
            },
            containerColor = StudioSurfaceVariant
        )
    }
}

// ==========================================
// 6. ALBUMS MANAGEMENT SCREEN
// ==========================================

@Composable
fun StudioAlbumsScreen(backend: AuraStudioBackend) {
    val albums by backend.albums.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Album Management", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = StudioTextPrimary)
        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(albums, key = { it.id }) { album ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = StudioSurface),
                    border = BorderStroke(1.dp, StudioCardBorder)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = album.coverUrl,
                            contentDescription = null,
                            modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(album.title, color = StudioTextPrimary, fontWeight = FontWeight.Bold)
                            Text(album.artist, color = StudioTextSecondary, fontSize = 12.sp)
                            Text("${album.releaseYear} • ${album.genre}", color = StudioAmberLight, fontSize = 11.sp)
                        }
                        IconButton(onClick = {
                            coroutineScope.launch {
                                backend.deleteAlbum(album.id).onSuccess {
                                    Toast.makeText(context, "Album deleted", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = StudioRed)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 7. USER MANAGEMENT SCREEN
// ==========================================

@Composable
fun StudioUsersScreen(backend: AuraStudioBackend) {
    val users by backend.users.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("User Management", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = StudioTextPrimary)
        Text("View active accounts and toggle account status", fontSize = 12.sp, color = StudioTextSecondary)
        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(users, key = { it.id }) { user ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = StudioSurface),
                    border = BorderStroke(1.dp, StudioCardBorder)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0xFF2A2A38)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = StudioAmber)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.username.ifBlank { "User" }, color = StudioTextPrimary, fontWeight = FontWeight.Bold)
                            Text(user.email, color = StudioTextSecondary, fontSize = 12.sp)
                            Text("Role: ${user.role}", color = if (user.role == "ADMIN") StudioAmber else StudioTextMuted, fontSize = 11.sp)
                        }
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    backend.toggleUserSuspension(user.id, !user.isSuspended).onSuccess {
                                        Toast.makeText(context, "User status updated", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (user.isSuspended) StudioGreen else StudioRed
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(if (user.isSuspended) "Enable" else "Disable", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// Edit dialog helper
@Composable
fun EditSongDialog(
    song: StudioSong,
    onDismiss: () -> Unit,
    onSave: (StudioSong) -> Unit
) {
    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist) }
    var genre by remember { mutableStateOf(song.genre) }
    var description by remember { mutableStateOf(song.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Metadata", color = StudioTextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(value = artist, onValueChange = { artist = it }, label = { Text("Artist") }, singleLine = true)
                OutlinedTextField(value = genre, onValueChange = { genre = it }, label = { Text("Genre") }, singleLine = true)
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(song.copy(title = title, artist = artist, genre = genre, description = description))
                },
                colors = ButtonDefaults.buttonColors(containerColor = StudioAmber, contentColor = Color.Black)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = StudioTextSecondary) }
        },
        containerColor = StudioSurfaceVariant
    )
}
