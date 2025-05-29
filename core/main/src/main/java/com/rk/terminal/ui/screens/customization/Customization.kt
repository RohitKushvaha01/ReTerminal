package com.rk.terminal.ui.screens.customization

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rk.components.compose.preferences.base.PreferenceGroup
import com.rk.components.compose.preferences.base.PreferenceLayout
import com.rk.components.compose.preferences.base.PreferenceTemplate
import com.rk.components.compose.preferences.switch.PreferenceSwitch
import com.rk.libcommons.child
import com.rk.libcommons.createFileIfNot
import com.rk.libcommons.dpToPx
import com.rk.settings.Settings
import com.rk.terminal.ui.components.InfoBlock
import com.rk.terminal.ui.components.SettingsToggle
import com.rk.terminal.ui.navHosts.horizontal_statusBar
import com.rk.terminal.ui.navHosts.showStatusBar
import com.rk.terminal.ui.screens.terminal.bitmap
import com.rk.terminal.ui.screens.terminal.darkText
import com.rk.terminal.ui.screens.terminal.setFont
import com.rk.terminal.ui.screens.terminal.showHorizontalToolbar
import com.rk.terminal.ui.screens.terminal.showToolbar
import com.rk.terminal.ui.screens.terminal.showVirtualKeys
import com.rk.terminal.ui.screens.terminal.terminalView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


private const val min_text_size = 10f
private const val max_text_size = 20f

