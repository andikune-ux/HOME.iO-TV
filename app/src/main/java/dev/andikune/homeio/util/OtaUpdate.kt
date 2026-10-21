package dev.andikune.homeio.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import dev.andikune.homeio.BuildConfig
import dev.andikune.homeio.presentation.components.GlassColors
import dev.andikune.homeio.presentation.components.NeonFocusBorder
import dev.andikune.homeio.service.UpdateDownloadService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

// ============================================================
// 1. UPDATE MANAGER
// ============================================================
object UpdateManager {
    private const val TAG = "UpdateManager"
    private const val GITHUB_API = "https://api.github.com/repos/andikune-ux/HOME.iO-TV/releases/latest"

    data class UpdateInfo(
        val available: Boolean, val currentVersion: String, val latestVersion: String,
        val downloadUrl: String?, val releaseNotes: String?, val isForceUpdate: Boolean,
        val errorType: String? = null
    )

    suspend fun checkForUpdate(): UpdateInfo = withContext(Dispatchers.IO) {
        val current = try { BuildConfig.VERSION_NAME } catch (e: Exception) { "v.1.1.1" }
        try {
            val conn = (URL(GITHUB_API).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "HomeIO-App")
                connectTimeout = 10000; readTimeout = 10000
            }
            if (conn.responseCode == 403) return@withContext noUpdate(current, "RATE_LIMIT")
            if (conn.responseCode == 404) return@withContext noUpdate(current, "NOT_FOUND")
            if (conn.responseCode != 200) return@withContext noUpdate(current, "HTTP_${conn.responseCode}")

            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            conn.disconnect()

            val latest = json.optString("tag_name", "")
            val notes = json.optString("body", "")
            if (latest.isBlank()) return@withContext noUpdate(current, "NO_TAG")

            val isNewer = compareVersions(latest, current) > 0
            val isForce = isNewer && notes.lines().any { it.trim().uppercase().startsWith("FORCE_UPDATE=TRUE") }

            var dlUrl: String? = null
            json.optJSONArray("assets")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val name = arr.getJSONObject(i).optString("name", "")
                    if (name.endsWith(".apk", true)) {
                        dlUrl = arr.getJSONObject(i).getString("browser_download_url")
                        break
                    }
                }
            }
            UpdateInfo(isNewer, current, latest, dlUrl, notes, isForce)
        } catch (e: Exception) {
            Log.e(TAG, "checkForUpdate: ${e.message}")
            noUpdate(current, "NETWORK")
        }
    }

    private fun compareVersions(v1: String, v2: String): Int {
        fun parse(v: String): List<Int> {
            return v.removePrefix("v").removePrefix("V").substringBefore("-")
                .split(".").filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }
        }
        val p1 = parse(v1); val p2 = parse(v2)
        for (i in 0 until maxOf(p1.size, p2.size)) {
            val a = p1.getOrElse(i) { 0 }; val b = p2.getOrElse(i) { 0 }
            if (a != b) return a - b
        }
        return 0
    }

    private fun noUpdate(cur: String, errorType: String? = null) = UpdateInfo(
        false, cur, cur, null, null, false, errorType
    )
}

// ============================================================
// 2. UPDATE PREFS
// ============================================================
object UpdatePrefs {
    private const val PREFS = "homeio_update_prefs"
    private const val KEY_SKIP = "skipped_version"
    private const val KEY_LAST_CHECK = "last_check_time"
    private const val CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L

    fun getSkipped(ctx: Context): String? = ctx.getSharedPreferences(PREFS, 0).getString(KEY_SKIP, null)
    fun setSkipped(ctx: Context, v: String) { ctx.getSharedPreferences(PREFS, 0).edit().putString(KEY_SKIP, v).apply() }
    fun shouldCheck(ctx: Context): Boolean = System.currentTimeMillis() - ctx.getSharedPreferences(PREFS, 0).getLong(KEY_LAST_CHECK, 0) >= CHECK_INTERVAL_MS
    fun markChecked(ctx: Context) { ctx.getSharedPreferences(PREFS, 0).edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply() }
    fun shouldShow(ctx: Context, latest: String, force: Boolean): Boolean = force || getSkipped(ctx) != latest
}

