import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.util.Base64

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.zaviro.mshdyc"
    minSdk = 24
    targetSdk = 36
    versionCode = 4
    versionName = "1.3"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  packaging {
    jniLibs {
      useLegacyPackaging = true
    }
    resources {
      excludes += setOf(
        "/META-INF/*.version",
        "/META-INF/AL2.0",
        "/META-INF/LGPL2.1"
      )
    }
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
val envFile = rootProject.file(".env")
if (!envFile.exists()) {
  val exampleEnvFile = rootProject.file(".env.example")
  if (exampleEnvFile.exists()) {
    exampleEnvFile.copyTo(envFile, overwrite = false)
  } else {
    envFile.writeText("")
  }
}

val debugKeystoreFile = rootProject.file("debug.keystore")
val debugKeystoreBase64File = rootProject.file("debug.keystore.base64")
if (!debugKeystoreFile.exists() && debugKeystoreBase64File.exists()) {
  val cleanBase64 = debugKeystoreBase64File.readText().replace("\\s+".toRegex(), "")
  debugKeystoreFile.writeBytes(Base64.getDecoder().decode(cleanBase64))
}

secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

tasks.withType<Test> {
  failFast = true
}

tasks.matching { it.name == "packageDebug" }.configureEach {
  doLast {
    val apkFile = layout.buildDirectory.file("outputs/apk/debug/app-debug.apk").get().asFile
    val ksFile = rootProject.file("debug.keystore")
    if (apkFile.exists() && ksFile.exists()) {
      val sdkRoot = System.getenv("ANDROID_SDK_ROOT")
        ?: System.getenv("ANDROID_HOME")
        ?: "/opt/android/sdk"
      val buildToolsDirs = file("$sdkRoot/build-tools").listFiles()?.sortedBy { it.name } ?: emptyList()
      val latestBuildTools = buildToolsDirs.lastOrNull()
      val zipalignBin = latestBuildTools?.resolve("zipalign")
      val apksignerBin = latestBuildTools?.resolve("apksigner")
      if (zipalignBin != null && zipalignBin.canExecute() && apksignerBin != null && apksignerBin.canExecute()) {
        val alignedApk = File(apkFile.parentFile, "app-debug-aligned.apk")
        ProcessBuilder(
          zipalignBin.absolutePath, "-f", "-p", "4",
          apkFile.absolutePath, alignedApk.absolutePath
        ).inheritIO().start().waitFor()
        if (alignedApk.exists() && alignedApk.length() > 0) {
          alignedApk.copyTo(apkFile, overwrite = true)
          alignedApk.delete()
        }
        ProcessBuilder(
          apksignerBin.absolutePath, "sign",
          "--ks", ksFile.absolutePath,
          "--ks-pass", "pass:android",
          "--key-pass", "pass:android",
          "--ks-key-alias", "androiddebugkey",
          "--min-sdk-version", "21",
          "--v1-signing-enabled", "true",
          "--v2-signing-enabled", "true",
          "--v3-signing-enabled", "true",
          apkFile.absolutePath
        ).inheritIO().start().waitFor()
      }
    }
  }
}


// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  // implementation(libs.androidx.room.ktx)
  // implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  // implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  // Firestore:
  implementation(libs.firebase.firestore)

  // Firebase Auth and Google Sign-In via Credential Manager:
  implementation(libs.firebase.auth)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)
  implementation(libs.googleid)
  // implementation(libs.firebase.appcheck.recaptcha)
  // implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  // implementation(libs.logging.interceptor)
  // implementation(libs.moshi.kotlin)
  // implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  // implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  // "ksp"(libs.androidx.room.compiler)
  // "ksp"(libs.moshi.kotlin.codegen)
}