@Composable
fun Customization(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    PreferenceLayout(label = "Customizations") {
        var sliderPosition by remember { mutableFloatStateOf(Settings.terminal_font_size.toFloat()) }
        PreferenceGroup {
            PreferenceTemplate(title = { Text("Text Size") }) {
                Text(sliderPosition.toInt().toString())
            }
            PreferenceTemplate(title = {}) {
                Slider(
                    modifier = modifier,
                    value = sliderPosition,
                    onValueChange = {
                        sliderPosition = it
                        Settings.terminal_font_size = it.toInt()
                        terminalView.get()?.setTextSize(dpToPx(it.toFloat(), context))
                    },
                    steps = (max_text_size - min_text_size).toInt() - 1,
                    valueRange = min_text_size..max_text_size,
                )
            }
        }

        fun getFileNameFromUri(context: Context, uri: Uri): String? {
            if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex != -1) {
                        return cursor.getString(nameIndex)
                    }
                }
            } else if (uri.scheme == ContentResolver.SCHEME_FILE) {
                return File(uri.path!!).name
            }
            return null
        }

        PreferenceGroup {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(15.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Outlined.Info, contentDescription = null)
                }
                Text(
                    text = "Only monospaced fonts are supported. Non-monospaced fonts may not render correctly.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            val scope = rememberCoroutineScope()
            val font by remember { mutableStateOf<File>(context.filesDir.child("font.ttf")) }
            var fontExists by remember { mutableStateOf(font.exists()) }

            var fontName by remember { mutableStateOf(if (!fontExists || !font.canRead()){
                "No Font Selected"
            }else{
                Settings.custom_font_name
            }) }

            val fontLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let {
                    scope.launch(Dispatchers.IO){
                        font.createFileIfNot()
                        context.contentResolver.openInputStream(it)?.use { inputStream ->
                            font.outputStream().use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }

                        val name = getFileNameFromUri(context,uri).toString()
                        Settings.custom_font_name = name
                        fontName = name
                        fontExists = font.exists()
                        setFont(Typeface.createFromFile(font))
                    }
                }

            }

            PreferenceTemplate(
                modifier = Modifier.clickable(onClick = {
                    scope.launch{
                        fontLauncher.launch("font/ttf")
                    }
                }),
                title = {
                    Text("Custom Font")
                },
                description = {
                    Text(fontName)
                },
                endWidget = {
                    if (fontExists){
                        IconButton(onClick = {
                            scope.launch{
                                font.delete()
                                fontName = "No Font Selected"
                                Settings.custom_font_name = "No Font Selected"
                                setFont(Typeface.MONOSPACE)
                                fontExists = font.exists()
                            }
                        }) {
                            Icon(imageVector = Icons.Outlined.Delete,contentDescription = "delete")
                        }
                    }
                }
            )
        }

        PreferenceGroup {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val image by remember { mutableStateOf<File>(context.filesDir.child("background")) }


            var imageExists by remember { mutableStateOf(image.exists()) }



            var backgroundName by remember { mutableStateOf(if (!imageExists || !image.canRead()){
                "No Image Selected"
            }else{
                Settings.custom_background_name
            }) }



            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let {
                    scope.launch(Dispatchers.IO){
                        image.createFileIfNot()
                        context.contentResolver.openInputStream(it)?.use { inputStream ->
                            image.outputStream().use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }

                        val name = getFileNameFromUri(context,uri).toString()
                        Settings.custom_background_name = name
                        backgroundName = name


                        withContext(Dispatchers.IO) {
                            val file = context.filesDir.child("background")
                            if (!file.exists()) return@withContext
                            bitmap.value = BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                            bitmap.value?.apply {
                                val androidBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                val buffer = IntArray(width * height)
                                readPixels(buffer, 0, 0, width, height)
                                androidBitmap.setPixels(buffer, 0, width, 0, 0, width, height)
                                Palette.from(androidBitmap).generate { palette ->
                                    val dominantColor = palette?.getDominantColor(android.graphics.Color.WHITE)
                                    val luminance = androidx.core.graphics.ColorUtils.calculateLuminance(dominantColor ?: android.graphics.Color.WHITE)
                                    val blackText = luminance > 0.5f
                                    Settings.blackTextColor = blackText
                                    darkText.value = blackText
                                }
                            }

                        }
                        imageExists = image.exists()
                    }

                }


            }

            PreferenceTemplate(
                modifier = Modifier.clickable(onClick = {
                    scope.launch{
                        launcher.launch("image/*")
                    }
                }),
                title = {
                    Text("Custom Background")
                },
                description = {
                    Text(backgroundName)
                },
                endWidget = {
                    val darkMode = isSystemInDarkTheme()
                    if (imageExists){
                        IconButton(onClick = {
                            scope.launch{
                                image.delete()
                                Settings.custom_background_name = "No Image Selected"
                                backgroundName = "No Image Selected"
                                darkText.value = !darkMode
                                imageExists = image.exists()
                                bitmap.value = null
                            }
                        }) {
                            Icon(imageVector = Icons.Outlined.Delete,contentDescription = "delete")
                        }
                    }

                }
            )

        }

        PreferenceGroup {
            SettingsToggle(label = "Bell", description = "Play bell sound", showSwitch = true, default = Settings.bell, sideEffect = {
                Settings.bell = it
            })

            SettingsToggle(label = "Vibrate", description = "Virtual keypad vibration", showSwitch = true, default = Settings.vibrate, sideEffect = {
                Settings.vibrate = it
            })
        }

        PreferenceGroup {
            SettingsToggle(
                label = "StatusBar",
                description = "Show statusbar",
                showSwitch = true,
                default = Settings.statusBar, sideEffect = {
                    Settings.statusBar = it
                    showStatusBar.value = it
                })

            SettingsToggle(
                label = "Horizontal StatusBar",
                description = "Show statusbar in horizontal mode",
                showSwitch = true,
                default = Settings.horizontal_statusBar, sideEffect = {
                    Settings.horizontal_statusBar = it
                    horizontal_statusBar.value = it
                })


            val sideEffect:(Boolean)-> Unit = {
                if (!it && showToolbar.value){
                    MaterialAlertDialogBuilder(context).apply {
                        setTitle("Attention")
                        setMessage("Turning off the toolbar may prevent the drawer from opening on some devices. If this happens, you'll need to clear the app data to fix it.");
                        setPositiveButton("OK"){_,_ ->
                            Settings.toolbar = it
                            showToolbar.value = it
                        }
                        setNegativeButton("Cancel",null)
                        show()
                    }
                }else{
                    Settings.toolbar = it
                    showToolbar.value = it
                }

            }


            PreferenceSwitch(checked = showToolbar.value,
                onCheckedChange = {
                    sideEffect.invoke(it)
                },
                label = "TitleBar",
                modifier = modifier,
                description = "Show titlebar",
                onClick = {
                    sideEffect.invoke(!showToolbar.value)
                })

            SettingsToggle(
                isEnabled = showToolbar.value,
                label = "Horizontal TitleBar",
                description = "Show ToolBar in horizontal mode",
                showSwitch = true,
                default = Settings.toolbar_in_horizontal, sideEffect = {
                    Settings.toolbar_in_horizontal = it
                    showHorizontalToolbar.value = it
                })
            SettingsToggle(
                label = "Virtual Keys",
                description = "Show virtual keys below terminal",
                showSwitch = true,
                default = Settings.virtualKeys, sideEffect = {
                    Settings.virtualKeys = it
                    showVirtualKeys.value = it
                })

            SettingsToggle(
                label = "Hide soft Keyboard",
                description = "Hide virtual keyboard if hardware keyboard is connected",
                showSwitch = true,
                default = Settings.hide_soft_keyboard_if_hwd, sideEffect = {
                    Settings.hide_soft_keyboard_if_hwd = it
                })

        }


    }


}
