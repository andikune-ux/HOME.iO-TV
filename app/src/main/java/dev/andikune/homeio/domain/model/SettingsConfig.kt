package dev.andikune.homeio.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SettingsConfig(
    // --- WALLPAPER & SKALA ---
    val wallpaperMode: String = "SINEMATIK",
    val wallpaperScaleMode: String = "FILL",
    val customPhotoPath: String? = null,
    val customVideoPath: String? = null,
    val customActiveType: String = "PHOTO", // "PHOTO" atau "VIDEO"
    val wallpaperPath: String? = null, // Path lama
    val wallpaperType: String = "PHOTO", // Path lama
    val forceSoftwareDecoder: Boolean = false, // Switch Software Decoder

    // --- UI & TAMPILAN ---
    val appIconSizeMultiplier: Float = 1.0f,
    val safeAreaPercent: Int = 0,
    val isHighPerformanceBlurEnabled: Boolean = true,
    val favoritePackages: List<String> = emptyList(),
    
    // --- SISTEM & PERFORMA ---
    val isKioskModeEnabled: Boolean = false,
    val isAutoRamBoostEnabled: Boolean = false,
    
    // --- BAHASA & WAKTU ---
    val languageCode: String = "id",
    val timeFormat: String = "24h",
    val showSeconds: Boolean = false,
    
    // --- CUACA & LOKASI ---
    val weatherLocation: String = "Jakarta, ID",
    
    // --- SCREENSAVER ---
    val screensaverEnabled: Boolean = true,
    val screensaverTimeoutMinutes: Int = 5,
    
    // --- KECERAHAN ---
    val brightnessMode: String = "MANUAL", // "MANUAL", "SCHEDULE", "AUTO"
    val manualBrightness: Int = 100
)
