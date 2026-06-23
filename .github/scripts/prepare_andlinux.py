from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def patch(path, pairs):
    p = ROOT / path
    text = p.read_text()
    for old, new in pairs:
        text = text.replace(old, new)
    p.write_text(text)


patch('settings.gradle.kts', [('rootProject.name = "ReTerminal"', 'rootProject.name = "AndLinux"')])
patch('core/main/src/main/java/com/rk/terminal/ui/screens/terminal/TerminalScreen.kt', [('Text(text = "ReTerminal"', 'Text(text = "AndLinux"')])
patch('core/main/src/main/java/com/rk/AlpineDocumentProvider.kt', [('val applicationName = "ReTerminal"', 'val applicationName = "AndLinux"'), ('Log.w("Alpine",', 'Log.w("AndLinux",')])
patch('core/main/src/main/java/com/rk/terminal/service/SessionService.kt', [('.setContentTitle("ReTerminal")', '.setContentTitle("AndLinux")')])
patch('core/resources/src/main/res/values/strings.xml', [('ReTerminal Android shell', 'AndLinux Android shell')])
patch('core/resources/src/main/res/values-zh/strings.xml', [('ReTerminal Android Shell', 'AndLinux Android Shell')])

settings = ROOT / 'core/main/src/main/java/com/rk/settings/Settings.kt'
text = settings.read_text()
needle = '''    var default_shell
        get() = Preference.getString(key = "default_shell", default = "ash")
        set(value) = Preference.setString(key = "default_shell", value)

    var custom_background_name'''
if needle in text and 'var terminal_theme' not in text:
    text = text.replace(needle, '''    var default_shell
        get() = Preference.getString(key = "default_shell", default = "ash")
        set(value) = Preference.setString(key = "default_shell", value)

    var terminal_theme
        get() = Preference.getString(key = "terminal_theme", default = "Default")
        set(value) = Preference.setString(key = "terminal_theme", value)

    var custom_background_name''')
settings.write_text(text)

cust = ROOT / 'core/main/src/main/java/com/rk/terminal/ui/screens/customization/Customization.kt'
text = cust.read_text()
if 'import androidx.compose.material3.RadioButton' not in text:
    text = text.replace('import androidx.compose.material3.MaterialTheme\n', 'import androidx.compose.material3.MaterialTheme\nimport androidx.compose.material3.RadioButton\n')
if 'import com.rk.libcommons.localDir' not in text:
    text = text.replace('import com.rk.libcommons.createFileIfNot\n', 'import com.rk.libcommons.createFileIfNot\nimport com.rk.libcommons.localDir\n')
if 'import com.termux.terminal.TerminalColors' not in text:
    text = text.replace('import com.rk.terminal.ui.screens.terminal.ShortcutCaptureDialog\n', 'import com.rk.terminal.ui.screens.terminal.ShortcutCaptureDialog\nimport com.termux.terminal.TerminalColors\n')
if 'import java.util.Properties' not in text:
    text = text.replace('import java.io.File\n', 'import java.io.File\nimport java.util.Properties\n')
if 'private fun andLinuxThemeProperties' not in text:
    text = text.replace('private const val max_text_size = 20f\n', '''private const val max_text_size = 20f

private fun andLinuxThemeProperties(name: String): String = when (name) {
    "Dracula" -> """
foreground=#f8f8f2
background=#282a36
cursor=#f8f8f2
color0=#000000
color1=#ff5555
color2=#50fa7b
color3=#f1fa8c
color4=#bd93f9
color5=#ff79c6
color6=#8be9fd
color7=#bbbbbb
color8=#555555
color9=#ff5555
color10=#50fa7b
color11=#f1fa8c
color12=#caa9fa
color13=#ff92d0
color14=#9aedfe
color15=#ffffff
""".trimIndent()
    "Gruvbox" -> """
foreground=#ebdbb2
background=#282828
cursor=#ebdbb2
color0=#282828
color1=#cc241d
color2=#98971a
color3=#d79921
color4=#458588
color5=#b16286
color6=#689d6a
color7=#a89984
color8=#928374
color9=#fb4934
color10=#b8bb26
color11=#fabd2f
color12=#83a598
color13=#d3869b
color14=#8ec07c
color15=#ebdbb2
""".trimIndent()
    "Monokai" -> """
foreground=#f8f8f2
background=#272822
cursor=#f8f8f2
color0=#272822
color1=#f92672
color2=#a6e22e
color3=#f4bf75
color4=#66d9ef
color5=#ae81ff
color6=#a1efe4
color7=#f8f8f2
color8=#75715e
color9=#f92672
color10=#a6e22e
color11=#f4bf75
color12=#66d9ef
color13=#ae81ff
color14=#a1efe4
color15=#f9f8f5
""".trimIndent()
    "Nord" -> """
foreground=#d8dee9
background=#2e3440
cursor=#d8dee9
color0=#3b4252
color1=#bf616a
color2=#a3be8c
color3=#ebcb8b
color4=#81a1c1
color5=#b48ead
color6=#88c0d0
color7=#e5e9f0
color8=#4c566a
color9=#bf616a
color10=#a3be8c
color11=#ebcb8b
color12=#81a1c1
color13=#b48ead
color14=#8fbcbb
color15=#eceff4
""".trimIndent()
    else -> ""
}
''')
if 'val themeNames = listOf("Default", "Dracula", "Gruvbox", "Monokai", "Nord")' not in text:
    marker = '        fun getFileNameFromUri(context: Context, uri: Uri): String? {'
    group = '''        PreferenceGroup {
            var selectedTheme by remember { mutableStateOf(Settings.terminal_theme) }
            val themeNames = listOf("Default", "Dracula", "Gruvbox", "Monokai", "Nord")
            val colorsFile = localDir().child("colors.properties")

            fun applyTheme(themeName: String) {
                selectedTheme = themeName
                Settings.terminal_theme = themeName
                val data = andLinuxThemeProperties(themeName)
                if (data.isBlank()) {
                    colorsFile.delete()
                    TerminalColors.COLOR_SCHEME.updateWith(Properties())
                } else {
                    colorsFile.parentFile?.mkdirs()
                    colorsFile.writeText(data)
                    val props = Properties()
                    props.load(data.byteInputStream())
                    TerminalColors.COLOR_SCHEME.updateWith(props)
                }
                Settings.blackTextColor = false
                darkText.value = false
                terminalView.get()?.apply {
                    mEmulator?.mColors?.reset()
                    onScreenUpdated()
                }
            }

            themeNames.forEach { themeName ->
                PreferenceTemplate(
                    modifier = Modifier.clickable { applyTheme(themeName) },
                    title = { Text(themeName) },
                    description = { Text(if (themeName == "Default") "Built-in terminal colors" else "Terminal color preset") },
                    startWidget = { RadioButton(selected = selectedTheme == themeName, onClick = { applyTheme(themeName) }) }
                )
            }
        }

'''
    text = text.replace(marker, group + marker)
cust.write_text(text)
