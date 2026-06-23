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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.rk.libcommons.isDarkMode
import com.rk.settings.Settings

private val LightColorScheme =
    lightColorScheme(
        primary = md_theme_light_primary,
        onPrimary = md_theme_light_onPrimary,
        primaryContainer = md_theme_light_primaryContainer,
        onPrimaryContainer = md_theme_light_onPrimaryContainer,
        secondary = md_theme_light_secondary,
        onSecondary = md_theme_light_onSecondary,
        secondaryContainer = md_theme_light_secondaryContainer,
        onSecondaryContainer = md_theme_light_onSecondaryContainer,
        tertiary = md_theme_light_tertiary,
        onTertiary = md_theme_light_onTertiary,
        tertiaryContainer = md_theme_light_tertiaryContainer,
        onTertiaryContainer = md_theme_light_onTertiaryContainer,
        error = md_theme_light_error,
        errorContainer = md_theme_light_errorContainer,
        onError = md_theme_light_onError,
        onErrorContainer = md_theme_light_onErrorContainer,
        background = md_theme_light_background,
        onBackground = md_theme_light_onBackground,
        surface = md_theme_light_surface,
        onSurface = md_theme_light_onSurface,
        surfaceVariant = md_theme_light_surfaceVariant,
        onSurfaceVariant = md_theme_light_onSurfaceVariant,
        outline = md_theme_light_outline,
        inverseOnSurface = md_theme_light_inverseOnSurface,
        inverseSurface = md_theme_light_inverseSurface,
        inversePrimary = md_theme_light_inversePrimary,
        surfaceTint = md_theme_light_surfaceTint,
        outlineVariant = md_theme_light_outlineVariant,
        scrim = md_theme_light_scrim,
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = md_theme_dark_primary,
        onPrimary = md_theme_dark_onPrimary,
        primaryContainer = md_theme_dark_primaryContainer,
        onPrimaryContainer = md_theme_dark_onPrimaryContainer,
        secondary = md_theme_dark_secondary,
        onSecondary = md_theme_dark_onSecondary,
        secondaryContainer = md_theme_dark_secondaryContainer,
        onSecondaryContainer = md_theme_dark_onSecondaryContainer,
        tertiary = md_theme_dark_tertiary,
        onTertiary = md_theme_dark_onTertiary,
        tertiaryContainer = md_theme_dark_tertiaryContainer,
        onTertiaryContainer = md_theme_dark_onTertiaryContainer,
        error = md_theme_dark_error,
        errorContainer = md_theme_dark_errorContainer,
        onError = md_theme_dark_onError,
        onErrorContainer = md_theme_dark_onErrorContainer,
        background = md_theme_dark_background,
        onBackground = md_theme_dark_onBackground,
        surface = md_theme_dark_surface,
        onSurface = md_theme_dark_onSurface,
        surfaceVariant = md_theme_dark_surfaceVariant,
        onSurfaceVariant = md_theme_dark_onSurfaceVariant,
        outline = md_theme_dark_outline,
        inverseOnSurface = md_theme_dark_inverseOnSurface,
        inverseSurface = md_theme_dark_inverseSurface,
        inversePrimary = md_theme_dark_inversePrimary,
        surfaceTint = md_theme_dark_surfaceTint,
        outlineVariant = md_theme_dark_outlineVariant,
        scrim = md_theme_dark_scrim,
    )

private data class TerminalUiPalette(
    val isDark: Boolean,
    val background: Color,
    val foreground: Color,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val error: Color,
)

