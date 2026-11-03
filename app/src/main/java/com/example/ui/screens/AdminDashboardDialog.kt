package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.*
import com.example.ui.theme.FbBlue
import com.example.ui.theme.VerifiedBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardDialog(
    reports: List<ContentReport>,
    auditLogs: List<ModerationAuditLog>,
    isLoading: Boolean,
    errorMessage: String?,
    diagnosticInfo: String? = null,
    onRefresh: () -> Unit,
    onDismissReport: (reportId: String, reason: String) -> Unit,
    onHideContent: (reportId: String?, contentType: ReportContentType, contentId: String, reason: String) -> Unit,
    onRestoreContent: (contentType: ReportContentType, contentId: String, reason: String) -> Unit,
    onSuspendUser: (userId: String, durationDays: Int, reason: String, reportId: String?) -> Unit,
    onReinstateUser: (userId: String, reason: String) -> Unit,
    onDismiss: () -> Unit,
    // Extended 15-Section Admin Control Panel Parameters (with defaults for backward compatibility)
    adminRole: AdminRole = AdminRole.NONE,
    currentUser: User = SampleData.sampleCurrentUser,
    activeSection: AdminControlPanelSection = AdminControlPanelSection.DASHBOARD,
    onSelectSection: (AdminControlPanelSection) -> Unit = {},
    stats: AdminDashboardStats = AdminDashboardStats(),
    registeredUsers: List<User> = emptyList(),
    allPosts: List<Post> = emptyList(),
    verificationRequests: List<VerificationRequest> = emptyList(),
    villages: List<YarkhoonVillage> = emptyList(),
    officialAnnouncements: List<OfficialAnnouncement> = emptyList(),
    curatedTrendingTopics: List<CuratedTrendingTopic> = emptyList(),
    adminAccounts: List<AdminAccountRecord> = emptyList(),
    sessionStartedAt: Long = 0L,
    onApprovePost: (postId: String, reason: String) -> Unit = { _, _ -> },
    onRejectPost: (postId: String, reason: String) -> Unit = { _, _ -> },
    onTogglePinPost: (postId: String, isPinned: Boolean) -> Unit = { _, _ -> },
    onFeaturePost: (postId: String, isFeatured: Boolean, durationDays: Int, delayHours: Int) -> Unit = { _, _, _, _ -> },
    onToggleTrendingPost: (postId: String, isTrending: Boolean) -> Unit = { _, _ -> },
    onPromotePost: (postId: String, isPromoted: Boolean, durationDays: Int, category: String, location: String) -> Unit = { _, _, _, _, _ -> },
    onSetUserVerification: (userId: String, isVerified: Boolean, reason: String) -> Unit = { _, _, _ -> },
    onReviewVerificationRequest: (requestId: String, userId: String, approve: Boolean, notes: String) -> Unit = { _, _, _, _ -> },
    onSaveTrendingTopic: (id: String?, tag: String, headline: String, category: String, villageName: String, isPinned: Boolean) -> Unit = { _, _, _, _, _, _ -> },
    onDeleteTrendingTopic: (String) -> Unit = {},
    onSaveVillage: (id: String?, name: String, tehsil: String, district: String, description: String, population: String) -> Unit = { _, _, _, _, _, _ -> },
    onDeleteVillage: (id: String, name: String) -> Unit = { _, _ -> },
    onPublishAnnouncement: (title: String, body: String, category: String, targetLocation: String, priority: String) -> Unit = { _, _, _, _, _ -> },
    onAssignAdminRole: (uid: String, email: String, name: String, role: AdminRole, permissions: List<String>) -> Unit = { _, _, _, _, _ -> },
    onRevokeAdminRole: (uid: String, reason: String) -> Unit = { _, _ -> },
    onViewUserProfile: (User) -> Unit = {},
    onAdminLogout: () -> Unit = {}
) {
    var currentSection by remember(activeSection) { mutableStateOf(activeSection) }
    var isMenuDrawerExpanded by remember { mutableStateOf(false) }

    val selectSection: (AdminControlPanelSection) -> Unit = { sec ->
        currentSection = sec
        onSelectSection(sec)
        isMenuDrawerExpanded = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("admin_dashboard_dialog"),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_launcher_foreground_asset),
                                    contentDescription = "YARKHOON.COM Logo",
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                                Text(
                                    text = "Yarkhoon Admin Control Panel",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (adminRole.canAccessAdminPanel) FbBlue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = adminRole.displayName.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (adminRole.canAccessAdminPanel) FbBlue else MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                                Text(
                                    text = "• ${currentSection.title}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_admin_dashboard_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { isMenuDrawerExpanded = !isMenuDrawerExpanded },
                            modifier = Modifier.testTag("toggle_admin_menu_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "All 15 Sections")
                        }
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.testTag("refresh_admin_dashboard_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            ) {
                // Horizontal scrollable 15-section navigation bar
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("admin_sections_nav_bar"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AdminControlPanelSection.entries) { section ->
                        val isSelected = currentSection == section
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectSection(section) },
                            label = {
                                Text(
                                    text = section.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (section == AdminControlPanelSection.LOGOUT) MaterialTheme.colorScheme.error else FbBlue,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("admin_nav_chip_${section.name.lowercase()}")
                        )
                    }
                }

                // Expandable quick-jump grid of all 15 sections when Menu icon is tapped
                if (isMenuDrawerExpanded) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("admin_all_sections_drawer"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Jump to Control Panel Section (15 Modules)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = FbBlue
                            )
                            AdminControlPanelSection.entries.chunked(3).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    rowItems.forEach { sec ->
                                        OutlinedButton(
                                            onClick = { selectSection(sec) },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(sec.title, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = FbBlue
                    )
                }

                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (!diagnosticInfo.isNullOrBlank() || adminRole == AdminRole.NONE) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("admin_runtime_diagnostics_card"),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Runtime Authorization Diagnostic Telemetry",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = diagnosticInfo ?: "• Current Status: Role evaluated as ${adminRole.displayName} (Ordinary User).\n• Target Owner: isBdCPBYupN4SDJZ1yRiSZ8K0FF2 (hamarachitralonline@gmail.com)\n• Root Cause: Production Firestore rules have not been deployed or adminRoles document does not exist on live server.",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Section Content Router (All 15 Required Sections)
                Box(modifier = Modifier.fillMaxSize()) {
                    when (currentSection) {
                        AdminControlPanelSection.DASHBOARD -> {
                            DashboardOverviewSection(
                                stats = stats,
                                adminRole = adminRole,
                                onNavigateToSection = selectSection
                            )
                        }
                        AdminControlPanelSection.USER_MANAGEMENT -> {
                            UserManagementSection(
                                adminRole = adminRole,
                                users = registeredUsers,
                                reports = reports,
                                onSuspendUser = onSuspendUser,
                                onReinstateUser = onReinstateUser,
                                onSetUserVerification = onSetUserVerification,
                                onViewUserProfile = onViewUserProfile
                            )
                        }
                        AdminControlPanelSection.POST_MANAGEMENT -> {
                            PostManagementSection(
                                adminRole = adminRole,
                                posts = allPosts,
                                onApprovePost = onApprovePost,
                                onRejectPost = onRejectPost,
                                onRestorePost = { postId, reason ->
                                    onRestoreContent(ReportContentType.POST, postId, reason)
                                }
                            )
                        }
                        AdminControlPanelSection.VIRAL_POSTS -> {
                            ViralPostsSection(
                                adminRole = adminRole,
                                posts = allPosts,
                                onTogglePinPost = onTogglePinPost,
                                onToggleTrendingPost = onToggleTrendingPost,
                                onFeaturePost = onFeaturePost
                            )
                        }
                        AdminControlPanelSection.FEATURED_POSTS -> {
                            FeaturedPostsSection(
                                adminRole = adminRole,
                                posts = allPosts,
                                onFeaturePost = onFeaturePost
                            )
                        }
                        AdminControlPanelSection.BLUE_TICK_VERIFICATION -> {
                            BlueTickVerificationSection(
                                adminRole = adminRole,
                                verificationRequests = verificationRequests,
                                users = registeredUsers,
                                onReviewRequest = onReviewVerificationRequest,
                                onSetUserVerification = onSetUserVerification
                            )
                        }
                        AdminControlPanelSection.PROMOTED_POSTS -> {
                            PromotedPostsSection(
                                adminRole = adminRole,
                                posts = allPosts,
                                onPromotePost = onPromotePost
                            )
                        }
                        AdminControlPanelSection.TRENDING_TOPICS -> {
                            TrendingTopicsSection(
                                adminRole = adminRole,
                                posts = allPosts,
                                curatedTopics = curatedTrendingTopics,
                                onSaveTopic = onSaveTrendingTopic,
                                onDeleteTopic = onDeleteTrendingTopic,
                                onTogglePostTrending = onToggleTrendingPost
                            )
                        }
                        AdminControlPanelSection.VILLAGE_MANAGEMENT -> {
                            VillageManagementSection(
                                adminRole = adminRole,
                                villages = villages,
                                onSaveVillage = onSaveVillage,
                                onDeleteVillage = onDeleteVillage
                            )
                        }
                        AdminControlPanelSection.REPORTS_AND_COMPLAINTS -> {
                            ReportsModerationSubTab(
                                reports = reports,
                                onDismissReport = onDismissReport,
                                onHideContent = onHideContent,
                                onSuspendUser = onSuspendUser
                            )
                        }
                        AdminControlPanelSection.NOTIFICATIONS -> {
                            OfficialNotificationsSection(
                                adminRole = adminRole,
                                announcements = officialAnnouncements,
                                onPublishAnnouncement = onPublishAnnouncement
                            )
                        }
                        AdminControlPanelSection.ADMIN_MANAGEMENT -> {
                            AdminManagementSection(
                                adminRole = adminRole,
                                adminAccounts = adminAccounts,
                                registeredUsers = registeredUsers,
                                onAssignAdminRole = onAssignAdminRole,
                                onRevokeAdminRole = onRevokeAdminRole
                            )
                        }
                        AdminControlPanelSection.ACTIVITY_LOGS -> {
                            AuditLogsSubTab(
                                auditLogs = auditLogs,
                                onRestoreContent = onRestoreContent,
                                onReinstateUser = onReinstateUser
                            )
                        }
                        AdminControlPanelSection.SETTINGS -> {
                            AdminSettingsSection(
                                adminRole = adminRole,
                                currentUser = currentUser,
                                sessionStartedAt = sessionStartedAt,
                                onNavigateToLogout = { selectSection(AdminControlPanelSection.LOGOUT) }
                            )
                        }
                        AdminControlPanelSection.LOGOUT -> {
                            AdminLogoutConfirmationSection(
                                currentUser = currentUser,
                                adminRole = adminRole,
                                onConfirmLogout = {
                                    onDismiss()
                                    onAdminLogout()
                                },
                                onCancel = { selectSection(AdminControlPanelSection.DASHBOARD) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SECTION 1: REAL-TIME ADMIN DASHBOARD OVERVIEW
// ==========================================
@Composable
private fun DashboardOverviewSection(
    stats: AdminDashboardStats,
    adminRole: AdminRole,
    onNavigateToSection: (AdminControlPanelSection) -> Unit
) {
    val statMetrics = listOf(
        Triple("Total Registered Users", stats.totalRegisteredUsers.toString(), AdminControlPanelSection.USER_MANAGEMENT),
        Triple("Active Users", stats.activeUsers.toString(), AdminControlPanelSection.USER_MANAGEMENT),
        Triple("Total Posts", stats.totalPosts.toString(), AdminControlPanelSection.POST_MANAGEMENT),
        Triple("Pending Posts", stats.pendingPosts.toString(), AdminControlPanelSection.POST_MANAGEMENT),
        Triple("Approved Posts", stats.approvedPosts.toString(), AdminControlPanelSection.POST_MANAGEMENT),
        Triple("Rejected / Hidden Posts", stats.rejectedPosts.toString(), AdminControlPanelSection.POST_MANAGEMENT),
        Triple("Total Verified Accounts", stats.totalVerifiedAccounts.toString(), AdminControlPanelSection.BLUE_TICK_VERIFICATION),
        Triple("Total Featured Posts", stats.totalFeaturedPosts.toString(), AdminControlPanelSection.FEATURED_POSTS),
        Triple("Total Promoted Posts", stats.totalPromotedPosts.toString(), AdminControlPanelSection.PROMOTED_POSTS),
        Triple("Trending Posts", stats.trendingPostsCount.toString(), AdminControlPanelSection.VIRAL_POSTS),
        Triple("Pending Verifications", stats.pendingVerificationRequests.toString(), AdminControlPanelSection.BLUE_TICK_VERIFICATION),
        Triple("Reported Content", stats.reportedContentCount.toString(), AdminControlPanelSection.REPORTS_AND_COMPLAINTS)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_section_dashboard"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Yarkhoon Live Database Telemetry",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "All 12 administrative indicators below are computed directly from live Cloud Firestore collections. Tap any metric card to open its management module.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        items(statMetrics.chunked(2)) { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { (label, value, targetSection) ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToSection(targetSection) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = value,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = FbBlue
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SECTION 2: USER MANAGEMENT
// ==========================================
@Composable
private fun UserManagementSection(
    adminRole: AdminRole,
    users: List<User>,
    reports: List<ContentReport>,
    onSuspendUser: (userId: String, durationDays: Int, reason: String, reportId: String?) -> Unit,
    onReinstateUser: (userId: String, reason: String) -> Unit,
    onSetUserVerification: (userId: String, isVerified: Boolean, reason: String) -> Unit,
    onViewUserProfile: (User) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf("ALL") } // ALL, REPORTED, SUSPENDED, VERIFIED
    var suspendTargetUser by remember { mutableStateOf<User?>(null) }
    var suspendReason by remember { mutableStateOf("") }
    var suspendDays by remember { mutableIntStateOf(7) }

    val reportedUserIds = remember(reports) {
        reports.mapNotNull { it.reportedUserId }.toSet()
    }

    val filteredUsers = remember(users, searchQuery, filterMode, reportedUserIds) {
        users.filter { u ->
            val matchesQuery = searchQuery.isBlank() ||
                u.name.contains(searchQuery, ignoreCase = true) ||
                u.email.contains(searchQuery, ignoreCase = true) ||
                u.handle.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (filterMode) {
                "REPORTED" -> reportedUserIds.contains(u.id)
                "SUSPENDED" -> u.isSuspended
                "VERIFIED" -> u.isVerified
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_section_user_management"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search users by name, email, or @handle") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_user_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ALL" to "All (${users.size})", "REPORTED" to "Reported", "SUSPENDED" to "Suspended", "VERIFIED" to "Verified").forEach { (code, label) ->
                    FilterChip(
                        selected = filterMode == code,
                        onClick = { filterMode = code },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }
        }

        items(filteredUsers, key = { it.id }) { u ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(u.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                if (u.isVerified) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Verified Blue Tick",
                                        tint = VerifiedBadge,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                if (u.isSuspended) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.errorContainer
                                    ) {
                                        Text(
                                            text = "SUSPENDED",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text("${u.handle} • ${u.email.ifBlank { "No email on file" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Posts: ${u.postsCount} • Followers: ${u.followersCount}", style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = { onViewUserProfile(u) }) {
                            Text("Profile")
                        }
                    }

                    if (adminRole.canManageUsers) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (u.isSuspended) {
                                Button(
                                    onClick = { onReinstateUser(u.id, "Account activated by administrator") },
                                    modifier = Modifier.weight(1f).testTag("activate_user_btn_${u.id}"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
                                ) {
                                    Text("Activate Account", fontSize = 11.sp)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { suspendTargetUser = u },
                                    modifier = Modifier.weight(1f).testTag("suspend_user_btn_${u.id}"),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Suspend", fontSize = 11.sp)
                                }
                            }

                            if (adminRole.canManageVerification) {
                                OutlinedButton(
                                    onClick = {
                                        onSetUserVerification(
                                            u.id,
                                            !u.isVerified,
                                            if (!u.isVerified) "Blue Tick granted from User Management" else "Blue Tick removed from User Management"
                                        )
                                    },
                                    modifier = Modifier.weight(1f).testTag("toggle_verify_user_btn_${u.id}")
                                ) {
                                    Text(if (u.isVerified) "Remove Blue Tick" else "Grant Blue Tick", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    suspendTargetUser?.let { target ->
        AlertDialog(
            onDismissRequest = { suspendTargetUser = null },
            title = { Text("Suspend ${target.name}?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select suspension duration and record the policy reason:")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1, 7, 30, 0).forEach { d ->
                            FilterChip(
                                selected = suspendDays == d,
                                onClick = { suspendDays = d },
                                label = { Text(if (d == 0) "Permanent" else "${d}d") }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = suspendReason,
                        onValueChange = { suspendReason = it },
                        label = { Text("Suspension Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSuspendUser(target.id, suspendDays, suspendReason.ifBlank { "Community policy violation" }, null)
                        suspendReason = ""
                        suspendTargetUser = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Suspension")
                }
            },
            dismissButton = {
                TextButton(onClick = { suspendTargetUser = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ==========================================
// SECTION 3: POST MANAGEMENT (APPROVE / REJECT / RESTORE)
// ==========================================
@Composable
private fun PostManagementSection(
    adminRole: AdminRole,
    posts: List<Post>,
    onApprovePost: (postId: String, reason: String) -> Unit,
    onRejectPost: (postId: String, reason: String) -> Unit,
    onRestorePost: (postId: String, reason: String) -> Unit
) {
    var statusFilter by remember { mutableStateOf("ALL") } // ALL, PENDING, APPROVED, REJECTED
    val filtered = remember(posts, statusFilter) {
        posts.filter { p ->
            when (statusFilter) {
                "PENDING" -> p.moderationStatus.equals("PENDING", ignoreCase = true)
                "APPROVED" -> !p.isModeratedHidden && p.moderationStatus.equals("APPROVED", ignoreCase = true)
                "REJECTED" -> p.isModeratedHidden || p.moderationStatus.equals("REJECTED", ignoreCase = true)
                else -> true
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_section_post_management"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ALL" to "All (${posts.size})", "PENDING" to "Pending", "APPROVED" to "Approved", "REJECTED" to "Rejected").forEach { (code, label) ->
                    FilterChip(
                        selected = statusFilter == code,
                        onClick = { statusFilter = code },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }
        }

        items(filtered, key = { it.id }) { post ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${post.author.name} (${post.author.handle})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (post.isModeratedHidden || post.moderationStatus == "REJECTED") MaterialTheme.colorScheme.errorContainer else Color(0xFFDCFCE7)
                        ) {
                            Text(
                                text = if (post.isModeratedHidden) "REJECTED" else post.moderationStatus,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (post.isModeratedHidden || post.moderationStatus == "REJECTED") MaterialTheme.colorScheme.error else Color(0xFF15803D),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(post.text, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Text(
                        "Genuine Engagement: ${post.likesCount} Likes • ${post.commentsCount} Comments • ${post.sharesCount} Shares",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (adminRole.canModerateContent) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onApprovePost(post.id, "Approved in Post Management") },
                                modifier = Modifier.weight(1f).testTag("approve_post_btn_${post.id}"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
                            ) {
                                Text("Approve Post", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { onRejectPost(post.id, "Rejected in Post Management") },
                                modifier = Modifier.weight(1f).testTag("reject_post_btn_${post.id}"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Reject / Hide", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SECTION 4: VIRAL POSTS MANAGEMENT
// ==========================================
@Composable
private fun ViralPostsSection(
    adminRole: AdminRole,
    posts: List<Post>,
    onTogglePinPost: (postId: String, isPinned: Boolean) -> Unit,
    onToggleTrendingPost: (postId: String, isTrending: Boolean) -> Unit,
    onFeaturePost: (postId: String, isFeatured: Boolean, durationDays: Int, delayHours: Int) -> Unit
) {
    val rankedPosts = remember(posts) {
        posts.sortedByDescending { (if (it.isPinned) 10000 else 0) + it.likesCount + (it.commentsCount * 2) + (it.sharesCount * 3) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_section_viral_posts"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Viral & High-Engagement Community Posts", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(
                "Pin important community posts to the top of the feed, apply Trending labels, or highlight Featured posts based on genuine community engagement.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(rankedPosts, key = { it.id }) { post ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(post.author.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (post.isPinned) {
                                Text("📌 Pinned", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = FbBlue)
                            }
                            if (post.isTrendingLabeled) {
                                Text("🔥 Trending", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFB91C1C))
                            }
                            if (post.isCurrentlyFeatured) {
                                Text("⭐ Featured", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                            }
                        }
                    }
                    Text(post.text, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Text(
                        "Real Metrics: ${post.likesCount} Likes • ${post.commentsCount} Comments • ${post.sharesCount} Shares",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (adminRole.canManageFeaturedAndPromoted) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { onTogglePinPost(post.id, !post.isPinned) },
                                modifier = Modifier.weight(1f).testTag("pin_post_btn_${post.id}")
                            ) {
                                Text(if (post.isPinned) "Unpin" else "Pin to Top", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { onToggleTrendingPost(post.id, !post.isTrendingLabeled) },
                                modifier = Modifier.weight(1f).testTag("trend_post_btn_${post.id}")
                            ) {
                                Text(if (post.isTrendingLabeled) "Untrend" else "Trending Label", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { onFeaturePost(post.id, !post.isFeatured, 7, 0) },
                                modifier = Modifier.weight(1f).testTag("quick_feature_btn_${post.id}")
                            ) {
                                Text(if (post.isFeatured) "Unfeature" else "Feature", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SECTION 5: FEATURED POSTS & SCHEDULING
// ==========================================
@Composable
private fun FeaturedPostsSection(
    adminRole: AdminRole,
    posts: List<Post>,
    onFeaturePost: (postId: String, isFeatured: Boolean, durationDays: Int, delayHours: Int) -> Unit
) {
    var selectedDurationDays by remember { mutableIntStateOf(7) }
    var selectedScheduleDelayHours by remember { mutableIntStateOf(0) }
    val sdf = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_section_featured_posts"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Featured Post Schedule & Expiration Settings", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text("Select default duration and start schedule before featuring any post below:", style = MaterialTheme.typography.bodySmall)
                    Text("Featured Expiration Duration:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1, 3, 7, 14, 30).forEach { days ->
                            FilterChip(
                                selected = selectedDurationDays == days,
                                onClick = { selectedDurationDays = days },
                                label = { Text("$days Days", fontSize = 11.sp) }
                            )
                        }
                    }
                    Text("Schedule Start Time:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0 to "Immediate", 6 to "In 6 Hours", 24 to "In 24 Hours").forEach { (hrs, label) ->
                            FilterChip(
                                selected = selectedScheduleDelayHours == hrs,
                                onClick = { selectedScheduleDelayHours = hrs },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        items(posts, key = { it.id }) { post ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(post.author.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        if (post.isFeatured) {
                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFFEF3C7)) {
                                Text(
                                    text = "⭐ FEATURED",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(post.text, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val untilMs = post.featuredUntil ?: 0L
                    val scheduledMs = post.featuredScheduledAt ?: 0L
                    if (post.isFeatured && untilMs > 0L) {
                        Text(
                            text = "Scheduled: ${if (scheduledMs > 0L) sdf.format(Date(scheduledMs)) else "Immediate"} • Expires: ${sdf.format(Date(untilMs))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB45309)
                        )
                    }
                    if (adminRole.canManageFeaturedAndPromoted) {
                        Button(
                            onClick = {
                                onFeaturePost(post.id, !post.isFeatured, selectedDurationDays, selectedScheduleDelayHours)
                            },
                            modifier = Modifier.fillMaxWidth().testTag("schedule_feature_post_btn_${post.id}"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (post.isFeatured) MaterialTheme.colorScheme.error else FbBlue
                            )
                        ) {
                            Text(
                                if (post.isFeatured) "Remove from Featured Posts"
                                else "Feature Post ($selectedDurationDays Days)"
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SECTION 6: BLUE TICK VERIFICATION SYSTEM
// ==========================================
@Composable
private fun BlueTickVerificationSection(
    adminRole: AdminRole,
    verificationRequests: List<VerificationRequest>,
    users: List<User>,
    onReviewRequest: (requestId: String, userId: String, approve: Boolean, notes: String) -> Unit,
    onSetUserVerification: (userId: String, isVerified: Boolean, reason: String) -> Unit
) {
    var reviewNotes by remember { mutableStateOf("") }
    val verifiedUsers = remember(users) { users.filter { it.isVerified } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_section_blue_tick_verification"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Blue Tick Account Verification Requests (${verificationRequests.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Approve or reject community verification requests, or grant/remove Blue Tick badges directly.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (verificationRequests.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No pending Blue Tick verification requests in queue.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(verificationRequests, key = { it.id }) { req ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("${req.userName} (${req.userHandle})", fontWeight = FontWeight.Bold)
                                Text("Category: ${req.category}", style = MaterialTheme.typography.labelSmall, color = FbBlue)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = req.status.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text("Supporting Info: ${req.supportingInfo}", style = MaterialTheme.typography.bodySmall)
                        if (adminRole.canManageVerification && req.status == VerificationRequestStatus.PENDING) {
                            OutlinedTextField(
                                value = reviewNotes,
                                onValueChange = { reviewNotes = it },
                                label = { Text("Verification Decision Notes") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        onReviewRequest(req.id, req.userId, true, reviewNotes.ifBlank { "Identity & community standing verified." })
                                        reviewNotes = ""
                                    },
                                    modifier = Modifier.weight(1f).testTag("approve_verification_btn_${req.id}"),
                                    colors = ButtonDefaults.buttonColors(containerColor = VerifiedBadge)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Approve Blue Tick", fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        onReviewRequest(req.id, req.userId, false, reviewNotes.ifBlank { "Insufficient verification documentation." })
                                        reviewNotes = ""
                                    },
                                    modifier = Modifier.weight(1f).testTag("reject_verification_btn_${req.id}"),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Reject", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Currently Verified Blue Tick Accounts (${verifiedUsers.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        items(verifiedUsers, key = { it.id }) { u ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Blue Tick", tint = VerifiedBadge)
                        Column {
                            Text(u.name, fontWeight = FontWeight.Bold)
                            Text(u.handle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (adminRole.canManageVerification) {
                        OutlinedButton(
                            onClick = { onSetUserVerification(u.id, false, "Blue Tick revoked by administrator") },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("revoke_blue_tick_btn_${u.id}")
                        ) {
                            Text("Remove Blue Tick", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SECTION 7: PROMOTED POSTS MANAGEMENT
// ==========================================
@Composable
private fun PromotedPostsSection(
    adminRole: AdminRole,
    posts: List<Post>,
    onPromotePost: (postId: String, isPromoted: Boolean, durationDays: Int, category: String, location: String) -> Unit
) {
    var promoDurationDays by remember { mutableIntStateOf(7) }
    var targetCategory by remember { mutableStateOf("Community Highlight") }
    var targetLocation by remember { mutableStateOf("All Yarkhoon & Chitral") }
    val sdf = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_section_promoted_posts"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Promoted Post Campaign Configuration", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Promoted posts are transparently labeled '📢 Promoted' and display real impressions and genuine user engagement.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = targetCategory,
                        onValueChange = { targetCategory = it },
                        label = { Text("Target Category (e.g. Local Business, Education, Event)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = targetLocation,
                        onValueChange = { targetLocation = it },
                        label = { Text("Target Village / Location") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(3, 7, 14, 30).forEach { d ->
                            FilterChip(
                                selected = promoDurationDays == d,
                                onClick = { promoDurationDays = d },
                                label = { Text("$d Days", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        items(posts, key = { it.id }) { post ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(post.author.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        if (post.isCurrentlyPromoted) {
                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFDBEAFE)) {
                                Text(
                                    text = "📢 PROMOTED",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D4ED8),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(post.text, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        "Engagement: ${post.likesCount} Likes • ${post.commentsCount} Comments • ${post.sharesCount} Shares",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val promoEndMs = post.promotionEndDate ?: 0L
                    if (post.isPromoted) {
                        Text(
                            text = "Target: ${post.promotionTargetLocation ?: "All"} (${post.promotionTargetCategory ?: "General"}) • Ends: ${if (promoEndMs > 0L) sdf.format(Date(promoEndMs)) else "Active"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = FbBlue
                        )
                    }
                    if (adminRole.canManageFeaturedAndPromoted) {
                        Button(
                            onClick = {
                                onPromotePost(post.id, !post.isPromoted, promoDurationDays, targetCategory, targetLocation)
                            },
                            modifier = Modifier.fillMaxWidth().testTag("promote_post_btn_${post.id}"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (post.isPromoted) MaterialTheme.colorScheme.error else FbBlue
                            )
                        ) {
                            Text(if (post.isPromoted) "End Promotion Early" else "Promote Post ($promoDurationDays Days)")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SECTION 15: SECURE ADMIN LOGOUT CONFIRMATION
// ==========================================
@Composable
private fun AdminLogoutConfirmationSection(
    currentUser: User,
    adminRole: AdminRole,
    onConfirmLogout: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("admin_section_logout"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Logout",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("End Administrator Session?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "You are currently signed in as ${currentUser.name} (${adminRole.displayName}). Logging out will immediately terminate your administrative session and clear cached credentials.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onConfirmLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("confirm_admin_logout_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Confirm Secure Admin Logout", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Cancel & Return to Dashboard")
        }
    }
}

// ==========================================
// SECTION 10: REPORTS & COMPLAINTS
// ==========================================
@Composable
private fun ReportsModerationSubTab(
    reports: List<ContentReport>,
    onDismissReport: (reportId: String, reason: String) -> Unit,
    onHideContent: (reportId: String?, contentType: ReportContentType, contentId: String, reason: String) -> Unit,
    onSuspendUser: (userId: String, durationDays: Int, reason: String, reportId: String?) -> Unit
) {
    var selectedFilter by remember { mutableStateOf<ReportStatus?>(ReportStatus.PENDING) }
    val filteredReports = remember(reports, selectedFilter) {
        if (selectedFilter == null) reports else reports.filter { it.status == selectedFilter }
    }

    Column(modifier = Modifier.fillMaxSize().testTag("admin_section_reports")) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == ReportStatus.PENDING,
                onClick = { selectedFilter = ReportStatus.PENDING },
                label = { Text("Pending") }
            )
            FilterChip(
                selected = selectedFilter == ReportStatus.RESOLVED,
                onClick = { selectedFilter = ReportStatus.RESOLVED },
                label = { Text("Resolved") }
            )
            FilterChip(
                selected = selectedFilter == ReportStatus.DISMISSED,
                onClick = { selectedFilter = ReportStatus.DISMISSED },
                label = { Text("Dismissed") }
            )
            FilterChip(
                selected = selectedFilter == null,
                onClick = { selectedFilter = null },
                label = { Text("All (${reports.size})") }
            )
        }

        if (filteredReports.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No reports matching the selected filter.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredReports, key = { it.id }) { report ->
                    ReportAdminCard(
                        report = report,
                        onDismissReport = onDismissReport,
                        onHideContent = onHideContent,
                        onSuspendUser = onSuspendUser
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportAdminCard(
    report: ContentReport,
    onDismissReport: (reportId: String, reason: String) -> Unit,
    onHideContent: (reportId: String?, contentType: ReportContentType, contentId: String, reason: String) -> Unit,
    onSuspendUser: (userId: String, durationDays: Int, reason: String, reportId: String?) -> Unit
) {
    var actionNote by remember { mutableStateOf("") }
    var showSuspendDialog by remember { mutableStateOf(false) }
    var suspendDays by remember { mutableIntStateOf(7) }
    val sdf = remember { SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_report_card_${report.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = "${report.contentType.label} • ${report.reason.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = sdf.format(Date(report.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Reported User: ${report.reportedUserName ?: "Unknown"} (ID: ${report.reportedUserId ?: "N/A"})",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )

            if (report.contentSummary.isNotBlank()) {
                Text(
                    text = "Content Preview: \"${report.contentSummary}\"",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (report.description.isNotBlank()) {
                Text(
                    text = "Reporter Notes: ${report.description}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (report.status == ReportStatus.PENDING) {
                OutlinedTextField(
                    value = actionNote,
                    onValueChange = { actionNote = it },
                    placeholder = { Text("Moderation decision reason...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onDismissReport(report.id, actionNote.ifBlank { "Reviewed and dismissed." })
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Dismiss", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            onHideContent(
                                report.id,
                                report.contentType,
                                report.contentId,
                                actionNote.ifBlank { "Policy violation (${report.reason.label})" }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Remove Content", fontSize = 11.sp)
                    }

                    if (!report.reportedUserId.isNullOrBlank()) {
                        Button(
                            onClick = { showSuspendDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Suspend", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (showSuspendDialog && !report.reportedUserId.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { showSuspendDialog = false },
            title = { Text("Suspend User Account", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Suspend ${report.reportedUserName ?: report.reportedUserId}:")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1, 7, 30, 0).forEach { days ->
                            FilterChip(
                                selected = suspendDays == days,
                                onClick = { suspendDays = days },
                                label = { Text(if (days == 0) "Indefinite" else "${days}d") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSuspendUser(
                            report.reportedUserId,
                            suspendDays,
                            actionNote.ifBlank { "Repeated policy violation (${report.reason.label})" },
                            report.id
                        )
                        showSuspendDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Suspension")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSuspendDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ==========================================
// SECTION 13: ACTIVITY LOGS (AUDIT TRAIL)
// ==========================================
@Composable
private fun AuditLogsSubTab(
    auditLogs: List<ModerationAuditLog>,
    onRestoreContent: (contentType: ReportContentType, contentId: String, reason: String) -> Unit,
    onReinstateUser: (userId: String, reason: String) -> Unit
) {
    val sdf = remember { SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()) }

    if (auditLogs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().testTag("admin_section_activity_logs"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No administrative audit actions recorded yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("admin_section_activity_logs"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(auditLogs, key = { it.id }) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = log.actionType.label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = FbBlue
                            )
                            Text(
                                text = sdf.format(Date(log.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Target: ${log.targetType.uppercase()} (${log.targetId}) • By: ${log.adminEmail}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Reason / Details: ${log.reason}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
