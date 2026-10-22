package dev.andikune.homeio.data

/**
 * AppKnowledge — Memory Knowledge & Instruction untuk HOME.iO
 * File ini dibaca oleh BackupManager untuk export ke .txt.
 * Versi ini mencakup seluruh evolusi dari Batch 0 hingga Final QA (Carousel & QRIS).
 */
object AppKnowledge {

    const val APP_NAME = "HOME.iO TV"
    const val APP_DESCRIPTION = "Launcher Android TV & STB dengan UI iOS 26 Liquid Glass"
    const val REPO_URL = "https://github.com/andikune-ux/HOME.iO-TV"
    const val REPO_OWNER = "andikune-ux"
    const val REPO_NAME = "HOME.iO-TV"

    // MEDIA SOSIAL (WA Sudah Diperbarui)
    const val SOCIAL_WA = "https://chat.whatsapp.com/BAejUrfRW39FOt8biJleUM?s=cl&p=a&mlu=4&ilr=4"
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
│   │   └── HomeScreen.kt              -> UI Utama (Center-Locked Carousel, Sliding Pane, Widget)
│   ├── onboarding/
│   │   ├── OnboardingPermissionDialog.kt -> Dialog Izin Awal
│   │   └── QrisDonationPopup.kt       -> Dialog Donasi QRIS (7s delay, Mampir/Tutup)
│   ├── settings/
│   │   ├── SettingsScreen.kt          -> 2-Pane Layout (8 Kategori)
│   │   ├── WallpaperSection.kt
│   │   ├── ScreensaverSection.kt
│   │   ├── PerformanceSection.kt      -> Cache Cleaner & RAM Booster
│   │   ├── LanguageSection.kt
│   │   ├── TvDisplaySection.kt        -> Safe Area, Brightness, App Icon Size
│   │   ├── BackupBiasaSection.kt      -> Backup Tema (JSON)
│   │   ├── InfoAppSection.kt          -> Identitas, Backup Aman, Medsos, AI Prompt
│   │   ├── AppCacheDialog.kt          -> Dialog list cache app lain
│   │   └── (File Section lainnya)
│   ├── wallpaper/
│   │   ├── WallpaperView.kt           -> ExoPlayer (Video) & Image
│   │   └── FileManagerDialog.kt
│   ├── screensaver/
│   │   └── ScreensaverActivity.kt     -> Fix Layar Hitam 0% Pekat
│   └── components/
│       ├── GlassTheme.kt              -> Modifier Liquid Glass (Blur API 31+)
│       ├── NeonFocusBorder.kt         -> Animasi Glow Ringan (Tanpa Rotasi Berat)
│       ├── TvSlider.kt & TvToggle.kt  -> Komponen D-Pad friendly
│       └── (Komponen UI lainnya)
├── service/
│   ├── UpdateDownloadService.kt       -> Foreground Service untuk OTA
│   └── LauncherNotificationService.kt -> Listener Notifikasi Sistem Asli
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

2. HOME SCREEN (Modern Center-Locked Carousel)
   - Layar utama terkunci (Fixed). Hanya daftar aplikasi & Pane yang bisa bergerak.
   - Widget Jam, Tanggal, Cuaca, dan RAM minimalis (tanpa border) di pojok kanan atas.
   - Tombol Settings (⚙️) di samping Jam.
   - Aplikasi Favorit (3 kotak statis).
   - Semua Aplikasi (Center-Locked Carousel): Border statis di tengah, aplikasi bergeser melewatinya.
   - Fitur D-Pad Ujung: Menekan Kiri di App 1 membuka Pane. Menekan Kanan di App terakhir membuka Grid Overlay.
   - Fitur Sentuh (HP): Swipe untuk scroll, Tap App di pinggir untuk ke tengah, Tap App di tengah untuk buka.
   - D-Pad Bawah membuka Overlay Grid (Semua Aplikasi).

3. Sliding Pane Kiri (Overlay)
   - Dipicu oleh tombol Hamburger (☰) di tengah kiri layar.
   - Overlay transparan dengan Scrim (klik luar untuk tutup di HP).
   - Focus Trap: D-Pad terkunci di dalam Pane saat terbuka.
   - Berisi: Search, Notifikasi Asli (Notification Listener), Home, RAM Booster, File Manager, Sideload, Settings.
   - Jika izin notifikasi belum aktif, muncul tombol pintas untuk ke Pengaturan Android.

4. Pop-up Donasi QRIS (Awal Buka Aplikasi)
   - Muncul otomatis setelah Onboarding selesai (Sebelum cek update).
   - Tampil 7 detik dengan loading berputar & teks "Lagi menyiapkan aplikasi...".
   - Teks di atas gambar: "Donasi buat admin beli kopi".
   - Tombol "Mampir" (Buka WA) dan "Tutup" (Lanjut App).
   - D-Pad terkunci di Pop-up.

5. Wallpaper Engine
   - Foto & Video (ExoPlayer Mute + Seamless Loop).
   - Mode Skala: FIT, FILL, ZOOM, CROP.
   - Anti Memory Leak (Player dilepas saat ganti video).

6. Pengaturan 2-Pane Glass (8 Kategori)
   - Wallpaper, Screensaver, Performa, Bahasa, Tampilan TV, Pembaruan, Backup, Info.
   - Auto-save (Debounce) dan Force Save saat keluar layar.
   - Tampilan TV: Atur Safe Area, Kecerahan, Kiosk Mode, Auto-Start, dan UKURAN IKON APLIKASI (Slider 0.6x - 1.5x).

7. OTA Update via GitHub (Full Otomatis)
   - Auto-check saat splash selesai (Cooldown 6 jam).
   - Dialog Kaca (UpdateDialog): Changelog, Progress Bar, Tombol Skip/Nanti/Update.
   - Force Update: Jika tag release mengandung 'FORCE_UPDATE=true'.
   - Download via Foreground Service (Anti putus, notifikasi interaktif).

8. Manajemen RAM & Cache
   - Widget RAM di Home dengan tombol Boost (menampilkan Toast sukses).
   - Clear cache app sendiri (100% works).
   - Baca ukuran cache app lain (butuh izin Usage Access).

9. Kiosk & System
   - Lock Task Mode, Watchdog Service, Auto-Start saat TV booting.
   - Default Launcher Prompt.
   - Screensaver benar-benar 0% (Hitam Pekat).
   - USB OTG Popup.

10. Backup & Restore
    - Backup Biasa (JSON) & Backup Aman (TXT).

11. Info Aplikasi & Tools
    - Identitas App & Versi.
    - Tombol Salin Prompt AI.
    - Link Media Sosial (WA, TikTok, IG).
    """.trimIndent()

    val WORKFLOW_RULES = """
=== ATURAN FORMAT RESPON ===
1. Visualisasi folder (emoji + indentasi).
2. Path lengkap file (code block).
3. Nama file (code block).
4. URL edit / URL new (code block).
5. Kode TIMPA FULL (JANGAN SURUH EDIT MANUAL, SEPANJANG APAPUN ITU).
6. Pesan commit.

=== ANTI-TRUNCATION ===
- Batas aman copy-paste: 400-600 baris per bubble.
- Jika < 600 baris: kirim 1 BAGIAN.
- Jika > 600 baris: WAJIB dibagi menjadi beberapa BAGIAN di dalam 1 bubble.
- JANGAN potong di tengah fungsi.

=== ATURAN KERJA ===
1. Konfirmasi sebelum eksekusi. TUNGGU aba-aba "EKSEKUSI" dari user.
2. Jujur kalau belum tahu, jangan menebak.
3. JANGAN hapus fitur lama tanpa izin.
4. Lihat kode asli dulu sebelum timpa.
5. Kerjakan PER FILE (bukan per fitur).

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
- Nama           : HOME.iO TV
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
5. OOM / Lag saat scroll App Grid (Drawable dihapus dari AppItem, ganti Async Image).
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
20. Layar Blur Total di Android 12 (Fix GlassTheme, cek API >= TIRAMISU).
21. Animasi NeonFocusBorder terlalu berat (Diganti animasi Pulse ringan).
22. D-Pad nyangkut saat panel kiri terbuka (Ditambah Focus Trap & Scrim).
23. System.gc() dan Kill Processes membuat TV Freeze (Dihapus dari SystemUtils).
24. Infinite Constraints Crash di SettingsScreen (Ganti Column(verticalScroll) ke LazyColumn).
25. QRIS Popup tidak muncul (Logic di MainActivity diperbaiki).
26. QRIS Popup tombol terpotong (Ditambah verticalScroll di dalam Popup).
27. Border Carousel tidak membungkus ikon (Ditambah scale 1.15f pada Border statis).
28. Posisi awal Carousel mentok kiri (Set initialFirstVisibleItemIndex = 2).
29. Notifikasi asli belum ada (Ditambahkan LauncherNotificationService).
30. Tombol RAM Booster tidak bereaksi (Ditambah Toast feedback).

=== BELUM DIPERBAIKI (BATASAN ANDROID) ===
1. Hapus cache aplikasi LAIN secara otomatis tanpa root (Hanya bisa via list app + buka Settings App Info per app).
2. Notifikasi Foreground Service bisa gagal jika user menolak izin POST_NOTIFICATIONS.
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

[22-10-2026] Unresolved reference 'zIndex' di HomeScreen.kt
Solusi: Import yang benar adalah `import androidx.compose.ui.zIndex` (bukan ui.draw.zIndex).

[22-10-2026] Syntax error 'Expecting a top level declaration' di MainActivity.kt
Solusi: File korup akibat paste terpotong atau cache. Timpa ulang file MainActivity.kt dengan Full Kode.
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
Halo AI. Saya pakai aplikasi HOME.iO TV (Android TV, Jetpack Compose).
Tolong baca konteks di bawah ini dulu sebelum saya kasih tugas.
Setelah paham, konfirmasi ke saya, baru kita mulai kerja.

=== IDENTITAS PROYEK ===
- Nama Aplikasi : HOME.iO TV
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
5. Kode TIMPA FULL (JANGAN SURUH EDIT MANUAL, SEPANJANG APAPUN ITU)
6. Pesan commit

KETENTUAN:
- Pakai code block (3 backtick) untuk path/URL/nama file.
- Batas aman copy-paste: 400-600 baris. Jika > 600, bagi menjadi beberapa BAGIAN di dalam 1 bubble.
- JANGAN potong di tengah fungsi.

=== ATURAN KERJA ===
1. Konfirmasi dulu sebelum eksekusi (TUNGGU aba-aba EKSEKUSI).
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
