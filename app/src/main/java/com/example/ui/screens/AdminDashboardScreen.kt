package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LogoSlot
import com.example.model.OrderStatus
import com.example.ui.components.ZaviroDynamicLogo
import com.example.ui.theme.*
import com.example.viewmodel.ZaviroViewModel

@Composable
fun AdminDashboardScreen(
    viewModel: ZaviroViewModel,
    onBack: () -> Unit
) {
    val isAdminLoggedIn by viewModel.isAdminLoggedIn.collectAsState()
    val adminPassword by viewModel.adminPasswordInput.collectAsState()
    val adminError by viewModel.adminErrorMessage.collectAsState()
    val branding by viewModel.branding.collectAsState()

    var selectedAdminTab by remember { mutableIntStateOf(0) } // 0: Orders, 1: Manage Menu, 2: Deals, 3: Settings
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showLoginPassword by remember { mutableStateOf(false) }
    var localPassword by remember { mutableStateOf(adminPassword) }
    LaunchedEffect(adminPassword) {
        if (adminPassword != localPassword) {
            localPassword = adminPassword
        }
    }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    val submitAdminLogin = {
        val candidate = localPassword.ifEmpty { adminPassword }
        val success = viewModel.loginAdmin(candidate)
        if (success) {
            localPassword = ""
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ZaviroBackground)
            .testTag("admin_dashboard_screen")
    ) {
        // Executive Deep Dark Admin Header
        Surface(
            color = ZaviroDarkBase,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("admin_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    ZaviroDynamicLogo(
                        branding = branding,
                        slot = LogoSlot.ADMIN,
                        modifier = Modifier.size(36.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${branding.brandName} Admin Portal",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (isAdminLoggedIn) "Logged In • Store Dashboard" else "Store Management",
                            color = Color(0xFFD6CECE),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (isAdminLoggedIn) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showChangePasswordDialog = true },
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("admin_change_password_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockReset,
                                contentDescription = "Change Password",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Change Password", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Surface(
                            color = ZaviroRed,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.logoutAdmin() }
                                .testTag("admin_logout_btn")
                        ) {
                            Text(
                                text = "Logout",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        if (!isAdminLoggedIn) {
            // Login Prompt
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = ZaviroSurfaceCard),
                    border = BorderStroke(1.dp, ZaviroSurfaceBorder),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Deep Dark Logo Banner inside Login Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ZaviroDarkBase)
                                .padding(vertical = 22.dp, horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ZaviroDynamicLogo(
                                branding = branding,
                                slot = LogoSlot.LOGIN,
                                modifier = Modifier
                                    .fillMaxWidth(0.68f)
                                    .height(120.dp)
                                    .testTag("admin_zaviro_logo"),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${branding.brandName} Portal Access",
                                color = ZaviroTextPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp
                            )

                            Text(
                                text = "Sign in to manage menu items, value deals, pricing, branding, and live kitchen orders.",
                                color = ZaviroTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 6.dp)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            OutlinedTextField(
                                value = localPassword,
                                onValueChange = { newValue ->
                                    val hasNewline = newValue.contains('\n') || newValue.contains('\r')
                                    val sanitized = newValue.replace("\n", "").replace("\r", "")
                                    localPassword = sanitized
                                    viewModel.adminPasswordInput.value = sanitized
                                    viewModel.adminErrorMessage.value = null
                                    if (hasNewline) {
                                        submitAdminLogin()
                                    }
                                },
                                placeholder = { Text("Enter Admin Password", color = ZaviroTextMuted) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = ZaviroRed
                                    )
                                },
                                visualTransformation = if (showLoginPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done,
                                    capitalization = KeyboardCapitalization.None,
                                    autoCorrectEnabled = false
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { submitAdminLogin() },
                                    onGo = { submitAdminLogin() },
                                    onSend = { submitAdminLogin() },
                                    onNext = { submitAdminLogin() },
                                    onSearch = { submitAdminLogin() }
                                ),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { showLoginPassword = !showLoginPassword }) {
                                        Icon(
                                            imageVector = if (showLoginPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle password visibility",
                                            tint = ZaviroTextMuted
                                        )
                                    }
                                },
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
                                    .testTag("admin_password_input")
                            )

                            if (adminError != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = adminError ?: "",
                                    color = ZaviroRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("admin_login_error")
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = { submitAdminLogin() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ZaviroRed,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("admin_login_submit_btn")
                            ) {
                                Text("Unlock Dashboard", fontWeight = FontWeight.Black, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        } else {
            // Logged in Admin Interface
            Surface(
                color = ZaviroSurfaceCard,
                shadowElevation = 2.dp
            ) {
                TabRow(
                    selectedTabIndex = selectedAdminTab,
                    containerColor = ZaviroSurfaceCard,
                    contentColor = ZaviroRed,
                    indicator = { tabPositions ->
                        if (selectedAdminTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedAdminTab]),
                                height = 3.dp,
                                color = ZaviroRed
                            )
                        }
                    }
                ) {
                    Tab(
                        selected = selectedAdminTab == 0,
                        onClick = { selectedAdminTab = 0 },
                        selectedContentColor = ZaviroRed,
                        unselectedContentColor = ZaviroTextSecondary,
                        text = { Text("Orders", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        modifier = Modifier.testTag("admin_tab_orders")
                    )
                    Tab(
                        selected = selectedAdminTab == 1,
                        onClick = { selectedAdminTab = 1 },
                        selectedContentColor = ZaviroRed,
                        unselectedContentColor = ZaviroTextSecondary,
                        text = { Text("Manage Menu", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        modifier = Modifier.testTag("admin_tab_manage_menu")
                    )
                    Tab(
                        selected = selectedAdminTab == 2,
                        onClick = { selectedAdminTab = 2 },
                        selectedContentColor = ZaviroRed,
                        unselectedContentColor = ZaviroTextSecondary,
                        text = { Text("Deals", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        modifier = Modifier.testTag("admin_tab_deals")
                    )
                    Tab(
                        selected = selectedAdminTab == 3,
                        onClick = { selectedAdminTab = 3 },
                        selectedContentColor = ZaviroRed,
                        unselectedContentColor = ZaviroTextSecondary,
                        text = { Text("Settings", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        modifier = Modifier.testTag("admin_tab_settings")
                    )
                }
            }

            when (selectedAdminTab) {
                0 -> AdminOrdersTab(viewModel)
                1 -> AdminManageMenuTab(viewModel)
                2 -> AdminDealsTab(viewModel)
                3 -> AdminSettingsTab(viewModel)
            }
        }
    }

    if (showChangePasswordDialog) {
        AdminChangePasswordDialog(
            viewModel = viewModel,
            onDismiss = { showChangePasswordDialog = false }
        )
    }
}

@Composable
private fun AdminStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.shadow(2.dp, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = ZaviroSurfaceCard),
        border = BorderStroke(1.dp, ZaviroSurfaceBorder),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = label,
                    color = ZaviroTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                color = ZaviroTextPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun AdminOrdersTab(viewModel: ZaviroViewModel) {
    val orders by viewModel.orders.collectAsState()
    val products by viewModel.products.collectAsState()
    val deals by viewModel.deals.collectAsState()

    val activeCount = orders.count { it.status != OrderStatus.DELIVERED && it.status != OrderStatus.CANCELLED }
    val totalRevenue = orders.filter { it.status != OrderStatus.CANCELLED }.sumOf { it.total }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Modern Statistics Layout
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Store Overview & Live Metrics",
                    color = ZaviroTextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminStatCard(
                        label = "TOTAL ORDERS",
                        value = "${orders.size}",
                        icon = Icons.Default.ReceiptLong,
                        accentColor = ZaviroRed,
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatCard(
                        label = "ACTIVE LIVE",
                        value = "$activeCount",
                        icon = Icons.Default.LocalFireDepartment,
                        accentColor = ZaviroBurgundy,
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatCard(
                        label = "REVENUE",
                        value = "Rs $totalRevenue",
                        icon = Icons.Default.Payments,
                        accentColor = ZaviroGreen,
                        modifier = Modifier.weight(1.2f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminStatCard(
                        label = "MENU ITEMS",
                        value = "${products.count { it.isAvailable }}/${products.size} Active",
                        icon = Icons.Default.RestaurantMenu,
                        accentColor = ZaviroRed,
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatCard(
                        label = "VALUE DEALS",
                        value = "${deals.count { it.isAvailable }}/${deals.size} Active",
                        icon = Icons.Default.LocalOffer,
                        accentColor = ZaviroBurgundy,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (orders.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = ZaviroSurfaceCard),
                    border = BorderStroke(1.dp, ZaviroSurfaceBorder),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = null,
                            tint = ZaviroTextMuted,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No customer orders yet",
                            color = ZaviroTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Incoming orders will appear here in real time.",
                            color = ZaviroTextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            item {
                Text(
                    text = "Customer Orders (${orders.size})",
                    color = ZaviroTextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(orders, key = { it.id }) { order ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = ZaviroSurfaceCard),
                    border = BorderStroke(1.dp, ZaviroSurfaceBorder),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = ZaviroDarkBase,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = order.orderNumber,
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (order.status) {
                                    OrderStatus.DELIVERED -> ZaviroGreen
                                    OrderStatus.CANCELLED -> ZaviroRed
                                    else -> ZaviroRedTint
                                }
                            ) {
                                Text(
                                    text = order.status.label,
                                    color = when (order.status) {
                                        OrderStatus.DELIVERED, OrderStatus.CANCELLED -> Color.White
                                        else -> ZaviroRed
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Customer: ${order.customerName} • ${order.phone}",
                            color = ZaviroTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Address: ${order.deliveryAddress}",
                            color = ZaviroTextSecondary,
                            fontSize = 12.sp
                        )

                        if (order.orderNotes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Notes: ${order.orderNotes}",
                                color = ZaviroBurgundy,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ZaviroBackground,
                            border = BorderStroke(1.dp, ZaviroSurfaceBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                order.items.forEach { item ->
                                    Text(
                                        text = "• ${item.quantity}x ${item.name} (Rs ${item.unitPrice})",
                                        color = ZaviroTextPrimary,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Total to Collect: Rs ${order.total} (COD)",
                            color = ZaviroRed,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Update Order Status:",
                            color = ZaviroTextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OrderStatus.entries.filter { it != OrderStatus.CANCELLED }.forEach { status ->
                                val isSelected = order.status == status
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) ZaviroRed else ZaviroBackground,
                                    border = BorderStroke(1.dp, if (isSelected) ZaviroRed else ZaviroSurfaceBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { viewModel.updateOrderStatus(order.id, status) }
                                ) {
                                    Text(
                                        text = when (status) {
                                            OrderStatus.RECEIVED -> "Received"
                                            OrderStatus.PREPARING -> "Cooking"
                                            OrderStatus.READY -> "Ready"
                                            OrderStatus.OUT_FOR_DELIVERY -> "Out"
                                            OrderStatus.DELIVERED -> "Done"
                                            else -> status.name
                                        },
                                        color = if (isSelected) Color.White else ZaviroTextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminSettingsTab(viewModel: ZaviroViewModel) {
    var settingsSubTab by remember { mutableIntStateOf(0) } // 0: Change Password, 1: Branding, 2: Delivery & Store

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = settingsSubTab,
            containerColor = ZaviroBackground,
            contentColor = ZaviroRed,
            indicator = { tabPositions ->
                if (settingsSubTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[settingsSubTab]),
                        height = 2.5.dp,
                        color = ZaviroRed
                    )
                }
            }
        ) {
            Tab(
                selected = settingsSubTab == 0,
                onClick = { settingsSubTab = 0 },
                selectedContentColor = ZaviroRed,
                unselectedContentColor = ZaviroTextSecondary,
                text = { Text("Change Password", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                modifier = Modifier.testTag("settings_subtab_password")
            )
            Tab(
                selected = settingsSubTab == 1,
                onClick = { settingsSubTab = 1 },
                selectedContentColor = ZaviroRed,
                unselectedContentColor = ZaviroTextSecondary,
                text = { Text("Branding", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                modifier = Modifier.testTag("settings_subtab_branding")
            )
            Tab(
                selected = settingsSubTab == 2,
                onClick = { settingsSubTab = 2 },
                selectedContentColor = ZaviroRed,
                unselectedContentColor = ZaviroTextSecondary,
                text = { Text("Delivery & Store", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                modifier = Modifier.testTag("settings_subtab_delivery")
            )
        }

        when (settingsSubTab) {
            0 -> AdminChangePasswordSection(viewModel)
            1 -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                AdminBrandingSection(viewModel)
            }
            2 -> AdminDeliverySettingsSection(viewModel)
        }
    }
}

@Composable
fun AdminDeliverySettingsSection(viewModel: ZaviroViewModel) {
    val settings by viewModel.settings.collectAsState()
    val branding by viewModel.branding.collectAsState()
    var deliveryFeeInput by remember(settings.deliveryFee) { mutableStateOf("${settings.deliveryFee}") }
    var announcementInput by remember(settings.announcement) { mutableStateOf(settings.announcement) }
    var whatsappInput by remember(settings.whatsappNumber) { mutableStateOf(settings.whatsappNumber) }
    var instagramInput by remember(settings.instagramHandle) { mutableStateOf(settings.instagramHandle) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = ZaviroSurfaceCard),
            border = BorderStroke(1.dp, ZaviroSurfaceBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Delivery & Store Settings",
                    color = ZaviroTextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
                Text(
                    text = "Configure customer delivery fee & store contact info across Lahore",
                    color = ZaviroTextMuted,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = deliveryFeeInput,
                    onValueChange = { deliveryFeeInput = it },
                    label = { Text("Delivery Fee in PKR (Rs)", color = ZaviroTextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ZaviroTextPrimary,
                        unfocusedTextColor = ZaviroTextPrimary,
                        focusedBorderColor = ZaviroRed,
                        unfocusedBorderColor = ZaviroSurfaceBorder
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_delivery_fee_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = announcementInput,
                    onValueChange = { announcementInput = it },
                    label = { Text("Store Announcement Banner", color = ZaviroTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ZaviroTextPrimary,
                        unfocusedTextColor = ZaviroTextPrimary,
                        focusedBorderColor = ZaviroRed,
                        unfocusedBorderColor = ZaviroSurfaceBorder
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = whatsappInput,
                    onValueChange = { whatsappInput = it },
                    label = { Text("WhatsApp Number", color = ZaviroTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ZaviroTextPrimary,
                        unfocusedTextColor = ZaviroTextPrimary,
                        focusedBorderColor = ZaviroRed,
                        unfocusedBorderColor = ZaviroSurfaceBorder
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = instagramInput,
                    onValueChange = { instagramInput = it },
                    label = { Text("Instagram Handle", color = ZaviroTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ZaviroTextPrimary,
                        unfocusedTextColor = ZaviroTextPrimary,
                        focusedBorderColor = ZaviroRed,
                        unfocusedBorderColor = ZaviroSurfaceBorder
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val fee = deliveryFeeInput.toIntOrNull() ?: settings.deliveryFee
                        viewModel.updateStoreSettings(
                            settings.copy(
                                deliveryFee = fee,
                                announcement = announcementInput.trim().ifBlank { settings.announcement },
                                whatsappNumber = whatsappInput.trim().ifBlank { settings.whatsappNumber },
                                instagramHandle = instagramInput.trim().ifBlank { settings.instagramHandle }
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZaviroRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("admin_update_delivery_fee_btn")
                ) {
                    Text("Save Store & Delivery Settings", fontWeight = FontWeight.Black)
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = ZaviroSurfaceCard),
            border = BorderStroke(1.dp, ZaviroSurfaceBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ZAVIRO Business Identity",
                    color = ZaviroTextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Brand: ${branding.brandName}", color = ZaviroRed, fontWeight = FontWeight.Bold)
                Text(text = "Tagline: ${branding.tagline}", color = ZaviroTextPrimary, fontWeight = FontWeight.SemiBold)
                Text(text = "WhatsApp: ${settings.whatsappNumber}", color = ZaviroTextSecondary)
                Text(text = "Instagram: ${settings.instagramHandle}", color = ZaviroTextSecondary)
                Text(text = "Coverage: ${settings.deliveryAreas}", color = ZaviroTextSecondary)
                Text(text = "Payment Method: Cash on Delivery (COD)", color = ZaviroTextSecondary)
            }
        }
    }
}
