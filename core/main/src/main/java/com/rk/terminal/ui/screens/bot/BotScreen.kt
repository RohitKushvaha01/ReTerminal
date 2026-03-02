package com.rk.terminal.ui.screens.bot

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.rk.libcommons.*
import com.rk.terminal.ui.activities.terminal.MainActivity
import com.rk.terminal.ui.routes.MainActivityRoutes
import com.rk.terminal.ui.screens.terminal.TerminalBackEnd
import com.rk.terminal.ui.screens.terminal.changeSession
import com.rk.terminal.ui.screens.terminal.terminalView
import com.rk.settings.Settings
import com.termux.view.TerminalView

@Composable
fun BotScreen(
    mainActivity: MainActivity,
    navController: NavHostController
) {
    val context = mainActivity
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("FileStreamBot Control", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = {
                startBot(mainActivity)
                navController.navigate(MainActivityRoutes.MainScreen.route)
            }) {
                Text("Iniciar servidor")
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = {
                navController.navigate(MainActivityRoutes.BotSettings.route)
            }) {
                Text("Configurações")
            }
        }
    }
}

fun startBot(mainActivity: MainActivity) {
    val sessionId = "FileStreamBot"

    val env = mutableMapOf<String, String>()
    env["START_BOT"] = "1"
    env["TELEGRAM_API_ID"] = Settings.bot_api_id
    env["TELEGRAM_API_HASH"] = Settings.bot_api_hash
    env["OWNER_ID"] = Settings.bot_owner_id
    env["ALLOWED_USER_IDS"] = Settings.bot_allowed_ids
    env["TELEGRAM_BOT_USERNAME"] = Settings.bot_username
    env["TELEGRAM_BOT_TOKEN"] = Settings.bot_token
    env["TELEGRAM_CHANNEL_ID"] = Settings.bot_channel_id
    env["SECRET_CODE_LENGTH"] = Settings.bot_secret_length
    env["BASE_URL"] = Settings.bot_base_url
    env["BIND_ADDRESS"] = Settings.bot_bind_addr
    env["PORT"] = Settings.bot_port

    val terminalViewInstance = terminalView.get() ?: TerminalView(mainActivity, null)
    val client = TerminalBackEnd(terminalViewInstance, mainActivity)
    mainActivity.sessionBinder!!.createSession(
        sessionId,
        client,
        mainActivity,
        workingMode = 0, // ALPINE
        env = env
    )
    changeSession(mainActivity, sessionId)
}
