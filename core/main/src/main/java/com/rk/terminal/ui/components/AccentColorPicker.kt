package com.rk.terminal.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rk.resources.strings
import com.rk.terminal.ui.theme.ThemePalette

/**
 * A horizontally scrollable row of [ColorPaletteSwatch] items,
 * mimicking Android's accent color picker when Dynamic Colors is disabled.
 */
@Composable
fun AccentColorPicker(
    selectedPalette: ThemePalette,
    isDarkTheme: Boolean,
    onPaletteSelected: (ThemePalette) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null
                )
            },
            headlineContent = {
                Text(
                    text = stringResource(strings.accent_color),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            },
            supportingContent = {
                Text(
                    text = selectedPalette.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, top = 4.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(
                items = ThemePalette.entries.toList(),
                key = { it.name }
            ) { palette ->
                ColorPaletteSwatch(
                    palette = palette,
                    selected = palette == selectedPalette,
                    isDarkTheme = isDarkTheme,
                    onClick = { onPaletteSelected(palette) }
                )
            }
        }
    }
}

/**
 * A single circular color-palette swatch for the accent color picker.
 *
 * Visually splits the circle into 3 geometric sections:
 *  - Top half -> primary color
 *  - Bottom-left quarter -> secondary color
 *  - Bottom-right quarter -> tertiary color
 */
@Composable
fun ColorPaletteSwatch(
    palette: ThemePalette,
    selected: Boolean,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    swatchSize: Float = 52f
) {
    val primary = if (isDarkTheme) palette.primaryDark else palette.primaryLight
    val secondary = if (isDarkTheme) palette.secondaryDark else palette.secondaryLight
    val tertiary = if (isDarkTheme) palette.tertiaryDark else palette.tertiaryLight

    val selectionProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "selectionAnimation"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Canvas(
        modifier = modifier
            .size(swatchSize.dp)
            .semantics { contentDescription = palette.displayName }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = (swatchSize / 2 + 4).dp),
                onClick = onClick
            )
    ) {
        val canvasSize = size.minDimension
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = canvasSize / 2f

        val ringPadding = 4.dp.toPx()
        val outerRadius = radius + ringPadding
        if (selectionProgress > 0f) {
            drawCircle(
                color = primary.copy(alpha = selectionProgress * 0.8f),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 3.dp.toPx() * selectionProgress)
            )
        }

        val arcRect = Size(canvasSize, canvasSize)
        val arcTopLeft = Offset(
            (size.width - canvasSize) / 2f,
            (size.height - canvasSize) / 2f
        )

        // Top half: primary
        drawArc(
            color = primary,
            startAngle = -180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = arcTopLeft,
            size = arcRect
        )

        // Bottom-left quarter: secondary
        drawArc(
            color = secondary,
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = arcTopLeft,
            size = arcRect
        )

        // Bottom-right quarter: tertiary
        drawArc(
            color = tertiary,
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = arcTopLeft,
            size = arcRect
        )

        // Checkmark
        if (selectionProgress > 0.01f) {
            drawCheckmark(center, radius * 0.35f, selectionProgress)
        }
    }
}

private fun DrawScope.drawCheckmark(
    center: Offset,
    checkSize: Float,
    progress: Float
) {
    val strokeWidth = 2.5.dp.toPx()
    val alpha = progress.coerceIn(0f, 1f)
    val scale = 0.5f + 0.5f * progress
    val scaledSize = checkSize * scale

    drawCircle(
        color = Color.Black.copy(alpha = 0.35f * alpha),
        radius = checkSize * 1.1f * scale,
        center = center
    )

    val checkColor = Color.White.copy(alpha = alpha)
    val startX = center.x - scaledSize * 0.5f
    val startY = center.y + scaledSize * 0.05f
    val midX = center.x - scaledSize * 0.1f
    val midY = center.y + scaledSize * 0.45f
    val endX = center.x + scaledSize * 0.6f
    val endY = center.y - scaledSize * 0.35f

    drawLine(
        color = checkColor,
        start = Offset(startX, startY),
        end = Offset(midX, midY),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    drawLine(
        color = checkColor,
        start = Offset(midX, midY),
        end = Offset(endX, endY),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
}
