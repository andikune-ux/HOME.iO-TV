package dev.andikune.homeio.data

/**
 * AppKnowledge — Memory Knowledge & Instruction untuk Home.io
 * File ini dibaca oleh BackupManager untuk export ke .txt.
 * Versi ini mencakup seluruh evolusi dari Batch 0 hingga Final QA.
 */
object AppKnowledge {

    const val APP_NAME = "Home.io"
    const val APP_DESCRIPTION = "Launcher Android TV & STB dengan UI iOS 26 Liquid Glass"
    const val REPO_URL = "https://github.com/andikune-ux/HOME.iO-TV"
    const val REPO_OWNER = "andikune-ux"
    const val REPO_NAME = "HOME.iO-TV"

    // MEDIA SOSIAL (Diambil dari Masjid.io)
    const val SOCIAL_WA = "https://chat.whatsapp.com/ErJpG34fdzwL9FOmoh4fNN"
    const val SOCIAL_TIKTOK = "https://www.tiktok.com/@nayyra.une"
    const val SOCIAL_IG = "https://www.instagram.com/nayyra.une"

    val APP_STRUCTURE = """
app/src/main/java/dev/andikune/homeio/
├── HomeIoApplication.kt
├── MainActivity.kt                    -> Activity Utama (Splash, Navigasi, Dialog, OTA, Kiosk)
├── di/AppContainer.kt                 -> Dependency Injection
├── data/
│   ├── local/                         -> DataStore & Prefs
│   ├── remote/                        -> Retrofit (GitHub API, Open-Meteo)
│   └── AppKnowledge.kt                -> File ini (Memory AI)
├── domain/
│   ├── model/                         -> Model data (SettingsConfig, AppItem, dll)
│   └── repository/                    -> Interface
├── presentation/
│   ├── home/
│   │   └── HomeScreen.kt              -> UI Utama (Sliding Pane Overlay, Layar Terkunci, Widget Minimalis)
│   ├── settings/
│   │   ├── SettingsScreen.kt          -> 2-Pane Layout (8 Kategori)
│   │   ├── WallpaperSection.kt
│   │   ├── ScreensaverSection.kt
│   │   ├── PerformanceSection.kt      -> Cache Cleaner & RAM Booster
│   │   ├── LanguageSection.kt
│   │   ├── TvDisplaySection.kt        -> Safe Area & Brightness
│   │   ├── BackupBiasaSection.kt      -> Backup Tema (JSON)
│   │   ├── InfoAppSection.kt          -> Identitas, Backup Aman, Medsos, AI Prompt
│   │   ├── AppCacheDialog.kt          -> Dialog list cache app lain
│   │   └── (File Section lainnya)
│   ├── wallpaper/
│   │   ├── WallpaperView.kt           -> ExoPlayer (Video) & Image
│   │   └── FileManagerDialog.kt
│   ├── screensaver/
│   │   └── ScreensaverActivity.kt
│   ├── onboarding/
│   │   └── OnboardingPermissionDialog.kt
│   └── components/
│       ├── GlassTheme.kt              -> Modifier Liquid Glass (Real Blur API 31+)
│       ├── NeonFocusBorder.kt         -> Efek Glow Berputar D-Pad (Lightweight Pulse)
│       ├── TvSlider.kt & TvToggle.kt  -> Komponen D-Pad friendly
│       └── (Komponen UI lainnya)
├── service/
│   ├── UpdateDownloadService.kt       -> Foreground Service untuk OTA
│   └── (Service lainnya)
├── kiosk/
│   ├── KioskManager.kt                -> Lock Task Mode
│   ├── WatchdogService.kt             -> Auto-restart jika crash
│   └── AutoStartService.kt            -> Buka saat boot
├── receiver/
│   ├── BootReceiver.kt                -> Deteksi boot
│   └── UsbReceiver.kt                 -> Deteksi flashdisk
└── util/
    ├── OtaUpdate.kt                   -> Logic OTA (UpdateManager, ApkDownloader, UI Dialog)
    ├── CacheCleaner.kt                -> Logic RAM & Cache (Usage Access)
    ├── BackupBiasaManager.kt          -> Export/Import JSON
    ├── BackupManager.kt               -> Export TXT (AppKnowledge)
    ├── DateTimeFormatter.kt           -> Format Jam (Auto TZ, 24H/12H) & Tanggal (ID/EN)
    ├── LauncherHelper.kt              -> Pengecekan Default Launcher
    ├── CrashHandler.kt                -> Penangkap Error
    └── (Util lainnya)
    """.trimIndent()

