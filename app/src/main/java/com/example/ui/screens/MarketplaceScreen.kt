package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.LocalDining
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.MarketplaceItem
import com.example.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val EmeraldGreen = Color(0xFF00A86B)
private val WarmOrange = Color(0xFFF77F00)
private val DeepPurple = Color(0xFF7B1FA2)
private val AmberGold = Color(0xFFD97706)
private val RoseRed = Color(0xFFE11D48)
private val SkyCyan = Color(0xFF0284C7)

/**
 * Category metadata for marketplace navigation and filtering
 */
data class CategoryDefinition(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val tagColor: Color,
    val description: String,
    val aliases: List<String> = emptyList()
)

val MARKETPLACE_CATEGORIES = listOf(
    CategoryDefinition(
        id = "All",
        name = "All Items",
        icon = Icons.Outlined.Storefront,
        tagColor = FacebookBlue,
        description = "Explore everything in Yarkhoon & Chitral Marketplace"
    ),
    CategoryDefinition(
        id = "Electronics",
        name = "Electronics",
        icon = Icons.Outlined.Devices,
        tagColor = SkyCyan,
        description = "Solar gear, mountain walkie-talkies, cameras, gadgets & phones",
        aliases = listOf("Electronics", "Gadgets", "Tech", "Device", "Solar", "Radio", "Camera")
    ),
    CategoryDefinition(
        id = "Clothing",
        name = "Clothing",
        icon = Icons.Outlined.Checkroom,
        tagColor = RoseRed,
        description = "Handwoven woolen Shu, Pakol caps, jackets, shawls & warm wear",
        aliases = listOf("Clothing", "Apparel", "Fashion", "Wear", "Shu", "Cap", "Pakol", "Shawl")
    ),
    CategoryDefinition(
        id = "Home Goods",
        name = "Home Goods",
        icon = Icons.Outlined.Weekend,
        tagColor = WarmOrange,
        description = "Walnut carvings, handmade wool rugs, cedar chests & home items",
        aliases = listOf("Home Goods", "Home Decor", "Furniture", "Craft", "Handicrafts", "Rug", "Carving")
    ),
    CategoryDefinition(
        id = "Food & Organic",
        name = "Food & Organic",
        icon = Icons.Outlined.LocalDining,
        tagColor = EmeraldGreen,
        description = "Sun-dried apricots, pure mountain honey, walnuts & organic foods",
        aliases = listOf("Food & Organic", "Food & Edibles", "Food", "Organic", "Honey", "Dry Fruits")
    ),
    CategoryDefinition(
        id = "Sports & Outdoor",
        name = "Sports & Outdoor",
        icon = Icons.Outlined.Terrain,
        tagColor = DeepPurple,
        description = "Trekking poles, 4-season tents, sleeping bags & alpine gear",
        aliases = listOf("Sports & Outdoor", "Sports", "Outdoor", "Trekking", "Camping", "Tent", "Hiking")
    ),
    CategoryDefinition(
        id = "Books & Culture",
        name = "Books & Culture",
        icon = Icons.Outlined.MenuBook,
        tagColor = AmberGold,
        description = "Khowar dictionaries, Hindukush history, local literature & music",
        aliases = listOf("Books & Culture", "Books", "Art & Culture", "Culture", "Literature")
    ),
    CategoryDefinition(
        id = "Vehicles & Parts",
        name = "Vehicles & Parts",
        icon = Icons.Outlined.DirectionsCar,
        tagColor = Color(0xFF475569),
        description = "4x4 Jeep parts, snow chains, travel accessories & tires",
        aliases = listOf("Vehicles & Parts", "Vehicles", "Parts", "Jeep", "Automotive")
    )
)

/**
 * Category matching logic supporting aliases and fallback search
 */
fun matchesCategory(itemCategory: String, targetCategoryId: String): Boolean {
    if (targetCategoryId == "All") return true
    val catDef = MARKETPLACE_CATEGORIES.firstOrNull { it.id.equals(targetCategoryId, ignoreCase = true) }
    if (catDef != null) {
        if (catDef.aliases.any { alias -> itemCategory.contains(alias, ignoreCase = true) }) {
            return true
        }
    }
    return itemCategory.equals(targetCategoryId, ignoreCase = true) ||
            itemCategory.contains(targetCategoryId, ignoreCase = true)
}

