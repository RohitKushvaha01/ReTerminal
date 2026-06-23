from pathlib import Path
import re

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

terminal = ROOT / 'core/main/src/main/java/com/rk/terminal/ui/screens/terminal/TerminalScreen.kt'
text = terminal.read_text()
if 'val savedColorsFile = localDir().child("colors.properties")' not in text:
    text = text.replace(
        'LaunchedEffect(Unit){\n        withContext(Dispatchers.IO){',
        '''LaunchedEffect(Unit){
        withContext(Dispatchers.IO){
            val savedColorsFile = localDir().child("colors.properties")
            if (savedColorsFile.exists() && savedColorsFile.canRead()) {
                runCatching {
                    FileInputStream(savedColorsFile).use { input ->
                        val props = Properties()
                        props.load(input)
                        TerminalColors.COLOR_SCHEME.updateWith(props)
                    }
                }
            }'''
    )

# Terminal text/cursor color must not be forced to white/black when a real terminal theme is selected.
text = text.replace(
    'darkText.value = !isDarkMode',
    'darkText.value = if (Settings.terminal_theme == "Default") !isDarkMode else Settings.blackTextColor'
)

# Patch terminalView.mEmulator first. The standalone mEmulator regex has a negative lookbehind so it
# cannot accidentally turn terminalView.mEmulator into terminalView.if (...).
text = re.sub(
    r'(?P<indent>\s*)terminalView\.mEmulator\?\.mColors\?\.mCurrentColors\?\.apply \{\n\s*set\(256, color\)\n\s*set\(258, color\)\n\s*\}',
    lambda m: f'{m.group("indent")}if (Settings.terminal_theme == "Default") {{\n'
              f'{m.group("indent")}    terminalView.mEmulator?.mColors?.mCurrentColors?.apply {{\n'
              f'{m.group("indent")}        set(256, color)\n'
              f'{m.group("indent")}        set(258, color)\n'
              f'{m.group("indent")}    }}\n'
              f'{m.group("indent")}}} else {{\n'
              f'{m.group("indent")}    terminalView.mEmulator?.mColors?.reset()\n'
              f'{m.group("indent")}}}',
    text
)

text = re.sub(
    r'(?P<indent>\s*)(?<!\.)mEmulator\?\.mColors\?\.mCurrentColors\?\.apply \{\n\s*set\(256, getViewColor\(\)\)\n\s*set\(258, getViewColor\(\)\)\n\s*\}',
    lambda m: f'{m.group("indent")}if (Settings.terminal_theme == "Default") {{\n'
              f'{m.group("indent")}    mEmulator?.mColors?.mCurrentColors?.apply {{\n'
              f'{m.group("indent")}        set(256, getViewColor())\n'
              f'{m.group("indent")}        set(258, getViewColor())\n'
              f'{m.group("indent")}    }}\n'
              f'{m.group("indent")}}} else {{\n'
              f'{m.group("indent")}    mEmulator?.mColors?.reset()\n'
              f'{m.group("indent")}}}',
    text
)

text = re.sub(
    r'(?P<indent>\s*)(?<!\.)mEmulator\?\.mColors\?\.mCurrentColors\?\.apply \{\n\s*set\(256, color\)\n\s*set\(258, color\)\n\s*\}',
    lambda m: f'{m.group("indent")}if (Settings.terminal_theme == "Default") {{\n'
              f'{m.group("indent")}    mEmulator?.mColors?.mCurrentColors?.apply {{\n'
              f'{m.group("indent")}        set(256, color)\n'
              f'{m.group("indent")}        set(258, color)\n'
              f'{m.group("indent")}    }}\n'
              f'{m.group("indent")}}} else {{\n'
              f'{m.group("indent")}    mEmulator?.mColors?.reset()\n'
              f'{m.group("indent")}}}',
    text
)

terminal.write_text(text)

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

