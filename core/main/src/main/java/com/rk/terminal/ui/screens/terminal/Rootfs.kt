package com.rk.terminal.ui.screens.terminal

import android.content.Context
import android.os.Build
import androidx.compose.runtime.mutableStateOf
import com.rk.libcommons.child
import com.rk.libcommons.localDir
import com.rk.libcommons.toast
import com.rk.settings.Preference
import com.rk.settings.Settings
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

enum class ExecMode(val value: Int) {
    CHROOT(0),
    PROOT(1);

    companion object {
        fun fromInt(v: Int): ExecMode? = entries.firstOrNull { it.value == v }
    }
}

object Rootfs {
    private const val WOLFI_UPDATED_AT = "wolfi_updated_at"

    var isInstalled = mutableStateOf(false)
    var execMode = mutableStateOf(ExecMode.fromInt(Settings.exec_mode))
    var distro = mutableStateOf(Settings.distro)
    var loginShell = mutableStateOf(Settings.login_shell)

    @Volatile
    private var wolfiDownloading = false

    fun setExecMode(mode: ExecMode) {
        execMode.value = mode
        Settings.exec_mode = mode.value
    }

    fun setDistro(mode: Int) {
        distro.value = mode
        Settings.distro = mode
    }

    fun setLoginShell(mode: Int) {
        loginShell.value = mode
        Settings.login_shell = mode
    }

    fun checkInstallation(context: Context) {
        isInstalled.value = isRootfsInstalled(context)
    }

    fun isRootfsInstalled(context: Context): Boolean {
        val alpineDir = context.localDir().child("alpine")
        val isExtracted = alpineDir.exists() && (alpineDir.list()?.any { it != "root" && it != "tmp" } == true)
        val isArchivePresent = context.filesDir.child("alpine.tar.gz").exists()
        return isExtracted || isArchivePresent
    }

    fun downloadWolfi(context: Context) = syncWolfi(context, checkFirst = false)

    fun checkWolfiUpdate(context: Context) = syncWolfi(context, checkFirst = true)

    private fun syncWolfi(context: Context, checkFirst: Boolean) {
        if (wolfiDownloading) return
        if (!checkFirst && isWolfiDownloaded(context)) return
        val abi = Build.SUPPORTED_ABIS.firstOrNull { it == "arm64-v8a" || it == "x86_64" }
        if (abi == null) {
            toast("Wolfi requires arm64 or x86_64, use Alpine on this device")
            return
        }
        val arch = if (abi == "arm64-v8a") "aarch64" else "x86_64"
        wolfiDownloading = true
        if (!checkFirst) toast("Downloading Wolfi rootfs (~24 MB)")
        Thread {
            val tmp = context.filesDir.child("wolfi.tar.gz.part")
            try {
                val (updatedAt, url) = fetchWolfiRelease(arch)
                if (checkFirst && isWolfiDownloaded(context) && Preference.getString(WOLFI_UPDATED_AT, "") == updatedAt) {
                    toast("Wolfi rootfs is up to date")
                    return@Thread
                }
                if (checkFirst) toast("Updating Wolfi rootfs...")
                var downloadUrl = url
                var redirects = 0
                while (true) {
                    val conn = URL(downloadUrl).openConnection() as HttpURLConnection
                    conn.connectTimeout = 15000
                    conn.readTimeout = 60000
                    conn.instanceFollowRedirects = false
                    when (val code = conn.responseCode) {
                        in 200..299 -> {
                            conn.inputStream.use { input ->
                                FileOutputStream(tmp).use { output -> input.copyTo(output) }
                            }
                            conn.disconnect()
                            break
                        }
                        301, 302, 303, 307, 308 -> {
                            downloadUrl = conn.getHeaderField("Location") ?: throw IOException("Missing redirect location")
                            conn.disconnect()
                            if (++redirects > 5) throw IOException("Too many redirects")
                        }
                        else -> throw IOException("HTTP $code")
                    }
                }
                val target = context.filesDir.child("wolfi.tar.gz")
                if (!tmp.renameTo(target)) {
                    tmp.copyTo(target, overwrite = true)
                    tmp.delete()
                }
                val dir = context.localDir().child("wolfi")
                val extracted = dir.exists() && dir.list()?.isNotEmpty() == true
                if (extracted) {
                    extractRootfs(target, dir)
                }
                Preference.setString(WOLFI_UPDATED_AT, updatedAt)
                toast(
                    when {
                        !extracted -> "Wolfi rootfs downloaded, extracts on first session"
                        checkFirst -> "Wolfi rootfs updated"
                        else -> "Wolfi rootfs downloaded"
                    }
                )
            } catch (e: Exception) {
                tmp.delete()
                toast("Wolfi ${if (checkFirst) "update" else "download"} failed: ${e.message}")
            } finally {
                wolfiDownloading = false
            }
        }.start()
    }

    private fun fetchWolfiRelease(arch: String): Pair<String, String> {
        val conn = URL("https://api.github.com/repos/leloush-x/wolfi-os-rootfs/releases/tags/wolfi-latest").openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        val json = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()
        val assets = JSONObject(json).getJSONArray("assets")
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (asset.getString("name").contains(arch)) {
                return asset.getString("updated_at") to asset.getString("browser_download_url")
            }
        }
        throw IOException("No $arch asset in latest release")
    }

    private fun extractRootfs(archive: File, dir: File) {
        val process = ProcessBuilder("/system/bin/sh", "-c", "tar -xf '${archive.absolutePath}' -C '${dir.absolutePath}'")
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        if (process.waitFor() != 0) {
            throw IOException("Extract failed${if (output.isBlank()) "" else ": $output.trim()"}")
        }
    }

    fun isWolfiDownloaded(context: Context): Boolean {
        val file = context.filesDir.child("wolfi.tar.gz")
        return file.exists() && file.length() > 0
    }
}
