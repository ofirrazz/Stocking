package com.stocksocial.ui.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import com.stocksocial.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PriceLineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val lineColor = context.getColor(R.color.primary_gold)
    private val gridColor = context.getColor(R.color.border)
    private val labelColor = context.getColor(R.color.text_muted)

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = lineColor
        strokeWidth = 4f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = gridColor
        strokeWidth = 1f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(8f, 8f), 0f)
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = lineColor
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = labelColor
        textSize = 22f
    }
    private val linePath = Path()
    private val fillPath = Path()
    private var timedPoints: List<Pair<Long, Double>> = emptyList()
    private val dateFmt = SimpleDateFormat("d/M", Locale.getDefault())

    fun setTimedPoints(newPoints: List<Pair<Long, Double>>) {
        timedPoints = newPoints
        invalidate()
    }

    fun setPoints(newPoints: List<Double>) {
        timedPoints = newPoints.mapIndexed { i, v -> i.toLong() to v }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val points = timedPoints.map { it.second }
        if (points.size < 2) return
        val w = width.toFloat()
        val h = height.toFloat()
        val min = points.minOrNull() ?: return
        val max = points.maxOrNull() ?: return
        val range = (max - min).takeIf { it > 0.0 } ?: 1.0
        val labelPad = 28f
        val left = paddingLeft.toFloat() + 36f
        val right = w - paddingRight - 8f
        val top = paddingTop.toFloat() + 8f
        val bottom = h - paddingBottom - labelPad

        for (i in 0..2) {
            val ratio = i / 2f
            val y = top + ratio * (bottom - top)
            val v = max - ratio * (max - min)
            canvas.drawLine(left, y, right, y, gridPaint)
            val label = String.format(Locale.US, "%.1f", v)
            canvas.drawText(label, paddingLeft.toFloat(), y + 6f, textPaint)
        }

        linePath.reset()
        fillPath.reset()

        var lastX = 0f
        var lastY = 0f
        points.forEachIndexed { index, value ->
            val x = left + (right - left) * (index / (points.size - 1f))
            val normalized = ((value - min) / range).toFloat()
            val y = bottom - normalized * (bottom - top)
            if (index == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, bottom)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            lastX = x
            lastY = y
        }

        fillPath.lineTo(lastX, bottom)
        fillPath.close()

        fillPaint.shader = LinearGradient(
            0f, top, 0f, bottom,
            Color.argb(55, 201, 177, 122),
            Color.argb(0, 201, 177, 122),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(linePath, linePaint)

        if (points.isNotEmpty()) {
            canvas.drawCircle(lastX, lastY, 6f, dotPaint)
        }

        val times = timedPoints.map { it.first }
        if (times.size >= 2) {
            val idxs = listOf(0, times.size / 2, times.size - 1).distinct().sorted()
            idxs.forEach { idx ->
                val x = left + (right - left) * (idx / (points.size - 1f))
                val t = times[idx] * 1000L
                val lbl = try {
                    dateFmt.format(Date(t))
                } catch (_: Exception) {
                    ""
                }
                canvas.drawText(lbl, x - textPaint.measureText(lbl) / 2f, h - 8f, textPaint)
            }
        }
    }
}
