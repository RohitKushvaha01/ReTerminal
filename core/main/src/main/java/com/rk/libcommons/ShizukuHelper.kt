package com.rk.libcommons

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import java.io.File

object ShizukuHelper {
    private const val REQUEST_CODE = 1001
    private const val RISH = "rish"
    private const val RISH_DEX = "rish_shizuku.dex"

    fun isRunning(): Boolean = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    fun hasPermission(): Boolean = runCatching {
        isRunning() && !Shizuku.isPreV11() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    fun requestPermissionIfNeeded(onResult: (Boolean) -> Unit = {}) {
        runCatching {
            if (!isRunning()) {
                onResult(false)
                return
            }
            if (hasPermission()) {
                onResult(true)
                return
            }
            lateinit var listener: Shizuku.OnRequestPermissionResultListener
            listener = Shizuku.OnRequestPermissionResultListener { code, result ->
                if (code == REQUEST_CODE) {
                    Shizuku.removeRequestPermissionResultListener(listener)
                    onResult(result == PackageManager.PERMISSION_GRANTED)
                }
            }
            Shizuku.addRequestPermissionResultListener(listener)
            Shizuku.requestPermission(REQUEST_CODE)
        }.onFailure {
            it.printStackTrace()
            onResult(false)
        }
    }

    /** Asks for permission as soon as the Shizuku service is (or becomes) available. */
    fun watch() {
        runCatching {
            Shizuku.addBinderReceivedListenerSticky { requestPermissionIfNeeded() }
        }.onFailure { it.printStackTrace() }
    }

    fun rishFile(context: Context): File = context.localBinDir().child(RISH)

    /** Copies rish + rish_shizuku.dex from assets. Android 14+ refuses to load a writable dex, so it is made read-only. */
    fun installRish(context: Context) {
        val bin = context.localBinDir()
        val dex = bin.child(RISH_DEX)
        if (dex.exists()) {
            dex.setWritable(true)
            dex.delete()
        }
        runCatching {
            context.assets.open(RISH_DEX).use { input -> dex.outputStream().use { input.copyTo(it) } }
            dex.setReadOnly()
            val script = bin.child(RISH)
            context.assets.open(RISH).use { input -> script.outputStream().use { input.copyTo(it) } }
            script.setExecutable(true)
        }.onFailure { it.printStackTrace() }
    }
}
