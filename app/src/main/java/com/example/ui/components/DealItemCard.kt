package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Deal
import com.example.ui.theme.*

@Composable
fun DealItemCard(
    deal: Deal,
    onOrderDeal: (Deal) -> Unit,
    onDealClick: ((Deal) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val buttonInteraction = remember { MutableInteractionSource() }
    val isPressed by buttonInteraction.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "deal_btn_scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = ZaviroSurfaceBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .clip(RoundedCornerShape(18.dp))
            .clickable { (onDealClick ?: onOrderDeal).invoke(deal) }
            .testTag("deal_card_${deal.dealNumber}"),
        colors = CardDefaults.cardColors(containerColor = ZaviroSurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column {
            // Deep Dark / Black Base Header with Warm Red Deal Number & Category Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ZaviroDarkBase)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ZaviroRed,
                        modifier = Modifier.padding(end = 10.dp)
                    ) {
                        Text(
                            text = "DEAL #${deal.dealNumber}",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 0.4.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Text(
                        text = deal.name,
                        color = ZaviroOnDarkPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ZaviroDarkElevated
                ) {
                    Text(
                        text = deal.category.uppercase(),
                        color = ZaviroOnDarkSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large attractive food image thumbnail
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ZaviroSurfaceDark)
                        .border(1.dp, ZaviroSurfaceBorder, RoundedCornerShape(14.dp))
                ) {
                    val imageRes = if (deal.drawableRes != 0) deal.drawableRes else R.drawable.zaviro_hero_banner
                    ZaviroFoodImage(
                        imageUrl = deal.imageUrl,
                        fallbackDrawableRes = imageRes,
                        contentDescription = deal.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("deal_card_image_${deal.id}"),
                        contentScale = ContentScale.Crop
                    )

                    if (deal.saveAmount > 0) {
                        Surface(
                            shape = RoundedCornerShape(bottomEnd = 10.dp),
                            color = ZaviroRed,
                            modifier = Modifier.align(Alignment.TopStart)
                        ) {
                            Text(
                                text = "SAVE Rs ${deal.saveAmount}",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Combo items list inside deal
                Column(modifier = Modifier.weight(1f)) {
                    if (deal.description.isNotBlank()) {
                        Text(
                            text = deal.description,
                            color = ZaviroBurgundy,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    deal.items.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(ZaviroBurgundySoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = ZaviroRed,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item,
                                color = ZaviroTextWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = ZaviroSurfaceBorder, thickness = 1.dp)

            // Pricing and CTA footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ZaviroSurfaceElevated.copy(alpha = 0.55f))
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "COMBO PRICE",
                        color = ZaviroTextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Rs ${deal.price}",
                            color = ZaviroRed,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            fontSize = 19.sp
                        )
                        if (deal.originalPrice > deal.price) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Rs ${deal.originalPrice}",
                                color = ZaviroTextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                textDecoration = TextDecoration.LineThrough
                            )
                        }
                    }
                }

                Button(
                    onClick = { onOrderDeal(deal) },
                    interactionSource = buttonInteraction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZaviroRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    modifier = Modifier
                        .height(42.dp)
                        .scale(buttonScale)
                        .testTag("order_deal_${deal.dealNumber}")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddShoppingCart,
                        contentDescription = "Order",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Order Now",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

