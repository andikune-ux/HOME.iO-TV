package dev.andikune.homeio.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import dev.andikune.homeio.BuildConfig
import dev.andikune.homeio.presentation.components.GlassColors
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

object UpdateManager {
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

            val latest = json.optString("tag_name", "Versi Baru").trim().ifEmpty { "Versi Baru" }
            val rawNotes = json.optString("body", "").trim()
            val notes = if (rawNotes.isEmpty()) "Tidak ada catatan rilis." else rawNotes

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

object ApkDownloader {
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

@Composable
fun UpdateDialog(
    info: UpdateManager.UpdateInfo, progress: Float, isDownloading: Boolean,
    isReady: Boolean, isError: Boolean = false, onUpdate: () -> Unit,
    onLater: () -> Unit, onSkip: () -> Unit
) {
    val isForce = info.isForceUpdate
    val focusReq = remember { FocusRequester() }

    BackHandler(enabled = isForce || isDownloading) { }

    LaunchedEffect(Unit) { delay(500); if (isActive) runCatching { focusReq.requestFocus() } }

    Dialog(
        onDismissRequest = { if (!isForce && !isDownloading) onLater() },
        properties = DialogProperties(dismissOnBackPress = !isForce && !isDownloading, usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.9f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(GlassColors.DeepBackground)
                    .border(2.dp, GlassColors.AccentGlow, RoundedCornerShape(18.dp))
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.SystemUpdate, null, tint = GlassColors.AccentGlow, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(10.dp))
                
                Text("UPDATE TERSEDIA", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = GlassColors.AccentGlow)
                Text("Versi: ${info.latestVersion}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GlassColors.TextPrimary)
                
                Spacer(Modifier.height(16.dp))
                
                Column(Modifier.fillMaxWidth().background(GlassColors.GlassTint, RoundedCornerShape(12.dp)).padding(14.dp)) {
                    Text("📋 Rincian Pembaruan:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GlassColors.AccentGlow)
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.height(120.dp).verticalScroll(rememberScrollState())) {
                        Text(info.releaseNotes ?: "-", fontSize = 13.sp, color = GlassColors.TextPrimary, lineHeight = 18.sp)
                    }
                }

                if (isDownloading || isReady || isError) {
                    Spacer(Modifier.height(16.dp))
                    val label = when { isError -> "❌ Download gagal"; isReady -> "✅ Download selesai"; else -> "⬇️ Mengunduh... ${(progress * 100).toInt()}%" }
                    val labelColor = when { isError -> GlassColors.AccentRed; isReady -> GlassColors.AccentGreen; else -> GlassColors.AccentGlow }
                    Text(label, color = labelColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color.Gray)) {
                        Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(when { isError -> GlassColors.AccentRed; isReady -> GlassColors.AccentGreen; else -> GlassColors.AccentGlow }))
                    }
                }

                Spacer(Modifier.height(20.dp))
                
                if (isError) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.weight(1f)) { OtaButton("TUTUP", GlassColors.SurfaceDark, onLater) }
                        Box(Modifier.weight(1f)) { OtaButton("COBA LAGI", GlassColors.AccentRed, onUpdate, focusReq) }
                    }
                } else if (isReady) {
                    OtaButton("INSTALL SEKARANG", GlassColors.AccentGreen, onUpdate, focusReq)
                } else if (isDownloading) {
                    OtaButton("MENGUNDUH...", GlassColors.SurfaceDark, {})
                } else if (isForce) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (focus) Color.White else bg)
            .focusRequester(requester).onFocusChanged { focus = it.isFocused }.focusable().clickable { onClick() }
            .padding(vertical = 14.dp), contentAlignment = Alignment.Center
    ) {
        Text(txt, color = if (focus) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun ApkFileListSection() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var refreshKey by remember { mutableStateOf(0) }
    val list = remember(refreshKey) { ApkDownloader.getList(ctx) }
    val usingFallback = remember { ApkDownloader.isUsingFallbackPath(ctx) }

    var isChecking by remember { mutableStateOf(false) }
    var checkResult by remember { mutableStateOf<String?>(null) }
    var isErrorResult by remember { mutableStateOf(false) }

    var showUpdateDialog by remember { mutableStateOf(false) }
    var localUpdateInfo by remember { mutableStateOf<UpdateManager.UpdateInfo?>(null) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadReady by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == UpdateDownloadService.ACTION_PROGRESS) {
                    val state = UpdateDownloadService.readState(ctx)
                    downloadProgress = state.progress
                    when (state.status) {
                        "DOWNLOADING" -> { isDownloading = true; downloadReady = false; isError = false }
                        "COMPLETE" -> { isDownloading = false; downloadReady = true; isError = false; downloadProgress = 1f; refreshKey++ }
                        "ERROR" -> { isDownloading = false; downloadReady = false; isError = true; downloadProgress = 0f }
                    }
                }
            }
        }
        val filter = IntentFilter(UpdateDownloadService.ACTION_PROGRESS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ctx.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag") ctx.registerReceiver(receiver, filter)
        }
        onDispose { ctx.unregisterReceiver(receiver) }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (usingFallback) {
            Row(Modifier.fillMaxWidth().background(GlassColors.AccentAmber.copy(alpha = 0.15f), RoundedCornerShape(8.dp)).border(1.dp, GlassColors.AccentAmber, RoundedCornerShape(8.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = GlassColors.AccentAmber, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Izin storage belum diberikan. File tersimpan di lokasi internal.", color = GlassColors.AccentAmber, fontSize = 11.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        var isFocused by remember { mutableStateOf(false) }
        Box(
            Modifier.fillMaxWidth(0.8f).clip(RoundedCornerShape(16.dp))
                .background(if (isFocused) GlassColors.AccentGlow.copy(alpha = 0.3f) else GlassColors.GlassTint)
                .border(if (isFocused) 3.dp else 2.dp, GlassColors.AccentGlow, RoundedCornerShape(16.dp))
                .onFocusChanged { isFocused = it.isFocused }.focusable()
                .clickable(enabled = !isChecking) {
                    isChecking = true; checkResult = null
                    scope.launch {
                        val info = UpdateManager.checkForUpdate()
                        isChecking = false
                        if (info.available) {
                            isErrorResult = false
                            checkResult = "✅ Update tersedia: ${info.latestVersion}"
                            localUpdateInfo = info
                            showUpdateDialog = true
                        } else {
                            isErrorResult = true
                            val msg = if (info.errorType != null) "Gagal cek: ${info.errorType}" else "Aplikasi sudah versi terbaru (${info.currentVersion})"
                            checkResult = "❌ $msg"
                        }
                    }
                }.padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isChecking) {
                    CircularProgressIndicator(color = GlassColors.AccentGlow, modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                    Spacer(Modifier.height(10.dp))
                    Text("MENGHUBUNGI GITHUB...", color = GlassColors.AccentGlow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                } else {
                    Icon(Icons.Default.Refresh, null, tint = GlassColors.AccentGlow, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("CEK PEMBARUAN APLIKASI", color = GlassColors.AccentGlow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Tekan untuk memeriksa versi terbaru di GitHub", color = GlassColors.TextSecondary, fontSize = 11.sp)
                }
            }
        }

        checkResult?.let { msg ->
            Text(
                text = msg,
                color = if (isErrorResult) GlassColors.AccentRed else GlassColors.AccentGreen,
                fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("📦 RIWAYAT UNDUHAN", color = GlassColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(GlassColors.GlassTint).border(1.dp, GlassColors.GlassBorder, RoundedCornerShape(8.dp))
                .clickable { refreshKey++ }.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Refresh, null, tint = GlassColors.TextPrimary, modifier = Modifier.size(14.dp))
            }
        }

        if (list.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                Text("Belum ada file APK yang diunduh.", color = GlassColors.TextSecondary, fontSize = 12.sp)
            }
        } else {
            list.forEach { file ->
                Row(Modifier.fillMaxWidth().background(GlassColors.GlassTint, RoundedCornerShape(8.dp)).border(1.dp, GlassColors.GlassBorder, RoundedCornerShape(8.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Folder, null, tint = GlassColors.AccentGlow, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
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

    if (showUpdateDialog && localUpdateInfo != null) {
        UpdateDialog(
            info = localUpdateInfo!!,
            progress = downloadProgress,
            isDownloading = isDownloading,
            isReady = downloadReady,
            isError = isError,
            onUpdate = {
                if (downloadReady || isError) {
                    if (isError) {
                        isError = false; downloadProgress = 0f; isDownloading = true
                        localUpdateInfo!!.downloadUrl?.let { UpdateDownloadService.start(ctx, it, "HomeIO_${System.currentTimeMillis()}.apk") }
                    } else {
                        val path = UpdateDownloadService.readState(ctx).filePath
                        if (path != null) {
                            if (!ApkDownloader.install(ctx, path)) Toast.makeText(ctx, "Beri izin install APK", Toast.LENGTH_LONG).show()
                        } else { downloadReady = false; isError = true }
                    }
                } else {
                    localUpdateInfo!!.downloadUrl?.let {
                        isDownloading = true; downloadProgress = 0f; downloadReady = false; isError = false
                        UpdateDownloadService.start(ctx, it, "HomeIO_${System.currentTimeMillis()}.apk")
                    }
                }
            },
            onLater = { showUpdateDialog = false },
            onSkip = { 
                UpdatePrefs.setSkipped(ctx, localUpdateInfo!!.latestVersion)
                showUpdateDialog = false 
            }
        )
    }
}
