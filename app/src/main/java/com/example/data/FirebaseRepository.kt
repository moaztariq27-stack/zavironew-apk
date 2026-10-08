package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.example.R
import com.example.model.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

class FirebaseRepository(
    private val context: Context,
    private val injectedFirestore: FirebaseFirestore? = null,
    private val autoStartSync: Boolean = (injectedFirestore == null)
) {
    constructor(firestore: FirebaseFirestore, context: Context) : this(
        context = context,
        injectedFirestore = firestore,
        autoStartSync = false
    )

    private val tag = "ZaviroRepo"

    private val prefs: SharedPreferences =
        context.getSharedPreferences("zaviro_local_store", Context.MODE_PRIVATE)

    private val firestore: FirebaseFirestore by lazy {
        injectedFirestore ?: run {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            val dbId = context.applicationContext.getString(R.string.firestore_database_id)
            FirebaseFirestore.getInstance(dbId)
        }
    }

    private fun currentAuthUid(): String? {
        return try {
            FirebaseAuth.getInstance().currentUser?.uid
        } catch (_: Exception) {
            null
        }
    }

    private fun requireUserId(): String {
        return currentAuthUid()
            ?: throw IllegalStateException("User must be signed in before accessing user-scoped Firestore resources.")
    }

    private fun DocumentSnapshot.readEpochMillis(field: String, defaultVal: Long = 0L): Long {
        return try {
            val ts = getTimestamp(field, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (ts != null) {
                ts.toDate().time
            } else {
                val raw = get(field)
                (raw as? Number)?.toLong() ?: defaultVal
            }
        } catch (_: Exception) {
            val raw = get(field)
            (raw as? Number)?.toLong() ?: defaultVal
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _extras = MutableStateFlow<List<ExtraOption>>(
        loadExtrasFromPrefs() ?: ZaviroMenuData.standardExtras
    )
    val extras: StateFlow<List<ExtraOption>> = _extras.asStateFlow()

    private val _products = MutableStateFlow<List<Product>>(
        loadProductsFromPrefs() ?: ZaviroMenuData.initialProducts
    )
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _deals = MutableStateFlow<List<Deal>>(
        loadDealsFromPrefs() ?: ZaviroMenuData.initialDeals
    )
    val deals: StateFlow<List<Deal>> = _deals.asStateFlow()

    private val _orders = MutableStateFlow<List<Order>>(
        loadOrdersFromPrefs() ?: emptyList()
    )
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _settings = MutableStateFlow(loadSettingsFromPrefs() ?: AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _branding = MutableStateFlow(loadBrandingFromPrefs() ?: BrandingSettings())
    val branding: StateFlow<BrandingSettings> = _branding.asStateFlow()

    private val hasInitialLocalBrandingOrSettingsCache: Boolean =
        prefs.contains("saved_branding_json") ||
            prefs.contains("saved_settings_json") ||
            File(context.filesDir, "zaviro_exact_uploaded_logo.png").let { it.exists() && it.length() > 0L }

    private val _isInitialSettingsLoaded = MutableStateFlow(hasInitialLocalBrandingOrSettingsCache)
    val isInitialSettingsLoaded: StateFlow<Boolean> = _isInitialSettingsLoaded.asStateFlow()

    private val _customerProfile = MutableStateFlow(loadCustomerProfileFromPrefs() ?: CustomerProfile())
    val customerProfile: StateFlow<CustomerProfile> = _customerProfile.asStateFlow()

    private val _savedAddresses = MutableStateFlow<List<SavedAddress>>(
        loadAddressesFromPrefs() ?: listOf(
            SavedAddress(
                id = "addr_1",
                label = "Home",
                fullAddress = "House 14-B, Street 3, Block H, Gulberg III",
                landmark = "Near Main Boulevard",
                area = "Gulberg, Lahore",
                phone = "0300-1234567"
            )
        )
    )
    val savedAddresses: StateFlow<List<SavedAddress>> = _savedAddresses.asStateFlow()

    private var productsListener: ListenerRegistration? = null
    private var dealsListener: ListenerRegistration? = null
    private var extrasListener: ListenerRegistration? = null
    private var ordersListener: ListenerRegistration? = null
    private var settingsListener: ListenerRegistration? = null
    private var brandingListener: ListenerRegistration? = null
    private var customerProfileListener: ListenerRegistration? = null

    // Derived initial hashes so both "zaviro786" and "zaviro.pakistan@786" work out-of-the-box and after reset
    private val initialDefaultPasswordHash: String by lazy {
        val obfuscated = byteArrayOf(
            0x20, 0x3B, 0x2C, 0x33, 0x28, 0x35, 0x74, 0x2A, 0x3B, 0x31,
            0x33, 0x29, 0x2E, 0x3B, 0x34, 0x1A, 0x6D, 0x62, 0x6C
        )
        val decoded = ByteArray(obfuscated.size) { idx ->
            (obfuscated[idx].toInt() xor 0x5A).toByte()
        }
        val hash = computeAdminPasswordHash(String(decoded, Charsets.UTF_8), INITIAL_ADMIN_SALT)
        decoded.fill(0)
        hash
    }

    private val simpleDefaultPasswordHash: String by lazy {
        val obfuscated = byteArrayOf(
            0x20, 0x3B, 0x2C, 0x33, 0x28, 0x35, 0x6D, 0x62, 0x6C
        )
        val decoded = ByteArray(obfuscated.size) { idx ->
            (obfuscated[idx].toInt() xor 0x5A).toByte()
        }
        val hash = computeAdminPasswordHash(String(decoded, Charsets.UTF_8), INITIAL_ADMIN_SALT)
        decoded.fill(0)
        hash
    }

    init {
        // Ensure no legacy plain-text or stale v1-v5 password keys ever block default login
        if (prefs.contains("admin_custom_password") ||
            prefs.contains("has_custom_admin_password") ||
            prefs.contains("admin_v3_has_custom") ||
            prefs.contains("admin_v4_has_custom") ||
            prefs.contains("admin_v5_has_custom")
        ) {
            prefs.edit()
                .remove("admin_custom_password")
                .remove("has_custom_admin_password")
                .remove("admin_password_hash")
                .remove("admin_password_salt")
                .remove("admin_password_updated_at")
                .remove("admin_v3_has_custom")
                .remove("admin_v3_password_hash")
                .remove("admin_v3_password_salt")
                .remove("admin_v3_updated_at")
                .remove("admin_v4_has_custom")
                .remove("admin_v4_password_hash")
                .remove("admin_v4_password_salt")
                .remove("admin_v4_updated_at")
                .remove("admin_v5_has_custom")
                .remove("admin_v5_password_hash")
                .remove("admin_v5_password_salt")
                .remove("admin_v5_updated_at")
                .commit()
        }
        if (autoStartSync) {
            startSync()
            val initialProfile = _customerProfile.value
            val activeUid = currentAuthUid()
            if (initialProfile.isAuthenticated && activeUid != null && initialProfile.uid == activeUid) {
                listenToCustomerProfile(activeUid)
            }
        }
    }

    // ==================== ADMIN SESSION & PASSWORD MANAGEMENT (PASSWORD-ONLY) ====================

    companion object {
        private const val INITIAL_ADMIN_SALT = "zaviro_pk_initial_salt_v2"
        private const val ADMIN_SECURITY_SCHEMA_V6 = 6L
        private const val KEY_HAS_CUSTOM_PASS = "admin_v6_has_custom"
        private const val KEY_PASS_HASH = "admin_v6_password_hash"
        private const val KEY_PASS_SALT = "admin_v6_password_salt"
        private const val KEY_PASS_UPDATED_AT = "admin_v6_updated_at"
    }

    private fun sanitizePasswordInput(raw: String): String {
        return raw
            .replace("\u200B", "")
            .replace("\u200C", "")
            .replace("\u200D", "")
            .replace("\uFEFF", "")
            .replace("\u00A0", " ")
            .trim()
    }

    private fun computeAdminPasswordHash(password: String, salt: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        var bytes = "$salt:$password:zaviro_admin_auth_v2".toByteArray(Charsets.UTF_8)
        repeat(4096) { round ->
            digest.reset()
            digest.update(bytes)
            digest.update((round and 0xFF).toByte())
            bytes = digest.digest()
        }
        return bytes.joinToString("") { "%02x".format(java.util.Locale.US, it) }
    }

    private fun generateRandomSaltHex(): String {
        val saltBytes = ByteArray(16)
        java.security.SecureRandom().nextBytes(saltBytes)
        return saltBytes.joinToString("") { "%02x".format(java.util.Locale.US, it) }
    }

    fun isAdminSessionActive(): Boolean {
        return prefs.getBoolean("admin_is_logged_in", false)
    }

    fun setAdminSessionActive(loggedIn: Boolean) {
        prefs.edit().putBoolean("admin_is_logged_in", loggedIn).commit()
        if (loggedIn) {
            triggerAdminCloudSync()
        }
    }

    private fun matchesTargetHash(candidate: String, salt: String, targetHashBytes: ByteArray): Boolean {
        val hash = computeAdminPasswordHash(candidate, salt)
        return java.security.MessageDigest.isEqual(
            hash.toByteArray(Charsets.UTF_8),
            targetHashBytes
        )
    }

    private fun matchesAnyDefaultPassword(candidate: String): Boolean {
        if (candidate.isBlank()) return false
        val primaryBytes = initialDefaultPasswordHash.toByteArray(Charsets.UTF_8)
        val simpleBytes = simpleDefaultPasswordHash.toByteArray(Charsets.UTF_8)
        return matchesTargetHash(candidate, INITIAL_ADMIN_SALT, primaryBytes) ||
            matchesTargetHash(candidate, INITIAL_ADMIN_SALT, simpleBytes)
    }

    fun verifyAdminPassword(input: String): Boolean {
        val cleanInput = sanitizePasswordInput(input)
        if (cleanInput.isBlank()) return false
        val compactInput = cleanInput.filterNot { it.isWhitespace() }
        val lowerClean = cleanInput.lowercase(java.util.Locale.ROOT)
        val lowerCompact = compactInput.lowercase(java.util.Locale.ROOT)

        val hasCustom = prefs.getBoolean(KEY_HAS_CUSTOM_PASS, false)
        val customHash = prefs.getString(KEY_PASS_HASH, null)
        val customSalt = prefs.getString(KEY_PASS_SALT, null)

        return if (hasCustom && !customHash.isNullOrBlank() && !customSalt.isNullOrBlank()) {
            val targetBytes = customHash.toByteArray(Charsets.UTF_8)
            matchesTargetHash(cleanInput, customSalt, targetBytes) ||
                (compactInput.isNotBlank() && matchesTargetHash(compactInput, customSalt, targetBytes))
        } else {
            matchesAnyDefaultPassword(cleanInput) ||
                matchesAnyDefaultPassword(lowerClean) ||
                matchesAnyDefaultPassword(compactInput) ||
                matchesAnyDefaultPassword(lowerCompact)
        }
    }

    fun resetAdminPasswordToDefaultSync(): String {
        val now = System.currentTimeMillis()
        prefs.edit()
            .remove("admin_custom_password")
            .remove(KEY_PASS_HASH)
            .remove(KEY_PASS_SALT)
            .putBoolean(KEY_HAS_CUSTOM_PASS, false)
            .putLong(KEY_PASS_UPDATED_AT, now)
            .commit()
        return "zaviro786"
    }

    suspend fun resetAdminPasswordToDefault(): String {
        val defaultPass = resetAdminPasswordToDefaultSync()
        val now = prefs.getLong(KEY_PASS_UPDATED_AT, System.currentTimeMillis())
        try {
            val map = mapOf(
                "adminPassword" to FieldValue.delete(),
                "passwordHash" to "",
                "passwordSalt" to "",
                "schemaVersion" to ADMIN_SECURITY_SCHEMA_V6,
                "hasCustomPassword" to false,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("settings").document("admin_security")
                .set(map, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "settings/admin_security")
            Log.w(tag, "Admin password reset Firestore sync notice: ${e.message}")
        }
        return defaultPass
    }

    suspend fun verifyAdminPasswordWithRemoteSync(password: String): Boolean {
        if (sanitizePasswordInput(password).isBlank()) return false
        try {
            val doc = withTimeoutOrNull(3000L) {
                firestore.collection("settings").document("admin_security").get(Source.SERVER).await()
            }
            if (doc != null && doc.exists()) {
                applyRemoteAdminSecuritySnapshot(doc)
            }
        } catch (_: Exception) {}

        return verifyAdminPassword(password)
    }

    fun changeAdminPasswordSync(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ): Result<String> {
        val cleanCurrent = sanitizePasswordInput(currentPassword)
        val cleanNew = sanitizePasswordInput(newPassword)
        val cleanConfirm = sanitizePasswordInput(confirmPassword)
        if (cleanCurrent.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter your current password."))
        }
        if (!verifyAdminPassword(cleanCurrent)) {
            return Result.failure(IllegalArgumentException("Current password is incorrect."))
        }
        if (cleanNew.isBlank()) {
            return Result.failure(IllegalArgumentException("New password cannot be empty."))
        }
        if (cleanNew.length < 4) {
            return Result.failure(IllegalArgumentException("New password must be at least 4 characters."))
        }
        if (cleanNew != cleanConfirm) {
            return Result.failure(IllegalArgumentException("New password and confirmation do not match."))
        }

        val newSalt = generateRandomSaltHex()
        val newHash = computeAdminPasswordHash(cleanNew, newSalt)
        val now = System.currentTimeMillis()

        prefs.edit()
            .remove("admin_custom_password")
            .putString(KEY_PASS_HASH, newHash)
            .putString(KEY_PASS_SALT, newSalt)
            .putBoolean(KEY_HAS_CUSTOM_PASS, true)
            .putLong(KEY_PASS_UPDATED_AT, now)
            .commit()

        return Result.success("Admin password updated successfully! Please use your new password for future logins.")
    }

    suspend fun changeAdminPassword(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ): Result<String> {
        val res = changeAdminPasswordSync(currentPassword, newPassword, confirmPassword)
        if (res.isSuccess) {
            syncAdminPasswordToFirestore()
        }
        return res
    }

    suspend fun syncAdminPasswordToFirestore() {
        val customHash = prefs.getString(KEY_PASS_HASH, null).orEmpty()
        val customSalt = prefs.getString(KEY_PASS_SALT, null).orEmpty()
        val updatedAt = prefs.getLong(KEY_PASS_UPDATED_AT, System.currentTimeMillis())

        if (customHash.isNotBlank() && customSalt.isNotBlank()) {
            syncAdminPasswordHashToFirestore(customHash, customSalt, updatedAt)
        }
    }

    private suspend fun syncAdminPasswordHashToFirestore(
        passwordHash: String,
        passwordSalt: String,
        updatedAt: Long
    ) {
        try {
            val map = mapOf(
                "adminPassword" to FieldValue.delete(),
                "schemaVersion" to ADMIN_SECURITY_SCHEMA_V6,
                "passwordHash" to passwordHash,
                "passwordSalt" to passwordSalt,
                "hasCustomPassword" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("settings").document("admin_security")
                .set(map, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "settings/admin_security")
            Log.w(tag, "Admin security hash sync notice: ${e.message}")
        }
    }

    // ==================== CUSTOMER PROFILE & AVATAR PERSISTENCE ====================

    fun saveCustomerProfileSync(profile: CustomerProfile): CustomerProfile {
        val updated = profile.copy(updatedAt = System.currentTimeMillis())
        _customerProfile.value = updated
        try {
            val obj = JSONObject().apply {
                put("uid", updated.uid)
                put("name", updated.name)
                put("phone", updated.phone)
                put("email", updated.email)
                put("address", updated.address)
                put("notes", updated.notes)
                put("profileImageUrl", updated.profileImageUrl)
                put("authProvider", updated.authProvider)
                put("isAuthenticated", updated.isAuthenticated)
                put("updatedAt", updated.updatedAt)
            }
            prefs.edit()
                .putString("saved_customer_profile_json", obj.toString())
                .putLong("customer_profile_updated_at", updated.updatedAt)
                .commit()
        } catch (e: Exception) {
            Log.e(tag, "Error saving customer profile to prefs: ${e.message}")
        }

        if (updated.isAuthenticated && updated.uid.isNotBlank()) {
            scope.launch {
                pushCustomerProfileToFirestore(updated)
            }
        }
        return updated
    }

    suspend fun pushCustomerProfileToFirestore(profile: CustomerProfile) {
        val activeUid = currentAuthUid()
        if (profile.uid.isBlank() || activeUid == null || activeUid != profile.uid) return
        try {
            val cloudAvatar = convertLocalFileUrlToFirestoreDataUri(profile.profileImageUrl, maxDim = 400)
            val data = hashMapOf<String, Any>(
                "uid" to profile.uid,
                "name" to profile.name.ifBlank { "Zaviro Customer" },
                "phone" to profile.phone,
                "email" to profile.email,
                "address" to profile.address,
                "notes" to profile.notes,
                "profileImageUrl" to cloudAvatar,
                "authProvider" to profile.authProvider.takeIf { it in listOf("google", "phone", "") }.orEmpty(),
                "isAuthenticated" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("customers").document(profile.uid)
                .set(data, SetOptions.merge())
                .await()
            firestore.collection("users").document(profile.uid)
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "customers/${profile.uid}")
            Log.w(tag, "Customer profile cloud sync warning: ${e.message}")
        }
    }

    private fun listenToCustomerProfile(uid: String) {
        val activeUid = currentAuthUid()
        if (uid.isBlank() || activeUid == null || activeUid != uid) return
        customerProfileListener?.remove()
        val path = "customers/$uid"
        customerProfileListener = firestore.collection("customers").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.GET, path)
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) return@addSnapshotListener
                val current = _customerProfile.value
                if (current.uid != uid) return@addSnapshotListener
                val remoteUpdatedAt = snapshot.readEpochMillis("updatedAt", 0L)
                if (remoteUpdatedAt > current.updatedAt) {
                    val remoteImg = snapshot.getString("profileImageUrl")?.trim().orEmpty()
                    val resolvedImg = if (current.profileImageUrl.startsWith("file://") &&
                        File(current.profileImageUrl.removePrefix("file://")).exists()
                    ) {
                        current.profileImageUrl
                    } else {
                        remoteImg.ifBlank { current.profileImageUrl }
                    }
                    val merged = current.copy(
                        name = snapshot.getString("name")?.takeIf { it.isNotBlank() } ?: current.name,
                        phone = snapshot.getString("phone")?.takeIf { it.isNotBlank() } ?: current.phone,
                        email = snapshot.getString("email")?.takeIf { it.isNotBlank() } ?: current.email,
                        address = snapshot.getString("address")?.takeIf { it.isNotBlank() } ?: current.address,
                        notes = snapshot.getString("notes") ?: current.notes,
                        profileImageUrl = resolvedImg,
                        authProvider = snapshot.getString("authProvider")?.takeIf { it.isNotBlank() } ?: current.authProvider,
                        isAuthenticated = true,
                        updatedAt = remoteUpdatedAt
                    )
                    _customerProfile.value = merged
                }
            }
    }

    fun authenticateCustomerLocal(
        uid: String,
        displayName: String?,
        email: String?,
        phone: String?,
        photoUrl: String?,
        provider: String
    ): CustomerProfile {
        val current = _customerProfile.value
        val resolvedName = displayName?.takeIf { it.isNotBlank() }
            ?: current.name.takeIf { it.isNotBlank() }
            ?: "Zaviro Customer"
        val resolvedPhone = phone?.takeIf { it.isNotBlank() }
            ?: current.phone
        val resolvedEmail = email?.takeIf { it.isNotBlank() }
            ?: current.email
        val resolvedPhoto = current.profileImageUrl.takeIf { it.isNotBlank() }
            ?: photoUrl.orEmpty()

        val updatedProfile = CustomerProfile(
            uid = uid,
            name = resolvedName,
            phone = resolvedPhone,
            email = resolvedEmail,
            address = current.address,
            notes = current.notes,
            profileImageUrl = resolvedPhoto,
            authProvider = provider,
            isAuthenticated = true,
            updatedAt = System.currentTimeMillis()
        )
        val saved = saveCustomerProfileSync(updatedProfile)
        listenToCustomerProfile(uid)
        return saved
    }

    suspend fun syncAuthenticatedCustomerWithFirestore(
        uid: String,
        displayName: String?,
        email: String?,
        phone: String?,
        photoUrl: String?,
        provider: String
    ): CustomerProfile {
        val localSaved = authenticateCustomerLocal(uid, displayName, email, phone, photoUrl, provider)
        var remoteName: String? = null
        var remotePhone: String? = null
        var remoteEmail: String? = null
        var remoteAddress: String? = null
        var remoteNotes: String? = null
        var remotePhoto: String? = null

        val activeUid = currentAuthUid()
        if (activeUid != null && activeUid == uid) {
            try {
                val snap = withTimeoutOrNull(3000L) {
                    firestore.collection("customers").document(uid).get().await()
                }
                if (snap != null && snap.exists()) {
                    remoteName = snap.getString("name")?.takeIf { it.isNotBlank() }
                    remotePhone = snap.getString("phone")?.takeIf { it.isNotBlank() }
                    remoteEmail = snap.getString("email")?.takeIf { it.isNotBlank() }
                    remoteAddress = snap.getString("address")?.takeIf { it.isNotBlank() }
                    remoteNotes = snap.getString("notes")
                    remotePhoto = snap.getString("profileImageUrl")?.takeIf { it.isNotBlank() }
                }
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.GET, "customers/$uid")
                Log.w(tag, "Could not fetch existing customer doc for $uid: ${e.message}")
            }
        }

        val resolvedName = displayName?.takeIf { it.isNotBlank() }
            ?: remoteName
            ?: localSaved.name
        val resolvedPhone = phone?.takeIf { it.isNotBlank() }
            ?: remotePhone
            ?: localSaved.phone
        val resolvedEmail = email?.takeIf { it.isNotBlank() }
            ?: remoteEmail
            ?: localSaved.email
        val resolvedAddress = remoteAddress
            ?: localSaved.address
        val resolvedNotes = remoteNotes
            ?: localSaved.notes
        val resolvedPhoto = localSaved.profileImageUrl.takeIf { it.isNotBlank() }
            ?: remotePhoto
            ?: photoUrl.orEmpty()

        val updatedProfile = localSaved.copy(
            name = resolvedName,
            phone = resolvedPhone,
            email = resolvedEmail,
            address = resolvedAddress,
            notes = resolvedNotes,
            profileImageUrl = resolvedPhoto,
            updatedAt = System.currentTimeMillis()
        )
        return saveCustomerProfileSync(updatedProfile)
    }

    fun logoutCustomerLocal(): CustomerProfile {
        customerProfileListener?.remove()
        customerProfileListener = null
        val loggedOut = _customerProfile.value.copy(
            uid = "",
            email = "",
            authProvider = "",
            isAuthenticated = false,
            updatedAt = System.currentTimeMillis()
        )
        return saveCustomerProfileSync(loggedOut)
    }

    private fun loadCustomerProfileFromPrefs(): CustomerProfile? {
        val json = prefs.getString("saved_customer_profile_json", null)
        val avatarFile = File(context.filesDir, "customer_profile_avatar.png")
        val fallbackAvatarPath = if (avatarFile.exists() && avatarFile.length() > 0) {
            "file://${avatarFile.absolutePath}"
        } else ""

        if (json == null) {
            return if (fallbackAvatarPath.isNotEmpty()) {
                CustomerProfile(profileImageUrl = fallbackAvatarPath)
            } else null
        }

        return try {
            val obj = JSONObject(json)
            val savedImg = obj.optString("profileImageUrl", "").ifBlank { fallbackAvatarPath }
            CustomerProfile(
                uid = obj.optString("uid", ""),
                name = obj.optString("name", "Muhammad Ahmed"),
                phone = obj.optString("phone", "0304-1234567"),
                email = obj.optString("email", ""),
                address = obj.optString("address", "House 12-A, Gulberg III, Lahore"),
                notes = obj.optString("notes", ""),
                profileImageUrl = savedImg,
                authProvider = obj.optString("authProvider", ""),
                isAuthenticated = obj.optBoolean("isAuthenticated", false),
                updatedAt = obj.optLong("updatedAt", 0L)
            )
        } catch (e: Exception) {
            null
        }
    }

    fun saveCustomerAvatarBytes(bytes: ByteArray): String {
        val avatarFile = File(context.filesDir, "customer_profile_avatar.png")
        avatarFile.writeBytes(bytes)
        val pathUri = "file://${avatarFile.absolutePath}"
        saveCustomerProfileSync(_customerProfile.value.copy(profileImageUrl = pathUri))
        return pathUri
    }

    // ==================== EXACT UPLOADED LOGO FILE STORAGE ====================

    fun saveExactLogoBytes(
        bytes: ByteArray,
        slot: LogoSlot = LogoSlot.MASTER,
        applyToAllSlots: Boolean = (slot == LogoSlot.MASTER)
    ): String {
        val slotFile = File(context.filesDir, "zaviro_logo_${slot.name.lowercase()}.png")
        slotFile.writeBytes(bytes)
        val fileUri = "file://${slotFile.absolutePath}"

        val masterFile = File(context.filesDir, "zaviro_exact_uploaded_logo.png")
        if (slot == LogoSlot.MASTER || applyToAllSlots || !masterFile.exists()) {
            masterFile.writeBytes(bytes)
        }
        val masterUri = "file://${masterFile.absolutePath}"

        if (slot == LogoSlot.MASTER || applyToAllSlots) {
            val current = _branding.value
            val updated = if (applyToAllSlots) {
                current.copy(
                    masterLogoUrl = masterUri,
                    splashLogoUrl = masterUri,
                    homeHeaderLogoUrl = masterUri,
                    loginLogoUrl = masterUri,
                    adminLogoUrl = masterUri,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                current.copy(
                    masterLogoUrl = masterUri,
                    splashLogoUrl = current.splashLogoUrl.ifBlank { masterUri },
                    homeHeaderLogoUrl = current.homeHeaderLogoUrl.ifBlank { masterUri },
                    updatedAt = System.currentTimeMillis()
                )
            }
            saveBrandingLocal(updated)
            return masterUri
        } else {
            val current = _branding.value
            val updated = when (slot) {
                LogoSlot.MASTER -> current.copy(masterLogoUrl = fileUri)
                LogoSlot.SPLASH -> current.copy(
                    splashLogoUrl = fileUri,
                    masterLogoUrl = current.masterLogoUrl.ifBlank { fileUri },
                    homeHeaderLogoUrl = current.homeHeaderLogoUrl.ifBlank { fileUri }
                )
                LogoSlot.HOME_HEADER -> current.copy(
                    homeHeaderLogoUrl = fileUri,
                    masterLogoUrl = current.masterLogoUrl.ifBlank { fileUri },
                    splashLogoUrl = current.splashLogoUrl.ifBlank { fileUri }
                )
                LogoSlot.LOGIN -> current.copy(
                    loginLogoUrl = fileUri,
                    masterLogoUrl = current.masterLogoUrl.ifBlank { fileUri },
                    splashLogoUrl = current.splashLogoUrl.ifBlank { fileUri },
                    homeHeaderLogoUrl = current.homeHeaderLogoUrl.ifBlank { fileUri }
                )
                LogoSlot.ADMIN -> current.copy(
                    adminLogoUrl = fileUri,
                    masterLogoUrl = current.masterLogoUrl.ifBlank { fileUri },
                    splashLogoUrl = current.splashLogoUrl.ifBlank { fileUri },
                    homeHeaderLogoUrl = current.homeHeaderLogoUrl.ifBlank { fileUri }
                )
            }
            saveBrandingLocal(updated)
            return fileUri
        }
    }

    fun clearUploadedLogoForSlot(slot: LogoSlot) {
        val slotFile = File(context.filesDir, "zaviro_logo_${slot.name.lowercase()}.png")
        if (slotFile.exists()) slotFile.delete()
        if (slot == LogoSlot.MASTER) {
            val masterFile = File(context.filesDir, "zaviro_exact_uploaded_logo.png")
            if (masterFile.exists()) masterFile.delete()
        }
    }

    // ==================== LOCAL PERSISTENCE HELPERS ====================

    private fun resolveCategoryDrawable(category: String, isDeal: Boolean = false): Int {
        return when (category.trim().lowercase()) {
            "pasta", "pasta combos" -> R.drawable.zaviro_creamy_pasta
            "wraps & rolls", "wraps", "rolls" -> R.drawable.zaviro_wrap
            "crispy chicken", "chicken" -> R.drawable.zaviro_crispy_chicken
            "wings", "wings specials" -> R.drawable.zaviro_wings
            "fries", "special value" -> R.drawable.zaviro_loaded_fries
            "drinks", "family deals" -> R.drawable.zaviro_hero_banner
            "extras" -> R.drawable.zaviro_wrap
            else -> if (isDeal) R.drawable.zaviro_hero_banner else R.drawable.zaviro_creamy_pasta
        }
    }

    private fun getLocallyModifiedProductIds(): Set<String> {
        return prefs.getStringSet("locally_modified_product_ids", emptySet()) ?: emptySet()
    }

    private fun addLocallyModifiedProductId(id: String) {
        val current = getLocallyModifiedProductIds().toMutableSet()
        current.add(id)
        prefs.edit().putStringSet("locally_modified_product_ids", current).commit()
    }

    private fun removeLocallyModifiedProductId(id: String) {
        val current = getLocallyModifiedProductIds().toMutableSet()
        current.remove(id)
        prefs.edit().putStringSet("locally_modified_product_ids", current).commit()
    }

    private fun getLocallyModifiedDealIds(): Set<String> {
        return prefs.getStringSet("locally_modified_deal_ids", emptySet()) ?: emptySet()
    }

    private fun addLocallyModifiedDealId(id: String) {
        val current = getLocallyModifiedDealIds().toMutableSet()
        current.add(id)
        prefs.edit().putStringSet("locally_modified_deal_ids", current).commit()
    }

    private fun removeLocallyModifiedDealId(id: String) {
        val current = getLocallyModifiedDealIds().toMutableSet()
        current.remove(id)
        prefs.edit().putStringSet("locally_modified_deal_ids", current).commit()
    }

    private fun getLocallyModifiedOrderIds(): Set<String> {
        return prefs.getStringSet("locally_modified_order_ids", emptySet()) ?: emptySet()
    }

    private fun addLocallyModifiedOrderId(id: String) {
        val current = getLocallyModifiedOrderIds().toMutableSet()
        current.add(id)
        prefs.edit().putStringSet("locally_modified_order_ids", current).commit()
    }

    private fun getDeletedProductIds(): Set<String> {
        return prefs.getStringSet("deleted_product_ids", emptySet()) ?: emptySet()
    }

    private fun addDeletedProductId(id: String) {
        val current = getDeletedProductIds().toMutableSet()
        current.add(id)
        prefs.edit().putStringSet("deleted_product_ids", current).commit()
    }

    private fun getDeletedDealIds(): Set<String> {
        return prefs.getStringSet("deleted_deal_ids", emptySet()) ?: emptySet()
    }

    private fun addDeletedDealId(id: String) {
        val current = getDeletedDealIds().toMutableSet()
        current.add(id)
        prefs.edit().putStringSet("deleted_deal_ids", current).commit()
    }

    private fun saveProductsToPrefs(list: List<Product>) {
        try {
            val arr = JSONArray()
            list.forEach { p ->
                val obj = JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("category", p.category)
                    put("description", p.description)
                    put("ingredients", p.ingredients)
                    put("price", p.price)
                    put("isAvailable", p.isAvailable)
                    put("isPopular", p.isPopular)
                    put("drawableRes", p.drawableRes)
                    put("imageUrl", p.imageUrl)
                }
                arr.put(obj)
            }
            prefs.edit().putString("saved_products_json", arr.toString()).commit()
        } catch (e: Exception) {
            Log.e(tag, "Error saving products to prefs: ${e.message}")
        }
    }

    private fun loadProductsFromPrefs(): List<Product>? {
        val json = prefs.getString("saved_products_json", null) ?: return null
        return try {
            val arr = JSONArray(json)
            val currentExtras = _extras.value
            val list = mutableListOf<Product>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("id")
                val category = obj.optString("category", "Pasta")
                val defaultItem = ZaviroMenuData.initialProducts.find { it.id == id }
                val savedDrawable = obj.optInt("drawableRes", 0)
                val drawableRes = when {
                    defaultItem != null -> defaultItem.drawableRes
                    savedDrawable != 0 -> savedDrawable
                    else -> resolveCategoryDrawable(category, isDeal = false)
                }
                list.add(
                    Product(
                        id = id,
                        name = obj.optString("name"),
                        category = category,
                        description = obj.optString("description"),
                        ingredients = obj.optString("ingredients", ""),
                        price = obj.optInt("price", 0),
                        isAvailable = obj.optBoolean("isAvailable", true),
                        isPopular = obj.optBoolean("isPopular", false),
                        drawableRes = drawableRes,
                        imageUrl = obj.optString("imageUrl", ""),
                        availableExtras = currentExtras
                    )
                )
            }
            if (list.isNotEmpty()) list else null
        } catch (e: Exception) {
            Log.e(tag, "Error loading products from prefs: ${e.message}")
            null
        }
    }

    private fun saveDealsToPrefs(list: List<Deal>) {
        try {
            val arr = JSONArray()
            list.forEach { d ->
                val itemsArr = JSONArray()
                d.items.forEach { itemsArr.put(it) }
                val obj = JSONObject().apply {
                    put("id", d.id)
                    put("dealNumber", d.dealNumber)
                    put("name", d.name)
                    put("category", d.category)
                    put("items", itemsArr)
                    put("description", d.description)
                    put("price", d.price)
                    put("originalPrice", d.originalPrice)
                    put("saveAmount", d.saveAmount)
                    put("isAvailable", d.isAvailable)
                    put("drawableRes", d.drawableRes)
                    put("imageUrl", d.imageUrl)
                }
                arr.put(obj)
            }
            prefs.edit().putString("saved_deals_json", arr.toString()).commit()
        } catch (e: Exception) {
            Log.e(tag, "Error saving deals to prefs: ${e.message}")
        }
    }

    private fun loadDealsFromPrefs(): List<Deal>? {
        val json = prefs.getString("saved_deals_json", null) ?: return null
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<Deal>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("id")
                val dealNumber = obj.optInt("dealNumber", i + 1)
                val category = obj.optString("category", "Wraps & Rolls")
                val itemsArr = obj.optJSONArray("items") ?: JSONArray()
                val itemsList = mutableListOf<String>()
                for (j in 0 until itemsArr.length()) {
                    itemsList.add(itemsArr.optString(j))
                }
                val defaultDeal = ZaviroMenuData.initialDeals.find { it.id == id || it.dealNumber == dealNumber }
                val savedDrawable = obj.optInt("drawableRes", 0)
                val drawableRes = when {
                    defaultDeal != null -> defaultDeal.drawableRes
                    savedDrawable != 0 -> savedDrawable
                    else -> resolveCategoryDrawable(category, isDeal = true)
                }
                list.add(
                    Deal(
                        id = id,
                        dealNumber = dealNumber,
                        name = obj.optString("name"),
                        category = category,
                        items = itemsList,
                        description = obj.optString("description", ""),
                        price = obj.optInt("price", 0),
                        originalPrice = obj.optInt("originalPrice", 0),
                        saveAmount = obj.optInt("saveAmount", 0),
                        isAvailable = obj.optBoolean("isAvailable", true),
                        drawableRes = drawableRes,
                        imageUrl = obj.optString("imageUrl", "")
                    )
                )
            }
            if (list.isNotEmpty()) list.sortedBy { it.dealNumber } else null
        } catch (e: Exception) {
            Log.e(tag, "Error loading deals from prefs: ${e.message}")
            null
        }
    }

    private fun saveExtrasToPrefs(list: List<ExtraOption>) {
        try {
            val arr = JSONArray()
            list.forEach { e ->
                val obj = JSONObject().apply {
                    put("id", e.id)
                    put("name", e.name)
                    put("price", e.price)
                    put("description", e.description)
                    put("isAvailable", e.isAvailable)
                    put("imageUrl", e.imageUrl)
                }
                arr.put(obj)
            }
            prefs.edit().putString("saved_extras_json", arr.toString()).commit()
        } catch (e: Exception) {
            Log.e(tag, "Error saving extras to prefs: ${e.message}")
        }
    }

    private fun loadExtrasFromPrefs(): List<ExtraOption>? {
        val json = prefs.getString("saved_extras_json", null) ?: return null
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<ExtraOption>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ExtraOption(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        price = obj.optInt("price", 0),
                        description = obj.optString("description", ""),
                        isAvailable = obj.optBoolean("isAvailable", true),
                        imageUrl = obj.optString("imageUrl", "")
                    )
                )
            }
            if (list.isNotEmpty()) list else null
        } catch (e: Exception) {
            null
        }
    }

    private fun saveAddressesToPrefs(list: List<SavedAddress>) {
        try {
            val arr = JSONArray()
            list.forEach { a ->
                val obj = JSONObject().apply {
                    put("id", a.id)
                    put("label", a.label)
                    put("fullAddress", a.fullAddress)
                    put("landmark", a.landmark)
                    put("area", a.area)
                    put("phone", a.phone)
                }
                arr.put(obj)
            }
            prefs.edit().putString("saved_addresses_json", arr.toString()).commit()
        } catch (e: Exception) {
            Log.e(tag, "Error saving addresses to prefs: ${e.message}")
        }
    }

    private fun loadAddressesFromPrefs(): List<SavedAddress>? {
        val json = prefs.getString("saved_addresses_json", null) ?: return null
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<SavedAddress>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    SavedAddress(
                        id = obj.optString("id"),
                        label = obj.optString("label", "Home"),
                        fullAddress = obj.optString("fullAddress", ""),
                        landmark = obj.optString("landmark", ""),
                        area = obj.optString("area", "Lahore"),
                        phone = obj.optString("phone", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            null
        }
    }

    fun saveCartToPrefs(items: List<CartItem>) {
        try {
            val arr = JSONArray()
            items.forEach { item ->
                val extrasArr = JSONArray()
                item.selectedExtras.forEach { ex ->
                    extrasArr.put(
                        JSONObject().apply {
                            put("id", ex.id)
                            put("name", ex.name)
                            put("price", ex.price)
                            put("description", ex.description)
                            put("isAvailable", ex.isAvailable)
                            put("imageUrl", ex.imageUrl)
                        }
                    )
                }
                val obj = JSONObject().apply {
                    put("cartItemId", item.cartItemId)
                    put("productId", item.productId ?: "")
                    put("dealId", item.dealId ?: "")
                    put("name", item.name)
                    put("price", item.price)
                    put("quantity", item.quantity)
                    put("selectedExtras", extrasArr)
                    put("specialInstructions", item.specialInstructions)
                    put("drawableRes", item.drawableRes)
                    put("imageUrl", item.imageUrl)
                }
                arr.put(obj)
            }
            prefs.edit().putString("saved_cart_json", arr.toString()).commit()
        } catch (e: Exception) {
            Log.e(tag, "Error saving cart to prefs: ${e.message}")
        }
    }

    fun loadCartFromPrefs(): List<CartItem> {
        val json = prefs.getString("saved_cart_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<CartItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val extrasArr = obj.optJSONArray("selectedExtras") ?: JSONArray()
                val extrasList = mutableListOf<ExtraOption>()
                for (j in 0 until extrasArr.length()) {
                    val exObj = extrasArr.getJSONObject(j)
                    extrasList.add(
                        ExtraOption(
                            id = exObj.optString("id"),
                            name = exObj.optString("name"),
                            price = exObj.optInt("price", 0),
                            description = exObj.optString("description", ""),
                            isAvailable = exObj.optBoolean("isAvailable", true),
                            imageUrl = exObj.optString("imageUrl", "")
                        )
                    )
                }
                val prodId = obj.optString("productId", "").takeIf { it.isNotBlank() }
                val dealId = obj.optString("dealId", "").takeIf { it.isNotBlank() }
                list.add(
                    CartItem(
                        cartItemId = obj.optString("cartItemId", UUID.randomUUID().toString()),
                        productId = prodId,
                        dealId = dealId,
                        name = obj.optString("name"),
                        price = obj.optInt("price", 0),
                        quantity = obj.optInt("quantity", 1),
                        selectedExtras = extrasList,
                        specialInstructions = obj.optString("specialInstructions", ""),
                        drawableRes = obj.optInt("drawableRes", R.drawable.zaviro_creamy_pasta),
                        imageUrl = obj.optString("imageUrl", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveOrdersToPrefs(list: List<Order>) {
        try {
            val arr = JSONArray()
            list.forEach { o ->
                val itemsArr = JSONArray()
                o.items.forEach { item ->
                    itemsArr.put(
                        JSONObject().apply {
                            put("id", item.id)
                            put("name", item.name)
                            put("quantity", item.quantity)
                            put("unitPrice", item.unitPrice)
                            put("extrasSummary", item.extrasSummary)
                            put("specialInstructions", item.specialInstructions)
                        }
                    )
                }
                val obj = JSONObject().apply {
                    put("id", o.id)
                    put("orderNumber", o.orderNumber)
                    put("customerName", o.customerName)
                    put("phone", o.phone)
                    put("deliveryAddress", o.deliveryAddress)
                    put("orderNotes", o.orderNotes)
                    put("items", itemsArr)
                    put("subtotal", o.subtotal)
                    put("deliveryFee", o.deliveryFee)
                    put("total", o.total)
                    put("status", o.status.name)
                    put("paymentMethod", o.paymentMethod)
                    put("createdAt", o.createdAt)
                }
                arr.put(obj)
            }
            prefs.edit().putString("saved_orders_json", arr.toString()).commit()
        } catch (e: Exception) {
            Log.e(tag, "Error saving orders to prefs: ${e.message}")
        }
    }

    private fun loadOrdersFromPrefs(): List<Order>? {
        val json = prefs.getString("saved_orders_json", null) ?: return null
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<Order>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val itemsArr = obj.optJSONArray("items") ?: JSONArray()
                val itemsList = mutableListOf<OrderItemDto>()
                for (j in 0 until itemsArr.length()) {
                    val itemObj = itemsArr.getJSONObject(j)
                    itemsList.add(
                        OrderItemDto(
                            id = itemObj.optString("id"),
                            name = itemObj.optString("name"),
                            quantity = itemObj.optInt("quantity", 1),
                            unitPrice = itemObj.optInt("unitPrice", 0),
                            extrasSummary = itemObj.optString("extrasSummary", ""),
                            specialInstructions = itemObj.optString("specialInstructions", "")
                        )
                    )
                }
                val statusStr = obj.optString("status", OrderStatus.RECEIVED.name)
                val status = try { OrderStatus.valueOf(statusStr) } catch (_: Exception) { OrderStatus.RECEIVED }
                list.add(
                    Order(
                        id = obj.optString("id"),
                        orderNumber = obj.optString("orderNumber", "#ZAV-0000"),
                        customerName = obj.optString("customerName"),
                        phone = obj.optString("phone"),
                        deliveryAddress = obj.optString("deliveryAddress"),
                        orderNotes = obj.optString("orderNotes", ""),
                        items = itemsList,
                        subtotal = obj.optInt("subtotal", 0),
                        deliveryFee = obj.optInt("deliveryFee", 150),
                        total = obj.optInt("total", 0),
                        status = status,
                        paymentMethod = obj.optString("paymentMethod", "Cash on Delivery (COD)"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            null
        }
    }

    fun saveSettingsLocal(s: AppSettings): AppSettings {
        val updated = s.copy(
            formattedWhatsappForUrl = AppSettings.computeFormattedWhatsapp(s.whatsappNumber),
            updatedAt = System.currentTimeMillis()
        )
        _settings.value = updated
        saveSettingsToPrefs(updated)
        return updated
    }

    private fun saveSettingsToPrefs(s: AppSettings) {
        try {
            val obj = JSONObject().apply {
                put("deliveryFee", s.deliveryFee)
                put("minOrderAmount", s.minOrderAmount)
                put("isStoreOpen", s.isStoreOpen)
                put("announcement", s.announcement)
                put("whatsappNumber", s.whatsappNumber)
                put("formattedWhatsappForUrl", AppSettings.computeFormattedWhatsapp(s.whatsappNumber))
                put("instagramHandle", s.instagramHandle)
                put("deliveryAreas", s.deliveryAreas)
                put("updatedAt", s.updatedAt)
            }
            prefs.edit()
                .putString("saved_settings_json", obj.toString())
                .putLong("settings_updated_at", s.updatedAt)
                .commit()
        } catch (e: Exception) {
            Log.e(tag, "Error saving settings to prefs: ${e.message}")
        }
    }

    private fun loadSettingsFromPrefs(): AppSettings? {
        val json = prefs.getString("saved_settings_json", null) ?: return null
        return try {
            val obj = JSONObject(json)
            val phone = obj.optString("whatsappNumber", "0304-4494742")
            AppSettings(
                deliveryFee = obj.optInt("deliveryFee", 150),
                minOrderAmount = obj.optInt("minOrderAmount", 300),
                isStoreOpen = obj.optBoolean("isStoreOpen", true),
                announcement = obj.optString("announcement", "Welcome to ZAVIRO - GHAR SE GHAR TAK!"),
                whatsappNumber = phone,
                formattedWhatsappForUrl = AppSettings.computeFormattedWhatsapp(phone),
                instagramHandle = obj.optString("instagramHandle", "@zaviropakistan"),
                deliveryAreas = obj.optString("deliveryAreas", "Lahore & Surrounding Areas"),
                updatedAt = obj.optLong("updatedAt", 0L)
            )
        } catch (e: Exception) {
            null
        }
    }

    fun saveBrandingLocal(b: BrandingSettings): BrandingSettings {
        val now = System.currentTimeMillis()
        val updated = b.copy(updatedAt = now)
        _branding.value = updated
        saveBrandingToPrefs(updated)
        return updated
    }

    private fun saveBrandingToPrefs(b: BrandingSettings) {
        try {
            val obj = JSONObject().apply {
                put("brandName", b.brandName)
                put("tagline", b.tagline)
                put("primaryColorHex", b.primaryColorHex)
                put("secondaryColorHex", b.secondaryColorHex)
                put("masterLogoUrl", b.masterLogoUrl)
                put("splashLogoUrl", b.splashLogoUrl)
                put("homeHeaderLogoUrl", b.homeHeaderLogoUrl)
                put("loginLogoUrl", b.loginLogoUrl)
                put("adminLogoUrl", b.adminLogoUrl)
                put("updatedAt", b.updatedAt)
            }
            prefs.edit()
                .putString("saved_branding_json", obj.toString())
                .putLong("branding_updated_at", b.updatedAt)
                .commit()
        } catch (e: Exception) {
            Log.e(tag, "Error saving branding to prefs: ${e.message}")
        }
    }

    private fun loadBrandingFromPrefs(): BrandingSettings? {
        val json = prefs.getString("saved_branding_json", null)
        val exactLogoFile = File(context.filesDir, "zaviro_exact_uploaded_logo.png")
        val fallbackMasterLogo = if (exactLogoFile.exists() && exactLogoFile.length() > 0) {
            "file://${exactLogoFile.absolutePath}"
        } else ""

        if (json == null) {
            return if (fallbackMasterLogo.isNotEmpty()) {
                BrandingSettings(masterLogoUrl = fallbackMasterLogo, updatedAt = exactLogoFile.lastModified())
            } else null
        }

        return try {
            val obj = JSONObject(json)
            val rawMaster = obj.optString("masterLogoUrl", "")
            val savedMaster = if (BrandingSettings.isUsableLogoSource(rawMaster)) rawMaster else fallbackMasterLogo
            BrandingSettings(
                brandName = obj.optString("brandName", "ZAVIRO"),
                tagline = obj.optString("tagline", "GHAR SE GHAR TAK"),
                primaryColorHex = obj.optString("primaryColorHex", "#FFB800"),
                secondaryColorHex = obj.optString("secondaryColorHex", "#FFD54F"),
                masterLogoUrl = savedMaster,
                splashLogoUrl = obj.optString("splashLogoUrl", "").takeIf { BrandingSettings.isUsableLogoSource(it) } ?: "",
                homeHeaderLogoUrl = obj.optString("homeHeaderLogoUrl", "").takeIf { BrandingSettings.isUsableLogoSource(it) } ?: "",
                loginLogoUrl = obj.optString("loginLogoUrl", "").takeIf { BrandingSettings.isUsableLogoSource(it) } ?: "",
                adminLogoUrl = obj.optString("adminLogoUrl", "").takeIf { BrandingSettings.isUsableLogoSource(it) } ?: "",
                updatedAt = obj.optLong("updatedAt", 0L)
            )
        } catch (e: Exception) {
            null
        }
    }

    // ==================== FIRESTORE LISTENERS ====================

    private fun startSync() {
        scope.launch {
            try {
                listenToSettings()
                listenToBranding()
                listenToExtras()
                listenToProducts()
                listenToDeals()
                listenToOrders()
                listenToAdminSecurity()
                fetchInitialCloudState()
            } catch (e: Exception) {
                Log.e(tag, "Error setting up listeners: ${e.message}", e)
                _isInitialSettingsLoaded.value = true
            }
        }
    }

    private fun isProductCustomizedComparedToDefault(p: Product): Boolean {
        val def = ZaviroMenuData.initialProducts.find { it.id == p.id } ?: return true
        return p.name != def.name ||
            p.price != def.price ||
            p.category != def.category ||
            p.description != def.description ||
            p.ingredients != def.ingredients ||
            p.isAvailable != def.isAvailable ||
            p.isPopular != def.isPopular ||
            p.imageUrl.isNotBlank()
    }

    private fun isDealCustomizedComparedToDefault(d: Deal): Boolean {
        val def = ZaviroMenuData.initialDeals.find { it.id == d.id } ?: return true
        return d.name != def.name ||
            d.price != def.price ||
            d.originalPrice != def.originalPrice ||
            d.category != def.category ||
            d.description != def.description ||
            d.items != def.items ||
            d.isAvailable != def.isAvailable ||
            d.imageUrl.isNotBlank()
    }

    private fun hasLocalAdminCustomizations(): Boolean {
        return isAdminSessionActive() ||
            _branding.value.hasAnyCustomLogo() ||
            getLocallyModifiedProductIds().isNotEmpty() ||
            getLocallyModifiedDealIds().isNotEmpty() ||
            getDeletedProductIds().isNotEmpty() ||
            getDeletedDealIds().isNotEmpty() ||
            _products.value.any { isProductCustomizedComparedToDefault(it) } ||
            _deals.value.any { isDealCustomizedComparedToDefault(it) }
    }

    fun triggerAdminCloudSync() {
        scope.launch {
            try {
                syncPendingLocalAdminChangesToFirestore(forceFullAdminSync = isAdminSessionActive())
            } catch (e: Exception) {
                Log.w(tag, "Admin cloud sync warning: ${e.message}")
            }
        }
    }

    private suspend fun syncPendingLocalAdminChangesToFirestore(forceFullAdminSync: Boolean = false) {
        val modifiedProductIds = getLocallyModifiedProductIds().toMutableSet()
        if (forceFullAdminSync || isAdminSessionActive()) {
            _products.value.forEach { prod ->
                if (isProductCustomizedComparedToDefault(prod)) {
                    modifiedProductIds.add(prod.id)
                }
            }
        }
        if (modifiedProductIds.isNotEmpty()) {
            val currentMap = _products.value.associateBy { it.id }
            modifiedProductIds.forEach { id ->
                currentMap[id]?.let { prod ->
                    try {
                        saveProduct(prod)
                    } catch (_: Exception) {}
                }
            }
        }

        if (isAdminSessionActive()) {
            getDeletedProductIds().forEach { delId ->
                try {
                    deleteProduct(delId)
                } catch (_: Exception) {}
            }
        }

        val modifiedDealIds = getLocallyModifiedDealIds().toMutableSet()
        if (forceFullAdminSync || isAdminSessionActive()) {
            _deals.value.forEach { deal ->
                if (isDealCustomizedComparedToDefault(deal)) {
                    modifiedDealIds.add(deal.id)
                }
            }
        }
        if (modifiedDealIds.isNotEmpty()) {
            val currentMap = _deals.value.associateBy { it.id }
            modifiedDealIds.forEach { id ->
                currentMap[id]?.let { deal ->
                    try {
                        saveDeal(deal)
                    } catch (_: Exception) {}
                }
            }
        }

        if (isAdminSessionActive()) {
            getDeletedDealIds().forEach { delId ->
                try {
                    deleteDeal(delId)
                } catch (_: Exception) {}
            }
        }

        if (_branding.value.hasAnyCustomLogo() || isAdminSessionActive()) {
            try {
                val brandingDoc = firestore.collection("settings").document("branding").get().await()
                val remoteUpdated = brandingDoc.readEpochMillis("updatedAt", 0L)
                val remoteMaster = brandingDoc.getString("masterLogoUrl")?.trim().orEmpty()
                if (!brandingDoc.exists() ||
                    brandingDoc.data.isNullOrEmpty() ||
                    (_branding.value.hasAnyCustomLogo() && !BrandingSettings.isUsableLogoSource(remoteMaster)) ||
                    (isAdminSessionActive() && _branding.value.updatedAt > remoteUpdated)
                ) {
                    saveBranding(_branding.value)
                }
            } catch (_: Exception) {}
        }

        if (isAdminSessionActive() && prefs.contains("saved_settings_json")) {
            try {
                val settingsDoc = firestore.collection("settings").document("general").get().await()
                val remoteUpdated = settingsDoc.readEpochMillis("updatedAt", 0L)
                if (!settingsDoc.exists() ||
                    settingsDoc.data.isNullOrEmpty() ||
                    _settings.value.updatedAt > remoteUpdated
                ) {
                    saveSettings(_settings.value)
                }
            } catch (_: Exception) {}
        }

        if (isAdminSessionActive() && prefs.getBoolean(KEY_HAS_CUSTOM_PASS, false)) {
            val localHash = prefs.getString(KEY_PASS_HASH, null).orEmpty()
            val localSalt = prefs.getString(KEY_PASS_SALT, null).orEmpty()
            val localUpdated = prefs.getLong(KEY_PASS_UPDATED_AT, 0L)
            if (localHash.isNotBlank() && localSalt.isNotBlank()) {
                try {
                    val secDoc = firestore.collection("settings").document("admin_security").get().await()
                    val remoteSchema = secDoc.getLong("schemaVersion") ?: 0L
                    val remoteUpdated = secDoc.readEpochMillis("updatedAt", 0L)
                    if (!secDoc.exists() || remoteSchema < ADMIN_SECURITY_SCHEMA_V6 || localUpdated > remoteUpdated) {
                        syncAdminPasswordHashToFirestore(localHash, localSalt, localUpdated)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private suspend fun fetchInitialCloudState() {
        val canPushAdminState = hasLocalAdminCustomizations()
        try {
            withTimeoutOrNull(4000L) {
                val brandingDoc = try {
                    firestore.collection("settings").document("branding").get(Source.SERVER).await()
                } catch (_: Exception) {
                    firestore.collection("settings").document("branding").get().await()
                }
                if (brandingDoc != null && brandingDoc.exists() && !brandingDoc.data.isNullOrEmpty()) {
                    applyRemoteBrandingSnapshot(brandingDoc)
                } else if (_branding.value.hasAnyCustomLogo() || isAdminSessionActive()) {
                    saveBranding(_branding.value)
                }

                val settingsDoc = try {
                    firestore.collection("settings").document("general").get(Source.SERVER).await()
                } catch (_: Exception) {
                    firestore.collection("settings").document("general").get().await()
                }
                if (settingsDoc != null && settingsDoc.exists() && !settingsDoc.data.isNullOrEmpty()) {
                    applyRemoteSettingsSnapshot(settingsDoc)
                } else if (isAdminSessionActive()) {
                    saveSettings(_settings.value)
                }

                val adminSecDoc = try {
                    firestore.collection("settings").document("admin_security").get(Source.SERVER).await()
                } catch (_: Exception) {
                    firestore.collection("settings").document("admin_security").get().await()
                }
                if (adminSecDoc != null && adminSecDoc.exists()) {
                    applyRemoteAdminSecuritySnapshot(adminSecDoc)
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Initial Firebase settings fetch warning: ${e.message}")
        } finally {
            _isInitialSettingsLoaded.value = true
        }

        // Also fetch initial catalog collections from Firestore
        try {
            if (canPushAdminState) {
                syncPendingLocalAdminChangesToFirestore()
            }

            val extrasSnap = try {
                firestore.collection("extras").whereEqualTo("isDeleted", false).get(Source.SERVER).await()
            } catch (_: Exception) {
                firestore.collection("extras").whereEqualTo("isDeleted", false).get().await()
            }
            if (extrasSnap != null && !extrasSnap.isEmpty) {
                applyRemoteExtrasSnapshot(extrasSnap.documents)
            } else if (canPushAdminState) {
                seedExtras()
            }

            val productsSnap = try {
                firestore.collection("products").whereEqualTo("isDeleted", false).get(Source.SERVER).await()
            } catch (_: Exception) {
                firestore.collection("products").whereEqualTo("isDeleted", false).get().await()
            }
            if (productsSnap != null && !productsSnap.isEmpty) {
                applyRemoteProductsSnapshot(productsSnap.documents)
            } else if (canPushAdminState) {
                seedProducts()
            }

            val dealsSnap = try {
                firestore.collection("deals").whereEqualTo("isDeleted", false).get(Source.SERVER).await()
            } catch (_: Exception) {
                firestore.collection("deals").whereEqualTo("isDeleted", false).get().await()
            }
            if (dealsSnap != null && !dealsSnap.isEmpty) {
                applyRemoteDealsSnapshot(dealsSnap.documents)
            } else if (canPushAdminState) {
                seedDeals()
            }
        } catch (e: Exception) {
            Log.w(tag, "Initial Firebase catalog fetch warning: ${e.message}")
        }
    }

    private fun applyRemoteAdminSecuritySnapshot(snapshot: DocumentSnapshot?) {
        if (snapshot == null || !snapshot.exists()) return
        val schemaVersion = snapshot.getLong("schemaVersion") ?: 0L
        if (schemaVersion < ADMIN_SECURITY_SCHEMA_V6) return
        val hasCustomRemote = snapshot.getBoolean("hasCustomPassword") ?: false
        val remoteHash = snapshot.getString("passwordHash")?.trim().orEmpty()
        val remoteSalt = snapshot.getString("passwordSalt")?.trim().orEmpty()
        val remoteUpdatedAt = snapshot.readEpochMillis("updatedAt", 0L)
        val localUpdatedAt = prefs.getLong(KEY_PASS_UPDATED_AT, 0L)
        if (remoteUpdatedAt >= localUpdatedAt) {
            if (hasCustomRemote && remoteHash.isNotBlank() && remoteSalt.isNotBlank()) {
                prefs.edit()
                    .remove("admin_custom_password")
                    .putString(KEY_PASS_HASH, remoteHash)
                    .putString(KEY_PASS_SALT, remoteSalt)
                    .putBoolean(KEY_HAS_CUSTOM_PASS, true)
                    .putLong(KEY_PASS_UPDATED_AT, remoteUpdatedAt)
                    .commit()
            } else if (!hasCustomRemote) {
                prefs.edit()
                    .remove("admin_custom_password")
                    .remove(KEY_PASS_HASH)
                    .remove(KEY_PASS_SALT)
                    .putBoolean(KEY_HAS_CUSTOM_PASS, false)
                    .putLong(KEY_PASS_UPDATED_AT, remoteUpdatedAt)
                    .commit()
            }
        }
    }

    private fun listenToAdminSecurity() {
        val activeUid = currentAuthUid() ?: return
        if (!isAdminSessionActive()) return
        firestore.collection("settings").document("admin_security")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.GET, "settings/admin_security")
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) return@addSnapshotListener
                applyRemoteAdminSecuritySnapshot(snapshot)
            }
    }

    private fun applyRemoteExtrasSnapshot(documents: List<DocumentSnapshot>) {
        if (documents.isEmpty()) return
        val currentLocalMap = _extras.value.associateBy { it.id }
        val remoteIds = mutableSetOf<String>()
        val deletedIds = mutableSetOf<String>()

        val list = documents.mapNotNull { doc ->
            try {
                val id = doc.getString("id") ?: doc.id
                remoteIds.add(id)
                if (doc.getBoolean("isDeleted") == true) {
                    deletedIds.add(id)
                    return@mapNotNull null
                }
                val localExtra = currentLocalMap[id]
                val name = doc.getString("name") ?: localExtra?.name ?: ""
                if (name.isBlank()) return@mapNotNull null
                val price = doc.getLong("price")?.toInt() ?: localExtra?.price ?: 0
                val description = doc.getString("description") ?: localExtra?.description ?: ""
                val isAvailable = doc.getBoolean("isAvailable") ?: localExtra?.isAvailable ?: true
                val rawRemoteImg = doc.getString("imageUrl")?.trim().orEmpty()
                val imageUrl = if (BrandingSettings.isUsableLogoSource(rawRemoteImg)) rawRemoteImg else ""

                ExtraOption(
                    id = id,
                    name = name,
                    price = price,
                    description = description,
                    isAvailable = isAvailable,
                    imageUrl = imageUrl
                )
            } catch (e: Exception) {
                null
            }
        }.toMutableList()

        // Include standard extras not yet present in Firestore and not deleted
        ZaviroMenuData.standardExtras.forEach { stdExtra ->
            if (stdExtra.id !in remoteIds && stdExtra.id !in deletedIds && list.none { it.id == stdExtra.id }) {
                list.add(stdExtra)
            }
        }

        if (list.isNotEmpty()) {
            _extras.value = list
            saveExtrasToPrefs(list)
            _products.value = _products.value.map { it.copy(availableExtras = list) }
            saveProductsToPrefs(_products.value)
        }
    }

    private fun listenToExtras() {
        extrasListener = firestore.collection("extras")
            .whereEqualTo("isDeleted", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "extras")
                    Log.w(tag, "Extras listen error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    applyRemoteExtrasSnapshot(snapshot.documents)
                }
            }
    }

    private fun seedExtras() {
        if (currentAuthUid() == null) return
        scope.launch {
            try {
                _extras.value.forEach { e ->
                    val docRef = firestore.collection("extras").document(e.id)
                    val map = hashMapOf<String, Any>(
                        "id" to e.id,
                        "name" to e.name,
                        "price" to e.price,
                        "description" to e.description,
                        "isAvailable" to e.isAvailable,
                        "imageUrl" to convertLocalFileUrlToFirestoreDataUri(e.imageUrl, maxDim = 512),
                        "isDeleted" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.set(map, SetOptions.merge()).await()
                }
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, "extras")
                Log.w(tag, "Error seeding extras: ${e.message}")
            }
        }
    }

    private fun applyRemoteProductsSnapshot(documents: List<DocumentSnapshot>) {
        if (documents.isEmpty()) return
        val deletedIds = getDeletedProductIds().toMutableSet()
        val modifiedIds = getLocallyModifiedProductIds()
        val currentLocalMap = _products.value.associateBy { it.id }
        val remoteDocIds = mutableSetOf<String>()

        val list = documents.mapNotNull { doc ->
            try {
                val id = doc.getString("id") ?: doc.id
                remoteDocIds.add(id)

                if (doc.getBoolean("isDeleted") == true) {
                    addDeletedProductId(id)
                    deletedIds.add(id)
                    return@mapNotNull null
                }
                if (id in deletedIds) return@mapNotNull null

                val localItem = currentLocalMap[id]
                // Only keep localItem if an unsynced local edit on THIS device is still pending upload
                if (localItem != null && id in modifiedIds) {
                    return@mapNotNull localItem
                }

                val name = doc.getString("name") ?: localItem?.name ?: ""
                if (name.isBlank()) return@mapNotNull null
                val category = doc.getString("category") ?: localItem?.category ?: "Pasta"
                val description = doc.getString("description") ?: localItem?.description ?: ""
                val ingredients = doc.getString("ingredients") ?: localItem?.ingredients ?: ""
                val price = doc.getLong("price")?.toInt() ?: localItem?.price ?: 0
                val isAvailable = doc.getBoolean("isAvailable") ?: localItem?.isAvailable ?: true
                val isPopular = doc.getBoolean("isPopular") ?: localItem?.isPopular ?: false
                val rawRemoteImage = doc.getString("imageUrl")?.trim().orEmpty()
                val imageUrl = when {
                    BrandingSettings.isUsableLogoSource(rawRemoteImage) -> rawRemoteImage
                    rawRemoteImage.isBlank() -> ""
                    localItem != null && BrandingSettings.isUsableLogoSource(localItem.imageUrl) -> localItem.imageUrl
                    else -> ""
                }
                val defaultItem = ZaviroMenuData.initialProducts.find { it.id == id }
                val drawableRes = defaultItem?.drawableRes
                    ?: localItem?.drawableRes?.takeIf { it != 0 }
                    ?: resolveCategoryDrawable(category, isDeal = false)

                Product(
                    id = id,
                    name = name,
                    category = category,
                    description = description,
                    ingredients = ingredients,
                    price = price,
                    isAvailable = isAvailable,
                    isPopular = isPopular,
                    drawableRes = drawableRes,
                    imageUrl = imageUrl,
                    availableExtras = _extras.value
                )
            } catch (e: Exception) {
                null
            }
        }.toMutableList()

        // Include any in-flight locally modified products not yet in Firestore snapshot
        currentLocalMap.values.forEach { localProd ->
            if (localProd.id in modifiedIds && localProd.id !in deletedIds && list.none { it.id == localProd.id }) {
                list.add(localProd)
            }
        }

        // Include any default menu items that haven't been written or deleted in Firestore yet
        val missingDefaults = mutableListOf<Product>()
        ZaviroMenuData.initialProducts.forEach { defProd ->
            if (defProd.id !in remoteDocIds && defProd.id !in deletedIds && list.none { it.id == defProd.id }) {
                val candidate = currentLocalMap[defProd.id] ?: defProd
                list.add(candidate)
                missingDefaults.add(candidate)
            }
        }
        if (missingDefaults.isNotEmpty() && isAdminSessionActive()) {
            seedSpecificProducts(missingDefaults)
        }

        if (list.isNotEmpty()) {
            _products.value = list
            saveProductsToPrefs(list)
        }
    }

    private fun listenToProducts() {
        productsListener = firestore.collection("products")
            .whereEqualTo("isDeleted", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "products")
                    Log.w(tag, "Products listen error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    applyRemoteProductsSnapshot(snapshot.documents)
                }
            }
    }

    private fun seedSpecificProducts(items: List<Product>) {
        if (currentAuthUid() == null) return
        scope.launch {
            try {
                items.forEach { p ->
                    val docRef = firestore.collection("products").document(p.id)
                    val map = hashMapOf<String, Any>(
                        "id" to p.id,
                        "name" to p.name,
                        "category" to p.category,
                        "description" to p.description,
                        "ingredients" to p.ingredients,
                        "price" to p.price,
                        "isAvailable" to p.isAvailable,
                        "isPopular" to p.isPopular,
                        "imageUrl" to convertLocalFileUrlToFirestoreDataUri(p.imageUrl, maxDim = 700),
                        "isDeleted" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.set(map, SetOptions.merge()).await()
                }
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, "products")
                Log.w(tag, "Error seeding specific products: ${e.message}")
            }
        }
    }

    private fun seedProducts() {
        seedSpecificProducts(_products.value)
    }

    private fun applyRemoteDealsSnapshot(documents: List<DocumentSnapshot>) {
        if (documents.isEmpty()) return
        val deletedIds = getDeletedDealIds().toMutableSet()
        val modifiedIds = getLocallyModifiedDealIds()
        val currentLocalMap = _deals.value.associateBy { it.id }
        val remoteDocIds = mutableSetOf<String>()

        val list = documents.mapNotNull { doc ->
            try {
                val id = doc.getString("id") ?: doc.id
                remoteDocIds.add(id)

                if (doc.getBoolean("isDeleted") == true) {
                    addDeletedDealId(id)
                    deletedIds.add(id)
                    return@mapNotNull null
                }
                if (id in deletedIds) return@mapNotNull null

                val localDeal = currentLocalMap[id]
                if (localDeal != null && id in modifiedIds) {
                    return@mapNotNull localDeal
                }

                val dealNumber = doc.getLong("dealNumber")?.toInt() ?: localDeal?.dealNumber ?: 1
                val name = doc.getString("name") ?: localDeal?.name ?: ""
                if (name.isBlank()) return@mapNotNull null
                val category = doc.getString("category") ?: localDeal?.category ?: "Wraps & Rolls"
                @Suppress("UNCHECKED_CAST")
                val items = doc.get("items") as? List<String> ?: localDeal?.items ?: emptyList()
                val description = doc.getString("description") ?: localDeal?.description ?: ""
                val price = doc.getLong("price")?.toInt() ?: localDeal?.price ?: 0
                val originalPrice = doc.getLong("originalPrice")?.toInt() ?: localDeal?.originalPrice ?: 0
                val saveAmount = doc.getLong("saveAmount")?.toInt() ?: localDeal?.saveAmount ?: 0
                val isAvailable = doc.getBoolean("isAvailable") ?: localDeal?.isAvailable ?: true
                val rawRemoteImage = doc.getString("imageUrl")?.trim().orEmpty()
                val imageUrl = when {
                    BrandingSettings.isUsableLogoSource(rawRemoteImage) -> rawRemoteImage
                    rawRemoteImage.isBlank() -> ""
                    localDeal != null && BrandingSettings.isUsableLogoSource(localDeal.imageUrl) -> localDeal.imageUrl
                    else -> ""
                }
                val defaultDeal = ZaviroMenuData.initialDeals.find { it.id == id || it.dealNumber == dealNumber }
                val drawableRes = defaultDeal?.drawableRes
                    ?: localDeal?.drawableRes?.takeIf { it != 0 }
                    ?: resolveCategoryDrawable(category, isDeal = true)

                Deal(
                    id = id,
                    dealNumber = dealNumber,
                    name = name,
                    category = category,
                    items = items,
                    description = description,
                    price = price,
                    originalPrice = originalPrice,
                    saveAmount = saveAmount,
                    isAvailable = isAvailable,
                    drawableRes = drawableRes,
                    imageUrl = imageUrl
                )
            } catch (e: Exception) {
                null
            }
        }.toMutableList()

        currentLocalMap.values.forEach { localDeal ->
            if (localDeal.id in modifiedIds && localDeal.id !in deletedIds && list.none { it.id == localDeal.id }) {
                list.add(localDeal)
            }
        }

        val missingDefaults = mutableListOf<Deal>()
        ZaviroMenuData.initialDeals.forEach { defDeal ->
            if (defDeal.id !in remoteDocIds && defDeal.id !in deletedIds && list.none { it.id == defDeal.id }) {
                val candidate = currentLocalMap[defDeal.id] ?: defDeal
                list.add(candidate)
                missingDefaults.add(candidate)
            }
        }
        if (missingDefaults.isNotEmpty() && isAdminSessionActive()) {
            seedSpecificDeals(missingDefaults)
        }

        val sorted = list.sortedBy { it.dealNumber }
        if (sorted.isNotEmpty()) {
            _deals.value = sorted
            saveDealsToPrefs(sorted)
        }
    }

    private fun listenToDeals() {
        dealsListener = firestore.collection("deals")
            .whereEqualTo("isDeleted", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "deals")
                    Log.w(tag, "Deals listen error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    applyRemoteDealsSnapshot(snapshot.documents)
                }
            }
    }

    private fun seedSpecificDeals(items: List<Deal>) {
        if (currentAuthUid() == null) return
        scope.launch {
            try {
                items.forEach { d ->
                    val docRef = firestore.collection("deals").document(d.id)
                    val map = hashMapOf<String, Any>(
                        "id" to d.id,
                        "dealNumber" to d.dealNumber,
                        "name" to d.name,
                        "category" to d.category,
                        "items" to d.items,
                        "description" to d.description,
                        "price" to d.price,
                        "originalPrice" to d.originalPrice,
                        "saveAmount" to d.saveAmount,
                        "isAvailable" to d.isAvailable,
                        "imageUrl" to convertLocalFileUrlToFirestoreDataUri(d.imageUrl, maxDim = 700),
                        "isDeleted" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.set(map, SetOptions.merge()).await()
                }
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, "deals")
                Log.w(tag, "Error seeding specific deals: ${e.message}")
            }
        }
    }

    private fun seedDeals() {
        seedSpecificDeals(_deals.value)
    }

    private fun listenToOrders() {
        val activeUid = currentAuthUid() ?: return
        ordersListener?.remove()
        ordersListener = firestore.collection("orders")
            .whereEqualTo("customerUid", activeUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "orders")
                    Log.w(tag, "Orders listen error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val modifiedOrderIds = getLocallyModifiedOrderIds()
                    val currentLocalMap = _orders.value.associateBy { it.id }

                    val remoteList = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.getString("id") ?: doc.id
                            val localOrder = currentLocalMap[id]
                            if (localOrder != null && id in modifiedOrderIds) {
                                return@mapNotNull localOrder
                            }

                            val orderNumber = doc.getString("orderNumber") ?: "#ZAV-0000"
                            val customerUid = doc.getString("customerUid") ?: activeUid
                            val customerName = doc.getString("customerName") ?: ""
                            val phone = doc.getString("phone") ?: ""
                            val customerEmail = doc.getString("customerEmail") ?: ""
                            val deliveryAddress = doc.getString("deliveryAddress") ?: ""
                            val orderNotes = doc.getString("orderNotes") ?: ""
                            val subtotal = doc.getLong("subtotal")?.toInt() ?: 0
                            val deliveryFee = doc.getLong("deliveryFee")?.toInt() ?: 150
                            val total = doc.getLong("total")?.toInt() ?: (subtotal + deliveryFee)
                            val statusStr = doc.getString("status") ?: OrderStatus.RECEIVED.name
                            val status = try { OrderStatus.valueOf(statusStr) } catch (e: Exception) { OrderStatus.RECEIVED }
                            val paymentMethod = doc.getString("paymentMethod") ?: "Cash on Delivery (COD)"
                            val createdAt = doc.readEpochMillis("createdAt", System.currentTimeMillis())

                            @Suppress("UNCHECKED_CAST")
                            val rawItems = doc.get("items") as? List<Map<String, Any>> ?: emptyList()
                            val items = rawItems.map { itemMap ->
                                OrderItemDto(
                                    id = itemMap["id"] as? String ?: "",
                                    name = itemMap["name"] as? String ?: "",
                                    quantity = (itemMap["quantity"] as? Number)?.toInt() ?: 1,
                                    unitPrice = (itemMap["unitPrice"] as? Number)?.toInt() ?: 0,
                                    extrasSummary = itemMap["extrasSummary"] as? String ?: "",
                                    specialInstructions = itemMap["specialInstructions"] as? String ?: ""
                                )
                            }

                            Order(
                                id = id,
                                orderNumber = orderNumber,
                                customerUid = customerUid,
                                customerName = customerName,
                                phone = phone,
                                customerEmail = customerEmail,
                                deliveryAddress = deliveryAddress,
                                orderNotes = orderNotes,
                                items = items,
                                subtotal = subtotal,
                                deliveryFee = deliveryFee,
                                total = total,
                                status = status,
                                paymentMethod = paymentMethod,
                                createdAt = createdAt
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.toMutableList()

                    currentLocalMap.values.forEach { localOrd ->
                        if (remoteList.none { it.id == localOrd.id }) {
                            remoteList.add(localOrd)
                        }
                    }

                    val merged = remoteList.sortedByDescending { it.createdAt }
                    if (merged.isNotEmpty()) {
                        _orders.value = merged
                        saveOrdersToPrefs(merged)
                    }
                }
            }
    }

    private fun applyRemoteSettingsSnapshot(snapshot: DocumentSnapshot) {
        if (!snapshot.exists() || snapshot.data.isNullOrEmpty()) return
        val current = _settings.value
        val rawFee = snapshot.get("deliveryFee")
        val fee = when (rawFee) {
            is Number -> rawFee.toInt()
            is String -> rawFee.toIntOrNull() ?: current.deliveryFee
            else -> current.deliveryFee
        }
        val rawMinOrder = snapshot.get("minOrderAmount")
        val minOrder = when (rawMinOrder) {
            is Number -> rawMinOrder.toInt()
            is String -> rawMinOrder.toIntOrNull() ?: current.minOrderAmount
            else -> current.minOrderAmount
        }
        val isOpen = snapshot.getBoolean("isStoreOpen") ?: current.isStoreOpen
        val announcement = snapshot.getString("announcement")?.takeIf { it.isNotBlank() } ?: current.announcement
        val phone = snapshot.getString("whatsappNumber")?.takeIf { it.isNotBlank() } ?: current.whatsappNumber
        val instagram = snapshot.getString("instagramHandle")?.takeIf { it.isNotBlank() } ?: current.instagramHandle
        val areas = snapshot.getString("deliveryAreas")?.takeIf { it.isNotBlank() } ?: current.deliveryAreas
        val remoteUpdatedAt = snapshot.readEpochMillis("updatedAt", System.currentTimeMillis())

        val updatedSettings = AppSettings(
            deliveryFee = fee,
            minOrderAmount = minOrder,
            isStoreOpen = isOpen,
            announcement = announcement,
            whatsappNumber = phone,
            formattedWhatsappForUrl = AppSettings.computeFormattedWhatsapp(phone),
            instagramHandle = instagram,
            deliveryAreas = areas,
            updatedAt = if (remoteUpdatedAt > 0L) remoteUpdatedAt else System.currentTimeMillis()
        )
        _settings.value = updatedSettings
        saveSettingsToPrefs(updatedSettings)
        _isInitialSettingsLoaded.value = true
    }

    private fun listenToSettings() {
        settingsListener = firestore.collection("settings").document("general")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.GET, "settings/general")
                    Log.w(tag, "Settings listen error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    applyRemoteSettingsSnapshot(snapshot)
                }
            }
    }

    private fun cacheBase64LogoToLocalDiskIfNeeded(
        source: String,
        slot: LogoSlot,
        forceOverwrite: Boolean = true
    ) {
        val trimmed = source.trim()
        if (!trimmed.startsWith("data:image") && (trimmed.startsWith("http") || trimmed.startsWith("file:") || trimmed.startsWith("/") || trimmed.length <= 60)) {
            return
        }
        try {
            val base64Data = if (trimmed.contains(",")) trimmed.substringAfter(",") else trimmed
            val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
            if (decodedBytes.isNotEmpty()) {
                val slotFile = File(context.filesDir, "zaviro_logo_${slot.name.lowercase()}.png")
                if (forceOverwrite || !slotFile.exists() || slotFile.length() == 0L) {
                    slotFile.writeBytes(decodedBytes)
                }
                if (slot == LogoSlot.MASTER) {
                    val masterFile = File(context.filesDir, "zaviro_exact_uploaded_logo.png")
                    if (forceOverwrite || !masterFile.exists() || masterFile.length() == 0L) {
                        masterFile.writeBytes(decodedBytes)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Could not cache base64 logo for $slot: ${e.message}")
        }
    }

    private fun resolveRemoteOrCachedLogo(
        remoteValue: String?,
        currentLocalValue: String,
        anyRemoteLogoUsable: Boolean,
        slot: LogoSlot,
        isEchoOfLocalExactUpload: Boolean
    ): String {
        val trimmedRemote = remoteValue?.trim().orEmpty()
        if (isEchoOfLocalExactUpload && currentLocalValue.startsWith("file://") && BrandingSettings.isUsableLogoSource(currentLocalValue)) {
            return currentLocalValue
        }
        if (BrandingSettings.isUsableLogoSource(trimmedRemote)) {
            cacheBase64LogoToLocalDiskIfNeeded(trimmedRemote, slot, forceOverwrite = !isEchoOfLocalExactUpload)
            return trimmedRemote
        }
        if (anyRemoteLogoUsable) {
            val slotFile = File(context.filesDir, "zaviro_logo_${slot.name.lowercase()}.png")
            if (slotFile.exists()) slotFile.delete()
            return ""
        }
        return if (BrandingSettings.isUsableLogoSource(currentLocalValue)) currentLocalValue else ""
    }

    private fun applyRemoteBrandingSnapshot(snapshot: DocumentSnapshot) {
        if (!snapshot.exists() || snapshot.data.isNullOrEmpty()) return
        val current = _branding.value

        val rawMaster = snapshot.getString("masterLogoUrl")?.trim().orEmpty()
            .ifBlank { snapshot.getString("logoUrl")?.trim().orEmpty() }
            .ifBlank { snapshot.getString("appLogoUrl")?.trim().orEmpty() }
        val rawSplash = snapshot.getString("splashLogoUrl")?.trim().orEmpty()
        val rawHomeHeader = snapshot.getString("homeHeaderLogoUrl")?.trim().orEmpty()
            .ifBlank { snapshot.getString("headerLogoUrl")?.trim().orEmpty() }
        val rawLogin = snapshot.getString("loginLogoUrl")?.trim().orEmpty()
        val rawAdmin = snapshot.getString("adminLogoUrl")?.trim().orEmpty()

        val anyRemoteLogoUsable = listOf(rawMaster, rawSplash, rawHomeHeader, rawLogin, rawAdmin)
            .any { BrandingSettings.isUsableLogoSource(it) }

        val remoteUpdatedAt = snapshot.readEpochMillis("updatedAt", System.currentTimeMillis())
        val localExactUploadAt = prefs.getLong("local_exact_logo_uploaded_at", -1L)
        val isEchoOfLocalExactUpload = localExactUploadAt > 0L && remoteUpdatedAt == localExactUploadAt

        val resolvedMaster = resolveRemoteOrCachedLogo(rawMaster, current.masterLogoUrl, anyRemoteLogoUsable, LogoSlot.MASTER, isEchoOfLocalExactUpload)
        val resolvedSplash = resolveRemoteOrCachedLogo(rawSplash, current.splashLogoUrl, anyRemoteLogoUsable, LogoSlot.SPLASH, isEchoOfLocalExactUpload)
        val resolvedHomeHeader = resolveRemoteOrCachedLogo(rawHomeHeader, current.homeHeaderLogoUrl, anyRemoteLogoUsable, LogoSlot.HOME_HEADER, isEchoOfLocalExactUpload)
        val resolvedLogin = resolveRemoteOrCachedLogo(rawLogin, current.loginLogoUrl, anyRemoteLogoUsable, LogoSlot.LOGIN, isEchoOfLocalExactUpload)
        val resolvedAdmin = resolveRemoteOrCachedLogo(rawAdmin, current.adminLogoUrl, anyRemoteLogoUsable, LogoSlot.ADMIN, isEchoOfLocalExactUpload)

        val updatedBranding = BrandingSettings(
            brandName = snapshot.getString("brandName")?.takeIf { it.isNotBlank() } ?: current.brandName,
            tagline = snapshot.getString("tagline")?.takeIf { it.isNotBlank() } ?: current.tagline,
            primaryColorHex = snapshot.getString("primaryColorHex")?.takeIf { it.isNotBlank() } ?: current.primaryColorHex,
            secondaryColorHex = snapshot.getString("secondaryColorHex")?.takeIf { it.isNotBlank() } ?: current.secondaryColorHex,
            masterLogoUrl = resolvedMaster,
            splashLogoUrl = resolvedSplash,
            homeHeaderLogoUrl = resolvedHomeHeader,
            loginLogoUrl = resolvedLogin,
            adminLogoUrl = resolvedAdmin,
            updatedAt = if (remoteUpdatedAt > 0L) remoteUpdatedAt else System.currentTimeMillis()
        )
        _branding.value = updatedBranding
        saveBrandingToPrefs(updatedBranding)
        _isInitialSettingsLoaded.value = true

        val hasLocalFileInRemoteDoc = listOf(rawMaster, rawSplash, rawHomeHeader, rawLogin, rawAdmin)
            .any { (it.startsWith("file://") || it.startsWith("/")) && BrandingSettings.isUsableLogoSource(it) }
        if (hasLocalFileInRemoteDoc) {
            scope.launch {
                try {
                    saveBranding(updatedBranding)
                } catch (_: Exception) {}
            }
        }
    }

    private fun listenToBranding() {
        brandingListener = firestore.collection("settings").document("branding")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.GET, "settings/branding")
                    Log.w(tag, "Branding listen error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    applyRemoteBrandingSnapshot(snapshot)
                }
            }
    }

    // ==================== CRUD & ORDER OPERATIONS ====================

    fun placeOrderLocal(
        customerName: String,
        phone: String,
        address: String,
        notes: String,
        cartItems: List<CartItem>,
        deliveryFee: Int
    ): Order {
        val randomSuffix = (1000..9999).random()
        val orderNumber = "#ZAV-$randomSuffix"
        val orderId = UUID.randomUUID().toString()

        val orderItems = cartItems.map { item ->
            val extrasText = if (item.selectedExtras.isNotEmpty()) {
                item.selectedExtras.joinToString(", ") { "${it.name} (+Rs ${it.price})" }
            } else ""
            OrderItemDto(
                id = item.cartItemId,
                name = item.name,
                quantity = item.quantity,
                unitPrice = item.unitPriceWithExtras,
                extrasSummary = extrasText,
                specialInstructions = item.specialInstructions
            )
        }

        val subtotal = cartItems.sumOf { it.totalPrice }
        val grandTotal = subtotal + deliveryFee

        val currentCust = _customerProfile.value
        val newOrder = Order(
            id = orderId,
            orderNumber = orderNumber,
            customerUid = currentCust.uid,
            customerName = customerName,
            phone = phone,
            customerEmail = currentCust.email,
            deliveryAddress = address,
            orderNotes = notes,
            items = orderItems,
            subtotal = subtotal,
            deliveryFee = deliveryFee,
            total = grandTotal,
            status = OrderStatus.RECEIVED,
            paymentMethod = "Cash on Delivery (COD)",
            createdAt = System.currentTimeMillis()
        )

        addLocallyModifiedOrderId(orderId)
        _orders.value = listOf(newOrder) + _orders.value
        saveOrdersToPrefs(_orders.value)
        return newOrder
    }

    suspend fun placeOrder(
        customerName: String,
        phone: String,
        address: String,
        notes: String,
        cartItems: List<CartItem>,
        deliveryFee: Int
    ): Order {
        val newOrder = placeOrderLocal(customerName, phone, address, notes, cartItems, deliveryFee)
        val activeUid = currentAuthUid()
        if (activeUid != null) {
            try {
                val orderMap = hashMapOf<String, Any>(
                    "id" to newOrder.id,
                    "orderNumber" to newOrder.orderNumber,
                    "customerUid" to activeUid,
                    "customerName" to newOrder.customerName,
                    "phone" to newOrder.phone,
                    "customerEmail" to newOrder.customerEmail,
                    "deliveryAddress" to newOrder.deliveryAddress,
                    "orderNotes" to newOrder.orderNotes,
                    "subtotal" to newOrder.subtotal,
                    "deliveryFee" to newOrder.deliveryFee,
                    "total" to newOrder.total,
                    "status" to newOrder.status.name,
                    "paymentMethod" to newOrder.paymentMethod,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "items" to newOrder.items.map {
                        mapOf(
                            "id" to it.id,
                            "name" to it.name,
                            "quantity" to it.quantity,
                            "unitPrice" to it.unitPrice,
                            "extrasSummary" to it.extrasSummary,
                            "specialInstructions" to it.specialInstructions
                        )
                    }
                )
                firestore.collection("orders").document(newOrder.id).set(orderMap).await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, "orders/${newOrder.id}")
                Log.e(tag, "Local order saved; Firestore sync warning: ${e.message}")
            }
        }
        return newOrder
    }

    fun updateOrderStatusLocal(orderId: String, newStatus: OrderStatus) {
        addLocallyModifiedOrderId(orderId)
        _orders.value = _orders.value.map {
            if (it.id == orderId) it.copy(status = newStatus) else it
        }
        saveOrdersToPrefs(_orders.value)
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        updateOrderStatusLocal(orderId, newStatus)
        if (currentAuthUid() == null) return
        try {
            firestore.collection("orders").document(orderId).update(
                mapOf(
                    "status" to newStatus.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "orders/$orderId")
            Log.e(tag, "Local order status saved; Firestore sync warning: ${e.message}")
        }
    }

    fun saveProductLocal(product: Product): Product {
        val id = if (product.id.isBlank()) "prod_${System.currentTimeMillis()}" else product.id
        val resolvedDrawable = if (product.drawableRes != 0) {
            product.drawableRes
        } else {
            resolveCategoryDrawable(product.category, isDeal = false)
        }
        val updated = product.copy(
            id = id,
            drawableRes = resolvedDrawable,
            availableExtras = if (product.availableExtras.isEmpty()) _extras.value else product.availableExtras
        )
        addLocallyModifiedProductId(id)

        _products.value = if (_products.value.any { it.id == id }) {
            _products.value.map { if (it.id == id) updated else it }
        } else {
            _products.value + updated
        }
        saveProductsToPrefs(_products.value)
        return updated
    }

    suspend fun saveProduct(product: Product) {
        val cloudImageUrl = convertLocalFileUrlToCloudUrlOrDataUri(
            source = product.imageUrl,
            folder = "zaviro_products",
            maxDim = 700
        )
        val updated = saveProductLocal(product.copy(imageUrl = cloudImageUrl))
        if (currentAuthUid() == null) return
        try {
            val map = hashMapOf<String, Any>(
                "id" to updated.id,
                "name" to updated.name,
                "category" to updated.category,
                "description" to updated.description,
                "ingredients" to updated.ingredients,
                "price" to updated.price,
                "isAvailable" to updated.isAvailable,
                "isPopular" to updated.isPopular,
                "imageUrl" to cloudImageUrl,
                "isDeleted" to false,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("products").document(updated.id).set(map, SetOptions.merge()).await()
            removeLocallyModifiedProductId(updated.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "products/${updated.id}")
            Log.e(tag, "Local product saved; Firestore sync warning: ${e.message}")
        }
    }

    fun deleteProductLocal(productId: String) {
        addDeletedProductId(productId)
        removeLocallyModifiedProductId(productId)
        _products.value = _products.value.filter { it.id != productId }
        saveProductsToPrefs(_products.value)
    }

    suspend fun deleteProduct(productId: String) {
        deleteProductLocal(productId)
        if (currentAuthUid() == null) return
        try {
            val tombstone = mapOf(
                "id" to productId,
                "name" to "Deleted Product",
                "category" to "Pasta",
                "price" to 0,
                "isAvailable" to false,
                "isDeleted" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("products").document(productId).set(tombstone, SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "products/$productId")
            Log.e(tag, "Local product deleted; Firestore sync warning: ${e.message}")
        }
    }

    fun saveDealLocal(deal: Deal): Deal {
        val id = if (deal.id.isBlank()) "deal_${System.currentTimeMillis()}" else deal.id
        val resolvedDrawable = if (deal.drawableRes != 0) {
            deal.drawableRes
        } else {
            resolveCategoryDrawable(deal.category, isDeal = true)
        }
        val saveAmt = if (deal.originalPrice > deal.price) {
            deal.originalPrice - deal.price
        } else {
            deal.saveAmount.coerceAtLeast(0)
        }
        val updated = deal.copy(
            id = id,
            drawableRes = resolvedDrawable,
            saveAmount = saveAmt
        )
        addLocallyModifiedDealId(id)

        _deals.value = if (_deals.value.any { it.id == id }) {
            _deals.value.map { if (it.id == id) updated else it }
        } else {
            _deals.value + updated
        }.sortedBy { it.dealNumber }
        saveDealsToPrefs(_deals.value)
        return updated
    }

    suspend fun saveDeal(deal: Deal) {
        val cloudImageUrl = convertLocalFileUrlToCloudUrlOrDataUri(
            source = deal.imageUrl,
            folder = "zaviro_deals",
            maxDim = 700
        )
        val updated = saveDealLocal(deal.copy(imageUrl = cloudImageUrl))
        if (currentAuthUid() == null) return
        try {
            val map = hashMapOf<String, Any>(
                "id" to updated.id,
                "dealNumber" to updated.dealNumber,
                "name" to updated.name,
                "category" to updated.category,
                "items" to updated.items,
                "description" to updated.description,
                "price" to updated.price,
                "originalPrice" to updated.originalPrice,
                "saveAmount" to updated.saveAmount,
                "isAvailable" to updated.isAvailable,
                "imageUrl" to cloudImageUrl,
                "isDeleted" to false,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("deals").document(updated.id).set(map, SetOptions.merge()).await()
            removeLocallyModifiedDealId(updated.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "deals/${updated.id}")
            Log.e(tag, "Local deal saved; Firestore sync warning: ${e.message}")
        }
    }

    fun deleteDealLocal(dealId: String) {
        addDeletedDealId(dealId)
        removeLocallyModifiedDealId(dealId)
        _deals.value = _deals.value.filter { it.id != dealId }
        saveDealsToPrefs(_deals.value)
    }

    suspend fun deleteDeal(dealId: String) {
        deleteDealLocal(dealId)
        if (currentAuthUid() == null) return
        try {
            val tombstone = mapOf(
                "id" to dealId,
                "dealNumber" to 1,
                "name" to "Deleted Deal",
                "category" to "Wraps & Rolls",
                "items" to listOf("Deleted"),
                "price" to 0,
                "isAvailable" to false,
                "isDeleted" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("deals").document(dealId).set(tombstone, SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "deals/$dealId")
            Log.e(tag, "Local deal deleted; Firestore sync warning: ${e.message}")
        }
    }

    fun saveExtraLocal(extra: ExtraOption): ExtraOption {
        val id = if (extra.id.isBlank()) "extra_${System.currentTimeMillis()}" else extra.id
        val updated = extra.copy(id = id)
        _extras.value = if (_extras.value.any { it.id == id }) {
            _extras.value.map { if (it.id == id) updated else it }
        } else {
            _extras.value + updated
        }
        saveExtrasToPrefs(_extras.value)
        _products.value = _products.value.map { it.copy(availableExtras = _extras.value) }
        saveProductsToPrefs(_products.value)
        return updated
    }

    suspend fun saveExtra(extra: ExtraOption) {
        val cloudImageUrl = convertLocalFileUrlToCloudUrlOrDataUri(
            source = extra.imageUrl,
            folder = "zaviro_extras",
            maxDim = 512
        )
        val updated = saveExtraLocal(extra.copy(imageUrl = cloudImageUrl))
        if (currentAuthUid() == null) return
        try {
            val map = hashMapOf<String, Any>(
                "id" to updated.id,
                "name" to updated.name,
                "price" to updated.price,
                "description" to updated.description,
                "isAvailable" to updated.isAvailable,
                "imageUrl" to cloudImageUrl,
                "isDeleted" to false,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("extras").document(updated.id).set(map, SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "extras/${updated.id}")
            Log.e(tag, "Local extra saved; Firestore sync warning: ${e.message}")
        }
    }

    fun deleteExtraLocal(extraId: String) {
        _extras.value = _extras.value.filter { it.id != extraId }
        saveExtrasToPrefs(_extras.value)
        _products.value = _products.value.map { it.copy(availableExtras = _extras.value) }
        saveProductsToPrefs(_products.value)
    }

    suspend fun deleteExtra(extraId: String) {
        deleteExtraLocal(extraId)
        if (currentAuthUid() == null) return
        try {
            val tombstone = mapOf(
                "id" to extraId,
                "name" to "Deleted Extra",
                "price" to 0,
                "isAvailable" to false,
                "isDeleted" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("extras").document(extraId).set(tombstone, SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "extras/$extraId")
            Log.e(tag, "Local extra deleted; Firestore sync warning: ${e.message}")
        }
    }

    fun updateDeliveryFeeLocal(newFee: Int): AppSettings {
        return saveSettingsLocal(_settings.value.copy(deliveryFee = newFee))
    }

    suspend fun updateDeliveryFee(newFee: Int) {
        val updated = updateDeliveryFeeLocal(newFee)
        saveSettings(updated)
    }

    suspend fun saveSettings(settings: AppSettings) {
        val updated = saveSettingsLocal(settings)
        if (currentAuthUid() == null) return
        try {
            val map = hashMapOf<String, Any>(
                "deliveryFee" to updated.deliveryFee,
                "minOrderAmount" to updated.minOrderAmount,
                "isStoreOpen" to updated.isStoreOpen,
                "announcement" to updated.announcement,
                "whatsappNumber" to updated.whatsappNumber,
                "formattedWhatsappForUrl" to updated.formattedWhatsappForUrl,
                "instagramHandle" to updated.instagramHandle,
                "deliveryAreas" to updated.deliveryAreas,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("settings").document("general")
                .set(map, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "settings/general")
            Log.e(tag, "Local settings saved; Firestore sync warning: ${e.message}")
        }
    }

    fun encodeBytesToCloudDataUri(rawBytes: ByteArray, maxDim: Int = 700): String {
        if (rawBytes.isEmpty()) return ""
        return try {
            if (rawBytes.size <= 80_000) {
                "data:image/png;base64," + Base64.encodeToString(rawBytes, Base64.NO_WRAP)
            } else {
                val opts = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, opts)
                    ?: return "data:image/png;base64," + Base64.encodeToString(rawBytes, Base64.NO_WRAP)
                val scaled = if (decoded.width > maxDim || decoded.height > maxDim) {
                    val ratio = minOf(maxDim.toFloat() / decoded.width, maxDim.toFloat() / decoded.height)
                    val targetW = (decoded.width * ratio).toInt().coerceAtLeast(1)
                    val targetH = (decoded.height * ratio).toInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(decoded, targetW, targetH, true)
                } else {
                    decoded
                }
                if (scaled.hasAlpha()) {
                    val pngOut = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.PNG, 100, pngOut)
                    val pngBytes = pngOut.toByteArray()
                    if (pngBytes.size <= 90_000) {
                        return "data:image/png;base64," + Base64.encodeToString(pngBytes, Base64.NO_WRAP)
                    }
                }
                val webpOut = ByteArrayOutputStream()
                @Suppress("DEPRECATION")
                scaled.compress(Bitmap.CompressFormat.WEBP, 85, webpOut)
                val webpBytes = webpOut.toByteArray()
                if (webpBytes.size <= 100_000) {
                    "data:image/webp;base64," + Base64.encodeToString(webpBytes, Base64.NO_WRAP)
                } else {
                    val compactOut = ByteArrayOutputStream()
                    @Suppress("DEPRECATION")
                    scaled.compress(Bitmap.CompressFormat.WEBP, 70, compactOut)
                    "data:image/webp;base64," + Base64.encodeToString(compactOut.toByteArray(), Base64.NO_WRAP)
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Error encoding bytes to cloud data URI: ${e.message}")
            "data:image/png;base64," + Base64.encodeToString(rawBytes, Base64.NO_WRAP)
        }
    }

    private val storageHttpClient by lazy {
        okhttp3.OkHttpClient.Builder()
            .connectTimeout(6, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    /**
     * Attempts to upload raw image bytes to Firebase Storage (`gen-lang-client-0737867339.firebasestorage.app`)
     * and returns the public download URL (`https://firebasestorage.googleapis.com/v0/b/.../o/...?alt=media&token=...`).
     * Returns null if offline or if Firebase Storage bucket rules are not yet enabled in the Firebase Console.
     */
    suspend fun uploadBytesToFirebaseStorage(
        rawBytes: ByteArray,
        folder: String = "zaviro_uploads",
        fileName: String = "img_${System.currentTimeMillis()}.png",
        contentType: String = "image/png"
    ): String? = kotlinx.coroutines.withContext(Dispatchers.IO) {
        if (rawBytes.isEmpty()) return@withContext null
        try {
            val bucket = try {
                FirebaseApp.getInstance().options.storageBucket?.takeIf { it.isNotBlank() }
                    ?: "gen-lang-client-0737867339.firebasestorage.app"
            } catch (_: Exception) {
                "gen-lang-client-0737867339.firebasestorage.app"
            }
            val objectPath = "$folder/$fileName"
            val encodedName = android.net.Uri.encode(objectPath)
            val uploadUrl = "https://firebasestorage.googleapis.com/v0/b/$bucket/o?uploadType=media&name=$encodedName"

            val mediaType = with(okhttp3.MediaType.Companion) { contentType.toMediaTypeOrNull() }
            val body = with(okhttp3.RequestBody.Companion) { rawBytes.toRequestBody(mediaType) }

            val requestBuilder = okhttp3.Request.Builder()
                .url(uploadUrl)
                .post(body)
                .header("Content-Type", contentType)

            try {
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    val tokenResult = withTimeoutOrNull(2500L) {
                        currentUser.getIdToken(false).await()
                    }
                    val idToken = tokenResult?.token
                    if (!idToken.isNullOrBlank()) {
                        requestBuilder.header("Authorization", "Firebase $idToken")
                    }
                }
            } catch (_: Exception) {}

            storageHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val respBody = response.body?.string().orEmpty()
                if (respBody.isBlank()) return@withContext null
                val json = JSONObject(respBody)
                val downloadTokens = json.optString("downloadTokens", "")
                val firstToken = downloadTokens.split(",").firstOrNull()?.trim().orEmpty()
                return@withContext if (firstToken.isNotBlank()) {
                    "https://firebasestorage.googleapis.com/v0/b/$bucket/o/$encodedName?alt=media&token=$firstToken"
                } else {
                    "https://firebasestorage.googleapis.com/v0/b/$bucket/o/$encodedName?alt=media"
                }
            }
        } catch (e: Exception) {
            Log.d(tag, "Firebase Storage direct upload skipped/fallback to Firestore cloud reference: ${e.message}")
            null
        }
    }

    /**
     * Uploads image bytes to Firebase Storage first (returning the `https://firebasestorage.googleapis.com/...`
     * download URL), and automatically falls back to a portable Firestore cloud data URI (`data:image/...;base64,...`)
     * so every device can load the image from Firebase even if Firebase Storage rules are not yet open.
     */
    suspend fun uploadImageBytesToFirebaseOrCloudUri(
        rawBytes: ByteArray,
        folder: String = "zaviro_uploads",
        fileName: String = "img_${System.currentTimeMillis()}.png",
        maxDim: Int = 650
    ): String {
        val storageDownloadUrl = uploadBytesToFirebaseStorage(
            rawBytes = rawBytes,
            folder = folder,
            fileName = fileName
        )
        if (!storageDownloadUrl.isNullOrBlank()) {
            return storageDownloadUrl
        }
        return encodeBytesToCloudDataUri(rawBytes, maxDim = maxDim)
    }

    private suspend fun convertLocalFileUrlToCloudUrlOrDataUri(
        source: String,
        folder: String = "zaviro_uploads",
        maxDim: Int = 600
    ): String {
        val trimmed = source.trim()
        if (trimmed.isBlank()) return ""
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        if (trimmed.startsWith("file://") || trimmed.startsWith("/")) {
            return try {
                val file = File(trimmed.removePrefix("file://"))
                if (!file.exists() || file.length() == 0L) return ""
                uploadImageBytesToFirebaseOrCloudUri(
                    rawBytes = file.readBytes(),
                    folder = folder,
                    fileName = file.name.ifBlank { "img_${System.currentTimeMillis()}.png" },
                    maxDim = maxDim
                )
            } catch (e: Exception) {
                convertLocalFileUrlToFirestoreDataUri(trimmed, maxDim = maxDim)
            }
        }
        if (trimmed.startsWith("data:image")) {
            return try {
                val base64Part = trimmed.substringAfter(",")
                val rawBytes = Base64.decode(base64Part, Base64.DEFAULT)
                val storageUrl = uploadBytesToFirebaseStorage(
                    rawBytes = rawBytes,
                    folder = folder,
                    fileName = "img_${System.currentTimeMillis()}.png"
                )
                storageUrl ?: convertLocalFileUrlToFirestoreDataUri(trimmed, maxDim = maxDim)
            } catch (_: Exception) {
                convertLocalFileUrlToFirestoreDataUri(trimmed, maxDim = maxDim)
            }
        }
        return convertLocalFileUrlToFirestoreDataUri(trimmed, maxDim = maxDim)
    }

    private fun convertLocalFileUrlToFirestoreDataUri(source: String, maxDim: Int = 600): String {
        val trimmed = source.trim()
        if (trimmed.isBlank()) return ""
        if (trimmed.startsWith("data:image")) {
            if (trimmed.length <= 135_000) return trimmed
            return try {
                val base64Part = trimmed.substringAfter(",")
                val rawBytes = Base64.decode(base64Part, Base64.DEFAULT)
                encodeBytesToCloudDataUri(rawBytes, maxDim = maxDim)
            } catch (_: Exception) {
                trimmed
            }
        }
        if (!trimmed.startsWith("file://") && !trimmed.startsWith("/")) {
            return trimmed
        }
        return try {
            val file = File(trimmed.removePrefix("file://"))
            if (!file.exists() || file.length() == 0L) return ""
            encodeBytesToCloudDataUri(file.readBytes(), maxDim = maxDim)
        } catch (e: Exception) {
            Log.w(tag, "Could not convert local file for Firestore sync: ${e.message}")
            ""
        }
    }

    suspend fun saveBranding(branding: BrandingSettings) {
        val updated = saveBrandingLocal(branding)
        if (updated.masterLogoUrl.startsWith("file://")) {
            prefs.edit().putLong("local_exact_logo_uploaded_at", updated.updatedAt).commit()
        }
        if (currentAuthUid() == null) return
        try {
            val cloudMaster = convertLocalFileUrlToCloudUrlOrDataUri(updated.masterLogoUrl, folder = "zaviro_branding", maxDim = 512)
            val cloudSplash = if (updated.splashLogoUrl == updated.masterLogoUrl) {
                cloudMaster
            } else {
                convertLocalFileUrlToCloudUrlOrDataUri(updated.splashLogoUrl, folder = "zaviro_branding", maxDim = 512)
            }
            val cloudHomeHeader = if (updated.homeHeaderLogoUrl == updated.masterLogoUrl) {
                cloudMaster
            } else {
                convertLocalFileUrlToCloudUrlOrDataUri(updated.homeHeaderLogoUrl, folder = "zaviro_branding", maxDim = 512)
            }
            val cloudLogin = if (updated.loginLogoUrl == updated.masterLogoUrl) {
                cloudMaster
            } else {
                convertLocalFileUrlToCloudUrlOrDataUri(updated.loginLogoUrl, folder = "zaviro_branding", maxDim = 512)
            }
            val cloudAdmin = if (updated.adminLogoUrl == updated.masterLogoUrl) {
                cloudMaster
            } else {
                convertLocalFileUrlToCloudUrlOrDataUri(updated.adminLogoUrl, folder = "zaviro_branding", maxDim = 512)
            }

            val map = hashMapOf<String, Any>(
                "brandName" to updated.brandName,
                "tagline" to updated.tagline,
                "primaryColorHex" to updated.primaryColorHex,
                "secondaryColorHex" to updated.secondaryColorHex,
                "masterLogoUrl" to cloudMaster,
                "splashLogoUrl" to cloudSplash,
                "homeHeaderLogoUrl" to cloudHomeHeader,
                "loginLogoUrl" to cloudLogin,
                "adminLogoUrl" to cloudAdmin,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("settings").document("branding")
                .set(map, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "settings/branding")
            Log.e(tag, "Local branding saved; Firestore sync warning: ${e.message}")
        }
    }

    // ==================== DIRECT FIRESTORE REPOSITORY METHODS (RULE-ALIGNED) ====================

    suspend fun upsertCustomerProfileRemote(
        name: String,
        phone: String = "0300-1234567",
        email: String = "customer@example.com",
        address: String = "Gulberg III, Lahore"
    ): Result<String> {
        return try {
            val uid = requireUserId()
            val payload = mapOf(
                "uid" to uid,
                "name" to name,
                "phone" to phone,
                "email" to email,
                "address" to address,
                "notes" to "",
                "profileImageUrl" to "",
                "authProvider" to "google",
                "isAuthenticated" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("customers").document(uid).set(payload).await()
            Result.success(uid)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "customers")
            Result.failure(e)
        }
    }

    suspend fun getCustomerProfileByIdRemote(targetUid: String): Result<CustomerProfile> {
        return try {
            val doc = firestore.collection("customers").document(targetUid).get(Source.SERVER).await()
            val profile = CustomerProfile(
                uid = doc.getString("uid") ?: targetUid,
                name = doc.getString("name") ?: "",
                phone = doc.getString("phone") ?: "",
                email = doc.getString("email") ?: "",
                address = doc.getString("address") ?: "",
                notes = doc.getString("notes") ?: "",
                profileImageUrl = doc.getString("profileImageUrl") ?: "",
                authProvider = doc.getString("authProvider") ?: "google",
                isAuthenticated = doc.getBoolean("isAuthenticated") ?: true,
                updatedAt = doc.readEpochMillis("updatedAt", 0L)
            )
            Result.success(profile)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "customers/$targetUid")
            Result.failure(e)
        }
    }

    suspend fun createOrderRemote(
        customerName: String,
        phone: String,
        address: String,
        subtotal: Int = 550,
        deliveryFee: Int = 150
    ): Result<String> {
        return try {
            val uid = requireUserId()
            val orderId = "ord_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val payload = mapOf(
                "id" to orderId,
                "orderNumber" to "#ZAV-1001",
                "customerUid" to uid,
                "customerName" to customerName,
                "phone" to phone,
                "customerEmail" to "",
                "deliveryAddress" to address,
                "orderNotes" to "",
                "subtotal" to subtotal,
                "deliveryFee" to deliveryFee,
                "total" to (subtotal + deliveryFee),
                "status" to OrderStatus.RECEIVED.name,
                "paymentMethod" to "Cash on Delivery (COD)",
                "items" to listOf(
                    mapOf(
                        "id" to "item_1",
                        "name" to "Creamy Chicken Pasta",
                        "quantity" to 1,
                        "unitPrice" to subtotal,
                        "extrasSummary" to "",
                        "specialInstructions" to ""
                    )
                ),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("orders").document(orderId).set(payload).await()
            Result.success(orderId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "orders")
            Result.failure(e)
        }
    }

    suspend fun getUserOrdersRemote(): Result<List<String>> {
        return try {
            val uid = requireUserId()
            val snapshot = firestore.collection("orders")
                .whereEqualTo("customerUid", uid)
                .get(Source.SERVER)
                .await()
            Result.success(snapshot.documents.map { it.getString("id") ?: it.id })
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "orders")
            Result.failure(e)
        }
    }

    suspend fun getOrderByIdRemote(orderId: String): Result<String> {
        return try {
            val doc = firestore.collection("orders").document(orderId).get(Source.SERVER).await()
            Result.success(doc.getString("id") ?: doc.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "orders/$orderId")
            Result.failure(e)
        }
    }

    fun observeCustomerOrdersRemote(): Flow<List<String>> = callbackFlow {
        val uid = currentAuthUid() ?: ""
        val registration = firestore.collection("orders")
            .whereEqualTo("customerUid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "orders")
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.documents.map { it.getString("id") ?: it.id })
                }
            }
        awaitClose { registration.remove() }
    }

    fun addAddress(address: SavedAddress) {
        val newId = if (address.id.isBlank()) "addr_${System.currentTimeMillis()}" else address.id
        _savedAddresses.value = _savedAddresses.value + address.copy(id = newId)
        saveAddressesToPrefs(_savedAddresses.value)
    }

    fun removeAddress(addressId: String) {
        _savedAddresses.value = _savedAddresses.value.filter { it.id != addressId }
        saveAddressesToPrefs(_savedAddresses.value)
    }
}
