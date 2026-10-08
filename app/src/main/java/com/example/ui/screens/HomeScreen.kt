package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ZaviroMenuData
import com.example.model.Deal
import com.example.model.LogoSlot
import com.example.model.Product
import com.example.ui.components.FoodItemCard
import com.example.ui.components.WhatsAppFloatingButton
import com.example.ui.components.ZaviroDynamicLogo
import com.example.ui.theme.*
import com.example.viewmodel.ZaviroViewModel

@Composable
fun HomeScreen(
    viewModel: ZaviroViewModel,
    onNavigateToMenu: () -> Unit,
    onNavigateToDeals: () -> Unit,
    onProductClick: (Product) -> Unit,
    onDealClick: (Deal) -> Unit
) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsState()
    val deals by viewModel.deals.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val branding by viewModel.branding.collectAsState()

    val popularProducts = remember(products) {
        products.filter { it.isAvailable && it.isPopular }.take(4)
    }

    val searchMatchingProducts = remember(products, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else products.filter {
            it.isAvailable && (
                it.name.contains(searchQuery, ignoreCase = true) ||
                    it.category.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true)
                )
        }.take(6)
    }

    val featuredDeals = remember(deals) {
        deals.filter { it.isAvailable }.take(5)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ZaviroBlack)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_screen_column"),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Deep Dark / Black Hero Section with Zaviro Branding, Hero Food Backdrop & Order Now CTA
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ZaviroDarkBase)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.zaviro_hero_banner),
                        contentDescription = "Zaviro Banner",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(290.dp),
                        contentScale = ContentScale.Crop
                    )

                    // Deep dark base overlay for clean contrast
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(290.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        ZaviroDarkBase.copy(alpha = 0.72f),
                                        ZaviroDarkBase.copy(alpha = 0.88f),
                                        ZaviroDarkBase
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Store status & delivery pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = ZaviroDarkElevated.copy(alpha = 0.92f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ZaviroDarkBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (settings.isStoreOpen) Color(0xFF22C55E) else ZaviroRed)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (settings.isStoreOpen) "OPEN NOW • ${settings.announcement}" else "CURRENTLY CLOSED",
                                    color = ZaviroOnDarkSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Untouched Zaviro Logo placement
                        ZaviroDynamicLogo(
                            branding = branding,
                            slot = LogoSlot.HOME_HEADER,
                            modifier = Modifier
                                .fillMaxWidth(0.68f)
                                .height(128.dp)
                                .testTag("home_zaviro_logo"),
                            contentScale = ContentScale.Fit
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Hot, Crispy & Freshly Prepared — Delivered to Your Doorstep",
                            color = ZaviroOnDarkSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Prominent "Order Now" & "Hot Deals" Hero CTAs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                        ) {
                            Button(
                                onClick = onNavigateToMenu,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ZaviroRed,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 11.dp),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestaurantMenu,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(7.dp))
                                Text(
                                    text = "Order Now",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            OutlinedButton(
                                onClick = onNavigateToDeals,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = ZaviroDarkElevated,
                                    contentColor = ZaviroOnDarkPrimary
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ZaviroDarkBorder),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 11.dp),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalOffer,
                                    contentDescription = null,
                                    tint = ZaviroRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "15 Combo Deals",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // Clean Search Bar
            item {
                Surface(
                    color = ZaviroBlack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        placeholder = {
                            Text(
                                text = "Search pasta, crispy chicken, wings, wraps...",
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
                            focusedContainerColor = ZaviroSurfaceCard,
                            unfocusedContainerColor = ZaviroSurfaceCard,
                            focusedBorderColor = ZaviroRed,
                            unfocusedBorderColor = ZaviroSurfaceBorder,
                            focusedTextColor = ZaviroTextWhite,
                            unfocusedTextColor = ZaviroTextWhite
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_search_input")
                    )
                }
            }

            // Live Search Results (when user types in home search input)
            if (searchQuery.isNotBlank()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Search Results (${searchMatchingProducts.size})",
                                color = ZaviroTextWhite,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Search Full Menu →",
                                color = ZaviroRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { onNavigateToMenu() }
                            )
                        }
                        if (searchMatchingProducts.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = ZaviroSurfaceCard,
                                border = androidx.compose.foundation.BorderStroke(1.dp, ZaviroSurfaceBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No matching items found. Tap 'Search Full Menu' to browse all categories.",
                                    color = ZaviroTextSecondary,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
                            searchMatchingProducts.forEach { product ->
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

            // Promotional Deal Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onNavigateToDeals() },
                    colors = CardDefaults.cardColors(containerColor = ZaviroBurgundyDark),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ZaviroRed,
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Text(
                                    text = "15 VALUE COMBO DEALS",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Text(
                                text = "More Food • Bigger Savings",
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Complete meal deals starting from Rs 499",
                                color = ZaviroOnDarkSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = onNavigateToDeals,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = ZaviroBurgundyDark
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            modifier = Modifier
                                .height(42.dp)
                                .testTag("home_order_deals_btn")
                        ) {
                            Text(
                                text = "View Deals",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Food Categories Section
            item {
                Column(modifier = Modifier.padding(top = 18.dp)) {
                    SectionHeaderRow(
                        title = "Explore Categories",
                        subtitle = "Freshly crafted Pakistani & continental favorites",
                        actionText = "Full Menu",
                        onActionClick = onNavigateToMenu
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val dynamicCategories = remember(products) {
                        val base = ZaviroMenuData.categories.filter { it != "All" }
                        val custom = products.filter { it.isAvailable }
                            .map { it.category.trim() }
                            .filter { it.isNotBlank() && base.none { b -> b.equals(it, ignoreCase = true) } }
                            .distinct()
                        base + custom
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(dynamicCategories) { category ->
                            CategoryPill(
                                category = category,
                                onClick = {
                                    viewModel.selectedCategory.value = category
                                    onNavigateToMenu()
                                }
                            )
                        }
                    }
                }
            }

            // Featured Deals Preview Section
            item {
                Column(modifier = Modifier.padding(top = 22.dp)) {
                    SectionHeaderRow(
                        title = "Featured Combo Deals",
                        subtitle = "Handpicked value meals for solo & family",
                        actionText = "View All 15",
                        onActionClick = onNavigateToDeals
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(featuredDeals) { deal ->
                            HomeDealCard(
                                deal = deal,
                                onClick = { onDealClick(deal) },
                                onAdd = { viewModel.addDealToCart(deal) }
                            )
                        }
                    }
                }
            }

            // Popular Products Section
            item {
                Column(modifier = Modifier.padding(top = 24.dp)) {
                    SectionHeaderRow(
                        title = "Popular Right Now",
                        subtitle = "Our most ordered signature dishes in Lahore",
                        actionText = "See All",
                        onActionClick = onNavigateToMenu
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        popularProducts.forEach { product ->
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

            // Why ZAVIRO Section
            item {
                WhyZaviroSection()
            }

            // Direct Contact & Fast Delivery Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = ZaviroDarkBase),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ZaviroRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TwoWheeler,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "FAST HOME DELIVERY",
                                    color = ZaviroOnDarkPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    letterSpacing = 0.4.sp
                                )
                                Text(
                                    text = "${settings.deliveryAreas} • Delivery Rs ${settings.deliveryFee}",
                                    color = ZaviroOnDarkMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.openWhatsApp(context) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ZaviroWhatsApp,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = "WhatsApp",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = settings.whatsappNumber,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = { viewModel.callZaviro(context) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = ZaviroDarkElevated,
                                    contentColor = Color.White
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ZaviroDarkBorder),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Call",
                                    tint = ZaviroRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Call Direct",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating WhatsApp button
        WhatsAppFloatingButton(
            phoneNumber = settings.whatsappNumber,
            onClick = { viewModel.openWhatsApp(context) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
        )
    }
}

@Composable
private fun SectionHeaderRow(
    title: String,
    subtitle: String,
    actionText: String,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = ZaviroTextWhite,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp
            )
            Text(
                text = subtitle,
                color = ZaviroTextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
        }
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ZaviroBurgundySoft,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable { onActionClick() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = actionText,
                    color = ZaviroRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = ZaviroRed,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
fun CategoryPill(
    category: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ZaviroSurfaceCard,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, ZaviroSurfaceBorder),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("cat_pill_$category")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(ZaviroBurgundySoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (category) {
                        "Pasta" -> Icons.Default.DinnerDining
                        "Crispy Chicken" -> Icons.Default.Fastfood
                        "Wings" -> Icons.Default.TakeoutDining
                        "Wraps & Rolls" -> Icons.Default.LunchDining
                        "Fries" -> Icons.Default.BakeryDining
                        "Drinks" -> Icons.Default.LocalDrink
                        else -> Icons.Default.Restaurant
                    },
                    contentDescription = category,
                    tint = ZaviroRed,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = category,
                color = ZaviroTextWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun HomeDealCard(
    deal: Deal,
    onClick: () -> Unit,
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(248.dp)
            .border(1.dp, ZaviroSurfaceBorder, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = ZaviroSurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(122.dp)
                    .background(ZaviroSurfaceDark)
            ) {
                com.example.ui.components.ZaviroFoodImage(
                    imageUrl = deal.imageUrl,
                    fallbackDrawableRes = if (deal.drawableRes != 0) deal.drawableRes else R.drawable.zaviro_hero_banner,
                    contentDescription = deal.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ZaviroDarkBase.copy(alpha = 0.85f),
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = "DEAL #${deal.dealNumber}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
                if (deal.saveAmount > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ZaviroRed,
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = "SAVE Rs ${deal.saveAmount}",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = deal.name,
                    color = ZaviroTextWhite,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = deal.items.joinToString(" • "),
                    color = ZaviroTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Rs ${deal.price}",
                            color = ZaviroRed,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                        if (deal.originalPrice > deal.price) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Rs ${deal.originalPrice}",
                                color = ZaviroTextMuted,
                                fontSize = 11.sp,
                                textDecoration = TextDecoration.LineThrough
                            )
                        }
                    }
                    Button(
                        onClick = onAdd,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ZaviroRed,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(9.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun WhyZaviroSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Text(
            text = "Why Choose ZAVIRO?",
            color = ZaviroTextWhite,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp
        )
        Text(
            text = "Good Food • Good Mood • Ghar Se Ghar Tak",
            color = ZaviroRed,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WhyPillarItem(
                icon = Icons.Default.Eco,
                title = "100% Fresh Halal",
                subtitle = "Premium meat & daily fresh ingredients",
                modifier = Modifier.weight(1f)
            )
            WhyPillarItem(
                icon = Icons.Default.Shield,
                title = "Hygienic Kitchen",
                subtitle = "Strict food safety & clean preparation",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WhyPillarItem(
                icon = Icons.Default.ElectricMoped,
                title = "Hot & Fast Delivery",
                subtitle = "Sealed thermal packaging to your door",
                modifier = Modifier.weight(1f)
            )
            WhyPillarItem(
                icon = Icons.Default.Favorite,
                title = "Signature Recipes",
                subtitle = "Rich sauces & authentic Pakistani spice",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun WhyPillarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ZaviroSurfaceCard,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, ZaviroSurfaceBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ZaviroBurgundySoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = ZaviroRed,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                color = ZaviroTextWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                color = ZaviroTextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

