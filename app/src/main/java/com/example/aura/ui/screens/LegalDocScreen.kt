package com.example.aura.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalDocScreen(
    title: String,
    type: String, // "PRIVACY", "TERMS", "COPYRIGHT"
    onBack: () -> Unit
) {
    val textContent = when (type) {
        "PRIVACY" -> """
            AURA PRIVACY POLICY
            Last Updated: September 2026

            1. Information We Collect
            When you register for AURA, we collect your email address, username, and encrypted password credentials. During playback, we record anonymous streaming telemetry and listening statistics to provide accurate recommendation queues and artist royalty analytics.

            2. How Information is Used
            Your account information is used strictly to provide your streaming experience, synchronize playlists and liked songs across devices, and manage your account settings. We do not sell your personal information to third parties.

            3. Local Storage & Downloads
            For audio tracks where offline caching is explicitly permitted by the rights holder, AURA stores encrypted audio files within the application's isolated internal storage directory. You can delete all cached audio at any time via Settings > Clear Download Cache.

            4. Advertising & Google AdMob
            AURA displays non-intrusive banner advertisements served via the Google Mobile Ads SDK. Google AdMob may use device advertising identifiers in accordance with Google Play Developer Program Policies to deliver non-personalized or personalized ads based on your system consent.

            5. Account & Data Deletion
            In compliance with Google Play Developer Policies, users may permanently delete their account and all associated personal data directly within the application under Settings > Account > Delete Account, or by contacting privacy@aura.io.
        """.trimIndent()

        "TERMS" -> """
            AURA TERMS & CONDITIONS
            Last Updated: September 2026

            1. Acceptance of Terms
            By accessing or using AURA, you agree to be bound by these Terms and Conditions. If you do not agree, you must immediately discontinue use of the platform.

            2. Streaming License
            All music content, recordings, album art, and metadata available on AURA are licensed for personal, non-commercial streaming purposes only. You may not rebroadcast, reverse-engineer, decompile, extract, or redistribute audio files without explicit authorization.

            3. User Conduct
            Users may create playlists and personalize profiles. Any user attempting unauthorized downloads of restricted tracks, scraping catalog data, or uploading malicious content will have their account immediately suspended.

            4. Disclaimer & Warranties
            AURA provides high-fidelity audio streaming on an "as is" and "as available" basis. While we strive for lossless fidelity and minimal latency, uninterrupted service cannot be guaranteed across varying cellular network conditions.

            5. Contact
            For legal inquiries, contact legal@aura.io.
        """.trimIndent()

        "COPYRIGHT" -> """
            AURA COPYRIGHT POLICY & DMCA COMPLIANCE
            Last Updated: September 2026

            1. Respect for Intellectual Property
            AURA strictly respects the intellectual property rights of musical artists, composers, publishers, and record labels. All songs hosted on the AURA platform are uploaded and managed through the authenticated AURA Studio administration console with explicit rights attribution.

            2. DMCA Notice and Takedown Procedure
            If you are a copyright owner or an authorized agent thereof and believe that any content hosted on AURA infringes upon your copyright, you may submit a formal notification pursuant to the Digital Millennium Copyright Act (17 U.S.C. § 512(c)) by providing our designated agent with the following information:
            - Identification of the copyrighted work claimed to have been infringed;
            - Identification of the material that is claimed to be infringing, including Song Title, Artist Name, and AURA Share URL;
            - Your contact information (Full legal name, email address, physical address, and telephone number);
            - A statement of good faith belief that the disputed use is not authorized by the copyright owner, its agent, or the law;
            - A statement under penalty of perjury that the information in the notification is accurate and that you are authorized to act on behalf of the owner;
            - A physical or electronic signature.

            Designated DMCA Agent:
            AURA Copyright Operations
            Email: dmca@aura.io / support@aura.io

            3. In-App Content Reporting
            Users and rights holders can also report tracks directly inside the app on any Song Details page by tapping "Report Content or Copyright Issue".
        """.trimIndent()

        else -> "Document not found."
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, color = AuraTextPrimary, fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = textContent,
                color = AuraTextSecondary,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
