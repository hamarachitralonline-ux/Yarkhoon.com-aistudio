package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.components.BusinessCard
import com.example.ui.components.MarketplaceItemCard
import com.example.ui.theme.FbBlue
import com.example.ui.theme.FbBlueLight

@Composable
fun BusinessHubScreen(
    // Current user for permissions (edit/delete)
    currentUser: User,
    // Active Hub Section (Services vs Buy & Sell)
    activeSection: BusinessHubSection,
    onSectionSelected: (BusinessHubSection) -> Unit,

    // Business & Services State
    businesses: List<BusinessProfile>,
    selectedBusinessCategory: BusinessCategory,
    onBusinessCategorySelected: (BusinessCategory) -> Unit,
    businessSearchQuery: String,
    onBusinessSearchQueryChange: (String) -> Unit,
    onContactBusiness: (BusinessProfile) -> Unit,
    onOpenRegisterBusiness: () -> Unit = {},

    // Buy & Sell Marketplace State
    marketplaceItems: List<MarketplaceItem>,
    selectedMarketplaceCategory: MarketplaceCategory,
    onMarketplaceCategorySelected: (MarketplaceCategory) -> Unit,
    marketplaceSearchQuery: String,
    onMarketplaceSearchQueryChange: (String) -> Unit,
    onContactSeller: (MarketplaceItem) -> Unit,
    onOpenSellItem: () -> Unit,
    onEditMarketplaceItem: (MarketplaceItem) -> Unit,
    onDeleteMarketplaceItem: (String) -> Unit = {},
    onMarkMarketplaceItemSold: (String) -> Unit = {},
    onReportBusiness: ((BusinessProfile) -> Unit)? = null,
    onReportMarketplaceItem: ((MarketplaceItem) -> Unit)? = null,

    modifier: Modifier = Modifier
) {
    // Filter businesses
    val filteredBusinesses = businesses.filter { biz ->
        val matchesCategory = selectedBusinessCategory == BusinessCategory.ALL || biz.category == selectedBusinessCategory
        val matchesQuery = businessSearchQuery.isBlank() ||
                biz.businessName.contains(businessSearchQuery, ignoreCase = true) ||
                biz.serviceHighlight.contains(businessSearchQuery, ignoreCase = true) ||
                biz.location.contains(businessSearchQuery, ignoreCase = true)
        matchesCategory && matchesQuery
    }

    // Filter marketplace items
    val filteredMarketplaceItems = marketplaceItems.filter { item ->
        val matchesCategory = selectedMarketplaceCategory == MarketplaceCategory.ALL || item.category == selectedMarketplaceCategory
        val matchesQuery = marketplaceSearchQuery.isBlank() ||
                item.title.contains(marketplaceSearchQuery, ignoreCase = true) ||
                item.description.contains(marketplaceSearchQuery, ignoreCase = true) ||
                item.location.contains(marketplaceSearchQuery, ignoreCase = true) ||
                item.price.contains(marketplaceSearchQuery, ignoreCase = true)
        matchesCategory && matchesQuery
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("business_hub_screen"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Tabs Header (Business & Services vs Buy and Sell)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hub_section_selector"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Tab 1: Business & Services
                    val isServicesActive = activeSection == BusinessHubSection.SERVICES
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSectionSelected(BusinessHubSection.SERVICES) }
                            .testTag("tab_business_services"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isServicesActive) FbBlue else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.BusinessCenter,
                                contentDescription = null,
                                tint = if (isServicesActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Services",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isServicesActive) FontWeight.Bold else FontWeight.Medium,
                                color = if (isServicesActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Tab 2: Buy and Sell
                    val isMarketplaceActive = activeSection == BusinessHubSection.MARKETPLACE
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSectionSelected(BusinessHubSection.MARKETPLACE) }
                            .testTag("tab_buy_and_sell"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isMarketplaceActive) FbBlue else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = if (isMarketplaceActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Buy & Sell",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isMarketplaceActive) FontWeight.Bold else FontWeight.Medium,
                                color = if (isMarketplaceActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Section Banner & Quick Action
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("business_hub_banner"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FbBlue)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    if (activeSection == BusinessHubSection.SERVICES) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BusinessCenter,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Business & Services Hub",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            FilledTonalButton(
                                onClick = onOpenRegisterBusiness,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color.White,
                                    contentColor = FbBlue
                                ),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("register_service_btn")
                            ) {
                                Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Register", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Find verified electricians, plumbers, hotels, mechanics, solar technicians, carpenters, teachers, and skilled trades across Yarkhoon & Chitral.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    } else {
                        // Buy and Sell Marketplace Banner
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Buy & Sell Marketplace",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Button(
                                onClick = onOpenSellItem,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = FbBlue
                                ),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("sell_an_item_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sell an Item", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Buy and sell cars, motorbikes, mobile phones, fresh fruits, organic vegetables, farm produce, and local products with zero broker fees.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = if (activeSection == BusinessHubSection.SERVICES) businessSearchQuery else marketplaceSearchQuery,
                onValueChange = if (activeSection == BusinessHubSection.SERVICES) onBusinessSearchQueryChange else onMarketplaceSearchQueryChange,
                placeholder = {
                    Text(
                        if (activeSection == BusinessHubSection.SERVICES) {
                            "Find a service, plumber, electrician, hotel, mechanic..."
                        } else {
                            "Search items, cars, bikes, phones, fruits, vegetables..."
                        }
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = FbBlue
                    )
                },
                trailingIcon = {
                    val query = if (activeSection == BusinessHubSection.SERVICES) businessSearchQuery else marketplaceSearchQuery
                    if (query.isNotBlank()) {
                        IconButton(
                            onClick = {
                                if (activeSection == BusinessHubSection.SERVICES) onBusinessSearchQueryChange("") else onMarketplaceSearchQueryChange("")
                            }
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hub_search_bar"),
                shape = RoundedCornerShape(24.dp),
                singleLine = true
            )
        }

        // Category Filter Chips
        item {
            if (activeSection == BusinessHubSection.SERVICES) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(BusinessCategory.values()) { category ->
                        val isSelected = selectedBusinessCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { onBusinessCategorySelected(category) },
                            label = {
                                Text(
                                    text = category.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FbBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(MarketplaceCategory.values()) { category ->
                        val isSelected = selectedMarketplaceCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { onMarketplaceCategorySelected(category) },
                            label = {
                                Text(
                                    text = category.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FbBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // List Header Count & Quick Sell Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (activeSection == BusinessHubSection.SERVICES) {
                    Text(
                        text = "Businesses & Skilled Services (${filteredBusinesses.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "Marketplace Listings (${filteredMarketplaceItems.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = onOpenSellItem,
                        colors = ButtonDefaults.textButtonColors(contentColor = FbBlue),
                        modifier = Modifier.testTag("header_sell_btn")
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Post Listing", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // List Content: Services vs Marketplace
        if (activeSection == BusinessHubSection.SERVICES) {
            if (filteredBusinesses.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No businesses match your search in this category.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredBusinesses, key = { it.id }) { business ->
                    BusinessCard(
                        business = business,
                        onContactClick = { onContactBusiness(business) },
                        onReportClick = onReportBusiness?.let { { it(business) } }
                    )
                }
            }
        } else {
            // Marketplace Items
            if (filteredMarketplaceItems.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ShoppingBag,
                                contentDescription = null,
                                tint = FbBlue,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No listings found in this category",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Be the first to list a car, phone, fruit, or item in Yarkhoon Buy & Sell!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onOpenSellItem,
                                colors = ButtonDefaults.buttonColors(containerColor = FbBlue),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sell an Item Now")
                            }
                        }
                    }
                }
            } else {
                items(filteredMarketplaceItems, key = { it.id }) { item ->
                    MarketplaceItemCard(
                        item = item,
                        currentUser = currentUser,
                        onContactClick = { onContactSeller(item) },
                        onEditClick = { onEditMarketplaceItem(item) },
                        onDeleteClick = { onDeleteMarketplaceItem(item.id) },
                        onMarkSoldClick = { onMarkMarketplaceItemSold(item.id) },
                        onReportClick = onReportMarketplaceItem?.let { { it(item) } }
                    )
                }
            }
        }
    }
}
