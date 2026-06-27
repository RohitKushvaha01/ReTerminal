package com.rk.terminal.ui.screens.customization

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rk.components.compose.preferences.base.PreferenceGroup
import com.rk.components.compose.preferences.base.PreferenceLayout
import com.rk.components.compose.preferences.base.PreferenceTemplate
import com.rk.components.compose.preferences.switch.PreferenceSwitch
import com.rk.libcommons.*
import com.rk.resources.strings
import com.rk.settings.Settings
import com.rk.terminal.ui.activities.terminal.MainViewModel
import com.rk.terminal.ui.components.SettingsToggle
import com.rk.terminal.ui.screens.terminal.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.math.RoundingMode
import java.text.DecimalFormat

private const val MIN_TEXT_SIZE = 10f
private const val MAX_TEXT_SIZE = 20f

@Composable
fun Customization(
    mainActivity: com.rk.terminal.ui.activities.terminal.MainActivity,
    navController: NavController,
    mainViewModel: MainViewModel = viewModel(mainActivity),
    terminalViewModel: TerminalViewModel = viewModel(mainActivity)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    PreferenceLayout(
        label = stringResource(strings.customizations),
        onBack = { navController.popBackStack() }
    ) {
        var sliderPosition by remember { mutableFloatStateOf(Settings.terminal_font_size.toFloat()) }
        
        PreferenceGroup {
            PreferenceTemplate(title = { Text(stringResource(strings.text_size)) }) {
                Text(sliderPosition.toInt().toString())
            }
            PreferenceTemplate(title = {}) {
                Slider(
                    value = sliderPosition,
                    onValueChange = {
                        sliderPosition = it
                        Settings.terminal_font_size = it.toInt()
                        terminalViewModel.terminalView?.setTextSize(dpToPx(it, context))
                    },
                    steps = (MAX_TEXT_SIZE - MIN_TEXT_SIZE).toInt() - 1,
                    valueRange = MIN_TEXT_SIZE..MAX_TEXT_SIZE,
                )
            }
        }

        PreferenceGroup {
            FontSection(terminalViewModel)
        }

        PreferenceGroup {
            BackgroundSection(terminalViewModel)
        }

        PreferenceGroup {
            PreferenceTemplate(title = { Text(stringResource(strings.wallpaper_alpha)) }) {
                Text(DecimalFormat("0.##").apply { roundingMode = RoundingMode.HALF_UP }.format(terminalViewModel.wallAlpha))
            }
            PreferenceTemplate(title = {}) {
                Slider(
                    value = terminalViewModel.wallAlpha,
                    onValueChange = { terminalViewModel.wallAlpha = it },
                    onValueChangeFinished = { Settings.wallTransparency = terminalViewModel.wallAlpha }
                )
            }
        }

        PreferenceGroup() {
            PreferenceTemplate(title = { Text("Background Blur") }) {
                Text(terminalViewModel.backgroundBlur.toInt().toString())
            }
            PreferenceTemplate(title = {}) {
                Slider(
                    value = terminalViewModel.backgroundBlur,
                    onValueChange = { terminalViewModel.backgroundBlur = it },
                    onValueChangeFinished = { Settings.background_blur = terminalViewModel.backgroundBlur },
                    valueRange = 0f..25f,
                    steps = 24
                )
            }
        }

        PreferenceGroup {
            SettingsToggle(label = stringResource(strings.bell), description = stringResource(strings.bell_desc), showSwitch = true, default = Settings.bell, sideEffect = { Settings.bell = it })
            SettingsToggle(label = stringResource(strings.vibrate), description = stringResource(strings.vibrate_desc), showSwitch = true, default = Settings.vibrate, sideEffect = { Settings.vibrate = it })
        }

        PreferenceGroup {
            SettingsToggle(
                label = stringResource(strings.statusbar),
                description = stringResource(strings.statusbar_desc),
                showSwitch = true,
                default = Settings.statusBar,
                sideEffect = {
                    Settings.statusBar = it
                    mainViewModel.showStatusBar = it
                }
            )

            SettingsToggle(
                label = stringResource(strings.horizontal_statusbar),
                description = stringResource(strings.horizontal_statusbar_desc),
                showSwitch = true,
                default = Settings.horizontal_statusBar,
                sideEffect = {
                    Settings.horizontal_statusBar = it
                    mainViewModel.horizontalStatusBar = it
                }
            )

            ToolbarSection(terminalViewModel)

            SettingsToggle(
                label = stringResource(strings.horizontal_titlebar),
                description = stringResource(strings.horizontal_titlebar_desc),
                showSwitch = true,
                isEnabled = terminalViewModel.showToolbar,
                default = Settings.toolbar_in_horizontal,
                sideEffect = {
                    Settings.toolbar_in_horizontal = it
                    terminalViewModel.showHorizontalToolbar = it
                }
            )

            SettingsToggle(
                label = stringResource(strings.virtual_keys),
                description = stringResource(strings.virtual_keys_desc),
                showSwitch = true,
                default = Settings.virtualKeys,
                sideEffect = {
                    Settings.virtualKeys = it
                    terminalViewModel.showVirtualKeys = it
                }
            )

            SettingsToggle(
                label = stringResource(strings.hide_soft_keyboard),
                description = stringResource(strings.hide_soft_keyboard_desc),
                showSwitch = true,
                default = Settings.hide_soft_keyboard_if_hwd,
                sideEffect = { Settings.hide_soft_keyboard_if_hwd = it }
            )
        }

        ShortcutSection()
    }
}

