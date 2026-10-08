package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Order
import com.example.ui.theme.*
import com.example.viewmodel.ZaviroViewModel

@Composable
fun CheckoutScreen(
    viewModel: ZaviroViewModel,
    onOrderSuccess: (Order) -> Unit,
    onBack: () -> Unit
) {
    val customerName by viewModel.customerName.collectAsState()
    val customerPhone by viewModel.customerPhone.collectAsState()
    val deliveryAddress by viewModel.deliveryAddress.collectAsState()
    val orderNotes by viewModel.orderNotes.collectAsState()
    val isPlacingOrder by viewModel.isPlacingOrder.collectAsState()
    val subtotal by viewModel.cartSubtotal.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val grandTotal = subtotal + settings.deliveryFee

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = ZaviroBlack,
        unfocusedContainerColor = ZaviroBlack,
        focusedBorderColor = ZaviroRed,
        unfocusedBorderColor = ZaviroSurfaceBorder,
        focusedTextColor = ZaviroTextWhite,
        unfocusedTextColor = ZaviroTextWhite
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ZaviroBlack)
            .testTag("checkout_screen")
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Deep Dark / Black Header Banner
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ZaviroDarkBase,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(ZaviroRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TwoWheeler,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Checkout & Delivery",
                            color = ZaviroOnDarkPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                        Text(
                            text = "Cash on Delivery Available Across Lahore",
                            color = ZaviroOnDarkMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Customer Login (Phone OTP / Google Account)
            CustomerAuthCard(
                viewModel = viewModel
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Delivery Information Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ZaviroSurfaceCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ZaviroSurfaceBorder),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = ZaviroRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Delivery Information",
                            color = ZaviroTextWhite,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Customer Name
                    Text(
                        text = "Customer Name *",
                        color = ZaviroTextWhite,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { viewModel.updateCustomerName(it) },
                        placeholder = { Text("Enter your full name", color = ZaviroTextMuted, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Person, contentDescription = "Name", tint = ZaviroRed)
                        },
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkout_name_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Phone
                    Text(
                        text = "Phone Number *",
                        color = ZaviroTextWhite,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customerPhone,
                        onValueChange = { viewModel.updateCustomerPhone(it) },
                        placeholder = { Text("0304-xxxxxxx", color = ZaviroTextMuted, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Phone, contentDescription = "Phone", tint = ZaviroRed)
                        },
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkout_phone_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Delivery Address
                    Text(
                        text = "Delivery Address (Lahore & Surrounding Areas) *",
                        color = ZaviroTextWhite,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = deliveryAddress,
                        onValueChange = { viewModel.updateDeliveryAddress(it) },
                        placeholder = { Text("House #, Street #, Sector / Area, Landmark", color = ZaviroTextMuted, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Home, contentDescription = "Address", tint = ZaviroRed)
                        },
                        minLines = 2,
                        colors = fieldColors,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkout_address_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Order Notes
                    Text(
                        text = "Order Notes / Instructions (Optional)",
                        color = ZaviroTextWhite,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = orderNotes,
                        onValueChange = { viewModel.updateOrderNotes(it) },
                        placeholder = { Text("E.g., Call upon arrival, keep extra spicy...", color = ZaviroTextMuted, fontSize = 13.sp) },
                        colors = fieldColors,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkout_notes_input")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Method Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, ZaviroRed, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = ZaviroBurgundySoft),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ZaviroRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = "COD",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cash on Delivery (COD)",
                            color = ZaviroTextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Pay in cash when our rider delivers to your doorstep",
                            color = ZaviroBurgundy,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = ZaviroRed,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Order Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ZaviroSurfaceCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ZaviroSurfaceBorder),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Order Summary",
                            color = ZaviroTextWhite,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ZaviroBurgundySoft
                        ) {
                            Text(
                                text = "${cartItems.sumOf { it.quantity }} items",
                                color = ZaviroRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    cartItems.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.quantity}x ${item.name}",
                                color = ZaviroTextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                            Text(
                                text = "Rs ${item.totalPrice}",
                                color = ZaviroTextWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = ZaviroSurfaceBorder)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Items Subtotal", color = ZaviroTextSecondary, fontSize = 13.sp)
                        Text("Rs $subtotal", color = ZaviroTextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Delivery Fee", color = ZaviroTextSecondary, fontSize = 13.sp)
                        Text("Rs ${settings.deliveryFee}", color = ZaviroTextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = ZaviroSurfaceBorder)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total to Pay (COD)",
                            color = ZaviroTextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Rs $grandTotal",
                            color = ZaviroRed,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            fontSize = 19.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Place Order Bottom Bar
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
                            text = "GRAND TOTAL",
                            color = ZaviroTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = "Rs $grandTotal",
                            color = ZaviroRed,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.placeOrder { placedOrder ->
                                onOrderSuccess(placedOrder)
                            }
                        },
                        enabled = !isPlacingOrder && cartItems.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ZaviroRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 26.dp, vertical = 14.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("checkout_place_order_btn")
                    ) {
                        if (isPlacingOrder) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Place Order (COD)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

