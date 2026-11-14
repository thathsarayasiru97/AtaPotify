package com.example.data.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.BuildConfig
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class GitHubAppRelease(
    val tagName: String,
    val title: String,
    val releaseNotes: String,
    val releasePageUrl: String,
    val apkDownloadUrl: String?,
    val apkFileName: String?,
    val apkSizeBytes: Long,
    val publishedAt: String,
)

sealed interface AppUpdateCheckResult {
    data class UpdateAvailable(val release: GitHubAppRelease) : AppUpdateCheckResult
    data class UpToDate(val currentVersion: String) : AppUpdateCheckResult
}

object AppUpdateService {
    const val GITHUB_OWNER = "thathsarayasiru97"
    const val GITHUB_REPO = "AtaPotify"
    private const val LATEST_RELEASE_API =
        "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    suspend fun checkForLatestRelease(
        currentVersionName: String = BuildConfig.VERSION_NAME,
    ): Result<AppUpdateCheckResult> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "AtaPotify-Android/$currentVersionName")
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.use { BufferedReader(InputStreamReader(it)).readText() }.orEmpty()
            conn.disconnect()

            if (code == 404) {
                return@runCatching AppUpdateCheckResult.UpToDate(currentVersionName)
            }
            if (code !in 200..299) {
                val msg = runCatching { JSONObject(body).optString("message") }
                    .getOrNull().orEmpty().ifBlank { "HTTP $code" }
                error("Unable to check GitHub releases: $msg")
            }

            val json = JSONObject(body)
            if (json.optBoolean("draft", false)) {
                return@runCatching AppUpdateCheckResult.UpToDate(currentVersionName)
            }

            val tagName = json.optString("tag_name").trim()
            if (tagName.isBlank()) {
                return@runCatching AppUpdateCheckResult.UpToDate(currentVersionName)
            }

            val assets = json.optJSONArray("assets")
            var apkUrl: String? = null
            var apkName: String? = null
            var apkSize = 0L
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val name = asset.optString("name")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url").takeIf { it.isNotBlank() }
                        apkName = name
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            val release = GitHubAppRelease(
                tagName = tagName,
                title = json.optString("name").ifBlank { tagName },
                releaseNotes = json.optString("body").trim(),
                releasePageUrl = json.optString("html_url")
                    .ifBlank { "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/latest" },
                apkDownloadUrl = apkUrl,
                apkFileName = apkName,
                apkSizeBytes = apkSize,
                publishedAt = json.optString("published_at"),
            )

            if (isRemoteVersionNewer(tagName, currentVersionName)) {
                AppUpdateCheckResult.UpdateAvailable(release)
            } else {
                AppUpdateCheckResult.UpToDate(currentVersionName)
            }
        }
    }

    fun launchUpdateDownload(context: Context, release: GitHubAppRelease): Boolean {
        val targetUrl = release.apkDownloadUrl ?: release.releasePageUrl
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
        }.isSuccess
    }

    fun isRemoteVersionNewer(remoteTag: String, currentVersionName: String): Boolean {
        val remote = parseVersionTuple(remoteTag) ?: return false
        val current = parseVersionTuple(currentVersionName) ?: return false
        val maxLen = maxOf(remote.size, current.size)
        for (i in 0 until maxLen) {
            val r = remote.getOrElse(i) { 0 }
            val c = current.getOrElse(i) { 0 }
            if (r != c) return r > c
        }
        // If numeric parts match, a stable release (no hyphen suffix) is newer than a pre-release
        val remoteHasPre = remoteTag.trim().removePrefix("v").removePrefix("V").contains("-")
        val currentHasPre = currentVersionName.trim().removePrefix("v").removePrefix("V").contains("-")
        return !remoteHasPre && currentHasPre
    }

    internal fun parseVersionTuple(rawVersion: String): List<Int>? {
        val cleaned = rawVersion.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore("+")
            .substringBefore("-")
        if (cleaned.isBlank()) return null
        val parts = cleaned.split(".")
        val nums = parts.mapNotNull { segment ->
            segment.takeWhile { it.isDigit() }.toIntOrNull()
        }
        return nums.takeIf { it.isNotEmpty() }
    }

    fun formatApkSize(bytes: Long): String {
        if (bytes <= 0L) return ""
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1.0) {
            String.format(java.util.Locale.US, "%.1f MB", mb)
        } else {
            "${(bytes / 1024L).coerceAtLeast(1L)} KB"
        }
    }
}
