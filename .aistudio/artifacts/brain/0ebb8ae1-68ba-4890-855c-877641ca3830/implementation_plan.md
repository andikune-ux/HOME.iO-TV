# Perbaikan Unduhan APK di GitHub Actions (Upload Artifacts)

Solusi terarah untuk mengatasi masalah tanda unduh/download yang tidak muncul di GitHub Actions (`Artifacts: —`) setelah build Android selesai dengan sukses (`assembleDebug`).

---

## User Review & Critical Decisions

> [!IMPORTANT]
> Masalah hilangnya tanda download **bukan karena token secret atau izin repo**, melainkan karena workflow GitHub Actions belum memiliki instruksi penyimpanan file (`actions/upload-artifact@v4`).

- **Pilihan Terkonfirmasi**: Pengguna memilih opsi **Hanya GitHub Actions Artifacts** (file APK siap diunduh dalam format `.zip` langsung dari halaman ringkasan run Actions).
- **Lokasi File APK di Runner**: `app/build/outputs/apk/debug/app-debug.apk`.
- **Nama File Artifact yang Dihasilkan**: `HomeIO-TV-Debug-APK`.

---

## 1. Overview & Analisa Masalah

### Akar Penyebab
Pada tangkapan layar yang Anda lampirkan:
* **Workflow Run #136 & #140**: Status menunjukkan `Success` dan Gradle menjalankan task `assembleDebug` dengan sukses.
* Namun di panel **Artifacts**, tertera **`—`** (strip kosong).
* Pada virtual machine runner GitHub Actions (Ubuntu runner), semua file yang dihasilkan dalam proses kompilasi berada di storage sementara (ephemeral). Begitu job selesai, runner dihapus beserta seluruh filenya, kecuali ada step `actions/upload-artifact` yang secara eksplisit mengunggah file tersebut ke storage cloud GitHub.

### Solusi
Menambahkan step `actions/upload-artifact@v4` tepat setelah langkah `./gradlew assembleDebug` pada file `.github/workflows/build.yml`.

---

## 2. Alur Pengguna (User Experience Flow)

```
┌────────────────────────┐      ┌──────────────────────────┐      ┌─────────────────────────────┐
│ 1. Push / Dispatch Run │ ───> │ 2. Gradle assembleDebug  │ ───> │ 3. Upload Artifact Step     │
│    di GitHub Actions   │      │    (Menghasilkan APK)    │      │    (actions/upload-artifact)│
└────────────────────────┘      └──────────────────────────┘      └──────────────┬──────────────┘
                                                                                 │
                                                                                 ▼
                                                                  ┌─────────────────────────────┐
                                                                  │ 4. Halaman Summary Run      │
                                                                  │    Tampil Bagian "Artifacts"│
                                                                  │    [⬇ HomeIO-TV-Debug-APK]  │
                                                                  └─────────────────────────────┘
```

1. **Jalankan Workflow**: Pengguna melakukan push commit atau memicu `workflow_dispatch` ("Re-run all jobs").
2. **Kompilasi**: Gradle menyusun APK di path `app/build/outputs/apk/debug/app-debug.apk`.
3. **Pengunggahan Otomatis**: GitHub Actions mengemas file APK ke dalam artifact cloud storage.
4. **Unduh Mudah**: Pada halaman Actions Run (seperti pada screenshot Anda), di bawah tabel summary akan muncul kartu **Artifacts** dengan tombol unduh **HomeIO-TV-Debug-APK**.

---

## 3. Keputusan Teknis & Alternatif

- **Pendekatan yang Dipilih: `actions/upload-artifact@v4`**
  - *Alasan*: Kompatibel penuh dengan node runtime terbaru GitHub Actions (menghindari warning deprecasi Node.js 20), tidak memerlukan token personal (PAT) khusus, dan langsung menampilkan link unduhan di layar Actions yang sedang Anda pantau.
  - *Alternatif yang Ditinggalkan (GitHub Releases)*: Dieliminasi sesuai pilihan Anda agar tidak mengotori tab Releases dengan build debug harian.
- **Handling Path Fleksibel**:
  - Menggunakan path wildcard: `app/build/outputs/apk/debug/*.apk` agar tetap berhasil meskipun ada penamaan custom pada APK.

---

## 4. Spesifikasi File Workflow (`.github/workflows/build.yml`)

Berikut rancangan skrip workflow lengkap yang siap digunakan di repositori GitHub Anda:

```yaml
name: Build & Auto Release

on:
  push:
    branches: [ "main" ]
  pull_request:
    branches: [ "main" ]
  workflow_dispatch:

permissions:
  contents: read

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout Repository
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew || true

      - name: Build with Gradle
        run: ./gradlew assembleDebug

      - name: Upload APK Artifact
        uses: actions/upload-artifact@v4
        with:
          name: HomeIO-TV-Debug-APK
          path: app/build/outputs/apk/debug/*.apk
          retention-days: 14
```

> [!NOTE]
> Setelah Anda menekan tombol **Proceed**, saya akan membuatkan file `.github/workflows/build.yml` ini di repositori sehingga Anda dapat langsung melakukan push atau menyalinnya ke GitHub.
