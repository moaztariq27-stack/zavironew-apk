package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ZaviroMenuData
import com.example.model.Product
import com.example.ui.components.FoodItemCard
import com.example.ui.theme.*
import com.example.viewmodel.ZaviroViewModel

@Composable
fun MenuScreen(
    viewModel: ZaviroViewModel,
    onProductClick: (Product) -> Unit
) {
    val allProducts by viewModel.products.collectAsState()
    val filteredProducts by viewModel.filteredProducts.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val dynamicCategories = remember(allProducts) {
        val base = ZaviroMenuData.categories
        val custom = allProducts.filter { it.isAvailable }
            .map { it.category.trim() }
            .filter { it.isNotBlank() && base.none { b -> b.equals(it, ignoreCase = true) } }
            .distinct()
        base + custom
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ZaviroBlack)
            .testTag("menu_screen")
    ) {
        // Top Filter & Search Header Surface
        Surface(
            color = ZaviroSurfaceCard,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = {
                        Text(
                            text = "Search in $selectedCategory menu...",
                            color = ZaviroTextMuted,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = ZaviroRed
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = ZaviroTextMuted
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ZaviroBlack,
                        unfocusedContainerColor = ZaviroBlack,
                        focusedBorderColor = ZaviroRed,
                        unfocusedBorderColor = ZaviroSurfaceBorder,
                        focusedTextColor = ZaviroTextWhite,
                        unfocusedTextColor = ZaviroTextWhite
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("menu_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable category tabs
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(dynamicCategories) { cat ->
                        val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectedCategory.value = cat },
                            label = {
                                Text(
                                    text = cat,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = ZaviroBlack,
                                labelColor = ZaviroTextSecondary,
                                selectedContainerColor = ZaviroRed,
                                selectedLabelColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (isSelected) ZaviroRed else ZaviroSurfaceBorder,
                                selectedBorderColor = ZaviroRed,
                                enabled = true,
                                selected = isSelected
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("category_tab_$cat")
                        )
                    }
                }
            }
        }

        // Section Title and Item Count Badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selectedCategory == "All") "All Menu Items" else "$selectedCategory Menu",
                color = ZaviroTextWhite,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ZaviroBurgundySoft
            ) {
                Text(
                    text = "${filteredProducts.size} items",
                    color = ZaviroRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        if (filteredProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.RestaurantMenu,
                        contentDescription = "Empty",
                        tint = ZaviroTextMuted,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No items found",
                        color = ZaviroTextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Try adjusting your search query or select another category.",
                        color = ZaviroTextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("menu_products_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(filteredProducts, key = { it.id }) { product ->
                    FoodItemCard(
                        product = product,
                        onProductClick = onProductClick,
                        onQuickAdd = {
                            viewModel.addProductToCart(product, 1, emptyList(), "")
                        }
                    )
                }
            }
        }
    }
}

