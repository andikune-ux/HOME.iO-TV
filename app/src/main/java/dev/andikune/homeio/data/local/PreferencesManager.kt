package dev.andikune.homeio.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
        private val KEY_SCREENSAVER_TIMEOUT = intPreferencesKey("screensaver_timeout")
        private val KEY_BLUR_MODE = booleanPreferencesKey("blur_mode_high_perf")
        private val KEY_AUTO_RAM_BOOST = booleanPreferencesKey("auto_ram_boost")
        private val KEY_NTP_CHECK = booleanPreferencesKey("ntp_check")
    }

    val settingsFlow: Flow<SettingsConfig> = context.dataStore.data.map { prefs ->
        SettingsConfig(
            wallpaperPath = prefs[KEY_WALLPAPER_PATH],
            wallpaperType = prefs[KEY_WALLPAPER_TYPE] ?: "IMAGE",
            wallpaperScaleMode = prefs[KEY_SCALE_MODE] ?: "FILL",
            favoritePackages = prefs[KEY_FAVORITES]?.toList() ?: emptyList(),
            languageCode = prefs[KEY_LANGUAGE] ?: "in",
            screensaverTimeoutMinutes = prefs[KEY_SCREENSAVER_TIMEOUT] ?: 5,
            isHighPerformanceBlurEnabled = prefs[KEY_BLUR_MODE] ?: true,
            isAutoRamBoostEnabled = prefs[KEY_AUTO_RAM_BOOST] ?: true,
            isNtpCheckEnabled = prefs[KEY_NTP_CHECK] ?: true
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
            prefs[KEY_SCREENSAVER_TIMEOUT] = config.screensaverTimeoutMinutes
            prefs[KEY_BLUR_MODE] = config.isHighPerformanceBlurEnabled
            prefs[KEY_AUTO_RAM_BOOST] = config.isAutoRamBoostEnabled
            prefs[KEY_NTP_CHECK] = config.isNtpCheckEnabled
        }
    }
}
