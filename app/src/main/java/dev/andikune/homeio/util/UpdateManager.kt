package dev.andikune.homeio.util

import android.util.Log
import dev.andikune.homeio.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * UpdateManager — Cek versi terbaru dari GitHub Releases API.
 *
 * Fitur:
 *   - Auto-detect versi terbaru (tag_name)
 *   - Parse marker FORCE_UPDATE=true dari release body
 *   - Ambil download URL dari assets APK
 */
object UpdateManager {

    private const val TAG = "UpdateManager"

    // GANTI sesuai repo Anda jika berubah
    private const val REPO_OWNER = "andikune-ux"
    private const val REPO_NAME = "HOME.iO-TV"
    private const val GITHUB_API =
        "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"

    data class UpdateInfo(
        val available: Boolean,
        val currentVersion: String,
        val latestVersion: String,
        val downloadUrl: String?,
        val releaseNotes: String?,
        val releaseDate: String?,
        val isForceUpdate: Boolean
    )

    suspend fun checkForUpdate(): UpdateInfo = withContext(Dispatchers.IO) {
        val currentVersion = try {
            BuildConfig.VERSION_NAME
        } catch (e: Exception) {
            "v.1.1.1"
        }

        try {
            val connection = (URL(GITHUB_API).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "HomeIO-App")
                connectTimeout = 10_000
                readTimeout = 10_000
            }

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                Log.w(TAG, "GitHub API response: $responseCode")
                connection.disconnect()
                return@withContext noUpdate(currentVersion)
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val json = JSONObject(response)
            val latestVersion = json.optString("tag_name", "")
            val releaseNotes = json.optString("body", "")
            val releaseDate = json.optString("published_at", "")
            val downloadUrl = findApkAssetUrl(json)

            if (latestVersion.isBlank()) {
                return@withContext noUpdate(currentVersion)
            }

            val isNewer = compareVersions(latestVersion, currentVersion) > 0
            val isForce = isNewer && parseForceUpdate(releaseNotes)

            Log.d(TAG, "current=$currentVersion, latest=$latestVersion, isNewer=$isNewer, isForce=$isForce")

            UpdateInfo(
                available = isNewer,
                currentVersion = currentVersion,
                latestVersion = latestVersion,
                downloadUrl = downloadUrl,
                releaseNotes = releaseNotes,
                releaseDate = releaseDate,
                isForceUpdate = isForce
            )
        } catch (e: Exception) {
            Log.e(TAG, "Cek update gagal: ${e.message}", e)
            noUpdate(currentVersion)
        }
    }

    private fun parseForceUpdate(releaseNotes: String?): Boolean {
        if (releaseNotes.isNullOrBlank()) return false
        return releaseNotes.lineSequence().any { line ->
            val trimmed = line.trim().uppercase()
            trimmed == "FORCE_UPDATE=TRUE" || trimmed.startsWith("FORCE_UPDATE=TRUE")
        }
    }

    private fun findApkAssetUrl(json: JSONObject): String? {
        return try {
            val assets = json.optJSONArray("assets") ?: return null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name", "")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    return asset.getString("browser_download_url")
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Bandingkan dua versi (format apapun yang dipisah titik).
     * Contoh: v.1.20.14 vs v.1.21.5
     * Return: >0 jika v1 lebih baru, <0 jika v2 lebih baru, 0 jika sama.
     */
    private fun compareVersions(v1: String, v2: String): Int {
        fun parse(v: String): List<Int> {
            return v.removePrefix("v").removePrefix("V")
                .split(".")
                .mapNotNull { it.trim().toIntOrNull() }
        }
        val p1 = parse(v1)
        val p2 = parse(v2)
        val maxLen = maxOf(p1.size, p2.size)
        for (i in 0 until maxLen) {
            val a = p1.getOrElse(i) { 0 }
            val b = p2.getOrElse(i) { 0 }
            if (a != b) return a - b
        }
        return 0
    }

    private fun noUpdate(currentVersion: String) = UpdateInfo(
        available = false,
        currentVersion = currentVersion,
        latestVersion = currentVersion,
        downloadUrl = null,
        releaseNotes = null,
        releaseDate = null,
        isForceUpdate = false
    )
}
