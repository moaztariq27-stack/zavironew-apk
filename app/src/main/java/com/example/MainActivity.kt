package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ZaviroBottomNav
import com.example.ui.components.ZaviroTopBar
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ZaviroBlack
import com.example.viewmodel.Screen
import com.example.viewmodel.ZaviroViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ZaviroViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val branding by viewModel.branding.collectAsStateWithLifecycle()
            val isInitialSettingsLoaded by viewModel.isInitialSettingsLoaded.collectAsStateWithLifecycle()

            MyApplicationTheme(
                primaryColorHex = branding.primaryColorHex,
                secondaryColorHex = branding.secondaryColorHex
            ) {
                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    SplashScreen(
                        branding = branding,
                        isInitialSettingsLoaded = isInitialSettingsLoaded,
                        onTimeout = { showSplash = false }
                    )
                } else {
                    val currentScreen by viewModel.screen.collectAsStateWithLifecycle()
                    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
                    val totalCartCount = cartItems.sumOf { it.quantity }

                val isPrimaryTab = currentScreen is Screen.Home ||
                        currentScreen is Screen.Menu ||
                        currentScreen is Screen.Deals ||
                        currentScreen is Screen.MyOrders ||
                        currentScreen is Screen.Profile

                // Handle system back press
                BackHandler(enabled = !isPrimaryTab || currentScreen !is Screen.Home) {
                    if (!viewModel.navigateBack()) {
                        if (currentScreen !is Screen.Home) {
                            viewModel.switchTab(Screen.Home)
                        }
                    }
                }

                val screenTitle = when (currentScreen) {
                    is Screen.Home -> null
                    is Screen.Menu -> "ZAVIRO Menu"
                    is Screen.Deals -> "15 Amazing Deals"
                    is Screen.ProductDetail -> (currentScreen as Screen.ProductDetail).product.name
                    is Screen.DealDetail -> "Deal Details"
                    is Screen.Cart -> "Cart & Review"
                    is Screen.Checkout -> "Checkout"
                    is Screen.OrderConfirmation -> "Order Placed"
                    is Screen.OrderTracking -> "Order Tracking"
                    is Screen.MyOrders -> "My Orders"
                    is Screen.Profile -> "Profile & Support"
                    is Screen.CustomerLogin -> "Customer Login"
                    is Screen.AdminDashboard -> "Admin Portal"
                    is Screen.PwaWebApp -> "Web App & PWA"
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ZaviroBlack),
                    contentAlignment = Alignment.TopCenter
                ) {
                Scaffold(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .background(ZaviroBlack),
                    containerColor = ZaviroBlack,
                    topBar = {
                        if (currentScreen !is Screen.AdminDashboard && currentScreen !is Screen.PwaWebApp) {
                            ZaviroTopBar(
                                title = screenTitle,
                                showBackButton = !isPrimaryTab,
                                onBackClick = {
                                    if (!viewModel.navigateBack()) {
                                        viewModel.switchTab(Screen.Home)
                                    }
                                },
                                cartItemCount = totalCartCount,
                                branding = branding,
                                onCartClick = { viewModel.navigateTo(Screen.Cart) },
                                onAdminClick = { viewModel.navigateTo(Screen.AdminDashboard) },
                                onPwaClick = { viewModel.navigateTo(Screen.PwaWebApp) },
                                onLogoClick = { viewModel.switchTab(Screen.Home) }
                            )
                        }
                    },
                    bottomBar = {
                        if (isPrimaryTab) {
                            ZaviroBottomNav(
                                currentScreen = currentScreen,
                                onNavigate = { target ->
                                    viewModel.switchTab(target)
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
                            when (screen) {
                                is Screen.Home -> HomeScreen(
                                    viewModel = viewModel,
                                    onNavigateToMenu = { viewModel.switchTab(Screen.Menu) },
                                    onNavigateToDeals = { viewModel.switchTab(Screen.Deals) },
                                    onProductClick = { product ->
                                        viewModel.navigateTo(Screen.ProductDetail(product))
                                    },
                                    onDealClick = { deal ->
                                        viewModel.navigateTo(Screen.DealDetail(deal))
                                    }
                                )

                                is Screen.Menu -> MenuScreen(
                                    viewModel = viewModel,
                                    onProductClick = { product ->
                                        viewModel.navigateTo(Screen.ProductDetail(product))
                                    }
                                )

                                is Screen.Deals -> DealsScreen(
                                    viewModel = viewModel,
                                    onDealClick = { deal ->
                                        viewModel.navigateTo(Screen.DealDetail(deal))
                                    }
                                )

                                is Screen.ProductDetail -> ProductDetailScreen(
                                    product = screen.product,
                                    viewModel = viewModel,
                                    onBack = { viewModel.navigateBack() }
                                )

                                is Screen.DealDetail -> DealDetailScreen(
                                    deal = screen.deal,
                                    viewModel = viewModel,
                                    onBack = { viewModel.navigateBack() }
                                )

                                is Screen.Cart -> CartScreen(
                                    viewModel = viewModel,
                                    onProceedToCheckout = { viewModel.navigateTo(Screen.Checkout) },
                                    onExploreMenu = { viewModel.switchTab(Screen.Menu) }
                                )

                                is Screen.Checkout -> CheckoutScreen(
                                    viewModel = viewModel,
                                    onOrderSuccess = { placedOrder ->
                                        viewModel.navigateTo(Screen.OrderConfirmation(placedOrder))
                                    },
                                    onBack = { viewModel.navigateBack() }
                                )

                                is Screen.OrderConfirmation -> OrderConfirmationScreen(
                                    order = screen.order,
                                    onTrackOrder = { orderId ->
                                        viewModel.trackOrder(orderId)
                                    },
                                    onBackToHome = { viewModel.switchTab(Screen.Home) }
                                )

                                is Screen.OrderTracking -> OrderTrackingScreen(
                                    orderId = screen.orderId,
                                    viewModel = viewModel,
                                    onBack = { viewModel.navigateBack() }
                                )

                                is Screen.MyOrders -> MyOrdersScreen(
                                    viewModel = viewModel,
                                    onTrackOrder = { orderId ->
                                        viewModel.trackOrder(orderId)
                                    },
                                    onExploreMenu = { viewModel.switchTab(Screen.Menu) }
                                )

                                is Screen.Profile -> ProfileScreen(
                                    viewModel = viewModel,
                                    onNavigateToOrders = { viewModel.switchTab(Screen.MyOrders) },
                                    onNavigateToAdmin = { viewModel.navigateTo(Screen.AdminDashboard) }
                                )

                                is Screen.CustomerLogin -> CustomerLoginScreen(
                                    viewModel = viewModel,
                                    onBack = {
                                        if (!viewModel.navigateBack()) {
                                            viewModel.switchTab(Screen.Home)
                                        }
                                    }
                                )

                                is Screen.AdminDashboard -> AdminDashboardScreen(
                                    viewModel = viewModel,
                                    onBack = { viewModel.navigateBack() }
                                )

                                is Screen.PwaWebApp -> PwaWebScreen(
                                    viewModel = viewModel,
                                    onBack = { viewModel.navigateBack() }
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
}
