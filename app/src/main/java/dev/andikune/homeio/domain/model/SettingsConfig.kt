package dev.andikune.homeio.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SettingsConfig(
    // === WALLPAPER ===
    val wallpaperPath: String? = null,
    val wallpaperType: String = "IMAGE", // IMAGE or VIDEO
    val wallpaperScaleMode: String = "FILL", // FIT, FILL, ZOOM, CROP

    // === FAVORITES ===
    val favoritePackages: List<String> = emptyList(),

    // === BAHASA & UMUM ===
    val languageCode: String = "in",
    val timeFormat: String = "24H", // "24H" or "12H"  — NEW
    val showSeconds: Boolean = true, // — NEW
    val isNtpCheckEnabled: Boolean = true,

    // === SCREENSAVER ===
    val screensaverEnabled: Boolean = true, // — NEW
    val screensaverTimeoutMinutes: Int = 5,

    // === PERFORMA ===
    val isHighPerformanceBlurEnabled: Boolean = true,
    val isAutoRamBoostEnabled: Boolean = true,

    // === CUACA ===
    val weatherLocation: String = "Jakarta, ID" // — NEW
)
