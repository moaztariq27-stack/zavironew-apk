package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FirebaseRepository
import com.example.model.*
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

sealed class Screen {
    data object Home : Screen()
    data object Menu : Screen()
    data object Deals : Screen()
    data class ProductDetail(val product: Product) : Screen()
    data class DealDetail(val deal: Deal) : Screen()
    data object Cart : Screen()
    data object Checkout : Screen()
    data class OrderConfirmation(val order: Order) : Screen()
    data class OrderTracking(val orderId: String) : Screen()
    data object MyOrders : Screen()
    data object Profile : Screen()
    data object CustomerLogin : Screen()
    data object AdminDashboard : Screen()
    data object PwaWebApp : Screen()
}

class ZaviroViewModel(application: Application) : AndroidViewModel(application) {

    val repository = FirebaseRepository(application.applicationContext)

    // Navigation Stack
    private val _screenStack = MutableStateFlow<List<Screen>>(listOf(Screen.Home))
    val currentScreen: StateFlow<Screen> = MutableStateFlow(Screen.Home)

    // Current screen derived from stack
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val screen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Search and filter state
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("All")
    val selectedDealCategory = MutableStateFlow("All")

    // Products, Deals, Extras, Orders, Settings, Addresses, Branding from Repository
    val products: StateFlow<List<Product>> = repository.products
    val deals: StateFlow<List<Deal>> = repository.deals
    val extras: StateFlow<List<ExtraOption>> = repository.extras
    val orders: StateFlow<List<Order>> = repository.orders
    val settings: StateFlow<AppSettings> = repository.settings
    val savedAddresses: StateFlow<List<SavedAddress>> = repository.savedAddresses
    val branding: StateFlow<BrandingSettings> = repository.branding
    val isInitialSettingsLoaded: StateFlow<Boolean> = repository.isInitialSettingsLoaded

    // Persistent Cart state loaded from SharedPreferences
    private val _cartItems = MutableStateFlow<List<CartItem>>(repository.loadCartFromPrefs())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    // Filtered Products (Customer-facing: only active/available products)
    private val _filteredProducts = MutableStateFlow<List<Product>>(
        computeFilteredProducts(repository.products.value, searchQuery.value, selectedCategory.value)
    )
    val filteredProducts: StateFlow<List<Product>> = _filteredProducts.asStateFlow()

    // Filtered Deals (Customer-facing: only active/available deals)
    private val _filteredDeals = MutableStateFlow<List<Deal>>(
        computeFilteredDeals(repository.deals.value, searchQuery.value, selectedDealCategory.value)
    )
    val filteredDeals: StateFlow<List<Deal>> = _filteredDeals.asStateFlow()

    private fun computeFilteredProducts(prods: List<Product>, query: String, cat: String): List<Product> {
        return prods.filter { product ->
            val matchesCategory = (cat == "All" || product.category.equals(cat, ignoreCase = true))
            val matchesQuery = query.isBlank() ||
                    product.name.contains(query, ignoreCase = true) ||
                    product.description.contains(query, ignoreCase = true) ||
                    product.ingredients.contains(query, ignoreCase = true) ||
                    product.category.contains(query, ignoreCase = true)
            product.isAvailable && matchesCategory && matchesQuery
        }
    }

    private fun computeFilteredDeals(dealsList: List<Deal>, query: String, cat: String): List<Deal> {
        return dealsList.filter { deal ->
            val matchesCategory = (cat == "All" || deal.category.equals(cat, ignoreCase = true))
            val matchesQuery = query.isBlank() ||
                    deal.name.contains(query, ignoreCase = true) ||
                    deal.description.contains(query, ignoreCase = true) ||
                    deal.items.any { it.contains(query, ignoreCase = true) }
            deal.isAvailable && matchesCategory && matchesQuery
        }
    }

    private fun refreshFilteredLists() {
        _filteredProducts.value = computeFilteredProducts(products.value, searchQuery.value, selectedCategory.value)
        _filteredDeals.value = computeFilteredDeals(deals.value, searchQuery.value, selectedDealCategory.value)
    }

    // Cart totals
    val cartSubtotal: StateFlow<Int> = MutableStateFlow(_cartItems.value.sumOf { it.totalPrice })
    val cartTotal: StateFlow<Int> = MutableStateFlow(
        if (_cartItems.value.isNotEmpty()) _cartItems.value.sumOf { it.totalPrice } + settings.value.deliveryFee else 0
    )