// ============================================================
// 3. APK DOWNLOADER
// ============================================================
object ApkDownloader {
    private const val TAG = "ApkDownloader"
    private const val FOLDER = "HomeiO/Pembaharuan"

    data class State(val isDownloading: Boolean = false, val progress: Float = 0f, val savedFilePath: String? = null, val error: String? = null, val isFinished: Boolean = false)

    private fun getDir(ctx: Context): File {
        val externalDir = File(Environment.getExternalStorageDirectory(), FOLDER)
        if (!externalDir.exists()) externalDir.mkdirs()
        return if (externalDir.canWrite()) externalDir else File(ctx.getExternalFilesDir(null), FOLDER).also { if (!it.exists()) it.mkdirs() }
    }

    fun isUsingFallbackPath(ctx: Context): Boolean {
        val externalDir = File(Environment.getExternalStorageDirectory(), FOLDER)
        return !externalDir.exists() || !externalDir.canWrite()
    }

    fun download(ctx: Context, urlStr: String, fileName: String): Flow<State> = flow {
        emit(State(isDownloading = true))
        try {
            val dir = getDir(ctx)
            dir.listFiles()?.filter { it.name.endsWith(".part") }?.forEach { it.delete() }
            val target = File(dir, fileName)
            if (target.exists() && isValid(ctx, target)) { emit(State(isFinished = true, savedFilePath = target.absolutePath, progress = 1f)); return@flow }
            if (target.exists()) target.delete()

            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply { connectTimeout = 30000; readTimeout = 60000; instanceFollowRedirects = true; setRequestProperty("User-Agent", "HomeIO-App") }
            var respCode = conn.responseCode
            var activeConn = conn
            if (respCode == HttpURLConnection.HTTP_MOVED_TEMP || respCode == HttpURLConnection.HTTP_MOVED_PERM) {
                val redirectUrl = conn.getHeaderField("Location")
                if (redirectUrl != null) {
                    activeConn = (URL(redirectUrl).openConnection() as HttpURLConnection).apply { connectTimeout = 30000; readTimeout = 60000; setRequestProperty("User-Agent", "HomeIO-App") }
                    respCode = activeConn.responseCode
                }
            }
            if (respCode != HttpURLConnection.HTTP_OK) { emit(State(error = "Server Error $respCode")); return@flow }

            val input = activeConn.inputStream
            val total = activeConn.contentLengthLong
            val partFile = File(target.parentFile, "${target.name}.part")
            FileOutputStream(partFile).use { out ->
                val buf = ByteArray(8192); var read: Int; var totalRead = 0L
                while (input.read(buf).also { read = it } != -1) {
                    out.write(buf, 0, read); totalRead += read
                    emit(State(isDownloading = true, progress = if (total > 0) totalRead.toFloat() / total else 0f))
                }
            }
            input.close(); activeConn.disconnect()
            if (!partFile.renameTo(target)) { partFile.delete(); throw IllegalStateException("Gagal menyimpan APK") }
            emit(State(isFinished = true, savedFilePath = target.absolutePath, progress = 1f))
        } catch (e: Exception) { emit(State(error = e.message ?: "Download gagal")) }
    }.flowOn(Dispatchers.IO)

    fun getList(ctx: Context): List<File> = getDir(ctx).listFiles()?.filter { it.name.endsWith(".apk") }?.sortedByDescending { it.lastModified() } ?: emptyList()
    fun delete(path: String): Boolean = try { File(path).delete() } catch (e: Exception) { false }

    fun isValid(ctx: Context, file: File): Boolean {
        if (file.length() < 1024) return false
        return try { ctx.packageManager.getPackageArchiveInfo(file.absolutePath, 0)?.packageName == ctx.packageName } catch (e: Exception) { false }
    }