@Composable
private fun FontSection(viewModel: TerminalViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fontFile = context.filesDir.child("font.ttf")
    var fontExists by remember { mutableStateOf(fontFile.exists()) }
    val noFontSelected = stringResource(strings.no_font_selected)
    var fontName by remember { mutableStateOf(if (!fontExists || !fontFile.canRead()) noFontSelected else Settings.custom_font_name) }

    val fontLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                fontFile.createFileIfNot()
                context.contentResolver.openInputStream(it)?.use { input ->
                    fontFile.outputStream().use { output -> input.copyTo(output) }
                }
                val name = context.getFileNameFromUri(it).toString()
                Settings.custom_font_name = name
                withContext(Dispatchers.Main) {
                    fontName = name
                    fontExists = true
                    viewModel.setFont(Typeface.createFromFile(fontFile))
                }
            }
        }
    }

    PreferenceTemplate(
        modifier = Modifier.clickable { fontLauncher.launch("font/ttf") },
        title = { Text(stringResource(strings.custom_font)) },
        description = { Text(fontName) },
        endWidget = {
            if (fontExists) {
                IconButton(onClick = {
                    scope.launch {
                        fontFile.delete()
                        fontName = noFontSelected
                        Settings.custom_font_name = noFontSelected
                        viewModel.setFont(Typeface.MONOSPACE)
                        fontExists = false
                    }
                }) {
                    Icon(imageVector = Icons.Outlined.Delete, contentDescription = "delete")
                }
            }
        }
    )
}

@Composable
private fun BackgroundSection(viewModel: TerminalViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imageFile = context.filesDir.child("background")
    var imageExists by remember { mutableStateOf(imageFile.exists()) }
    val noImageSelected = stringResource(strings.no_image_selected)
    var backgroundName by remember { mutableStateOf(if (!imageExists || !imageFile.canRead()) noImageSelected else Settings.custom_background_name) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                imageFile.createFileIfNot()
                context.contentResolver.openInputStream(it)?.use { input ->
                    imageFile.outputStream().use { output -> input.copyTo(output) }
                }
                val name = context.getFileNameFromUri(it).toString()
                Settings.custom_background_name = name
                
                val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                bitmap?.let { b ->
                    val palette = Palette.from(b).generate()
                    val dominantColor = palette.getDominantColor(android.graphics.Color.WHITE)
                    val luminance = androidx.core.graphics.ColorUtils.calculateLuminance(dominantColor)
                    val isDark = luminance > 0.5f
                    Settings.blackTextColor = isDark
                    withContext(Dispatchers.Main) {
                        TerminalUtils.darkText.value = isDark
                        viewModel.bitmap = b.asImageBitmap()
                        backgroundName = name
                        imageExists = true
                    }
                }
            }
        }
    }

    PreferenceTemplate(
        modifier = Modifier.clickable { launcher.launch("image/*") },
        title = { Text(stringResource(strings.custom_background)) },
        description = { Text(backgroundName) },
        endWidget = {
            if (imageExists) {
                val isDarkMode = isSystemInDarkTheme()
                IconButton(onClick = {
                    scope.launch {
                        imageFile.delete()
                        Settings.custom_background_name = noImageSelected
                        backgroundName = noImageSelected
                        TerminalUtils.darkText.value = !isDarkMode
                        imageExists = false
                        viewModel.bitmap = null
                    }
                }) {
                    Icon(imageVector = Icons.Outlined.Delete, contentDescription = "delete")
                }
            }
        }
    )
}