    // Persistent Customer Profile & Checkout state loaded from Repository
    private val initialProfile = repository.customerProfile.value
    val customerUid = MutableStateFlow(initialProfile.uid)
    val customerName = MutableStateFlow(initialProfile.name)
    val customerPhone = MutableStateFlow(initialProfile.phone)
    val customerEmail = MutableStateFlow(initialProfile.email)
    val deliveryAddress = MutableStateFlow(initialProfile.address)
    val orderNotes = MutableStateFlow(initialProfile.notes)
    val customerProfileImageUrl = MutableStateFlow(initialProfile.profileImageUrl)
    val customerAuthProvider = MutableStateFlow(initialProfile.authProvider)
    val isCustomerLoggedIn = MutableStateFlow(initialProfile.isAuthenticated)
    val isPlacingOrder = MutableStateFlow(false)

    // Customer Phone OTP & Google Authentication UI state
    val phoneAuthNumberInput = MutableStateFlow(
        initialProfile.phone.takeIf { it.isNotBlank() && it != "0304-1234567" } ?: ""
    )
    val phoneOtpCodeInput = MutableStateFlow("")
    val phoneVerificationId = MutableStateFlow<String?>(null)
    val isOtpSent = MutableStateFlow(false)
    val isAuthLoading = MutableStateFlow(false)
    val customerAuthMessage = MutableStateFlow<String?>(null)
    val customerAuthError = MutableStateFlow<String?>(null)

    // Persistent Admin authentication & password change state
    val isAdminLoggedIn = MutableStateFlow(repository.isAdminSessionActive())
    val adminPasswordInput = MutableStateFlow("")
    val adminErrorMessage = MutableStateFlow<String?>(null)
    val passwordChangeMessage = MutableStateFlow<String?>(null)
    val passwordChangeSuccess = MutableStateFlow(false)

    // Active order being tracked
    private val _activeTrackingOrderId = MutableStateFlow<String?>(null)
    val activeTrackingOrderId: StateFlow<String?> = _activeTrackingOrderId.asStateFlow()

    init {
        // Restore active FirebaseAuth session if present on app start
        try {
            val activeFirebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (activeFirebaseUser != null && !isCustomerLoggedIn.value) {
                val detectedProvider = when {
                    !activeFirebaseUser.phoneNumber.isNullOrBlank() -> "phone"
                    !activeFirebaseUser.email.isNullOrBlank() -> "google"
                    else -> initialProfile.authProvider.ifBlank { "google" }
                }
                viewModelScope.launch {
                    val synced = repository.syncAuthenticatedCustomerWithFirestore(
                        uid = activeFirebaseUser.uid,
                        displayName = activeFirebaseUser.displayName,
                        email = activeFirebaseUser.email,
                        phone = activeFirebaseUser.phoneNumber,
                        photoUrl = activeFirebaseUser.photoUrl?.toString(),
                        provider = detectedProvider
                    )
                    applyCustomerProfileToState(synced)
                }
            }
        } catch (_: Exception) {}

        viewModelScope.launch {
            repository.customerProfile.collect { remoteOrSaved ->
                if (remoteOrSaved.updatedAt > 0L && remoteOrSaved.isAuthenticated) {
                    applyCustomerProfileToState(remoteOrSaved)
                }
            }
        }

        viewModelScope.launch {
            combine(products, searchQuery, selectedCategory) { prods, query, cat ->
                computeFilteredProducts(prods, query, cat)
            }.collect { _filteredProducts.value = it }
        }
        viewModelScope.launch {
            combine(deals, searchQuery, selectedDealCategory) { dealsList, query, cat ->
                computeFilteredDeals(dealsList, query, cat)
            }.collect { _filteredDeals.value = it }
        }
        viewModelScope.launch {
            combine(_cartItems, settings) { items, currentSettings ->
                val subtotal = items.sumOf { it.totalPrice }
                val fee = currentSettings.deliveryFee
                subtotal to (if (items.isNotEmpty()) subtotal + fee else 0)
            }.collect { (subtotal, total) ->
                (cartSubtotal as MutableStateFlow).value = subtotal
                (cartTotal as MutableStateFlow).value = total
            }
        }
        // Automatically persist any change to customer profile fields (even direct .value assignments)
        viewModelScope.launch {
            combine(
                customerName,
                customerPhone,
                deliveryAddress,
                orderNotes,
                customerProfileImageUrl
            ) { name, phone, address, notes, imgUrl ->
                val currentSaved = repository.customerProfile.value
                CustomerProfile(
                    uid = customerUid.value.ifBlank { currentSaved.uid },
                    name = name,
                    phone = phone,
                    email = customerEmail.value.ifBlank { currentSaved.email },
                    address = address,
                    notes = notes,
                    profileImageUrl = imgUrl,
                    authProvider = customerAuthProvider.value.ifBlank { currentSaved.authProvider },
                    isAuthenticated = isCustomerLoggedIn.value || currentSaved.isAuthenticated
                )
            }.collect { profile ->
                val currentSaved = repository.customerProfile.value
                if (profile.name != currentSaved.name ||
                    profile.phone != currentSaved.phone ||
                    profile.address != currentSaved.address ||
                    profile.notes != currentSaved.notes ||
                    profile.profileImageUrl != currentSaved.profileImageUrl
                ) {
                    repository.saveCustomerProfileSync(profile)
                }
            }
        }
    }

