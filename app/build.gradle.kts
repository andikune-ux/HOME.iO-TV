import java.util.Calendar
import java.util.TimeZone

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// ============================================================
// VERSIONING OTOMATIS HOME.IO
// Format: v.[bulan_index].[tanggal].[commit]
// Contoh: v.1.20.14
//   - Bulan Index  : Oktober 2026 = 1 (naik +1 setiap ganti bulan)
//   - Tanggal      : Hari ini real-time
//   - Commit       : GITHUB_RUN_NUMBER (fallback timestamp lokal)
// ============================================================
val jakartaTz = TimeZone.getTimeZone("Asia/Jakarta")
val nowCalendar = Calendar.getInstance(jakartaTz)

val START_YEAR = 2026
val START_MONTH = 10 // Oktober

val currentYear = nowCalendar.get(Calendar.YEAR)
val currentMonth = nowCalendar.get(Calendar.MONTH) + 1 // Jan=1
val currentDay = nowCalendar.get(Calendar.DAY_OF_MONTH)

val monthsSinceStart = (currentYear - START_YEAR) * 12 + (currentMonth - START_MONTH)
val monthIndex = (monthsSinceStart + 1).coerceAtLeast(1)

val githubRunNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()
val fallbackCommit = ((System.currentTimeMillis() / 1000) % 10000).toInt()
val commitNumber = githubRunNumber ?: fallbackCommit

val autoVersionName = "v.$monthIndex.$currentDay.$commitNumber"
val autoVersionCode = monthIndex * 1_000_000 + currentDay * 10_000 + (commitNumber % 10_000)

android {
    namespace = "dev.andikune.homeio"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.andikune.homeio"
        minSdk = 24
        targetSdk = 35
        versionCode = autoVersionCode
        versionName = autoVersionName
        buildConfigField("String", "VERSION_NAME", "\"$autoVersionName\"")
        buildConfigField("int", "VERSION_CODE", "$autoVersionCode")
    }

    // ============================================================
    // JANGAN UBAH BLOK SIGNING CONFIGS INI!
    // ============================================================
    signingConfigs {
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
            signingConfig = signingConfigs.getByName("debugConfig")
        }
        debug {
            signingConfig = signingConfigs.getByName("debugConfig")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.coil.compose)
    implementation(libs.retrofit)
    implementation(libs.converter.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
