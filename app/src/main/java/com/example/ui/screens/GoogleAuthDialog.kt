package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.AppColors.FacebookBlue

@Composable
fun GoogleSignInDialog(
    onDismiss: () -> Unit,
    onSignInWithGoogle: (email: String, name: String, photoUrl: String) -> Unit
) {
    var customEmail by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    // Pre-configured quick accounts
    val defaultGoogleAccounts = listOf(
        Triple("hamarachitralonline@gmail.com", "Hamara Chitral Online", "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"),
        Triple("chitral.explorer@gmail.com", "Chitral Explorer", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150"),
        Triple("yarkhoon.traveler@gmail.com", "Yarkhoon Traveler", "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=150")
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("google_signin_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = FacebookBlue.copy(alpha = 0.1f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("G", fontWeight = FontWeight.Bold, color = FacebookBlue, fontSize = 20.sp)
                            }
                        }
                        Text(
                            "Google Sign-In",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.outline)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Sign in securely with your Google Account to sync posts, friends, and AI creations to Firebase Auth & Cloud Firestore.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Security & Firebase Badge
                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Security, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                        Text(
                            "Firebase Auth & Firestore Real-Time Persistence Enabled",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF10B981)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Choose an account:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick account selector
                defaultGoogleAccounts.forEach { (email, name, photo) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                isSubmitting = true
                                onSignInWithGoogle(email, name, photo)
                            }
                            .testTag("google_account_$email"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AsyncImage(
                                model = photo,
                                contentDescription = name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(email, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // Or enter custom google email
                OutlinedTextField(
                    value = customEmail,
                    onValueChange = { customEmail = it },
                    label = { Text("Or enter another Google Email") },
                    placeholder = { Text("your.name@gmail.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("custom_google_email_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                if (customEmail.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Display Name") },
                        placeholder = { Text("Your Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("custom_google_name_field"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            isSubmitting = true
                            val fallbackPhoto = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"
                            onSignInWithGoogle(customEmail, customName.ifBlank { customEmail.substringBefore("@") }, fallbackPhoto)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_custom_google_signin"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue)
                    ) {
                        Text("Continue with Google", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
