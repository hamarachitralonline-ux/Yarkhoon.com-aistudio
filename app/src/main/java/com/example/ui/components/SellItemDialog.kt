package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.MarketplaceCategory
import com.example.data.MarketplaceItem
import com.example.ui.theme.FbBlue

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SellItemDialog(
    itemToEdit: MarketplaceItem? = null,
    onDismiss: () -> Unit,
    onSubmit: (
        itemId: String?,
        title: String,
        category: MarketplaceCategory,
        price: String,
        description: String,
        phone: String,
        location: String,
        imageUrls: List<String>
    ) -> Unit
) {
    val isEditing = itemToEdit != null
    var title by remember { mutableStateOf(itemToEdit?.title ?: "") }
    var selectedCategory by remember { mutableStateOf(itemToEdit?.category ?: MarketplaceCategory.OTHER) }
    var price by remember { mutableStateOf(itemToEdit?.price ?: "") }
    var description by remember { mutableStateOf(itemToEdit?.description ?: "") }
    var phone by remember { mutableStateOf(itemToEdit?.phone ?: "+92 345 ") }
    var location by remember { mutableStateOf(itemToEdit?.location ?: "Yarkhoon Valley") }
    val productImages = remember {
        mutableStateListOf<String>().apply {
            if (itemToEdit != null) {
                addAll(itemToEdit.imageUrls)
            }
        }
    }

    // Direct Device Gallery Photo Picker for multiple product images
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 6)
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            val uriStr = uri.toString()
            if (!productImages.contains(uriStr)) {
                productImages.add(uriStr)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .testTag("sell_item_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = FbBlue.copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isEditing) Icons.Default.Edit else Icons.Default.AddShoppingCart,
                                    contentDescription = null,
                                    tint = FbBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isEditing) "Edit Marketplace Listing" else "Sell an Item",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Yarkhoon Buy & Sell Marketplace",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_sell_item_dialog")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Scrollable Form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Item Title Field
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Item Name / Title *") },
                        placeholder = { Text("e.g. Toyota Prado 1998, Fresh Valley Apples...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sell_item_name_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Category Selector
                    Text(
                        text = "Category *",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MarketplaceCategory.values().filter { it != MarketplaceCategory.ALL }.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = {
                                    Text(
                                        text = cat.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FbBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // Price Field
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Price (e.g. PKR 150,000 or PKR 2,500/kg) *") },
                        placeholder = { Text("Enter price in PKR...") },
                        leadingIcon = {
                            Icon(Icons.Default.Sell, contentDescription = null, tint = FbBlue)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sell_item_price_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Contact Phone
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Contact Phone / WhatsApp *") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = FbBlue)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sell_item_phone_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Location / Town
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location (Town / Valley)") },
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = FbBlue)
                        },
                        placeholder = { Text("e.g. Booni, Mastuj, Yarkhoon Lasht...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Item Description
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Item Description *") },
                        placeholder = { Text("Provide details: condition, year, model, quantity, delivery options...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("sell_item_desc_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Product Photos from Device Gallery
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Product Photos (${productImages.size}/6):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        FilledTonalButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = FbBlue.copy(alpha = 0.12f),
                                contentColor = FbBlue
                            ),
                            modifier = Modifier.testTag("select_item_photos_from_gallery")
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Select from Gallery", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (productImages.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(productImages) { imgUrl ->
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.5.dp, FbBlue, RoundedCornerShape(10.dp))
                                ) {
                                    AsyncImage(
                                        model = imgUrl,
                                        contentDescription = "Selected product image",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = { productImages.remove(imgUrl) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(2.dp)
                                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                            .size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove image",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Helpful empty state notice for photos
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Collections,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Add photos from your gallery to get more buyer inquiries (default demo image will be used if none selected).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Submit Button
                Button(
                    onClick = {
                        val finalImages = if (productImages.isNotEmpty()) {
                            productImages.toList()
                        } else {
                            listOf("https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800")
                        }
                        onSubmit(
                            itemToEdit?.id,
                            title.trim(),
                            selectedCategory,
                            price.trim(),
                            description.trim(),
                            phone.trim(),
                            location.trim(),
                            finalImages
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_sell_item_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FbBlue),
                    enabled = title.isNotBlank() && price.isNotBlank() && phone.isNotBlank() && description.isNotBlank()
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Publish,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEditing) "Save Changes" else "Publish Listing to Buy & Sell",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
