package com.example.aura.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aura.backend.AuraSharedBackend
import com.example.aura.model.Song
import com.example.ui.theme.*

@Composable
fun ContentReportDialog(
    song: Song?,
    backend: AuraSharedBackend,
    userEmail: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var reporterEmail by remember { mutableStateOf(userEmail ?: "") }
    var selectedReason by remember { mutableStateOf("Copyright Infringement") }
    var details by remember { mutableStateOf("") }
    var reportType by remember { mutableStateOf("COPYRIGHT") }
    var isSubmitted by remember { mutableStateOf(false) }

    val reasons = listOf(
        "Copyright Infringement" to "COPYRIGHT",
        "Inappropriate Content / Explicit Lyrics" to "INAPPROPRIATE",
        "Incorrect Artist / Metadata" to "METADATA",
        "Corrupted / Distorted Audio" to "AUDIO_QUALITY",
        "Other Violation" to "OTHER"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isSubmitted) "Report Received" else "Report Content",
                color = AuraTextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (isSubmitted) {
                Column {
                    Text(
                        text = "Thank you. Your report has been dispatched to the AURA moderation team and logged in the shared backend for administrative review.",
                        color = AuraMint,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            } else {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (song != null) {
                        Text(
                            text = "Reporting: \"${song.title}\" by ${song.artist}",
                            color = AuraCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Text("Select reason for report:", color = AuraTextMuted, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    reasons.forEach { (label, type) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedReason = label
                                    reportType = type
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedReason == label),
                                onClick = {
                                    selectedReason = label
                                    reportType = type
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(label, color = AuraTextPrimary, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = reporterEmail,
                        onValueChange = { reporterEmail = it },
                        label = { Text("Your Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = details,
                        onValueChange = { details = it },
                        label = { Text("Details / Explanation") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (isSubmitted) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AuraCyan, contentColor = androidx.compose.ui.graphics.Color.Black)
                ) {
                    Text("Close")
                }
            } else {
                Button(
                    onClick = {
                        if (reporterEmail.isBlank()) {
                            Toast.makeText(context, "Please enter your email", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        backend.submitReport(
                            songId = song?.id ?: "general",
                            songTitle = song?.title ?: "General Report",
                            reporterEmail = reporterEmail.trim(),
                            reason = selectedReason,
                            details = details.trim(),
                            type = reportType
                        )
                        isSubmitted = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraCyan, contentColor = androidx.compose.ui.graphics.Color.Black)
                ) {
                    Text("Submit Report")
                }
            }
        },
        dismissButton = {
            if (!isSubmitted) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = AuraTextMuted)
                }
            }
        },
        containerColor = AuraSurfaceVariant
    )
}
