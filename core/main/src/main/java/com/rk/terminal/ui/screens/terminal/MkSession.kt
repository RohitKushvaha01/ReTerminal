package com.rk.terminal.ui.screens.terminal

import android.content.Context
import com.rk.libcommons.alpineHomeDir
import com.rk.libcommons.child
import com.rk.libcommons.createFileIfNot
import com.rk.libcommons.localBinDir
import com.rk.libcommons.localDir
import com.rk.libcommons.localLibDir
import com.rk.terminal.App.Companion.getTempDir
import com.rk.terminal.BuildConfig
import com.rk.terminal.ui.screens.settings.WorkingMode
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import java.io.File

object MkSession {
    fun createSession(
        context: Context,
        sessionClient: TerminalSessionClient,
        sessionId: String,
        workingMode: Int,
        pendingCommand: PendingCommand? = null
    ): TerminalSession {
        with(context) {
            val envVariables = mapOf(
                "ANDROID_ART_ROOT" to System.getenv("ANDROID_ART_ROOT"),
                "ANDROID_DATA" to System.getenv("ANDROID_DATA"),
                "ANDROID_I18N_ROOT" to System.getenv("ANDROID_I18N_ROOT"),
                "ANDROID_ROOT" to System.getenv("ANDROID_ROOT"),
                "ANDROID_RUNTIME_ROOT" to System.getenv("ANDROID_RUNTIME_ROOT"),
                "ANDROID_TZDATA_ROOT" to System.getenv("ANDROID_TZDATA_ROOT"),
                "BOOTCLASSPATH" to System.getenv("BOOTCLASSPATH"),
                "DEX2OATBOOTCLASSPATH" to System.getenv("DEX2OATBOOTCLASSPATH"),
                "EXTERNAL_STORAGE" to System.getenv("EXTERNAL_STORAGE")
            )

            val workingDir = pendingCommand?.workingDir ?: alpineHomeDir().path

            val initFile: File = localBinDir().child("init-host")
            if (initFile.exists().not()) {
                initFile.createFileIfNot()
                assets.open("init-host.sh").bufferedReader().use { it.readText() }.let {
                    initFile.writeText(it)
                }
            }

            localBinDir().child("init").apply {
                if (exists().not()) {
                    createFileIfNot()
                    assets.open("init.sh").bufferedReader().use { it.readText() }.let {
                        writeText(it)
                    }
                }
            }

            val env = mutableListOf(
                "PATH=${System.getenv("PATH")}:/sbin:${localBinDir().absolutePath}",
                "HOME=/sdcard",
                "PUBLIC_HOME=${getExternalFilesDir(null)?.absolutePath}",
                "COLORTERM=truecolor",
                "TERM=xterm-256color",
                "LANG=C.UTF-8",
                "BIN=${localBinDir()}",
                "DEBUG=${BuildConfig.DEBUG}",
                "PREFIX=${filesDir.parentFile!!.path}",
                "LD_LIBRARY_PATH=${localLibDir().absolutePath}",
                "LINKER=${if (File("/system/bin/linker64").exists()) "/system/bin/linker64" else "/system/bin/linker"}",
                "NATIVE_LIB_DIR=${applicationInfo.nativeLibraryDir}",
                "PKG=${packageName}",
                "RISH_APPLICATION_ID=${packageName}",
                "PKG_PATH=${applicationInfo.sourceDir}",
                "PROOT_TMP_DIR=${getTempDir(this).child(sessionId).also { if (it.exists().not()) it.mkdirs() }}",
                "TMPDIR=${getTempDir(this).absolutePath}",
                "PROOT_LOADER=${applicationInfo.nativeLibraryDir}/libloader.so",
                "PROOT=${applicationInfo.nativeLibraryDir}/libproot.so",
            )

            val loader32 = "${applicationInfo.nativeLibraryDir}/libloader32.so"
            if (File(loader32).exists()) {
                env.add("PROOT_LOADER_32=$loader32")
            }

            env.addAll(envVariables.map { "${it.key}=${it.value}" })

            localDir().child("stat").apply {
                if (exists().not()) {
                    writeText(TerminalUtils.stat)
                }
            }

            localDir().child("vmstat").apply {
                if (exists().not()) {
                    writeText(TerminalUtils.vmstat)
                }
            }

            pendingCommand?.env?.let {
                env.addAll(it)
            }

            val args: Array<String>
            val shell = if (pendingCommand == null) {
                args = if (workingMode == WorkingMode.ALPINE) {
                    arrayOf("-c", initFile.absolutePath)
                } else {
                    arrayOf()
                }
                "/system/bin/sh"
            } else {
                args = pendingCommand.args
                pendingCommand.shell
            }

            return TerminalSession(
                shell,
                workingDir,
                args,
                env.toTypedArray(),
                TerminalEmulator.DEFAULT_TERMINAL_TRANSCRIPT_ROWS,
                sessionClient,
            )
        }
    }
}

data class PendingCommand(
    val shell: String,
    val args: Array<String>,
    val workingDir: String?,
    val env: List<String>?
)
