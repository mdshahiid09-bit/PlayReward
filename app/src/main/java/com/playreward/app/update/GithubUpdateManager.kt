package com.playreward.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.playreward.app.BuildConfig
import com.playreward.app.data.model.GithubReleaseInfo
import com.playreward.app.service.PlayRewardForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

sealed class DownloadProgressState {
    data object Idle : DownloadProgressState()
    data class Downloading(
        val percent: Int,
        val downloadedMb: Float,
        val totalMb: Float,
        val speedKbps: Int = 0
    ) : DownloadProgressState()
    data class Completed(val file: File) : DownloadProgressState()
    data class Failed(val reason: String) : DownloadProgressState()
}

data class UpdateCheckResult(
    val release: GithubReleaseInfo? = null,
    val errorMessage: String? = null
)

class GithubUpdateManager(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun checkLatestReleaseDetailed(owner: String, repo: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        val safeOwner = owner.ifBlank { BuildConfig.GITHUB_OWNER }
        val safeRepo = repo.ifBlank { BuildConfig.GITHUB_REPO }

        val urls = listOf(
            "https://api.github.com/repos/$safeOwner/$safeRepo/releases/latest",
            "https://api.github.com/repos/$safeOwner/$safeRepo/releases?per_page=5"
        )

        var lastError: String? = null
        for (attempt in 1..2) {
            for (url in urls) {
                try {
                    val request = Request.Builder()
                        .url(url)
                        .header("Accept", "application/vnd.github+json")
                        .header("User-Agent", "PlayReward-Android/${BuildConfig.VERSION_NAME}")
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.code == 404) {
                            lastError = "Release not found (make sure $safeOwner/$safeRepo is Public)."
                            return@use
                        }
                        if (!response.isSuccessful) {
                            lastError = "GitHub API HTTP ${response.code}"
                            return@use
                        }
                        val body = response.body?.string() ?: return@use
                        val releaseJson: JSONObject? = if (body.trim().startsWith("[")) {
                            val arr = JSONArray(body)
                            (0 until arr.length()).map { arr.getJSONObject(it) }
                                .firstOrNull { !it.optBoolean("draft", false) }
                        } else {
                            JSONObject(body)
                        }

                        if (releaseJson != null) {
                            val parsed = parseReleaseJson(releaseJson)
                            if (parsed != null) {
                                return@withContext if (isNewerVersion(parsed.tagName, BuildConfig.VERSION_NAME)) {
                                    UpdateCheckResult(release = parsed)
                                } else {
                                    UpdateCheckResult(release = null, errorMessage = null)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    lastError = e.localizedMessage ?: "Network error while checking updates."
                }
            }
            delay(1000)
        }
        UpdateCheckResult(release = null, errorMessage = lastError)
    }

    private fun parseReleaseJson(json: JSONObject): GithubReleaseInfo? {
        val tag = json.optString("tag_name", "").trim()
        if (tag.isBlank()) return null
        val name = json.optString("name", tag)
        val notes = json.optString("body", "New version with improvements and fixes.")
        val assets = json.optJSONArray("assets") ?: return null
        var apkUrl = ""
        var apkName = "PlayReward-$tag.apk"
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val candidateName = asset.optString("name", "")
            if (candidateName.endsWith(".apk", ignoreCase = true)) {
                apkUrl = asset.optString("browser_download_url", "")
                apkName = candidateName
                if (candidateName.contains("release", ignoreCase = true)) break
            }
        }
        if (apkUrl.isBlank()) return null
        return GithubReleaseInfo(tag, name, notes, apkUrl, apkName)
    }

    fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
        val cleanRemote = Regex("[0-9]+(\\.[0-9]+)*").find(remoteTag)?.value ?: return false
        val cleanCurrent = Regex("[0-9]+(\\.[0-9]+)*").find(currentVersion)?.value ?: return false
        val r = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
        val c = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(r.size, c.size)
        for (i in 0 until maxLen) {
            val rv = r.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (rv > cv) return true
            if (rv < cv) return false
        }
        return false
    }

    // Real byte-by-byte streaming download with smooth progress updates & foreground notification
    fun downloadUpdate(release: GithubReleaseInfo): Flow<DownloadProgressState> = flow {
        emit(DownloadProgressState.Downloading(0, 0f, 0f, 0))
        PlayRewardForegroundService.start(context, "Updating PlayReward ${release.tagName}", "Connecting...", 0)

        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
        val outFile = File(dir, release.apkFileName)
        if (outFile.exists()) outFile.delete()

        try {
            val request = Request.Builder()
                .url(release.apkDownloadUrl)
                .header("User-Agent", "PlayReward-Android/${BuildConfig.VERSION_NAME}")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    emit(DownloadProgressState.Failed("Download failed (HTTP ${response.code}). Tap Retry."))
                    return@flow
                }
                val body = response.body ?: run {
                    emit(DownloadProgressState.Failed("Empty APK stream."))
                    return@flow
                }
                val totalBytes = body.contentLength().coerceAtLeast(1L)
                val totalMb = totalBytes / 1048576f
                var downloadedBytes = 0L
                val buffer = ByteArray(8192)
                var lastEmitTime = System.currentTimeMillis()
                var lastBytesAtEmit = 0L
                var lastNotifPct = -1

                body.byteStream().use { input ->
                    FileOutputStream(outFile).use { output ->
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloadedBytes += read

                            val now = System.currentTimeMillis()
                            if (now - lastEmitTime >= 120L || downloadedBytes >= totalBytes) {
                                val dtSec = ((now - lastEmitTime).coerceAtLeast(1L)) / 1000f
                                val kbps = (((downloadedBytes - lastBytesAtEmit) / 1024f) / dtSec).toInt().coerceAtLeast(0)
                                val pct = ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 99)
                                val dlMb = downloadedBytes / 1048576f
                                emit(DownloadProgressState.Downloading(pct, dlMb, totalMb, kbps))
                                if (pct != lastNotifPct && pct % 2 == 0) {
                                    lastNotifPct = pct
                                    PlayRewardForegroundService.start(
                                        context,
                                        "Downloading PlayReward ${release.tagName}",
                                        "$pct% • ${String.format("%.1f", dlMb)} / ${String.format("%.1f", totalMb)} MB",
                                        pct
                                    )
                                }
                                lastEmitTime = now
                                lastBytesAtEmit = downloadedBytes
                            }
                        }
                        output.flush()
                    }
                }
                PlayRewardForegroundService.start(context, "PlayReward ${release.tagName} Ready", "Tap to install update", 100)
                emit(DownloadProgressState.Downloading(100, totalMb, totalMb, 0))
                delay(250)
                emit(DownloadProgressState.Completed(outFile))
            }
        } catch (e: Exception) {
            emit(DownloadProgressState.Failed(e.localizedMessage ?: "Download interrupted. Tap Retry."))
        }
    }.flowOn(Dispatchers.IO)

    fun installApk(file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            val permIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(permIntent)
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
