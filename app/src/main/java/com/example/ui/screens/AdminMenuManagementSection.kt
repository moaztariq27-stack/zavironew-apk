package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Deal
import com.example.model.ExtraOption
import com.example.model.Product
import com.example.ui.components.ZaviroFoodImage
import com.example.ui.theme.*
import com.example.viewmodel.ZaviroViewModel

private data class PendingDeleteTarget(
    val id: String,
    val name: String,
    val type: String // "product", "deal", "extra"
)

private data class PendingImageEditTarget(
    val id: String,
    val name: String,
    val currentImageUrl: String,
    val fallbackDrawableRes: Int,
    val isDeal: Boolean
)

@Composable
fun AdminManageMenuTab(viewModel: ZaviroViewModel) {
    val products by viewModel.products.collectAsState()
    val deals by viewModel.deals.collectAsState()
    val extras by viewModel.extras.collectAsState()

    var adminSearchQuery by remember { mutableStateOf("") }
    var selectedSection by remember { mutableStateOf("All") }

    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var isNewProduct by remember { mutableStateOf(false) }

    var editingDeal by remember { mutableStateOf<Deal?>(null) }
    var isNewDeal by remember { mutableStateOf(false) }

    var editingExtra by remember { mutableStateOf<ExtraOption?>(null) }
    var isNewExtra by remember { mutableStateOf(false) }

    var imageEditTarget by remember { mutableStateOf<PendingImageEditTarget?>(null) }
    var deleteTarget by remember { mutableStateOf<PendingDeleteTarget?>(null) }

    val sectionChips = remember(products) {
        val base = listOf(
            "All",
            "Pasta",
            "Wraps & Rolls",
            "Crispy Chicken",
            "Wings",
            "Fries",
            "Drinks",
            "Extras",
            "Deals",
            "Add-Ons"
        )
        val custom = products.map { it.category.trim() }
            .filter { it.isNotBlank() && base.none { b -> b.equals(it, ignoreCase = true) } }
            .distinct()
        base + custom
    }

    val filteredAdminProducts = remember(products, adminSearchQuery, selectedSection) {
        if (selectedSection == "Deals" || selectedSection == "Add-Ons") {
            emptyList()
        } else {
            products.filter { prod ->
                val matchesCat = selectedSection == "All" || prod.category.equals(selectedSection, ignoreCase = true)
                val matchesQuery = adminSearchQuery.isBlank() ||
                        prod.name.contains(adminSearchQuery, ignoreCase = true) ||
                        prod.category.contains(adminSearchQuery, ignoreCase = true) ||
                        prod.description.contains(adminSearchQuery, ignoreCase = true) ||
                        prod.ingredients.contains(adminSearchQuery, ignoreCase = true)
                matchesCat && matchesQuery
            }
        }
    }

    val filteredAdminDeals = remember(deals, adminSearchQuery, selectedSection) {
        if (selectedSection != "All" && selectedSection != "Deals") {
            emptyList()
        } else {
            deals.filter { deal ->
                adminSearchQuery.isBlank() ||
                        deal.name.contains(adminSearchQuery, ignoreCase = true) ||
                        deal.category.contains(adminSearchQuery, ignoreCase = true) ||
                        deal.items.any { it.contains(adminSearchQuery, ignoreCase = true) }
            }
        }
    }

    val filteredAdminExtras = remember(extras, adminSearchQuery, selectedSection) {
        if (selectedSection != "All" && selectedSection != "Add-Ons" && selectedSection != "Extras") {
            emptyList()
        } else {
            extras.filter { extra ->
                adminSearchQuery.isBlank() || extra.name.contains(adminSearchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ZaviroBackground)
            .testTag("admin_manage_menu_tab")
    ) {
        // Search & Action Header
        Surface(
            color = ZaviroSurfaceCard,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                OutlinedTextField(
                    value = adminSearchQuery,
                    onValueChange = { adminSearchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search menu items, deals, drinks, extras...",
                            color = ZaviroTextMuted,
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = ZaviroRed,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (adminSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { adminSearchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = ZaviroTextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ZaviroBackground,
                        unfocusedContainerColor = ZaviroBackground,
                        focusedBorderColor = ZaviroRed,
                        unfocusedBorderColor = ZaviroSurfaceBorder,
                        focusedTextColor = ZaviroTextPrimary,
                        unfocusedTextColor = ZaviroTextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_menu_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(sectionChips) { chip ->
                        val isSelected = selectedSection == chip
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedSection = chip },
                            label = {
                                Text(
                                    text = chip,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ZaviroRed,
                                selectedLabelColor = Color.White,
                                containerColor = ZaviroBackground,
                                labelColor = ZaviroTextPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = ZaviroSurfaceBorder,
                                selectedBorderColor = ZaviroRed
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("admin_filter_chip_$chip")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Add Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val defaultCat = if (selectedSection in listOf("Pasta", "Wraps & Rolls", "Crispy Chicken", "Wings", "Fries", "Drinks", "Extras")) {
                                selectedSection
                            } else "Pasta"
                            editingProduct = Product(
                                id = "",
                                name = "",
                                category = defaultCat,
                                description = "",
                                ingredients = "",
                                price = 399,
                                isAvailable = true,
                                isPopular = false
                            )
                            isNewProduct = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ZaviroRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("admin_add_menu_item_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Menu Item", fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }

                    OutlinedButton(
                        onClick = {
                            val nextNum = (deals.maxOfOrNull { it.dealNumber } ?: 15) + 1
                            editingDeal = Deal(
                                id = "",
                                dealNumber = nextNum,
                                name = "",
                                category = "Special Value",
                                items = listOf("Chicken Wrap & Roll", "Regular Fries", "345ml Drink"),
                                description = "",
                                price = 599,
                                originalPrice = 699,
                                saveAmount = 100,
                                isAvailable = true
                            )
                            isNewDeal = true
                        },
                        border = BorderStroke(1.dp, ZaviroRed),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ZaviroRed),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("admin_add_deal_btn")
                    ) {
                        Icon(imageVector = Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Deal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            editingExtra = ExtraOption(
                                id = "",
                                name = "",
                                price = 80,
                                description = "",
                                isAvailable = true
                            )
                            isNewExtra = true
                        },
                        border = BorderStroke(1.dp, ZaviroSurfaceBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ZaviroTextPrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("admin_add_extra_btn")
                    ) {
                        Text("+ Add-On", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Items List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("admin_menu_items_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 60.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filteredAdminProducts.isNotEmpty()) {
                item {
                    Text(
                        text = "Food, Drinks & Extras (${filteredAdminProducts.size})",
                        color = ZaviroTextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }

                items(filteredAdminProducts, key = { "prod_${it.id}" }) { prod ->
                    AdminProductItemCard(
                        product = prod,
                        onToggleAvailability = { viewModel.toggleProductAvailability(prod) },
                        onEdit = {
                            editingProduct = prod
                            isNewProduct = false
                        },
                        onChangeImage = {
                            imageEditTarget = PendingImageEditTarget(
                                id = prod.id,
                                name = prod.name,
                                currentImageUrl = prod.imageUrl,
                                fallbackDrawableRes = prod.drawableRes,
                                isDeal = false
                            )
                        },
                        onDelete = {
                            deleteTarget = PendingDeleteTarget(
                                id = prod.id,
                                name = prod.name,
                                type = "product"
                            )
                        }
                    )
                }
            }

            if (filteredAdminDeals.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Value Deals (${filteredAdminDeals.size})",
                        color = ZaviroTextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }

                items(filteredAdminDeals, key = { "deal_${it.id}" }) { deal ->
                    AdminDealItemCard(
                        deal = deal,
                        onToggleAvailability = { viewModel.toggleDealAvailability(deal) },
                        onEdit = {
                            editingDeal = deal
                            isNewDeal = false
                        },
                        onChangeImage = {
                            imageEditTarget = PendingImageEditTarget(
                                id = deal.id,
                                name = "Deal ${deal.dealNumber}: ${deal.name}",
                                currentImageUrl = deal.imageUrl,
                                fallbackDrawableRes = deal.drawableRes,
                                isDeal = true
                            )
                        },
                        onDelete = {
                            deleteTarget = PendingDeleteTarget(
                                id = deal.id,
                                name = "Deal ${deal.dealNumber}: ${deal.name}",
                                type = "deal"
                            )
                        }
                    )
                }
            }

            if (filteredAdminExtras.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Customizable Add-On Options (${filteredAdminExtras.size})",
                        color = ZaviroTextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }

                items(filteredAdminExtras, key = { "extra_${it.id}" }) { extra ->
                    AdminExtraItemCard(
                        extra = extra,
                        onToggleAvailability = { viewModel.toggleExtraAvailability(extra) },
                        onEdit = {
                            editingExtra = extra
                            isNewExtra = false
                        },
                        onDelete = {
                            deleteTarget = PendingDeleteTarget(
                                id = extra.id,
                                name = extra.name,
                                type = "extra"
                            )
                        }
                    )
                }
            }
        }
    }

    // Edit Product Dialog
    editingProduct?.let { prod ->
        AdminEditProductDialog(
            product = prod,
            isNew = isNewProduct,
            viewModel = viewModel,
            onDismiss = { editingProduct = null },
            onSave = { updatedProduct ->
                viewModel.saveProduct(updatedProduct)
                editingProduct = null
            }
        )
    }

    // Edit Deal Dialog
    editingDeal?.let { deal ->
        AdminEditDealDialog(
            deal = deal,
            isNew = isNewDeal,
            viewModel = viewModel,
            onDismiss = { editingDeal = null },
            onSave = { updatedDeal ->
                viewModel.saveDeal(updatedDeal)
                editingDeal = null
            }
        )
    }

    // Edit Extra Dialog
    editingExtra?.let { extra ->
        AdminEditExtraDialog(
            extra = extra,
            isNew = isNewExtra,
            onDismiss = { editingExtra = null },
            onSave = { updatedExtra ->
                viewModel.saveExtra(updatedExtra)
                editingExtra = null
            }
        )
    }

    // Dedicated Change Image Dialog
    imageEditTarget?.let { target ->
        AdminChangeItemImageDialog(
            target = target,
            viewModel = viewModel,
            onDismiss = { imageEditTarget = null },
            onSaveImage = { newImageUrl ->
                if (target.isDeal) {
                    viewModel.updateDealImage(target.id, newImageUrl)
                } else {
                    viewModel.updateProductImage(target.id, newImageUrl)
                }
                imageEditTarget = null
            }
        )
    }

    // Confirm Permanent Deletion Dialog
    deleteTarget?.let { target ->
        AdminConfirmDeleteDialog(
            itemName = target.name,
            onConfirmDelete = {
                when (target.type) {
                    "product" -> viewModel.deleteProduct(target.id)
                    "deal" -> viewModel.deleteDeal(target.id)
                    "extra" -> viewModel.deleteExtra(target.id)
                }
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
fun AdminDealsTab(viewModel: ZaviroViewModel) {
    val deals by viewModel.deals.collectAsState()
    var editingDeal by remember { mutableStateOf<Deal?>(null) }
    var isNewDeal by remember { mutableStateOf(false) }
    var imageEditTarget by remember { mutableStateOf<PendingImageEditTarget?>(null) }
    var deleteTarget by remember { mutableStateOf<PendingDeleteTarget?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ZaviroBackground)
            .testTag("admin_deals_tab")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Deals Management (${deals.size})",
                color = ZaviroTextPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp
            )

            Button(
                onClick = {
                    val nextNum = (deals.maxOfOrNull { it.dealNumber } ?: 15) + 1
                    editingDeal = Deal(
                        id = "",
                        dealNumber = nextNum,
                        name = "",
                        category = "Special Value",
                        items = listOf("Chicken Wrap & Roll", "Regular Fries", "345ml Drink"),
                        description = "",
                        price = 599,
                        originalPrice = 699,
                        saveAmount = 100,
                        isAvailable = true
                    )
                    isNewDeal = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ZaviroRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("admin_deals_tab_add_btn")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Deal", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Deal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 50.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(deals, key = { it.id }) { deal ->
                AdminDealItemCard(
                    deal = deal,
                    onToggleAvailability = { viewModel.toggleDealAvailability(deal) },
                    onEdit = {
                        editingDeal = deal
                        isNewDeal = false
                    },
                    onChangeImage = {
                        imageEditTarget = PendingImageEditTarget(
                            id = deal.id,
                            name = "Deal ${deal.dealNumber}: ${deal.name}",
                            currentImageUrl = deal.imageUrl,
                            fallbackDrawableRes = deal.drawableRes,
                            isDeal = true
                        )
                    },
                    onDelete = {
                        deleteTarget = PendingDeleteTarget(
                            id = deal.id,
                            name = "Deal ${deal.dealNumber}: ${deal.name}",
                            type = "deal"
                        )
                    }
                )
            }
        }
    }

    editingDeal?.let { deal ->
        AdminEditDealDialog(
            deal = deal,
            isNew = isNewDeal,
            viewModel = viewModel,
            onDismiss = { editingDeal = null },
            onSave = { updatedDeal ->
                viewModel.saveDeal(updatedDeal)
                editingDeal = null
            }
        )
    }

    imageEditTarget?.let { target ->
        AdminChangeItemImageDialog(
            target = target,
            viewModel = viewModel,
            onDismiss = { imageEditTarget = null },
            onSaveImage = { newImageUrl ->
                viewModel.updateDealImage(target.id, newImageUrl)
                imageEditTarget = null
            }
        )
    }

    deleteTarget?.let { target ->
        AdminConfirmDeleteDialog(
            itemName = target.name,
            onConfirmDelete = {
                viewModel.deleteDeal(target.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun AdminProductItemCard(
    product: Product,
    onToggleAvailability: () -> Unit,
    onEdit: () -> Unit,
    onChangeImage: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .testTag("admin_product_card_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = ZaviroSurfaceCard),
        border = BorderStroke(1.dp, if (product.isAvailable) ZaviroSurfaceBorder else ZaviroRed.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Item Image Thumbnail with click to change image
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, ZaviroSurfaceBorder, RoundedCornerShape(12.dp))
                        .clickable { onChangeImage() }
                ) {
                    val fallbackRes = if (product.drawableRes != 0) product.drawableRes else R.drawable.zaviro_creamy_pasta
                    ZaviroFoodImage(
                        imageUrl = product.imageUrl,
                        fallbackDrawableRes = fallbackRes,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Surface(
                        color = ZaviroDarkBase.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(topStart = 8.dp),
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Change Image",
                            tint = Color.White,
                            modifier = Modifier
                                .padding(4.dp)
                                .size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = product.name,
                            color = ZaviroTextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ZaviroRedTint
                        ) {
                            Text(
                                text = product.category,
                                color = ZaviroRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Rs ${product.price}",
                            color = ZaviroRed,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (product.isAvailable) ZaviroGreen.copy(alpha = 0.14f) else ZaviroRed.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = if (product.isAvailable) "ACTIVE" else "INACTIVE",
                                color = if (product.isAvailable) ZaviroGreen else ZaviroRed,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (product.imageUrl.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = ZaviroBackground,
                                border = BorderStroke(1.dp, ZaviroSurfaceBorder)
                            ) {
                                Text(
                                    text = "CUSTOM IMG",
                                    color = ZaviroBurgundy,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (product.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = product.description,
                            color = ZaviroTextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (product.ingredients.isNotBlank()) {
                        Text(
                            text = "Ingredients: ${product.ingredients}",
                            color = ZaviroTextMuted,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Active / Inactive Switch
                Switch(
                    checked = product.isAvailable,
                    onCheckedChange = { onToggleAvailability() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ZaviroRed,
                        uncheckedThumbColor = ZaviroTextMuted,
                        uncheckedTrackColor = ZaviroBackground
                    ),
                    modifier = Modifier.testTag("toggle_product_${product.id}")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = ZaviroSurfaceBorder, thickness = 0.7.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Mobile-Friendly Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    border = BorderStroke(1.dp, ZaviroRed),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ZaviroRed),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("edit_product_${product.id}")
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Item", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Item", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onChangeImage,
                    border = BorderStroke(1.dp, ZaviroSurfaceBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ZaviroTextPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("change_image_product_${product.id}")
                ) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = "Change Image", tint = ZaviroBurgundy, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Change Image", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("delete_product_${product.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Item",
                        tint = ZaviroRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminDealItemCard(
    deal: Deal,
    onToggleAvailability: () -> Unit,
    onEdit: () -> Unit,
    onChangeImage: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .testTag("admin_deal_card_${deal.id}"),
        colors = CardDefaults.cardColors(containerColor = ZaviroSurfaceCard),
        border = BorderStroke(1.dp, if (deal.isAvailable) ZaviroSurfaceBorder else ZaviroRed.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, ZaviroSurfaceBorder, RoundedCornerShape(12.dp))
                        .clickable { onChangeImage() }
                ) {
                    val fallbackRes = if (deal.drawableRes != 0) deal.drawableRes else R.drawable.zaviro_hero_banner
                    ZaviroFoodImage(
                        imageUrl = deal.imageUrl,
                        fallbackDrawableRes = fallbackRes,
                        contentDescription = deal.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Surface(
                        color = ZaviroDarkBase.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(topStart = 8.dp),
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Change Image",
                            tint = Color.White,
                            modifier = Modifier
                                .padding(4.dp)
                                .size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Deal ${deal.dealNumber}: ${deal.name}",
                        color = ZaviroTextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Rs ${deal.price} (Orig: Rs ${deal.originalPrice})",
                            color = ZaviroRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (deal.isAvailable) ZaviroGreen.copy(alpha = 0.14f) else ZaviroRed.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = if (deal.isAvailable) "ACTIVE" else "INACTIVE",
                                color = if (deal.isAvailable) ZaviroGreen else ZaviroRed,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = deal.items.joinToString(", "),
                        color = ZaviroTextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Switch(
                    checked = deal.isAvailable,
                    onCheckedChange = { onToggleAvailability() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ZaviroRed,
                        uncheckedThumbColor = ZaviroTextMuted,
                        uncheckedTrackColor = ZaviroBackground
                    ),
                    modifier = Modifier.testTag("toggle_deal_${deal.id}")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = ZaviroSurfaceBorder, thickness = 0.7.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    border = BorderStroke(1.dp, ZaviroRed),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ZaviroRed),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("edit_deal_${deal.id}")
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Deal", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Deal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onChangeImage,
                    border = BorderStroke(1.dp, ZaviroSurfaceBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ZaviroTextPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("change_image_deal_${deal.id}")
                ) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = "Change Image", tint = ZaviroBurgundy, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Change Image", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("delete_deal_${deal.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Deal",
                        tint = ZaviroRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminExtraItemCard(
    extra: ExtraOption,
    onToggleAvailability: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(14.dp))
            .testTag("admin_extra_card_${extra.id}"),
        colors = CardDefaults.cardColors(containerColor = ZaviroSurfaceCard),
        border = BorderStroke(1.dp, ZaviroSurfaceBorder),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = extra.name,
                    color = ZaviroTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Add-On Price: +Rs ${extra.price}",
                    color = ZaviroRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                if (extra.description.isNotBlank()) {
                    Text(
                        text = extra.description,
                        color = ZaviroTextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Switch(
                checked = extra.isAvailable,
                onCheckedChange = { onToggleAvailability() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = ZaviroRed,
                    uncheckedThumbColor = ZaviroTextMuted,
                    uncheckedTrackColor = ZaviroBackground
                ),
                modifier = Modifier.testTag("toggle_extra_${extra.id}")
            )

            IconButton(
                onClick = onEdit,
                modifier = Modifier.testTag("edit_extra_${extra.id}")
            ) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Extra", tint = ZaviroRed)
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_extra_${extra.id}")
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Extra", tint = ZaviroRed)
            }
        }
    }
}

@Composable
private fun AdminEditProductDialog(
    product: Product,
    isNew: Boolean,
    viewModel: ZaviroViewModel,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf(product.name) }
    var category by remember { mutableStateOf(product.category.ifBlank { "Pasta" }) }
    var priceStr by remember { mutableStateOf("${product.price}") }
    var description by remember { mutableStateOf(product.description) }
    var ingredients by remember { mutableStateOf(product.ingredients) }
    var isAvailable by remember { mutableStateOf(product.isAvailable) }
    var isPopular by remember { mutableStateOf(product.isPopular) }
    var draftImageUrl by remember { mutableStateOf(product.imageUrl) }
    var manualUrlInput by remember { mutableStateOf("") }

    val availableCategories = listOf(
        "Pasta",
        "Wraps & Rolls",
        "Crispy Chicken",
        "Wings",
        "Fries",
        "Drinks",
        "Extras"
    )

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val base64 = viewModel.processImageUriToBase64(context, uri)
            if (base64 != null) {
                draftImageUrl = base64
                Toast.makeText(context, "New image loaded into preview! Tap Save to apply.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Could not decode selected image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ZaviroSurfaceCard,
        shape = RoundedCornerShape(18.dp),
        title = {
            Text(
                text = if (isNew) "Add New Menu Item" else "Edit ${product.name}",
                color = ZaviroTextPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .testTag("edit_product_dialog"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Image Preview & Upload Section
                Text(
                    text = "Item Image Preview",
                    color = ZaviroRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ZaviroBackground)
                        .border(1.dp, ZaviroSurfaceBorder, RoundedCornerShape(12.dp))
                        .testTag("edit_item_image_preview"),
                    contentAlignment = Alignment.Center
                ) {
                    val fallbackRes = if (product.drawableRes != 0) product.drawableRes else R.drawable.zaviro_creamy_pasta
                    ZaviroFoodImage(
                        imageUrl = draftImageUrl,
                        fallbackDrawableRes = fallbackRes,
                        contentDescription = name.ifBlank { "Item Preview" },
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    if (draftImageUrl != product.imageUrl) {
                        Surface(
                            color = ZaviroGreen,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .padding(8.dp)
                                .align(Alignment.TopEnd)
                        ) {
                            Text(
                                text = "NEW IMAGE PREVIEW",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ZaviroRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_item_upload_image_btn")
                    ) {
                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload from Phone", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (draftImageUrl.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                draftImageUrl = ""
                                manualUrlInput = ""
                            },
                            border = BorderStroke(1.dp, ZaviroSurfaceBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ZaviroTextSecondary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("edit_item_reset_image_btn")
                        ) {
                            Text("Reset", fontSize = 11.sp)
                        }
                    }
                }

                // Optional Image URL / Data URI input for instant preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = manualUrlInput,
                        onValueChange = { manualUrlInput = it },
                        placeholder = { Text("Or paste image URL / data URI", color = ZaviroTextMuted, fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ZaviroBackground,
                            unfocusedContainerColor = ZaviroBackground,
                            focusedBorderColor = ZaviroRed,
                            unfocusedBorderColor = ZaviroSurfaceBorder,
                            focusedTextColor = ZaviroTextPrimary,
                            unfocusedTextColor = ZaviroTextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_item_image_url_input")
                    )

                    TextButton(
                        onClick = {
                            if (manualUrlInput.isNotBlank()) {
                                draftImageUrl = manualUrlInput.trim()
                            }
                        },
                        modifier = Modifier.testTag("edit_item_apply_image_btn")
                    ) {
                        Text("Preview", color = ZaviroRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                HorizontalDivider(color = ZaviroSurfaceBorder)

                // Item Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name", color = ZaviroTextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ZaviroTextPrimary,
                        unfocusedTextColor = ZaviroTextPrimary,
                        focusedBorderColor = ZaviroRed,
                        unfocusedBorderColor = ZaviroSurfaceBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_item_name_input")
                )

                // Category Chips + Input
                Text(
                    text = "Select or Enter Category",
                    color = ZaviroTextMuted,
                    fontSize = 11.sp
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(availableCategories) { cat ->
                        val isSelected = category.equals(cat, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ZaviroRed,
                                selectedLabelColor = Color.White,
                                containerColor = ZaviroBackground,
                                labelColor = ZaviroTextPrimary
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category", color = ZaviroTextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ZaviroTextPrimary,
                        unfocusedTextColor = ZaviroTextPrimary,
                        focusedBorderColor = ZaviroRed,
                        unfocusedBorderColor = ZaviroSurfaceBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_item_category_input")
                )

                // Price
                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text("Price in PKR (Rs)", color = ZaviroTextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ZaviroTextPrimary,
                        unfocusedTextColor = ZaviroTextPrimary,
                        focusedBorderColor = ZaviroRed,
                        unfocusedBorderColor = ZaviroSurfaceBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_item_price_input")
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description", color = ZaviroTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ZaviroTextPrimary,
                        unfocusedTextColor = ZaviroTextPrimary,
                        focusedBorderColor = ZaviroRed,
                        unfocusedBorderColor = ZaviroSurfaceBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_item_description_input")
                )

                // Ingredients / Details
                OutlinedTextField(
                    value = ingredients,
                    onValueChange = { ingredients = it },
                    label = { Text("Ingredients & Details", color = ZaviroTextMuted) },
                    placeholder = { Text("E.g., Fresh chicken, herbs, garlic sauce...", color = ZaviroTextMuted, fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ZaviroTextPrimary,
                        unfocusedTextColor = ZaviroTextPrimary,
                        focusedBorderColor = ZaviroRed,
                        unfocusedBorderColor = ZaviroSurfaceBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_item_ingredients_input")
                )

                // Availability toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Item Available (Active)", color = ZaviroTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Disable to hide temporarily from customer menu", color = ZaviroTextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = isAvailable,
                        onCheckedChange = { isAvailable = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ZaviroRed
                        ),
                        modifier = Modifier.testTag("edit_item_available_switch")
                    )
                }

                // Popular toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Featured / Popular Badge", color = ZaviroTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Highlight on Home Screen & Menu", color = ZaviroTextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = isPopular,
                        onCheckedChange = { isPopular = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ZaviroRed
                        ),
                        modifier = Modifier.testTag("edit_item_popular_switch")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedName = name.trim().ifBlank { "New Zaviro Item" }
                    val newPrice = priceStr.toIntOrNull() ?: product.price
                    val finalImageUrl = if (manualUrlInput.isNotBlank() && draftImageUrl == product.imageUrl) {
                        manualUrlInput.trim()
                    } else {
                        draftImageUrl
                    }
                    onSave(
                        product.copy(
                            name = trimmedName,
                            category = category.trim().ifBlank { "Pasta" },
                            price = newPrice,
                            description = description.trim(),
                            ingredients = ingredients.trim(),
                            isAvailable = isAvailable,
                            isPopular = isPopular,
                            imageUrl = finalImageUrl
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ZaviroRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("edit_item_save_btn")
            ) {
                Text("Save", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("edit_item_cancel_btn")
            ) {
                Text("Cancel", color = ZaviroTextSecondary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun AdminChangeItemImageDialog(
    target: PendingImageEditTarget,
    viewModel: ZaviroViewModel,
    onDismiss: () -> Unit,
    onSaveImage: (String) -> Unit
) {
    val context = LocalContext.current
    var draftImageUrl by remember { mutableStateOf(target.currentImageUrl) }
    var manualUrlInput by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val base64 = viewModel.processImageUriToBase64(context, uri)
            if (base64 != null) {
                draftImageUrl = base64
                Toast.makeText(context, "Image loaded! Check preview below and tap Save.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to process selected image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ZaviroSurfaceCard,
        shape = RoundedCornerShape(18.dp),
        title = {
            Column {
                Text(
                    text = "Edit Item Image",
                    color = ZaviroTextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
                Text(
                    text = target.name,
                    color = ZaviroRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .testTag("change_item_image_dialog"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Image Preview (Before Saving):",
                    color = ZaviroTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ZaviroBackground)
                        .border(1.5.dp, ZaviroRed, RoundedCornerShape(14.dp))
                        .testTag("change_image_preview_box"),
                    contentAlignment = Alignment.Center
                ) {
                    val fallbackRes = if (target.fallbackDrawableRes != 0) {
                        target.fallbackDrawableRes
                    } else {
                        R.drawable.zaviro_creamy_pasta
                    }
                    ZaviroFoodImage(
                        imageUrl = draftImageUrl,
                        fallbackDrawableRes = fallbackRes,
                        contentDescription = target.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    if (draftImageUrl != target.currentImageUrl) {
                        Surface(
                            color = ZaviroGreen,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .padding(10.dp)
                                .align(Alignment.TopEnd)
                        ) {
                            Text(
                                text = "PREVIEW READY",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZaviroRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("change_image_upload_phone_btn")
                ) {
                    Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload New Image from Phone", fontWeight = FontWeight.Black, fontSize = 13.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = manualUrlInput,
                        onValueChange = { manualUrlInput = it },
                        placeholder = { Text("Or enter image URL / data URI", color = ZaviroTextMuted, fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ZaviroBackground,
                            unfocusedContainerColor = ZaviroBackground,
                            focusedBorderColor = ZaviroRed,
                            unfocusedBorderColor = ZaviroSurfaceBorder,
                            focusedTextColor = ZaviroTextPrimary,
                            unfocusedTextColor = ZaviroTextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("change_image_url_input")
                    )

                    Button(
                        onClick = {
                            if (manualUrlInput.isNotBlank()) {
                                draftImageUrl = manualUrlInput.trim()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ZaviroBurgundy,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("change_image_apply_url_btn")
                    ) {
                        Text("Preview", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                if (draftImageUrl.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            draftImageUrl = ""
                            manualUrlInput = ""
                        },
                        border = BorderStroke(1.dp, ZaviroSurfaceBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ZaviroTextSecondary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("change_image_reset_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore Default Item Image", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalUrl = if (manualUrlInput.isNotBlank() && draftImageUrl == target.currentImageUrl) {
                        manualUrlInput.trim()
                    } else {
                        draftImageUrl
                    }
                    onSaveImage(finalUrl)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ZaviroRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("change_image_save_btn")
            ) {
                Text("Save", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("change_image_cancel_btn")
            ) {
                Text("Cancel", color = ZaviroTextSecondary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun AdminEditDealDialog(
    deal: Deal,
    isNew: Boolean,
    viewModel: ZaviroViewModel,
    onDismiss: () -> Unit,
    onSave: (Deal) -> Unit
) {
    val context = LocalContext.current

    var dealNumberStr by remember { mutableStateOf("${deal.dealNumber}") }
    var name by remember { mutableStateOf(deal.name) }
    var category by remember { mutableStateOf(deal.category.ifBlank { "Wraps & Rolls" }) }
    var priceStr by remember { mutableStateOf("${deal.price}") }
    var origPriceStr by remember { mutableStateOf("${deal.originalPrice}") }
    var itemsText by remember { mutableStateOf(deal.items.joinToString(", ")) }
    var description by remember { mutableStateOf(deal.description) }
    var isAvailable by remember { mutableStateOf(deal.isAvailable) }
    var draftImageUrl by remember { mutableStateOf(deal.imageUrl) }
    var manualUrlInput by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val base64 = viewModel.processImageUriToBase64(context, uri)
            if (base64 != null) {
                draftImageUrl = base64
                Toast.makeText(context, "Deal image loaded into preview!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ZaviroSurfaceCard,
        shape = RoundedCornerShape(18.dp),
        title = {
            Text(
                text = if (isNew) "Add New Deal" else "Edit Deal ${deal.dealNumber}",
                color = ZaviroTextPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .testTag("edit_deal_dialog"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Deal Image Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ZaviroBackground)
                        .border(1.dp, ZaviroSurfaceBorder, RoundedCornerShape(12.dp))
                        .testTag("edit_deal_image_preview")
                ) {
                    val fallbackRes = if (deal.drawableRes != 0) deal.drawableRes else R.drawable.zaviro_hero_banner
                    ZaviroFoodImage(
                        imageUrl = draftImageUrl,
                        fallbackDrawableRes = fallbackRes,
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZaviroRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_deal_upload_image_btn")
                ) {
                    Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload Deal Image from Phone", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = manualUrlInput,
                        onValueChange = { manualUrlInput = it },
                        placeholder = { Text("Or paste deal image URL", color = ZaviroTextMuted, fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ZaviroTextPrimary,
                            unfocusedTextColor = ZaviroTextPrimary,
                            focusedBorderColor = ZaviroRed,
                            unfocusedBorderColor = ZaviroSurfaceBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_deal_image_url_input")
                    )
                    TextButton(
                        onClick = {
                            if (manualUrlInput.isNotBlank()) {
                                draftImageUrl = manualUrlInput.trim()
                            }
                        },
                        modifier = Modifier.testTag("edit_deal_apply_image_btn")
                    ) {
                        Text("Preview", color = ZaviroRed, fontWeight = FontWeight.Bold)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dealNumberStr,
                        onValueChange = { dealNumberStr = it },
                        label = { Text("Deal #", color = ZaviroTextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ZaviroTextPrimary,
                            unfocusedTextColor = ZaviroTextPrimary,
                            focusedBorderColor = ZaviroRed,
                            unfocusedBorderColor = ZaviroSurfaceBorder
                        ),
                        modifier = Modifier
                            .weight(0.35f)
                            .testTag("edit_deal_number_input")
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Deal Name", color = ZaviroTextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ZaviroTextPrimary,
                            unfocusedTextColor = ZaviroTextPrimary,
                            focusedBorderColor = ZaviroRed,
                            unfocusedBorderColor = ZaviroSurfaceBorder
                        ),
                        modifier = Modifier
                            .weight(0.65f)
                            .testTag("edit_deal_name_input")
                    )
                }

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Deal Category", color = ZaviroTextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ZaviroTextPrimary,
                        unfocusedTextColor = ZaviroTextPrimary,
                        focusedBorderColor = ZaviroRed,
                        unfocusedBorderColor = ZaviroSurfaceBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_deal_category_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Deal Price (Rs)", color = ZaviroTextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ZaviroTextPrimary,
                            unfocusedTextColor = ZaviroTextPrimary,
                            focusedBorderColor = ZaviroRed,
                            unfocusedBorderColor = ZaviroSurfaceBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_deal_price_input")
                    )

                    OutlinedTextField(
                        value = origPriceStr,
                        onValueChange = { origPriceStr = it },
                        label = { Text("Original Price (Rs)", color = ZaviroTextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ZaviroTextPrimary,
                            unfocusedTextColor = ZaviroTextPrimary,
                            focusedBorderColor = ZaviroRed,
                            unfocusedBorderColor = ZaviroSurfaceBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_deal_original_price_input")
                    )
                }

                OutlinedTextField(
                    value = itemsText,
                    onValueChange = { itemsText = it },
                    label = { Text("Included Deal Items (comma-separated)", color = ZaviroTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ZaviroTextPrimary,
                        unfocusedTextColor = ZaviroTextPrimary,
                        focusedBorderColor = ZaviroRed,
                        unfocusedBorderColor = ZaviroSurfaceBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_deal_items_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Deal Description / Details", color = ZaviroTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ZaviroTextPrimary,
                        unfocusedTextColor = ZaviroTextPrimary,
                        focusedBorderColor = ZaviroRed,
                        unfocusedBorderColor = ZaviroSurfaceBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_deal_description_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Deal Active / Available", color = ZaviroTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Switch(
                        checked = isAvailable,
                        onCheckedChange = { isAvailable = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ZaviroRed
                        ),
                        modifier = Modifier.testTag("edit_deal_available_switch")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dNum = dealNumberStr.toIntOrNull() ?: deal.dealNumber
                    val np = priceStr.toIntOrNull() ?: deal.price
                    val op = origPriceStr.toIntOrNull() ?: deal.originalPrice
                    val save = (op - np).coerceAtLeast(0)
                    val parsedItems = itemsText.split(",", "\n")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    val finalImageUrl = if (manualUrlInput.isNotBlank() && draftImageUrl == deal.imageUrl) {
                        manualUrlInput.trim()
                    } else {
                        draftImageUrl
                    }
                    onSave(
                        deal.copy(
                            dealNumber = dNum,
                            name = name.trim().ifBlank { "ZAVIRO VALUE DEAL" },
                            category = category.trim().ifBlank { "Special Value" },
                            items = if (parsedItems.isNotEmpty()) parsedItems else deal.items,
                            description = description.trim(),
                            price = np,
                            originalPrice = op,
                            saveAmount = save,
                            isAvailable = isAvailable,
                            imageUrl = finalImageUrl
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ZaviroRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("edit_deal_save_btn")
            ) {
                Text("Save", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("edit_deal_cancel_btn")
            ) {
                Text("Cancel", color = ZaviroTextSecondary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun AdminEditExtraDialog(
    extra: ExtraOption,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (ExtraOption) -> Unit
) {
    var name by remember { mutableStateOf(extra.name) }
    var priceStr by remember { mutableStateOf("${extra.price}") }
    var description by remember { mutableStateOf(extra.description) }
    var isAvailable by remember { mutableStateOf(extra.isAvailable) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ZaviroSurfaceCard,
        shape = RoundedCornerShape(18.dp),
        title = {
            Text(
                text = if (isNew) "Add New Extra / Add-On" else "Edit ${extra.name}",
                color = ZaviroTextPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Extra Option Name", color = ZaviroTextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ZaviroTextPrimary,
                        unfocusedTextColor = ZaviroTextPrimary,
                        focusedBorderColor = ZaviroRed,
                        unfocusedBorderColor = ZaviroSurfaceBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_extra_name_input")
                )

                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text("Price in PKR (Rs)", color = ZaviroTextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ZaviroTextPrimary,
                        unfocusedTextColor = ZaviroTextPrimary,
                        focusedBorderColor = ZaviroRed,
                        unfocusedBorderColor = ZaviroSurfaceBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_extra_price_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Details (Optional)", color = ZaviroTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ZaviroTextPrimary,
                        unfocusedTextColor = ZaviroTextPrimary,
                        focusedBorderColor = ZaviroRed,
                        unfocusedBorderColor = ZaviroSurfaceBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_extra_description_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Available", color = ZaviroTextPrimary, fontWeight = FontWeight.Bold)
                    Switch(
                        checked = isAvailable,
                        onCheckedChange = { isAvailable = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ZaviroRed
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        extra.copy(
                            name = name.trim().ifBlank { "Extra Option" },
                            price = priceStr.toIntOrNull() ?: extra.price,
                            description = description.trim(),
                            isAvailable = isAvailable
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ZaviroRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("edit_extra_save_btn")
            ) {
                Text("Save", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("edit_extra_cancel_btn")
            ) {
                Text("Cancel", color = ZaviroTextSecondary)
            }
        }
    )
}

@Composable
private fun AdminConfirmDeleteDialog(
    itemName: String,
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ZaviroSurfaceCard,
        shape = RoundedCornerShape(18.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Confirm Delete",
                    tint = ZaviroRed,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Confirm Permanent Deletion",
                    color = ZaviroTextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.testTag("confirm_delete_dialog"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Are you sure you want to permanently delete \"$itemName\" from the menu?",
                    color = ZaviroTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Tip: If you only want to hide this item temporarily, tap Cancel and use the Active/Inactive switch instead.",
                    color = ZaviroTextSecondary,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ZaviroRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_delete_item_btn")
            ) {
                Text("Delete Permanently", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_delete_item_btn")
            ) {
                Text("Cancel", color = ZaviroTextSecondary, fontWeight = FontWeight.Bold)
            }
        }
    )
}