THEME_FUNCTION = r'''private fun andLinuxThemeProperties(name: String): String = when (name) {
    "Dracula" -> """
color0=#21222c
color1=#ff5555
color2=#50fa7b
color3=#f1fa8c
color4=#bd93f9
color5=#ff79c6
color6=#8be9fd
color7=#f8f8f2
color8=#6272a4
color9=#ff6e6e
color10=#69ff94
color11=#ffffa5
color12=#d6acff
color13=#ff92df
color14=#a4ffff
color15=#ffffff
foreground=#f8f8f2
background=#282a36
cursor=#f8f8f2
""".trimIndent()
    "Nord" -> """
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
foreground=#d8dee9
background=#2e3440
cursor=#d8dee9
""".trimIndent()
    "Solarized Dark" -> """
color0=#073642
color1=#dc322f
color2=#859900
color3=#b58900
color4=#268bd2
color5=#d33682
color6=#2aa198
color7=#eee8d5
color8=#002b36
color9=#cb4b16
color10=#586e75
color11=#657b83
color12=#839496
color13=#6c71c4
color14=#93a1a1
color15=#fdf6e3
foreground=#839496
background=#002b36
cursor=#839496
""".trimIndent()
    "Solarized Light" -> """
color0=#073642
color1=#dc322f
color2=#859900
color3=#b58900
color4=#268bd2
color5=#d33682
color6=#2aa198
color7=#eee8d5
color8=#002b36
color9=#cb4b16
color10=#586e75
color11=#657b83
color12=#839496
color13=#6c71c4
color14=#93a1a1
color15=#fdf6e3
foreground=#657b83
background=#fdf6e3
cursor=#657b83
""".trimIndent()
    "Gruvbox Dark" -> """
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
foreground=#ebdbb2
background=#282828
cursor=#ebdbb2
""".trimIndent()
    "Gruvbox Light" -> """
color0=#fbf1c7
color1=#cc241d
color2=#98971a
color3=#d79921
color4=#458588
color5=#b16286
color6=#689d6a
color7=#7c6f64
color8=#928374
color9=#9d0006
color10=#79740e
color11=#b57614
color12=#076678
color13=#8f3f71
color14=#427b58
color15=#3c3836
foreground=#3c3836
background=#fbf1c7
cursor=#3c3836
""".trimIndent()
    "One Dark" -> """
color0=#282c34
color1=#e06c75
color2=#98c379
color3=#e5c07b
color4=#61afef
color5=#c678dd
color6=#56b6c2
color7=#abb2bf
color8=#5c6370
color9=#e06c75
color10=#98c379
color11=#e5c07b
color12=#61afef
color13=#c678dd
color14=#56b6c2
color15=#ffffff
foreground=#abb2bf
background=#282c34
cursor=#528bff
""".trimIndent()
    "Tokyo Night" -> """
color0=#15161e
color1=#f7768e
color2=#9ece6a
color3=#e0af68
color4=#7aa2f7
color5=#bb9af7
color6=#7dcfff
color7=#a9b1d6
color8=#414868
color9=#f7768e
color10=#9ece6a
color11=#e0af68
color12=#7aa2f7
color13=#bb9af7
color14=#7dcfff
color15=#c0caf5
foreground=#a9b1d6
background=#1a1b26
cursor=#c0caf5
""".trimIndent()
    "Tokyo Night Light" -> """
color0=#0f0f14
color1=#8c4351
color2=#485e30
color3=#8f5e15
color4=#34548a
color5=#5a4a78
color6=#0f4b6e
color7=#343b58
color8=#9699a3
color9=#8c4351
color10=#485e30
color11=#8f5e15
color12=#34548a
color13=#5a4a78
color14=#0f4b6e
color15=#343b58
foreground=#343b58
background=#d5d6db
cursor=#343b58
""".trimIndent()
    "Catppuccin Mocha" -> """
color0=#45475a
color1=#f38ba8
color2=#a6e3a1
color3=#f9e2af
color4=#89b4fa
color5=#f5c2e7
color6=#94e2d5
color7=#bac2de
color8=#585b70
color9=#f38ba8
color10=#a6e3a1
color11=#f9e2af
color12=#89b4fa
color13=#f5c2e7
color14=#94e2d5
color15=#a6adc8
foreground=#cdd6f4
background=#1e1e2e
cursor=#f5e0dc
""".trimIndent()
    "Catppuccin Latte" -> """
color0=#5c5f77
color1=#d20f39
color2=#40a02b
color3=#df8e1d
color4=#1e66f5
color5=#ea76cb
color6=#179299
color7=#acb0be
color8=#6c6f85
color9=#d20f39
color10=#40a02b
color11=#df8e1d
color12=#1e66f5
color13=#ea76cb
color14=#179299
color15=#bcc0cc
foreground=#4c4f69
background=#eff1f5
cursor=#dc8a78
""".trimIndent()
    "Monokai" -> """
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
foreground=#f8f8f2
background=#272822
cursor=#f8f8f2
""".trimIndent()
    "Material Dark" -> """
color0=#212121
color1=#f07178
color2=#c3e88d
color3=#ffcb6b
color4=#82aaff
color5=#c792ea
color6=#89ddff
color7=#eeffff
color8=#545454
color9=#f07178
color10=#c3e88d
color11=#ffcb6b
color12=#82aaff
color13=#c792ea
color14=#89ddff
color15=#ffffff
foreground=#eeffff
background=#212121
cursor=#ffcc00
""".trimIndent()
    "Ayu Dark" -> """
color0=#0a0e14
color1=#ff3333
color2=#b8cc52
color3=#e7c547
color4=#36a3d9
color5=#f07178
color6=#95e6cb
color7=#b3b1ad
color8=#626a73
color9=#ff6565
color10=#eafe84
color11=#fff779
color12=#68d5ff
color13=#ffa3aa
color14=#c7fffd
color15=#ffffff
foreground=#b3b1ad
background=#0a0e14
cursor=#e6b450
""".trimIndent()
    "Ayu Light" -> """
color0=#000000
color1=#ff3333
color2=#86b300
color3=#f29718
color4=#41a6d9
color5=#f07178
color6=#4dbf99
color7=#ffffff
color8=#323232
color9=#ff6565
color10=#b8e532
color11=#ffc94a
color12=#73d8ff
color13=#ffa3aa
color14=#7ff1cb
color15=#ffffff
foreground=#5c6166
background=#fafafa
cursor=#ff6a00
""".trimIndent()
    else -> ""
}

private fun andLinuxThemeIsLight(name: String): Boolean = name in setOf(
    "Solarized Light", "Gruvbox Light", "Tokyo Night Light", "Catppuccin Latte", "Ayu Light"
)
'''

