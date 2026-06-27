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
import com.rk.terminal.ui.screens.terminal.Rootfs
import com.rk.terminal.ui.screens.terminal.TerminalScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

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

    LaunchedEffect(Unit) {
        if (isSetupComplete) {
            Rootfs.isInstalled.value = true
            return@LaunchedEffect
        }

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

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (!isSetupComplete) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (error != null) {
                    Text("Setup Failed: $error", color = MaterialTheme.colorScheme.error)
                } else {
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
