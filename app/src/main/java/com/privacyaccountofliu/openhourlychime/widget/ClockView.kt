package com.privacyaccountofliu.openhourlychime.widget

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import java.time.LocalDateTime

class ClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.BLACK
    }

    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.RED
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 40f
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var bounds: Rect
    private var centerX = 0f
    private var centerY = 0f
    private var radius = 0f

    private var hourAngle = 0f
    private var minuteAngle = 0f
    private var secondAngle = 0f

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateTime()
            invalidate()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.post(updateRunnable)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(updateRunnable)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bounds = Rect(0, 0, w, h)
        centerX = w / 2f
        centerY = h / 2f
        radius = (w.coerceAtMost(h) / 2f * 0.8f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawCircle(centerX, centerY, radius, paint)

        for (i in 0 until 12) {
            val angle = Math.PI / 6 * i
            val startX = centerX + cos(angle) * (radius - 30)
            val startY = centerY + sin(angle) * (radius - 30)
            val stopX = centerX + cos(angle) * radius
            val stopY = centerY + sin(angle) * radius

            canvas.drawLine(startX.toFloat(), startY.toFloat(), stopX.toFloat(), stopY.toFloat(), paint)

            val textX = centerX + sin(angle) * (radius - 70)
            val textY = centerY + -cos(angle) * (radius - 70) + 15
            canvas.drawText(if (i == 0) "12" else i.toString(), textX.toFloat(), textY.toFloat(), textPaint)
        }
        drawHand(canvas, hourAngle, radius * 0.5f, Color.BLACK, 8f)
        drawHand(canvas, minuteAngle, radius * 0.7f, Color.BLUE, 6f)
        drawHand(canvas, secondAngle, radius * 0.9f, Color.RED, 3f)
        canvas.drawCircle(centerX, centerY, 12f, centerPaint)
    }

    private fun drawHand(canvas: Canvas, angle: Float, length: Float, color: Int, strokeWidth: Float) {
        paint.color = color
        paint.strokeWidth = strokeWidth

        val radian = Math.toRadians(angle.toDouble())
        val endX = centerX + cos(radian) * length
        val endY = centerY + sin(radian) * length

        canvas.drawLine(centerX, centerY, endX.toFloat(), endY.toFloat(), paint)
    }

    private fun updateTime() {
        val calendar = LocalDateTime.now()
        secondAngle = calendar.second / 60f * 360f - 90f
        minuteAngle = calendar.minute / 60f * 360f - 90f // 360/60=6
        hourAngle = calendar.hour % 12f / 12f * 360f - 90f + (minuteAngle + 90f) / 12f
    }
}