package com.rk.terminal.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Pre-defined accent color palettes from Droidspaces-OSS.
 *
 * Each palette defines primary, secondary, and tertiary colors for both
 * light and dark modes.
 */
enum class ThemePalette(
    val displayName: String,
    val primaryLight: Color,
    val secondaryLight: Color,
    val tertiaryLight: Color,
    val primaryDark: Color,
    val secondaryDark: Color,
    val tertiaryDark: Color
) {
    CATPPUCCIN(
        displayName = "Catppuccin",
        primaryLight = Color(0xFF8AADF4),   // Blue
        secondaryLight = Color(0xFFB7BDF8), // Lavender
        tertiaryLight = Color(0xFFA6DA95),  // Green
        primaryDark = Color(0xFF7DC4E4),    // Sky
        secondaryDark = Color(0xFFF5BDE6),  // Pink
        tertiaryDark = Color(0xFFA6DA95)    // Green
    ),
    OCEAN(
        displayName = "Ocean",
        primaryLight = Color(0xFF0277BD),   // Deep Blue
        secondaryLight = Color(0xFF00ACC1), // Cyan
        tertiaryLight = Color(0xFF26A69A),  // Teal
        primaryDark = Color(0xFF4FC3F7),    // Light Blue
        secondaryDark = Color(0xFF4DD0E1),  // Light Cyan
        tertiaryDark = Color(0xFF80CBC4)    // Light Teal
    ),
    FOREST(
        displayName = "Forest",
        primaryLight = Color(0xFF2E7D32),   // Deep Green
        secondaryLight = Color(0xFF558B2F), // Olive Green
        tertiaryLight = Color(0xFF8D6E63),  // Brown
        primaryDark = Color(0xFF81C784),    // Light Green
        secondaryDark = Color(0xFFA5D6A7),  // Pale Green
        tertiaryDark = Color(0xFFBCAAA4)    // Light Brown
    ),
    SUNSET(
        displayName = "Sunset",
        primaryLight = Color(0xFFD84315),   // Deep Orange
        secondaryLight = Color(0xFFF4511E), // Orange-Red
        tertiaryLight = Color(0xFFFFB300),  // Amber
        primaryDark = Color(0xFFFF8A65),    // Light Orange
        secondaryDark = Color(0xFFFF8A80),  // Light Coral
        tertiaryDark = Color(0xFFFFD54F)    // Light Amber
    ),
    AMETHYST(
        displayName = "Amethyst",
        primaryLight = Color(0xFF6A1B9A),   // Deep Purple
        secondaryLight = Color(0xFF8E24AA), // Purple
        tertiaryLight = Color(0xFFAD1457),  // Deep Pink
        primaryDark = Color(0xFFCE93D8),    // Light Purple
        secondaryDark = Color(0xFFBA68C8),  // Medium Purple
        tertiaryDark = Color(0xFFF48FB1)    // Light Pink
    ),
    SAKURA(
        displayName = "Sakura",
        primaryLight = Color(0xFFD81B60),   // Pink
        secondaryLight = Color(0xFFEC407A), // Rose
        tertiaryLight = Color(0xFF7E57C2),  // Violet
        primaryDark = Color(0xFFF48FB1),    // Light Pink
        secondaryDark = Color(0xFFF8BBD0),  // Pale Pink
        tertiaryDark = Color(0xFFB39DDB)    // Light Violet
    );

    companion object {
        fun fromName(name: String): ThemePalette =
            entries.find { it.name.equals(name, ignoreCase = true) } ?: CATPPUCCIN
    }
}

/**
 * Blend two colors by the given ratio.
 * ratio=0 -> returns this, ratio=1 -> returns other.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun Color.blend(other: Color, ratio: Float): Color {
    val inv = 1f - ratio
    return Color(
        red = red * inv + other.red * ratio,
        green = green * inv + other.green * ratio,
        blue = blue * inv + other.blue * ratio,
        alpha = 1f
    )
}

/**
 * Create a complete dark color scheme from the given [ThemePalette].
 */
