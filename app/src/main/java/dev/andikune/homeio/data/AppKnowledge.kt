package dev.andikune.homeio.data

/**
 * AppKnowledge — Memory Knowledge & Instruction untuk Home.io
 * File ini dibaca oleh BackupManager untuk export ke .txt
 */
object AppKnowledge {

    const val APP_NAME = "Home.io"
    const val APP_DESCRIPTION = "Launcher Android TV & STB dengan UI iOS 26 Liquid Glass"
    const val REPO_URL = "https://github.com/andikune-ux/HOME.iO-TV"
    const val REPO_OWNER = "andikune-ux"
    const val REPO_NAME = "HOME.iO-TV"

    val APP_STRUCTURE = """
app/src/main/java/dev/andikune/homeio/
├── HomeIoApplication.kt
├── MainActivity.kt
├── di/AppContainer.kt
├── data/
│   ├── local/         -> DataStore, FileProvider, Backup
│   ├── remote/        -> Retrofit GitHub Releases, Open-Meteo
│   ├── model/         -> Model data aplikasi
│   └── repository/    -> Implementasi repository
├── domain/
│   ├── model/         -> Domain entities
│   ├── repository/    -> Interface
│   └── usecase/       -> Business logic
├── presentation/
│   ├── home/          -> HomeScreen (horizontal-only)
│   ├── settings/      -> SettingsScreen (2-pane glass)
│   ├── wallpaper/     -> WallpaperEngine (foto/video)
│   ├── screensaver/   -> ScreensaverActivity (OLED)
│   ├── crashlog/      -> UI crash log
│   ├── splash/        -> Splash animation
│   └── components/    -> GlassCard, FocusableRow, dll
├── service/           -> DownloadService (OTA), BootReceiver
└── util/              -> CrashHandler, BackupManager, BlurEffect
    """.trimIndent()

    val APP_FEATURES = """
1. UI/UX iOS 26 Liquid Glass
   - Efek blur (RenderEffect API 31+)
   - Sudut membulat, glowing shadow
   - Fallback solid transparan (API < 31)

2. Home Screen HORIZONTAL-ONLY
   - Panel kiri: Jam, Search, Home, System & Utils
   - Panel kanan: Widget Cuaca, Info Sistem, Favorite Apps (3), All Apps
   - Context Menu (Long Press 500ms)
   - Smart Icon Handler (TV banner vs HP icon)
   - Full D-Pad + Touch support
   - Sound Feedback saat navigasi

3. Widget Real-time
   - Cuaca (GPS + Open-Meteo, tanpa API key)
   - Info Sistem (RAM, Storage)
   - NTP Time Check

4. Wallpaper Engine
   - Foto/Video kustom
   - ExoPlayer Mute + Seamless Loop
   - Scale Mode: Fit/Fill/Zoom/Crop
   - In-App File Manager + Gallery intent

5. Pengaturan 2-Pane Glass Split Screen
   - Kategori kiri, Kontrol kanan
   - Semua submenu bisa scroll (kecuali Home)

6. OTA Update via GitHub
   - Foreground Service (tidak batal saat Back)
   - Retry dialog jika gagal
   - Path: /HomeiO/Pembaharuan/
   - In-App Package Installer

7. Backup Aman
   - Export AppKnowledge + Prefs ke .txt
   - Path: /HomeiO/Backup aman/

8. Screensaver OLED
   - Brightness 0% + Timer slider
   - Wake-up via remote/touch

9. System Utilities
   - Auto-start (BOOT_COMPLETED)
   - USB OTG Popup
   - RAM Booster otomatis on Home
   - Splash Screen animasi kaca

10. Debugging
    - UncaughtExceptionHandler
    - UI Crash Log + Salin Stack Trace
    """.trimIndent()

    val WORKFLOW_RULES = """
=== ATURAN FORMAT RESPON ===
1. Visualisasi folder (emoji + indentasi)
2. Path lengkap file (code block)
3. Nama file (code block)
4. URL edit / URL new (code block)
5. Kode TIMPA FULL (jangan suruh user edit manual)
6. Pesan commit

=== ANTI-TRUNCATION ===
- Batas aman copy-paste: 400-600 baris per bubble
- Jika < 600 baris: kirim 1 BAGIAN
- Jika > 600 baris: WAJIB 2 BAGIAN dalam 1 bubble
- JANGAN potong di tengah fungsi

=== ATURAN KERJA ===
1. Konfirmasi sebelum eksekusi
2. JANGAN hapus fitur lama tanpa izin
3. Lihat kode asli dulu sebelum timpa
4. Kerjakan PER FILE (bukan per fitur)
5. Konfirmasi sebelum lanjut batch

=== ATURAN KERAS (JANGAN DILANGGAR) ===
- JANGAN UBAH debug.keystore
- JANGAN UBAH blok signingConfigs di build.gradle.kts
- JANGAN UBAH .github/workflows/build.yml (bagian signing)
- Build HANYA via GitHub Actions
- JANGAN install APK dari AI Studio
    """.trimIndent()

    val DEVELOPER_INSTRUCTION = """
IDENTITAS USER:
- Nama  : Andi
- Email : andikune@gmail.com
- Device: HP Android (GitHub Mobile)

APLIKASI:
- Nama           : Home.io
- Package        : dev.andikune.homeio
- Platform       : Android TV / STB
- Min SDK        : 24 (Android 7.0)
- Target SDK     : 35 (Android 15)
- Stack          : Kotlin, Jetpack Compose, Compose TV

REPO: https://github.com/andikune-ux/HOME.iO-TV
    """.trimIndent()

    val KNOWN_ISSUES = """
=== SUDAH DIPERBAIKI ===
1. Build error compileSdk (eksperimental -> 35)
2. AGP 8.5.0 -> 8.6.0 (wajib untuk tv-material 1.1.0)
3. Compose BOM 2024.06.00 -> 2024.10.01
4. tv-material 1.0.0-alpha11 -> 1.1.0
5. Manifest kosong -> lengkap dengan permissions & service
6. themes.xml kosong -> Theme.HomeIo valid
7. debug.keystore -> permanen di repo via workflow

=== BELUM DIPERBAIKI ===
1. Liquid Glass blur asli (RenderEffect API 31+)
2. HomeScreen masih scroll vertikal (harus horizontal-only)
3. Wallpaper Engine (foto/video) belum ada
4. DownloadService OTA belum fungsional
5. ScreensaverActivity belum ada
6. Crash Log UI belum ada
7. Widget Cuaca masih dummy
8. RAM Booster belum ada
    """.trimIndent()

    val BUILD_ERROR_HISTORY = """
=== RIWAYAT BUILD ERROR ===

[20-10-2026] KSP Configuration Cache Error
Solusi: android.useAndroidX=true + configuration-cache=false

[20-10-2026] tv-material:1.0.0-alpha11 not found
Solusi: upgrade ke 1.1.0 (versi stabil)

[20-10-2026] AAR Metadata require compileSdk 35
Solusi: naikkan compileSdk & targetSdk ke 35, AGP ke 8.6.0

[20-10-2026] debug.keystore not found
Solusi: jalankan workflow setup-keystore.yml (sekali saja)
    """.trimIndent()

    val BACKUP_INFO = """
=== BACKUP AMAN ===
Path: /sdcard/HomeiO/Backup aman/
Format Nama: Backup Aman-Home.io-dd-MM-yyyy-HHmmss.txt
Fallback: context.filesDir/HomeiO/Backup aman/
    """.trimIndent()
}
