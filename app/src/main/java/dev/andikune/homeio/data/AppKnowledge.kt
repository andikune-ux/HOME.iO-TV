package dev.andikune.homeio.data

object AppKnowledge {
    const val APP_NAME = "HOME.iO TV"
    const val APP_DESCRIPTION = "Launcher Android TV & STB dengan UI iOS 26 Liquid Glass"
    
    // Repo Private (Source Code)
    const val REPO_URL = "https://github.com/andikune-ux/HOME.iO-TV"
    const val GITHUB_REPO = "https://github.com/andikune-ux/HOME.iO-TV"
    
    // Repo Public (Release APK)
    const val PUBLIC_REPO_URL = "https://github.com/andikune-ux/dev.andikune-apps"
    const val GITHUB_PUBLIC_REPO = "https://github.com/andikune-ux/dev.andikune-apps"
    
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
- Repo Private  : https://github.com/andikune-ux/HOME.iO-TV
- Repo Public   : https://github.com/andikune-ux/dev.andikune-apps
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
6. JANGAN kirim kode sebelum saya minta (tunggu aba-aba).

=== ATURAN KERAS ===
- JANGAN PERNAH ubah debug.keystore.
- JANGAN PERNAH ubah signingConfigs.
- JANGAN PERNAH ubah build.yml (bagian signing).
- Build HANYA via GitHub Actions. JANGAN install APK dari AI Studio.
- OTA Update mengambil data dari repo public (dev.andikune-apps) dengan filter tag "homeio-".

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

27. Temuan: Pengaturan wallpaper kustom hilang setelah aplikasi direstart
    File   : PreferencesManager.kt, SettingsConfig.kt
    Fix    : Tambahkan persistence DataStore untuk wallpaperMode, customPhotoPath,
             customVideoPath, customActiveType, forceSoftwareDecoder, dan
             isKeepScreenOnEnabled.

28. Temuan: Toggle Auto-Start saat TV menyala tidak dihormati
    File   : receiver/BootReceiver.kt
    Fix    : BootReceiver membaca isAutoStartOnBoot sebelum menjalankan AutoStartService.

29. Temuan: AutoStartService dapat gagal pada Android modern
    File   : AndroidManifest.xml
    Fix    : Tambahkan permission FOREGROUND_SERVICE_SPECIAL_USE untuk service.

30. Temuan: Konversi video wallpaper menggunakan Thread.sleep tetap berjalan walaupun Transformer belum selesai
    File   : WallpaperSection.kt
    Fix    : Tunggu callback resmi Transformer dan validasi file hasil sebelum dipakai.

31. Temuan: Format tanggal bahasa Inggris salah karena Calendar.MONTH zero-based
    File   : DateTimeFormatter.kt
    Fix    : Gunakan nilai tanggal sebenarnya sehingga Januari tidak tampil sebagai "0 January".

32. Temuan: Backup pada hari yang sama dapat saling menimpa
    File   : BackupManager.kt
    Fix    : Tambahkan jam, menit, dan detik pada nama file backup.

33. Temuan: File ZIP biasa dapat dianggap sebagai APK valid
    File   : OtaUpdate.kt
    Fix    : Pastikan package archive ditemukan dan package name sesuai aplikasi.

34. Temuan: Pemeriksaan update terlalu sering mengakses GitHub API
    File   : MainActivity.kt, OtaUpdate.kt
    Fix    : Gunakan interval pengecekan enam jam melalui UpdatePrefs.

35. Temuan: Daftar notifikasi berisiko kehilangan update bersamaan
    File   : LauncherNotificationService.kt
    Fix    : Serialisasikan perubahan StateFlow dan bersihkan instance service.

36. Temuan: Fallback folder Downloads sebelumnya tidak benar-benar dipakai
    File   : FileManagerDialog.kt
    Fix    : Coba baca folder Downloads ketika izin storage penuh belum diberikan.

37. Temuan: Unit test gagal karena dependency JUnit tidak tersedia
    File   : app/build.gradle.kts
    Fix    : Tambahkan testImplementation JUnit 4.13.2 tanpa mengubah identitas, signing, atau konfigurasi release.

38. Temuan: Skala ikon aplikasi default masih 0.8x
    File   : SettingsConfig.kt, PreferencesManager.kt
    Fix    : Ubah default dan fallback DataStore menjadi 1.0x.

39. Temuan: Carousel aplikasi terlalu banyak dan fokus tidak berada di tengah
    File   : HomeScreen.kt
    Fix    : Tampilkan maksimal tujuh aplikasi pada carousel dengan fokus awal di item keempat, membentuk pola tiga kiri, fokus, tiga kanan.

40. Temuan: Neon focus border kurang terlihat di atas wallpaper putih
    File   : NeonFocusBorder.kt
    Fix    : Perjelas inti border putih, jejak biru, alpha, dan radius glow.

41. Temuan: Weather dapat kosong terlalu lama setelah gangguan internet singkat
    File   : MainActivity.kt
    Fix    : Gunakan retry 30 detik saat belum ada data dan pertahankan data terakhir saat request berikutnya gagal.

42. Temuan: Fokus pane kiri dapat bocor ke area Home di belakang pane
    File   : HomeScreen.kt
    Fix    : D-pad kanan saat pane kiri terbuka menutup pane dan tidak diteruskan ke konten belakang.

43. Temuan: Slider Safe Area dan pengaturan lain menjebak navigasi kiri/kanan
    File   : TvSlider.kt
    Fix    : D-pad kiri/kanan hanya mengubah nilai setelah tombol OK mengaktifkan slider. Tekan OK lagi untuk keluar dari mode slider.

44. Temuan: OLED screensaver tidak benar-benar menerapkan brightness 0%
    File   : ScreensaverActivity.kt
    Fix    : Ganti BRIGHTNESS_OVERRIDE_OFF menjadi screenBrightness = 0.0f secara konsisten. (Butuh izin WRITE_SETTINGS / ADB untuk maksimal).

45. Temuan: D-Pad Carousel infinite scroll (geser sendiri tanpa henti)
    File   : HomeScreen.kt
    Fix    : Hapus intersepsi LazyRow dan LaunchedEffect(selectedIndex) yang memicu animateScrollToItem. Gunakan snapshotFlow isScrollInProgress.

46. Temuan: Double border fokus (Carousel bawah dan menu atas nyala bersamaan)
    File   : HomeScreen.kt
    Fix    : Tambahkan state isCarouselFocused, border bawah hanya nyala jika isCarouselFocused == true.

47. Temuan: Slider gak bisa digeser (mentok KIRI/KANAN)
    File   : SettingsScreen.kt, TvSlider.kt
    Fix    : Hapus intersepsi tombol Kiri di LazyColumn. Tambahkan mode AKTIF (Tekan OK) di TvSlider.

48. Temuan: Tombol Hapus di Pembaruan App loncat ke Sidebar
    File   : OtaUpdate.kt
    Fix    : Buat composable FocusableActionText untuk tombol PASANG/HAPUS agar bisa fokus D-Pad.

49. Temuan: Kecerahan 0% tidak membuat layar hitam pekat
    File   : HomeScreen.kt
    Fix    : Tambahkan overlay Box(Color.Black) jika manualBrightness == 0.

50. Temuan: OTA Update mengambil semua rilis aplikasi lain
    File   : OtaUpdate.kt
    Fix    : Ubah endpoint ke /releases, filter tag dengan prefix "homeio-".

51. Temuan: OTA Workflow error 'Detached HEAD'
    File   : .github/workflows/update-changelog.yml
    Fix    : Tambahkan 'ref: main' pada actions/checkout dan 'git push origin main'.

52. Temuan: Kuota Artifact GitHub Actions penuh
    File   : build.yml
    Fix    : Hapus step Upload Artifact, andalkan rilis ke repo public.

53. Temuan: Tombol fisik Remote Settings (176) & Input (178) tidak berfungsi
    File   : MainActivity.kt
    Fix    : Tambahkan override onKeyDown di MainActivity untuk menangkap KEYCODE_SETTINGS dan KEYCODE_TV_INPUT.

54. Temuan: Launcher bawaan mengalahkan HOME.iO saat boot (tie-break)
    File   : AndroidManifest.xml
    Fix    : Ubah android:priority pada intent-filter HOME menjadi 100.

55. Temuan: Status izin di Sidebar tidak terupdate setelah kembali dari pengaturan sistem
    File   : HomeScreen.kt
    Fix    : Tambahkan LifecycleEventObserver ON_RESUME untuk me-refresh status izin notifikasi.

56. Temuan: Izin Overlay (SYSTEM_ALERT_WINDOW) tidak ada di daftar pengecekan aplikasi
    File   : PermissionHelper.kt, PermissionSection.kt, OnboardingPermissionDialog.kt
    Fix    : Tambahkan PermissionType.OVERLAY dan tambahkan UI baris izinnya.

57. Temuan: Fallback izin sistem (Info App) membuat UI kebingungan menandai izin
    File   : PermissionHelper.kt
    Fix    : Ubah return value openPermissionSettings menjadi false jika hanya membuka fallback, agar UI memunculkan status "Mode Kompatibilitas".

58. Temuan: Kiosk Mode bisa aktif tidak sengaja tanpa konfirmasi
    File   : HomeScreen.kt
    Fix    : Tambahkan Dialog Konfirmasi sebelum mengaktifkan Kiosk Mode.

59. Temuan: Tidak ada cara untuk memaksa hentikan (Force Stop) aplikasi yang nyangkut
    File   : AppContextMenuDialog.kt, AndroidManifest.xml
    Fix    : Tambahkan menu "Paksa Hentikan" di Context Menu menggunakan ActivityManager.killBackgroundProcesses.

60. Temuan: Navigasi D-Pad mentok di menu Pengaturan
    File   : SettingsScreen.kt
    Fix    : Ganti LazyColumn menjadi Column + verticalScroll pada pane kanan.
    """.trimIndent()

    val BUILD_ERROR_HISTORY = """
=== RIWAYAT BUILD ERROR ===
1. KSP Configuration Cache Error -> android.useAndroidX=true + configuration-cache=false
2. tv-material:1.0.0-alpha11 not found -> upgrade ke 1.1.0
3. AAR Metadata require compileSdk 35 -> naikkan compileSdk & targetSdk ke 35
4. Unresolved reference 'withStyle' -> tambah import androidx.compose.ui.text.withStyle
5. Unresolved reference 'onFocusChanged' -> tambah import androidx.compose.ui.focus.onFocusChanged
6. Unresolved reference 'wp_video' -> gunakan context.resources.getIdentifier
7. Build lokal pertama tertahan karena Android SDK belum tersedia.
8. Build berikutnya tertahan karena toolchain Java tidak memiliki jlink.
9. Build berhasil setelah Android SDK API 35 dan JDK lengkap tersedia.
10. Unit test gagal karena dependency JUnit belum dideklarasikan.
11. Setelah dependency JUnit ditambahkan, testDebugUnitTest dan assembleDebug berhasil.
12. GitHub Actions berhasil pada commit 8795374, run 40117478357.
13. Unresolved reference 'toPx' di NeonFocusBorder -> gunakan LocalDensity.
14. Unresolved 'removeNotification' -> buat companion object di LauncherNotificationService.
    """.trimIndent()

    val BACKUP_INFO = """
=== INFORMASI BACKUP ===
Backup Aman (TXT): /sdcard/HomeiO/Backup aman/
Backup Biasa (JSON): /sdcard/HomeiO/Backup/
Format Aman Lama: Backup Aman-Home.io-dd-MM-yyyy.txt
Format Aman Terbaru: Backup Aman-Home.io-dd-MM-yyyy-HHmmss.txt
Catatan: Format terbaru mencegah backup pada hari yang sama menimpa file sebelumnya.
Format Biasa: Backup_HomeIO_HHmmss_ddMMyyyy.json
    """.trimIndent()

    val APP_STRUCTURE = """
app/src/main/java/dev/andikune/homeio/
├── MainActivity.kt
├── presentation/home/HomeScreen.kt
├── presentation/home/AppContextMenuDialog.kt
├── presentation/settings/SettingsScreen.kt
├── presentation/settings/PermissionSection.kt
├── presentation/settings/SystemControlScreen.kt
├── presentation/settings/WallpaperSection.kt
├── presentation/wallpaper/FileManagerDialog.kt
├── presentation/wallpaper/WallpaperView.kt
├── presentation/components/TvSlider.kt
├── presentation/components/AutoFocusPane.kt
├── presentation/components/NeonFocusBorder.kt
├── data/local/PreferencesManager.kt
├── receiver/BootReceiver.kt
├── service/LauncherNotificationService.kt
├── util/OtaUpdate.kt
├── util/PermissionHelper.kt
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
8. OTA Update GitHub (Multi-App support via repo public).
9. Screensaver (Pause saat Pop-up).
10. Dialog Pop-up (QRIS, Launcher, Context Menu).
11. Persistence wallpaper, decoder, dan layar tetap menyala.
12. Auto-start boot yang mengikuti toggle pengguna.
13. Konversi video wallpaper berbasis callback Transformer.
14. Validasi APK OTA berdasarkan package aplikasi.
15. Interval pengecekan update otomatis enam jam.
16. Backup unik dengan timestamp lengkap.
17. File Manager fallback ke Downloads.
18. Notification listener dengan update state yang lebih aman.
19. Carousel aplikasi maksimal tujuh item dengan fokus presisi.
20. Neon focus border putih-biru dengan kontras tinggi.
21. Weather internet dengan retry dan data terakhir.
22. Pane kiri mengunci fokus dan menutup saat D-pad kanan ditekan.
23. Slider settings memakai mode aktivasi OK.
24. OLED screensaver dengan override brightness.
25. Tombol PASANG & HAPUS di Pembaruan App support D-Pad.
26. Konfirmasi keamanan sebelum mengaktifkan Kiosk Mode.
27. Tombol darurat Force Stop (Paksa Hentikan) di menu konteks aplikasi.
28. Halaman Izin Aplikasi terpusat di menu Pengaturan dengan analisa status (Banner).
29. Deteksi izin notifikasi secara real-time di Sidebar kiri.
30. Dukungan tombol fisik Remote Settings (176) & Input (178) di level Activity.
31. Fallback otomatis ke Info Aplikasi jika TV membatasi halaman izin sistem.
32. Launcher Tie-Break diatasi dengan priority 100.
    """.trimIndent()
}
