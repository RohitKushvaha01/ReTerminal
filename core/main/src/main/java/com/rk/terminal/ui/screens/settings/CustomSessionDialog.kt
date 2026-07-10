package com.rk.terminal.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.BufferedInputStream
import java.io.File
import java.net.URLConnection

private fun classifyWithFileCommand(path: String): String? {
    return try {
        val process = ProcessBuilder("file", "-b", path)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText().trim()
        val exited = process.waitFor()
        if (exited == 0 && output.isNotBlank()) output else null
    } catch (e: Exception) {
        null
    }
}

private fun classifyWithAndroidMime(path: String): String? {
    return try {
        File(path).inputStream().use { raw ->
            BufferedInputStream(raw).use { buffered ->
                URLConnection.guessContentTypeFromStream(buffered)
            }
        }
    } catch (e: Exception) {
        null
    }
}

private fun nullByteCheck(path: String): Boolean {
    val file = File(path)
    if (!file.exists() || !file.isFile || file.length() == 0L) return false
    return try {
        val sampleSize = minOf(file.length(), 8192L).toInt()
        val bytes = file.inputStream().use { it.readNBytes(sampleSize) }
        if (bytes.contains(0.toByte())) return false
        bytes.toString(Charsets.UTF_8).isNotBlank()
    } catch (e: Exception) {
        false
    }
}

private fun isPosixShellScript(path: String): Boolean {
    val fileOutput = classifyWithFileCommand(path)
    if (fileOutput != null) {
        val lower = fileOutput.lowercase()
        val badSignals = listOf(
            "elf", "executable", "shared object", "image data", "archive",
            "zip", "apk", "compressed", "audio", "video", "pdf document"
        )
        if (badSignals.any { lower.contains(it) }) return false

        val goodSignals = listOf("shell script", "posix shell script", "ascii text", "text executable", "script text", "text data")
        if (goodSignals.any { lower.contains(it) }) return true
        // file exists but output didn't match either list clearly; fall through to next check
    }

    val mimeType = classifyWithAndroidMime(path)
    if (mimeType != null) {
        val badMimes = listOf(
            "application/x-elf", "application/x-executable", "application/zip",
            "application/vnd.android.package-archive", "image/", "audio/", "video/",
            "application/pdf", "application/octet-stream"
        )
        if (badMimes.any { mimeType.startsWith(it) }) return false
        if (mimeType.startsWith("text/")) return true
    }

    // Neither tool gave a confident signal (or file doesn't exist as a recognizable
    // type at all, e.g. a plain script with no MIME match); fall back to a basic
    // binary/text heuristic so legitimate shell scripts aren't blocked.
    return nullByteCheck(path)
}

@Composable
fun CustomSessionDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, shellPath: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var shellPath by remember { mutableStateOf("/sdcard/ReTerminal/") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun validate(): String? {
        val trimmedName = name.trim()
        val trimmedPath = shellPath.trim()

        if (trimmedName.isBlank()) return "Session name cannot be empty"
        if (trimmedPath.isBlank()) return "Shell script path cannot be empty"
        if (!trimmedPath.startsWith("/")) return "Path must be an absolute path (start with /)"

        val file = File(trimmedPath)
        if (!file.exists()) return "File does not exist at this path"
        if (!file.isFile) return "Path is not a file"
        if (!isPosixShellScript(trimmedPath)) return "File does not appear to be a valid shell script"

        return null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Custom Session") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Session name") },
                    isError = errorMessage != null && name.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = shellPath,
                    onValueChange = {
                        shellPath = it
                        errorMessage = null
                    },
                    label = { Text("Shell script path") },
                    isError = errorMessage != null && shellPath.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val error = validate()
                if (error != null) {
                    errorMessage = error
                } else {
                    onSave(name.trim(), shellPath.trim())
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
