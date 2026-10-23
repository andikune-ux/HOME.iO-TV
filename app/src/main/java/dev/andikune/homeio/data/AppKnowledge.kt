package dev.andikune.homeio.data

object AppKnowledge {
    const val APP_NAME = "HOME.iO TV"
    const val APP_DESCRIPTION = "Launcher Android TV & STB dengan UI iOS 26 Liquid Glass"
    const val REPO_URL = "https://github.com/andikune-ux/HOME.iO-TV"
    const val GITHUB_REPO = "https://github.com/andikune-ux/HOME.iO-TV"
    const val GITHUB_TREE_API = "https://api.github.com/repos/andikune-ux/HOME.iO-TV/git/trees/main?recursive=1"
    const val GITHUB_RAW_BASE = "https://raw.githubusercontent.com/andikune-ux/HOME.iO-TV/main/"
    const val GITHUB_BLOB_BASE = "https://github.com/andikune-ux/HOME.iO-TV/blob/main/"

    // KONSTANTA SOSIAL MEDIA & PROMPT (WAJIB ADA)
    const val SOCIAL_WA = "https://chat.whatsapp.com/BAejUrfRW39FOt8biJleUM?s=cl&p=a&mlu=4&ilr=4"
    const val SOCIAL_TIKTOK = "https://www.tiktok.com/@nayyra.une"
    const val SOCIAL_IG = "https://www.instagram.com/nayyra.une"

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
2. Path lengkap file
3. Nama file
4. URL Edit (file lama) / URL New (file baru)
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

    val WORKFLOW_RULES = """
PRINSIP UTAMA:
- KERJAKAN PER FILE
- KONFIRMASI SEBELUM EKSEKUSI
- JANGAN ASUMSI - LIHAT KODE ASLI DULU
- JANGAN HAPUS FITUR LAMA TANPA IZIN

ATURAN FORMAT:
1. Visualisasi folder
2. Path lengkap file
3. Nama file
4. URL Edit / New
5. Kode TIMPA FULL
6. Pesan Commit
    """.trimIndent()

    val KNOWN_ISSUES = """
=== LOG LENGKAP TEMUAN MASALAH & PERBAIKAN ===

1. Temuan : Nama aplikasi di launcher tertulis "Home.io" bukan "HOME.iO TV"
   File   : AndroidManifest.xml, strings.xml, values-in/strings.xml
   Fix    : Ganti hardcode label menjadi @string/app_name dan perbaiki file terjemahan.

2. Temuan : TV layar menjadi buram/hitam total saat membuka pop-up atau settings
   File   : GlassTheme.kt
   Fix    : Matikan Modifier.blur secara paksa untuk semua perangkat TV (Solid Fallback).

3. Temuan : Tombol OK pada remote harus ditekan 2x agar merespon
   File   : HomeScreen.kt, AppContextMenuDialog.kt, TvToggle.kt, TvSlider.kt, SettingsScreen.kt, dll
   Fix    : Tambahkan interseptor onPreviewKeyEvent (Enter/DPAD_CENTER) di seluruh tombol.

4. Temuan : Menahan tombol Kiri/Kanan di Carousel membuat fokus berantakan & balik ke awal
   File   : HomeScreen.kt
   Fix    : Tambahkan throttle isProcessingKey dan cek isScrollInProgress agar animasi tidak tumpang tindih.

5. Temuan : Border fokus carousel melayang dan tidak membungkus ikon dengan pas
   File   : NeonFocusBorder.kt, HomeScreen.kt
   Fix    : Hapus double scale di NeonFocusBorder, satukan border langsung ke container ikon.

6. Temuan : Fokus D-Pad nyangkut/hilang saat menu kiri (Hamburger) ditutup
   File   : HomeScreen.kt
   Fix    : Tambahkan FocusRequester khusus dan delay 400ms untuk mengembalikan fokus ke tombol Hamburger.

7. Temuan : Aplikasi menjadi lag/berat saat scroll daftar aplikasi
   File   : NeonFocusBorder.kt, HomeScreen.kt
   Fix    : Cache objek Paint/Path di NeonFocusBorder dan gunakan LruCache untuk memori ikon.

8. Temuan : Aplikasi khusus TV (Leanback) tidak mau terbuka saat diklik
   File   : HomeScreen.kt
   Fix    : Gunakan pm.getLeanbackLaunchIntentForPackage sebelum memanggil intent biasa.

9. Temuan : Pop-up Donasi QRIS hanya muncul sekali seumur hidup aplikasi
   File   : MainActivity.kt
   Fix    : Pisahkan logika QRIS dari Onboarding/Launcher agar selalu muncul setiap app dibuka.

10. Temuan: Layar Screensaver (hitam) tiba-tiba muncul saat Pop-up sedang dibaca
    File   : MainActivity.kt
    Fix    : Pause perhitungan timer idle (screensaverTimeoutMs) jika ada pop-up (QRIS/Launcher/Update).

11. Temuan: Suara klik berbunyi dua kali (double beep) saat menekan tombol
    File   : MainActivity.kt, SoundFeedback.kt
    Fix    : Hapus pemanggilan suara untuk OK/Enter di dispatchKeyEvent, dan ganti STREAM_SYSTEM ke STREAM_MUSIC.

12. Temuan: Cuaca menampilkan "29°C Cerah" (hardcoded) saat TV tidak ada internet
    File   : MainActivity.kt
    Fix    : Tangkap Exception dan set weatherInfo = null agar UI menampilkan "--°C".

13. Temuan: Crash (Force Close) jika izin storage tiba-tiba dicabut saat aplikasi berjalan
    File   : MainActivity.kt, FileManagerDialog.kt
    Fix    : Tambahkan pengecekan izin (Environment.isExternalStorageManager) di onResume dan sebelum load file.

14. Temuan: Fokus D-Pad gagal pindah dari Sidebar (kiri) ke Konten (kanan) di menu Settings
    File   : SettingsScreen.kt
    Fix    : Tambahkan delay 250ms dan FocusRequester langsung di dalam LazyColumn kanan.

15. Temuan: Kiosk Mode terkunci permanen dan tidak bisa keluar aplikasi tanpa restart TV
    File   : SystemControlScreen.kt, HomeScreen.kt
    Fix    : Tambahkan tombol darurat "Matikan Kiosk" tanpa PIN di System Control Center (ikon merah).

16. Temuan: Notifikasi tidak bisa dihapus, ikon tidak muncul, dan menumpuk penuh
    File   : LauncherNotificationService.kt, HomeScreen.kt
    Fix    : Tambahkan fungsi removeNotification, removeAllNotifications, dan load ikon via PackageManager.

17. Temuan: Build Error "Unresolved reference 'withStyle'" pada saat compile
    File   : OtaUpdate.kt
    Fix    : Tambahkan import androidx.compose.ui.text.withStyle.

18. Temuan: Build Error "Unresolved reference 'onFocusChanged'" pada saat compile
    File   : SystemControlScreen.kt
    Fix    : Tambahkan import androidx.compose.ui.focus.onFocusChanged.

19. Temuan: Build Error "Unresolved reference 'wp_video'" karena file belum ada
    File   : WallpaperView.kt
    Fix    : Ubah pemanggilan R.raw.wp_video menjadi context.resources.getIdentifier (aman jika file kosong).

20. Temuan: Slider patah/aneh saat digambar di TV (bawaan Material3)
    File   : TvSlider.kt
    Fix    : Buat Custom Slider dengan Box agar garis tebal dan rapi.

21. Temuan: Ikon aplikasi gagal dimuat (blank/crash) di menu Info App
    File   : InfoAppSection.kt
    Fix    : Ganti pengambilan ikon menggunakan ctx.packageManager.getApplicationIcon.

22. Temuan: Pengaturan Bahasa terasa menipu karena teks tetap Indonesia saat dipilih English
    File   : LanguageSection.kt
    Fix    : Ditambahkan peringatan (Beta) dan memastikan D-Pad OK 1x.

23. Temuan: Pilihan Mode Kecerahan SCHEDULE hilang dari menu Tampilan TV
    File   : TvDisplaySection.kt
    Fix    : Tambahkan opsi SCHEDULE pada deretan tombol kecerahan.

24. Temuan: Peringatan "Izin Storage" tetap muncul meski izin sudah diberikan
    File   : BackupBiasaSection.kt
    Fix    : Gunakan LaunchedEffect/Lifecycle agar status izin di-refresh secara real-time.

25. Temuan: Memory Leak (aplikasi hang) pada Toast di proses Boost RAM
    File   : PerformanceSection.kt
    Fix    : Gunakan applicationContext untuk pemanggilan Toast di dalam Coroutine panjang.

26. Temuan: Overload resolution ambiguity pada BackupManager.kt
    File   : BackupManager.kt
    Fix    : Gunakan appendLine(CharSequence) dan pastikan semua konstanta di AppKnowledge.kt tersedia.
    """.trimIndent()

