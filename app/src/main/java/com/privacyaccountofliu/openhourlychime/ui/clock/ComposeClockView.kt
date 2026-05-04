package com.privacyaccountofliu.openhourlychime.ui.clock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import java.time.LocalDateTime

private val ClockBackground = Color(0xFF1A1A2E)
private val ClockOuterRing = Color(0xFFE8E8E8)
private val ClockInnerRing = Color(0xFF16213E)
private val TickColor = Color(0xFFBBBBBB)
private val HourHandColor = Color(0xFFE8E8E8)
private val MinuteHandColor = Color(0xFFDDDDDD)
private val SecondHandColor = Color(0xFFFF4444)
private val CenterDotColor = Color(0xFFFF4444)
private val NumberColor = Color(0xFFE8E8E8)

@Composable
fun ComposeClockView(modifier: Modifier = Modifier) {
    val currentTime = remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime.value = LocalDateTime.now()
            delay(200)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(16.dp)
    ) {
        val w = size.width
        val h = size.height
        val centerX = w / 2f
        val centerY = h / 2f
        val radius = (minOf(w, h) / 2f) * 0.88f

        // Background circle
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF2A2A4A), Color(0xFF1A1A2E)),
                center = Offset(centerX, centerY),
                radius = radius * 1.35f
            ),
            radius = radius * 1.1f,
            center = Offset(centerX, centerY)
        )

        // Outer decorative ring
        drawCircle(
            color = ClockOuterRing,
            radius = radius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
        )

        // Inner ring
        drawCircle(
            color = ClockOuterRing.copy(alpha = 0.3f),
            radius = radius - 12.dp.toPx(),
            center = Offset(centerX, centerY),
            style = Stroke(width = 1.dp.toPx())
        )

        val time = currentTime.value
        val hour = time.hour % 12
        val minute = time.minute
        val second = time.second

        // Draw hour tick marks
        for (i in 0 until 12) {
            val angleRad = Math.PI / 6 * i
            val sinA = sin(angleRad)
            val cosA = cos(angleRad)

            val isMainTick = i % 3 == 0

            val tickStart = if (isMainTick) radius - 26.dp.toPx() else radius - 18.dp.toPx()
            val tickEnd = radius - 8.dp.toPx()

            val innerX = centerX + sinA * tickStart
            val innerY = centerY - cosA * tickStart
            val outerX = centerX + sinA * tickEnd
            val outerY = centerY - cosA * tickEnd

            drawLine(
                color = if (isMainTick) ClockOuterRing else TickColor,
                start = Offset(innerX.toFloat(), innerY.toFloat()),
                end = Offset(outerX.toFloat(), outerY.toFloat()),
                strokeWidth = if (isMainTick) 4.dp.toPx() else 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Numbers (only at 12, 3, 6, 9)
            if (isMainTick) {
                val textRadius = radius - 46.dp.toPx()
                val textX = centerX + sinA * textRadius
                val textY = centerY - cosA * textRadius + 14.dp.toPx()
                drawContext.canvas.nativeCanvas.drawText(
                    when (i) {
                        0 -> "12"
                        3 -> "3"
                        6 -> "6"
                        9 -> "9"
                        else -> ""
                    },
                    textX.toFloat(),
                    textY.toFloat(),
                    android.graphics.Paint().apply {
                        textSize = 18.dp.toPx()
                        color = android.graphics.Color.parseColor("#E8E8E8")
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                        isFakeBoldText = true
                    }
                )
            }

            // Small minute dots
            if (!isMainTick) {
                val dotRadius = radius * 1.02f  // outside
                val dotX = centerX + sinA * dotRadius
                val dotY = centerY - cosA * dotRadius
                // Small ticks at non-main positions already drawn above
            }
        }

        // Calculate hand angles
        val secondAngle = second / 60f * 360f - 90f
        val minuteAngle = minute / 60f * 360f - 90f + second / 60f * 6f
        val hourAngle = hour / 12f * 360f - 90f + (minute / 60f) * 30f

        // Hour hand
        drawHand(centerX, centerY, radius * 0.45f, hourAngle, HourHandColor, 8.dp.toPx())
        // Minute hand
        drawHand(centerX, centerY, radius * 0.65f, minuteAngle, MinuteHandColor, 5.dp.toPx())
        // Second hand
        drawHand(centerX, centerY, radius * 0.78f, secondAngle, SecondHandColor, 2.dp.toPx())

        // Center decoration: outer ring + inner dot
        drawCircle(color = ClockOuterRing, radius = 8.dp.toPx(), center = Offset(centerX, centerY))
        drawCircle(color = CenterDotColor, radius = 5.dp.toPx(), center = Offset(centerX, centerY))

        // Second hand counterweight
        val counterRad = Math.toRadians(secondAngle + 180.0)
        val counterX = centerX + cos(counterRad).toFloat() * (radius * 0.15f)
        val counterY = centerY + sin(counterRad).toFloat() * (radius * 0.15f)
        drawCircle(color = SecondHandColor, radius = 4.dp.toPx(), center = Offset(counterX, counterY))
    }
}

private fun DrawScope.drawHand(
    centerX: Float, centerY: Float, length: Float,
    angleDeg: Float, color: Color, strokeWidth: Float
) {
    val rad = Math.toRadians(angleDeg.toDouble())
    val endX = centerX + cos(rad).toFloat() * length
    val endY = centerY + sin(rad).toFloat() * length
    drawLine(
        color = color,
        start = Offset(centerX, centerY),
        end = Offset(endX, endY),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
}
