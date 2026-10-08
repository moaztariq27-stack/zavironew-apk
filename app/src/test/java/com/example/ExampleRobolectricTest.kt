package com.example

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.FirebaseRepository
import com.example.model.LogoSlot
import com.example.model.Product
import com.example.viewmodel.ZaviroViewModel
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ExampleRobolectricTest {

    private lateinit var application: Application

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        application.getSharedPreferences("zaviro_local_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        application.getSharedPreferences("zaviro_local_store", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        application.filesDir.listFiles()?.forEach { it.delete() }
    }

    @Test
    fun verifyCustomerUsernameAndProfileImagePersistAcrossAppRestart() {
        val viewModel = ZaviroViewModel(application)

        // 1. Change Customer username, phone, address, and notes
        viewModel.updateCustomerName("Moaz Tariq")
        viewModel.updateCustomerPhone("0300-9998877")
        viewModel.updateDeliveryAddress("DHA Phase 6, Lahore")
        viewModel.updateOrderNotes("Ring bell twice")
        viewModel.persistCurrentCustomerProfileSync()

        // 2. Upload Customer profile image from a local file URI
        val testAvatarBytes = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08
        )
        val tempAvatarFile = File(application.cacheDir, "customer_test_avatar.png")
        tempAvatarFile.writeBytes(testAvatarBytes)
        val savedAvatarUri = viewModel.uploadCustomerProfileImageUri(application, Uri.fromFile(tempAvatarFile))
        assertNotNull("Uploaded customer profile image URI must not be null", savedAvatarUri)
        assertTrue(savedAvatarUri!!.startsWith("file://"))

        // Close app and reopen (instantiate new ViewModel & Repository from disk)
        val reopenedViewModel = ZaviroViewModel(application)
        assertEquals("Moaz Tariq", reopenedViewModel.customerName.value)
        assertEquals("0300-9998877", reopenedViewModel.customerPhone.value)
        assertEquals("DHA Phase 6, Lahore", reopenedViewModel.deliveryAddress.value)
        assertEquals("Ring bell twice", reopenedViewModel.orderNotes.value)
        assertEquals(savedAvatarUri, reopenedViewModel.customerProfileImageUrl.value)

        // Verify exact bytes of the persisted customer avatar file
        val persistedAvatarFile = File(reopenedViewModel.customerProfileImageUrl.value.removePrefix("file://"))
        assertTrue("Persisted customer avatar file must exist on disk after restart", persistedAvatarFile.exists())
        assertArrayEquals("Customer profile image bytes must match exact uploaded file", testAvatarBytes, persistedAvatarFile.readBytes())
    }

    @Test
    fun verifyExactUploadedZaviroLogoPersistsByteForByteAcrossAppRestart() {
        val viewModel = ZaviroViewModel(application)

        // Create a test original Zaviro logo file with exact bytes (simulating user's exact uploaded logo)
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.argb(255, 255, 184, 0))
        val sourceLogoFile = File(application.cacheDir, "original_zaviro_logo_upload.png")
        java.io.FileOutputStream(sourceLogoFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val originalUploadedBytes = sourceLogoFile.readBytes()
        assertTrue("Original logo bytes must be non-empty", originalUploadedBytes.isNotEmpty())

        // Upload the exact Zaviro logo via ViewModel
        val savedLogoPath = viewModel.uploadExactLogoUri(
            context = application,
            uri = Uri.fromFile(sourceLogoFile),
            slot = LogoSlot.MASTER,
            applyToAllSlots = true
        )
        assertNotNull("Saved logo path must not be null", savedLogoPath)
        assertTrue(savedLogoPath!!.startsWith("file://"))

        // Verify all slots in branding immediately point to the exact uploaded logo
        assertEquals(savedLogoPath, viewModel.branding.value.masterLogoUrl)
        assertEquals(savedLogoPath, viewModel.branding.value.getLogoForSlot(LogoSlot.SPLASH))
        assertEquals(savedLogoPath, viewModel.branding.value.getLogoForSlot(LogoSlot.HOME_HEADER))
        assertEquals(savedLogoPath, viewModel.branding.value.getLogoForSlot(LogoSlot.LOGIN))
        assertEquals(savedLogoPath, viewModel.branding.value.getLogoForSlot(LogoSlot.ADMIN))

        // Close app and reopen (instantiate new ViewModel & Repository from disk)
        val reopenedViewModel = ZaviroViewModel(application)
        assertEquals(savedLogoPath, reopenedViewModel.branding.value.masterLogoUrl)
        assertEquals(savedLogoPath, reopenedViewModel.branding.value.getLogoForSlot(LogoSlot.MASTER))
        assertEquals(savedLogoPath, reopenedViewModel.branding.value.getLogoForSlot(LogoSlot.HOME_HEADER))

        // Verify the persisted file on disk is 100% byte-for-byte identical to the uploaded file (not re-encoded or modified)
        val persistedLogoFile = File(reopenedViewModel.branding.value.masterLogoUrl.removePrefix("file://"))
        assertTrue("Persisted exact Zaviro logo file must exist after app restart", persistedLogoFile.exists())
        assertArrayEquals(
            "Persisted Zaviro logo must be 100% byte-for-byte identical to the uploaded logo",
            originalUploadedBytes,
            persistedLogoFile.readBytes()
        )
    }

    @Test
    fun verifyCompleteAdminFlowAndPersistence() {
        // 1. Initialize ViewModel (simulating first app launch)
        val viewModel = ZaviroViewModel(application)

        // 2. Verify Admin Login with default passwords ("zaviro786" and "zaviro.pakistan@786")
        assertFalse(viewModel.isAdminLoggedIn.value)
        assertFalse(viewModel.loginAdmin("wrong_password"))
        assertFalse("Invalid password must not be accepted", viewModel.loginAdmin("admin123"))

        // Verify "zaviro786" works out of the box
        assertTrue("Default passkey zaviro786 must be accepted", viewModel.loginAdmin("zaviro786"))
        assertTrue(viewModel.isAdminLoggedIn.value)
        viewModel.logoutAdmin()

        // Verify Customer login remains functional and independent of Admin Panel password access
        viewModel.onGoogleSignInSuccess(
            uid = "cust_google_test_1",
            displayName = "Ali Customer",
            email = "ali.customer@example.com",
            phoneNumber = "03001112233",
            photoUrl = null
        )
        assertTrue(viewModel.isCustomerLoggedIn.value)
        assertFalse("Customer login must not automatically unlock Admin Panel without Admin password", viewModel.isAdminLoggedIn.value)

        // Entering the default password ("zaviro.pakistan@786") also opens Admin Panel immediately
        assertTrue(viewModel.loginAdmin("zaviro.pakistan@786"))
        assertTrue(viewModel.isAdminLoggedIn.value)

        // Verify mobile keyboard auto-space after period and auto-capitalization also unlock default Admin Panel
        viewModel.logoutAdmin()
        assertTrue("Mobile keyboard auto-space after period must be accepted", viewModel.loginAdmin("zaviro. pakistan@786"))
        viewModel.logoutAdmin()
        assertTrue("Mobile keyboard auto-capitalization must be accepted", viewModel.loginAdmin("Zaviro.pakistan@786"))
        assertTrue(viewModel.isAdminLoggedIn.value)

        // 3. Change Admin Password -> Validate current password, match confirmation, and persist securely
        var changeSuccess = viewModel.changeAdminPassword("wrong_current", "newZaviro2025", "newZaviro2025")
        assertFalse(changeSuccess)

        changeSuccess = viewModel.changeAdminPassword("zaviro.pakistan@786", "newZaviro2025", "mismatch2025")
        assertFalse(changeSuccess)

        changeSuccess = viewModel.changeAdminPassword("zaviro.pakistan@786", "newZaviro2025", "newZaviro2025")
        assertTrue(changeSuccess)

        // Verify password is NEVER stored in plain text in SharedPreferences
        val prefs = application.getSharedPreferences("zaviro_local_prefs", android.content.Context.MODE_PRIVATE)
        assertFalse("Plain-text password key must never exist in SharedPreferences", prefs.contains("admin_custom_password"))
        val allPrefsDump = prefs.all.values.joinToString(" ")
        assertFalse("New password must not appear in plain text in SharedPreferences", allPrefsDump.contains("newZaviro2025"))
        assertFalse("Default password must not appear in plain text in SharedPreferences", allPrefsDump.contains("zaviro.pakistan@786"))

        // Verify logout & re-login requires the NEW password and old default password stops working
        viewModel.logoutAdmin()
        assertFalse(viewModel.isAdminLoggedIn.value)
        assertFalse("Old default password must no longer work after change", viewModel.loginAdmin("zaviro.pakistan@786"))
        assertTrue("New password must work", viewModel.loginAdmin("newZaviro2025"))

        // 4. Manage Menu -> Edit existing food item & Change Image -> Save
        val pastaItem = viewModel.products.value.first { it.id == "prod_creamy_chicken_pasta" }
        val testBitmap = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888)
        testBitmap.eraseColor(Color.rgb(255, 184, 0))
        val uploadedDataUri = viewModel.processBitmapToBase64(testBitmap)
        assertTrue("Uploaded image should be a valid data URI", uploadedDataUri.startsWith("data:image/"))

        val updatedPasta = pastaItem.copy(
            name = "ZAVIRO Royal Alfredo Pasta",
            description = "Extra creamy white sauce pasta with grilled chicken chunks and herbs.",
            ingredients = "Penne Pasta, Double Cream, Parmesan, Grilled Chicken, Oregano",
            price = 599,
            category = "Pasta",
            isAvailable = true,
            imageUrl = uploadedDataUri
        )
        viewModel.saveProduct(updatedPasta)

        // Verify Customer App sees updated item and image immediately
        val customerVisiblePasta = viewModel.products.value.first { it.id == "prod_creamy_chicken_pasta" }
        assertEquals("ZAVIRO Royal Alfredo Pasta", customerVisiblePasta.name)
        assertEquals(599, customerVisiblePasta.price)
        assertEquals("Penne Pasta, Double Cream, Parmesan, Grilled Chicken, Oregano", customerVisiblePasta.ingredients)
        assertEquals(uploadedDataUri, customerVisiblePasta.imageUrl)

        // 5. Edit existing Deal & Image
        val deal1 = viewModel.deals.value.first { it.id == "deal_1" }
        val updatedDeal1 = deal1.copy(
            name = "ZAVIRO SUPER QUICK BITE",
            price = 525,
            items = listOf("Loaded Zinger Wrap", "Large Masala Fries", "500ml Drink"),
            imageUrl = uploadedDataUri
        )
        viewModel.saveDeal(updatedDeal1)
        assertEquals("ZAVIRO SUPER QUICK BITE", viewModel.deals.value.first { it.id == "deal_1" }.name)
        assertEquals(525, viewModel.deals.value.first { it.id == "deal_1" }.price)

        // 6. Edit Extra / Add-on item
        val extraCheese = viewModel.extras.value.first { it.id == "extra_cheese" }
        viewModel.saveExtra(extraCheese.copy(name = "Double Cheddar Cheese", price = 120))
        assertEquals("Double Cheddar Cheese", viewModel.extras.value.first { it.id == "extra_cheese" }.name)
        assertEquals(120, viewModel.extras.value.first { it.id == "extra_cheese" }.price)

        // 7. Add a new Menu Item and then Delete an item
        val newMenuItem = Product(
            id = "prod_custom_peri_bites",
            name = "Peri Peri Chicken Bites",
            category = "Crispy Chicken",
            description = "Spicy peri peri stuffed chicken bites with cheese dip.",
            ingredients = "Chicken Breast, Jalapenos, Mozzarella, Peri Peri Sauce",
            price = 449,
            imageUrl = uploadedDataUri,
            isAvailable = true
        )
        viewModel.saveProduct(newMenuItem)
        assertTrue(viewModel.products.value.any { it.id == "prod_custom_peri_bites" })

        // 8. Update Admin Store / Delivery settings
        viewModel.updateStoreSettings(
            viewModel.settings.value.copy(
                deliveryFee = 200,
                announcement = "Special Eid & Late Night Delivery Active!"
            )
        )

        // 9. Verify Persistence after closing/reopening app (new Repository + new ViewModel instance)
        val reopenedRepo = FirebaseRepository(application)
        assertTrue("New admin password must persist after app restart", reopenedRepo.verifyAdminPassword("newZaviro2025"))
        assertFalse("Old default admin password must remain invalid after app restart", reopenedRepo.verifyAdminPassword("zaviro.pakistan@786"))
        assertFalse("Default zaviro786 password must remain invalid while custom password is active", reopenedRepo.verifyAdminPassword("zaviro786"))

        val reopenedViewModel = ZaviroViewModel(application)
        assertTrue("Admin login session must remain active if not logged out", reopenedViewModel.isAdminLoggedIn.value)

        // Verify Reset Admin Password to Default restores zaviro786 and zaviro.pakistan@786
        reopenedViewModel.logoutAdmin()
        assertFalse(reopenedViewModel.isAdminLoggedIn.value)
        val resetPass = reopenedViewModel.resetAdminPasswordToDefault(autoUnlock = false)
        assertEquals("zaviro786", resetPass)
        assertTrue("After reset, zaviro786 must unlock Admin Panel", reopenedViewModel.loginAdmin("zaviro786"))
        assertTrue(reopenedViewModel.isAdminLoggedIn.value)

        val persistedPasta = reopenedViewModel.products.value.first { it.id == "prod_creamy_chicken_pasta" }
        assertEquals("ZAVIRO Royal Alfredo Pasta", persistedPasta.name)
        assertEquals(599, persistedPasta.price)
        assertEquals("Penne Pasta, Double Cream, Parmesan, Grilled Chicken, Oregano", persistedPasta.ingredients)
        assertEquals(uploadedDataUri, persistedPasta.imageUrl)

        val persistedNewItem = reopenedViewModel.products.value.firstOrNull { it.id == "prod_custom_peri_bites" }
        assertTrue("Newly added menu item must persist after app restart", persistedNewItem != null)

        val persistedDeal1 = reopenedViewModel.deals.value.first { it.id == "deal_1" }
        assertEquals("ZAVIRO SUPER QUICK BITE", persistedDeal1.name)
        assertEquals(525, persistedDeal1.price)
        assertEquals(uploadedDataUri, persistedDeal1.imageUrl)

        val persistedExtra = reopenedViewModel.extras.value.first { it.id == "extra_cheese" }
        assertEquals("Double Cheddar Cheese", persistedExtra.name)
        assertEquals(120, persistedExtra.price)

        assertEquals(200, reopenedViewModel.settings.value.deliveryFee)
        assertEquals("Special Eid & Late Night Delivery Active!", reopenedViewModel.settings.value.announcement)

        // Test deletion persistence
        reopenedViewModel.deleteProduct("prod_custom_peri_bites")
        val thirdViewModel = ZaviroViewModel(application)
        assertFalse("Deleted menu item must stay deleted after restart", thirdViewModel.products.value.any { it.id == "prod_custom_peri_bites" })
    }

    @Test
    fun verifyFirebaseSettingsAndBrandingPersistenceAcrossNewDeviceInstall() {
        val viewModel = ZaviroViewModel(application)

        // Create custom branding logo as a portable data URI (as stored in Firestore settings/branding)
        val logoBitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
        logoBitmap.eraseColor(Color.rgb(220, 40, 40))
        val remoteLogoDataUri = viewModel.processBitmapToBase64(logoBitmap)

        // Update branding and store settings
        val configuredBranding = com.example.model.BrandingSettings(
            masterLogoUrl = remoteLogoDataUri,
            homeHeaderLogoUrl = remoteLogoDataUri,
            splashLogoUrl = remoteLogoDataUri,
            loginLogoUrl = remoteLogoDataUri,
            adminLogoUrl = remoteLogoDataUri,
            primaryColorHex = "#E53935",
            secondaryColorHex = "#FF8A80",
            updatedAt = 1712345678900L
        )
        viewModel.saveBranding(configuredBranding)

        val configuredSettings = com.example.model.AppSettings(
            deliveryFee = 250,
            minOrderAmount = 500,
            isStoreOpen = true,
            announcement = "Zaviro Special Ramadan Deals Live!",
            whatsappNumber = "0321-7654321",
            instagramHandle = "@zaviro_official",
            deliveryAreas = "All Sectors of Lahore"
        )
        viewModel.updateStoreSettings(configuredSettings)

        // Verify all logo slots resolve to the configured Firebase logo
        val activeBranding = viewModel.branding.value
        assertEquals(remoteLogoDataUri, activeBranding.getLogoForSlot(LogoSlot.MASTER))
        assertEquals(remoteLogoDataUri, activeBranding.getLogoForSlot(LogoSlot.HOME_HEADER))
        assertEquals(remoteLogoDataUri, activeBranding.getLogoForSlot(LogoSlot.SPLASH))
        assertEquals(remoteLogoDataUri, activeBranding.getLogoForSlot(LogoSlot.LOGIN))
        assertEquals(remoteLogoDataUri, activeBranding.getLogoForSlot(LogoSlot.ADMIN))
        assertEquals("#E53935", activeBranding.primaryColorHex)
        assertEquals("#FF8A80", activeBranding.secondaryColorHex)

        // Verify AppSettings fields including formattedWhatsappForUrl derived from whatsappNumber
        val activeSettings = viewModel.settings.value
        assertEquals(250, activeSettings.deliveryFee)
        assertEquals(500, activeSettings.minOrderAmount)
        assertEquals("0321-7654321", activeSettings.whatsappNumber)
        assertEquals("923217654321", activeSettings.formattedWhatsappForUrl)
        assertEquals("All Sectors of Lahore", activeSettings.deliveryAreas)

        // Verify local cache survival on offline restart
        val restartedViewModel = ZaviroViewModel(application)
        assertEquals(remoteLogoDataUri, restartedViewModel.branding.value.getLogoForSlot(LogoSlot.MASTER))
        assertEquals("#E53935", restartedViewModel.branding.value.primaryColorHex)
        assertEquals(250, restartedViewModel.settings.value.deliveryFee)
        assertEquals("0321-7654321", restartedViewModel.settings.value.whatsappNumber)

        // Verify that Admin uploading a product/deal image from phone gallery produces a portable cloud data URI
        // (not a device-only file:// path) and decodes cleanly on another phone where local filesDir is empty
        val wingsBitmap = Bitmap.createBitmap(60, 60, Bitmap.Config.ARGB_8888)
        wingsBitmap.eraseColor(Color.rgb(255, 140, 0))
        val tempGalleryFile = File(application.cacheDir, "admin_wings_photo.png")
        java.io.FileOutputStream(tempGalleryFile).use { out ->
            wingsBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val cloudWingsImageUri = viewModel.processImageUriToBase64(application, Uri.fromFile(tempGalleryFile))
        assertNotNull("Processed gallery image must not be null", cloudWingsImageUri)
        assertTrue(
            "Admin uploaded image must be a cloud-portable data URI instead of a local-only file path",
            cloudWingsImageUri!!.startsWith("data:image/")
        )

        val decodedOnPhoneB = com.example.ui.components.decodeExactBitmapFromSource(cloudWingsImageUri, filesDir = null)
        assertNotNull("Customer Phone B must decode the cloud image URI without local files", decodedOnPhoneB)
        assertEquals(60, decodedOnPhoneB!!.width)
        assertEquals(60, decodedOnPhoneB.height)
    }

    @Test
    fun verifyCustomerPhoneOtpAndGoogleLoginPersistence() {
        val viewModel = ZaviroViewModel(application)
        assertFalse("Customer should not be logged in initially", viewModel.isCustomerLoggedIn.value)

        // 1. Test Phone Number formatting & OTP flow
        assertEquals("+923001234567", viewModel.formatPakistanPhoneToE164("0300-1234567"))
        assertEquals("+923219876543", viewModel.formatPakistanPhoneToE164("+92 321 9876543"))

        viewModel.sendPhoneOtp(activity = null, rawPhone = "0300-1234567")
        assertTrue("OTP sent flag should be true after requesting OTP", viewModel.isOtpSent.value)
        assertNotNull("Verification ID should be set", viewModel.phoneVerificationId.value)

        viewModel.verifyPhoneOtp(otpCode = "123456")
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        Thread.sleep(100)
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        assertTrue("Customer must be authenticated after OTP verification", viewModel.isCustomerLoggedIn.value)
        assertEquals("phone", viewModel.customerAuthProvider.value)
        assertEquals("0300-1234567", viewModel.customerPhone.value)

        // Verify Phone login persists across app restart
        val restartedAfterPhone = ZaviroViewModel(application)
        assertTrue("Customer phone login session must persist across app restart", restartedAfterPhone.isCustomerLoggedIn.value)
        assertEquals("phone", restartedAfterPhone.customerAuthProvider.value)
        assertEquals("0300-1234567", restartedAfterPhone.customerPhone.value)

        // 2. Test Logout
        restartedAfterPhone.logoutCustomer()
        assertFalse("Customer must be logged out after calling logoutCustomer()", restartedAfterPhone.isCustomerLoggedIn.value)

        // 3. Test Google Account Sign-In flow & persistence
        restartedAfterPhone.onGoogleSignInSuccess(
            uid = "google_uid_998877",
            displayName = "Ali Raza",
            email = "aliraza@example.com",
            phoneNumber = "0333-4455667",
            photoUrl = null
        )
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        Thread.sleep(100)
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        assertTrue("Customer must be authenticated after Google Sign-In", restartedAfterPhone.isCustomerLoggedIn.value)
        assertEquals("google", restartedAfterPhone.customerAuthProvider.value)
        assertEquals("Ali Raza", restartedAfterPhone.customerName.value)
        assertEquals("aliraza@example.com", restartedAfterPhone.customerEmail.value)

        // Verify Google login persists across app restart
        val restartedAfterGoogle = ZaviroViewModel(application)
        assertTrue("Google login session must persist across app restart", restartedAfterGoogle.isCustomerLoggedIn.value)
        assertEquals("google", restartedAfterGoogle.customerAuthProvider.value)
        assertEquals("Ali Raza", restartedAfterGoogle.customerName.value)
        assertEquals("aliraza@example.com", restartedAfterGoogle.customerEmail.value)
    }
}
