package dev.andikune.homeio.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SettingsConfig(
    // === WALLPAPER ===
    val wallpaperMode: String = "SINEMATIK",
    val wallpaperPath: String? = null,
    val customPhotoPath: String? = null,
    val customVideoPath: String? = null,
    val customActiveType: String = "PHOTO",
    val wallpaperType: String = "IMAGE",
    val wallpaperScaleMode: String = "FILL",
    val forceSoftwareDecoder: Boolean = false,

    // === FAVORITES ===
    val favoritePackages: List<String> = emptyList(),

    // === BAHASA & UMUM ===
    val languageCode: String = "in",
    val timeFormat: String = "24H",
    val showSeconds: Boolean = true,
    val isNtpCheckEnabled: Boolean = true,

    // === SCREENSAVER ===
    val screensaverEnabled: Boolean = true,
    val screensaverTimeoutMinutes: Int = 5,

    // === PERFORMA ===
    val isHighPerformanceBlurEnabled: Boolean = true,
    val isAutoRamBoostEnabled: Boolean = true,

    // === CUACA ===
    val weatherLocation: String = "Jakarta, ID",

    // === KIOSK & WATCHDOG ===
    val isKioskModeEnabled: Boolean = false,
    val isAutoStartOnBoot: Boolean = true,

    // === TAMPILAN TV ===
    val safeAreaPercent: Float = 2f,
    val brightnessMode: String = "MANUAL",
    val manualBrightness: Int = 100,
    val appIconSizeMultiplier: Float = 1.0f,
    
    // TAMBAHAN: Layar Tetap Nyala
    val isKeepScreenOnEnabled: Boolean = false
)
