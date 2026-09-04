package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.theme.YarkhwoonBlue

private val FacebookBlue = Color(0xFF1877F2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchDialog(
    posts: List<Post>,
    users: List<User>,
    marketplaceItems: List<MarketplaceItem>,
    services: List<ServiceListing>,
    groups: List<Group>,
    onDismiss: () -> Unit,
    onSelectUser: (User) -> Unit,
    onSelectPost: (Post) -> Unit,
    onSelectMarketplace: (MarketplaceItem) -> Unit,
    onSelectService: (ServiceListing) -> Unit,
    onSelectGroup: (Group) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "People", "Posts", "Marketplace", "Services", "Groups")

    val query = searchQuery.trim().lowercase()

    val filteredUsers = remember(query, users) {
        if (query.isBlank()) emptyList()
        else users.filter {
            it.fullName.lowercase().contains(query) || it.username.lowercase().contains(query)
        }
    }

    val filteredPosts = remember(query, posts) {
        if (query.isBlank()) emptyList()
        else posts.filter {
            it.content.lowercase().contains(query) || it.authorName.lowercase().contains(query)
        }
    }

    val filteredMarketplace = remember(query, marketplaceItems) {
        if (query.isBlank()) emptyList()
        else marketplaceItems.filter {
            it.title.lowercase().contains(query) || it.description.lowercase().contains(query) || it.category.lowercase().contains(query)
        }
    }

    val filteredServices = remember(query, services) {
        if (query.isBlank()) emptyList()
        else services.filter {
            it.serviceType.lowercase().contains(query) || it.description.lowercase().contains(query) || it.providerName.lowercase().contains(query)
        }
    }

    val filteredGroups = remember(query, groups) {
        if (query.isBlank()) emptyList()
        else groups.filter {
            it.name.lowercase().contains(query) || it.description.lowercase().contains(query) || it.category.lowercase().contains(query)
        }
    }

    val totalResultsCount = filteredUsers.size + filteredPosts.size + filteredMarketplace.size + filteredServices.size + filteredGroups.size

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
                // Top Search Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close Search")
                            }

                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search Yarkhoon.com...", fontSize = 15.sp) },
                                singleLine = true,
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (searchQuery.isNotBlank()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(Icons.Filled.Close, contentDescription = "Clear")
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                // Quick prompt voice search
                                                searchQuery = "Yarkhoon Valley"
                                            },
                                            modifier = Modifier.testTag("search_voice_mic_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Mic,
                                                contentDescription = "Voice Search",
                                                tint = FacebookBlue
                                            )
                                        }
                                    }
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("search_text_input")
                            )
                        }

                        // Category Filter Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp)
                        ) {
                            items(categories) { category ->
                                val isSelected = selectedCategory == category
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCategory = category },
                                    label = { Text(category, fontSize = 12.sp) },
                                    leadingIcon = {
                                        when (category) {
                                            "People" -> Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                            "Posts" -> Icon(Icons.Filled.Article, contentDescription = null, modifier = Modifier.size(16.dp))
                                            "Marketplace" -> Icon(Icons.Filled.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                                            "Services" -> Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                                            "Groups" -> Icon(Icons.Filled.Groups, contentDescription = null, modifier = Modifier.size(16.dp))
                                            else -> null
                                        }
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = FacebookBlue.copy(alpha = 0.15f),
                                        selectedLabelColor = FacebookBlue,
                                        selectedLeadingIconColor = FacebookBlue
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        }
                    }
                }

                // Results Body
                if (query.isBlank()) {
                    // Search Suggestions & Trending in Yarkhoon
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Search anything in Yarkhoon",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Find friends, posts, marketplace items, local services, and community groups.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        // Quick Suggestion Chips
                        Text(
                            "Popular Searches:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            SuggestionChip(
                                onClick = { searchQuery = "Chitral" },
                                label = { Text("Chitral") }
                            )
                            SuggestionChip(
                                onClick = { searchQuery = "Yarkhoon" },
                                label = { Text("Yarkhoon") }
                            )
                            SuggestionChip(
                                onClick = { searchQuery = "Polo" },
                                label = { Text("Polo") }
                            )
                        }
                    }
                } else if (totalResultsCount == 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No results found for \"$searchQuery\"",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Try searching for another name, service, or keyword.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. People / Users
                        if ((selectedCategory == "All" || selectedCategory == "People") && filteredUsers.isNotEmpty()) {
                            item {
                                SearchSectionHeader(title = "People", count = filteredUsers.size)
                            }
                            items(filteredUsers) { user ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelectUser(user)
                                            onDismiss()
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        AsyncImage(
                                            model = user.avatarUrl.ifBlank { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150" },
                                            contentDescription = user.fullName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(user.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                if (user.isVerified) {
                                                    Icon(Icons.Filled.Verified, contentDescription = null, tint = FacebookBlue, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                            Text("@${user.username}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                            if (user.bio.isNotBlank()) {
                                                Text(user.bio, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        }

                        // 2. Posts
                        if ((selectedCategory == "All" || selectedCategory == "Posts") && filteredPosts.isNotEmpty()) {
                            item {
                                SearchSectionHeader(title = "Posts", count = filteredPosts.size)
                            }
                            items(filteredPosts) { post ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelectPost(post)
                                            onDismiss()
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            AsyncImage(
                                                model = post.authorAvatarUrl,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                            Text(post.authorName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(post.content, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }

                        // 3. Marketplace Items
                        if ((selectedCategory == "All" || selectedCategory == "Marketplace") && filteredMarketplace.isNotEmpty()) {
                            item {
                                SearchSectionHeader(title = "Marketplace", count = filteredMarketplace.size)
                            }
                            items(filteredMarketplace) { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelectMarketplace(item)
                                            onDismiss()
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        AsyncImage(
                                            model = item.imageUrl.ifBlank { "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=300" },
                                            contentDescription = item.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                                            Text("PKR ${item.price.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FacebookBlue)
                                            Text(item.category, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Services
                        if ((selectedCategory == "All" || selectedCategory == "Services") && filteredServices.isNotEmpty()) {
                            item {
                                SearchSectionHeader(title = "Local Services", count = filteredServices.size)
                            }
                            items(filteredServices) { service ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelectService(service)
                                            onDismiss()
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = FacebookBlue.copy(alpha = 0.12f),
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Filled.Build, contentDescription = null, tint = FacebookBlue, modifier = Modifier.size(22.dp))
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(service.serviceType, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("By ${service.providerName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(service.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }

                        // 5. Groups
                        if ((selectedCategory == "All" || selectedCategory == "Groups") && filteredGroups.isNotEmpty()) {
                            item {
                                SearchSectionHeader(title = "Groups", count = filteredGroups.size)
                            }
                            items(filteredGroups) { group ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelectGroup(group)
                                            onDismiss()
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        AsyncImage(
                                            model = group.coverUrl.ifBlank { "https://images.unsplash.com/photo-1542224566-6e85f2e6772f?w=300" },
                                            contentDescription = group.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(group.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("${group.memberCount} members · ${group.category}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchSectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "$count found",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
