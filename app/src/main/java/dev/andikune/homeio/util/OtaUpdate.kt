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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import dev.andikune.homeio.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================================
// 1. LOGIC: UPDATE MANAGER (CEK GITHUB)
// ============================================================
object UpdateManager {
    private const val GITHUB_API = "https://api.github.com/repos/andikune-ux/HOME.iO-TV/releases/latest"

    data class UpdateInfo(
        val available: Boolean, val currentVersion: String, val latestVersion: String,
        val downloadUrl: String?, val releaseNotes: String?, val isForceUpdate: Boolean
    )

    suspend fun checkForUpdate(): UpdateInfo = withContext(Dispatchers.IO) {
        val current = try { BuildConfig.VERSION_NAME } catch (e: Exception) { "v.1.1.1" }
        try {
            val conn = (URL(GITHUB_API).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                connectTimeout = 10000; readTimeout = 10000
            }
            if (conn.responseCode != 200) return@withContext noUpdate(current)
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            conn.disconnect()

            val latest = json.optString("tag_name", "")
            val notes = json.optString("body", "")
            if (latest.isBlank()) return@withContext noUpdate(current)

            val isNewer = compareVersions(latest, current) > 0
            val isForce = isNewer && notes.lines().any { it.trim().uppercase().startsWith("FORCE_UPDATE=TRUE") }
            var dlUrl: String? = null
            json.optJSONArray("assets")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val name = arr.getJSONObject(i).optString("name", "")
                    if (name.endsWith(".apk", true)) dlUrl = arr.getJSONObject(i).getString("browser_download_url")
                }
            }
            UpdateInfo(isNewer, current, latest, dlUrl, notes, isForce)
        } catch (e: Exception) { noUpdate(current) }
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val p1 = v1.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        val p2 = v2.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(p1.size, p2.size)) {
            val a = p1.getOrElse(i) { 0 }; val b = p2.getOrElse(i) { 0 }
            if (a != b) return a - b
        }
        return 0
    }
    private fun noUpdate(cur: String) = UpdateInfo(false, cur, cur, null, null, false)
}

// ============================================================
// 2. LOGIC: UPDATE PREFS (SKIP & COOLDOWN)
// ============================================================
object UpdatePrefs {
    private const val PREFS = "homeio_update_prefs"
    private const val KEY_SKIP = "skipped_version"
    private const val KEY_LAST_CHECK = "last_check_time"

    fun getSkipped(ctx: Context): String? = ctx.getSharedPreferences(PREFS, 0).getString(KEY_SKIP, null)
    fun setSkipped(ctx: Context, v: String) { ctx.getSharedPreferences(PREFS, 0).edit().putString(KEY_SKIP, v).apply() }
    fun shouldCheck(ctx: Context): Boolean = System.currentTimeMillis() - ctx.getSharedPreferences(PREFS, 0).getLong(KEY_LAST_CHECK, 0) >= 6 * 60 * 60 * 1000L
    fun markChecked(ctx: Context) { ctx.getSharedPreferences(PREFS, 0).edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply() }
    fun shouldShow(ctx: Context, latest: String, force: Boolean): Boolean = force || getSkipped(ctx) != latest
}

// ============================================================
// 3. LOGIC: APK DOWNLOADER (DOWNLOAD & INSTALL)
// ============================================================
object ApkDownloader {
    private const val FOLDER = "HomeiO/Pembaharuan"

    data class State(val isDownloading: Boolean = false, val progress: Float = 0f, val savedFilePath: String? = null, val error: String? = null, val isFinished: Boolean = false)

    private fun getDir(ctx: Context): File {
        val dir = File(Environment.getExternalStorageDirectory(), FOLDER)
        if (!dir.exists()) dir.mkdirs()
        return if (dir.canWrite()) dir else File(ctx.getExternalFilesDir(null), FOLDER).also { if (!it.exists()) it.mkdirs() }
    }

    fun download(ctx: Context, urlStr: String, fileName: String): Flow<State> = flow {
        emit(State(isDownloading = true))
        try {
            val target = File(getDir(ctx), fileName)
            if (target.exists() && isValid(ctx, target)) {
                emit(State(isFinished = true, savedFilePath = target.absolutePath, progress = 1f)); return@flow
            }
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply { connectTimeout = 30000; readTimeout = 60000; instanceFollowRedirects = true }
            if (conn.responseCode != 200) { emit(State(error = "Server Error ${conn.responseCode}")); return@flow }

            val input = conn.inputStream
            val total = conn.contentLengthLong
            val partFile = File(target.parentFile, "${target.name}.part")
            FileOutputStream(partFile).use { out ->
                val buf = ByteArray(8192); var read: Int; var totalRead = 0L
                while (input.read(buf).also { read = it } != -1) {
                    out.write(buf, 0, read); totalRead += read
                    emit(State(isDownloading = true, progress = if (total > 0) totalRead.toFloat() / total else 0f))
                }
            }
            input.close(); conn.disconnect()
            if (!partFile.renameTo(target)) throw Exception("Gagal simpan file")
            emit(State(isFinished = true, savedFilePath = target.absolutePath, progress = 1f))
        } catch (e: Exception) { emit(State(error = e.message)) }
    }.flowOn(Dispatchers.IO)

