package com.example

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private fun hash(seed: Int, i: Int): Int =
    abs((seed * 31 + i * 17) * 2654435761L.toInt()).let { if (it < 0) 0 else it }

private fun Path.quadTo(x1: Float, y1: Float, x2: Float, y2: Float) {
    quadraticBezierTo(x1, y1, x2, y2)
}

fun DrawScope.inkLine(from: Offset, to: Offset, seed: Int, color: Color, widthPx: Float, wobblePx: Float) {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val len = hypot(dx, dy)
    if (len < 0.001f) return

    val nx = -dy / len
    val ny = dx / len
    val segments = 6

    val controls = Array(segments) { i ->
        val tm = (i + 0.5f) / segments.toFloat()
        val perp = ((hash(seed, i) % 100) / 50f - 1f) * wobblePx
        Offset(
            x = from.x + dx * tm + nx * perp,
            y = from.y + dy * tm + ny * perp
        )
    }

    val path = Path().apply {
        moveTo(from.x, from.y)
        for (i in 0 until segments) {
            val ctrl = controls[i]
            val end = if (i < segments - 1) {
                val nextCtrl = controls[i + 1]
                Offset((ctrl.x + nextCtrl.x) * 0.5f, (ctrl.y + nextCtrl.y) * 0.5f)
            } else {
                to
            }
            quadTo(ctrl.x, ctrl.y, end.x, end.y)
        }
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = widthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

fun DrawScope.inkCircle(center: Offset, radiusPx: Float, seed: Int, color: Color, widthPx: Float, progress: Float = 1f) {
    if (radiusPx <= 0f || progress <= 0f) return

    val steps = 24
    val startAngle = ((hash(seed, 0) % 360) / 180f) * PI.toFloat()
    val totalSweep = (1.15f * 2f * PI).toFloat()
    val aspectSkew = ((hash(seed, 1) % 100) / 50f - 1f) * 0.04f
    val baseRx = radiusPx * (1f + aspectSkew)
    val baseRy = radiusPx * (1f - aspectSkew)

    val points = Array(steps + 1) { i ->
        val t = i.toFloat() / steps.toFloat()
        val angle = startAngle + totalSweep * t
        val drift = 1f + ((hash(seed, i + 2) % 100) / 50f - 1f) * 0.08f
        val rx = baseRx * drift
        val ry = baseRy * drift
        Offset(
            x = center.x + cos(angle) * rx,
            y = center.y + sin(angle) * ry
        )
    }

    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 0 until steps) {
            val ctrl = points[i]
            val next = points[i + 1]
            val end = if (i < steps - 1) {
                Offset((ctrl.x + next.x) * 0.5f, (ctrl.y + next.y) * 0.5f)
            } else {
                next
            }
            quadTo(ctrl.x, ctrl.y, end.x, end.y)
        }
    }

    val drawTarget = if (progress >= 0.999f) {
        path
    } else {
        val segment = Path()
        val measure = PathMeasure()
        measure.setPath(path, false)
        measure.getSegment(0f, measure.length * progress.coerceIn(0f, 1f), segment, true)
        segment
    }

    drawPath(
        path = drawTarget,
        color = color,
        style = Stroke(width = widthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}
