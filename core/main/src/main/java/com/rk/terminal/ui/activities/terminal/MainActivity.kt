package com.rk.terminal.ui.activities.terminal

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rk.libcommons.child
import com.rk.libcommons.localDir
import com.rk.terminal.ui.navHosts.MainActivityNavHost
import com.rk.terminal.ui.routes.MainActivityRoutes
import com.rk.terminal.ui.screens.terminal.CustomSession
import com.rk.terminal.ui.screens.terminal.MkSession
import com.rk.terminal.ui.screens.terminal.RunScriptDialog
import com.rk.terminal.ui.screens.terminal.TerminalBackEnd
import com.rk.terminal.ui.screens.terminal.TerminalViewModel
import com.rk.terminal.ui.theme.KarbonTheme
import com.rk.terminal.ui.theme.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class MainActivity : ComponentActivity() {
    val viewModel: MainViewModel by viewModels()
    private val terminalViewModel: TerminalViewModel by viewModels()
    private var isKeyboardVisible = false
    private var wasKeyboardOpen = false
    private var pendingScript by mutableStateOf<File?>(null)

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                // Optional: Handle permission denied
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.apply(this)
        enableEdgeToEdge()
        requestPermission()

        if (intent.hasExtra("awake_intent")) {
            moveTaskToBack(true)
        }

        handleViewIntent(intent)

        setContent {
            val systemDark = isSystemInDarkTheme()
            val isDarkThemeActive = if (viewModel.followSystemTheme) systemDark else viewModel.isDarkMode
            KarbonTheme(
                darkTheme = isDarkThemeActive,
                highContrastDarkTheme = viewModel.isAmoled,
                dynamicColor = viewModel.isMonet,
                themePalette = viewModel.themePalette
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    MainActivityNavHost(
                        navController = navController,
                        mainActivity = this@MainActivity
                    )

                    val backStackEntry by navController.currentBackStackEntryAsState()
                    val focusManager = LocalFocusManager.current
                    val keyboardController = LocalSoftwareKeyboardController.current

                    LaunchedEffect(backStackEntry?.destination?.route) {
                        if (backStackEntry?.destination?.route != MainActivityRoutes.MainScreen.route) {
                            focusManager.clearFocus(force = true)
                            terminalViewModel.terminalView?.clearFocus()
                            keyboardController?.hide()
                        }
                    }

                    pendingScript?.let { script ->
                        RunScriptDialog(
                            scriptName = script.name,
                            onDismiss = { pendingScript = null },
                            onRun = { mode, custom -> runScript(script, mode, custom) }
                        )
                    }
                }
            }
        }
        
        setupKeyboardListener()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleViewIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        viewModel.startAndBindService(this)
    }

    override fun onStop() {
        super.onStop()
        viewModel.unbindService(this)
    }

    override fun onPause() {
        super.onPause()
        wasKeyboardOpen = isKeyboardVisible
    }

    override fun onResume() {
        super.onResume()
        if (wasKeyboardOpen && !isKeyboardVisible) {
            terminalViewModel.terminalView?.let { terminalView ->
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(terminalView, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupKeyboardListener() {
        val rootView = findViewById<View>(android.R.id.content)
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.rootView.height
            val keypadHeight = screenHeight - rect.bottom
            isKeyboardVisible = keypadHeight > screenHeight * 0.15
        }
    }

    private fun handleViewIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val uri = intent.data ?: return
        if (!isShellScriptUri(intent, uri)) return

        lifecycleScope.launch {
            val script = withContext(Dispatchers.IO) { saveScriptToLocal(uri) }
            if (script != null) {
                pendingScript = script
            }
        }
    }

    private fun isShellScriptUri(intent: Intent, uri: Uri): Boolean {
        val mime = intent.type?.lowercase(Locale.ROOT)
        if (mime in setOf("application/x-sh", "text/x-sh", "text/x-shellscript", "application/x-shellscript")) {
            return true
        }
        return uri.lastPathSegment?.lowercase(Locale.ROOT)?.endsWith(".sh") == true
    }

    private fun saveScriptToLocal(uri: Uri): File? {
        return try {
            val scriptsDir = localDir().child("scripts").apply { mkdirs() }
            val name = queryDisplayName(uri)?.ifBlank { null } ?: "script.sh"
            val safeName = File(name).name.let {
                if (it.endsWith(".sh", ignoreCase = true)) it else "$it.sh"
            }
            val target = File(scriptsDir, safeName)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            target.setExecutable(true)
            target
        } catch (e: Exception) {
            null
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun runScript(script: File, mode: Int, custom: CustomSession?) {
        val binder = viewModel.sessionBinder ?: return
        val terminal = terminalViewModel.terminalView ?: return
        val client = TerminalBackEnd(terminal, this)
        val pendingCommand = MkSession.buildScriptPendingCommand(this, script, mode, custom)
        val id = generateUniqueSessionId(binder.getService().sessionList.keys.toList())
        binder.createSession(id, client, mode, pendingCommand)
        terminalViewModel.changeSession(this, binder, id)
        pendingScript = null
    }

    private fun generateUniqueSessionId(existingIds: List<String>): String {
        var index = 1
        var newId: String
        do {
            newId = "script$index"
            index++
        } while (newId in existingIds)
        return newId
    }
}