    fun install(ctx: Context, path: String): Boolean {
        val file = File(path)
        if (!file.exists() || !isValid(ctx, file)) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !ctx.packageManager.canRequestPackageInstalls()) {
            try { ctx.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply { data = Uri.parse("package:${ctx.packageName}"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {}
            return false
        }
        return try {
            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
            ctx.startActivity(Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "application/vnd.android.package-archive"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION) })
            true
        } catch (e: Exception) { false }
    }
}

// ============================================================
// 4. UPDATE DIALOG
// ============================================================
@Composable
fun UpdateDialog(info: UpdateManager.UpdateInfo, progress: Float, isDownloading: Boolean, isReady: Boolean, isError: Boolean = false, onUpdate: () -> Unit, onLater: () -> Unit, onSkip: () -> Unit) {
    val isForce = info.isForceUpdate
    val focusReq = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(500); if (isActive) runCatching { focusReq.requestFocus() } }

    Dialog(onDismissRequest = { if (!isForce && !isDownloading) onLater() }, properties = DialogProperties(dismissOnBackPress = !isForce && !isDownloading, usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.9f).clip(RoundedCornerShape(18.dp)).background(GlassColors.DeepBackground).border(2.dp, GlassColors.AccentGlow, RoundedCornerShape(18.dp)).padding(16.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.SystemUpdate, null, tint = GlassColors.AccentGlow, modifier = Modifier.size(40.dp))
                Text("UPDATE TERSEDIA", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GlassColors.AccentGlow)
                Text("Versi ${info.latestVersion} tersedia", fontSize = 12.sp, color = GlassColors.TextSecondary)
                Spacer(Modifier.height(10.dp))
                Column(Modifier.fillMaxWidth().background(GlassColors.GlassTint, RoundedCornerShape(10.dp)).padding(10.dp)) {
                    Text("Apa yang baru:", fontSize = 12.sp, color = GlassColors.AccentGlow)
                    Box(Modifier.height(80.dp).verticalScroll(rememberScrollState())) { Text(info.releaseNotes ?: "-", fontSize = 12.sp, color = GlassColors.TextPrimary) }
                }
                if (isDownloading || isReady || isError) {
                    Spacer(Modifier.height(10.dp))
                    val label = when { isError -> "❌ Download gagal"; isReady -> "✅ Download selesai"; else -> "⬇️ Mengunduh... ${(progress * 100).toInt()}%" }
                    val labelColor = when { isError -> GlassColors.AccentRed; isReady -> GlassColors.AccentGreen; else -> GlassColors.AccentGlow }
                    Text(label, color = labelColor, fontSize = 12.sp)
                    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.Gray)) {
                        Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(when { isError -> GlassColors.AccentRed; isReady -> GlassColors.AccentGreen; else -> GlassColors.AccentGlow }))
                    }
                }
                Spacer(Modifier.height(20.dp))
                if (isError) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { OtaButton("TUTUP", GlassColors.SurfaceDark, onLater) }
                        Box(Modifier.weight(1f)) { OtaButton("COBA LAGI", GlassColors.AccentRed, onUpdate, focusReq) }
                    }
                } else if (isReady) { OtaButton("INSTALL SEKARANG", GlassColors.AccentGreen, onUpdate, focusReq) }
                else if (isDownloading) { OtaButton("MENGUNDUH...", GlassColors.SurfaceDark, {}) }
                else if (isForce) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { OtaButton("NANTI", GlassColors.SurfaceDark, onLater) }
                        Box(Modifier.weight(1f)) { OtaButton("UPDATE", GlassColors.AccentGlow, onUpdate, focusReq) }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { OtaButton("SKIP", GlassColors.SurfaceDark, onSkip) }
                        Box(Modifier.weight(1f)) { OtaButton("NANTI", GlassColors.SurfaceDark, onLater) }
                        Box(Modifier.weight(1f)) { OtaButton("UPDATE", GlassColors.AccentGlow, onUpdate, focusReq) }
                    }
                }
            }
        }
    }
}

