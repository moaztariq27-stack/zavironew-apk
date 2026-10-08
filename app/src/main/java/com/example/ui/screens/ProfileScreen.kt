package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LogoSlot
import com.example.model.SavedAddress
import com.example.ui.components.ZaviroCustomerAvatar
import com.example.ui.components.ZaviroDynamicLogo
import com.example.ui.theme.*
import com.example.viewmodel.Screen
import com.example.viewmodel.ZaviroViewModel

@Composable
fun ProfileScreen(
    viewModel: ZaviroViewModel,
    onNavigateToOrders: () -> Unit,
    onNavigateToAdmin: () -> Unit
) {
    val context = LocalContext.current
    val customerName by viewModel.customerName.collectAsState()
    val customerPhone by viewModel.customerPhone.collectAsState()
    val deliveryAddress by viewModel.deliveryAddress.collectAsState()
    val customerProfileImageUrl by viewModel.customerProfileImageUrl.collectAsState()
    val savedAddresses by viewModel.savedAddresses.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val branding by viewModel.branding.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAddAddressDialog by remember { mutableStateOf(false) }

    val profileAvatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val savedPath = viewModel.uploadCustomerProfileImageUri(context, uri)
            if (savedPath != null) {
                Toast.makeText(context, "Profile picture updated & saved!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Could not load selected image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ZaviroBackground)
            .testTag("profile_screen")
    ) {
        // Deep Dark Brand Header
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
                        text = "Customer Profile",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )
                    Text(
                        text = "${branding.brandName} Member • ${branding.tagline}",
                        color = Color(0xFFD6CECE),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Surface(
                    color = ZaviroRed,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "MEMBER",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        letterSpacing = 0.6.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Brand Logo Card (Deep dark showcase frame so original logo looks crisp)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(3.dp, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = ZaviroDarkBase),
                border = BorderStroke(1.dp, ZaviroDarkBorder),
                shape = RoundedCornerShape(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ZaviroDynamicLogo(
                        branding = branding,
                        slot = LogoSlot.MASTER,
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .height(125.dp)
                            .testTag("profile_zaviro_logo"),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // Admin Portal Access Card (Immediately accessible at top of Profile screen)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(3.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onNavigateToAdmin() }
                    .testTag("profile_admin_portal_card"),
                colors = CardDefaults.cardColors(containerColor = ZaviroDarkSurface),
                border = BorderStroke(1.dp, ZaviroRed.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ZaviroRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Store Admin Portal",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Manage products, deals, prices & live orders",
                                color = Color(0xFFD6CECE),
                                fontSize = 12.sp
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Secure",
                        tint = Color.White
                    )
                }
            }

            // Customer Authentication Card (Phone Number SMS OTP & Continue with Google)
            CustomerAuthCard(
                viewModel = viewModel
            )

            // Customer Info Card
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clickable {
                                        profileAvatarPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                    .testTag("profile_avatar_box"),
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(ZaviroRedTint)
                                        .border(1.5.dp, ZaviroRed, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ZaviroCustomerAvatar(
                                        profileImageUrl = customerProfileImageUrl,
                                        contentDescription = "Customer Avatar",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(ZaviroRed)
                                        .border(1.5.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Change Profile Photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = customerName.ifBlank { "Zaviro Guest" },
                                    color = ZaviroTextPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    modifier = Modifier.testTag("profile_customer_name_text")
                                )
                                Text(
                                    text = customerPhone.ifBlank { "No phone set" },
                                    color = ZaviroRed,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ZaviroRedTint,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showEditProfileDialog = true }
                                .testTag("edit_profile_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Profile",
                                    tint = ZaviroRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Edit",
                                    color = ZaviroRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = ZaviroSurfaceBorder)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Default Address",
                            tint = ZaviroRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = deliveryAddress.ifBlank { "No default address set" },
                            color = ZaviroTextSecondary,
                            fontSize = 13.sp,
                            maxLines = 2
                        )
                    }
                }
            }

            // Saved Addresses Card
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
                        Text(
                            text = "Saved Delivery Addresses",
                            color = ZaviroTextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )

                        TextButton(onClick = { showAddAddressDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                tint = ZaviroRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add", color = ZaviroRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    savedAddresses.forEach { addr ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ZaviroBackground,
                            border = BorderStroke(1.dp, ZaviroSurfaceBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.updateDeliveryAddress("${addr.fullAddress}, ${addr.area}")
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${addr.label} • ${addr.area}",
                                        color = ZaviroRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = addr.fullAddress,
                                        color = ZaviroTextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.repository.removeAddress(addr.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = ZaviroTextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Orders Shortcut
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onNavigateToOrders() },
                colors = CardDefaults.cardColors(containerColor = ZaviroSurfaceCard),
                border = BorderStroke(1.dp, ZaviroSurfaceBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ZaviroRedTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = "Orders",
                                tint = ZaviroRed,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Order History & Tracking",
                                color = ZaviroTextPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "View live food status and previous invoices",
                                color = ZaviroTextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Go",
                        tint = ZaviroTextSecondary
                    )
                }
            }

            // Support & Contact Card
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
                        text = "Customer Support & Contact",
                        color = ZaviroTextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Have questions or need to customize an order?",
                        color = ZaviroTextMuted,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // WhatsApp Row
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ZaviroWhatsApp.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, ZaviroWhatsApp.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openWhatsApp(context) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "WhatsApp",
                                tint = ZaviroWhatsApp,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "WhatsApp Chat",
                                    color = ZaviroTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = settings.whatsappNumber,
                                    color = ZaviroWhatsApp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                text = "Open →",
                                color = ZaviroWhatsApp,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Call Row
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ZaviroBackground,
                        border = BorderStroke(1.dp, ZaviroSurfaceBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.callZaviro(context) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Call",
                                tint = ZaviroRed,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Direct Call Support",
                                    color = ZaviroTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = settings.whatsappNumber,
                                    color = ZaviroTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                text = "Call →",
                                color = ZaviroRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Instagram Row
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ZaviroBackground,
                        border = BorderStroke(1.dp, ZaviroSurfaceBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openInstagram(context) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Instagram",
                                tint = ZaviroInstagram,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Official Instagram",
                                    color = ZaviroTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = settings.instagramHandle,
                                    color = ZaviroInstagram,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                text = "Follow →",
                                color = ZaviroInstagram,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Web App & Installable PWA Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { viewModel.navigateTo(Screen.PwaWebApp) }
                    .testTag("profile_pwa_card"),
                colors = CardDefaults.cardColors(containerColor = ZaviroSurfaceCard),
                border = BorderStroke(1.dp, ZaviroSurfaceBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ZaviroRedTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.InstallMobile,
                                contentDescription = "PWA",
                                tint = ZaviroRed,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Web App & Installable PWA",
                                color = ZaviroTextPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Full-screen browser app for Android & iPhone (iOS)",
                                color = ZaviroTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Text(
                        text = "Open →",
                        color = ZaviroRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(70.dp))
        }
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        var tempName by remember { mutableStateOf(customerName) }
        var tempPhone by remember { mutableStateOf(customerPhone) }
        var tempAddress by remember { mutableStateOf(deliveryAddress) }
        var tempProfileImage by remember { mutableStateOf(customerProfileImageUrl) }

        val dialogAvatarLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                val savedPath = viewModel.uploadCustomerProfileImageUri(context, uri)
                if (savedPath != null) {
                    tempProfileImage = savedPath
                    Toast.makeText(context, "Profile image selected!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            containerColor = ZaviroSurfaceCard,
            shape = RoundedCornerShape(18.dp),
            title = {
                Text(
                    text = "Edit Profile Details",
                    color = ZaviroTextPrimary,
                    fontWeight = FontWeight.Black
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Photo Picker inside dialog
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(ZaviroRedTint)
                                .border(1.5.dp, ZaviroRed, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            ZaviroCustomerAvatar(
                                profileImageUrl = tempProfileImage,
                                contentDescription = "Avatar Preview",
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                dialogAvatarLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ZaviroRed),
                            border = BorderStroke(1.dp, ZaviroRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("change_profile_image_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Change Profile Photo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Name", color = ZaviroTextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ZaviroTextPrimary,
                            unfocusedTextColor = ZaviroTextPrimary,
                            focusedBorderColor = ZaviroRed,
                            unfocusedBorderColor = ZaviroSurfaceBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_profile_name_input")
                    )
                    OutlinedTextField(
                        value = tempPhone,
                        onValueChange = { tempPhone = it },
                        label = { Text("Phone", color = ZaviroTextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ZaviroTextPrimary,
                            unfocusedTextColor = ZaviroTextPrimary,
                            focusedBorderColor = ZaviroRed,
                            unfocusedBorderColor = ZaviroSurfaceBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_profile_phone_input")
                    )
                    OutlinedTextField(
                        value = tempAddress,
                        onValueChange = { tempAddress = it },
                        label = { Text("Delivery Address", color = ZaviroTextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ZaviroTextPrimary,
                            unfocusedTextColor = ZaviroTextPrimary,
                            focusedBorderColor = ZaviroRed,
                            unfocusedBorderColor = ZaviroSurfaceBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_profile_address_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateCustomerProfile(
                            name = tempName.trim(),
                            phone = tempPhone.trim(),
                            address = tempAddress.trim(),
                            profileImageUrl = tempProfileImage
                        )
                        Toast.makeText(context, "Profile saved permanently!", Toast.LENGTH_SHORT).show()
                        showEditProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZaviroRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("save_profile_btn")
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = ZaviroTextSecondary)
                }
            }
        )
    }

    // Add Address Dialog
    if (showAddAddressDialog) {
        var label by remember { mutableStateOf("Home") }
        var fullAddress by remember { mutableStateOf("") }
        var area by remember { mutableStateOf("Lahore") }

        AlertDialog(
            onDismissRequest = { showAddAddressDialog = false },
            containerColor = ZaviroSurfaceCard,
            shape = RoundedCornerShape(18.dp),
            title = {
                Text(
                    text = "Add Delivery Address",
                    color = ZaviroTextPrimary,
                    fontWeight = FontWeight.Black
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Label (Home, Office, Other)", color = ZaviroTextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ZaviroTextPrimary,
                            unfocusedTextColor = ZaviroTextPrimary,
                            focusedBorderColor = ZaviroRed,
                            unfocusedBorderColor = ZaviroSurfaceBorder
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = area,
                        onValueChange = { area = it },
                        label = { Text("Area / Sector (e.g. Gulberg, DHA, Bahria)", color = ZaviroTextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ZaviroTextPrimary,
                            unfocusedTextColor = ZaviroTextPrimary,
                            focusedBorderColor = ZaviroRed,
                            unfocusedBorderColor = ZaviroSurfaceBorder
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = fullAddress,
                        onValueChange = { fullAddress = it },
                        label = { Text("Complete Address", color = ZaviroTextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ZaviroTextPrimary,
                            unfocusedTextColor = ZaviroTextPrimary,
                            focusedBorderColor = ZaviroRed,
                            unfocusedBorderColor = ZaviroSurfaceBorder
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (fullAddress.isNotBlank()) {
                            viewModel.repository.addAddress(
                                SavedAddress(
                                    label = label,
                                    fullAddress = fullAddress,
                                    area = area
                                )
                            )
                            showAddAddressDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZaviroRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Add Address", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAddressDialog = false }) {
                    Text("Cancel", color = ZaviroTextSecondary)
                }
            }
        )
    }
}
