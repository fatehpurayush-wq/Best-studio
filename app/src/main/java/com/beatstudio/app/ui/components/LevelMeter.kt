package com.beatstudio.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.beatstudio.app.ui.theme.NeonCyan
import com.beatstudio.app.ui.theme.PhonkCoral
import com.beatstudio.app.ui.theme.StudioAmber
import com.beatstudio.app.ui.theme.StudioEmerald
import com.beatstudio.app.ui.theme.StudioSurfaceHighlight

@Composable
fun VerticalLevelMeter(
    level: Float, // 0..1
    modifier: Modifier = Modifier
) {
    val clamped = level.coerceIn(0f, 1f)
    Canvas(modifier = modifier.width(10.dp).height(76.dp)) {
        drawRoundRect(
            color = StudioSurfaceHighlight,
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
        val fillH = size.height * clamped
        if (fillH > 1f) {
            val brush = Brush.verticalGradient(
                colors = listOf(PhonkCoral, StudioAmber, StudioEmerald),
                startY = 0f,
                endY = size.height
            )
            drawRoundRect(
                brush = brush,
                topLeft = Offset(0f, size.height - fillH),
                size = Size(size.width, fillH),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }
    }
}

@Composable
fun StereoLevelMeter(
    leftLevel: Float,
    rightLevel: Float,
    modifier: Modifier = Modifier
) {
    val l = leftLevel.coerceIn(0f, 1f)
    val r = rightLevel.coerceIn(0f, 1f)
    Canvas(modifier = modifier.width(96.dp).height(14.dp)) {
        val barH = (size.height - 3.dp.toPx()) / 2f
        val brush = Brush.horizontalGradient(
            colors = listOf(NeonCyan, StudioEmerald, StudioAmber, PhonkCoral)
        )
        // Left bar
        drawRoundRect(
            color = StudioSurfaceHighlight,
            topLeft = Offset.Zero,
            size = Size(size.width, barH),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )
        if (l > 0.01f) {
            drawRoundRect(
                brush = brush,
                topLeft = Offset.Zero,
                size = Size(size.width * l, barH),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )
        }
        // Right bar
        val y2 = barH + 3.dp.toPx()
        drawRoundRect(
            color = StudioSurfaceHighlight,
            topLeft = Offset(0f, y2),
            size = Size(size.width, barH),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )
        if (r > 0.01f) {
            drawRoundRect(
                brush = brush,
                topLeft = Offset(0f, y2),
                size = Size(size.width * r, barH),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )
        }
    }
}
