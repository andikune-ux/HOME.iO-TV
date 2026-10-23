package dev.andikune.homeio.data

object AppKnowledge {
    const val APP_NAME = "HOME.iO TV"
    const val APP_DESCRIPTION = "Launcher Android TV & STB dengan UI iOS 26 Liquid Glass"
    const val REPO_URL = "https://github.com/andikune-ux/HOME.iO-TV"
    const val GITHUB_REPO = "https://github.com/andikune-ux/HOME.iO-TV"
    const val GITHUB_TREE_API = "https://api.github.com/repos/andikune-ux/HOME.iO-TV/git/trees/main?recursive=1"
    const val GITHUB_RAW_BASE = "https://raw.githubusercontent.com/andikune-ux/HOME.iO-TV/main/"
    const val GITHUB_BLOB_BASE = "https://github.com/andikune-ux/HOME.iO-TV/blob/main/"

    val APP_STRUCTURE = """
app/src/main/java/dev/andikune/homeio/
├── HomeIoApplication.kt
├── MainActivity.kt                    -> Activity Utama (Splash, Navigasi, Dialog, OTA, Kiosk, System Control)
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
│   │   └── HomeScreen.kt              -> UI Utama (System Control, Carousel, Notifikasi)
│   │   └── AppContextMenuDialog.kt    -> Pop-up Tahan Lama Aplikasi
│   │   └── SearchDialog.kt            -> Pencarian Aplikasi
│   ├── onboarding/
│   │   ├── OnboardingPermissionDialog.kt -> Dialog 6 Izin Awal
│   │   └── QrisDonationPopup.kt       -> Dialog Donasi QRIS
│   ├── settings/
│   │   ├── SettingsScreen.kt          -> Wadah Menu Pengaturan
│   │   ├── WallpaperSection.kt        -> 3 Wallpaper Default + Kustom
│   │   ├── ScreensaverSection.kt      -> Preview layar hitam
│   │   ├── PerformanceSection.kt      -> RAM & Cache
│   │   ├── LanguageSection.kt         -> Bahasa & Jam
│   │   ├── TvDisplaySection.kt        -> Kecerahan & Kiosk
│   │   ├── BackupBiasaSection.kt      -> Backup JSON
│   │   ├── InfoAppSection.kt          -> Info & Medsos
│   │   ├── AppCacheDialog.kt          -> Dialog hapus cache
│   │   └── SystemControlScreen.kt     -> WiFi, BT, Suara, Kecerahan
│   ├── wallpaper/
│   │   ├── WallpaperView.kt           -> Render Video/Foto/OLED
│   │   └── FileManagerDialog.kt
│   ├── screensaver/
│   │   └── ScreensaverActivity.kt     -> Fix Layar Hitam 0% Pekat
│   └── components/
│       ├── GlassTheme.kt              -> Modifier Liquid Glass
│       ├── NeonFocusBorder.kt         -> Animasi Glow Ringan
│       ├── TvSlider.kt & TvToggle.kt  -> Komponen UI TV
│       └── (Komponen UI lainnya)
├── service/
│   ├── UpdateDownloadService.kt       -> Foreground Service untuk OTA
│   └── LauncherNotificationService.kt -> Listener Notifikasi Sistem
├── kiosk/
│   ├── KioskManager.kt                -> Lock Task Mode
│   ├── WatchdogService.kt             -> Auto-restart jika crash
│   └── AutoStartService.kt            -> Buka saat boot
├── receiver/
│   ├── BootReceiver.kt                -> Deteksi boot
│   └── UsbReceiver.kt                 -> Deteksi flashdisk
└── util/
    ├── OtaUpdate.kt                   -> Logic OTA
    ├── CacheCleaner.kt                -> Logic RAM & Cache
    ├── BackupBiasaManager.kt          -> Export/Import JSON
    ├── BackupManager.kt               -> Export TXT (AppKnowledge)
    ├── DateTimeFormatter.kt           -> Format Jam & Tanggal
    ├── SoundFeedback.kt               -> Suara klik (STREAM_MUSIC)
    ├── LauncherHelper.kt              -> Pengecekan Default Launcher
    └── CrashHandler.kt                -> Penangkap Error
    """.trimIndent()

