package dev.andikune.homeio.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SettingsConfig(
    val wallpaperPath: String? = null,
    val wallpaperType: String = "IMAGE", // IMAGE or VIDEO
    val wallpaperScaleMode: String = "FILL", // FIT, FILL, ZOOM, CROP
    val favoritePackages: List<String> = emptyList(),
    val languageCode: String = "in",
    val screensaverTimeoutMinutes: Int = 5,
    val isHighPerformanceBlurEnabled: Boolean = true,
    val isAutoRamBoostEnabled: Boolean = true,
    val isNtpCheckEnabled: Boolean = true
)
