package dev.shahzad.prefixshield

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class AppRelease(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val notes: String
)

object AppUpdate {
    const val MANIFEST_URL =
        "https://raw.githubusercontent.com/sahzadahmad246/prefixsheild/main/update.json"
    const val GITHUB_LATEST =
        "https://api.github.com/repos/sahzadahmad246/prefixsheild/releases/latest"

    fun installedCode(context: Context): Long {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
    }

    fun installedName(context: Context): String {
        return context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
    }

    fun isNewer(release: AppRelease?, installedCode: Long): Boolean {
        if (release == null) return false
        return release.versionCode > installedCode && release.apkUrl.isNotBlank()
    }

    fun fetch(onDone: (Result<AppRelease>) -> Unit) {
        Thread {
            val result = runCatching { readGithub() }.recoverCatching { readManifest() }
            Handler(Looper.getMainLooper()).post { onDone(result) }
        }.start()
    }

    fun download(context: Context, url: String, onDone: (Result<File>) -> Unit) {
        Thread {
            val result = runCatching {
                val dir = File(context.cacheDir, "update").apply { mkdirs() }
                val file = File(dir, "app-update.apk")
                open(url).inputStream.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                file
            }
            Handler(Looper.getMainLooper()).post { onDone(result) }
        }.start()
    }

    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apk)
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            data = uri
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    private fun readGithub(): AppRelease {
        val json = JSONObject(open(GITHUB_LATEST).inputStream.bufferedReader().readText())
        val assets = json.optJSONArray("assets")
        var apk = ""
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                    apk = asset.optString("browser_download_url")
                    break
                }
            }
        }
        val body = json.optString("body")
        val code = Regex("""versionCode\s*[:=]\s*(\d+)""").find(body)?.groupValues?.get(1)?.toInt()
            ?: error("release notes need versionCode")
        val name = json.optString("tag_name").removePrefix("v").ifBlank { json.optString("name") }
        val notes = body.lineSequence()
            .filterNot { it.contains("versionCode") || it.contains("versionName") }
            .joinToString("\n")
            .trim()
        require(apk.isNotBlank()) { "release has no APK" }
        return AppRelease(code, name, apk, notes)
    }

    private fun readManifest(): AppRelease {
        val json = JSONObject(open(MANIFEST_URL).inputStream.bufferedReader().readText())
        return AppRelease(
            versionCode = json.optInt("versionCode"),
            versionName = json.optString("versionName"),
            apkUrl = json.optString("apkUrl"),
            notes = json.optString("notes")
        )
    }

    private fun open(url: String): HttpURLConnection {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 120000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "myPhone")
            setRequestProperty("Accept", "application/vnd.github+json, application/json, */*")
        }
        val code = conn.responseCode
        if (code in 300..399) {
            val next = conn.getHeaderField("Location") ?: error("redirect $code")
            conn.disconnect()
            return open(next)
        }
        if (code !in 200..299) error("http $code")
        return conn
    }
}
