package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.AppColors.FacebookBlue
import com.example.data.User

@Composable
fun MenuScreen(
    currentUser: User?,
    savedPostsCount: Int,
    marketplaceItemsCount: Int,
    servicesCount: Int,
    groupsCount: Int,
    pagesCount: Int = 0,
    isUserAdmin: Boolean,
    isOffline: Boolean = false,
    isSimulatedOfflineMode: Boolean = false,
    cachedPostsCount: Int = 0,
    cachedUsersCount: Int = 0,
    onToggleSimulatedOffline: () -> Unit = {},
    onNavigateToProfile: () -> Unit,
    onNavigateToMarketplace: () -> Unit,
    onNavigateToServices: () -> Unit,
    onNavigateToSavedPosts: () -> Unit,
    onNavigateToGroups: () -> Unit,
    onNavigateToPages: () -> Unit = {},
    onNavigateToChat: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToAiStudio: () -> Unit = {},
    onNavigateToGeminiChat: () -> Unit = {},
    onNavigateToLiveVoice: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenHelpSupport: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenTerms: () -> Unit,
    onLogout: () -> Unit
) {
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var isSettingsExpanded by remember { mutableStateOf(true) }
    var isHelpExpanded by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("more_menu_scroll_view"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. User Profile Header Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToProfile)
                    .testTag("menu_profile_header_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AsyncImage(
                        model = currentUser?.avatarUrl?.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150" },
                        contentDescription = currentUser?.fullName ?: "Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = currentUser?.fullName ?: "Yarkhoon User",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (currentUser?.isVerified == true || currentUser?.id == "admin") {
                                Icon(
                                    imageVector = Icons.Filled.Verified,
                                    contentDescription = "Verified",
                                    tint = FacebookBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = "@${currentUser?.username ?: "user"}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "View your profile",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FacebookBlue
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Go to Profile",
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // ==================== YARKHOON AI STUDIO & GEMINI SUITE ====================
        // Hidden as requested: Live Voice, Gemini Chat, Media Studio (code preserved)
        val showAiFeatures = false
        if (showAiFeatures) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = FacebookBlue, modifier = Modifier.size(20.dp))
                                Text("Yarkhoon AI & Media Studio", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = FacebookBlue.copy(alpha = 0.12f)
                            ) {
                                Text("Gemini + Veo + Lyria", color = FacebookBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // AI Media Studio
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onNavigateToAiStudio() }
                                    .testTag("menu_open_ai_studio_btn"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = FacebookBlue, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Media Studio", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Image, Music & Veo Video", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline, maxLines = 1)
                                }
                            }

                            // Gemini Chatbot
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onNavigateToGeminiChat() }
                                    .testTag("menu_open_gemini_chat_btn"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Icon(Icons.Filled.Psychology, contentDescription = null, tint = Color(0xFF4285F4), modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Gemini Chat", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Search Grounded Assistant", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline, maxLines = 1)
                                }
                            }

                            // Live Voice API
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onNavigateToLiveVoice() }
                                    .testTag("menu_open_live_voice_btn"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Live Voice", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Real-time Audio Talk", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }


        // Room Local Database & Offline Cache Management
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("menu_offline_cache_card"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOffline) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isOffline) Icons.Filled.CloudOff else Icons.Filled.Storage,
                                contentDescription = "Local Cache",
                                tint = if (isOffline) MaterialTheme.colorScheme.tertiary else FacebookBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Room Database & Local Cache",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = if (isOffline) "Offline Browsing Mode Active" else "Local Cache Synchronized",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isOffline) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                            else FacebookBlue.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = if (isOffline) "OFFLINE" else "ONLINE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOffline) MaterialTheme.colorScheme.tertiary else FacebookBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Cached Posts", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("$cachedPostsCount posts", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Cached Profiles", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("$cachedUsersCount profiles", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Simulate Offline Mode",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                "Browse cached feed posts and user profiles without internet",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Switch(
                            checked = isSimulatedOfflineMode,
                            onCheckedChange = { onToggleSimulatedOffline() },
                            modifier = Modifier.testTag("simulate_offline_toggle")
                        )
                    }
                }
            }
        }

        // 2. All Shortcuts Section
        item {
            Text(
                text = "All Shortcuts",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }

        // Grid of primary shortcut tiles
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Marketplace Tile
                    MenuShortcutTile(
                        icon = Icons.Filled.Storefront,
                        iconColor = Color(0xFF45BD62),
                        title = "Marketplace",
                        subtitle = "Buy & sell in Chitral",
                        badge = if (marketplaceItemsCount > 0) "$marketplaceItemsCount items" else null,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToMarketplace,
                        testTag = "menu_item_marketplace"
                    )

                    // Services Tile
                    MenuShortcutTile(
                        icon = Icons.Filled.Build,
                        iconColor = Color(0xFFF7B928),
                        title = "Services",
                        subtitle = "Find local trades & help",
                        badge = if (servicesCount > 0) "$servicesCount active" else null,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToServices,
                        testTag = "menu_item_services"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Groups Tile
                    MenuShortcutTile(
                        icon = Icons.Filled.Groups,
                        iconColor = Color(0xFF1877F2),
                        title = "Groups",
                        subtitle = "Villages & communities",
                        badge = if (groupsCount > 0) "$groupsCount groups" else null,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToGroups,
                        testTag = "menu_item_groups"
                    )

                    // Pages Tile
                    MenuShortcutTile(
                        icon = Icons.Filled.Layers,
                        iconColor = Color(0xFF0288D1),
                        title = "Pages",
                        subtitle = "News, media & brands",
                        badge = if (pagesCount > 0) "$pagesCount pages" else null,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToPages,
                        testTag = "menu_item_pages"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Saved Posts & Bookmarks Tile
                    MenuShortcutTile(
                        icon = Icons.Filled.Bookmark,
                        iconColor = Color(0xFFE91E63),
                        title = "Saved Posts",
                        subtitle = "Bookmarks & favorites",
                        badge = if (savedPostsCount > 0) "$savedPostsCount saved" else null,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToSavedPosts,
                        testTag = "menu_item_saved_posts"
                    )

                    // Chat / Messages Tile
                    MenuShortcutTile(
                        icon = Icons.Filled.ChatBubble,
                        iconColor = Color(0xFF0084FF),
                        title = "Messages",
                        subtitle = "Direct chats & friends",
                        badge = "Chat",
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToChat,
                        testTag = "menu_item_messages"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Admin Dashboard (if admin) or Help
                    if (isUserAdmin) {
                        MenuShortcutTile(
                            icon = Icons.Filled.Shield,
                            iconColor = Color(0xFFD32F2F),
                            title = "Admin Tools",
                            subtitle = "Moderate & broadcast",
                            badge = "Admin",
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToAdmin,
                            testTag = "menu_item_admin"
                        )
                    } else {
                        MenuShortcutTile(
                            icon = Icons.Filled.HelpOutline,
                            iconColor = Color(0xFF7B1FA2),
                            title = "Help & Support",
                            subtitle = "FAQs & community team",
                            badge = null,
                            modifier = Modifier.weight(1f),
                            onClick = onOpenHelpSupport,
                            testTag = "menu_item_help_tile"
                        )
                    }
                }
            }
        }

        // 3. Settings & Privacy Accordion / Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isSettingsExpanded = !isSettingsExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = FacebookBlue.copy(alpha = 0.12f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Settings, contentDescription = null, tint = FacebookBlue, modifier = Modifier.size(20.dp))
                                }
                            }
                            Text("Settings & Privacy", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Icon(
                            if (isSettingsExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }

                    AnimatedVisibility(visible = isSettingsExpanded) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            MenuListItem(
                                icon = Icons.Outlined.Tune,
                                title = "Preferences & Dark Mode",
                                subtitle = "Theme, notifications, sounds & data saver",
                                onClick = onOpenSettings,
                                testTag = "menu_settings_btn"
                            )
                            MenuListItem(
                                icon = Icons.Outlined.Lock,
                                title = "Privacy Policy",
                                subtitle = "How your information is protected",
                                onClick = onOpenPrivacyPolicy,
                                testTag = "menu_privacy_btn"
                            )
                            MenuListItem(
                                icon = Icons.Outlined.Description,
                                title = "Terms of Service",
                                subtitle = "Rules and guidelines for Yarkhoon.com",
                                onClick = onOpenTerms,
                                testTag = "menu_terms_btn"
                            )
                        }
                    }
                }
            }
        }

        // 4. Help & Community Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isHelpExpanded = !isHelpExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF0084FF).copy(alpha = 0.12f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.SupportAgent, contentDescription = null, tint = Color(0xFF0084FF), modifier = Modifier.size(20.dp))
                                }
                            }
                            Text("Help & Community", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Icon(
                            if (isHelpExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }

                    AnimatedVisibility(visible = isHelpExpanded) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            MenuListItem(
                                icon = Icons.Outlined.HelpCenter,
                                title = "Help Center & FAQs",
                                subtitle = "Guides on posting, marketplace & accounts",
                                onClick = onOpenHelpSupport,
                                testTag = "menu_help_center_btn"
                            )
                            MenuListItem(
                                icon = Icons.Outlined.Info,
                                title = "About Yarkhoon.com",
                                subtitle = "Connecting Chitral & Yarkhoon diaspora",
                                onClick = onOpenAbout,
                                testTag = "menu_about_btn"
                            )
                        }
                    }
                }
            }
        }

        // 5. Log Out Button
        item {
            Button(
                onClick = { showLogoutConfirmDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    contentColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("menu_logout_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Logout, contentDescription = "Log Out")
                    Text("Log Out", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Yarkhoon.com · Connecting Chitral & Diaspora",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }

    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            icon = {
                Icon(Icons.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            title = {
                Text("Log Out of Yarkhoon.com?")
            },
            text = {
                Text("Are you sure you want to log out? You can log back in anytime with your username or email.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Log Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MenuShortcutTile(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    badge: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(115.dp)
            .clickable(onClick = onClick)
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = iconColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = iconColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                if (badge != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = iconColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = iconColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun MenuListItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp)
        )
    }
}
