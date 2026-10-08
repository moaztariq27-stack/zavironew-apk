package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ExtraOption
import com.example.model.Product
import com.example.ui.theme.*
import com.example.viewmodel.ZaviroViewModel

@Composable
fun ProductDetailScreen(
    product: Product,
    viewModel: ZaviroViewModel,
    onBack: () -> Unit
) {
    val allProducts by viewModel.products.collectAsState()
    val allExtras by viewModel.extras.collectAsState()
    val liveProduct = remember(allProducts, product) {
        allProducts.find { it.id == product.id } ?: product
    }

    var quantity by remember { mutableIntStateOf(1) }
    val selectedExtras = remember { mutableStateListOf<ExtraOption>() }
    var specialInstructions by remember { mutableStateOf("") }

    val extrasToChoose = remember(allExtras) {
        allExtras.filter { it.isAvailable }
    }

    val currentUnitPrice = liveProduct.price + selectedExtras.sumOf { it.price }
    val totalPrice = currentUnitPrice * quantity

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ZaviroBlack)
            .testTag("product_detail_screen")
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Large Appetizing Product Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(256.dp)
                    .background(ZaviroDarkBase)
            ) {
                val imgRes = if (liveProduct.drawableRes != 0) liveProduct.drawableRes else R.drawable.zaviro_creamy_pasta
                com.example.ui.components.ZaviroFoodImage(
                    imageUrl = liveProduct.imageUrl,
                    fallbackDrawableRes = imgRes,
                    contentDescription = liveProduct.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("product_detail_image"),
                    contentScale = ContentScale.Crop
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.BottomStart),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ZaviroDarkBase.copy(alpha = 0.85f)
                    ) {
                        Text(
                            text = liveProduct.category.uppercase(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    if (liveProduct.isPopular) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ZaviroRed
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Whatshot,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "BESTSELLER",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }

            // Main Product Overview Card
            Column(modifier = Modifier.padding(16.dp)) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ZaviroSurfaceCard,
                    shadowElevation = 2.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ZaviroSurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = liveProduct.name,
                                color = ZaviroTextWhite,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Rs ${liveProduct.price}",
                                color = ZaviroRed,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = liveProduct.description,
                            color = ZaviroTextSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )

                        if (liveProduct.ingredients.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ZaviroBlack,
                                border = androidx.compose.foundation.BorderStroke(1.dp, ZaviroSurfaceBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "INGREDIENTS & PREPARATION",
                                        color = ZaviroBurgundy,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        letterSpacing = 0.6.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = liveProduct.ingredients,
                                        color = ZaviroTextSecondary,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = ZaviroSurfaceBorder, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(14.dp))

                        // Quantity selector row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Select Quantity",
                                    color = ZaviroTextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Freshly made to order",
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
                                        .testTag("qty_minus_button")
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
                                        .testTag("qty_plus_button")
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

                // Extras Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Add Extras & Toppings",
                            color = ZaviroTextWhite,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Optional delicious add-ons for your meal",
                            color = ZaviroTextMuted,
                            fontSize = 12.sp
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ZaviroSurfaceDark
                    ) {
                        Text(
                            text = "OPTIONAL",
                            color = ZaviroTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                extrasToChoose.forEach { extra ->
                    val isChecked = selectedExtras.any { it.id == extra.id }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isChecked) ZaviroBurgundySoft else ZaviroSurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isChecked) ZaviroRed else ZaviroSurfaceBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                if (isChecked) {
                                    selectedExtras.removeAll { it.id == extra.id }
                                } else {
                                    selectedExtras.add(extra)
                                }
                            }
                            .testTag("extra_option_${extra.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 13.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isChecked) ZaviroRed else ZaviroBlack)
                                        .border(
                                            1.dp,
                                            if (isChecked) ZaviroRed else ZaviroSurfaceBorder,
                                            RoundedCornerShape(6.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isChecked) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = extra.name,
                                    color = ZaviroTextWhite,
                                    fontSize = 14.sp,
                                    fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium
                                )
                            }

                            Text(
                                text = "+Rs ${extra.price}",
                                color = if (isChecked) ZaviroRed else ZaviroTextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Special Instructions
                Text(
                    text = "Special Kitchen Instructions",
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
                            text = "E.g., extra spicy, less mayo, garlic sauce on the side...",
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("special_instructions_input")
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Bottom Add to Cart Bar
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
                            viewModel.addProductToCart(
                                product = liveProduct,
                                quantity = quantity,
                                extras = selectedExtras.toList(),
                                specialInstructions = specialInstructions
                            )
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
                            .testTag("detail_add_to_cart_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = "Cart",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add to Cart",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

