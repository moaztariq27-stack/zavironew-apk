package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Deal
import com.example.ui.theme.*
import com.example.viewmodel.ZaviroViewModel

@Composable
fun DealDetailScreen(
    deal: Deal,
    viewModel: ZaviroViewModel,
    onBack: () -> Unit
) {
    val allDeals by viewModel.deals.collectAsState()
    val liveDeal = remember(allDeals, deal) {
        allDeals.find { it.id == deal.id } ?: deal
    }

    var quantity by remember { mutableIntStateOf(1) }
    var specialInstructions by remember { mutableStateOf("") }

    val totalPrice = liveDeal.price * quantity

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ZaviroBlack)
            .testTag("deal_detail_screen")
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Deal Hero Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(248.dp)
                    .background(ZaviroDarkBase)
            ) {
                val imgRes = if (liveDeal.drawableRes != 0) liveDeal.drawableRes else R.drawable.zaviro_hero_banner
                com.example.ui.components.ZaviroFoodImage(
                    imageUrl = liveDeal.imageUrl,
                    fallbackDrawableRes = imgRes,
                    contentDescription = liveDeal.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("deal_detail_image"),
                    contentScale = ContentScale.Crop
                )

                if (liveDeal.saveAmount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ZaviroRed,
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = "SAVE Rs ${liveDeal.saveAmount}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ZaviroDarkBase.copy(alpha = 0.85f),
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomStart)
                ) {
                    Text(
                        text = "DEAL #${liveDeal.dealNumber} • ${liveDeal.category.uppercase()}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ZaviroSurfaceCard,
                    shadowElevation = 2.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ZaviroSurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = liveDeal.name,
                            color = ZaviroTextWhite,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Rs ${liveDeal.price}",
                                color = ZaviroRed,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp
                            )
                            if (liveDeal.originalPrice > liveDeal.price) {
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Rs ${liveDeal.originalPrice}",
                                    color = ZaviroTextMuted,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    textDecoration = TextDecoration.LineThrough
                                )
                            }
                        }

                        if (liveDeal.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = liveDeal.description,
                                color = ZaviroTextSecondary,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = ZaviroSurfaceBorder, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        // Quantity Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Combo Quantity",
                                    color = ZaviroTextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Complete meal combo",
                                    color = ZaviroTextMuted,
                                    fontSize = 11.sp
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(ZaviroBlack, RoundedCornerShape(12.dp))
                                    .border(1.dp, ZaviroSurfaceBorder, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                IconButton(
                                    onClick = { if (quantity > 1) quantity-- },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (quantity > 1) ZaviroSurfaceCard else Color.Transparent)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrease",
                                        tint = if (quantity > 1) ZaviroTextWhite else ZaviroTextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Text(
                                    text = "$quantity",
                                    color = ZaviroTextWhite,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                IconButton(
                                    onClick = { quantity++ },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ZaviroRed)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increase",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // What's Included
                Text(
                    text = "What's Included in this Deal",
                    color = ZaviroTextWhite,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                liveDeal.items.forEach { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = ZaviroSurfaceCard),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ZaviroSurfaceBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(ZaviroBurgundySoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Included",
                                    tint = ZaviroRed,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = item,
                                color = ZaviroTextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Special Instructions
                Text(
                    text = "Special Instructions",
                    color = ZaviroTextWhite,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = specialInstructions,
                    onValueChange = { specialInstructions = it },
                    placeholder = {
                        Text(
                            text = "E.g., choice of drink (Pepsi/7Up), extra napkins...",
                            color = ZaviroTextMuted,
                            fontSize = 13.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ZaviroSurfaceCard,
                        unfocusedContainerColor = ZaviroSurfaceCard,
                        focusedBorderColor = ZaviroRed,
                        unfocusedBorderColor = ZaviroSurfaceBorder,
                        focusedTextColor = ZaviroTextWhite,
                        unfocusedTextColor = ZaviroTextWhite
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Bottom Add Button
        Surface(
            color = ZaviroSurfaceCard,
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                HorizontalDivider(color = ZaviroSurfaceBorder, thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOTAL AMOUNT",
                            color = ZaviroTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = "Rs $totalPrice",
                            color = ZaviroRed,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.addDealToCart(liveDeal, quantity, specialInstructions)
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ZaviroRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("deal_detail_add_to_cart_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = "Cart",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add Deal to Cart",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

