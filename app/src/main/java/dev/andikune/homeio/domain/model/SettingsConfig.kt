package dev.andikune.homeio.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SettingsConfig(
    // === WALLPAPER ===
    val wallpaperPath: String? = null,
    val wallpaperType: String = "IMAGE",
    val wallpaperScaleMode: String = "FILL",

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
    val safeAreaPercent: Float = 2f, // 0f - 10f
    val brightnessMode: String = "MANUAL", // MANUAL, AUTO, SCHEDULE
    val manualBrightness: Int = 100, // 0 - 100
    val appIconSizeMultiplier: Float = 0.8f // BARU: Default 0.8x (lebih kecil)
)
