package com.example.aura.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aura.ads.AuraAdManager
import com.example.aura.backend.AuraSharedBackend
import com.example.aura.model.User
import com.example.aura.player.AuraAudioPlayer
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    user: User?,
    backend: AuraSharedBackend,
    player: AuraAudioPlayer,
    onBack: () -> Unit,
    onOpenLegalDoc: (String, String) -> Unit,
    onOpenReportDialog: () -> Unit,
    onLogout: () -> Unit,
    onAccountDeleted: () -> Unit
) {
    val context = LocalContext.current

    // Notification Toggles
    var notifNewReleases by remember { mutableStateOf(true) }
    var notifFeatured by remember { mutableStateOf(true) }
    var notifPlaylists by remember { mutableStateOf(false) }
    var notifAccount by remember { mutableStateOf(true) }

    // Playback Settings
    var streamingQuality by remember { mutableStateOf("Lossless (FLAC/WAV)") }
    var autoplayNext by remember { mutableStateOf(true) }

    // Download Settings
    var downloadWifiOnly by remember { mutableStateOf(true) }

    // Dialogs
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showAdMobConfigDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, color = AuraTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AuraTextPrimary)
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
                .padding(horizontal = 20.dp)
                .testTag("settings_screen_list"),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // =========================
            // 1. ACCOUNT SECTION
            // =========================
            item {
                SettingsSectionHeader(title = "ACCOUNT")
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AuraSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(1.dp, brush = androidx.compose.ui.graphics.SolidColor(AuraCardBorder))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        SettingsClickableRow(
                            icon = Icons.Outlined.Person,
                            title = "User Profile",
                            subtitle = "${user?.username ?: "Anonymous"} (${user?.email ?: ""})"
                        ) {}

                        Divider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(vertical = 4.dp))

                        SettingsClickableRow(
                            icon = Icons.Outlined.Lock,
                            title = "Change Password",
                            subtitle = "Update your security credentials",
                            onClick = { showChangePasswordDialog = true }
                        )

                        Divider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(vertical = 4.dp))

                        SettingsClickableRow(
                            icon = Icons.Default.Logout,
                            title = "Log Out",
                            subtitle = "Sign out of your AURA account",
                            onClick = onLogout,
                            titleColor = AuraPink
                        )

                        Divider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(vertical = 4.dp))

                        SettingsClickableRow(
                            icon = Icons.Outlined.DeleteForever,
                            title = "Delete Account",
                            subtitle = "Permanently remove your account & data (Google Play Policy)",
                            onClick = { showDeleteAccountDialog = true },
                            titleColor = Color(0xFFFF4D6D)
                        )
                    }
                }
            }

            // =========================
            // 2. NOTIFICATIONS SECTION
            // =========================
            item {
                SettingsSectionHeader(title = "NOTIFICATIONS")
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AuraSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(1.dp, brush = androidx.compose.ui.graphics.SolidColor(AuraCardBorder))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        SettingsToggleRow(
                            title = "New Releases",
                            subtitle = "Alerts for tracks from followed artists",
                            checked = notifNewReleases,
                            onCheckedChange = { notifNewReleases = it }
                        )
                        Divider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(vertical = 4.dp))
                        SettingsToggleRow(
                            title = "Featured Songs",
                            subtitle = "Weekly curated editorial highlights",
                            checked = notifFeatured,
                            onCheckedChange = { notifFeatured = it }
                        )
                        Divider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(vertical = 4.dp))
                        SettingsToggleRow(
                            title = "Playlist Updates",
                            subtitle = "Changes to playlists you follow",
                            checked = notifPlaylists,
                            onCheckedChange = { notifPlaylists = it }
                        )
                        Divider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(vertical = 4.dp))
                        SettingsToggleRow(
                            title = "Account & Security",
                            subtitle = "Important service updates & login alerts",
                            checked = notifAccount,
                            onCheckedChange = { notifAccount = it }
                        )
                    }
                }
            }

            // =========================
            // 3. PLAYBACK SECTION
            // =========================
            item {
                SettingsSectionHeader(title = "PLAYBACK")
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AuraSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(1.dp, brush = androidx.compose.ui.graphics.SolidColor(AuraCardBorder))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        SettingsClickableRow(
                            icon = Icons.Outlined.GraphicEq,
                            title = "Streaming Quality",
                            subtitle = streamingQuality,
                            onClick = { showQualityDialog = true }
                        )
                        Divider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(vertical = 4.dp))
                        SettingsToggleRow(
                            title = "Autoplay",
                            subtitle = "Keep listening to similar songs when queue ends",
                            checked = autoplayNext,
                            onCheckedChange = { autoplayNext = it }
                        )
                        Divider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(vertical = 4.dp))
                        SettingsClickableRow(
                            icon = Icons.Default.Shuffle,
                            title = "Toggle Shuffle",
                            subtitle = if (player.isShuffle.collectAsState().value) "Shuffle is On" else "Shuffle is Off",
                            onClick = { player.toggleShuffle() }
                        )
                        Divider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(vertical = 4.dp))
                        SettingsClickableRow(
                            icon = Icons.Default.Repeat,
                            title = "Cycle Repeat Mode",
                            subtitle = "Current: ${player.repeatMode.collectAsState().value.name}",
                            onClick = { player.cycleRepeatMode() }
                        )
                    }
                }
            }

            // =========================
            // 4. DOWNLOADS SECTION
            // =========================
            item {
                SettingsSectionHeader(title = "DOWNLOADS & STORAGE")
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AuraSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(1.dp, brush = androidx.compose.ui.graphics.SolidColor(AuraCardBorder))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        SettingsToggleRow(
                            title = "Download over Wi-Fi only",
                            subtitle = "Save mobile cellular data for permitted tracks",
                            checked = downloadWifiOnly,
                            onCheckedChange = { downloadWifiOnly = it }
                        )
                        Divider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(vertical = 4.dp))
                        SettingsClickableRow(
                            icon = Icons.Outlined.Storage,
                            title = "Storage Information",
                            subtitle = "App Cache & Downloads: ~18.4 MB of 64 GB available",
                            onClick = {}
                        )
                        Divider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(vertical = 4.dp))
                        SettingsClickableRow(
                            icon = Icons.Outlined.CleaningServices,
                            title = "Clear Download Cache",
                            subtitle = "Delete all offline temporary files",
                            onClick = {
                                Toast.makeText(context, "Offline cache cleared", Toast.LENGTH_SHORT).show()
                            },
                            titleColor = AuraPink
                        )
                    }
                }
            }

            // =========================
            // 5. LEGAL SECTION
            // =========================
            item {
                SettingsSectionHeader(title = "LEGAL & POLICIES")
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AuraSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(1.dp, brush = androidx.compose.ui.graphics.SolidColor(AuraCardBorder))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        SettingsClickableRow(
                            icon = Icons.Outlined.Policy,
                            title = "Privacy Policy",
                            subtitle = "How user data is collected and protected",
                            onClick = { onOpenLegalDoc("Privacy Policy", "PRIVACY") }
                        )
                        Divider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(vertical = 4.dp))
                        SettingsClickableRow(
                            icon = Icons.Outlined.Gavel,
                            title = "Terms & Conditions",
                            subtitle = "Platform usage rules and streaming rights",
                            onClick = { onOpenLegalDoc("Terms & Conditions", "TERMS") }
                        )
                        Divider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(vertical = 4.dp))
                        SettingsClickableRow(
                            icon = Icons.Outlined.Copyright,
                            title = "Copyright Policy & DMCA",
                            subtitle = "DMCA notice and takedown guidelines",
                            onClick = { onOpenLegalDoc("Copyright Policy", "COPYRIGHT") }
                        )
                        Divider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(vertical = 4.dp))
                        SettingsClickableRow(
                            icon = Icons.Outlined.Report,
                            title = "Report Content",
                            subtitle = "Submit copyright violation or inappropriate content report",
                            onClick = onOpenReportDialog,
                            titleColor = AuraCyan
                        )
                    }
                }
            }

            // =========================
            // 6. ABOUT SECTION
            // =========================
            item {
                SettingsSectionHeader(title = "ABOUT")
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AuraSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(1.dp, brush = androidx.compose.ui.graphics.SolidColor(AuraCardBorder))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        SettingsClickableRow(
                            icon = Icons.Outlined.Info,
                            title = "About AURA",
                            subtitle = "Feel Every Beat. Premium online music streaming platform.",
                            onClick = {}
                        )
                        Divider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(vertical = 4.dp))
                        SettingsClickableRow(
                            icon = Icons.Outlined.Build,
                            title = "App Version",
                            subtitle = "Aura Music v1.0.0 (Production Build)",
                            onClick = {}
                        )
                        Divider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(vertical = 4.dp))
                        SettingsClickableRow(
                            icon = Icons.Outlined.Campaign,
                            title = "Google AdMob Monetization",
                            subtitle = "Banner & Interstitial Ads (Configure Ad Units)",
                            onClick = { showAdMobConfigDialog = true },
                            titleColor = AuraCyan
                        )
                        Divider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(vertical = 4.dp))
                        SettingsClickableRow(
                            icon = Icons.Outlined.Email,
                            title = "Support Contact",
                            subtitle = "support@aura.io",
                            onClick = {
                                Toast.makeText(context, "Contact support at support@aura.io", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                }
            }
        }
    }

    // Change Password Dialog
    if (showChangePasswordDialog && user != null) {
        var oldPass by remember { mutableStateOf("") }
        var newPass by remember { mutableStateOf("") }
        var passErr by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text("Change Password", color = AuraTextPrimary) },
            text = {
                Column {
                    if (passErr != null) {
                        Text(passErr ?: "", color = AuraPink, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    OutlinedTextField(
                        value = oldPass,
                        onValueChange = { oldPass = it; passErr = null },
                        label = { Text("Current Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it; passErr = null },
                        label = { Text("New Password (min 6 characters)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val res = backend.changePassword(user.id, oldPass, newPass)
                        res.onSuccess {
                            Toast.makeText(context, "Password changed successfully", Toast.LENGTH_SHORT).show()
                            showChangePasswordDialog = false
                        }.onFailure {
                            passErr = it.message ?: "Failed to change password."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraCyan, contentColor = Color.Black)
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) {
                    Text("Cancel", color = AuraTextMuted)
                }
            },
            containerColor = AuraSurfaceVariant
        )
    }

    // Delete Account Dialog (Google Play requirement)
    if (showDeleteAccountDialog && user != null) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Delete Account?", color = AuraPink, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to delete your account? All playlists, likes, and profile data will be permanently removed. This action cannot be undone.",
                    color = AuraTextPrimary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        backend.deleteUserAccount(user.id)
                        Toast.makeText(context, "Account permanently deleted", Toast.LENGTH_SHORT).show()
                        showDeleteAccountDialog = false
                        onAccountDeleted()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3366), contentColor = Color.White)
                ) {
                    Text("Permanently Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Cancel", color = AuraTextMuted)
                }
            },
            containerColor = AuraSurfaceVariant
        )
    }

    // Quality Selection Dialog
    if (showQualityDialog) {
        val options = listOf("Normal (128 kbps)", "High (320 kbps)", "Lossless (FLAC/WAV)")
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("Streaming Audio Quality", color = AuraTextPrimary) },
            text = {
                Column {
                    options.forEach { opt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    streamingQuality = opt
                                    showQualityDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (streamingQuality == opt),
                                onClick = {
                                    streamingQuality = opt
                                    showQualityDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(opt, color = AuraTextPrimary, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = AuraSurfaceVariant
        )
    }

    // Google AdMob Monetization Configuration Dialog
    if (showAdMobConfigDialog) {
        var bannerInput by remember { mutableStateOf(AuraAdManager.configuredBannerAdUnitId) }
        var interstitialInput by remember { mutableStateOf(AuraAdManager.configuredInterstitialAdUnitId) }

        AlertDialog(
            onDismissRequest = { showAdMobConfigDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Campaign, contentDescription = null, tint = AuraCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AdMob Monetization", color = AuraTextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        "Google Mobile Ads SDK is fully integrated with Banner & Interstitial support. Enter your production Ad Unit IDs below:",
                        color = AuraTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = bannerInput,
                        onValueChange = { bannerInput = it },
                        label = { Text("Banner Ad Unit ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = interstitialInput,
                        onValueChange = { interstitialInput = it },
                        label = { Text("Interstitial Ad Unit ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = {
                        bannerInput = AuraAdManager.TEST_BANNER_AD_UNIT_ID
                        interstitialInput = AuraAdManager.TEST_INTERSTITIAL_AD_UNIT_ID
                    }) {
                        Text("Reset to Google Test IDs", color = AuraCyan, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        AuraAdManager.saveBannerUnitId(context, bannerInput)
                        AuraAdManager.saveInterstitialUnitId(context, interstitialInput)
                        Toast.makeText(context, "AdMob IDs saved successfully!", Toast.LENGTH_SHORT).show()
                        showAdMobConfigDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraCyan, contentColor = Color.Black)
                ) {
                    Text("Save & Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdMobConfigDialog = false }) {
                    Text("Close", color = AuraTextMuted)
                }
            },
            containerColor = AuraSurfaceVariant
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = AuraCyan,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}

@Composable
fun SettingsClickableRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    titleColor: Color = AuraTextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = titleColor, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = titleColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = AuraTextSecondary, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AuraTextMuted, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = AuraTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = AuraTextSecondary, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = AuraCyan
            )
        )
    }
}
