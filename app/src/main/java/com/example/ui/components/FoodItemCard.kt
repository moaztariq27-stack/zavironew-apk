package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Whatshot
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Product
import com.example.ui.theme.*

@Composable
fun FoodItemCard(
    product: Product,
    onProductClick: (Product) -> Unit,
    onQuickAdd: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    val cardInteraction = remember { MutableInteractionSource() }
    val isCardPressed by cardInteraction.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isCardPressed) 0.985f else 1f,
        label = "food_card_scale"
    )

    val buttonInteraction = remember { MutableInteractionSource() }
    val isButtonPressed by buttonInteraction.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isButtonPressed) 0.94f else 1f,
        label = "add_btn_scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(cardScale)
            .border(
                width = 1.dp,
                color = ZaviroSurfaceBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = cardInteraction,
                indication = ripple()
            ) { onProductClick(product) }
            .testTag("food_card_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = ZaviroSurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column {
            // Large attractive food image as visual focus
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(144.dp)
                    .background(ZaviroSurfaceDark)
            ) {
                val imageRes = if (product.drawableRes != 0) product.drawableRes else R.drawable.zaviro_creamy_pasta
                ZaviroFoodImage(
                    imageUrl = product.imageUrl,
                    fallbackDrawableRes = imageRes,
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("food_card_image_${product.id}"),
                    contentScale = ContentScale.Crop
                )

                // Category badge on dark pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ZaviroDarkBase.copy(alpha = 0.82f),
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = product.category.uppercase(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (product.isPopular) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ZaviroRed,
                        modifier = Modifier
                            .padding(10.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Whatshot,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "BESTSELLER",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }
                }
            }

            // Food item details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 13.dp, vertical = 12.dp)
            ) {
                Text(
                    text = product.name,
                    color = ZaviroTextWhite,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = product.description,
                    color = ZaviroTextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.heightIn(min = 32.dp)
                )

                if (product.ingredients.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = product.ingredients,
                        color = ZaviroTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = ZaviroSurfaceBorder.copy(alpha = 0.7f), thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PRICE",
                            color = ZaviroTextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = "Rs ${product.price}",
                            color = ZaviroRed,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp
                        )
                    }

                    Button(
                        onClick = { onQuickAdd(product) },
                        interactionSource = buttonInteraction,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ZaviroRed,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 13.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 1.dp,
                            pressedElevation = 0.dp
                        ),
                        modifier = Modifier
                            .height(36.dp)
                            .scale(buttonScale)
                            .testTag("add_product_${product.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Add",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

