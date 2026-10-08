package com.example.model

data class CustomerProfile(
    val uid: String = "",
    val name: String = "Muhammad Ahmed",
    val phone: String = "0304-1234567",
    val email: String = "",
    val address: String = "House 12-A, Gulberg III, Lahore",
    val notes: String = "",
    val profileImageUrl: String = "",
    val authProvider: String = "",
    val isAuthenticated: Boolean = false,
    val updatedAt: Long = 0L
)

data class SavedAddress(
    val id: String = "",
    val label: String = "Home", // Home, Office, Other
    val fullAddress: String = "",
    val landmark: String = "",
    val area: String = "Lahore",
    val phone: String = ""
)

data class AppSettings(
    val deliveryFee: Int = 150,
    val minOrderAmount: Int = 300,
    val isStoreOpen: Boolean = true,
    val announcement: String = "Welcome to ZAVIRO - GHAR SE GHAR TAK! Serving Hot & Fresh Across Lahore.",
    val whatsappNumber: String = "0304-4494742",
    val formattedWhatsappForUrl: String = computeFormattedWhatsapp(whatsappNumber),
    val instagramHandle: String = "@zaviropakistan",
    val deliveryAreas: String = "Lahore & Surrounding Areas",
    val updatedAt: Long = 0L
) {
    companion object {
        fun computeFormattedWhatsapp(rawPhone: String): String {
            val digits = rawPhone.filter { it.isDigit() }
            return when {
                digits.startsWith("92") && digits.length >= 11 -> digits
                digits.startsWith("0") && digits.length >= 10 -> "92" + digits.substring(1)
                digits.isNotEmpty() -> digits
                else -> "923044494742"
            }
        }
    }
}

enum class LogoSlot(val label: String, val description: String) {
    MASTER("Master / Default Logo", "Used everywhere unless an individual slot logo is specified"),
    SPLASH("Splash Screen", "Displayed on the app launch screen"),
    HOME_HEADER("Home Header", "Displayed in the Home Screen hero section"),
    LOGIN("Login Screen", "Displayed on the Admin authentication portal"),
    ADMIN("Admin Dashboard", "Displayed in the Admin Dashboard header & branding")
}

data class BrandingSettings(
    val brandName: String = "ZAVIRO",
    val tagline: String = "GHAR SE GHAR TAK",
    val primaryColorHex: String = "#FFB800",
    val secondaryColorHex: String = "#FFD54F",
    val masterLogoUrl: String = "",
    val splashLogoUrl: String = "",
    val homeHeaderLogoUrl: String = "",
    val loginLogoUrl: String = "",
    val adminLogoUrl: String = "",
    val updatedAt: Long = 0L
) {
    fun getLogoForSlot(slot: LogoSlot): String {
        val specific = when (slot) {
            LogoSlot.MASTER -> masterLogoUrl
            LogoSlot.SPLASH -> splashLogoUrl
            LogoSlot.HOME_HEADER -> homeHeaderLogoUrl
            LogoSlot.LOGIN -> loginLogoUrl
            LogoSlot.ADMIN -> adminLogoUrl
        }
        return listOf(
            specific,
            masterLogoUrl,
            homeHeaderLogoUrl,
            splashLogoUrl,
            loginLogoUrl,
            adminLogoUrl
        ).firstOrNull { isUsableLogoSource(it) }?.trim() ?: ""
    }

    fun hasAnyCustomLogo(): Boolean {
        return listOf(
            masterLogoUrl,
            splashLogoUrl,
            homeHeaderLogoUrl,
            loginLogoUrl,
            adminLogoUrl
        ).any { isUsableLogoSource(it) }
    }

    companion object {
        fun isUsableLogoSource(source: String): Boolean {
            val trimmed = source.trim()
            if (trimmed.isBlank()) return false
            if (trimmed.startsWith("file://") || trimmed.startsWith("/")) {
                val file = java.io.File(trimmed.removePrefix("file://"))
                return file.exists() && file.length() > 0L
            }
            return true
        }
    }
}
