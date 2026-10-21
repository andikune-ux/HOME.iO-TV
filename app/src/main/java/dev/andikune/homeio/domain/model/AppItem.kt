package dev.andikune.homeio.domain.model

/**
 * FIX CRITICAL (Temuan #26 & #67): 
 * Menghapus Drawable dari model data untuk mencegah Out Of Memory (OOM).
 * Ikon dan Banner akan dimuat secara asinkron oleh Coil di UI (HomeScreen).
 * 
 * FIX (Temuan #63 & #69):
 * Menambahkan isSystemApp untuk mencegah crash saat user mencoba uninstall aplikasi sistem.
 */
data class AppItem(
    val packageName: String,
    val activityName: String,
    val label: String,
    val isTvApp: Boolean = false,
    val isFavorite: Boolean = false,
    val isSideloaded: Boolean = false,
    val isSystemApp: Boolean = false
)
