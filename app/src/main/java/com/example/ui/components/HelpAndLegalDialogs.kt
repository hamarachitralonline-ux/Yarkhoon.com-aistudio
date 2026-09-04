package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R

private val FacebookBlue = Color(0xFF1877F2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    currentDarkMode: Boolean?,
    onSetDarkMode: (Boolean?) -> Unit,
    onDismiss: () -> Unit
) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var friendRequestAlerts by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }
    var dataSaver by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Settings & Preferences", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Appearance Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Filled.DarkMode, contentDescription = null, tint = FacebookBlue)
                                Text("Display & Theme", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = currentDarkMode == null,
                                    onClick = { onSetDarkMode(null) },
                                    label = { Text("System", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = currentDarkMode == false,
                                    onClick = { onSetDarkMode(false) },
                                    label = { Text("Light", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = currentDarkMode == true,
                                    onClick = { onSetDarkMode(true) },
                                    label = { Text("Dark", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Notifications Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Filled.Notifications, contentDescription = null, tint = FacebookBlue)
                                Text("Notifications", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Push Notifications", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    Text("Receive updates about posts and stories", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                }
                                Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Friend Request Alerts", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    Text("Notify when someone sends a friend request", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                }
                                Switch(checked = friendRequestAlerts, onCheckedChange = { friendRequestAlerts = it })
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("In-App Sounds", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    Text("Play sounds for messages and reactions", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                }
                                Switch(checked = soundEnabled, onCheckedChange = { soundEnabled = it })
                            }
                        }
                    }

                    // Media & Data
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Filled.DataSaverOn, contentDescription = null, tint = FacebookBlue)
                                Text("Media & Network", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Data Saver Mode", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    Text("Compress photos and limit auto-playing videos", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                }
                                Switch(checked = dataSaver, onCheckedChange = { dataSaver = it })
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Help & Support", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Frequently Asked Questions", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FacebookBlue)
                            Spacer(modifier = Modifier.height(12.dp))

                            FAQItem("How do 24-Hour Stories work?", "Stories you upload remain visible on your profile and on the home tray for 24 hours before automatically expiring.")
                            FAQItem("How do I post in Marketplace?", "Tap the Menu or the '+' Create button at the top header and choose 'Marketplace Listing'. Fill in your price, contact, and upload photos.")
                            FAQItem("How do I list a service in Yarkhoon?", "Select 'Service Listing' from the Create menu. You can list transport, trades, tuition, and skilled services across Chitral.")
                            FAQItem("Can I zoom into profile pictures?", "Yes! Tap on any profile photo or avatar anywhere in the app to view it in full-screen high-resolution with pinch-to-zoom.")
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Contact Community Team", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FacebookBlue)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Have suggestions, need account assistance, or want to report inappropriate content?", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Email: support@yarkhoon.com", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text("Community Hotline: +92-345-YARKHOON", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text("Chitral & Yarkhoon Valley, KP, Pakistan", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FAQItem(question: String, answer: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(question, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
        if (expanded) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(answer, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
        }
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutYarkhoonDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("About Yarkhoon.com", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.yarkhoon_logo),
                        contentDescription = "Yarkhoon Logo",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Yarkhoon.com",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = FacebookBlue
                    )
                    Text("Version 2.4.0 (Redesign)", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(20.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Our Mission", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FacebookBlue)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Yarkhoon.com is dedicated to empowering and connecting the people of Yarkhoon Valley, Chitral, and the global Khowar diaspora. " +
                                "By blending high-performance social networking, 24-hour stories, community groups, local marketplace commerce, and essential services directories, " +
                                "we bring our vibrant culture, news, and economy together.",
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Key Highlights", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FacebookBlue)
                            Spacer(modifier = Modifier.height(8.dp))
                            BulletItem("24-Hour Stories & Rich Media Sharing")
                            BulletItem("High-Resolution Zoom for Profile Pictures")
                            BulletItem("Local Marketplace for Chitral Crafts & Goods")
                            BulletItem("Directory of Skilled Craftsmen & Local Services")
                            BulletItem("Real-time Messaging & Community Groups")
                            BulletItem("Built with Modern Material 3 & Jetpack Compose")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("© 2026 Yarkhoon.com. All rights reserved.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
private fun BulletItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(FacebookBlue)
        )
        Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyTermsDialog(title: String, isPrivacy: Boolean, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(title, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    if (isPrivacy) {
                        Text("Privacy Policy for Yarkhoon.com", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = FacebookBlue)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Last updated: August 2026", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(16.dp))

                        LegalSection("1. Information We Collect", "We collect information you provide directly, including your name, username, profile photo, bio, phone numbers for marketplace listings, and media you share via stories and posts.")
                        LegalSection("2. How We Protect Your Data", "Your authentication credentials and communications are safeguarded using industry-standard Firebase authentication and Firestore encryption protocols.")
                        LegalSection("3. Story Media Expiry", "Stories automatically expire and are purged after 24 hours of publication to ensure your temporary moments remain ephemeral.")
                        LegalSection("4. Your Choices & Controls", "You have full control over editing your profile, removing your marketplace listings, and deleting your posts at any time.")
                    } else {
                        Text("Terms of Service", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = FacebookBlue)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Last updated: August 2026", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(16.dp))

                        LegalSection("1. Community Guidelines", "Yarkhoon.com is a respectful community space. Harassment, hateful speech, and fraudulent commercial listings are strictly prohibited.")
                        LegalSection("2. Marketplace & Services Listings", "Sellers and service providers are solely responsible for the authenticity and quality of goods/services offered. Please exercise standard safety when meeting locally.")
                        LegalSection("3. Content Ownership", "You retain all rights to the photos, stories, and text you post on Yarkhoon.com.")
                    }
                }
            }
        }
    }
}

@Composable
private fun LegalSection(heading: String, body: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(heading, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(4.dp))
        Text(body, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