    val BUILD_ERROR_HISTORY = """
=== RIWAYAT BUILD ERROR ===
1. KSP Configuration Cache Error -> android.useAndroidX=true + configuration-cache=false
2. tv-material:1.0.0-alpha11 not found -> upgrade ke 1.1.0
3. AAR Metadata require compileSdk 35 -> naikkan compileSdk & targetSdk ke 35
4. Unresolved reference 'withStyle' -> tambah import androidx.compose.ui.text.withStyle
5. Unresolved reference 'onFocusChanged' -> tambah import androidx.compose.ui.focus.onFocusChanged
6. Unresolved reference 'wp_video' -> gunakan context.resources.getIdentifier
    """.trimIndent()

    val BACKUP_INFO = """
=== INFORMASI BACKUP ===
Backup Aman (TXT): /sdcard/HomeiO/Backup aman/
Backup Biasa (JSON): /sdcard/HomeiO/Backup/
Format Aman: Backup Aman-Home.io-dd-MM-yyyy-HHmmss.txt
Format Biasa: Backup_HomeIO_HHmmss_ddMMyyyy.json
    """.trimIndent()

    val APP_STRUCTURE = """
app/src/main/java/dev/andikune/homeio/
├── MainActivity.kt
├── presentation/home/HomeScreen.kt
├── presentation/settings/SettingsScreen.kt
├── presentation/settings/SystemControlScreen.kt
├── presentation/wallpaper/WallpaperView.kt
├── util/SoundFeedback.kt
└── data/AppKnowledge.kt
    """.trimIndent()

    val APP_FEATURES = """
1. UI/UX iOS 26 Liquid Glass (Solid Fallback di TV).
2. System Control Center (WiFi, BT, Volume, Kecerahan).
3. Home Screen Carousel (D-Pad OK 1x, Anti-Jitter).
4. Sliding Pane Kiri (Notifikasi Asli, RAM Booster).
5. 3 Wallpaper Default (Sinematik, Estetik, OLED, Kustom).
6. Pengaturan 2-Pane (UI Premium, D-Pad OK 1x).
7. Notifikasi Premium (Ikon Asli, Hapus Satu/Semua).
8. OTA Update GitHub (Tombol Darurat Batal).
9. Screensaver (Pause saat Pop-up).
10. Dialog Pop-up (QRIS, Launcher, Context Menu).
    """.trimIndent()
}