fun darkColorSchemeFor(palette: ThemePalette): ColorScheme {
    val p = palette.primaryDark
    val s = palette.secondaryDark
    val t = palette.tertiaryDark
    val base = Color(0xFF121212)

    return darkColorScheme(
        primary = p,
        onPrimary = Color(0xFF000000).blend(p, 0.08f),
        primaryContainer = p.blend(Color.Black, 0.40f),
        onPrimaryContainer = p.blend(Color.White, 0.75f),

        secondary = s,
        onSecondary = Color(0xFF000000).blend(s, 0.08f),
        secondaryContainer = s.blend(Color.Black, 0.40f),
        onSecondaryContainer = s.blend(Color.White, 0.75f),

        tertiary = t,
        onTertiary = Color(0xFF000000).blend(t, 0.08f),
        tertiaryContainer = t.blend(Color.Black, 0.40f),
        onTertiaryContainer = t.blend(Color.White, 0.75f),

        background = base.blend(p, 0.15f),
        onBackground = Color(0xFFE2E2E6),
        surface = base.blend(p, 0.15f),
        onSurface = Color(0xFFE2E2E6),
        surfaceVariant = Color(0xFF2B2B2F).blend(p, 0.25f),
        onSurfaceVariant = Color(0xFFC6C6CA),

        surfaceContainer = Color(0xFF1E1E22).blend(p, 0.20f),
        surfaceContainerHigh = Color(0xFF282830).blend(p, 0.22f),
        surfaceContainerHighest = Color(0xFF333338).blend(p, 0.25f),
        surfaceContainerLow = Color(0xFF1A1A1E).blend(p, 0.18f),
        surfaceContainerLowest = Color(0xFF0F0F13).blend(p, 0.15f),

        outline = Color(0xFF8E8E93).blend(p, 0.35f),
        outlineVariant = Color(0xFF46464A).blend(p, 0.30f),
        inverseSurface = Color(0xFFE2E2E6),
        inverseOnSurface = Color(0xFF303034),
        inversePrimary = palette.primaryLight
    )
}

/**
 * Create a complete light color scheme from the given [ThemePalette].
 */
fun lightColorSchemeFor(palette: ThemePalette): ColorScheme {
    val p = palette.primaryLight
    val s = palette.secondaryLight
    val t = palette.tertiaryLight
    val base = Color(0xFFFFFBFF)

    return lightColorScheme(
        primary = p,
        onPrimary = Color.White,
        primaryContainer = p.blend(Color.White, 0.55f),
        onPrimaryContainer = p.blend(Color.Black, 0.55f),

        secondary = s,
        onSecondary = Color.White,
        secondaryContainer = s.blend(Color.White, 0.55f),
        onSecondaryContainer = s.blend(Color.Black, 0.55f),

        tertiary = t,
        onTertiary = Color.White,
        tertiaryContainer = t.blend(Color.White, 0.55f),
        onTertiaryContainer = t.blend(Color.Black, 0.55f),

        background = base.blend(p, 0.06f),
        onBackground = Color(0xFF1B1B1F),
        surface = base.blend(p, 0.06f),
        onSurface = Color(0xFF1B1B1F),
        surfaceVariant = Color(0xFFE4E1E6).blend(p, 0.15f),
        onSurfaceVariant = Color(0xFF46464A),

        surfaceContainer = Color(0xFFF0EDF1).blend(p, 0.10f),
        surfaceContainerHigh = Color(0xFFEAE7EB).blend(p, 0.12f),
        surfaceContainerHighest = Color(0xFFE4E1E6).blend(p, 0.14f),
        surfaceContainerLow = Color(0xFFF6F3F7).blend(p, 0.08f),
        surfaceContainerLowest = base.blend(p, 0.04f),

        outline = Color(0xFF767680).blend(p, 0.22f),
        outlineVariant = Color(0xFFC6C6CA).blend(p, 0.18f),
        inverseSurface = Color(0xFF303034),
        inverseOnSurface = Color(0xFFF2F0F4),
        inversePrimary = palette.primaryDark
    )
}
