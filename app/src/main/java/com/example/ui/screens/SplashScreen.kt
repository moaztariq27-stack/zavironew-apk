package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BrandingSettings
import com.example.model.LogoSlot
import com.example.ui.components.ZaviroDynamicLogo
import com.example.ui.theme.ZaviroDarkBase
import com.example.ui.theme.ZaviroOnDarkMuted
import com.example.ui.theme.ZaviroRed
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    branding: BrandingSettings,
    isInitialSettingsLoaded: Boolean = true,
    onTimeout: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }

    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "splashAlpha"
    )

    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.92f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "splashScale"
    )

    val accentColor = remember(branding.primaryColorHex) {
        try {
            val hex = branding.primaryColorHex.trim()
            if (hex.isBlank() || hex.equals("#FFB800", ignoreCase = true)) {
                ZaviroRed
            } else {
                Color(android.graphics.Color.parseColor(hex))
            }
        } catch (e: Exception) {
            ZaviroRed
        }
    }

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    LaunchedEffect(isInitialSettingsLoaded) {
        if (isInitialSettingsLoaded) {
            delay(1400)
            onTimeout()
        }
    }

    val hasConfiguredSplashLogo = remember(branding) {
        branding.getLogoForSlot(LogoSlot.SPLASH).isNotBlank()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ZaviroDarkBase)
            .clickable { onTimeout() }
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            if (isInitialSettingsLoaded || hasConfiguredSplashLogo) {
                ZaviroDynamicLogo(
                    branding = branding,
                    slot = LogoSlot.SPLASH,
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .heightIn(max = 280.dp)
                        .scale(scaleAnim)
                        .alpha(alphaAnim)
                        .testTag("splash_zaviro_logo"),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "FRESH • HOT • AUTHENTIC TASTE",
                    color = ZaviroOnDarkMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp,
                    modifier = Modifier.alpha(alphaAnim)
                )

                Spacer(modifier = Modifier.height(28.dp))
            }

            CircularProgressIndicator(
                color = accentColor,
                strokeWidth = 2.5.dp,
                modifier = Modifier
                    .size(28.dp)
                    .alpha(alphaAnim)
            )
        }
    }
}