    fun getList(ctx: Context): List<File> = getDir(ctx).listFiles()?.filter { it.name.endsWith(".apk") }?.sortedByDescending { it.lastModified() } ?: emptyList()
    fun delete(path: String): Boolean = File(path).delete()
    fun isValid(ctx: Context, file: File): Boolean {
        if (file.length() < 1024) return false
        return try { ctx.packageManager.getPackageArchiveInfo(file.absolutePath, 0)?.packageName == ctx.packageName } catch (e: Exception) { false }
    }

    fun install(ctx: Context, path: String): Boolean {
        val file = File(path)
        if (!file.exists() || !isValid(ctx, file)) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !ctx.packageManager.canRequestPackageInstalls()) {
            ctx.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${ctx.packageName}"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }); return false
        }
        try {
            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
            ctx.startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }); return true
        } catch (e: Exception) { return false }
    }
}

// ============================================================
// 4. UI: UPDATE DIALOG (KACA)
// ============================================================
private val C_BG = Color(0xE60B1720); private val C_GOLD = Color(0xFFFFD700)
private val C_TXT = Color.White; private val C_SUB = Color(0xB3FFFFFF)

@Composable
fun UpdateDialog(
    info: UpdateManager.UpdateInfo, progress: Float, isDownloading: Boolean, isReady: Boolean,
    onUpdate: () -> Unit, onLater: () -> Unit, onSkip: () -> Unit
) {
    val isForce = info.isForceUpdate
    val focusReq = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(500); runCatching { focusReq.requestFocus() } }

    Dialog(onDismissRequest = { if (!isForce && !isDownloading) onLater() }, properties = DialogProperties(dismissOnBackPress = !isForce && !isDownloading, usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.9f).clip(RoundedCornerShape(18.dp)).background(C_BG).border(2.dp, C_GOLD, RoundedCornerShape(18.dp)).padding(16.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.SystemUpdate, null, tint = C_GOLD, modifier = Modifier.size(40.dp))
                Text("UPDATE TERSEDIA", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = C_GOLD)
                Text("Versi ${info.latestVersion} tersedia", fontSize = 12.sp, color = C_SUB)
                Spacer(Modifier.height(10.dp))
                
                // Changelog
                Column(Modifier.fillMaxWidth().background(Color(0x33FFFFFF), RoundedCornerShape(10.dp)).padding(10.dp)) {
                    Text("Apa yang baru:", fontSize = 12.sp, color = C_GOLD)
                    Text(info.releaseNotes ?: "-", fontSize = 12.sp, color = C_TXT, modifier = Modifier.height(80.dp).verticalScroll(rememberScrollState()))
                }

                if (isDownloading || isReady) {
                    Spacer(Modifier.height(10.dp))
                    Text(if (isReady) "✅ Download selesai" else "⬇️ Mengunduh... ${(progress * 100).toInt()}%", color = if (isReady) Color.Green else C_GOLD, fontSize = 12.sp)
                    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.Gray)) {
                        Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(if (isReady) Color.Green else C_GOLD))
                    }
                }

                Spacer(Modifier.height(20.dp))
                if (isReady) {
                    OtaButton("INSTALL SEKARANG", Color.Green, onUpdate, focusReq)
                } else if (isDownloading) {
                    OtaButton("MENGUNDUH...", Color.Gray, {})
                } else if (isForce) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { OtaButton("NANTI", Color.DarkGray, onLater) }
                        Box(Modifier.weight(1f)) { OtaButton("UPDATE", C_GOLD, onUpdate, focusReq) }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { OtaButton("SKIP", Color.DarkGray, onSkip) }
                        Box(Modifier.weight(1f)) { OtaButton("NANTI", Color.DarkGray, onLater) }
                        Box(Modifier.weight(1f)) { OtaButton("UPDATE", C_GOLD, onUpdate, focusReq) }
                    }
                }
            }
        }
    }
}

@Composable
private fun OtaButton(txt: String, bg: Color, onClick: () -> Unit, focusReq: FocusRequester? = null) {
    var focus by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (focus) Color.White else bg)
        .focusRequester(focusReq ?: remember { FocusRequester() })
        .onFocusChanged { focus = it.isFocused }.focusable().clickable { onClick() }.padding(12.dp), contentAlignment = Alignment.Center) {
        Text(txt, color = if (focus) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

// ============================================================
// 5. UI: APK FILE LIST SECTION (DIPANGGIL DARI SETTINGS)
// ============================================================
@Composable
fun ApkFileListSection() {
    val ctx = LocalContext.current
    var refresh by remember { mutableStateOf(0) }
    val list = remember(refresh) { ApkDownloader.getList(ctx) }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("📦 FILE PEMBARUAN (MANUAL INSTALL)", color = C_GOLD, fontWeight = FontWeight.Bold)
        if (list.isEmpty()) {
            Text("Belum ada file APK tersimpan.", color = C_SUB, fontSize = 12.sp)
        } else {
            list.forEach { file ->
                Row(Modifier.fillMaxWidth().background(Color(0x22FFFFFF), RoundedCornerShape(8.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(file.name, color = C_TXT, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("${file.length() / 1024} KB", color = C_SUB, fontSize = 10.sp)
                    }
                    Text("PASANG", color = Color.Green, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                        if (!ApkDownloader.install(ctx, file.absolutePath)) Toast.makeText(ctx, "Gagal install", Toast.LENGTH_SHORT).show()
                    }.padding(8.dp))
                    Text("HAPUS", color = Color.Red, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                        ApkDownloader.delete(file.absolutePath); refresh++
                    }.padding(8.dp))
                }
            }
        }
    }
}