private fun terminalUiPalette(name: String): TerminalUiPalette? = when (name) {
    "Dracula" -> TerminalUiPalette(true, Color(0xFF282A36), Color(0xFFF8F8F2), Color(0xFFBD93F9), Color(0xFF8BE9FD), Color(0xFFFF79C6), Color(0xFFFF5555))
    "Nord" -> TerminalUiPalette(true, Color(0xFF2E3440), Color(0xFFD8DEE9), Color(0xFF81A1C1), Color(0xFF88C0D0), Color(0xFFB48EAD), Color(0xFFBF616A))
    "Solarized Dark" -> TerminalUiPalette(true, Color(0xFF002B36), Color(0xFF839496), Color(0xFF268BD2), Color(0xFF2AA198), Color(0xFFD33682), Color(0xFFDC322F))
    "Solarized Light" -> TerminalUiPalette(false, Color(0xFFFDF6E3), Color(0xFF657B83), Color(0xFF268BD2), Color(0xFF2AA198), Color(0xFFD33682), Color(0xFFDC322F))
    "Gruvbox Dark" -> TerminalUiPalette(true, Color(0xFF282828), Color(0xFFEBDBB2), Color(0xFF458588), Color(0xFF689D6A), Color(0xFFB16286), Color(0xFFCC241D))
    "Gruvbox Light" -> TerminalUiPalette(false, Color(0xFFFBF1C7), Color(0xFF3C3836), Color(0xFF076678), Color(0xFF427B58), Color(0xFF8F3F71), Color(0xFFCC241D))
    "One Dark" -> TerminalUiPalette(true, Color(0xFF282C34), Color(0xFFABB2BF), Color(0xFF61AFEF), Color(0xFF56B6C2), Color(0xFFC678DD), Color(0xFFE06C75))
    "Tokyo Night" -> TerminalUiPalette(true, Color(0xFF1A1B26), Color(0xFFA9B1D6), Color(0xFF7AA2F7), Color(0xFF7DCFFF), Color(0xFFBB9AF7), Color(0xFFF7768E))
    "Tokyo Night Light" -> TerminalUiPalette(false, Color(0xFFD5D6DB), Color(0xFF343B58), Color(0xFF34548A), Color(0xFF0F4B6E), Color(0xFF5A4A78), Color(0xFF8C4351))
    "Catppuccin Mocha" -> TerminalUiPalette(true, Color(0xFF1E1E2E), Color(0xFFCDD6F4), Color(0xFF89B4FA), Color(0xFF94E2D5), Color(0xFFF5C2E7), Color(0xFFF38BA8))
    "Catppuccin Latte" -> TerminalUiPalette(false, Color(0xFFEFF1F5), Color(0xFF4C4F69), Color(0xFF1E66F5), Color(0xFF179299), Color(0xFFEA76CB), Color(0xFFD20F39))
    "Monokai" -> TerminalUiPalette(true, Color(0xFF272822), Color(0xFFF8F8F2), Color(0xFF66D9EF), Color(0xFFA1EFE4), Color(0xFFAE81FF), Color(0xFFF92672))
    "Material Dark" -> TerminalUiPalette(true, Color(0xFF212121), Color(0xFFEEFFFF), Color(0xFF82AAFF), Color(0xFF89DDFF), Color(0xFFC792EA), Color(0xFFF07178))
    "Ayu Dark" -> TerminalUiPalette(true, Color(0xFF0A0E14), Color(0xFFB3B1AD), Color(0xFF36A3D9), Color(0xFF95E6CB), Color(0xFFF07178), Color(0xFFFF3333))
    "Ayu Light" -> TerminalUiPalette(false, Color(0xFFFAFAFA), Color(0xFF5C6166), Color(0xFF41A6D9), Color(0xFF4DBF99), Color(0xFFF07178), Color(0xFFFF3333))
    else -> null
}

private fun blendColors(base: Color, overlay: Color, amount: Float): Color {
    val keep = 1f - amount
    return Color(
        red = base.red * keep + overlay.red * amount,
        green = base.green * keep + overlay.green * amount,
        blue = base.blue * keep + overlay.blue * amount,
        alpha = 1f,
    )
}

private fun darkenColor(color: Color, amount: Float): Color = blendColors(color, Color.Black, amount)

private fun readableOn(color: Color): Color = if (color.luminance() > 0.5f) Color.Black else Color.White