    val APP_FEATURES = """
1. UI/UX iOS 26 Liquid Glass
   - Efek Blur TV Dimatikan (Solid Fallback agar tidak crash/layar hitam).
   - Animasi Pulse Glow D-Pad (NeonFocusBorder) ringan.

2. SYSTEM CONTROL CENTER (V.1.22.203)
   - Deretan Ikon di Pojok Kiri Atas (Kiosk, WiFi, BT, Volume, Brightness, Shortcut).
   - WiFi & Bluetooth: Bisa scan dan connect langsung dari aplikasi.
   - Izin Kecerahan: Tombol pintas untuk meminta izin WRITE_SETTINGS.

3. HOME SCREEN (Modern Center-Locked Carousel)
   - Widget Jam, Tanggal, Cuaca (Offline safe), RAM.
   - Tombol Settings (⚙️).
   - Carousel: Border menyatu, tanpa double scale, anti-jitter, D-Pad OK 1x.
   - Fitur D-Pad Ujung: Kiri buka Pane, Kanan/Bawah buka Grid Overlay.
   - Tap untuk buka aplikasi, Tahan untuk Context Menu.

4. SLIDING PANE KIRI (Overlay)
   - Search, Notifikasi Asli (bisa hapus satu / hapus semua), RAM Booster, File Manager, Sideload, Settings.

5. 3 WALLPAPER DEFAULT
   - Sinematik: Memutar video dari res/raw/wp_video.mp4.
   - Estetik: Menampilkan foto dari res/raw/wp_photo.jpg.
   - Mode OLED: Layar hitam total.
   - Kustom: Galeri HP atau File Manager TV (Foto & Video tersimpan terpisah).

6. PENGATURAN 2-PANE (8 Kategori)
   - UI Premium Glassmorphism, D-Pad OK 1x.
   - Mode Kecerahan: MANUAL, AUTO, SCHEDULE.
   - Kiosk Mode Tanpa PIN (Bisa dimatikan lewat ikon merah di System Control atau menu).

7. NOTIFIKASI PREMIUM
   - Menampilkan ikon asli aplikasi pengirim.
   - Tombol X untuk hapus satu notifikasi.
   - Tombol "Hapus Semua".

8. OTA UPDATE GITHUB
   - Auto-check 6 jam, Force Update, Progress di notifikasi.
   - Ada tombol darurat 'Batal' jika download macet.

9. SCREENSAVER
   - Hitam pekat 0% (Anti burn-in).
   - Pause otomatis saat ada Pop-up (QRIS/Launcher/Update).

10. DIALOG POP-UP
    - QRIS: Muncul setiap aplikasi dibuka (D-Pad OK 1x).
    - Launcher Prompt: Meminta jadikan HOME.iO sebagai launcher utama.
    - Context Menu & App Cache Dialog: D-Pad OK 1x.
    """.trimIndent()

    val WORKFLOW_INSTRUCTION = """
=== BAGIAN 1 — ATURAN FORMAT RESPON (WAJIB) ===
Elemen wajib:
1. Visualisasi folder (emoji + indentasi)
2. Path lengkap file (code block)
3. Nama file (code block)
4. URL Edit / URL New (code block)
5. Kode TIMPA FULL (JANGAN SURUH EDIT MANUAL)
6. Pesan commit

=== BAGIAN 1B — ATURAN ANTI-TRUNCATION ===
- Batas aman copy-paste: 400-600 baris per bubble.
- Jika kode > 600 baris, WAJIB dibagi menjadi beberapa BAGIAN di dalam 1 bubble.
- JANGAN potong di tengah fungsi.

=== BAGIAN 2 — ATURAN KERJA ===
1. Konfirmasi dulu sebelum eksekusi (TUNGGU aba-aba "EKSEKUSI").
2. Jujur kalau belum tahu, jangan menebak.
3. JANGAN hapus fitur lama tanpa izin.
4. Lihat kode asli dulu sebelum timpa.
5. Kerjakan PER-FILE (bukan per-fitur).

=== BAGIAN 3 — ATURAN KERAS (JANGAN DILANGGAR) ===
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
- Device: HP Android (bukan PC)
- Aplikasi kerja: GitHub Mobile

APLIKASI:
- Nama           : HOME.iO TV
- Platform       : Android TV / STB
- Package        : dev.andikune.homeio
- Min SDK        : 24 (Android 7.0)
- Target SDK     : 35 (Android 15)
- Stack          : Kotlin, Jetpack Compose, Compose TV, Media3 ExoPlayer.

UPDATE WAJIB TIAP BUILD:
1. Bump versionName (GITHUB_RUN_NUMBER).
2. Update AppKnowledge.kt (struktur, fitur, bug history).

ATURAN D-PAD & UI TV:
- Semua tombol WAJIB pakai onPreviewKeyEvent (Enter/DPAD_CENTER) agar OK 1x.
- Jangan pakai Modifier.blur di TV (bikin layar hitam).
- Gunakan LruCache untuk ikon aplikasi (anti OOM).
- Selalu pasang FocusRequester saat pindah antar Pane.
    """.trimIndent()

