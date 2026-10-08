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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.ui.components.ZaviroDynamicLogo
import com.example.ui.theme.*
import com.example.viewmodel.ZaviroViewModel

@Composable
fun AdminBrandingSection(
    viewModel: ZaviroViewModel
) {
    val context = LocalContext.current
    val currentBranding by viewModel.branding.collectAsState()

    // Working draft state kept in sync with persistent branding
    var draftBranding by remember(currentBranding) { mutableStateOf(currentBranding) }
    var selectedSlot by remember { mutableStateOf(LogoSlot.MASTER) }
    var manualUrlInput by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    // Color preset lists
    val primaryPresets = listOf(
        "#C81E2B" to "Zaviro Red",
        "#6E141D" to "Burgundy",
        "#FFB800" to "Zaviro Gold",
        "#00C853" to "Emerald",
        "#2979FF" to "Royal Blue",
        "#7C4DFF" to "Purple"
    )

    val secondaryPresets = listOf(
        "#6E141D" to "Rich Burgundy",
        "#FFD54F" to "Gold Light",
        "#FFFFFF" to "Pure White",
        "#00E5FF" to "Cyan",
        "#FF80AB" to "Pink Rose"
    )

    // Exact Master Logo uploader (applies exact unmodified file across entire app immediately)
    val masterExactLogoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val savedPath = viewModel.uploadExactLogoUri(
                context = context,
                uri = uri,
                slot = LogoSlot.MASTER,
                applyToAllSlots = true
            )
            if (savedPath != null) {
                draftBranding = viewModel.branding.value
                Toast.makeText(
                    context,
                    "Exact original Zaviro logo uploaded & saved permanently across all screens!",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(context, "Could not load selected logo file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Slot-specific Android Photo Picker launcher (saves exact unmodified bytes immediately)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val savedPath = viewModel.uploadExactLogoUri(
                context = context,
                uri = uri,
                slot = selectedSlot,
                applyToAllSlots = (selectedSlot == LogoSlot.MASTER)
            )
            if (savedPath != null) {
                draftBranding = viewModel.branding.value
                Toast.makeText(
                    context,
                    "Exact logo saved permanently for ${selectedSlot.label}!",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(context, "Could not process image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_branding_section"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header + Direct 1-Tap Original Zaviro Logo Upload
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Branding",
                        tint = ZaviroRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Branding & Original Logo",
                            color = ZaviroTextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Upload your exact original Zaviro logo (100% quality & transparency preserved)",
                            color = ZaviroTextMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        masterExactLogoLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
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
                        .testTag("upload_original_zaviro_logo_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = "Upload Original Zaviro Logo",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Upload Original Zaviro Logo (Apply Everywhere)",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Slot Selector Card
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
                    text = "1. Select Logo Placement Slot",
                    color = ZaviroTextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
                Text(
                    text = "You can set a master logo or assign custom logos per screen",
                    color = ZaviroTextMuted,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                LogoSlot.entries.forEach { slot ->
                    val isSelected = selectedSlot == slot
                    val currentSlotLogo = draftBranding.getLogoForSlot(slot)
                    val isCustom = currentSlotLogo.isNotBlank()

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) ZaviroRedTint else ZaviroBackground,
                        border = BorderStroke(1.dp, if (isSelected) ZaviroRed else ZaviroSurfaceBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedSlot = slot }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = slot.label,
                                    color = if (isSelected) ZaviroRed else ZaviroTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = slot.description,
                                    color = ZaviroTextMuted,
                                    fontSize = 11.sp
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isCustom) ZaviroGreen else ZaviroSurfaceCard,
                                border = if (!isCustom) BorderStroke(1.dp, ZaviroSurfaceBorder) else null
                            ) {
                                Text(
                                    text = if (isCustom) "Custom" else "Default",
                                    color = if (isCustom) Color.White else ZaviroTextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Upload & Preview Card for Selected Slot
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
                    text = "2. Upload Logo for: ${selectedSlot.label}",
                    color = ZaviroTextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Live Preview Box (Deep dark background so original Zaviro logo is crystal clear)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ZaviroDarkBase)
                        .border(1.dp, ZaviroDarkBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ZaviroDynamicLogo(
                        branding = draftBranding,
                        slot = selectedSlot,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons for Image Upload
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ZaviroRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_gallery_logo_btn")
                    ) {
                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick Gallery", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.resetLogoSlot(selectedSlot)
                            draftBranding = viewModel.branding.value
                            manualUrlInput = ""
                            Toast.makeText(context, "Reset to default ZAVIRO logo", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ZaviroTextSecondary),
                        border = BorderStroke(1.dp, ZaviroSurfaceBorder),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Default", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Manual URL Input Option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualUrlInput,
                        onValueChange = { manualUrlInput = it },
                        placeholder = { Text("Or paste image URL (https://...)", color = ZaviroTextMuted, fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ZaviroBackground,
                            unfocusedContainerColor = ZaviroBackground,
                            focusedBorderColor = ZaviroRed,
                            unfocusedBorderColor = ZaviroSurfaceBorder,
                            focusedTextColor = ZaviroTextPrimary,
                            unfocusedTextColor = ZaviroTextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (manualUrlInput.isNotBlank()) {
                                val trimmedUrl = manualUrlInput.trim()
                                draftBranding = when (selectedSlot) {
                                    LogoSlot.MASTER -> draftBranding.copy(
                                        masterLogoUrl = trimmedUrl,
                                        splashLogoUrl = trimmedUrl,
                                        homeHeaderLogoUrl = trimmedUrl,
                                        loginLogoUrl = trimmedUrl,
                                        adminLogoUrl = trimmedUrl
                                    )
                                    LogoSlot.SPLASH -> draftBranding.copy(splashLogoUrl = trimmedUrl)
                                    LogoSlot.HOME_HEADER -> draftBranding.copy(homeHeaderLogoUrl = trimmedUrl)
                                    LogoSlot.LOGIN -> draftBranding.copy(loginLogoUrl = trimmedUrl)
                                    LogoSlot.ADMIN -> draftBranding.copy(adminLogoUrl = trimmedUrl)
                                }
                                viewModel.saveBranding(draftBranding)
                                Toast.makeText(context, "Logo URL applied & saved!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ZaviroBurgundy,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Apply", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Brand Identity (Name & Tagline)
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
                    text = "3. Brand Identity",
                    color = ZaviroTextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = draftBranding.brandName,
                    onValueChange = {
                        draftBranding = draftBranding.copy(brandName = it)
                        viewModel.saveBranding(draftBranding)
                    },
                    label = { Text("App / Brand Name", color = ZaviroTextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ZaviroBackground,
                        unfocusedContainerColor = ZaviroBackground,
                        focusedBorderColor = ZaviroRed,
                        unfocusedBorderColor = ZaviroSurfaceBorder,
                        focusedTextColor = ZaviroTextPrimary,
                        unfocusedTextColor = ZaviroTextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = draftBranding.tagline,
                    onValueChange = {
                        draftBranding = draftBranding.copy(tagline = it)
                        viewModel.saveBranding(draftBranding)
                    },
                    label = { Text("Brand Tagline", color = ZaviroTextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ZaviroBackground,
                        unfocusedContainerColor = ZaviroBackground,
                        focusedBorderColor = ZaviroRed,
                        unfocusedBorderColor = ZaviroSurfaceBorder,
                        focusedTextColor = ZaviroTextPrimary,
                        unfocusedTextColor = ZaviroTextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Theme Colors
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
                    text = "4. Brand Colors",
                    color = ZaviroTextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
                Text(
                    text = "Customize the primary accent and secondary accent colors",
                    color = ZaviroTextMuted,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Primary Color
                Text(
                    text = "Primary Accent Color: ${draftBranding.primaryColorHex}",
                    color = ZaviroTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    primaryPresets.forEach { (hex, _) ->
                        val isSelected = draftBranding.primaryColorHex.equals(hex, ignoreCase = true)
                        val color = try {
                            Color(android.graphics.Color.parseColor(hex))
                        } catch (e: Exception) {
                            ZaviroRed
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) ZaviroTextPrimary else ZaviroSurfaceBorder,
                                    shape = CircleShape
                                )
                                .clickable {
                                    draftBranding = draftBranding.copy(primaryColorHex = hex)
                                    viewModel.saveBranding(draftBranding)
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Secondary Color
                Text(
                    text = "Secondary Accent Color: ${draftBranding.secondaryColorHex}",
                    color = ZaviroTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    secondaryPresets.forEach { (hex, _) ->
                        val isSelected = draftBranding.secondaryColorHex.equals(hex, ignoreCase = true)
                        val color = try {
                            Color(android.graphics.Color.parseColor(hex))
                        } catch (e: Exception) {
                            ZaviroBurgundy
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) ZaviroTextPrimary else ZaviroSurfaceBorder,
                                    shape = CircleShape
                                )
                                .clickable {
                                    draftBranding = draftBranding.copy(secondaryColorHex = hex)
                                    viewModel.saveBranding(draftBranding)
                                }
                        )
                    }
                }
            }
        }

        // Save Button
        Button(
            onClick = {
                isSaving = true
                viewModel.saveBranding(draftBranding)
                isSaving = false
                Toast.makeText(context, "Branding settings saved permanently!", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = ZaviroRed,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("save_branding_btn")
        ) {
            Icon(imageVector = Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isSaving) "Saving..." else "Save All Branding Settings",
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