@Composable
private fun OtaButton(txt: String, bg: Color, onClick: () -> Unit, focusReq: FocusRequester? = null) {
    var focus by remember { mutableStateOf(false) }
    val requester = focusReq ?: remember { FocusRequester() }
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (focus) Color.White else bg)
        .focusRequester(requester).onFocusChanged { focus = it.isFocused }.focusable().clickable { onClick() }.padding(12.dp), contentAlignment = Alignment.Center) {
        Text(txt, color = if (focus) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

// ============================================================
// 5. APK FILE LIST SECTION (DENGAN TOMBOL CEK UPDATE MANUAL)
// ============================================================
@Composable
fun ApkFileListSection() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var refreshKey by remember { mutableStateOf(0) }
    val list = remember(refreshKey) { ApkDownloader.getList(ctx) }
    val usingFallback = remember { ApkDownloader.isUsingFallbackPath(ctx) }

    var isChecking by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (usingFallback) {
            Row(Modifier.fillMaxWidth().background(GlassColors.AccentAmber.copy(alpha = 0.15f), RoundedCornerShape(8.dp)).border(1.dp, GlassColors.AccentAmber, RoundedCornerShape(8.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = GlassColors.AccentAmber, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Izin storage belum diberikan. File tersimpan di lokasi internal.", color = GlassColors.AccentAmber, fontSize = 11.sp)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("📦 FILE PEMBARUAN", color = GlassColors.AccentGlow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // TOMBOL CEK UPDATE MANUAL
                Box(Modifier.clip(RoundedCornerShape(8.dp)).background(GlassColors.AccentGlow.copy(alpha = 0.15f))
                    .border(1.dp, GlassColors.AccentGlow, RoundedCornerShape(8.dp))
                    .clickable(enabled = !isChecking) {
                        isChecking = true
                        scope.launch {
                            val info = UpdateManager.checkForUpdate()
                            isChecking = false
                            if (info.available) {
                                Toast.makeText(ctx, "Update tersedia: ${info.latestVersion}\nBuka Home untuk melihat dialog.", Toast.LENGTH_LONG).show()
                            } else {
                                val msg = if (info.errorType != null) "Gagal cek: ${info.errorType}" else "Sudah versi terbaru (${info.currentVersion})"
                                Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
                            }
                        }
                    }.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isChecking) {
                            CircularProgressIndicator(color = GlassColors.AccentGlow, modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                            Text("CEK...", color = GlassColors.AccentGlow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Refresh, null, tint = GlassColors.AccentGlow, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("CEK UPDATE", color = GlassColors.AccentGlow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Tombol Refresh List
                Box(Modifier.clip(RoundedCornerShape(8.dp)).background(GlassColors.GlassTint).border(1.dp, GlassColors.GlassBorder, RoundedCornerShape(8.dp))
                    .clickable { refreshKey++ }.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Refresh, null, tint = GlassColors.TextPrimary, modifier = Modifier.size(14.dp))
                }
            }
        }

        if (list.isEmpty()) {
            Text("Belum ada file APK tersimpan.", color = GlassColors.TextSecondary, fontSize = 12.sp)
        } else {
            list.forEach { file ->
                Row(Modifier.fillMaxWidth().background(GlassColors.GlassTint, RoundedCornerShape(8.dp)).border(1.dp, GlassColors.GlassBorder, RoundedCornerShape(8.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(file.name, color = GlassColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("${file.length() / 1024} KB", color = GlassColors.TextSecondary, fontSize = 10.sp)
                    }
                    Text("PASANG", color = GlassColors.AccentGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.clickable {
                        if (!ApkDownloader.install(ctx, file.absolutePath)) Toast.makeText(ctx, "Gagal install", Toast.LENGTH_SHORT).show()
                    }.padding(8.dp))
                    Text("HAPUS", color = GlassColors.AccentRed, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.clickable { ApkDownloader.delete(file.absolutePath); refreshKey++ }.padding(8.dp))
                }
            }
        }
    }
}
