package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BrandingSettings
import com.example.model.LogoSlot
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZaviroTopBar(
    title: String? = null,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    cartItemCount: Int = 0,
    branding: BrandingSettings = BrandingSettings(),
    onCartClick: () -> Unit = {},
    onAdminClick: () -> Unit = {},
    onPwaClick: () -> Unit = {},
    onLogoClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ZaviroDarkBase)
    ) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = ZaviroDarkBase,
                titleContentColor = ZaviroOnDarkPrimary,
                navigationIconContentColor = ZaviroOnDarkPrimary,
                actionIconContentColor = ZaviroOnDarkPrimary
            ),
            navigationIcon = {
                if (showBackButton) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ZaviroDarkElevated)
                            .testTag("nav_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ZaviroOnDarkPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .padding(start = 14.dp, top = 4.dp, bottom = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onLogoClick() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ZaviroDynamicLogo(
                            branding = branding,
                            slot = LogoSlot.MASTER,
                            modifier = Modifier
                                .height(42.dp)
                                .width(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .testTag("topbar_zaviro_logo"),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            },
            title = {
                if (showBackButton && title != null) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ZaviroOnDarkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier.padding(start = 6.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = ZaviroRed,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "DELIVERING HOT & FRESH",
                                color = ZaviroOnDarkMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = "Lahore, Pakistan",
                            color = ZaviroOnDarkPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            actions = {
                // PWA / Web App button
                IconButton(
                    onClick = onPwaClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ZaviroDarkElevated)
                        .testTag("topbar_pwa_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.InstallMobile,
                        contentDescription = "Web App & PWA",
                        tint = ZaviroOnDarkSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Admin portal icon
                IconButton(
                    onClick = onAdminClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ZaviroDarkElevated)
                        .testTag("topbar_admin_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Admin Portal",
                        tint = ZaviroOnDarkSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Cart icon with badge
                Box(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (cartItemCount > 0) ZaviroRed else ZaviroDarkElevated)
                        .border(
                            width = 1.dp,
                            color = if (cartItemCount > 0) ZaviroRed else ZaviroDarkBorder,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { onCartClick() }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = "Cart",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                        if (cartItemCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = Color.White
                            ) {
                                Text(
                                    text = "$cartItemCount",
                                    color = ZaviroRed,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        )
        HorizontalDivider(
            color = ZaviroDarkBorder,
            thickness = 1.dp
        )
    }
}

