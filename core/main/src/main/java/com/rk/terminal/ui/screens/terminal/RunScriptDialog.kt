package com.rk.terminal.ui.screens.terminal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rk.resources.strings
import com.rk.terminal.ui.screens.settings.WorkingMode

@Composable
fun RunScriptDialog(
    scriptName: String,
    onDismiss: () -> Unit,
    onRun: (Int, CustomSession?) -> Unit
) {
    val customSessions = remember { CustomSessions.getAll() }
    var selectedMode by remember { mutableIntStateOf(WorkingMode.ALPINE) }
    var selectedCustom by remember { mutableStateOf<CustomSession?>(null) }
    var selectedIsCustom by remember { mutableStateOf(false) }

    fun select(mode: Int, custom: CustomSession?, isCustom: Boolean) {
        selectedMode = mode
        selectedCustom = custom
        selectedIsCustom = isCustom
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Run script?") },
        text = {
            Column {
                Text(
                    text = scriptName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Session type",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.height(8.dp))
                ScriptSessionOption(
                    title = "Alpine",
                    description = stringResource(strings.alpine_desc),
                    selected = !selectedIsCustom && selectedMode == WorkingMode.ALPINE
                ) { select(WorkingMode.ALPINE, null, false) }
                ScriptSessionOption(
                    title = "Android",
                    description = stringResource(strings.android_desc),
                    selected = !selectedIsCustom && selectedMode == WorkingMode.ANDROID
                ) { select(WorkingMode.ANDROID, null, false) }
                customSessions.forEach { session ->
                    ScriptSessionOption(
                        title = session.name,
                        description = session.shellPath,
                        selected = selectedIsCustom && selectedCustom?.id == session.id
                    ) { select(WorkingMode.ALPINE, session, true) }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onRun(selectedMode, selectedCustom) }) { Text("Run") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(stringResource(strings.cancel)) }
        }
    )
}

@Composable
private fun ScriptSessionOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
