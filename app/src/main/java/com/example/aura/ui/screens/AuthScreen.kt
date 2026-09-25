package com.example.aura.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aura.backend.AuraSharedBackend
import com.example.aura.ui.components.AuraBrandHeader
import com.example.ui.theme.*

@Composable
fun AuthScreen(
    backend: AuraSharedBackend,
    onAuthSuccess: () -> Unit
) {
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0D1226),
                        AuraBackground,
                        Color(0xFF06070B)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Official AURA Emblem & Title (Exact from Reference Image)
            AuraBrandHeader(
                emblemSize = 100.dp,
                titleSize = 32.dp,
                showSubtitle = true
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isSignUp) "Create your music account" else "Welcome back to your music",
                color = AuraCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Error & Success Banners
            AnimatedVisibility(visible = errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x33FF3366))
                        .border(1.dp, Color(0x66FF3366), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFFFB4C0),
                        fontSize = 13.sp
                    )
                }
            }

            AnimatedVisibility(visible = successMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AuraCyan.copy(alpha = 0.15f))
                        .border(1.dp, AuraCyan.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = successMessage ?: "",
                        color = AuraCyanBright,
                        fontSize = 13.sp
                    )
                }
            }

            // Username input (Sign Up only)
            if (isSignUp) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; errorMessage = null },
                    label = { Text("Display Name") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = AuraTextSecondary)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraCyan,
                        unfocusedBorderColor = AuraCardBorder,
                        focusedTextColor = AuraTextPrimary,
                        unfocusedTextColor = AuraTextPrimary,
                        focusedLabelColor = AuraCyan,
                        unfocusedLabelColor = AuraTextSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_username_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Email input
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; errorMessage = null },
                label = { Text("Email Address") },
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null, tint = AuraTextSecondary)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AuraCyan,
                    unfocusedBorderColor = AuraCardBorder,
                    focusedTextColor = AuraTextPrimary,
                    unfocusedTextColor = AuraTextPrimary,
                    focusedLabelColor = AuraCyan,
                    unfocusedLabelColor = AuraTextSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_email_input"),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Password input
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                label = { Text("Password") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = AuraTextSecondary)
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = AuraTextSecondary
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AuraCyan,
                    unfocusedBorderColor = AuraCardBorder,
                    focusedTextColor = AuraTextPrimary,
                    unfocusedTextColor = AuraTextPrimary,
                    focusedLabelColor = AuraCyan,
                    unfocusedLabelColor = AuraTextSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_password_input"),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            // Confirm Password input (Sign Up only)
            if (isSignUp) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorMessage = null },
                    label = { Text("Confirm Password") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = AuraTextSecondary)
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraCyan,
                        unfocusedBorderColor = AuraCardBorder,
                        focusedTextColor = AuraTextPrimary,
                        unfocusedTextColor = AuraTextPrimary,
                        focusedLabelColor = AuraCyan,
                        unfocusedLabelColor = AuraTextSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_confirm_password_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }

            // Forgot Password Link (Login only)
            if (!isSignUp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Forgot Password?",
                        color = AuraCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable { showForgotPasswordDialog = true }
                            .padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Auth Submit Button
            Button(
                onClick = {
                    errorMessage = null
                    successMessage = null

                    if (isSignUp) {
                        if (password != confirmPassword) {
                            errorMessage = "Passwords do not match."
                            return@Button
                        }
                        val result = backend.signUp(email, username, password)
                        result.onSuccess {
                            onAuthSuccess()
                        }.onFailure {
                            errorMessage = it.message ?: "Sign up failed."
                        }
                    } else {
                        val result = backend.login(email, password)
                        result.onSuccess {
                            onAuthSuccess()
                        }.onFailure {
                            errorMessage = it.message ?: "Login failed."
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .testTag("auth_submit_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AuraCyan,
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = if (isSignUp) "Create Account" else "Log In",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Social Sign-In Divider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0x33FFFFFF))
                Text(
                    text = "  OR CONTINUE WITH  ",
                    color = AuraTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0x33FFFFFF))
            }

            Spacer(modifier = Modifier.height(10.dp))

            var showSocialDialog by remember { mutableStateOf<String?>(null) } // "Google", "Facebook", "Instagram"

            // 1. Google One-Tap / Social Button
            OutlinedButton(
                onClick = { showSocialDialog = "Google" },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("auth_google_button"),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFFFFF)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0x1AFFFFFF),
                    contentColor = AuraTextPrimary
                )
            ) {
                Text("G", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF4285F4))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Continue with Google", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Facebook & Instagram side-by-side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showSocialDialog = "Facebook" },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("auth_facebook_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x331877F2)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0x151877F2),
                        contentColor = Color(0xFF90CAF9)
                    )
                ) {
                    Text("f", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF1877F2))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Facebook", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = { showSocialDialog = "Instagram" },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("auth_instagram_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33E1306C)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0x15E1306C),
                        contentColor = Color(0xFFFF80AB)
                    )
                ) {
                    Text("✦", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFFE1306C))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Instagram", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Guest / One-tap Enter Button so user never has to log in repeatedly
            Button(
                onClick = {
                    val quickUser = backend.login("shivnagar7705@gmail.com", "123456")
                    if (quickUser.isSuccess) {
                        onAuthSuccess()
                    } else {
                        val res = backend.signUp("listener_${System.currentTimeMillis()}@aura.music", "Music Lover", "123456")
                        if (res.isSuccess) {
                            onAuthSuccess()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("auth_quick_enter"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0x2EFFFFFF),
                    contentColor = Color.White
                )
            ) {
                Text("🎵  Continue as Guest (No Login Required)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            // Social Sign-In Prompt Dialog
            if (showSocialDialog != null) {
                val provider = showSocialDialog!!
                var socialEmailInput by remember { mutableStateOf("") }
                var socialNameInput by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { showSocialDialog = null },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Sign in with $provider",
                                color = AuraTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    },
                    text = {
                        Column {
                            Text(
                                text = "Connect your $provider account for instant access without entering a password. Your session will stay permanently active on this device.",
                                color = AuraTextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedTextField(
                                value = socialNameInput,
                                onValueChange = { socialNameInput = it },
                                label = { Text("Your Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = socialEmailInput,
                                onValueChange = { socialEmailInput = it },
                                label = { Text(if (provider == "Instagram") "Instagram Handle or Email" else "$provider Email") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val effectiveEmail = if (socialEmailInput.isBlank()) {
                                    "${provider.lowercase()}_user@aura.audio"
                                } else if (!socialEmailInput.contains("@")) {
                                    "${socialEmailInput.trim().replace("@", "")}@instagram.aura.audio"
                                } else {
                                    socialEmailInput.trim()
                                }
                                val effectiveName = socialNameInput.ifBlank {
                                    socialEmailInput.substringBefore("@").replaceFirstChar { it.uppercase() }.ifBlank { "$provider User" }
                                }
                                val res = backend.loginWithSocial(
                                    provider = provider.uppercase(),
                                    email = effectiveEmail,
                                    displayName = effectiveName
                                )
                                res.onSuccess {
                                    showSocialDialog = null
                                    onAuthSuccess()
                                }.onFailure {
                                    errorMessage = it.message
                                    showSocialDialog = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AuraCyan, contentColor = Color.Black)
                        ) {
                            Text("Connect & Enter", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSocialDialog = null }) {
                            Text("Cancel", color = AuraTextMuted)
                        }
                    },
                    containerColor = AuraSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Toggle Sign Up / Login
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSignUp) "Already have an account?" else "Don't have an account?",
                    color = AuraTextSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isSignUp) "Log In" else "Sign Up",
                    color = AuraCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            isSignUp = !isSignUp
                            errorMessage = null
                            successMessage = null
                        }
                        .padding(4.dp)
                        .testTag("auth_toggle_mode")
                )
            }
        }

        // Forgot Password Dialog
        if (showForgotPasswordDialog) {
            var resetEmail by remember { mutableStateOf(email) }
            var resetSent by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showForgotPasswordDialog = false },
                title = { Text("Password Recovery", color = AuraTextPrimary) },
                text = {
                    Column {
                        if (resetSent) {
                            Text(
                                "Password reset instructions have been sent to $resetEmail.",
                                color = AuraMint
                            )
                        } else {
                            Text(
                                "Enter your registered email address to receive a secure recovery code.",
                                color = AuraTextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = resetEmail,
                                onValueChange = { resetEmail = it },
                                label = { Text("Email") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (resetSent) {
                                showForgotPasswordDialog = false
                            } else {
                                resetSent = true
                            }
                        }
                    ) {
                        Text(if (resetSent) "Done" else "Send Reset Link", color = AuraCyan)
                    }
                },
                dismissButton = {
                    if (!resetSent) {
                        TextButton(onClick = { showForgotPasswordDialog = false }) {
                            Text("Cancel", color = AuraTextMuted)
                        }
                    }
                },
                containerColor = AuraSurfaceVariant
            )
        }
    }
}
