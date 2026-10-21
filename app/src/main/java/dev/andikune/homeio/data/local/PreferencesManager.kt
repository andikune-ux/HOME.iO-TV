package dev.andikune.homeio.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.andikune.homeio.domain.model.SettingsConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "homeio_settings")

class PreferencesManager(private val context: Context) {
    companion object {
        private val KEY_WALLPAPER_PATH = stringPreferencesKey("wallpaper_path")
        private val KEY_WALLPAPER_TYPE = stringPreferencesKey("wallpaper_type")
        private val KEY_SCALE_MODE = stringPreferencesKey("scale_mode")
        private val KEY_FAVORITES = stringSetPreferencesKey("favorite_packages")
        
        private val KEY_LANGUAGE = stringPreferencesKey("language_code")
        private val KEY_TIME_FORMAT = stringPreferencesKey("time_format")
        private val KEY_SHOW_SECONDS = booleanPreferencesKey("show_seconds")
        private val KEY_NTP_CHECK = booleanPreferencesKey("ntp_check")
        
        private val KEY_SCREENSAVER_ENABLED = booleanPreferencesKey("screensaver_enabled")
        private val KEY_SCREENSAVER_TIMEOUT = intPreferencesKey("screensaver_timeout")
        
        private val KEY_BLUR_MODE = booleanPreferencesKey("blur_mode_high_perf")
        private val KEY_AUTO_RAM_BOOST = booleanPreferencesKey("auto_ram_boost")
        private val KEY_WEATHER_LOCATION = stringPreferencesKey("weather_location")
        
        private val KEY_KIOSK_MODE = booleanPreferencesKey("kiosk_mode")
        private val KEY_AUTO_START_BOOT = booleanPreferencesKey("auto_start_boot")
        
        private val KEY_SAFE_AREA = floatPreferencesKey("safe_area_percent")
        private val KEY_BRIGHTNESS_MODE = stringPreferencesKey("brightness_mode")
        private val KEY_MANUAL_BRIGHTNESS = intPreferencesKey("manual_brightness")
        
        // KEY BARU UNTUK UKURAN IKON
        private val KEY_APP_ICON_SIZE = floatPreferencesKey("app_icon_size")
    }

    val settingsFlow: Flow<SettingsConfig> = context.dataStore.data.map { prefs ->
        SettingsConfig(
            wallpaperPath = prefs[KEY_WALLPAPER_PATH],
            wallpaperType = prefs[KEY_WALLPAPER_TYPE] ?: "IMAGE",
            wallpaperScaleMode = prefs[KEY_SCALE_MODE] ?: "FILL",
            favoritePackages = prefs[KEY_FAVORITES]?.toList() ?: emptyList(),
            
            languageCode = prefs[KEY_LANGUAGE] ?: "in",
            timeFormat = prefs[KEY_TIME_FORMAT] ?: "24H",
            showSeconds = prefs[KEY_SHOW_SECONDS] ?: true,
            isNtpCheckEnabled = prefs[KEY_NTP_CHECK] ?: true,
            
            screensaverEnabled = prefs[KEY_SCREENSAVER_ENABLED] ?: true,
            screensaverTimeoutMinutes = prefs[KEY_SCREENSAVER_TIMEOUT] ?: 5,
            
            isHighPerformanceBlurEnabled = prefs[KEY_BLUR_MODE] ?: true,
            isAutoRamBoostEnabled = prefs[KEY_AUTO_RAM_BOOST] ?: true,
            weatherLocation = prefs[KEY_WEATHER_LOCATION] ?: "Jakarta, ID",
            
            isKioskModeEnabled = prefs[KEY_KIOSK_MODE] ?: false,
            isAutoStartOnBoot = prefs[KEY_AUTO_START_BOOT] ?: true,
            
            safeAreaPercent = prefs[KEY_SAFE_AREA] ?: 2f,
            brightnessMode = prefs[KEY_BRIGHTNESS_MODE] ?: "MANUAL",
            manualBrightness = prefs[KEY_MANUAL_BRIGHTNESS] ?: 100,
            
            appIconSizeMultiplier = prefs[KEY_APP_ICON_SIZE] ?: 0.8f
        )
    }

    suspend fun updateSettings(config: SettingsConfig) {
        context.dataStore.edit { prefs ->
            if (config.wallpaperPath != null) {
                prefs[KEY_WALLPAPER_PATH] = config.wallpaperPath
            } else {
                prefs.remove(KEY_WALLPAPER_PATH)
            }
            prefs[KEY_WALLPAPER_TYPE] = config.wallpaperType
            prefs[KEY_SCALE_MODE] = config.wallpaperScaleMode
            prefs[KEY_FAVORITES] = config.favoritePackages.toSet()
            
            prefs[KEY_LANGUAGE] = config.languageCode
            prefs[KEY_TIME_FORMAT] = config.timeFormat
            prefs[KEY_SHOW_SECONDS] = config.showSeconds
            prefs[KEY_NTP_CHECK] = config.isNtpCheckEnabled
            
            prefs[KEY_SCREENSAVER_ENABLED] = config.screensaverEnabled
            prefs[KEY_SCREENSAVER_TIMEOUT] = config.screensaverTimeoutMinutes
            
            prefs[KEY_BLUR_MODE] = config.isHighPerformanceBlurEnabled
            prefs[KEY_AUTO_RAM_BOOST] = config.isAutoRamBoostEnabled
            prefs[KEY_WEATHER_LOCATION] = config.weatherLocation
            
            prefs[KEY_KIOSK_MODE] = config.isKioskModeEnabled
            prefs[KEY_AUTO_START_BOOT] = config.isAutoStartOnBoot
            
            prefs[KEY_SAFE_AREA] = config.safeAreaPercent
            prefs[KEY_BRIGHTNESS_MODE] = config.brightnessMode
            prefs[KEY_MANUAL_BRIGHTNESS] = config.manualBrightness
            
            prefs[KEY_APP_ICON_SIZE] = config.appIconSizeMultiplier
        }
    }
}