    val BUG_HISTORY = """
=== RIWAYAT TEMUAN MASALAH & PERBAIKAN (LOG LENGKAP) ===

1. Masalah : Nama aplikasi di launcher tertulis "Home.io" bukan "HOME.iO TV"
   File    : AndroidManifest.xml, strings.xml, values-in/strings.xml
   Solusi  : Ganti hardcode label menjadi @string/app_name dan perbaiki file terjemahan.

2. Masalah : TV layar menjadi buram/hitam total saat membuka pop-up atau settings
   File    : GlassTheme.kt
   Solusi  : Matikan Modifier.blur secara paksa untuk semua perangkat TV (Solid Fallback).

3. Masalah : Tombol OK pada remote harus ditekan 2x agar merespon
   File    : HomeScreen.kt, AppContextMenuDialog.kt, TvToggle.kt, TvSlider.kt, SettingsScreen.kt, dll
   Solusi  : Tambahkan interseptor onPreviewKeyEvent (Enter/DPAD_CENTER) di seluruh tombol.

4. Masalah : Menahan tombol Kiri/Kanan di Carousel membuat fokus berantakan & balik ke awal
   File    : HomeScreen.kt
   Solusi  : Tambahkan throttle isProcessingKey dan cek isScrollInProgress agar animasi tidak tumpang tindih.

5. Masalah : Border fokus carousel melayang dan tidak membungkus ikon dengan pas
   File    : NeonFocusBorder.kt, HomeScreen.kt
   Solusi  : Hapus double scale di NeonFocusBorder, satukan border langsung ke container ikon.

6. Masalah : Fokus D-Pad nyangkut/hilang saat menu kiri (Hamburger) ditutup
   File    : HomeScreen.kt
   Solusi  : Tambahkan FocusRequester khusus dan delay 400ms untuk mengembalikan fokus ke tombol Hamburger.

7. Masalah : Aplikasi menjadi lag/berat saat scroll daftar aplikasi
   File    : NeonFocusBorder.kt, HomeScreen.kt
   Solusi  : Cache objek Paint/Path di NeonFocusBorder dan gunakan LruCache untuk memori ikon.

8. Masalah : Aplikasi khusus TV (Leanback) tidak mau terbuka saat diklik
   File    : HomeScreen.kt
   Solusi  : Gunakan pm.getLeanbackLaunchIntentForPackage sebelum memanggil intent biasa.

9. Masalah : Pop-up Donasi QRIS hanya muncul sekali seumur hidup aplikasi
   File    : MainActivity.kt
   Solusi  : Pisahkan logika QRIS dari Onboarding/Launcher agar selalu muncul setiap app dibuka.

10. Masalah: Layar Screensaver (hitam) tiba-tiba muncul saat Pop-up sedang dibaca
    File    : MainActivity.kt
    Solusi  : Pause perhitungan timer idle (screensaverTimeoutMs) jika ada pop-up (QRIS/Launcher/Update).

11. Masalah: Suara klik berbunyi dua kali (double beep) saat menekan tombol
    File    : MainActivity.kt, SoundFeedback.kt
    Solusi  : Hapus pemanggilan suara untuk OK/Enter di dispatchKeyEvent, dan ganti STREAM_SYSTEM ke STREAM_MUSIC.

12. Masalah: Cuaca menampilkan "29°C Cerah" (hardcoded) saat TV tidak ada internet
    File    : MainActivity.kt
    Solusi  : Tangkap Exception dan set weatherInfo = null agar UI menampilkan "--°C".

13. Masalah: Crash (Force Close) jika izin storage tiba-tiba dicabut saat aplikasi berjalan
    File    : MainActivity.kt, FileManagerDialog.kt
    Solusi  : Tambahkan pengecekan izin (Environment.isExternalStorageManager) di onResume dan sebelum load file.

14. Masalah: Fokus D-Pad gagal pindah dari Sidebar (kiri) ke Konten (kanan) di menu Settings
    File    : SettingsScreen.kt
    Solusi  : Tambahkan delay 250ms dan FocusRequester langsung di dalam LazyColumn kanan.

15. Masalah: Kiosk Mode terkunci permanen dan tidak bisa keluar aplikasi tanpa restart TV
    File    : SystemControlScreen.kt, HomeScreen.kt
    Solusi  : Tambahkan tombol darurat "Matikan Kiosk" tanpa PIN di System Control Center (ikon merah).

16. Masalah: Notifikasi tidak bisa dihapus, ikon tidak muncul, dan menumpuk penuh
    File    : LauncherNotificationService.kt, HomeScreen.kt
    Solusi  : Tambahkan fungsi removeNotification, removeAllNotifications, dan load ikon via PackageManager.

17. Masalah: Build Error "Unresolved reference 'withStyle'" pada saat compile
    File    : OtaUpdate.kt
    Solusi  : Tambahkan import androidx.compose.ui.text.withStyle.

18. Masalah: Build Error "Unresolved reference 'onFocusChanged'" pada saat compile
    File    : SystemControlScreen.kt
    Solusi  : Tambahkan import androidx.compose.ui.focus.onFocusChanged.

19. Masalah: Build Error "Unresolved reference 'wp_video'" karena file belum ada
    File    : WallpaperView.kt
    Solusi  : Ubah pemanggilan R.raw.wp_video menjadi context.resources.getIdentifier (aman jika file kosong).

20. Masalah: Slider patah/aneh saat digambar di TV (bawaan Material3)
    File    : TvSlider.kt
    Solusi  : Buat Custom Slider dengan Box agar garis tebal dan rapi.

21. Masalah: Ikon aplikasi gagal dimuat (blank/crash) di menu Info App
    File    : InfoAppSection.kt
    Solusi  : Ganti pengambilan ikon menggunakan ctx.packageManager.getApplicationIcon.

22. Masalah: Pengaturan Bahasa terasa menipu karena teks tetap Indonesia saat dipilih English
    File    : LanguageSection.kt
    Solusi  : Ditambahkan peringatan (Beta) dan memastikan D-Pad OK 1x.

23. Masalah: Pilihan Mode Kecerahan SCHEDULE hilang dari menu Tampilan TV
    File    : TvDisplaySection.kt
    Solusi  : Tambahkan opsi SCHEDULE pada deretan tombol kecerahan.

24. Masalah: Peringatan "Izin Storage" tetap muncul meski izin sudah diberikan
    File    : BackupBiasaSection.kt
    Solusi  : Gunakan LaunchedEffect/Lifecycle agar status izin di-refresh secara real-time.

25. Masalah: Memory Leak (aplikasi hang) pada Toast di proses Boost RAM
    File    : PerformanceSection.kt
    Solusi  : Gunakan applicationContext untuk pemanggilan Toast di dalam Coroutine panjang.
    """.trimIndent()

    val MEMORY_KNOWLEDGE = """
=== ATURAN KETAT AI ===
1. JANGAN UBAH debug.keystore, signingConfigs, atau build.yml (signing).
2. Selalu gunakan onPreviewKeyEvent untuk semua tombol di TV.
3. Jangan gunakan Modifier.blur di TV (bikin layar hitam).
4. Cek kode asli sebelum menimpa.
5. Minta konfirmasi "EKSEKUSI" sebelum bertindak.
    """.trimIndent()
}