@Composable
private fun ToolbarSection(viewModel: TerminalViewModel) {
    val context = LocalContext.current
    val attentionTitle = stringResource(strings.attention)
    val toolbarWarning = stringResource(strings.toolbar_warning)
    val cancelStr = stringResource(strings.cancel)

    val toggleToolbar: (Boolean) -> Unit = { checked ->
        if (!checked && viewModel.showToolbar) {
            MaterialAlertDialogBuilder(context).apply {
                setTitle(attentionTitle)
                setMessage(toolbarWarning)
                setPositiveButton("OK") { _, _ ->
                    Settings.toolbar = false
                    viewModel.showToolbar = false
                }
                setNegativeButton(cancelStr, null)
                show()
            }
        } else {
            Settings.toolbar = checked
            viewModel.showToolbar = checked
        }
    }

    PreferenceSwitch(
        checked = viewModel.showToolbar,
        onCheckedChange = toggleToolbar,
        label = stringResource(strings.titlebar),
        description = stringResource(strings.titlebar_desc),
        onClick = { toggleToolbar(!viewModel.showToolbar) }
    )
}

@Composable
private fun ShortcutSection() {
    PreferenceGroup(heading = stringResource(strings.keyboard_shortcuts)) {
        var shortcutsEnabled by remember { mutableStateOf(Settings.shortcuts_enabled) }
        var showCaptureFor by remember { mutableStateOf<ShortcutAction?>(null) }

        SettingsToggle(
            label = stringResource(strings.keyboard_shortcuts),
            description = stringResource(strings.keyboard_shortcuts_desc),
            showSwitch = true,
            default = Settings.shortcuts_enabled,
            sideEffect = {
                Settings.shortcuts_enabled = it
                shortcutsEnabled = it
            }
        )

        ShortcutAction.entries.forEach { action ->
            val binding = Settings.getShortcutBinding(action)
            val labelRes = when (action) {
                ShortcutAction.PASTE -> strings.shortcut_paste
                ShortcutAction.NEW_SESSION -> strings.shortcut_new_session
                ShortcutAction.CLOSE_SESSION -> strings.shortcut_close_session
                ShortcutAction.SWITCH_SESSION_PREV -> strings.shortcut_switch_prev
                ShortcutAction.SWITCH_SESSION_NEXT -> strings.shortcut_switch_next
            }
            val descRes = when (action) {
                ShortcutAction.PASTE -> strings.shortcut_paste_desc
                ShortcutAction.NEW_SESSION -> strings.shortcut_new_session_desc
                ShortcutAction.CLOSE_SESSION -> strings.shortcut_close_session_desc
                ShortcutAction.SWITCH_SESSION_PREV -> strings.shortcut_switch_prev_desc
                ShortcutAction.SWITCH_SESSION_NEXT -> strings.shortcut_switch_next_desc
            }
            SettingsToggle(
                isEnabled = shortcutsEnabled,
                label = stringResource(labelRes),
                description = "${stringResource(descRes)} (${binding.toDisplayString()})",
                showSwitch = false,
                default = false,
                sideEffect = { showCaptureFor = action },
            )
        }

        if (showCaptureFor != null) {
            ShortcutCaptureDialog(
                action = showCaptureFor!!,
                onDismiss = { showCaptureFor = null },
                onConfirm = { binding ->
                    Settings.setShortcutBinding(showCaptureFor!!, binding)
                    showCaptureFor = null
                }
            )
        }
    }
}