private fun materialSchemeFromTerminal(palette: TerminalUiPalette): ColorScheme {
    val background = palette.background
    val foreground = palette.foreground
    val primary = palette.primary
    val secondary = palette.secondary
    val tertiary = palette.tertiary
    val error = palette.error

    return if (palette.isDark) {
        val surfaceVariant = blendColors(background, foreground, 0.08f)
        darkColorScheme(
            primary = primary,
            onPrimary = readableOn(primary),
            primaryContainer = blendColors(primary, background, 0.70f),
            onPrimaryContainer = blendColors(primary, Color.White, 0.30f),
            secondary = secondary,
            onSecondary = readableOn(secondary),
            secondaryContainer = blendColors(secondary, background, 0.70f),
            onSecondaryContainer = blendColors(secondary, Color.White, 0.30f),
            tertiary = tertiary,
            onTertiary = readableOn(tertiary),
            tertiaryContainer = blendColors(tertiary, background, 0.70f),
            onTertiaryContainer = blendColors(tertiary, Color.White, 0.30f),
            error = error,
            onError = readableOn(error),
            errorContainer = blendColors(error, background, 0.70f),
            onErrorContainer = blendColors(error, Color.White, 0.30f),
            background = background,
            onBackground = foreground,
            surface = background,
            onSurface = foreground,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = blendColors(foreground, background, 0.20f),
            outline = blendColors(foreground, background, 0.50f),
            outlineVariant = blendColors(foreground, background, 0.70f),
            inverseSurface = foreground,
            inverseOnSurface = background,
            inversePrimary = blendColors(primary, Color.Black, 0.30f),
            surfaceTint = primary,
            scrim = Color.Black,
        ).copy(
            surfaceContainerLowest = blendColors(background, foreground, 0.01f),
            surfaceContainerLow = blendColors(background, foreground, 0.03f),
            surfaceContainer = blendColors(background, foreground, 0.05f),
            surfaceContainerHigh = blendColors(background, foreground, 0.10f),
            surfaceContainerHighest = blendColors(background, foreground, 0.14f),
            surfaceBright = blendColors(background, foreground, 0.12f),
            surfaceDim = blendColors(background, Color.Black, 0.10f),
        )
    } else {
        val adjustedPrimary = darkenColor(primary, 0.10f)
        val adjustedSecondary = darkenColor(secondary, 0.10f)
        val adjustedTertiary = darkenColor(tertiary, 0.10f)
        val adjustedError = darkenColor(error, 0.10f)

        lightColorScheme(
            primary = adjustedPrimary,
            onPrimary = readableOn(adjustedPrimary),
            primaryContainer = blendColors(adjustedPrimary, background, 0.85f),
            onPrimaryContainer = darkenColor(adjustedPrimary, 0.30f),
            secondary = adjustedSecondary,
            onSecondary = readableOn(adjustedSecondary),
            secondaryContainer = blendColors(adjustedSecondary, background, 0.85f),
            onSecondaryContainer = darkenColor(adjustedSecondary, 0.30f),
            tertiary = adjustedTertiary,
            onTertiary = readableOn(adjustedTertiary),
            tertiaryContainer = blendColors(adjustedTertiary, background, 0.85f),
            onTertiaryContainer = darkenColor(adjustedTertiary, 0.30f),
            error = adjustedError,
            onError = readableOn(adjustedError),
            errorContainer = blendColors(adjustedError, background, 0.85f),
            onErrorContainer = darkenColor(adjustedError, 0.30f),
            background = background,
            onBackground = foreground,
            surface = background,
            onSurface = foreground,
            surfaceVariant = blendColors(background, foreground, 0.05f),
            onSurfaceVariant = blendColors(foreground, background, 0.30f),
            outline = blendColors(foreground, background, 0.50f),
            outlineVariant = blendColors(foreground, background, 0.30f),
            inverseSurface = foreground,
            inverseOnSurface = background,
            inversePrimary = blendColors(adjustedPrimary, Color.White, 0.30f),
            surfaceTint = adjustedPrimary,
            scrim = Color.Black,
        ).copy(
            surfaceContainerLowest = blendColors(background, Color.White, 0.50f),
            surfaceContainerLow = blendColors(background, foreground, 0.02f),
            surfaceContainer = blendColors(background, foreground, 0.03f),
            surfaceContainerHigh = blendColors(background, foreground, 0.08f),
            surfaceContainerHighest = blendColors(background, foreground, 0.10f),
            surfaceBright = blendColors(background, Color.White, 0.30f),
            surfaceDim = blendColors(background, foreground, 0.06f),
        )
    }
}

@Composable
fun KarbonTheme(
    darkTheme: Boolean = when (Settings.default_night_mode) {
        AppCompatDelegate.MODE_NIGHT_YES -> true
        AppCompatDelegate.MODE_NIGHT_NO -> false
        else -> isDarkMode(LocalContext.current)
    },
    highContrastDarkTheme: Boolean = Settings.amoled,
    dynamicColor: Boolean = Settings.monet,
    content: @Composable () -> Unit,
) {
    val terminalPalette = terminalUiPalette(Settings.terminal_theme)
    val colorScheme = when {
        terminalPalette != null -> materialSchemeFromTerminal(terminalPalette)
        dynamicColor && supportsDynamicTheming() -> {
            val context = LocalContext.current
            when {
                darkTheme && highContrastDarkTheme -> dynamicDarkColorScheme(context).copy(background = Color.Black, surface = Color.Black)
                darkTheme -> dynamicDarkColorScheme(context)
                else -> dynamicLightColorScheme(context)
            }
        }
        darkTheme && highContrastDarkTheme -> DarkColorScheme.copy(background = Color.Black, surface = Color.Black)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val useLightSystemBars = terminalPalette?.let { !it.isDark } ?: !darkTheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as Activity).apply {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = useLightSystemBars
                    isAppearanceLightNavigationBars = useLightSystemBars
                }
            }
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
fun supportsDynamicTheming() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
