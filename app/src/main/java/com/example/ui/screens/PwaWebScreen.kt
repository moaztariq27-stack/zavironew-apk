package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.LogoSlot
import com.example.ui.components.ZaviroDynamicLogo
import com.example.ui.theme.*
import com.example.viewmodel.Screen
import com.example.viewmodel.ZaviroViewModel

@Composable
fun PwaWebScreen(
    viewModel: ZaviroViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val branding by viewModel.branding.collectAsState()
    val deals by viewModel.deals.collectAsState()
    val products by viewModel.products.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Interactive Device Preview, 1: Install & Deploy Guide
    var selectedDevice by remember { mutableStateOf("iPhone") } // "Android" or "iPhone"
    var isInstalledSimulation by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ZaviroBackground)
            .testTag("pwa_web_screen")
    ) {
        // Header Bar
        Surface(
            color = ZaviroDarkBase,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Column {
                            Text(
                                text = "${branding.brandName} Web App & PWA",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Android Chrome & iPhone Safari Full-Screen Ready",
                                color = Color(0xFFD6CECE),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Surface(
                        color = ZaviroRed,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isInstalledSimulation) "INSTALLED" else "PWA READY",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = ZaviroSurfaceCard,
                    contentColor = ZaviroRed,
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                height = 3.dp,
                                color = ZaviroRed
                            )
                        }
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        selectedContentColor = ZaviroRed,
                        unselectedContentColor = ZaviroTextSecondary,
                        text = { Text("Mobile Browser & PWA Preview", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        selectedContentColor = ZaviroRed,
                        unselectedContentColor = ZaviroTextSecondary,
                        text = { Text("Android & iPhone Install Guide", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                }
            }
        }

        if (selectedTab == 0) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Device Selector Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = selectedDevice == "iPhone",
                        onClick = { selectedDevice = "iPhone" },
                        label = { Text("iPhone Safari PWA", fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.PhoneIphone,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ZaviroRed,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            containerColor = ZaviroSurfaceCard,
                            labelColor = ZaviroTextPrimary,
                            iconColor = ZaviroRed
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = selectedDevice == "Android",
                        onClick = { selectedDevice = "Android" },
                        label = { Text("Android Chrome PWA", fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ZaviroRed,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            containerColor = ZaviroSurfaceCard,
                            labelColor = ZaviroTextPrimary,
                            iconColor = ZaviroRed
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Simulated Full-Screen Mobile PWA Frame
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(28.dp))
                        .border(
                            width = 2.dp,
                            color = ZaviroDarkBase,
                            shape = RoundedCornerShape(28.dp)
                        ),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = ZaviroSurfaceCard)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Simulated OS / Browser Status Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ZaviroDarkBase)
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedDevice == "iPhone") "9:41  •  iPhone Full-Screen PWA" else "9:41  •  Android Standalone PWA",
                                color = Color(0xFFD6CECE),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(ZaviroGreen)
                                )
                                Text(
                                    text = "HTTPS • Live Sync",
                                    color = ZaviroGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // PWA Install Banner inside frame
                        if (!isInstalledSimulation) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ZaviroRedTint)
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Install ${branding.brandName} Web App",
                                        color = ZaviroRed,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = if (selectedDevice == "iPhone")
                                            "iOS Safari: Add to Home Screen for full-screen mode"
                                        else
                                            "Android Chrome: Install standalone app to home screen",
                                        color = ZaviroTextPrimary,
                                        fontSize = 11.sp
                                    )
                                }
                                Button(
                                    onClick = {
                                        isInstalledSimulation = true
                                        Toast.makeText(
                                            context,
                                            "${branding.brandName} PWA installed in full-screen standalone mode!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ZaviroRed,
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Install App", fontWeight = FontWeight.Black, fontSize = 11.sp)
                                }
                            }
                        }

                        // PWA Hero Section
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.zaviro_hero_banner),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                ZaviroDarkBase.copy(alpha = 0.65f),
                                                ZaviroDarkBase.copy(alpha = 0.95f)
                                            )
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                ZaviroDynamicLogo(
                                    branding = branding,
                                    slot = LogoSlot.HOME_HEADER,
                                    modifier = Modifier
                                        .fillMaxWidth(0.68f)
                                        .height(105.dp),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${branding.brandName} • ${branding.tagline}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Quick PWA Navigation Actions inside frame
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Live Responsive PWA Modules (${products.size} Items • ${deals.size} Deals)",
                                color = ZaviroTextPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.switchTab(Screen.Menu) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ZaviroRed,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Open Menu", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.switchTab(Screen.Deals) },
                                    border = BorderStroke(1.dp, ZaviroRed),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ZaviroRed),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("15 Value Deals", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.navigateTo(Screen.Cart) },
                                    border = BorderStroke(1.dp, ZaviroSurfaceBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ZaviroTextPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cart & Checkout", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.navigateTo(Screen.AdminDashboard) },
                                    border = BorderStroke(1.dp, ZaviroSurfaceBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ZaviroTextPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Admin & Branding", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        } else {
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Devices,
                                contentDescription = null,
                                tint = ZaviroRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Installable Full-Stack PWA",
                                color = ZaviroTextPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "The complete ZAVIRO Progressive Web App (/pwa/index.html, manifest.json, sw.js, and firebase.json) is ready for Android Chrome and iPhone Safari with real-time Firebase Firestore synchronization.",
                            color = ZaviroTextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = ZaviroRed,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Install on Android (Chrome / Edge / Samsung)",
                                color = ZaviroRed,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("1. Open the ZAVIRO Web App in Chrome on Android.", color = ZaviroTextPrimary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("2. Tap 'Install App' in the top banner or tap the browser menu (⋮) → 'Install app'.", color = ZaviroTextPrimary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("3. Launch ZAVIRO from your Home Screen in full-screen standalone mode.", color = ZaviroTextPrimary, fontSize = 13.sp)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PhoneIphone,
                                contentDescription = null,
                                tint = ZaviroRed,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Install on iPhone & iPad (iOS Safari)",
                                color = ZaviroRed,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("1. Open the ZAVIRO Web App in Safari on your iPhone.", color = ZaviroTextPrimary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("2. Tap the Share button (square with upward arrow) in the bottom Safari bar.", color = ZaviroTextPrimary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("3. Scroll down and select 'Add to Home Screen', then tap 'Add'.", color = ZaviroTextPrimary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("4. Launch ZAVIRO from your iPhone Home Screen with black-translucent status bar and safe-area support.", color = ZaviroTextPrimary, fontSize = 13.sp)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = ZaviroRed,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Google Cloud Run & Web App Deployment",
                                color = ZaviroRed,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Root Web App & Cloud Run files (/index.html, /server.js, /package.json, /Dockerfile, /manifest.json, /sw.js, /firebase.json) are configured at the project root.",
                            color = ZaviroTextPrimary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• To use AI Studio's Web Publish (Cloud Run) button: Export ZIP or Push to GitHub from the top-right Settings menu, then import into a New Web App in Google AI Studio.\n• Or deploy directly to Google Cloud Run / Firebase Hosting using the commands below:",
                            color = ZaviroTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(
                                    ClipData.newPlainText(
                                        "Cloud Run Deploy",
                                        "gcloud run deploy zaviro-web --source . --region asia-southeast1 --allow-unauthenticated --project gen-lang-client-0737867339"
                                    )
                                )
                                Toast.makeText(context, "Cloud Run deploy command copied!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ZaviroRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copy Google Cloud Run Deploy Command", fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("PWA Deploy", "firebase deploy --only hosting --project gen-lang-client-0737867339"))
                        Toast.makeText(context, "Firebase Hosting deploy command copied!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ZaviroBurgundy
                    ),
                    border = BorderStroke(1.dp, ZaviroBurgundy),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy Firebase Hosting Deploy Command", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
