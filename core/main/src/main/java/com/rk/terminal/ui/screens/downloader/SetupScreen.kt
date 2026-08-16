package com.rk.terminal.ui.screens.downloader

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.rk.libcommons.*
import com.rk.resources.strings
import com.rk.terminal.ui.activities.terminal.MainActivity
import com.rk.terminal.ui.screens.terminal.ExecMode
import com.rk.terminal.ui.screens.terminal.Rootfs
import com.rk.terminal.ui.screens.terminal.TerminalScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private fun hasRootAccess(): Boolean {
    val paths = listOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/su/bin/su")
    if (paths.none { File(it).exists() }) return false
    return try {
        val process = ProcessBuilder("su", "-c", "id").redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        val exited = process.waitFor()
        exited == 0 && output.contains("uid=0")
    } catch (e: Exception) {
        false
    }
}

@Composable
fun SetupScreen(
    modifier: Modifier = Modifier,
    mainActivity: MainActivity,
    navController: NavHostController
) {
    val context = LocalContext.current
    val installingStr = stringResource(strings.installing)
    val setupFailedStr = stringResource(strings.setup_failed)

    var isSetupComplete by remember { mutableStateOf(Rootfs.isRootfsInstalled(context)) }
    var error by remember { mutableStateOf<String?>(null) }
    var rootChecked by remember { mutableStateOf(false) }
    var showExecModeDialog by remember { mutableStateOf(false) }
    var extractionStarted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (Rootfs.execMode.value != null) {
            rootChecked = true
            if (isSetupComplete) {
                Rootfs.isInstalled.value = true
            } else {
                extractionStarted = true
            }
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            val hasRoot = hasRootAccess()
            withContext(Dispatchers.Main) {
                rootChecked = true
                if (hasRoot) {
                    showExecModeDialog = true
                } else {
                    Rootfs.setExecMode(ExecMode.PROOT)
                    if (isSetupComplete) {
                        Rootfs.isInstalled.value = true
                    } else {
                        extractionStarted = true
                    }
                }
            }
        }
    }

    if (showExecModeDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Root Access Detected") },
            text = {
                Text("Root access was found. Run the terminal with chroot (faster, requires root for every session) or proot (no root required, slightly slower)? You can change this later in Settings.")
            },
            confirmButton = {
                TextButton(onClick = {
                    Rootfs.setExecMode(ExecMode.CHROOT)
                    showExecModeDialog = false
                    if (isSetupComplete) {
                        Rootfs.isInstalled.value = true
                    } else {
                        extractionStarted = true
                    }
                }) { Text("Chroot") }
            },
            dismissButton = {
                TextButton(onClick = {
                    Rootfs.setExecMode(ExecMode.PROOT)
                    showExecModeDialog = false
                    if (isSetupComplete) {
                        Rootfs.isInstalled.value = true
                    } else {
                        extractionStarted = true
                    }
                }) { Text("Proot") }
            }
        )
    }

    LaunchedEffect(extractionStarted) {
        if (!extractionStarted || isSetupComplete) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                val abis = Build.SUPPORTED_ABIS
                val abi = abis.firstOrNull {
                    it in listOf("arm64-v8a", "armeabi-v7a", "x86_64")
                } ?: throw RuntimeException("Unsupported CPU architectures: ${abis.joinToString()}")
                val alpineArch = when (abi) {
                    "arm64-v8a" -> "aarch64"
                    "armeabi-v7a" -> "armhf"
                    "x86_64" -> "x86_64"
                    else -> throw RuntimeException("Unsupported ABI: $abi")
                }
                val assetName = "alpine-$alpineArch.tar.gz.rootfs"
                val outputFile = context.filesDir.child("alpine.tar.gz")
                if (!outputFile.exists() || outputFile.length() == 0L) {
                    context.assets.open(assetName).use { input ->
                        FileOutputStream(outputFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    Rootfs.isInstalled.value = true
                    isSetupComplete = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    error = e.javaClass.simpleName + ": " + e.message
                    toast(setupFailedStr.format(e.message))
                }
            }
        }
    }

    val ready = isSetupComplete && Rootfs.execMode.value != null

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (!ready) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (error != null) {
                    Text("Setup Failed: $error", color = MaterialTheme.colorScheme.error)
                } else if (!rootChecked) {
                    Text("Checking root access...", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator()
                } else if (!showExecModeDialog) {
                    Text(installingStr, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator()
                }
            }
        } else {
            TerminalScreen(mainActivity = mainActivity, navController = navController)
        }
    }
}