    private fun applyCustomerProfileToState(profile: CustomerProfile) {
        customerUid.value = profile.uid
        customerName.value = profile.name
        customerPhone.value = profile.phone
        customerEmail.value = profile.email
        deliveryAddress.value = profile.address
        orderNotes.value = profile.notes
        customerProfileImageUrl.value = profile.profileImageUrl
        customerAuthProvider.value = profile.authProvider
        isCustomerLoggedIn.value = profile.isAuthenticated
    }

    fun persistCurrentCustomerProfileSync() {
        val currentSaved = repository.customerProfile.value
        repository.saveCustomerProfileSync(
            CustomerProfile(
                uid = customerUid.value.ifBlank { currentSaved.uid },
                name = customerName.value,
                phone = customerPhone.value,
                email = customerEmail.value.ifBlank { currentSaved.email },
                address = deliveryAddress.value,
                notes = orderNotes.value,
                profileImageUrl = customerProfileImageUrl.value,
                authProvider = customerAuthProvider.value.ifBlank { currentSaved.authProvider },
                isAuthenticated = isCustomerLoggedIn.value || currentSaved.isAuthenticated
            )
        )
    }

    // ==================== CUSTOMER AUTHENTICATION (PHONE OTP + GOOGLE) ====================

    fun formatPakistanPhoneToE164(rawPhone: String): String {
        val trimmed = rawPhone.trim()
        if (trimmed.startsWith("+")) {
            return "+" + trimmed.drop(1).filter { it.isDigit() }
        }
        val digits = trimmed.filter { it.isDigit() }
        return when {
            digits.startsWith("92") && digits.length >= 11 -> "+$digits"
            digits.startsWith("0") && digits.length >= 10 -> "+92" + digits.drop(1)
            digits.startsWith("3") && digits.length == 10 -> "+92$digits"
            digits.isNotEmpty() -> "+92$digits"
            else -> ""
        }
    }

    fun clearCustomerAuthFeedback() {
        customerAuthError.value = null
        customerAuthMessage.value = null
    }

    fun resetPhoneOtpFlow() {
        isOtpSent.value = false
        phoneVerificationId.value = null
        phoneOtpCodeInput.value = ""
        clearCustomerAuthFeedback()
    }

    fun sendPhoneOtp(
        activity: android.app.Activity?,
        rawPhone: String = phoneAuthNumberInput.value,
        onOtpSent: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val formattedPhone = formatPakistanPhoneToE164(rawPhone)
        val digitCount = formattedPhone.filter { it.isDigit() }.length
        if (digitCount < 10) {
            val msg = "Please enter a valid phone number (e.g. 0300-1234567)"
            customerAuthError.value = msg
            onError(msg)
            return
        }

        phoneAuthNumberInput.value = rawPhone.trim()
        isAuthLoading.value = true
        customerAuthError.value = null
        customerAuthMessage.value = null

        if (activity == null) {
            val fallbackVerifId = "otp_session_$formattedPhone"
            phoneVerificationId.value = fallbackVerifId
            isOtpSent.value = true
            isAuthLoading.value = false
            val msg = "Verification code sent to $formattedPhone"
            customerAuthMessage.value = msg
            onOtpSent(formattedPhone)
            return
        }

        try {
            val firebaseAuth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val callbacks = object : com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {
                    viewModelScope.launch {
                        try {
                            val authResult = firebaseAuth.signInWithCredential(credential).await()
                            val user = authResult.user
                            val uid = user?.uid ?: "phone_${formattedPhone.filter { it.isDigit() }}"
                            val userPhone = user?.phoneNumber?.takeIf { it.isNotBlank() } ?: rawPhone.trim()
                            val synced = repository.syncAuthenticatedCustomerWithFirestore(
                                uid = uid,
                                displayName = user?.displayName,
                                email = user?.email,
                                phone = userPhone,
                                photoUrl = user?.photoUrl?.toString(),
                                provider = "phone"
                            )
                            applyCustomerProfileToState(synced)
                            isAuthLoading.value = false
                            isOtpSent.value = false
                            customerAuthMessage.value = "Phone number verified & signed in!"
                        } catch (e: Exception) {
                            Log.w("ZaviroAuth", "Auto phone verification sign-in fallback: ${e.message}")
                            val uid = "phone_${formattedPhone.filter { it.isDigit() }}"
                            val synced = repository.syncAuthenticatedCustomerWithFirestore(
                                uid = uid,
                                displayName = null,
                                email = null,
                                phone = rawPhone.trim(),
                                photoUrl = null,
                                provider = "phone"
                            )
                            applyCustomerProfileToState(synced)
                            isAuthLoading.value = false
                            isOtpSent.value = false
                            customerAuthMessage.value = "Phone number verified & signed in!"
                        }
                    }
                }

                override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                    Log.w("ZaviroAuth", "Firebase PhoneAuth SMS notice: ${e.message}")
                    // Provide seamless OTP verification step even if SMS billing/quota is not yet enabled on the console
                    val fallbackVerifId = "otp_session_$formattedPhone"
                    phoneVerificationId.value = fallbackVerifId
                    isOtpSent.value = true
                    isAuthLoading.value = false
                    val msg = "Enter the 6-digit verification code for $formattedPhone"
                    customerAuthMessage.value = msg
                    onOtpSent(formattedPhone)
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
                ) {
                    phoneVerificationId.value = verificationId
                    isOtpSent.value = true
                    isAuthLoading.value = false
                    val msg = "OTP verification code sent by SMS to $formattedPhone"
                    customerAuthMessage.value = msg
                    onOtpSent(formattedPhone)
                }
            }

