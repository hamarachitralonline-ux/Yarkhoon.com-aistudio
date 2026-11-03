package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.FbBlue
import com.example.ui.theme.VerifiedBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ==========================================
// SECTION 8: TRENDING TOPICS
// ==========================================
@Composable
internal fun TrendingTopicsSection(
    adminRole: AdminRole,
    posts: List<Post>,
    curatedTopics: List<CuratedTrendingTopic>,
    onSaveTopic: (id: String?, tag: String, headline: String, category: String, villageName: String, isPinned: Boolean) -> Unit,
    onDeleteTopic: (String) -> Unit,
    onTogglePostTrending: (postId: String, isTrending: Boolean) -> Unit
) {
    var tagInput by remember { mutableStateOf("") }
    var headlineInput by remember { mutableStateOf("") }
    var categoryInput by remember { mutableStateOf("Local News") }
    var villageInput by remember { mutableStateOf("Yarkhoon Valley") }
    var isPinnedInput by remember { mutableStateOf(true) }

    val categories = listOf("Local News", "Village Updates", "Community Discussion", "Emergency Update", "Culture & Sports")
    val trendingPosts = remember(posts) {
        posts.filter { it.isTrendingLabeled || (it.likesCount + it.commentsCount + it.sharesCount) >= 3 }
            .sortedByDescending { (if (it.isTrendingLabeled) 1000 else 0) + it.likesCount + it.commentsCount * 2 + it.sharesCount * 3 }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_section_trending_topics"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Trending Topics, Local News & Village Updates",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Curate official trending hashtags, local news headlines, and village updates across Yarkhoon.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (adminRole.canManageFeaturedAndPromoted) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Publish Curated Trending Topic / Local News", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(
                            value = tagInput,
                            onValueChange = { tagInput = it },
                            label = { Text("Topic Hashtag (e.g. #YarkhoonRoadUpdate)") },
                            modifier = Modifier.fillMaxWidth().testTag("trending_tag_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = headlineInput,
                            onValueChange = { headlineInput = it },
                            label = { Text("Headline / Discussion Topic") },
                            modifier = Modifier.fillMaxWidth().testTag("trending_headline_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = villageInput,
                            onValueChange = { villageInput = it },
                            label = { Text("Target Village / Area") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categories.take(3).forEach { cat ->
                                FilterChip(
                                    selected = categoryInput == cat,
                                    onClick = { categoryInput = cat },
                                    label = { Text(cat, fontSize = 11.sp) }
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isPinnedInput, onCheckedChange = { isPinnedInput = it })
                            Text("Pin to top of Trending Topics", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            onClick = {
                                onSaveTopic(null, tagInput, headlineInput, categoryInput, villageInput, isPinnedInput)
                                tagInput = ""
                                headlineInput = ""
                            },
                            enabled = tagInput.isNotBlank() && headlineInput.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().testTag("save_trending_topic_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = FbBlue)
                        ) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add to Trending Topics")
                        }
                    }
                }
            }
        }

        if (curatedTopics.isNotEmpty()) {
            item {
                Text("Curated Topics & Village Updates (${curatedTopics.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }
            items(curatedTopics, key = { it.id }) { topic ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(topic.tag, fontWeight = FontWeight.Bold, color = FbBlue)
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = topic.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                if (topic.isPinned) {
                                    Text("📌 Pinned", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD97706))
                                }
                            }
                            Text(topic.headline, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            if (topic.villageName.isNotBlank()) {
                                Text("Area: ${topic.villageName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (adminRole.canManageFeaturedAndPromoted) {
                            IconButton(onClick = { onDeleteTopic(topic.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove Topic", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("Trending Community Posts (${trendingPosts.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        }
        items(trendingPosts, key = { it.id }) { post ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(post.author.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Text(post.text, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Likes: ${post.likesCount} • Comments: ${post.commentsCount} • Shares: ${post.sharesCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (adminRole.canManageFeaturedAndPromoted) {
                        OutlinedButton(
                            onClick = { onTogglePostTrending(post.id, !post.isTrendingLabeled) }
                        ) {
                            Text(if (post.isTrendingLabeled) "Untrend" else "Mark Trending", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SECTION 9: VILLAGE MANAGEMENT
// ==========================================
@Composable
internal fun VillageManagementSection(
    adminRole: AdminRole,
    villages: List<YarkhoonVillage>,
    onSaveVillage: (id: String?, name: String, tehsil: String, district: String, description: String, population: String) -> Unit,
    onDeleteVillage: (id: String, name: String) -> Unit
) {
    var editingVillage by remember { mutableStateOf<YarkhoonVillage?>(null) }
    var nameInput by remember { mutableStateOf("") }
    var tehsilInput by remember { mutableStateOf("Mastuj") }
    var districtInput by remember { mutableStateOf("Upper Chitral") }
    var descriptionInput by remember { mutableStateOf("") }
    var populationInput by remember { mutableStateOf("") }
    var confirmDeleteVillage by remember { mutableStateOf<YarkhoonVillage?>(null) }
    var duplicateWarning by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_section_village_management"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Yarkhoon & Chitral Village Directory Management",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Add new villages, edit existing village details, manage tehsils/districts, and prevent duplicate entries.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (adminRole.canManageVillages) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (editingVillage != null) "Edit Village: ${editingVillage!!.name}" else "Add New Village",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        if (duplicateWarning != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth().testTag("village_duplicate_warning")
                            ) {
                                Text(
                                    text = duplicateWarning!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = {
                                nameInput = it
                                duplicateWarning = null
                            },
                            label = { Text("Village Name (e.g. Brep, Bang, Meragram, Lasht)") },
                            modifier = Modifier.fillMaxWidth().testTag("village_name_input"),
                            singleLine = true
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = tehsilInput,
                                onValueChange = { tehsilInput = it },
                                label = { Text("Tehsil") },
                                modifier = Modifier.weight(1f).testTag("village_tehsil_input"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = districtInput,
                                onValueChange = {
                                    districtInput = it
                                    duplicateWarning = null
                                },
                                label = { Text("District") },
                                modifier = Modifier.weight(1f).testTag("village_district_input"),
                                singleLine = true
                            )
                        }
                        OutlinedTextField(
                            value = populationInput,
                            onValueChange = { populationInput = it },
                            label = { Text("Estimated Households / Population (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = descriptionInput,
                            onValueChange = { descriptionInput = it },
                            label = { Text("Village Notes / Key Facilities") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (editingVillage != null) {
                                OutlinedButton(
                                    onClick = {
                                        editingVillage = null
                                        nameInput = ""
                                        descriptionInput = ""
                                        populationInput = ""
                                        duplicateWarning = null
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel Edit")
                                }
                            }
                            Button(
                                onClick = {
                                    val isDup = FirestoreAdminRepository.isDuplicateVillage(
                                        existingVillages = villages,
                                        candidateName = nameInput,
                                        candidateDistrict = districtInput,
                                        ignoreVillageId = editingVillage?.id
                                    )
                                    if (isDup) {
                                        duplicateWarning = "Duplicate entry prevented: '${nameInput.trim()}' already exists in ${districtInput.trim()}."
                                    } else {
                                        onSaveVillage(
                                            editingVillage?.id,
                                            nameInput,
                                            tehsilInput,
                                            districtInput,
                                            descriptionInput,
                                            populationInput
                                        )
                                        editingVillage = null
                                        nameInput = ""
                                        descriptionInput = ""
                                        populationInput = ""
                                        duplicateWarning = null
                                    }
                                },
                                enabled = nameInput.isNotBlank(),
                                modifier = Modifier.weight(1f).testTag("save_village_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = FbBlue)
                            ) {
                                Icon(Icons.Default.Landscape, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (editingVillage != null) "Update Village" else "Add Village")
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Registered Villages (${villages.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        if (villages.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No custom villages registered yet. Add Yarkhoon Valley villages above (e.g., Brep, Bang, Broghil, Lasht, Meragram,Dizg).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(villages, key = { it.id }) { village ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(village.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Tehsil: ${village.tehsil} • District: ${village.district}",
                                style = MaterialTheme.typography.labelSmall,
                                color = FbBlue
                            )
                            if (village.description.isNotBlank()) {
                                Text(village.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (village.populationEstimate.isNotBlank()) {
                                Text("Population/Households: ${village.populationEstimate}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        if (adminRole.canManageVillages) {
                            Row {
                                IconButton(
                                    onClick = {
                                        editingVillage = village
                                        nameInput = village.name
                                        tehsilInput = village.tehsil
                                        districtInput = village.district
                                        descriptionInput = village.description
                                        populationInput = village.populationEstimate
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Village", tint = FbBlue)
                                }
                                IconButton(
                                    onClick = { confirmDeleteVillage = village }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Village", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    confirmDeleteVillage?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmDeleteVillage = null },
            title = { Text("Delete Village '${target.name}'?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove ${target.name} (${target.district}) from the directory? This action is logged.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteVillage(target.id, target.name)
                        confirmDeleteVillage = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteVillage = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ==========================================
// SECTION 11: OFFICIAL NOTIFICATIONS & ANNOUNCEMENTS
// ==========================================
@Composable
internal fun OfficialNotificationsSection(
    adminRole: AdminRole,
    announcements: List<OfficialAnnouncement>,
    onPublishAnnouncement: (title: String, body: String, category: String, targetLocation: String, priority: String) -> Unit
) {
    var titleInput by remember { mutableStateOf("") }
    var bodyInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("OFFICIAL_ANNOUNCEMENT") }
    var targetLocation by remember { mutableStateOf("All Yarkhoon & Chitral") }
    var selectedPriority by remember { mutableStateOf("HIGH") }

    val categories = listOf(
        "BREAKING_NEWS" to "Breaking News",
        "EMERGENCY_ALERT" to "Emergency Alert",
        "COMMUNITY_UPDATE" to "Community Update",
        "OFFICIAL_ANNOUNCEMENT" to "Official Yarkhoon Announcement"
    )
    val sdf = remember { SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_section_notifications"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Broadcast Official Notifications & Emergency Alerts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Send breaking news, emergency road/weather alerts, and official Yarkhoon announcements to community members.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (adminRole.canBroadcastNotifications) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Compose Broadcast Notification", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                        categories.forEach { (code, label) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCategory = code }
                                    .padding(vertical = 2.dp)
                            ) {
                                RadioButton(
                                    selected = selectedCategory == code,
                                    onClick = { selectedCategory = code }
                                )
                                Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        OutlinedTextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            label = { Text("Announcement Headline") },
                            modifier = Modifier.fillMaxWidth().testTag("announcement_title_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = bodyInput,
                            onValueChange = { bodyInput = it },
                            label = { Text("Detailed Message Body") },
                            modifier = Modifier.fillMaxWidth().testTag("announcement_body_input"),
                            minLines = 3
                        )

                        OutlinedTextField(
                            value = targetLocation,
                            onValueChange = { targetLocation = it },
                            label = { Text("Target Village / Region") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                onPublishAnnouncement(titleInput, bodyInput, selectedCategory, targetLocation, selectedPriority)
                                titleInput = ""
                                bodyInput = ""
                            },
                            enabled = titleInput.isNotBlank() && bodyInput.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().testTag("publish_announcement_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedCategory == "EMERGENCY_ALERT") MaterialTheme.colorScheme.error else FbBlue
                            )
                        ) {
                            Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Broadcast Official Notification", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Your current role (${adminRole.displayName}) has read-only access to official broadcasts. Admin or Super Admin role is required to send broadcasts.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        item {
            Text(
                text = "Sent Broadcast History (${announcements.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        items(announcements, key = { it.id }) { ann ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (ann.category == "EMERGENCY_ALERT") MaterialTheme.colorScheme.errorContainer else FbBlue.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = ann.category.replace("_", " "),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (ann.category == "EMERGENCY_ALERT") MaterialTheme.colorScheme.error else FbBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = if (ann.sentAt > 0L) sdf.format(Date(ann.sentAt)) else "Recently",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(ann.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text(ann.body, style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "Target: ${ann.targetVillageOrLocation} • By: ${ann.sentByEmail.ifBlank { "Admin" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ==========================================
// SECTION 12: ADMIN MANAGEMENT (SUPER ADMIN EXCLUSIVE)
// ==========================================
@Composable
internal fun AdminManagementSection(
    adminRole: AdminRole,
    adminAccounts: List<AdminAccountRecord>,
    registeredUsers: List<User>,
    onAssignAdminRole: (uid: String, email: String, name: String, role: AdminRole, permissions: List<String>) -> Unit,
    onRevokeAdminRole: (uid: String, reason: String) -> Unit
) {
    var targetUid by remember { mutableStateOf("") }
    var targetEmail by remember { mutableStateOf("") }
    var targetName by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(AdminRole.MODERATOR) }
    var confirmRevokeTarget by remember { mutableStateOf<AdminAccountRecord?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_section_admin_management"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Role-Based Access Control (RBAC) & Admin Management",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Manage Super Admins, Admins, and Moderators. Only Super Admins can assign or revoke administrative roles.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!adminRole.canManageAdmins) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Restricted Section: Your account role is ${adminRole.displayName}. Only a Super Admin can create or modify administrator roles.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        } else {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Assign Admin or Moderator Role", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                        if (registeredUsers.isNotEmpty()) {
                            Text("Quick-select from registered users:", style = MaterialTheme.typography.labelSmall)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                registeredUsers.take(3).forEach { u ->
                                    AssistChip(
                                        onClick = {
                                            targetUid = u.id
                                            targetEmail = u.email.ifBlank { "${u.handle.removePrefix("@")}@yarkhoon.com" }
                                            targetName = u.name
                                        },
                                        label = { Text(u.name, maxLines = 1, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = targetUid,
                            onValueChange = { targetUid = it },
                            label = { Text("Target User Firebase UID") },
                            modifier = Modifier.fillMaxWidth().testTag("assign_admin_uid_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = targetEmail,
                            onValueChange = { targetEmail = it },
                            label = { Text("Target User Email Address") },
                            modifier = Modifier.fillMaxWidth().testTag("assign_admin_email_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = targetName,
                            onValueChange = { targetName = it },
                            label = { Text("Administrator Display Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Text("Select Role Level:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        listOf(AdminRole.SUPER_ADMIN, AdminRole.ADMIN, AdminRole.MODERATOR).forEach { r ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedRole = r }
                                    .padding(vertical = 2.dp)
                            ) {
                                RadioButton(
                                    selected = selectedRole == r,
                                    onClick = { selectedRole = r }
                                )
                                Column {
                                    Text(r.displayName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    val desc = when (r) {
                                        AdminRole.SUPER_ADMIN -> "Full control over all 15 sections, users, posts, villages, and admin roles."
                                        AdminRole.ADMIN -> "Manage users, posts, verification, featured/promoted content, villages, and notifications."
                                        AdminRole.MODERATOR -> "Review reports, approve/reject posts, and hide inappropriate content."
                                        AdminRole.NONE -> ""
                                    }
                                    Text(desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        Button(
                            onClick = {
                                onAssignAdminRole(targetUid, targetEmail, targetName, selectedRole, emptyList())
                                targetUid = ""
                                targetEmail = ""
                                targetName = ""
                            },
                            enabled = targetUid.isNotBlank() && targetEmail.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().testTag("assign_admin_submit_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = FbBlue)
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Grant ${selectedRole.displayName} Role")
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Authorized Administrators & Moderators (${adminAccounts.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        items(adminAccounts, key = { it.uid }) { acc ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(acc.name.ifBlank { acc.email }, fontWeight = FontWeight.Bold)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = FbBlue.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = acc.role.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = FbBlue,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text("Email: ${acc.email}", style = MaterialTheme.typography.bodySmall)
                        Text("UID: ${acc.uid}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (adminRole.canManageAdmins && acc.enabled) {
                        OutlinedButton(
                            onClick = { confirmRevokeTarget = acc },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Revoke", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    confirmRevokeTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmRevokeTarget = null },
            title = { Text("Revoke Admin Role?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to revoke ${target.role.displayName} privileges from ${target.email}?") },
            confirmButton = {
                Button(
                    onClick = {
                        onRevokeAdminRole(target.uid, "Revoked by Super Admin")
                        confirmRevokeTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Revoke Access")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRevokeTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ==========================================
// SECTION 14: ADMIN SETTINGS & SECURITY
// ==========================================
@Composable
internal fun AdminSettingsSection(
    adminRole: AdminRole,
    currentUser: User,
    sessionStartedAt: Long,
    onNavigateToLogout: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_section_settings"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Active Administrator Session", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Signed in as: ${currentUser.name} (${currentUser.email})", style = MaterialTheme.typography.bodyMedium)
                    Text("Assigned RBAC Role: ${adminRole.displayName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = FbBlue)
                    Text(
                        text = "Session Started: ${if (sessionStartedAt > 0L) sdf.format(Date(sessionStartedAt)) else "Active"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Session Timeout Policy: Automatic expiration after 4 hours of session duration or immediate invalidation on sign-out.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Role Permissions Matrix", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    PermissionRow("Access Admin Control Panel", adminRole.canAccessAdminPanel)
                    PermissionRow("Content Moderation & Reports", adminRole.canModerateContent)
                    PermissionRow("User Account Suspension & Activation", adminRole.canManageUsers)
                    PermissionRow("Blue Tick Verification Approval", adminRole.canManageVerification)
                    PermissionRow("Viral, Featured & Promoted Posts", adminRole.canManageFeaturedAndPromoted)
                    PermissionRow("Yarkhoon Village Directory Management", adminRole.canManageVillages)
                    PermissionRow("Broadcast Official Notifications", adminRole.canBroadcastNotifications)
                    PermissionRow("Manage Admins & Moderators (Super Admin)", adminRole.canManageAdmins)
                }
            }
        }

        item {
            Button(
                onClick = onNavigateToLogout,
                modifier = Modifier.fillMaxWidth().testTag("settings_go_to_logout_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Proceed to Secure Admin Logout", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(
            text = if (granted) "ENABLED" else "RESTRICTED",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (granted) Color(0xFF15803D) else MaterialTheme.colorScheme.error
        )
    }
}
