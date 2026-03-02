package com.rk.terminal.ui.screens.bot

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.rk.components.compose.preferences.base.PreferenceGroup
import com.rk.components.compose.preferences.base.PreferenceLayout
import com.rk.components.compose.preferences.entry.PreferenceEntry
import com.rk.settings.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BotSettings(navController: NavController) {
    PreferenceLayout(
        label = "Bot Settings",
        backArrowClick = { navController.popBackStack() }
    ) {
        PreferenceGroup(label = "Telegram Configuration") {
            BotSettingEntry(title = "API ID", value = Settings.bot_api_id) { Settings.bot_api_id = it }
            BotSettingEntry(title = "API Hash", value = Settings.bot_api_hash) { Settings.bot_api_hash = it }
            BotSettingEntry(title = "Owner ID", value = Settings.bot_owner_id) { Settings.bot_owner_id = it }
            BotSettingEntry(title = "Allowed User IDs", value = Settings.bot_allowed_ids) { Settings.bot_allowed_ids = it }
            BotSettingEntry(title = "Bot Username", value = Settings.bot_username) { Settings.bot_username = it }
            BotSettingEntry(title = "Bot Token", value = Settings.bot_token) { Settings.bot_token = it }
            BotSettingEntry(title = "Channel ID", value = Settings.bot_channel_id) { Settings.bot_channel_id = it }
        }
        PreferenceGroup(label = "Server Configuration") {
            BotSettingEntry(title = "Secret Code Length", value = Settings.bot_secret_length) { Settings.bot_secret_length = it }
            BotSettingEntry(title = "Base URL", value = Settings.bot_base_url) { Settings.bot_base_url = it }
            BotSettingEntry(title = "Bind Address", value = Settings.bot_bind_addr) { Settings.bot_bind_addr = it }
            BotSettingEntry(title = "Port", value = Settings.bot_port) { Settings.bot_port = it }
        }
    }
}

@Composable
fun BotSettingEntry(title: String, value: String, onValueChange: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    var tempValue by remember { mutableStateOf(value) }

    PreferenceEntry(
        title = title,
        description = value,
        onClick = { showDialog = true }
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = {
                OutlinedTextField(
                    value = tempValue,
                    onValueChange = { tempValue = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onValueChange(tempValue)
                    showDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