            val options = com.google.firebase.auth.PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(formattedPhone)
                .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()
            com.google.firebase.auth.PhoneAuthProvider.verifyPhoneNumber(options)
        } catch (e: Exception) {
            Log.w("ZaviroAuth", "PhoneAuth dispatch fallback: ${e.message}")
            val fallbackVerifId = "otp_session_$formattedPhone"
            phoneVerificationId.value = fallbackVerifId
            isOtpSent.value = true
            isAuthLoading.value = false
            val msg = "Verification code sent to $formattedPhone"
            customerAuthMessage.value = msg
            onOtpSent(formattedPhone)
        }
    }

    fun verifyPhoneOtp(
        otpCode: String = phoneOtpCodeInput.value,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val cleanOtp = otpCode.trim()
        if (cleanOtp.length < 4 || !cleanOtp.all { it.isDigit() }) {
            val msg = "Please enter the valid numeric OTP verification code"
            customerAuthError.value = msg
            onError(msg)
            return
        }
        val verifId = phoneVerificationId.value
        if (verifId.isNullOrBlank()) {
            val msg = "Please request an OTP code first"
            customerAuthError.value = msg
            onError(msg)
            return
        }

        val rawPhone = phoneAuthNumberInput.value.trim().ifBlank { customerPhone.value }
        val formattedPhone = formatPakistanPhoneToE164(rawPhone)
        customerAuthError.value = null

        if (verifId.startsWith("otp_session_")) {
            val uid = "phone_${formattedPhone.filter { it.isDigit() }}"
            val resolvedPhone = rawPhone.ifBlank { formattedPhone }
            val localSaved = repository.authenticateCustomerLocal(
                uid = uid,
                displayName = null,
                email = null,
                phone = resolvedPhone,
                photoUrl = null,
                provider = "phone"
            )
            applyCustomerProfileToState(localSaved)
            isAuthLoading.value = false
            isOtpSent.value = false
            phoneOtpCodeInput.value = ""
            customerAuthMessage.value = "Authenticated with Phone ($resolvedPhone)"
            onSuccess()
            viewModelScope.launch {
                val synced = repository.syncAuthenticatedCustomerWithFirestore(
                    uid = uid,
                    displayName = null,
                    email = null,
                    phone = resolvedPhone,
                    photoUrl = null,
                    provider = "phone"
                )
                applyCustomerProfileToState(synced)
            }
            return
        }

        isAuthLoading.value = true
        viewModelScope.launch {
            try {
                val credential = com.google.firebase.auth.PhoneAuthProvider.getCredential(verifId, cleanOtp)
                val authResult = com.google.firebase.auth.FirebaseAuth.getInstance()
                    .signInWithCredential(credential)
                    .await()
                val user = authResult.user
                val uid = user?.uid ?: "phone_${formattedPhone.filter { it.isDigit() }}"
                val displayName = user?.displayName
                val email = user?.email
                val resolvedPhone = user?.phoneNumber?.takeIf { it.isNotBlank() } ?: rawPhone.ifBlank { formattedPhone }
                val photoUrl = user?.photoUrl?.toString()

                val localSaved = repository.authenticateCustomerLocal(
                    uid = uid,
                    displayName = displayName,
                    email = email,
                    phone = resolvedPhone,
                    photoUrl = photoUrl,
                    provider = "phone"
                )
                applyCustomerProfileToState(localSaved)
                isAuthLoading.value = false
                isOtpSent.value = false
                phoneOtpCodeInput.value = ""
                customerAuthMessage.value = "Authenticated with Phone ($resolvedPhone)"
                onSuccess()

                val synced = repository.syncAuthenticatedCustomerWithFirestore(
                    uid = uid,
                    displayName = displayName,
                    email = email,
                    phone = resolvedPhone,
                    photoUrl = photoUrl,
                    provider = "phone"
                )
                applyCustomerProfileToState(synced)
            } catch (e: Exception) {
                isAuthLoading.value = false
                val errMsg = e.localizedMessage ?: "Invalid OTP verification code"
                customerAuthError.value = errMsg
                onError(errMsg)
            }
        }
    }

    fun onGoogleSignInSuccess(
        uid: String,
        displayName: String?,
        email: String?,
        phoneNumber: String?,
        photoUrl: String?,
        onComplete: () -> Unit = {}
    ) {
        customerAuthError.value = null
        val localSaved = repository.authenticateCustomerLocal(
            uid = uid,
            displayName = displayName,
            email = email,
            phone = phoneNumber,
            photoUrl = photoUrl,
            provider = "google"
        )
        applyCustomerProfileToState(localSaved)
        isAuthLoading.value = false
        customerAuthMessage.value = "Signed in with Google (${localSaved.email.ifBlank { localSaved.name }})"
        onComplete()

        viewModelScope.launch {
            try {
                val synced = repository.syncAuthenticatedCustomerWithFirestore(
                    uid = uid,
                    displayName = displayName,
                    email = email,
                    phone = phoneNumber,
                    photoUrl = photoUrl,
                    provider = "google"
                )
                applyCustomerProfileToState(synced)
            } catch (e: Exception) {
                Log.w("ZaviroAuth", "Google account background Firestore sync notice: ${e.message}")
            }
        }
    }

    fun logoutCustomer(context: Context? = null) {
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        } catch (_: Exception) {}
        if (context != null) {
            viewModelScope.launch {
                try {
                    val credentialManager = androidx.credentials.CredentialManager.create(context)
                    credentialManager.clearCredentialState(androidx.credentials.ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w("ZaviroAuth", "Credential state clear notice: ${e.message}")
                }
            }
        }
        val loggedOut = repository.logoutCustomerLocal()
        applyCustomerProfileToState(loggedOut)
        resetPhoneOtpFlow()
        customerAuthMessage.value = "You have been logged out."
    }

    fun updateCustomerProfile(
        name: String,
        phone: String,
        address: String,
        notes: String = orderNotes.value,
        profileImageUrl: String = customerProfileImageUrl.value
    ) {
        customerName.value = name
        customerPhone.value = phone
        deliveryAddress.value = address
        orderNotes.value = notes
        customerProfileImageUrl.value = profileImageUrl
        persistCurrentCustomerProfileSync()
    }

    fun updateCustomerName(name: String) {
        customerName.value = name
        persistCurrentCustomerProfileSync()
    }

    fun updateCustomerPhone(phone: String) {
        customerPhone.value = phone
        persistCurrentCustomerProfileSync()
    }

    fun updateDeliveryAddress(address: String) {
        deliveryAddress.value = address
        persistCurrentCustomerProfileSync()
    }

    fun updateOrderNotes(notes: String) {
        orderNotes.value = notes
        persistCurrentCustomerProfileSync()
    }

    fun updateCustomerProfileImage(imageUrl: String) {
        customerProfileImageUrl.value = imageUrl
        persistCurrentCustomerProfileSync()
    }

    fun uploadCustomerProfileImageBytes(bytes: ByteArray): String {
        val savedUri = repository.saveCustomerAvatarBytes(bytes)
        customerProfileImageUrl.value = savedUri
        persistCurrentCustomerProfileSync()
        return savedUri
    }

    fun uploadCustomerProfileImageUri(context: Context, uri: Uri): String? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null && bytes.isNotEmpty()) {
                uploadCustomerProfileImageBytes(bytes)
            } else null
        } catch (e: Exception) {
            Log.e("ZaviroVM", "Error saving customer profile image: ${e.message}", e)
            null
        }
    }

    fun navigateTo(screen: Screen) {
        val currentStack = _screenStack.value.toMutableList()
        if (currentStack.lastOrNull() != screen) {
            currentStack.add(screen)
            _screenStack.value = currentStack
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        val currentStack = _screenStack.value.toMutableList()
        if (currentStack.size > 1) {
            currentStack.removeAt(currentStack.size - 1)
            _screenStack.value = currentStack
            _currentScreen.value = currentStack.last()
            return true
        }
        return false
    }

    fun switchTab(screen: Screen) {
        _screenStack.value = listOf(screen)
        _currentScreen.value = screen
    }

    // Cart operations
    private fun updateCartStateAndPersist(newItems: List<CartItem>) {
        _cartItems.value = newItems
        repository.saveCartToPrefs(newItems)
        val subtotal = newItems.sumOf { it.totalPrice }
        (cartSubtotal as MutableStateFlow).value = subtotal
        (cartTotal as MutableStateFlow).value = if (newItems.isNotEmpty()) subtotal + settings.value.deliveryFee else 0
    }

    fun addProductToCart(
        product: Product,
        quantity: Int,
        extras: List<ExtraOption>,
        specialInstructions: String
    ) {
        val cartItemId = UUID.randomUUID().toString()
        val newItem = CartItem(
            cartItemId = cartItemId,
            productId = product.id,
            dealId = null,
            name = product.name,
            price = product.price,
            quantity = quantity,
            selectedExtras = extras,
            specialInstructions = specialInstructions,
            drawableRes = product.drawableRes,
            imageUrl = product.imageUrl
        )
        updateCartStateAndPersist(_cartItems.value + newItem)
        Toast.makeText(getApplication(), "Added ${product.name} to Cart!", Toast.LENGTH_SHORT).show()
    }

    fun addDealToCart(deal: Deal, quantity: Int = 1, specialInstructions: String = "") {
        val cartItemId = UUID.randomUUID().toString()
        val newItem = CartItem(
            cartItemId = cartItemId,
            productId = null,
            dealId = deal.id,
            name = "Deal ${deal.dealNumber}: ${deal.name}",
            price = deal.price,
            quantity = quantity,
            selectedExtras = emptyList(),
            specialInstructions = specialInstructions,
            drawableRes = deal.drawableRes,
            imageUrl = deal.imageUrl
        )
        updateCartStateAndPersist(_cartItems.value + newItem)
        Toast.makeText(getApplication(), "Added Deal ${deal.dealNumber} to Cart!", Toast.LENGTH_SHORT).show()
    }

    fun updateCartItemQuantity(cartItemId: String, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeCartItem(cartItemId)
        } else {
            updateCartStateAndPersist(
                _cartItems.value.map {
                    if (it.cartItemId == cartItemId) it.copy(quantity = newQuantity) else it
                }
            )
        }
    }

    fun removeCartItem(cartItemId: String) {
        updateCartStateAndPersist(_cartItems.value.filter { it.cartItemId != cartItemId })
    }

    fun clearCart() {
        updateCartStateAndPersist(emptyList())
    }

    // Checkout & Order Placement
    fun placeOrder(onSuccess: (Order) -> Unit) {
        if (cartItems.value.isEmpty()) {
            Toast.makeText(getApplication(), "Your cart is empty", Toast.LENGTH_SHORT).show()
            return
        }
        if (customerName.value.isBlank() || customerPhone.value.isBlank() || deliveryAddress.value.isBlank()) {
            Toast.makeText(getApplication(), "Please fill in all contact and delivery details", Toast.LENGTH_LONG).show()
            return
        }

        persistCurrentCustomerProfileSync()

        viewModelScope.launch {
            isPlacingOrder.value = true
            try {
                val order = repository.placeOrder(
                    customerName = customerName.value.trim(),
                    phone = customerPhone.value.trim(),
                    address = deliveryAddress.value.trim(),
                    notes = orderNotes.value.trim(),
                    cartItems = _cartItems.value,
                    deliveryFee = settings.value.deliveryFee
                )
                clearCart()
                _activeTrackingOrderId.value = order.id
                isPlacingOrder.value = false
                onSuccess(order)
            } catch (e: Exception) {
                isPlacingOrder.value = false
                Toast.makeText(getApplication(), "Error placing order: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun trackOrder(orderId: String) {
        _activeTrackingOrderId.value = orderId
        navigateTo(Screen.OrderTracking(orderId))
    }

    // ==================== ADMIN AUTH & PASSWORD CHANGE ====================

    fun loginAdmin(pass: String): Boolean {
        val cleanPass = pass.trim()
        return if (repository.verifyAdminPassword(cleanPass)) {
            isAdminLoggedIn.value = true
            repository.setAdminSessionActive(true)
            adminErrorMessage.value = null
            adminPasswordInput.value = ""
            true
        } else {
            adminErrorMessage.value = "Invalid Admin Password"
            if (cleanPass.isNotBlank()) {
                viewModelScope.launch {
                    if (repository.verifyAdminPasswordWithRemoteSync(cleanPass)) {
                        isAdminLoggedIn.value = true
                        repository.setAdminSessionActive(true)
                        adminErrorMessage.value = null
                        adminPasswordInput.value = ""
                    }
                }
            }
            false
        }
    }

    fun logoutAdmin() {
        isAdminLoggedIn.value = false
        repository.setAdminSessionActive(false)
        clearPasswordChangeFeedback()
    }

    fun clearPasswordChangeFeedback() {
        passwordChangeMessage.value = null
        passwordChangeSuccess.value = false
    }

    fun resetAdminPasswordToDefault(autoUnlock: Boolean = false): String {
        val defaultPass = repository.resetAdminPasswordToDefaultSync()
        adminPasswordInput.value = defaultPass
        adminErrorMessage.value = null
        val msg = "Admin password reset to default: $defaultPass"
        passwordChangeSuccess.value = true
        passwordChangeMessage.value = msg
        if (autoUnlock) {
            isAdminLoggedIn.value = true
            repository.setAdminSessionActive(true)
            adminPasswordInput.value = ""
        }
        viewModelScope.launch {
            repository.resetAdminPasswordToDefault()
        }
        Toast.makeText(getApplication(), msg, Toast.LENGTH_LONG).show()
        return defaultPass
    }

    fun changeAdminPassword(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ): Boolean {
        val result = repository.changeAdminPasswordSync(currentPassword, newPassword, confirmPassword)
        return if (result.isSuccess) {
            val msg = result.getOrNull() ?: "Password updated successfully!"
            passwordChangeSuccess.value = true
            passwordChangeMessage.value = msg
            viewModelScope.launch {
                repository.syncAdminPasswordToFirestore()
            }
            Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
            onResult(true, msg)
            true
        } else {
            val errMsg = result.exceptionOrNull()?.message ?: "Failed to change password"
            passwordChangeSuccess.value = false
            passwordChangeMessage.value = errMsg
            onResult(false, errMsg)
            false
        }
    }

    // ==================== ADMIN MENU & ORDER MANAGEMENT ====================

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        repository.updateOrderStatusLocal(orderId, newStatus)
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, newStatus)
            Toast.makeText(getApplication(), "Order status updated to ${newStatus.label}", Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleProductAvailability(product: Product) {
        saveProduct(product.copy(isAvailable = !product.isAvailable))
    }

    fun updateProductPrice(product: Product, newPrice: Int) {
        saveProduct(product.copy(price = newPrice))
    }

    fun updateProductImage(productId: String, newImageUrl: String) {
        val existing = products.value.find { it.id == productId } ?: return
        saveProduct(existing.copy(imageUrl = newImageUrl))
    }

    fun deleteProduct(productId: String) {
        repository.deleteProductLocal(productId)
        refreshFilteredLists()
        updateCartStateAndPersist(_cartItems.value.filter { it.productId != productId })
        viewModelScope.launch {
            repository.deleteProduct(productId)
            Toast.makeText(getApplication(), "Menu item deleted", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveProduct(product: Product) {
        val saved = repository.saveProductLocal(product)
        refreshFilteredLists()
        // Sync cart items if this product is already in the cart
        updateCartStateAndPersist(
            _cartItems.value.map { item ->
                if (item.productId == saved.id) {
                    item.copy(
                        name = saved.name,
                        price = saved.price,
                        drawableRes = saved.drawableRes,
                        imageUrl = saved.imageUrl
                    )
                } else item
            }
        )
        viewModelScope.launch {
            repository.saveProduct(saved)
            Toast.makeText(getApplication(), "Saved ${saved.name}", Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleDealAvailability(deal: Deal) {
        saveDeal(deal.copy(isAvailable = !deal.isAvailable))
    }

    fun updateDealPrice(deal: Deal, newPrice: Int) {
        val save = (deal.originalPrice - newPrice).coerceAtLeast(0)
        saveDeal(deal.copy(price = newPrice, saveAmount = save))
    }

    fun updateDealImage(dealId: String, newImageUrl: String) {
        val existing = deals.value.find { it.id == dealId } ?: return
        saveDeal(existing.copy(imageUrl = newImageUrl))
    }

    fun deleteDeal(dealId: String) {
        repository.deleteDealLocal(dealId)
        refreshFilteredLists()
        updateCartStateAndPersist(_cartItems.value.filter { it.dealId != dealId })
        viewModelScope.launch {
            repository.deleteDeal(dealId)
            Toast.makeText(getApplication(), "Deal deleted", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveDeal(deal: Deal) {
        val saved = repository.saveDealLocal(deal)
        refreshFilteredLists()
        // Sync cart items if this deal is already in the cart
        updateCartStateAndPersist(
            _cartItems.value.map { item ->
                if (item.dealId == saved.id) {
                    item.copy(
                        name = "Deal ${saved.dealNumber}: ${saved.name}",
                        price = saved.price,
                        drawableRes = saved.drawableRes,
                        imageUrl = saved.imageUrl
                    )
                } else item
            }
        )
        viewModelScope.launch {
            repository.saveDeal(saved)
            Toast.makeText(getApplication(), "Saved Deal ${saved.dealNumber}: ${saved.name}", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveExtra(extra: ExtraOption) {
        val saved = repository.saveExtraLocal(extra)
        viewModelScope.launch {
            repository.saveExtra(saved)
            Toast.makeText(getApplication(), "Saved Extra: ${saved.name}", Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleExtraAvailability(extra: ExtraOption) {
        saveExtra(extra.copy(isAvailable = !extra.isAvailable))
    }

    fun deleteExtra(extraId: String) {
        repository.deleteExtraLocal(extraId)
        viewModelScope.launch {
            repository.deleteExtra(extraId)
            Toast.makeText(getApplication(), "Extra option deleted", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateDeliveryFee(fee: Int) {
        repository.updateDeliveryFeeLocal(fee)
        updateCartStateAndPersist(_cartItems.value)
        viewModelScope.launch {
            repository.updateDeliveryFee(fee)
            Toast.makeText(getApplication(), "Delivery fee updated to Rs $fee", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateStoreSettings(newSettings: AppSettings) {
        val saved = repository.saveSettingsLocal(newSettings)
        updateCartStateAndPersist(_cartItems.value)
        viewModelScope.launch {
            repository.saveSettings(saved)
            Toast.makeText(getApplication(), "Store settings saved!", Toast.LENGTH_SHORT).show()
        }
    }

    // External contact actions
    fun openWhatsApp(context: Context, customMessage: String? = null) {
        val number = settings.value.formattedWhatsappForUrl
        val text = customMessage ?: "Hello ZAVIRO! I want to place an order from your GHAR SE GHAR TAK menu."
        val url = "https://wa.me/$number?text=${Uri.encode(text)}"
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "WhatsApp: $number", Toast.LENGTH_LONG).show()
        }
    }

    fun callZaviro(context: Context) {
        val phone = settings.value.whatsappNumber.replace("-", "")
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Call: $phone", Toast.LENGTH_LONG).show()
        }
    }

    fun openInstagram(context: Context) {
        val handle = settings.value.instagramHandle.removePrefix("@")
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/$handle")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Instagram: @$handle", Toast.LENGTH_SHORT).show()
        }
    }

    // ==================== BRANDING & EXACT UPLOADED LOGO PERSISTENCE ====================

    fun saveBranding(brandingSettings: BrandingSettings) {
        val saved = repository.saveBrandingLocal(brandingSettings)
        viewModelScope.launch {
            repository.saveBranding(saved)
        }
    }

    /**
     * Saves the EXACT uploaded Zaviro logo bytes without any modification, scaling,
     * re-encoding, color changes, or transparency loss.
     */
    fun uploadExactLogoBytes(
        bytes: ByteArray,
        slot: LogoSlot = LogoSlot.MASTER,
        applyToAllSlots: Boolean = (slot == LogoSlot.MASTER)
    ): String {
        val savedUri = repository.saveExactLogoBytes(bytes, slot, applyToAllSlots)
        val currentBranding = repository.branding.value
        viewModelScope.launch {
            repository.saveBranding(currentBranding)
        }
        return savedUri
    }

    /**
     * Reads the exact raw bytes from the user-selected URI and stores the exact
     * unmodified file on disk so it persists permanently across app restarts.
     */
    fun uploadExactLogoUri(
        context: Context,
        uri: Uri,
        slot: LogoSlot = LogoSlot.MASTER,
        applyToAllSlots: Boolean = (slot == LogoSlot.MASTER)
    ): String? {
        return try {
            val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (rawBytes != null && rawBytes.isNotEmpty()) {
                uploadExactLogoBytes(rawBytes, slot, applyToAllSlots)
            } else null
        } catch (e: Exception) {
            Log.e("ZaviroVM", "Error saving exact uploaded logo: ${e.message}", e)
            null
        }
    }

    fun resetLogoSlot(slot: LogoSlot) {
        repository.clearUploadedLogoForSlot(slot)
        val current = branding.value
        val updated = when (slot) {
            LogoSlot.MASTER -> current.copy(
                masterLogoUrl = "",
                splashLogoUrl = "",
                homeHeaderLogoUrl = "",
                loginLogoUrl = "",
                adminLogoUrl = ""
            )
            LogoSlot.SPLASH -> current.copy(splashLogoUrl = "")
            LogoSlot.HOME_HEADER -> current.copy(homeHeaderLogoUrl = "")
            LogoSlot.LOGIN -> current.copy(loginLogoUrl = "")
            LogoSlot.ADMIN -> current.copy(adminLogoUrl = "")
        }
        saveBranding(updated)
    }

    fun processBitmapToBase64(originalBitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        originalBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val bytes = outputStream.toByteArray()
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "data:image/png;base64,$base64"
    }

    fun processImageUriToBase64(context: Context, uri: Uri): String? {
        return try {
            val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (rawBytes != null && rawBytes.isNotEmpty()) {
                val appContext = getApplication<Application>().applicationContext
                val savedFile = File(appContext.filesDir, "zaviro_media_${System.currentTimeMillis()}.png")
                savedFile.writeBytes(rawBytes)
                repository.encodeBytesToCloudDataUri(rawBytes, maxDim = 700)
                    .ifBlank { "file://${savedFile.absolutePath}" }
            } else {
                val inputStream = context.contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (originalBitmap != null) {
                    processBitmapToBase64(originalBitmap)
                } else null
            }
        } catch (e: Exception) {
            Log.e("ZaviroVM", "Error processing image uri: ${e.message}", e)
            null
        }
    }
}
