package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ZaviroBrandColorScheme = lightColorScheme(
    primary = ZaviroRed,
    onPrimary = Color.White,
    primaryContainer = ZaviroGoldContainer,
    onPrimaryContainer = ZaviroBurgundy,
    secondary = ZaviroBurgundy,
    onSecondary = Color.White,
    secondaryContainer = ZaviroSurfaceDark,
    onSecondaryContainer = ZaviroTextWhite,
    background = ZaviroBlack,
    onBackground = ZaviroTextWhite,
    surface = ZaviroSurfaceCard,
    onSurface = ZaviroTextWhite,
    surfaceVariant = ZaviroSurfaceDark,
    onSurfaceVariant = ZaviroTextSecondary,
    outline = ZaviroSurfaceBorder,
    error = ZaviroRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    primaryColorHex: String = "#C81E2B",
    secondaryColorHex: String = "#8E141E",
    content: @Composable () -> Unit,
) {
    val resolvedPrimary = try {
        val hex = primaryColorHex.trim()
        if (hex.isBlank() || hex.equals("#FFB800", ignoreCase = true)) {
            ZaviroRed
        } else {
            Color(android.graphics.Color.parseColor(hex))
        }
    } catch (_: Exception) {
        ZaviroRed
    }
    val resolvedSecondary = try {
        val hex = secondaryColorHex.trim()
        if (hex.isBlank() || hex.equals("#FFD54F", ignoreCase = true)) {
            ZaviroBurgundy
        } else {
            Color(android.graphics.Color.parseColor(hex))
        }
    } catch (_: Exception) {
        ZaviroBurgundy
    }
    val colorScheme = ZaviroBrandColorScheme.copy(
        primary = resolvedPrimary,
        secondary = resolvedSecondary,
        onPrimaryContainer = resolvedSecondary
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

