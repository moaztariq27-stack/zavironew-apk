package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.Screen

@Composable
fun ZaviroBottomNav(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = ZaviroSurfaceBorder, thickness = 1.dp)
        NavigationBar(
            containerColor = ZaviroSurfaceCard,
            tonalElevation = 0.dp,
            modifier = Modifier.testTag("bottom_nav_bar")
        ) {
            val isHome = currentScreen is Screen.Home
            val isMenu = currentScreen is Screen.Menu
            val isDeals = currentScreen is Screen.Deals
            val isOrders = currentScreen is Screen.MyOrders
            val isProfile = currentScreen is Screen.Profile

            val itemColors = NavigationBarItemDefaults.colors(
                selectedIconColor = ZaviroRed,
                selectedTextColor = ZaviroRed,
                unselectedIconColor = ZaviroTextMuted,
                unselectedTextColor = ZaviroTextMuted,
                indicatorColor = ZaviroBurgundySoft
            )

            NavigationBarItem(
                selected = isHome,
                onClick = { onNavigate(Screen.Home) },
                icon = {
                    Icon(
                        imageVector = if (isHome) Icons.Filled.Home else Icons.Outlined.Home,
                        contentDescription = "Home",
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = "Home",
                        fontSize = 11.sp,
                        fontWeight = if (isHome) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = itemColors,
                modifier = Modifier.testTag("nav_item_home")
            )

            NavigationBarItem(
                selected = isMenu,
                onClick = { onNavigate(Screen.Menu) },
                icon = {
                    Icon(
                        imageVector = if (isMenu) Icons.Filled.RestaurantMenu else Icons.Outlined.RestaurantMenu,
                        contentDescription = "Menu",
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = "Menu",
                        fontSize = 11.sp,
                        fontWeight = if (isMenu) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = itemColors,
                modifier = Modifier.testTag("nav_item_menu")
            )

            NavigationBarItem(
                selected = isDeals,
                onClick = { onNavigate(Screen.Deals) },
                icon = {
                    BadgedBox(
                        badge = {
                            Badge(
                                containerColor = ZaviroRed,
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = "15",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isDeals) Icons.Filled.LocalOffer else Icons.Outlined.LocalOffer,
                            contentDescription = "Deals",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text = "Deals",
                        fontSize = 11.sp,
                        fontWeight = if (isDeals) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = itemColors,
                modifier = Modifier.testTag("nav_item_deals")
            )

            NavigationBarItem(
                selected = isOrders,
                onClick = { onNavigate(Screen.MyOrders) },
                icon = {
                    Icon(
                        imageVector = if (isOrders) Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong,
                        contentDescription = "Orders",
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = "Orders",
                        fontSize = 11.sp,
                        fontWeight = if (isOrders) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = itemColors,
                modifier = Modifier.testTag("nav_item_orders")
            )

            NavigationBarItem(
                selected = isProfile,
                onClick = { onNavigate(Screen.Profile) },
                icon = {
                    Icon(
                        imageVector = if (isProfile) Icons.Filled.Person else Icons.Outlined.Person,
                        contentDescription = "Profile",
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = "Profile",
                        fontSize = 11.sp,
                        fontWeight = if (isProfile) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = itemColors,
                modifier = Modifier.testTag("nav_item_profile")
            )
        }
    }
}

