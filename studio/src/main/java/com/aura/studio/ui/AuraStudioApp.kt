package com.aura.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.studio.backend.AuraStudioBackend

enum class StudioTab(val label: String) {
    DASHBOARD("Dashboard"),
    SONGS("Songs"),
    UPLOAD("Upload"),
    ARTISTS("Artists"),
    ALBUMS("Albums"),
    USERS("Users")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuraStudioApp() {
    val context = LocalContext.current
    val backend = remember { AuraStudioBackend.getInstance(context) }
    val currentAdmin by backend.currentAdmin.collectAsState()
    var currentTab by remember { mutableStateOf(StudioTab.DASHBOARD) }
    var showRulesDialog by remember { mutableStateOf(false) }

    if (showRulesDialog) {
        FirebaseRulesGuideDialog(
            backend = backend,
            onDismiss = { showRulesDialog = false }
        )
    }

    AuraStudioTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(StudioBackground)
        ) {
            if (currentAdmin == null) {
                // Secure Admin Login
                StudioAuthScreen(backend = backend)
            } else {
                // Authenticated Admin Dashboard
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AuraStudioImageLogo(size = 32.dp, animated = true)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "AURA STUDIO",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 17.sp,
                                            letterSpacing = 1.sp,
                                            color = StudioTextPrimary
                                        )
                                        Text(
                                            text = "Admin: ${currentAdmin?.fullName}",
                                            fontSize = 11.sp,
                                            color = StudioAmberLight
                                        )
                                    }
                                }
                            },
                            actions = {
                                IconButton(onClick = { showRulesDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.CloudQueue,
                                        contentDescription = "Firebase Setup & Rules",
                                        tint = StudioAmberLight
                                    )
                                }
                                IconButton(onClick = { backend.logout() }) {
                                    Icon(
                                        imageVector = Icons.Default.Logout,
                                        contentDescription = "Logout",
                                        tint = StudioRed
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = StudioSurface,
                                titleContentColor = StudioTextPrimary
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = StudioSurface,
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = currentTab == StudioTab.DASHBOARD,
                                onClick = { currentTab = StudioTab.DASHBOARD },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                                label = { Text("Dashboard") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = StudioAmber,
                                    indicatorColor = StudioAmber,
                                    unselectedIconColor = StudioTextMuted,
                                    unselectedTextColor = StudioTextMuted
                                )
                            )

                            NavigationBarItem(
                                selected = currentTab == StudioTab.SONGS,
                                onClick = { currentTab = StudioTab.SONGS },
                                icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Songs") },
                                label = { Text("Songs") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = StudioAmber,
                                    indicatorColor = StudioAmber,
                                    unselectedIconColor = StudioTextMuted,
                                    unselectedTextColor = StudioTextMuted
                                )
                            )

                            NavigationBarItem(
                                selected = currentTab == StudioTab.UPLOAD,
                                onClick = { currentTab = StudioTab.UPLOAD },
                                icon = { Icon(Icons.Default.CloudUpload, contentDescription = "Upload") },
                                label = { Text("Upload") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = StudioAmber,
                                    indicatorColor = StudioAmber,
                                    unselectedIconColor = StudioTextMuted,
                                    unselectedTextColor = StudioTextMuted
                                )
                            )

                            NavigationBarItem(
                                selected = currentTab == StudioTab.ARTISTS,
                                onClick = { currentTab = StudioTab.ARTISTS },
                                icon = { Icon(Icons.Default.Person, contentDescription = "Artists") },
                                label = { Text("Artists") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = StudioAmber,
                                    indicatorColor = StudioAmber,
                                    unselectedIconColor = StudioTextMuted,
                                    unselectedTextColor = StudioTextMuted
                                )
                            )

                            NavigationBarItem(
                                selected = currentTab == StudioTab.USERS,
                                onClick = { currentTab = StudioTab.USERS },
                                icon = { Icon(Icons.Default.Group, contentDescription = "Users") },
                                label = { Text("Users") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = StudioAmber,
                                    indicatorColor = StudioAmber,
                                    unselectedIconColor = StudioTextMuted,
                                    unselectedTextColor = StudioTextMuted
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            StudioTab.DASHBOARD -> StudioDashboardScreen(
                                backend = backend,
                                onNavigateTab = { currentTab = it }
                            )
                            StudioTab.SONGS -> StudioSongsScreen(backend = backend)
                            StudioTab.UPLOAD -> StudioUploadSongScreen(
                                backend = backend,
                                onUploaded = { currentTab = StudioTab.SONGS }
                            )
                            StudioTab.ARTISTS -> StudioArtistsScreen(backend = backend)
                            StudioTab.ALBUMS -> StudioAlbumsScreen(backend = backend)
                            StudioTab.USERS -> StudioUsersScreen(backend = backend)
                        }
                    }
                }
            }
        }
    }
}