if 'private fun andLinuxThemeProperties' not in text:
    text = text.replace('private const val max_text_size = 20f\n', 'private const val max_text_size = 20f\n\n' + THEME_FUNCTION + '\n')
if 'val themeNames = listOf(' not in text:
    marker = '        fun getFileNameFromUri(context: Context, uri: Uri): String? {'
    group = '''        PreferenceGroup {
            PreferenceTemplate(
                title = { Text("Color Scheme") },
                description = { Text("Terminal palettes based on Termix built-in schemes") }
            )

            var selectedTheme by remember { mutableStateOf(Settings.terminal_theme) }
            val themeNames = listOf(
                "Default",
                "Dracula",
                "Nord",
                "Solarized Dark",
                "Solarized Light",
                "Gruvbox Dark",
                "Gruvbox Light",
                "One Dark",
                "Tokyo Night",
                "Tokyo Night Light",
                "Catppuccin Mocha",
                "Catppuccin Latte",
                "Monokai",
                "Material Dark",
                "Ayu Dark",
                "Ayu Light"
            )
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
                val lightTheme = andLinuxThemeIsLight(themeName)
                Settings.blackTextColor = lightTheme
                darkText.value = lightTheme
                terminalView.get()?.apply {
                    mEmulator?.mColors?.reset()
                    onScreenUpdated()
                }
            }

            themeNames.forEach { themeName ->
                PreferenceTemplate(
                    modifier = Modifier.clickable { applyTheme(themeName) },
                    title = { Text(themeName) },
                    description = { Text(if (themeName == "Default") "Built-in terminal colors" else if (andLinuxThemeIsLight(themeName)) "Light terminal color preset" else "Dark terminal color preset") },
                    startWidget = { RadioButton(selected = selectedTheme == themeName, onClick = { applyTheme(themeName) }) }
                )
            }
        }

'''
    text = text.replace(marker, group + marker)
cust.write_text(text)