fun getCategoryDefinition(categoryName: String): CategoryDefinition {
    for (cat in MARKETPLACE_CATEGORIES) {
        if (cat.id == "All") continue
        if (cat.id.equals(categoryName, ignoreCase = true) ||
            cat.aliases.any { it.equals(categoryName, ignoreCase = true) || categoryName.contains(it, ignoreCase = true) }
        ) {
            return cat
        }
    }
    return CategoryDefinition(
        id = categoryName,
        name = categoryName,
        icon = Icons.Outlined.Storefront,
        tagColor = FacebookBlue,
        description = "Marketplace Listing"
    )
}

enum class MarketSortOption(val label: String) {
    NEWEST("Newest First"),
    PRICE_LOW_HIGH("Price: Low to High"),
    PRICE_HIGH_LOW("Price: High to Low")
}

enum class MarketAvailability(val label: String) {
    ALL("All Items"),
    AVAILABLE("Available"),
    SOLD("Sold")
}

enum class PricePreset(val label: String, val min: Double?, val max: Double?) {
    ALL("All Prices", null, null),
    UNDER_25("Under $25", null, 25.0),
    BETWEEN_25_75("$25 - $75", 25.0, 75.0),
    OVER_75("Over $75", 75.0, null)
}

// ==================== MAIN MARKETPLACE SCREEN ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    items: List<MarketplaceItem>,
    currentUser: User?,
    onToggleSold: (MarketplaceItem) -> Unit,
    onDeleteItem: (Int) -> Unit = {},
    onAddListingClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf("All") }
    var sortOption by remember { mutableStateOf(MarketSortOption.NEWEST) }
    var availabilityFilter by remember { mutableStateOf(MarketAvailability.ALL) }
    var pricePreset by remember { mutableStateOf(PricePreset.ALL) }
    var showCategoryPickerSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    // Selected item for deep detail sheet preview
    var selectedItemForDetail by remember { mutableStateOf<MarketplaceItem?>(null) }

    // Compute live item counts per category
    val categoryCounts = remember(items) {
        MARKETPLACE_CATEGORIES.associate { cat ->
            cat.id to if (cat.id == "All") {
                items.size
            } else {
                items.count { item -> matchesCategory(item.category, cat.id) }
            }
        }
    }

    // Filter and sort items
    val filteredItems = remember(
        items,
        searchQuery,
        selectedCategoryId,
        sortOption,
        availabilityFilter,
        pricePreset
    ) {
        items
            .filter { item ->
                // Search query match
                val matchSearch = searchQuery.isBlank() ||
                        item.title.contains(searchQuery, ignoreCase = true) ||
                        item.description.contains(searchQuery, ignoreCase = true) ||
                        item.sellerName.contains(searchQuery, ignoreCase = true)

                // Category match
                val matchCategory = matchesCategory(item.category, selectedCategoryId)

                // Availability match
                val matchAvailability = when (availabilityFilter) {
                    MarketAvailability.ALL -> true
                    MarketAvailability.AVAILABLE -> !item.isSold
                    MarketAvailability.SOLD -> item.isSold
                }

                // Price match
                val matchPrice = when {
                    pricePreset.min != null && pricePreset.max != null ->
                        item.price >= pricePreset.min!! && item.price <= pricePreset.max!!
                    pricePreset.min != null ->
                        item.price >= pricePreset.min!!
                    pricePreset.max != null ->
                        item.price <= pricePreset.max!!
                    else -> true
                }

                matchSearch && matchCategory && matchAvailability && matchPrice
            }
            .let { list ->
                when (sortOption) {
                    MarketSortOption.NEWEST -> list.sortedByDescending { it.timestamp }
                    MarketSortOption.PRICE_LOW_HIGH -> list.sortedBy { it.price }
                    MarketSortOption.PRICE_HIGH_LOW -> list.sortedByDescending { it.price }
                }
            }
    }

    val activeFiltersCount = (if (selectedCategoryId != "All") 1 else 0) +
            (if (availabilityFilter != MarketAvailability.ALL) 1 else 0) +
            (if (pricePreset != PricePreset.ALL) 1 else 0) +
            (if (sortOption != MarketSortOption.NEWEST) 1 else 0)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("marketplace_tab_view")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Search and Category Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 8.dp)
                ) {
                    // Search Bar and Category Explorer Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    if (selectedCategoryId == "All") "Search in Marketplace..." else "Search $selectedCategoryId...",
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = "Search icon",
                                    tint = FacebookBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.size(28.dp).testTag("clear_marketplace_search")
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                focusedBorderColor = FacebookBlue,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("marketplace_search")
                        )

                        // Browse All Categories Modal Trigger
                        Surface(
                            shape = CircleShape,
                            color = if (showCategoryPickerSheet || selectedCategoryId != "All") FacebookBlue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable { showCategoryPickerSheet = true }
                                .testTag("open_category_grid_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.Category,
                                    contentDescription = "Browse Categories",
                                    tint = if (selectedCategoryId != "All") FacebookBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Filter & Sort BottomSheet Trigger
                        Surface(
                            shape = CircleShape,
                            color = if (activeFiltersCount > 0) FacebookBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable { showFilterSheet = true }
                                .testTag("open_filter_sort_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                BadgedBox(
                                    badge = {
                                        if (activeFiltersCount > 0) {
                                            Badge(containerColor = WarmOrange) {
                                                Text(activeFiltersCount.toString(), fontSize = 10.sp)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Filled.Tune,
                                        contentDescription = "Filter and Sort",
                                        tint = if (activeFiltersCount > 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Horizontal Scrolling Category Filter Bar
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("marketplace_category_rail")
                    ) {
                        items(MARKETPLACE_CATEGORIES, key = { it.id }) { cat ->
                            val isSelected = selectedCategoryId == cat.id
                            val count = categoryCounts[cat.id] ?: 0

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) cat.tagColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable {
                                        selectedCategoryId = cat.id
                                    }
                                    .testTag("category_filter_${cat.id}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Icon(
                                        imageVector = cat.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected) Color.White else cat.tagColor
                                    )

                                    Text(
                                        text = cat.name,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )

                                    // Category count chip
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "$count",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Active Filter Pill Summary & Quick Dismiss
            AnimatedVisibility(
                visible = activeFiltersCount > 0 || searchQuery.isNotBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Active Filters:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (selectedCategoryId != "All") {
                        ActiveFilterChip(
                            label = "Category: $selectedCategoryId",
                            onRemove = { selectedCategoryId = "All" }
                        )
                    }

                    if (searchQuery.isNotBlank()) {
                        ActiveFilterChip(
                            label = "Search: \"$searchQuery\"",
                            onRemove = { searchQuery = "" }
                        )
                    }

                    if (availabilityFilter != MarketAvailability.ALL) {
                        ActiveFilterChip(
                            label = availabilityFilter.label,
                            onRemove = { availabilityFilter = MarketAvailability.ALL }
                        )
                    }

                    if (pricePreset != PricePreset.ALL) {
                        ActiveFilterChip(
                            label = pricePreset.label,
                            onRemove = { pricePreset = PricePreset.ALL }
                        )
                    }

                    if (sortOption != MarketSortOption.NEWEST) {
                        ActiveFilterChip(
                            label = sortOption.label,
                            onRemove = { sortOption = MarketSortOption.NEWEST }
                        )
                    }

                    TextButton(
                        onClick = {
                            selectedCategoryId = "All"
                            searchQuery = ""
                            availabilityFilter = MarketAvailability.ALL
                            pricePreset = PricePreset.ALL
                            sortOption = MarketSortOption.NEWEST
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Reset All", fontSize = 11.sp, color = FacebookBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Results Counter & Category Hero Header (when specific category selected)
            if (selectedCategoryId != "All") {
                val currentCategoryDef = MARKETPLACE_CATEGORIES.firstOrNull { it.id == selectedCategoryId }
                currentCategoryDef?.let { cat ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = cat.tagColor.copy(alpha = 0.08f)
                        ),
                        border = BorderStroke(1.dp, cat.tagColor.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = cat.tagColor,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(cat.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cat.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = cat.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 14.sp
                                )
                            }

                            IconButton(
                                onClick = { selectedCategoryId = "All" },
                                modifier = Modifier.size(28.dp).testTag("close_category_banner")
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "View all items", tint = cat.tagColor, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Main Product Grid
            if (filteredItems.isEmpty()) {
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
                                    Icons.Outlined.Storefront,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Text(
                            text = if (selectedCategoryId != "All") "No items in $selectedCategoryId" else "No listings match your search",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Try clearing filters, searching for something else, or list a new item to sell.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    selectedCategoryId = "All"
                                    searchQuery = ""
                                    availabilityFilter = MarketAvailability.ALL
                                    pricePreset = PricePreset.ALL
                                }
                            ) {
                                Text("Clear Filters")
                            }

                            Button(
                                onClick = onAddListingClick,
                                colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sell Item")
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("marketplace_grid")
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        MarketplaceProductCard(
                            item = item,
                            currentUser = currentUser,
                            onItemClick = { selectedItemForDetail = item },
                            onToggleSold = { onToggleSold(item) },
                            onDelete = { onDeleteItem(item.id) }
                        )
                    }
                }
            }
        }
    }

    // ==================== MODALS & SHEETS ====================

    // 1. Browse All Categories Modal Sheet
    if (showCategoryPickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCategoryPickerSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Browse by Category",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { showCategoryPickerSheet = false }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(MARKETPLACE_CATEGORIES) { cat ->
                        val isSelected = selectedCategoryId == cat.id
                        val count = categoryCounts[cat.id] ?: 0

                        Card(
                            modifier = Modifier
                                .testTag("category_modal_item_${cat.id}")
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    selectedCategoryId = cat.id
                                    showCategoryPickerSheet = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) cat.tagColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(14.dp),
                            border = if (isSelected) BorderStroke(2.dp, cat.tagColor) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = cat.tagColor,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = cat.icon,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = cat.tagColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "$count",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = cat.tagColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = cat.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = cat.description,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 2. Filter & Sort Options Sheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filter & Sort Listings",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { showFilterSheet = false }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                // Sort Section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Sort By", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FacebookBlue)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MarketSortOption.values().forEach { opt ->
                            FilterChip(
                                selected = sortOption == opt,
                                onClick = { sortOption = opt },
                                label = { Text(opt.label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FacebookBlue.copy(alpha = 0.15f),
                                    selectedLabelColor = FacebookBlue
                                )
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Availability Section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Availability", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FacebookBlue)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MarketAvailability.values().forEach { avail ->
                            FilterChip(
                                selected = availabilityFilter == avail,
                                onClick = { availabilityFilter = avail },
                                label = { Text(avail.label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FacebookBlue.copy(alpha = 0.15f),
                                    selectedLabelColor = FacebookBlue
                                )
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Price Range Section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Price Range", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FacebookBlue)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PricePreset.values()) { preset ->
                            FilterChip(
                                selected = pricePreset == preset,
                                onClick = { pricePreset = preset },
                                label = { Text(preset.label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FacebookBlue.copy(alpha = 0.15f),
                                    selectedLabelColor = FacebookBlue
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            sortOption = MarketSortOption.NEWEST
                            availabilityFilter = MarketAvailability.ALL
                            pricePreset = PricePreset.ALL
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset")
                    }

                    Button(
                        onClick = { showFilterSheet = false },
                        colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Apply Filters")
                    }
                }
            }
        }
    }

    // 3. Deep Detail Sheet for a Selected Item
    selectedItemForDetail?.let { item ->
        MarketplaceDetailSheet(
            item = item,
            currentUser = currentUser,
            allItems = items,
            onDismiss = { selectedItemForDetail = null },
            onToggleSold = {
                onToggleSold(item)
                selectedItemForDetail = item.copy(isSold = !item.isSold)
            },
            onDelete = {
                onDeleteItem(item.id)
                selectedItemForDetail = null
            },
            onSelectRelatedItem = { related ->
                selectedItemForDetail = related
            }
        )
    }
}

/**
 * Modern product item card with category pill, condition, price, and instant seller actions
 */
@Composable
fun MarketplaceProductCard(
    item: MarketplaceItem,
    currentUser: User?,
    onItemClick: () -> Unit,
    onToggleSold: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val categoryDef = getCategoryDefinition(item.category)
    val isMyItem = item.sellerId == "currentUser" || item.sellerId == (currentUser?.id ?: "")

    Card(
        onClick = onItemClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.testTag("market_card_${item.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(135.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Price badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                ) {
                    Text(
                        "$${item.price.toInt()}",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                // Category pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = categoryDef.tagColor,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            categoryDef.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = categoryDef.name,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (item.isSold) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = RoseRed
                        ) {
                            Text(
                                "SOLD",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.sellerName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FacebookBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (isMyItem) {
                    OutlinedButton(
                        onClick = onToggleSold,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .height(32.dp)
                            .testTag("mark_sold_button_${item.id}")
                    ) {
                        Text(if (item.isSold) "Relist Item" else "Mark as Sold", fontSize = 11.sp)
                    }
                } else {
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(
                                    Intent.ACTION_DIAL,
                                    Uri.parse("tel:${item.sellerContact}")
                                )
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Contact: ${item.sellerContact}", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .height(32.dp)
                            .testTag("contact_seller_${item.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                            Text("Contact", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Deep Detail Modal Sheet displaying item specifications, category information, and related listings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceDetailSheet(
    item: MarketplaceItem,
    currentUser: User?,
    allItems: List<MarketplaceItem>,
    onDismiss: () -> Unit,
    onToggleSold: () -> Unit,
    onDelete: () -> Unit,
    onSelectRelatedItem: (MarketplaceItem) -> Unit
) {
    val context = LocalContext.current
    val categoryDef = getCategoryDefinition(item.category)
    val isMyItem = item.sellerId == "currentUser" || item.sellerId == (currentUser?.id ?: "")

    val relatedItems = remember(item, allItems) {
        allItems.filter { it.id != item.id && matchesCategory(it.category, categoryDef.id) }.take(4)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = categoryDef.tagColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, categoryDef.tagColor.copy(alpha = 0.3f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(categoryDef.icon, contentDescription = null, tint = categoryDef.tagColor, modifier = Modifier.size(14.dp))
                        Text(categoryDef.name, color = categoryDef.tagColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Check out ${item.title} on Yarkhoon Marketplace")
                                putExtra(Intent.EXTRA_TEXT, "Check out \"${item.title}\" for $${item.price} in Yarkhoon Marketplace: https://yarkhoon.com/marketplace/${item.id}")
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Listing"))
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Share", tint = FacebookBlue)
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
            }

            // Image Hero View
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (item.isSold) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.65f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(shape = RoundedCornerShape(6.dp), color = RoseRed) {
                                Text(
                                    "ITEM SOLD",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Price & Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = FacebookBlue, modifier = Modifier.size(13.dp))
                        Text("Yarkhoon Valley & Chitral", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = FacebookBlue
                ) {
                    Text(
                        text = "$${item.price}",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Description
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Description", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = item.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Seller Contact Box
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = FacebookBlue.copy(alpha = 0.15f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = FacebookBlue, modifier = Modifier.size(24.dp))
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.sellerName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Seller Contact: ${item.sellerContact}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Action Buttons
            if (isMyItem) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onToggleSold,
                        colors = ButtonDefaults.buttonColors(containerColor = if (item.isSold) EmeraldGreen else WarmOrange),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (item.isSold) "Relist Item" else "Mark as Sold", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.sellerContact}"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Contact: ${item.sellerContact}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Call Seller", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:${item.sellerContact}")).apply {
                                    putExtra("sms_body", "Hi ${item.sellerName}, I am interested in your listing \"${item.title}\" on Yarkhoon Marketplace.")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Contact: ${item.sellerContact}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SMS Message", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            // Related Items in Same Category
            if (relatedItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "More in ${categoryDef.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(relatedItems) { related ->
                        Card(
                            onClick = { onSelectRelatedItem(related) },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.width(140.dp)
                        ) {
                            Column {
                                AsyncImage(
                                    model = related.imageUrl,
                                    contentDescription = related.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                )
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = related.title,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "$${related.price}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = FacebookBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Reusable chip for active filter dismissals
 */
@Composable
private fun ActiveFilterChip(
    label: String,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = FacebookBlue.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, FacebookBlue.copy(alpha = 0.3f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 3.dp, bottom = 3.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = FacebookBlue,
                fontWeight = FontWeight.Medium
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    Icons.Filled.Clear,
                    contentDescription = "Remove filter",
                    tint = FacebookBlue,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
