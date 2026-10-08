package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.BrandingSettings
import com.example.model.LogoSlot
import com.example.ui.theme.ZaviroGold
import java.io.File

fun decodeExactBitmapFromSource(
    source: String,
    filesDir: File? = null,
    slot: LogoSlot? = null
): Bitmap? {
    val trimmed = source.trim()
    val opts = BitmapFactory.Options().apply {
        inScaled = false
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }

    if (trimmed.isBlank()) {
        if (filesDir != null) {
            if (slot != null) {
                val slotFile = File(filesDir, "zaviro_logo_${slot.name.lowercase()}.png")
                if (slotFile.exists() && slotFile.length() > 0) {
                    try {
                        val bmp = BitmapFactory.decodeFile(slotFile.absolutePath, opts)
                        if (bmp != null) return bmp
                    } catch (_: Exception) {}
                }
            }
            val savedMaster = File(filesDir, "zaviro_exact_uploaded_logo.png")
            if (savedMaster.exists() && savedMaster.length() > 0) {
                return try {
                    BitmapFactory.decodeFile(savedMaster.absolutePath, opts)
                } catch (_: Exception) {
                    null
                }
            }
        }
        return null
    }

    // 1. Check local file path (file:// or absolute path)
    if (trimmed.startsWith("file://") || trimmed.startsWith("/")) {
        val filePath = trimmed.removePrefix("file://")
        val f = File(filePath)
        if (f.exists() && f.length() > 0) {
            try {
                val bmp = BitmapFactory.decodeFile(f.absolutePath, opts)
                if (bmp != null) return bmp
            } catch (_: Exception) {}
        }
    }

    // 2. Check Base64 data URI or raw Base64
    if (trimmed.startsWith("data:image") || (!trimmed.startsWith("http") && !trimmed.startsWith("content:") && !trimmed.startsWith("file:") && trimmed.length > 60)) {
        val base64Data = if (trimmed.contains(",")) {
            trimmed.substringAfter(",")
        } else {
            trimmed
        }
        try {
            val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
            val bmp = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size, opts)
            if (bmp != null) return bmp
        } catch (_: Exception) {}
    }

    return null
}

@Composable
fun ZaviroDynamicLogo(
    branding: BrandingSettings,
    slot: LogoSlot = LogoSlot.MASTER,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current
    val logoSource = remember(branding, slot) {
        branding.getLogoForSlot(slot).trim()
    }

    val exactBitmap = remember(logoSource, branding.updatedAt, slot) {
        decodeExactBitmapFromSource(logoSource, context.filesDir, slot) ?: try {
            val opts = BitmapFactory.Options().apply {
                inScaled = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            if (logoSource.isBlank()) {
                BitmapFactory.decodeResource(context.resources, R.drawable.zaviro_logo, opts)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    if (exactBitmap != null) {
        Image(
            bitmap = exactBitmap.asImageBitmap(),
            contentDescription = branding.brandName,
            modifier = modifier,
            contentScale = contentScale,
            filterQuality = FilterQuality.High
        )
    } else if (logoSource.isBlank()) {
        Image(
            painter = painterResource(id = R.drawable.zaviro_logo),
            contentDescription = branding.brandName,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(logoSource)
                .crossfade(false)
                .build(),
            placeholder = painterResource(id = R.drawable.zaviro_logo),
            error = painterResource(id = R.drawable.zaviro_logo),
            fallback = painterResource(id = R.drawable.zaviro_logo),
            contentDescription = branding.brandName,
            modifier = modifier,
            contentScale = contentScale,
            filterQuality = FilterQuality.High
        )
    }
}

@Composable
fun ZaviroCustomerAvatar(
    profileImageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val source = remember(profileImageUrl) { profileImageUrl.trim() }
    val decodedBitmap = remember(source) {
        if (source.isBlank()) {
            val f = File(context.filesDir, "customer_profile_avatar.png")
            if (f.exists() && f.length() > 0) {
                decodeExactBitmapFromSource("file://${f.absolutePath}", null)
            } else null
        } else {
            decodeExactBitmapFromSource(source, null)
        }
    }

    if (decodedBitmap != null) {
        Image(
            bitmap = decodedBitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier.fillMaxSize(),
            contentScale = contentScale,
            filterQuality = FilterQuality.High
        )
    } else if (source.isBlank()) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = contentDescription,
            tint = ZaviroGold,
            modifier = Modifier.size(28.dp)
        )
    } else {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(source)
                .crossfade(false)
                .build(),
            contentDescription = contentDescription,
            modifier = modifier.fillMaxSize(),
            contentScale = contentScale,
            filterQuality = FilterQuality.High
        )
    }
}

@Composable
fun ZaviroFoodImage(
    imageUrl: String,
    fallbackDrawableRes: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val safeFallback = if (fallbackDrawableRes != 0) fallbackDrawableRes else R.drawable.zaviro_creamy_pasta
    val source = remember(imageUrl) { imageUrl.trim() }

    val decodedBitmap = remember(source) {
        if (source.isBlank()) null else decodeExactBitmapFromSource(source, null)
    }

    if (source.isBlank()) {
        Image(
            painter = painterResource(id = safeFallback),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else if (decodedBitmap != null) {
        Image(
            bitmap = decodedBitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            filterQuality = FilterQuality.High
        )
    } else {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(source)
                .crossfade(false)
                .build(),
            placeholder = painterResource(id = safeFallback),
            error = painterResource(id = safeFallback),
            fallback = painterResource(id = safeFallback),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}