    val APP_FEATURES = """
1. UI/UX iOS 26 Liquid Glass
   - Efek Blur asli (RenderEffect API 31+) via GlassTheme.kt.
   - Fallback solid transparan (API < 31).
   - Animasi Pulse Glow D-Pad (NeonFocusBorder) yang ringan & hemat RAM.
   - Suara klik (SoundFeedback) saat navigasi.

2. Home Screen (Modern Locked Layout)
   - Layar utama terkunci (Fixed), hanya daftar aplikasi yang bisa scroll horizontal.
   - Panel kiri model Sliding Pane (Overlay) meluncur dari kiri, dipicu tombol (☰) di tengah layar.
   - Widget Jam, Cuaca, RAM minimalis (tanpa border) terletak di pojok kanan atas.
   - Ikon aplikasi standar TV modern (Async Image Loading, anti OOM).
   - Context Menu (Long Press 500ms): Pin, App Info, Uninstall (kecuali sistem).
   - Safe Launch: Anti-crash saat membuka app yang sudah di-uninstall.

3. Wallpaper Engine
   - Foto & Video (ExoPlayer Mute + Seamless Loop).
   - Mode Skala: FIT, FILL, ZOOM, CROP.
   - Anti Memory Leak (Player dilepas saat ganti video).

4. Pengaturan 2-Pane Glass (8 Kategori)
   - Wallpaper, Screensaver, Performa, Bahasa, Tampilan TV, Pembaruan, Backup, Info.
   - Auto-save (Debounce) dan Force Save saat keluar layar.
   - Scroll berfungsi penuh via D-Pad & Touchscreen.

5. OTA Update via GitHub (Full Otomatis)
   - Auto-check saat splash selesai (Cooldown 6 jam).
   - Dialog Kaca (UpdateDialog): Changelog, Progress Bar, Tombol Skip/Nanti/Update.
   - Force Update: Jika tag release mengandung 'FORCE_UPDATE=true'.
   - Download via Foreground Service (Anti putus, notifikasi interaktif).
   - Simpan di /HomeiO/Pembaharuan/ (Tidak auto-hapus).
   - Retry Download jika koneksi putus.

6. Manajemen RAM & Cache
   - Widget RAM di Home dengan tombol Boost.
   - Clear cache app sendiri (100% works).
   - Baca ukuran cache app lain (butuh izin Usage Access).
   - Tombol pintas ke Storage Settings TV.

7. Kiosk & System
   - Lock Task Mode (cegah keluar aplikasi).
   - Watchdog Service (auto-restart jika crash).
   - Auto-Start saat TV booting.
   - Default Launcher Prompt (memandu user jadikan launcher utama).
   - Brightness Schedule & Safe Area.
   - USB OTG Popup (buka File Manager otomatis).

8. Backup & Restore
   - Backup Biasa (JSON): Menyimpan tema, wallpaper, bahasa, dll.
   - Backup Aman (TXT): Menyimpan AppKnowledge (memory AI) + semua prefs.
   - Lokasi terpusat di /HomeiO/.

9. Info Aplikasi & Tools
   - Identitas App & Versi.
   - Tombol Salin Prompt AI (untuk handoff ke AI baru).
   - Link Media Sosial.
   - Crash Log UI (Salin stack trace jika app error).
    """.trimIndent()

    val WORKFLOW_RULES = """
=== ATURAN FORMAT RESPON ===
1. Visualisasi folder (emoji + indentasi).
2. Path lengkap file (code block).
3. Nama file (code block).
4. URL edit / URL new (code block).
5. Kode TIMPA FULL (jangan suruh user edit manual, sepanjang apapun itu).
6. Pesan commit.

=== ANTI-TRUNCATION ===
- Batas aman copy-paste: 400-600 baris per bubble.
- Jika < 600 baris: kirim 1 BAGIAN.
- Jika > 600 baris: WAJIB 2 BAGIAN dalam 1 bubble.
- JANGAN potong di tengah fungsi.

=== ATURAN KERJA ===
1. Konfirmasi sebelum eksekusi.
2. JANGAN hapus fitur lama tanpa izin.
3. Lihat kode asli dulu sebelum timpa.
4. Kerjakan PER FILE (bukan per fitur).
5. Konfirmasi sebelum lanjut batch.

=== ATURAN KERAS (JANGAN DILANGGAR) ===
- JANGAN UBAH debug.keystore.
- JANGAN UBAH blok signingConfigs di build.gradle.kts.
- JANGAN UBAH .github/workflows/build.yml (bagian signing).
- Build HANYA via GitHub Actions.
- JANGAN install APK dari AI Studio (bentrok signature).
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
- Stack          : Kotlin, Jetpack Compose, Compose TV, Media3 ExoPlayer.

REPO: https://github.com/andikune-ux/HOME.iO-TV
    """.trimIndent()

