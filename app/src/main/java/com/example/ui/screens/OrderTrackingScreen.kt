package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.ZaviroViewModel

@Composable
fun OrderTrackingScreen(
    orderId: String,
    viewModel: ZaviroViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val orders by viewModel.orders.collectAsState()
    val order = orders.find { it.id == orderId } ?: orders.firstOrNull()

    val steps = listOf(
        Triple("Order Received", "Your order has been accepted by ZAVIRO kitchen", Icons.Default.Receipt),
        Triple("Preparing", "Chef is cooking your fresh food to perfection", Icons.Default.Restaurant),
        Triple("Ready", "Packed safely in hot tamper-proof boxes", Icons.Default.Inventory),
        Triple("Out for Delivery", "Rider is heading to your doorstep in Lahore", Icons.Default.TwoWheeler),
        Triple("Delivered", "Delicious meal delivered. Enjoy your meal!", Icons.Default.CheckCircle)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ZaviroBlack)
            .testTag("order_tracking_screen")
    ) {
        if (order == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Order not found", color = ZaviroTextMuted)
            }
        } else {
            val currentStep = order.status.stepIndex

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Deep Dark / Black Header Status Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ZaviroDarkBase,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = ZaviroRed
                        ) {
                            Text(
                                text = "LIVE ORDER STATUS",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = order.status.label,
                            color = ZaviroOnDarkPrimary,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${order.orderNumber} • ${order.paymentMethod}",
                            color = ZaviroOnDarkMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 5-Stage Stepper Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ZaviroSurfaceCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ZaviroSurfaceBorder),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Live Kitchen & Rider Progress",
                            color = ZaviroTextWhite,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        steps.forEachIndexed { index, (title, desc, icon) ->
                            val isCompleted = index < currentStep
                            val isCurrent = index == currentStep
                            val isPending = index > currentStep

                            val circleColor by animateColorAsState(
                                targetValue = when {
                                    isCompleted -> ZaviroGreen
                                    isCurrent -> ZaviroRed
                                    else -> ZaviroBlack
                                },
                                label = "circleColor"
                            )

                            val iconColor = when {
                                isCompleted || isCurrent -> Color.White
                                else -> ZaviroTextMuted
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(38.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(circleColor)
                                            .border(
                                                width = if (isCurrent) 2.dp else 1.dp,
                                                color = when {
                                                    isCompleted -> ZaviroGreen
                                                    isCurrent -> ZaviroRed
                                                    else -> ZaviroSurfaceBorder
                                                },
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = title,
                                            tint = iconColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    if (index < steps.size - 1) {
                                        Box(
                                            modifier = Modifier
                                                .width(2.5.dp)
                                                .height(44.dp)
                                                .background(
                                                    if (index < currentStep) ZaviroGreen else ZaviroSurfaceBorder
                                                )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(bottom = if (index < steps.size - 1) 20.dp else 0.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = title,
                                            color = if (isPending) ZaviroTextMuted else ZaviroTextWhite,
                                            fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        if (isCurrent) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = ZaviroBurgundySoft
                                            ) {
                                                Text(
                                                    text = "IN PROGRESS",
                                                    color = ZaviroRed,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = desc,
                                        color = if (isPending) ZaviroTextMuted else ZaviroTextSecondary,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Order Details Preview Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ZaviroSurfaceCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ZaviroSurfaceBorder),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "DELIVERY ADDRESS",
                            color = ZaviroTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = order.deliveryAddress,
                            color = ZaviroTextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = ZaviroSurfaceBorder)
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Items (${order.items.size}): ${order.items.joinToString { "${it.quantity}x ${it.name}" }}",
                            color = ZaviroTextSecondary,
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Total Amount: Rs ${order.total} (COD)",
                            color = ZaviroRed,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Quick WhatsApp Contact for this order
                Button(
                    onClick = {
                        viewModel.openWhatsApp(
                            context,
                            "Hello ZAVIRO! Checking up on my order ${order.orderNumber} placed for ${order.deliveryAddress}."
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZaviroWhatsApp,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("tracking_whatsapp_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "WhatsApp",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "WhatsApp Rider / Support",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

