package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.SocialMediaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private val BrandBlue = Color(0xFF1877F2)
private val VerifiedBlue = Color(0xFF0084FF)
private val EmeraldGreen = Color(0xFF10B981)
private val CoralRed = Color(0xFFEF4444)
private val AmberOrange = Color(0xFFF59E0B)

val PageCoverPresets = listOf(
    "https://images.unsplash.com/photo-1542224566-6e85f2e6772f?w=800" to "Shandur Pass",
    "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800" to "Hindukush Peaks",
    "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800" to "Yarkhoon River",
    "https://images.unsplash.com/photo-1580301762395-21ce84d00bc6?w=800" to "Cultural Heritage",
    "https://images.unsplash.com/photo-1455390582262-044cdead277a?w=800" to "Literature & Arts"
)

val PageAvatarPresets = listOf(
    "https://images.unsplash.com/photo-1585829365295-ab7cd400c167?w=200" to "News & Media",
    "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=200" to "Culture & Arts",
    "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=200" to "Exploration & Sports",
    "https://images.unsplash.com/photo-1457369804613-52c61a468e7d?w=200" to "Education & Academy",
    "https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=200" to "Business & Trade"
)

val PageCategories = listOf(
    "All",
    "News & Media",
    "Community",
    "Education",
    "Organization",
    "Business",
    "Art & Culture",
    "Travel & Tourism",
    "Sports"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPagesScreen(
    viewModel: SocialMediaViewModel,
    onBack: () -> Unit,
    onOpenPageDetail: (Page) -> Unit
) {
    val context = LocalContext.current
    val allPages by viewModel.allPages.collectAsState()
    val myPages by viewModel.myPages.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val pageActionMessage by viewModel.pageActionMessage.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Explore, 1: Following, 2: My Pages
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(pageActionMessage) {
        pageActionMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearPageActionMessage()
        }
    }

    val filteredPages = remember(allPages, myPages, selectedTab, selectedCategory, searchQuery) {
        val baseList = when (selectedTab) {
            1 -> allPages.filter { it.isFollowedByMe }
            2 -> allPages.filter { it.ownerId == (currentUser?.id ?: "currentUser") }
            else -> allPages
        }
        baseList.filter { page ->
            val matchesCategory = (selectedCategory == "All") || page.category.equals(selectedCategory, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                    page.name.contains(searchQuery, ignoreCase = true) ||
                    page.username.contains(searchQuery, ignoreCase = true) ||
                    page.bio.contains(searchQuery, ignoreCase = true) ||
                    page.category.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Pages & Channels",
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp
                            )
                            Surface(
                                color = BrandBlue.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Official",
                                    color = BrandBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "News, organizations & community portals",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("pages_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("pages_create_btn"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Create", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Create Page", fontWeight = FontWeight.Bold) },
                containerColor = BrandBlue,
                contentColor = Color.White,
                modifier = Modifier.testTag("pages_fab_create")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("pages_search_input"),
                placeholder = { Text("Search pages by name, @handle or category...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = BrandBlue) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandBlue,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // Primary Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = BrandBlue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Discover (${allPages.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        val count = allPages.count { it.isFollowedByMe }
                        Text(
                            "Following ($count)",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            "My Pages (${myPages.size})",
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            // Categories horizontal chip row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PageCategories) { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = { Text(category, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Page List
            if (filteredPages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Layers,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        Text(
                            text = when (selectedTab) {
                                1 -> "You haven't followed any pages yet."
                                2 -> "You haven't created any pages yet."
                                else -> "No pages found matching your search."
                            },
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Official pages let organizations, newspapers, and creators publish broadcasts to the community.",
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.outline
                        )
                        if (selectedTab == 2) {
                            Button(
                                onClick = { showCreateDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create Your First Page")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
                ) {
                    items(filteredPages, key = { it.id }) { page ->
                        PageListItemCard(
                            page = page,
                            onOpen = { onOpenPageDetail(page) },
                            onToggleFollow = { viewModel.onToggleFollowPage(page) },
                            onShare = {
                                val link = viewModel.getPageShareLink(page)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Page Link", link))
                                Toast.makeText(context, "Page link copied: $link", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePageDialog(
            onDismiss = { showCreateDialog = false },
            onSubmit = { name, username, category, bio, avatar, cover, phone, email, website, location ->
                viewModel.onCreatePage(
                    name = name,
                    username = username,
                    category = category,
                    bio = bio,
                    avatarUrl = avatar,
                    coverUrl = cover,
                    phone = phone,
                    email = email,
                    website = website,
                    location = location
                ) { success, msg ->
                    if (success) {
                        showCreateDialog = false
                    } else {
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}

@Composable
fun PageListItemCard(
    page: Page,
    onOpen: () -> Unit,
    onToggleFollow: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .testTag("page_card_${page.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Cover Image Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                AsyncImage(
                    model = page.coverUrl.ifBlank { "https://images.unsplash.com/photo-1542224566-6e85f2e6772f?w=800" },
                    contentDescription = "Cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                            )
                        )
                )

                // Category pill on cover
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.55f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Text(
                        text = page.category,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Share link button on cover
                IconButton(
                    onClick = onShare,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.55f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.Share,
                                contentDescription = "Share",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Body content with avatar overlapping
            Row(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Page Avatar
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                ) {
                    AsyncImage(
                        model = page.avatarUrl.ifBlank { "https://images.unsplash.com/photo-1585829365295-ab7cd400c167?w=200" },
                        contentDescription = page.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = page.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (page.isVerified) {
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = "Verified",
                                tint = VerifiedBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "@${page.username} • ${page.followersCount} followers",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    if (page.bio.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = page.bio,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Follow / Unfollow Button
                if (page.isFollowedByMe) {
                    OutlinedButton(
                        onClick = onToggleFollow,
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, BrandBlue),
                        modifier = Modifier.testTag("page_follow_toggle_${page.id}")
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Following", color = BrandBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Button(
                        onClick = onToggleFollow,
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("page_follow_toggle_${page.id}")
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Follow", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageDetailScreen(
    page: Page,
    viewModel: SocialMediaViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val pageMembers by viewModel.selectedPageMembers.collectAsState()
    val pagePosts by viewModel.selectedPagePosts.collectAsState()
    val pageActionMessage by viewModel.pageActionMessage.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Feed, 1: About, 2: Admins
    var showEditDialog by remember { mutableStateOf(false) }
    var showAddAdminDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    // New Post Composer state
    var showCreatePostDialog by remember { mutableStateOf(false) }

    val currentUserId = currentUser?.id ?: "currentUser"
    val isOwner = page.ownerId == currentUserId || currentUserId == "admin"
    val isAdmin = isOwner || pageMembers.any { it.userId == currentUserId && it.role.equals("ADMIN", ignoreCase = true) }

    val coroutineScope = rememberCoroutineScope()
    var isUpdatingCover by remember { mutableStateOf(false) }
    var isUpdatingAvatar by remember { mutableStateOf(false) }

    // Cover Photo Picker Launcher from Device
    val detailCoverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isUpdatingCover = true
                try {
                    val savedFile = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            val file = File(context.filesDir, "page_cover_${System.currentTimeMillis()}.jpg")
                            file.outputStream().use { output -> input.copyTo(output) }
                            file
                        }
                    }
                    if (savedFile != null) {
                        val newCoverUrl = Uri.fromFile(savedFile).toString()
                        viewModel.onUpdatePage(
                            page = page,
                            name = page.name,
                            category = page.category,
                            bio = page.bio,
                            avatarUrl = page.avatarUrl,
                            coverUrl = newCoverUrl,
                            phone = page.phone,
                            email = page.email,
                            website = page.website,
                            location = page.location
                        ) { success, _ ->
                            if (success) {
                                Toast.makeText(context, "Page cover photo updated from device!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not update cover photo: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isUpdatingCover = false
                }
            }
        }
    }

    // Page Icon / Avatar Picker Launcher from Device
    val detailAvatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isUpdatingAvatar = true
                try {
                    val savedFile = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            val file = File(context.filesDir, "page_avatar_${System.currentTimeMillis()}.jpg")
                            file.outputStream().use { output -> input.copyTo(output) }
                            file
                        }
                    }
                    if (savedFile != null) {
                        val newAvatarUrl = Uri.fromFile(savedFile).toString()
                        viewModel.onUpdatePage(
                            page = page,
                            name = page.name,
                            category = page.category,
                            bio = page.bio,
                            avatarUrl = newAvatarUrl,
                            coverUrl = page.coverUrl,
                            phone = page.phone,
                            email = page.email,
                            website = page.website,
                            location = page.location
                        ) { success, _ ->
                            if (success) {
                                Toast.makeText(context, "Page icon updated from device!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not update page icon: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isUpdatingAvatar = false
                }
            }
        }
    }

    LaunchedEffect(pageActionMessage) {
        pageActionMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearPageActionMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = page.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (page.isVerified) {
                            Icon(Icons.Filled.Verified, contentDescription = "Verified", tint = VerifiedBlue, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("page_detail_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Share deep link button
                    IconButton(
                        onClick = {
                            val shareUrl = viewModel.getPageShareLink(page)
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Check out ${page.name} (@${page.username}) on Yarkhoon Social: $shareUrl")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Page"))
                        },
                        modifier = Modifier.testTag("page_share_btn")
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }

                    // Options menu
                    Box {
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Copy Deep Link") },
                                leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null) },
                                onClick = {
                                    showOptionsMenu = false
                                    val link = viewModel.getPageShareLink(page)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Page Link", link))
                                    Toast.makeText(context, "Link copied: $link", Toast.LENGTH_SHORT).show()
                                }
                            )
                            if (isAdmin) {
                                DropdownMenuItem(
                                    text = { Text("Edit Page Info") },
                                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                    onClick = {
                                        showOptionsMenu = false
                                        showEditDialog = true
                                    }
                                )
                            }
                            if (isOwner) {
                                DropdownMenuItem(
                                    text = { Text("Delete Page", color = CoralRed) },
                                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = CoralRed) },
                                    onClick = {
                                        showOptionsMenu = false
                                        showDeleteConfirmDialog = true
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Report Page") },
                                leadingIcon = { Icon(Icons.Filled.Flag, contentDescription = null) },
                                onClick = {
                                    showOptionsMenu = false
                                    showReportDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (isAdmin && activeTab == 0) {
                FloatingActionButton(
                    onClick = { showCreatePostDialog = true },
                    containerColor = BrandBlue,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("page_fab_new_post")
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Create Post")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Hero Cover & Avatar
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    AsyncImage(
                        model = page.coverUrl.ifBlank { "https://images.unsplash.com/photo-1542224566-6e85f2e6772f?w=800" },
                        contentDescription = "Page Cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                )
                            )
                    )

                    // Category Pill on Top Right
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = page.category,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    // Quick Cover Photo Edit Button (from device for page admin)
                    if (isAdmin) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.7f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable(enabled = !isUpdatingCover) {
                                    detailCoverPickerLauncher.launch("image/*")
                                }
                                .testTag("page_change_cover_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = "Edit Cover", tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (isUpdatingCover) "Uploading..." else "Edit Cover",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (isUpdatingCover) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }

            // Page Header Card with Avatar overlapping
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Avatar overlapping with device upload badge
                        Box(
                            modifier = Modifier
                                .offset(y = (-36).dp)
                                .size(82.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(78.dp)
                                    .clip(CircleShape)
                                    .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable(enabled = isAdmin && !isUpdatingAvatar) {
                                        detailAvatarPickerLauncher.launch("image/*")
                                    }
                            ) {
                                AsyncImage(
                                    model = page.avatarUrl.ifBlank { "https://images.unsplash.com/photo-1585829365295-ab7cd400c167?w=200" },
                                    contentDescription = page.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isUpdatingAvatar) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }

                            if (isAdmin) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(BrandBlue)
                                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                        .clickable(enabled = !isUpdatingAvatar) {
                                            detailAvatarPickerLauncher.launch("image/*")
                                        }
                                        .testTag("page_change_avatar_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.CameraAlt, contentDescription = "Change Icon", tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }

                        // Follow Action Button
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            if (page.isFollowedByMe) {
                                OutlinedButton(
                                    onClick = { viewModel.onToggleFollowPage(page) },
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.5.dp, BrandBlue)
                                ) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Following", color = BrandBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.onToggleFollowPage(page) },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Follow Page", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            if (isAdmin) {
                                FilledTonalIconButton(
                                    onClick = { showEditDialog = true }
                                ) {
                                    Icon(Icons.Filled.Settings, contentDescription = "Page Settings")
                                }
                            }
                        }
                    }

                    // Title & Username
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.offset(y = (-24).dp)
                    ) {
                        Text(
                            text = page.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (page.isVerified) {
                            Icon(Icons.Filled.Verified, contentDescription = "Verified", tint = VerifiedBlue, modifier = Modifier.size(20.dp))
                        }
                    }

                    Text(
                        text = "@${page.username} • ${page.category}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.offset(y = (-24).dp)
                    )

                    // Bio
                    if (page.bio.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = page.bio,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp,
                            modifier = Modifier.offset(y = (-20).dp)
                        )
                    }

                    // Stats & Meta row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-14).dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.People, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(16.dp))
                            Text("${page.followersCount} followers", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.Article, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                            Text("${pagePosts.size} updates", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        if (page.location.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = AmberOrange, modifier = Modifier.size(16.dp))
                                Text(page.location, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }

                    // Quick Deep Link Copy pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-6).dp)
                            .clickable {
                                val link = viewModel.getPageShareLink(page)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Page Link", link))
                                Toast.makeText(context, "Page link copied: $link", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.Link, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(16.dp))
                                Text("yarkhoon.com/page/${page.username}", fontSize = 12.sp, color = BrandBlue, fontWeight = FontWeight.SemiBold)
                            }
                            Text("Copy", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }

            // Tabs inside Page Detail
            item {
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = BrandBlue,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Updates (${pagePosts.size})", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("About", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = { Text("Admins (${pageMembers.size})", fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // Tab Content
            when (activeTab) {
                0 -> {
                    // Admin Composer Prompt Card
                    if (isAdmin) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .clickable { showCreatePostDialog = true }
                                    .testTag("page_admin_post_composer_card"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    AsyncImage(
                                        model = page.avatarUrl,
                                        contentDescription = "Avatar",
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Broadcast an update as ${page.name}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Share official news, announcements, or photos...",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    Icon(
                                        Icons.Filled.AddPhotoAlternate,
                                        contentDescription = "Add Post",
                                        tint = BrandBlue
                                    )
                                }
                            }
                        }
                    }

                    if (pagePosts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Campaign,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = "No updates posted yet.",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Updates published by this page will appear here.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    } else {
                        items(pagePosts, key = { it.id }) { post ->
                            PagePostCard(
                                post = post,
                                page = page,
                                isAdmin = isAdmin,
                                viewModel = viewModel,
                                onShare = {
                                    val shareIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "${post.content}\n\nVia ${page.name}: https://yarkhoon.com/page/${page.username}")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Update"))
                                }
                            )
                        }
                    }
                }
                1 -> {
                    // About Tab
                    item {
                        PageAboutCard(page = page)
                    }
                }
                2 -> {
                    // Admins & Team Tab
                    item {
                        PageAdminsSection(
                            page = page,
                            isOwner = isOwner,
                            members = pageMembers,
                            onAddAdmin = { showAddAdminDialog = true },
                            onRemoveMember = { member ->
                                viewModel.onRemovePageMember(page.id, member.userId, member.userName)
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showCreatePostDialog) {
        CreatePagePostDialog(
            page = page,
            onDismiss = { showCreatePostDialog = false },
            onSubmit = { content, mediaType, mediaUrls, linkUrl, isPinned ->
                viewModel.onCreatePagePost(
                    page = page,
                    content = content,
                    mediaType = mediaType,
                    mediaUrls = mediaUrls,
                    linkUrl = linkUrl,
                    isPinned = isPinned
                ) { success, msg ->
                    if (success) showCreatePostDialog = false
                    else Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showEditDialog) {
        EditPageDialog(
            page = page,
            onDismiss = { showEditDialog = false },
            onSubmit = { name, category, bio, avatar, cover, phone, email, website, location ->
                viewModel.onUpdatePage(
                    page = page,
                    name = name,
                    category = category,
                    bio = bio,
                    avatarUrl = avatar,
                    coverUrl = cover,
                    phone = phone,
                    email = email,
                    website = website,
                    location = location
                ) { success, _ ->
                    if (success) showEditDialog = false
                }
            }
        )
    }

    if (showAddAdminDialog) {
        AddAdminDialog(
            allUsers = allUsers.filter { it.id != page.ownerId },
            onDismiss = { showAddAdminDialog = false },
            onSelectUser = { targetUser ->
                viewModel.onAddPageAdmin(page, targetUser) { success, msg ->
                    if (success) showAddAdminDialog = false
                    else Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showReportDialog) {
        ReportPageDialog(
            page = page,
            onDismiss = { showReportDialog = false },
            onSubmit = { reason, details ->
                viewModel.onReportPage(page.id, page.name, reason, details) {
                    showReportDialog = false
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Page '${page.name}'?") },
            text = { Text("This will permanently delete this page, all published updates, and remove all followers. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.onDeletePage(page.id, page.name)
                        onBack()
                    }
                ) {
                    Text("Delete Permanently", color = CoralRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PagePostCard(
    post: PagePost,
    page: Page,
    isAdmin: Boolean,
    viewModel: SocialMediaViewModel,
    onShare: () -> Unit
) {
    val comments by viewModel.selectedPagePostComments.collectAsState()
    val isViewingComments = viewModel.selectedPagePostId.collectAsState().value == post.id
    var commentInputText by remember { mutableStateOf("") }

    val formattedDate = remember(post.timestamp) {
        SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault()).format(Date(post.timestamp))
    }

    val mediaUrls = remember(post.mediaUrlsJson) {
        try {
            val arr = JSONArray(post.mediaUrlsJson)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            list
        } catch (e: Exception) {
            emptyList<String>()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("page_post_${post.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Pinned indicator
            if (post.isPinned) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Icon(Icons.Filled.PushPin, contentDescription = "Pinned", tint = BrandBlue, modifier = Modifier.size(14.dp))
                    Text("Pinned Update", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                }
            }

            // Post Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AsyncImage(
                    model = post.pageAvatarUrl.ifBlank { page.avatarUrl },
                    contentDescription = post.pageName,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = post.pageName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        if (page.isVerified) {
                            Icon(Icons.Filled.Verified, contentDescription = "Verified", tint = VerifiedBlue, modifier = Modifier.size(15.dp))
                        }
                    }
                    Text(
                        text = "@${post.pageUsername} • $formattedDate",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                if (isAdmin) {
                    IconButton(
                        onClick = { viewModel.onDeletePagePost(post.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete Post", tint = CoralRed, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Post text
            if (post.content.isNotBlank()) {
                Text(
                    text = post.content,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 21.sp
                )
            }

            // Attached Media
            if (mediaUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                AsyncImage(
                    model = mediaUrls.first(),
                    contentDescription = "Post Media",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            // Link Preview
            if (post.linkUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Language, contentDescription = null, tint = BrandBlue)
                        Text(
                            text = post.linkUrl,
                            fontSize = 12.sp,
                            color = BrandBlue,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(6.dp))

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like Button
                TextButton(
                    onClick = { viewModel.onToggleLikePagePost(post) },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = if (post.isLikedByMe) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "Like",
                        tint = if (post.isLikedByMe) BrandBlue else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (post.likesCount > 0) "${post.likesCount}" else "Like",
                        color = if (post.isLikedByMe) BrandBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Comment Button
                TextButton(
                    onClick = {
                        if (isViewingComments) {
                            viewModel.selectPagePostForComments(null)
                        } else {
                            viewModel.selectPagePostForComments(post.id)
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        Icons.Outlined.Comment,
                        contentDescription = "Comments",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (post.commentsCount > 0) "${post.commentsCount} comments" else "Comment",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Share Button
                TextButton(
                    onClick = onShare,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        Icons.Outlined.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Share",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Expandable Comments Section
            AnimatedVisibility(visible = isViewingComments) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Comment Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = commentInputText,
                            onValueChange = { commentInputText = it },
                            placeholder = { Text("Write a public comment...", fontSize = 12.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("page_post_comment_input_${post.id}"),
                            shape = RoundedCornerShape(20.dp),
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                if (commentInputText.isNotBlank()) {
                                    viewModel.onAddPagePostComment(
                                        pagePost = post,
                                        content = commentInputText
                                    )
                                    commentInputText = ""
                                }
                            },
                            enabled = commentInputText.isNotBlank()
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (commentInputText.isNotBlank()) BrandBlue else MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Comments List
                    if (comments.isEmpty()) {
                        Text(
                            text = "No comments yet. Be the first to comment!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            comments.forEach { comment ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AsyncImage(
                                        model = comment.authorAvatarUrl,
                                        contentDescription = comment.authorName,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(comment.authorName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(comment.content, fontSize = 13.sp)
                                        }
                                    }
                                    if (isAdmin || comment.authorId == (viewModel.currentUser.collectAsState().value?.id ?: "")) {
                                        IconButton(
                                            onClick = { viewModel.onDeletePagePostComment(comment.id, post) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Filled.Close, contentDescription = "Delete Comment", tint = CoralRed, modifier = Modifier.size(14.dp))
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
fun PageAboutCard(page: Page) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("About & Contact Details", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            if (page.bio.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Description / Overview", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    Text(page.bio, fontSize = 14.sp, lineHeight = 20.sp)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }

            // Category & Handle
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Filled.Category, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                Column {
                    Text("Category", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    Text(page.category, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }

            // Location
            if (page.location.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = CoralRed, modifier = Modifier.size(20.dp))
                    Column {
                        Text("Location / Headquarters", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Text(page.location, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }

            // Website
            if (page.website.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.Language, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                    Column {
                        Text("Website", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Text(page.website, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = BrandBlue)
                    }
                }
            }

            // Phone
            if (page.phone.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.Phone, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                    Column {
                        Text("Phone / Helpline", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Text(page.phone, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }

            // Email
            if (page.email.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.Email, contentDescription = null, tint = AmberOrange, modifier = Modifier.size(20.dp))
                    Column {
                        Text("Email Address", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Text(page.email, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }

            // Created Date
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
                Column {
                    Text("Page Created", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    val formatted = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(page.createdAt))
                    Text(formatted, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun PageAdminsSection(
    page: Page,
    isOwner: Boolean,
    members: List<PageMember>,
    onAddAdmin: () -> Unit,
    onRemoveMember: (PageMember) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Page Administrators", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Admins can post updates, manage page info, and moderate comments.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
                if (isOwner) {
                    FilledTonalButton(
                        onClick = onAddAdmin,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                members.forEach { member ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AsyncImage(
                            model = member.userAvatarUrl,
                            contentDescription = member.userName,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(member.userName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (member.role == "OWNER") BrandBlue.copy(alpha = 0.15f) else EmeraldGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = member.role,
                                    color = if (member.role == "OWNER") BrandBlue else EmeraldGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (isOwner && member.role != "OWNER") {
                            IconButton(onClick = { onRemoveMember(member) }) {
                                Icon(Icons.Filled.RemoveCircleOutline, contentDescription = "Remove Admin", tint = CoralRed)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreatePageDialog(
    onDismiss: () -> Unit,
    onSubmit: (name: String, username: String, category: String, bio: String, avatar: String, cover: String, phone: String, email: String, website: String, location: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("News & Media") }
    var bio by remember { mutableStateOf("") }
    var avatarUrl by remember { mutableStateOf(PageAvatarPresets.first().first) }
    var coverUrl by remember { mutableStateOf(PageCoverPresets.first().first) }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("Chitral, Pakistan") }

    var isProcessingCover by remember { mutableStateOf(false) }
    var isProcessingAvatar by remember { mutableStateOf(false) }
    var isCustomCover by remember { mutableStateOf(false) }
    var isCustomAvatar by remember { mutableStateOf(false) }

    // Cover Photo Picker Launcher from Device
    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isProcessingCover = true
                try {
                    val savedFile = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            val file = File(context.filesDir, "page_cover_${System.currentTimeMillis()}.jpg")
                            file.outputStream().use { output -> input.copyTo(output) }
                            file
                        }
                    }
                    if (savedFile != null) {
                        coverUrl = Uri.fromFile(savedFile).toString()
                        isCustomCover = true
                        Toast.makeText(context, "Cover photo chosen from device!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not load cover photo", Toast.LENGTH_SHORT).show()
                } finally {
                    isProcessingCover = false
                }
            }
        }
    }

    // Page Icon / Avatar Picker Launcher from Device
    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isProcessingAvatar = true
                try {
                    val savedFile = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            val file = File(context.filesDir, "page_icon_${System.currentTimeMillis()}.jpg")
                            file.outputStream().use { output -> input.copyTo(output) }
                            file
                        }
                    }
                    if (savedFile != null) {
                        avatarUrl = Uri.fromFile(savedFile).toString()
                        isCustomAvatar = true
                        Toast.makeText(context, "Page icon chosen from device!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not load page icon", Toast.LENGTH_SHORT).show()
                } finally {
                    isProcessingAvatar = false
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .testTag("create_page_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Create Official Page", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Establish a presence for your brand, media, or organization", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Page Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (username.isBlank() && it.isNotBlank()) {
                                username = it.lowercase().replace("[^a-z0-9]".toRegex(), "")
                            }
                        },
                        label = { Text("Page Name *") },
                        placeholder = { Text("e.g. Chitral Today, Booni Sports Club") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("page_input_name"),
                        singleLine = true
                    )

                    // Username / Handle
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it.lowercase().replace(" ", "").replace("@", "") },
                        label = { Text("Page Username / Deep Link Handle *") },
                        prefix = { Text("@") },
                        placeholder = { Text("chitrallive") },
                        supportingText = { Text("Appears in your URL: yarkhoon.com/page/@$username") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("page_input_username"),
                        singleLine = true
                    )

                    // Category Selector
                    Text("Category", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(PageCategories.filter { it != "All" }) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 12.sp) }
                            )
                        }
                    }

                    // Bio
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Bio & Description") },
                        placeholder = { Text("Tell the community what your page is about...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )

                    // --- COVER PHOTO (Device Upload + Live Preview + Presets) ---
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Page Cover Photo", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            if (isCustomCover) {
                                Surface(
                                    color = Color(0xFFE8F5E9),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "✓ Device Upload",
                                        color = Color(0xFF2E7D32),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Cover Preview Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(125.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    width = 1.5.dp,
                                    color = if (isCustomCover) BrandBlue else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            AsyncImage(
                                model = coverUrl,
                                contentDescription = "Cover photo preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (isProcessingCover) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Upload cover from device button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { coverPickerLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("upload_page_cover_button")
                            ) {
                                Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (isCustomCover) "Change Device Cover" else "Upload Cover from Device",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (isCustomCover) {
                                OutlinedButton(
                                    onClick = {
                                        coverUrl = PageCoverPresets.first().first
                                        isCustomCover = false
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("reset_cover_preset_button")
                                ) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Preset", fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Or choose from scenic presets:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(PageCoverPresets) { (url, title) ->
                                val isSelected = !isCustomCover && coverUrl == url
                                Box(
                                    modifier = Modifier
                                        .size(width = 100.dp, height = 60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) BrandBlue else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            coverUrl = url
                                            isCustomCover = false
                                        }
                                ) {
                                    AsyncImage(model = url, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.6f),
                                        modifier = Modifier.align(Alignment.BottomCenter)
                                    ) {
                                        Text(title, fontSize = 9.sp, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                }
                            }
                        }
                    }

                    // --- PAGE ICON / AVATAR (Device Upload + Live Preview + Presets) ---
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Page Icon / Profile Picture", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            if (isCustomAvatar) {
                                Surface(
                                    color = Color(0xFFE8F5E9),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "✓ Device Upload",
                                        color = Color(0xFF2E7D32),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Circular Preview
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = 2.dp,
                                        color = if (isCustomAvatar) BrandBlue else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    )
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = "Avatar preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isProcessingAvatar) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                    }
                                }
                            }

                            // Upload buttons
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { avatarPickerLauncher.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("upload_page_icon_button")
                                ) {
                                    Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (isCustomAvatar) "Change Device Icon" else "Upload Icon from Device",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (isCustomAvatar) {
                                    OutlinedButton(
                                        onClick = {
                                            avatarUrl = PageAvatarPresets.first().first
                                            isCustomAvatar = false
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("reset_avatar_preset_button")
                                    ) {
                                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Use Preset Icon", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Or choose a preset icon:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(PageAvatarPresets) { (url, title) ->
                                val isSelected = !isCustomAvatar && avatarUrl == url
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) BrandBlue else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            avatarUrl = url
                                            isCustomAvatar = false
                                        }
                                ) {
                                    AsyncImage(model = url, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }

                    // Contact info
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        placeholder = { Text("Chitral, Pakistan") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number / WhatsApp") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Contact Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = website,
                        onValueChange = { website = it },
                        label = { Text("Website") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        onSubmit(name, username, category, bio, avatarUrl, coverUrl, phone, email, website, location)
                    },
                    enabled = name.isNotBlank() && username.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("create_page_submit_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Create Page", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun EditPageDialog(
    page: Page,
    onDismiss: () -> Unit,
    onSubmit: (name: String, category: String, bio: String, avatar: String, cover: String, phone: String, email: String, website: String, location: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var name by remember { mutableStateOf(page.name) }
    var category by remember { mutableStateOf(page.category) }
    var bio by remember { mutableStateOf(page.bio) }
    var avatarUrl by remember { mutableStateOf(page.avatarUrl) }
    var coverUrl by remember { mutableStateOf(page.coverUrl) }
    var phone by remember { mutableStateOf(page.phone) }
    var email by remember { mutableStateOf(page.email) }
    var website by remember { mutableStateOf(page.website) }
    var location by remember { mutableStateOf(page.location) }

    var isProcessingCover by remember { mutableStateOf(false) }
    var isProcessingAvatar by remember { mutableStateOf(false) }

    // Cover Photo Picker Launcher from Device
    val editCoverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isProcessingCover = true
                try {
                    val savedFile = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            val file = File(context.filesDir, "page_cover_${System.currentTimeMillis()}.jpg")
                            file.outputStream().use { output -> input.copyTo(output) }
                            file
                        }
                    }
                    if (savedFile != null) {
                        coverUrl = Uri.fromFile(savedFile).toString()
                        Toast.makeText(context, "Cover photo updated from device!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not load cover photo", Toast.LENGTH_SHORT).show()
                } finally {
                    isProcessingCover = false
                }
            }
        }
    }

    // Page Icon / Avatar Picker Launcher from Device
    val editAvatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isProcessingAvatar = true
                try {
                    val savedFile = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            val file = File(context.filesDir, "page_icon_${System.currentTimeMillis()}.jpg")
                            file.outputStream().use { output -> input.copyTo(output) }
                            file
                        }
                    }
                    if (savedFile != null) {
                        avatarUrl = Uri.fromFile(savedFile).toString()
                        Toast.makeText(context, "Page icon updated from device!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not load page icon", Toast.LENGTH_SHORT).show()
                } finally {
                    isProcessingAvatar = false
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .testTag("edit_page_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Edit Page Information", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Update your page brand details and media", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Page Name *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_page_name_input"),
                        singleLine = true
                    )

                    // Category Selector
                    Text("Category", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(PageCategories.filter { it != "All" }) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 12.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Bio & Description") },
                        placeholder = { Text("Tell the community what your page is about...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )

                    // --- COVER PHOTO SECTION ---
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Page Cover Photo", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(125.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    width = 1.5.dp,
                                    color = BrandBlue,
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            AsyncImage(
                                model = coverUrl.ifBlank { "https://images.unsplash.com/photo-1542224566-6e85f2e6772f?w=800" },
                                contentDescription = "Cover photo preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (isProcessingCover) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { editCoverPickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_page_upload_cover_button")
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload New Cover from Device", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Or choose scenic preset:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(PageCoverPresets) { (url, title) ->
                                val isSelected = coverUrl == url
                                Box(
                                    modifier = Modifier
                                        .size(width = 100.dp, height = 60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) BrandBlue else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { coverUrl = url }
                                ) {
                                    AsyncImage(model = url, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.6f),
                                        modifier = Modifier.align(Alignment.BottomCenter)
                                    ) {
                                        Text(title, fontSize = 9.sp, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                }
                            }
                        }
                    }

                    // --- PAGE ICON / AVATAR SECTION ---
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Page Icon / Profile Picture", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .border(width = 2.dp, color = BrandBlue, shape = CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = avatarUrl.ifBlank { "https://images.unsplash.com/photo-1585829365295-ab7cd400c167?w=200" },
                                    contentDescription = "Avatar preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isProcessingAvatar) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                    }
                                }
                            }

                            Button(
                                onClick = { editAvatarPickerLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("edit_page_upload_icon_button")
                            ) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload New Icon from Device", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Or choose a preset icon:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(PageAvatarPresets) { (url, title) ->
                                val isSelected = avatarUrl == url
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) BrandBlue else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { avatarUrl = url }
                                ) {
                                    AsyncImage(model = url, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        placeholder = { Text("Chitral, Pakistan") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = website,
                        onValueChange = { website = it },
                        label = { Text("Website") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        onSubmit(name, category, bio, avatarUrl, coverUrl, phone, email, website, location)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("edit_page_save_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun CreatePagePostDialog(
    page: Page,
    onDismiss: () -> Unit,
    onSubmit: (content: String, mediaType: String, mediaUrls: List<String>, linkUrl: String, isPinned: Boolean) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var linkUrl by remember { mutableStateOf("") }
    var selectedMediaUrl by remember { mutableStateOf("") }
    var isPinned by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .testTag("create_page_post_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AsyncImage(
                            model = page.avatarUrl,
                            contentDescription = page.name,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                        )
                        Column {
                            Text("Post as ${page.name}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Official broadcast", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        placeholder = { Text("What would you like to announce to your followers?") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("page_post_content_input"),
                        minLines = 4,
                        maxLines = 8
                    )

                    // Link input
                    OutlinedTextField(
                        value = linkUrl,
                        onValueChange = { linkUrl = it },
                        label = { Text("Link URL (optional)") },
                        leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null, tint = BrandBlue) },
                        placeholder = { Text("https://...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Sample Image Quick Attachment
                    Text("Attach Scenic Photo", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PageCoverPresets) { (url, title) ->
                            val isChosen = selectedMediaUrl == url
                            Box(
                                modifier = Modifier
                                    .size(width = 80.dp, height = 50.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isChosen) 2.5.dp else 1.dp,
                                        color = if (isChosen) BrandBlue else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        selectedMediaUrl = if (isChosen) "" else url
                                    }
                            ) {
                                AsyncImage(model = url, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            }
                        }
                    }

                    // Pinned toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.PushPin, contentDescription = null, tint = BrandBlue)
                            Text("Pin this update to top", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Switch(checked = isPinned, onCheckedChange = { isPinned = it })
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val mediaList = if (selectedMediaUrl.isNotBlank()) listOf(selectedMediaUrl) else emptyList()
                        val mediaType = if (selectedMediaUrl.isNotBlank()) "IMAGE" else "NONE"
                        onSubmit(content, mediaType, mediaList, linkUrl, isPinned)
                    },
                    enabled = content.isNotBlank() || selectedMediaUrl.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("page_post_publish_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Publish Update", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun AddAdminDialog(
    allUsers: List<User>,
    onDismiss: () -> Unit,
    onSelectUser: (User) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(allUsers, searchQuery) {
        allUsers.filter {
            searchQuery.isBlank() ||
                    it.fullName.contains(searchQuery, ignoreCase = true) ||
                    it.username.contains(searchQuery, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Add Page Administrator", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search users...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered) { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectUser(user) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = user.fullName,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("@${user.username}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                            }
                            Button(
                                onClick = { onSelectUser(user) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                            ) {
                                Text("Add", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportPageDialog(
    page: Page,
    onDismiss: () -> Unit,
    onSubmit: (reason: String, details: String) -> Unit
) {
    val reportReasons = listOf(
        "Misinformation / Fake News",
        "Harassment / Hate Speech",
        "Impersonation of Official Entity",
        "Spam / Fraudulent Content",
        "Inappropriate Media",
        "Other"
    )
    var selectedReason by remember { mutableStateOf(reportReasons.first()) }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Page '${page.name}'") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Help us keep Yarkhoon safe. What is the issue with this page?", fontSize = 13.sp)

                reportReasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason }
                        )
                        Text(reason, fontSize = 13.sp)
                    }
                }

                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    placeholder = { Text("Additional details (optional)...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedReason, details) },
                colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
            ) {
                Text("Submit Report", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
