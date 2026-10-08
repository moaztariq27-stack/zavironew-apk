package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.ui.theme.*
import com.example.viewmodel.ZaviroViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MyOrdersScreen(
    viewModel: ZaviroViewModel,
    onTrackOrder: (String) -> Unit,
    onExploreMenu: () -> Unit
) {
    val orders by viewModel.orders.collectAsState()

    val activeOrders = remember(orders) {
        orders.filter { it.status != OrderStatus.DELIVERED && it.status != OrderStatus.CANCELLED }
    }
    val pastOrders = remember(orders) {
        orders.filter { it.status == OrderStatus.DELIVERED || it.status == OrderStatus.CANCELLED }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ZaviroBlack)
            .testTag("my_orders_screen")
    ) {
        Surface(
            color = ZaviroDarkBase,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "My Orders",
                        color = ZaviroOnDarkPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                    Text(
                        text = "Track active deliveries & view order history",
                        color = ZaviroOnDarkMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (orders.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = ZaviroRed
                    ) {
                        Text(
                            text = "${orders.size} Total",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        if (orders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = ZaviroSurfaceCard,
                    shadowElevation = 2.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ZaviroSurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(ZaviroBurgundySoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = "No Orders",
                                tint = ZaviroRed,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Orders Yet",
                            color = ZaviroTextWhite,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "When you place an order with ZAVIRO, you can track its live kitchen & rider status right here!",
                            color = ZaviroTextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 19.sp
                        )
                        Spacer(modifier = Modifier.height(22.dp))
                        Button(
                            onClick = onExploreMenu,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ZaviroRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("my_orders_explore_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestaurantMenu,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Order Delicious Food", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("my_orders_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (activeOrders.isNotEmpty()) {
                    item {
                        Text(
                            text = "Active Orders (${activeOrders.size})",
                            color = ZaviroRed,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                        )
                    }

                    items(activeOrders, key = { it.id }) { order ->
                        OrderCardItem(
                            order = order,
                            isActive = true,
                            onTrack = { onTrackOrder(order.id) }
                        )
                    }
                }

                if (pastOrders.isNotEmpty()) {
                    item {
                        Text(
                            text = "Past Orders (${pastOrders.size})",
                            color = ZaviroTextWhite,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                        )
                    }

                    items(pastOrders, key = { it.id }) { order ->
                        OrderCardItem(
                            order = order,
                            isActive = false,
                            onTrack = { onTrackOrder(order.id) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun OrderCardItem(
    order: Order,
    isActive: Boolean,
    onTrack: () -> Unit
) {
    val dateString = remember(order.createdAt) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        sdf.format(Date(order.createdAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) ZaviroRed else ZaviroSurfaceBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable { onTrack() }
            .testTag("order_card_${order.id}"),
        colors = CardDefaults.cardColors(containerColor = ZaviroSurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = order.orderNumber,
                        color = ZaviroTextWhite,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = dateString,
                        color = ZaviroTextMuted,
                        fontSize = 11.sp
                    )
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (order.status) {
                        OrderStatus.DELIVERED -> ZaviroGreen
                        OrderStatus.CANCELLED -> ZaviroDarkBase
                        else -> ZaviroRed
                    }
                ) {
                    Text(
                        text = order.status.label.uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.4.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = ZaviroSurfaceBorder)
            Spacer(modifier = Modifier.height(10.dp))

            // Items preview
            order.items.forEach { item ->
                Text(
                    text = "${item.quantity}x ${item.name}",
                    color = ZaviroTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TOTAL AMOUNT",
                        color = ZaviroTextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Rs ${order.total}",
                        color = ZaviroRed,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp
                    )
                }

                Button(
                    onClick = onTrack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActive) ZaviroRed else ZaviroBlack,
                        contentColor = if (isActive) Color.White else ZaviroTextWhite
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TwoWheeler,
                        contentDescription = "Track",
                        tint = if (isActive) Color.White else ZaviroRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isActive) "Track Live" else "View Details",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

