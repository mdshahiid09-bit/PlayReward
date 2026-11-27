package com.playreward.app.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.playreward.app.BuildConfig
import com.playreward.app.data.model.GithubReleaseInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File

sealed class DownloadProgressState {
    data object Idle : DownloadProgressState()
    data class Downloading(val percent: Int, val downloadedMb: Float, val totalMb: Float) : DownloadProgressState()
    data class Completed(val file: File) : DownloadProgressState()
    data class Failed(val reason: String) : DownloadProgressState()
}

class GithubUpdateManager(private val context: Context) {
    private val client = OkHttpClient()

    suspend fun checkLatestRelease(owner: String, repo: String): GithubReleaseInfo? = withContext(Dispatchers.IO) {
        val safeOwner = owner.ifBlank { BuildConfig.GITHUB_OWNER }
        val safeRepo = repo.ifBlank { BuildConfig.GITHUB_REPO }
        val url = "https://api.github.com/repos/$safeOwner/$safeRepo/releases/latest"
        try {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "PlayReward-Android")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val tag = json.optString("tag_name", "").trim()
                val name = json.optString("name", tag)
                val notes = json.optString("body", "Bug fixes and performance improvements.")
                val assets = json.optJSONArray("assets") ?: return@withContext null
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
                if (apkUrl.isBlank() || !isNewerVersion(tag, BuildConfig.VERSION_NAME)) {
                    return@withContext null
                }
                GithubReleaseInfo(
                    tagName = tag,
                    releaseName = name,
                    releaseNotes = notes,
                    apkDownloadUrl = apkUrl,
                    apkFileName = apkName
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
        val r = remoteTag.removePrefix("v").removePrefix("V").split(".").mapNotNull { it.toIntOrNull() }
        val c = currentVersion.removePrefix("v").removePrefix("V").split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(r.size, c.size)
        for (i in 0 until maxLen) {
            val rv = r.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (rv > cv) return true
            if (rv < cv) return false
        }
        return false
    }

    fun downloadUpdate(release: GithubReleaseInfo): Flow<DownloadProgressState> = flow {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val outFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), release.apkFileName)
        if (outFile.exists()) outFile.delete()

        val request = DownloadManager.Request(Uri.parse(release.apkDownloadUrl))
            .setTitle("Updating PlayReward ${release.tagName}")
            .setDescription("Downloading latest APK...")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(outFile))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadId = dm.enqueue(request)
        var finished = false
        while (!finished) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor: Cursor? = dm.query(query)
            if (cursor != null && cursor.moveToFirst()) {
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                cursor.close()

                when (status) {
                    DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PENDING, DownloadManager.STATUS_PAUSED -> {
                        val pct = if (total > 0) ((downloaded * 100L) / total).toInt().coerceIn(0, 99) else 0
                        emit(DownloadProgressState.Downloading(pct, downloaded / 1048576f, (total.coerceAtLeast(1)) / 1048576f))
                    }
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        finished = true
                        emit(DownloadProgressState.Completed(outFile))
                    }
                    DownloadManager.STATUS_FAILED -> {
                        finished = true
                        emit(DownloadProgressState.Failed("Download failed. Please retry."))
                    }
                }
            } else {
                cursor?.close()
                finished = true
                emit(DownloadProgressState.Failed("Unable to track download."))
            }
            if (!finished) delay(350)
        }
    }.flowOn(Dispatchers.IO)

    fun installApk(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
