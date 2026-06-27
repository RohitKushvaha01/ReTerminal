package com.rk.terminal.ui.screens.terminal

import android.content.res.Configuration
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.rk.components.compose.preferences.base.PreferenceGroup
import com.rk.libcommons.child
import com.rk.resources.strings
import com.rk.terminal.ui.activities.terminal.MainActivity
import com.rk.terminal.ui.activities.terminal.MainViewModel
import com.rk.terminal.ui.components.SetStatusBarTextColor
import com.rk.terminal.ui.screens.settings.SettingsCard
import com.rk.terminal.ui.screens.settings.WorkingMode
import com.rk.terminal.ui.screens.terminal.virtualkeys.VirtualKeysListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    mainActivity: MainActivity,
    navController: NavController,
    mainViewModel: MainViewModel = viewModel(mainActivity),
    terminalViewModel: TerminalViewModel = viewModel(mainActivity)
) {
    val context = LocalContext.current
    val isDarkMode = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val configuration = LocalConfiguration.current
    val drawerWidth = (configuration.screenWidthDp * 0.84).dp
    var showAddDialog by remember { mutableStateOf(false) }

    val sessionBinder = mainViewModel.sessionBinder

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            if (context.filesDir.child("background").exists().not()) {
                TerminalUtils.darkText.value = !isDarkMode
            } else if (terminalViewModel.bitmap == null) {
                BitmapFactory.decodeFile(context.filesDir.child("background").absolutePath)?.asImageBitmap()?.let {
                    terminalViewModel.bitmap = it
                }
            }
        }
    }
    
    // Update virtual keys when they are available
    terminalViewModel.virtualKeysView?.apply {
        virtualKeysViewClient = terminalViewModel.terminalView?.mTermSession?.let { VirtualKeysListener(it) }
        buttonTextColor = TerminalUtils.getViewColor()
    }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    val isDarkIcons = if (drawerState.isClosed) TerminalUtils.darkText.value else !isDarkMode
    SetStatusBarTextColor(isDarkIcons = isDarkIcons)

    if (showAddDialog && sessionBinder != null) {
        AddSessionDialog(
            onDismiss = { showAddDialog = false },
            onCreateSession = { mode ->
                val sessionId = generateUniqueSessionId(sessionBinder.getService().sessionList.keys.toList())
                val terminal = terminalViewModel.terminalView ?: return@AddSessionDialog
                val client = TerminalBackEnd(terminal, mainActivity)
                sessionBinder.createSession(sessionId, client, mode)
                terminalViewModel.changeSession(context, sessionBinder, sessionId)
                showAddDialog = false
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen || !terminalViewModel.showToolbar,
        drawerContent = {
            TerminalDrawer(
                drawerWidth = drawerWidth,
                sessionBinder = sessionBinder,
                navController = navController,
                onAddSession = { showAddDialog = true },
                onSessionSelected = { id ->
                    sessionBinder?.let { terminalViewModel.changeSession(context, it, id) }
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            BackgroundImage(terminalViewModel)
            
            Column {
                if (terminalViewModel.showToolbar) {
                    TerminalTopBar(
                        sessionBinder = sessionBinder,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onAddClick = { showAddDialog = true },
                        color = TerminalUtils.getComposeColor()
                    )
                }

                val density = LocalDensity.current
                val topPadding = if (terminalViewModel.showToolbar) 0.dp else {
                    with(density) { TopAppBarDefaults.windowInsets.getTop(this).toDp() }
                }

                if (sessionBinder != null) {
                    TerminalViewLayout(
                        viewModel = terminalViewModel,
                        mainActivity = mainActivity,
                        sessionBinder = sessionBinder,
                        modifier = Modifier
                            .imePadding()
                            .navigationBarsPadding()
                            .padding(top = topPadding)
                            .fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun BackgroundImage(viewModel: TerminalViewModel) {
    viewModel.bitmap?.let { bitmap ->
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(viewModel.wallAlpha)
                .let {
                    if (viewModel.backgroundBlur > 0f) {
                        it.blur(viewModel.backgroundBlur.dp)
                    } else {
                        it
                    }
                }
                .zIndex(-1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSessionDialog(onDismiss: () -> Unit, onCreateSession: (Int) -> Unit) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        PreferenceGroup {
            SettingsCard(
                title = { Text("Alpine") },
                description = { Text(stringResource(strings.alpine_desc)) },
                onClick = { onCreateSession(WorkingMode.ALPINE) }
            )
            SettingsCard(
                title = { Text("Android") },
                description = { Text(stringResource(strings.android_desc)) },
                onClick = { onCreateSession(WorkingMode.ANDROID) }
            )
        }
    }
}

private fun generateUniqueSessionId(existingIds: List<String>): String {
    var index = 1
    var newId: String
    do {
        newId = "main$index"
        index++
    } while (newId in existingIds)
    return newId
}

const val VIRTUAL_KEYS = "[" +
    "\n  [\"ESC\", {\"key\": \"/\", \"popup\": \"\\\\\"}, {\"key\": \"-\", \"popup\": \"|\"}, \"HOME\", \"UP\", \"END\", \"PGUP\"]," +
    "\n  [\"TAB\", \"CTRL\", \"ALT\", \"LEFT\", \"DOWN\", \"RIGHT\", \"PGDN\"]" +
    "\n]"
