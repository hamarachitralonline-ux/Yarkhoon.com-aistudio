package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AppColors.FacebookBlue
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordDialog(
    onDismiss: () -> Unit,
    onSendResetEmail: (emailOrUsername: String, onResult: (Boolean, String, String?) -> Unit) -> Unit,
    onVerifyAndResetPassword: (emailOrUsername: String, code: String, newPassword: String, onResult: (Boolean, String) -> Unit) -> Unit,
    initialEmail: String = "",
    suggestedEmails: List<String> = emptyList(),
    onPasswordResetSuccess: ((email: String) -> Unit)? = null
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0: Send Email, 1: Enter Code & New Password
    var emailInput by rememberSaveable { mutableStateOf(initialEmail) }
    var recoveryCodeInput by rememberSaveable { mutableStateOf("") }
    var newPasswordInput by rememberSaveable { mutableStateOf("") }
    var confirmPasswordInput by rememberSaveable { mutableStateOf("") }
    
    var showNewPassword by rememberSaveable { mutableStateOf(false) }
    var showConfirmPassword by rememberSaveable { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var lastSentEmail by rememberSaveable { mutableStateOf("") }
    var receivedSandboxCode by rememberSaveable { mutableStateOf<String?>(null) }

    // Resend countdown timer
    var resendCountdown by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(resendCountdown) {
        if (resendCountdown > 0) {
            delay(1000L)
            resendCountdown -= 1
        }
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 480.dp)
                .wrapContentHeight()
                .testTag("forgot_password_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Top Row with Back/Close and Brand Lock Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE7F3FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockReset,
                                contentDescription = "Password Recovery",
                                tint = FacebookBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Account Recovery",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Firebase Auth Password Reset",
                                fontSize = 12.sp,
                                color = FacebookBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("close_forgot_password_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Navigation Tabs between Email Link and Direct Code Setup
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = FacebookBlue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { 
                            selectedTab = 0 
                            statusMessage = null
                        },
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("tab_email_reset"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Email Link", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )

                    Tab(
                        selected = selectedTab == 1,
                        onClick = { 
                            selectedTab = 1 
                            statusMessage = null
                        },
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("tab_code_reset"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Enter Code", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Status / Feedback Banner
                AnimatedVisibility(
                    visible = statusMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    statusMessage?.let { msg ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isError) Color(0xFFFDE8E8) else Color(0xFFDEF7EC),
                            border = BorderStroke(1.dp, if (isError) Color(0xFFF98080) else Color(0xFF31C48D))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isError) Color(0xFFE02424) else Color(0xFF0E9F6E),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = msg,
                                    fontSize = 12.sp,
                                    color = if (isError) Color(0xFF9B1C1C) else Color(0xFF03543F),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // ==================== TAB 0: SEND RESET EMAIL ====================
                if (selectedTab == 0) {
                    Text(
                        text = "Enter your registered email address or username. Firebase Authentication will dispatch a secure link to reset your password.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { 
                            emailInput = it
                            if (isError) statusMessage = null
                        },
                        label = { Text("Email Address or Username") },
                        placeholder = { Text("e.g., ali@yarkhoon.com or user@gmail.com") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email",
                                tint = FacebookBlue
                            )
                        },
                        trailingIcon = {
                            if (emailInput.isNotEmpty()) {
                                IconButton(onClick = { emailInput = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                if (emailInput.isNotBlank() && !isLoading && resendCountdown == 0) {
                                    isLoading = true
                                    statusMessage = null
                                    onSendResetEmail(emailInput) { success, msg, code ->
                                        isLoading = false
                                        isError = !success
                                        statusMessage = msg
                                        if (success) {
                                            lastSentEmail = emailInput.trim()
                                            receivedSandboxCode = code
                                            resendCountdown = 60
                                        }
                                    }
                                }
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("forgot_password_email_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Suggested / Registered demo accounts
                    val demoAccounts = remember(suggestedEmails) {
                        val list = mutableListOf("ali@yarkhoon.com", "ceo@yarkhoon.com")
                        suggestedEmails.forEach { if (!list.contains(it) && it.isNotBlank()) list.add(it) }
                        list
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Quick Demo Accounts:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(demoAccounts) { account ->
                            SuggestionChip(
                                onClick = {
                                    emailInput = account
                                    statusMessage = null
                                },
                                label = { Text(account, fontSize = 11.sp) },
                                shape = RoundedCornerShape(16.dp),
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }

                    // Sandbox Dev Recovery Code Card
                    if (receivedSandboxCode != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Instant Sandbox Code",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF1D4ED8)
                                    )
                                    Text(
                                        text = receivedSandboxCode ?: "",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        letterSpacing = 2.sp,
                                        color = Color(0xFF1E40AF)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Firebase Auth email has been dispatched. You can also use this 6-digit code to reset your password immediately.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF1E3A8A)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        recoveryCodeInput = receivedSandboxCode ?: ""
                                        selectedTab = 1
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                        .testTag("autofill_sandbox_code_button"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                                ) {
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Auto-fill & Set New Password", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Action Button: Send Email
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            if (emailInput.isBlank()) {
                                isError = true
                                statusMessage = "Please enter an email address or username."
                                return@Button
                            }
                            isLoading = true
                            statusMessage = null
                            onSendResetEmail(emailInput) { success, msg, code ->
                                isLoading = false
                                isError = !success
                                statusMessage = msg
                                if (success) {
                                    lastSentEmail = emailInput.trim()
                                    receivedSandboxCode = code
                                    resendCountdown = 60
                                }
                            }
                        },
                        enabled = !isLoading && resendCountdown == 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("send_reset_link_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FacebookBlue,
                            contentColor = Color.White
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Dispatching via Firebase...")
                        } else if (resendCountdown > 0) {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resend available in ${resendCountdown}s")
                        } else {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (lastSentEmail.isNotEmpty()) "Resend Reset Email" else "Send Reset Email",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Already have a 6-digit recovery code?",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = { 
                                selectedTab = 1
                                statusMessage = null
                            },
                            modifier = Modifier.testTag("switch_to_code_entry_button")
                        ) {
                            Text("Enter Code", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FacebookBlue)
                        }
                    }
                }

                // ==================== TAB 1: ENTER CODE & SET NEW PASSWORD ====================
                if (selectedTab == 1) {
                    Text(
                        text = "Enter your email, the 6-digit verification code sent from Firebase/Yarkhoon, and your new password.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Registered Email / Username") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = "Email", tint = FacebookBlue)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_password_email_field"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = recoveryCodeInput,
                        onValueChange = { 
                            if (it.length <= 6) recoveryCodeInput = it.filter { ch -> ch.isDigit() }
                        },
                        label = { Text("6-Digit Recovery Code") },
                        placeholder = { Text("e.g. 123456 or 786786") },
                        leadingIcon = {
                            Icon(Icons.Default.Pin, contentDescription = "Code", tint = FacebookBlue)
                        },
                        trailingIcon = {
                            if (receivedSandboxCode != null && recoveryCodeInput != receivedSandboxCode) {
                                TextButton(
                                    onClick = { recoveryCodeInput = receivedSandboxCode ?: "" },
                                    modifier = Modifier.testTag("paste_code_button")
                                ) {
                                    Text("Paste Code", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FacebookBlue)
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recovery_code_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newPasswordInput,
                        onValueChange = { newPasswordInput = it },
                        label = { Text("New Password (min 6 characters)") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = "New Password", tint = FacebookBlue)
                        },
                        trailingIcon = {
                            IconButton(onClick = { showNewPassword = !showNewPassword }) {
                                Icon(
                                    imageVector = if (showNewPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showNewPassword) "Hide password" else "Show password"
                                )
                            }
                        },
                        visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_password_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = confirmPasswordInput,
                        onValueChange = { confirmPasswordInput = it },
                        label = { Text("Confirm New Password") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = "Confirm Password", tint = FacebookBlue)
                        },
                        trailingIcon = {
                            IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                Icon(
                                    imageVector = if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showConfirmPassword) "Hide password" else "Show password"
                                )
                            }
                        },
                        visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_password_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()

                            if (emailInput.isBlank()) {
                                isError = true
                                statusMessage = "Please enter your email or username."
                                return@Button
                            }
                            if (recoveryCodeInput.length < 6) {
                                isError = true
                                statusMessage = "Please enter the complete 6-digit recovery code."
                                return@Button
                            }
                            if (newPasswordInput.length < 6) {
                                isError = true
                                statusMessage = "New password must be at least 6 characters."
                                return@Button
                            }
                            if (newPasswordInput != confirmPasswordInput) {
                                isError = true
                                statusMessage = "Passwords do not match. Please verify."
                                return@Button
                            }

                            isLoading = true
                            statusMessage = null

                            onVerifyAndResetPassword(emailInput, recoveryCodeInput, newPasswordInput) { success, msg ->
                                isLoading = false
                                isError = !success
                                statusMessage = msg
                                if (success) {
                                    onPasswordResetSuccess?.invoke(emailInput.trim())
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_new_password_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0F9D58),
                            contentColor = Color.White
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Updating Password...")
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Update Password", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(
                        onClick = { 
                            selectedTab = 0 
                            statusMessage = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("back_to_email_link_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp), tint = FacebookBlue)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Back to Email Reset Link", color = FacebookBlue, fontWeight = FontWeight.Medium)
                    }
                }

                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Bottom security reassurance footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Security",
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Secured by Google Firebase Authentication",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