    val KNOWN_ISSUES = """
=== SUDAH DIPERBAIKI (SELESAI) ===
1. Build error compileSdk, AGP, dan versi library.
2. Manifest & themes.xml kosong.
3. File keystore hilang (sudah otomatis via GitHub Actions).
4. Memory Leak pada Video Wallpaper.
5. OOM / Lag saat scroll App Grid (Bitmap dialihkan ke IO thread).
6. Data Loss saat keluar Settings (Force Save ditambahkan).
7. Force Update bisa di-bypass dengan tombol Back.
8. Crash saat membuka aplikasi yang sudah di-uninstall (Safe Launch).
9. Kiosk Mode crash (ditambahkan try-catch).
10. Cuaca tidak update (sekarang loop tiap 30 menit).
11. Auto-Focus D-Pad nyangkut (diperbaiki di SettingsScreen).
12. Notifikasi download tidak bisa diklik (PendingIntent ditambahkan).
13. Path fallback (Android/data) tidak terdeteksi (Warning UI ditambahkan).
14. Error parsing JSON GitHub (Null safety ditambahkan).
15. Release Tag kosong di GitHub Actions (Logika Bash timezone Jakarta).
16. NullPointer di CacheCleaner (applicationInfo null check).
17. Scroll macet di Settings & OTA (verticalScroll ditambahkan).
18. Format Tanggal/Jam menggunakan titik dua (:) bukan titik (.) (DateTimeFormatter Auto TZ).
19. Progress Download Error tidak reset (fix di OtaUpdate).
20. OOM (Out of Memory) pada AppItem & AppRepository (Drawable dihapus, ganti Async Image).
21. Layar Blur Total di Android 12 (Fix GlassTheme TIRAMISU).
22. Animasi NeonFocusBorder terlalu berat (Diganti animasi Pulse ringan).
23. D-Pad nyangkut saat panel kiri terbuka (Ditambah Focus Trap & Sliding Pane).
24. System.gc() dan Kill Processes membuat TV Freeze (Dihapus dari SystemUtils).
25. Format jam titik dua (:) dan zona waktu otomatis (DateTimeFormatter).

=== BELUM DIPERBAIKI (BATASAN ANDROID) ===
1. Hapus cache aplikasi LAIN secara otomatis tanpa root (Hanya bisa via list app + buka Settings App Info per app).
2. Notifikasi Foreground Service bisa gagal jika user menolak izin POST_NOTIFICATIONS (sudah ada handling, tapi butuh kesadaran user).
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

[21-10-2026] Unresolved reference 'background' di MainActivity
Solusi: hapus import androidx.compose.ui.background (salah package).

[21-10-2026] Argument type mismatch ApplicationInfo? di CacheCleaner
Solusi: tambahkan null-check (val appInfo: ApplicationInfo = pkg.applicationInfo ?: return@forEach).

[21-10-2026] HTTP 422 Validation Failed (tag_name is not valid)
Solusi: GitHub Actions tidak mengenali variabel Kotlin. Ubah logika build.yml menggunakan Bash (date + TZ=Asia/Jakarta).
    """.trimIndent()

    val BACKUP_INFO = """
=== BACKUP AMAN & BACKUP BIASA ===
Backup Aman (TXT): /sdcard/HomeiO/Backup aman/
Backup Biasa (JSON): /sdcard/HomeiO/Backup/
Format Backup Aman: Backup Aman-Home.io-dd-MM-yyyy-HHmmss.txt
Format Backup Biasa: Backup_HomeIO_HHmmss_ddMMyyyy.json
Fallback (jika izin storage ditolak):
- context.filesDir/HomeiO/...
- context.getExternalFilesDir(null)/HomeiO/...
    """.trimIndent()

    // PROMPT UNTUK SALIN CEPAT KE AI LAIN (HANDOFF)
    val AI_PROMPT = """
Halo AI. Saya pakai aplikasi Home.io (Android TV, Jetpack Compose).
Tolong baca konteks di bawah ini dulu sebelum saya kasih tugas.
Setelah paham, konfirmasi ke saya, baru kita mulai kerja.

=== IDENTITAS PROYEK ===
- Nama Aplikasi : Home.io
- Platform      : Android TV / STB (Jetpack Compose)
- Repo          : https://github.com/andikune-ux/HOME.iO-TV
- Package       : dev.andikune.homeio
- Versi Saat Ini: (Cek BuildConfig.VERSION_NAME)
- User          : Andi (HP Android, GitHub Mobile)

=== ATURAN FORMAT RESPON (WAJIB) ===
Setiap respon harus ada 6 elemen:
1. Visualisasi folder (emoji + indentasi)
2. Path lengkap file (code block)
3. Nama file (code block)
4. URL Edit (file lama) / URL New (file baru) (code block)
5. Kode TIMPA FULL (sepanjang apapun itu, jangan suruh edit manual)
6. Pesan commit

KETENTUAN:
- Pakai code block (3 backtick) untuk path/URL/nama file.
- Batas aman copy-paste: 400-600 baris. Jika > 600, bagi 2 BAGIAN dalam 1 bubble.
- JANGAN potong di tengah fungsi.

=== ATURAN KERJA ===
1. Konfirmasi dulu sebelum eksekusi.
2. Jujur kalau belum tahu, jangan menebak.
3. JANGAN hapus fitur lama tanpa izin.
4. Lihat kode asli dulu sebelum timpa.
5. Kerjakan per file (bukan per fitur).

=== ATURAN KERAS ===
- JANGAN PERNAH ubah debug.keystore.
- JANGAN PERNAH ubah signingConfigs.
- JANGAN PERNAH ubah build.yml (bagian signing).
- Build HANYA via GitHub Actions. JANGAN install APK dari AI Studio.

(Tulis tugas di sini)
    """.trimIndent()
}
