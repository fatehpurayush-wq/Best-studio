package com.beatstudio.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun Knob(
    label: String,
    value: Float, // 0..1
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    bipolar: Boolean = false
) {
    var currentValue by remember(value) { mutableFloatStateOf(value.coerceIn(0f, 1f)) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val delta = -dragAmount.y / 160f
                        currentValue = (currentValue + delta).coerceIn(0f, 1f)
                        onValueChange(currentValue)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val trackBg = MaterialTheme.colorScheme.surfaceVariant
            val centerFill = MaterialTheme.colorScheme.surface
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 5.dp.toPx()
                val pad = strokeW / 2f + 2.dp.toPx()
                val arcSize = Size(this.size.width - pad * 2f, this.size.height - pad * 2f)
                val topLeft = Offset(pad, pad)

                // Background 270° arc
                drawArc(
                    color = trackBg,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )

                // Active arc
                if (bipolar) {
                    val midAngle = 270f
                    val sweep = (currentValue - 0.5f) * 270f
                    drawArc(
                        color = accentColor,
                        startAngle = midAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                } else {
                    drawArc(
                        color = accentColor,
                        startAngle = 135f,
                        sweepAngle = currentValue * 270f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }

                // Inner knob circle + pointer dot
                val cx = this.size.width / 2f
                val cy = this.size.height / 2f
                val innerR = (this.size.minDimension / 2f) - strokeW - 4.dp.toPx()
                drawCircle(color = centerFill, radius = innerR, center = Offset(cx, cy))

                val angleRad = ((135f + currentValue * 270f) * PI / 180.0).toFloat()
                val indStart = Offset(cx + cos(angleRad) * (innerR * 0.35f), cy + sin(angleRad) * (innerR * 0.35f))
                val indEnd = Offset(cx + cos(angleRad) * (innerR * 0.82f), cy + sin(angleRad) * (innerR * 0.82f))
                drawLine(
                    color = accentColor,
                    start = indStart,
                    end = indEnd,
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            Text(
                text = if (bipolar) {
                    val p = ((currentValue - 0.5f) * 200f).roundToInt()
                    if (p == 0) "C" else if (p > 0) "R$p" else "L${-p}"
                } else {
                    "${(currentValue * 100f).roundToInt()}"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
