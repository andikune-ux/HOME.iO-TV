package dev.andikune.homeio.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SettingsConfig(
    // === WALLPAPER ===
    val wallpaperMode: String = "SINEMATIK", // SINEMATIK, ESTETIK, OLED, KUSTOM
    val wallpaperPath: String? = null, // Fallback untuk backward compatibility
    val customPhotoPath: String? = null,
    val customVideoPath: String? = null,
    val customActiveType: String = "PHOTO", // PHOTO, VIDEO
    val wallpaperType: String = "IMAGE", // IMAGE, VIDEO (Fallback lama)
    val wallpaperScaleMode: String = "FILL",
    
    // TAMBAHAN BARU KITA
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
    val brightnessMode: String = "MANUAL", // MANUAL, AUTO, SCHEDULE
    val manualBrightness: Int = 100,
    val appIconSizeMultiplier: Float = 0.8f
)
