package com.rk.terminal.ui.theme

import android.app.Activity
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.rk.libcommons.isDarkMode
import com.rk.settings.Settings

/*
 * More Themes
 */

private fun ColorScheme.toAmoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceBright = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color.Black,
    surfaceContainer = Color.Black,
    surfaceContainerHigh = Color.Black,
    surfaceContainerHighest = Color.Black,
)

@Composable
fun KarbonTheme(
    darkTheme: Boolean = when (Settings.default_night_mode) {
        AppCompatDelegate.MODE_NIGHT_YES -> true
        AppCompatDelegate.MODE_NIGHT_NO -> false
        else -> isDarkMode(LocalContext.current)
    },
    highContrastDarkTheme: Boolean = Settings.amoled,
    dynamicColor: Boolean = Settings.monet,
    themePalette: ThemePalette = Settings.theme_palette,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && supportsDynamicTheming() -> {
                val context = LocalContext.current
                when {
                    darkTheme && highContrastDarkTheme ->
                        dynamicDarkColorScheme(context).toAmoled()
                    darkTheme -> dynamicDarkColorScheme(context)
                    else -> dynamicLightColorScheme(context)
                }
            }

            darkTheme && highContrastDarkTheme ->
                darkColorSchemeFor(themePalette).toAmoled()
            darkTheme -> darkColorSchemeFor(themePalette)
            else -> lightColorSchemeFor(themePalette)
        }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as Activity).apply {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
fun supportsDynamicTheming() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
