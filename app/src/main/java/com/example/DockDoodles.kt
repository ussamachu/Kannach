package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser

enum class DockDoodle {
    PAGE,
    GARDEN,
    DRAW
}

private class ParsedDockDoodle(
    val spotPath: Path,
    val linePaths: List<Path>
)

private fun parseSvgPath(d: String): Path =
    PathParser().parsePathString(d).toPath()

@Composable
fun DockDoodleIcon(
    doodle: DockDoodle,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalKannachPalette.current

    val parsed = remember(doodle) {
        when (doodle) {
            DockDoodle.PAGE -> {
                val leftPage = parseSvgPath("M6 12 C14 9 20 10 24 14 L24 38 C20 34 14 33 6 36 Z")
                val rightPage = parseSvgPath("M42 12 C34 9 28 10 24 14 L24 38 C28 34 34 33 42 36 Z")
                val line1 = parseSvgPath("M29 19 C33 18 36 18 38 19")
                val line2 = parseSvgPath("M29 24 C33 23 36 23 38 24")
                val line3 = parseSvgPath("M29 29 C32 28 35 28 37 29")
                ParsedDockDoodle(
                    spotPath = leftPage,
                    linePaths = listOf(leftPage, rightPage, line1, line2, line3)
                )
            }
            DockDoodle.GARDEN -> {
                val stem = parseSvgPath("M24 41 C24 33 23 27 24 20")
                val leftLeaf = parseSvgPath("M24 29 C17 29 13 25 13 18 C20 18 24 22 24 29 Z")
                val rightLeaf = parseSvgPath("M24 25 C24 18 29 13 36 13 C36 20 31 25 24 25 Z")
                val ground = parseSvgPath("M12 41 C20 39 30 42 38 40")
                ParsedDockDoodle(
                    spotPath = rightLeaf,
                    linePaths = listOf(stem, leftLeaf, rightLeaf, ground)
                )
            }
            DockDoodle.DRAW -> {
                val body = parseSvgPath("M10 39 L13 30 L33 10 C35 8 38 8 39 10 C41 12 40 14 38 16 L18 36 Z")
                val tipLine = parseSvgPath("M13 30 L18 36")
                val scribble = parseSvgPath("M24 42 C28 40 32 43 40 41")
                ParsedDockDoodle(
                    spotPath = body,
                    linePaths = listOf(body, tipLine, scribble)
                )
            }
        }
    }

    val baseSpotColor = when (doodle) {
        DockDoodle.PAGE -> palette.orange
        DockDoodle.GARDEN -> palette.green
        DockDoodle.DRAW -> palette.red
    }
    val spotAlpha = if (selected) 0.75f else 0.35f
    val spotColor = baseSpotColor.copy(alpha = spotAlpha)
    val inkColor = palette.ink

    Canvas(modifier = modifier) {
        val scale = size.minDimension / 48f
        val offsetX = (size.width - 48f * scale) / 2f
        val offsetY = (size.height - 48f * scale) / 2f

        withTransform({
            translate(left = offsetX, top = offsetY)
            scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
        }) {
            withTransform({
                translate(left = 2.5f, top = 2.5f)
            }) {
                drawPath(
                    path = parsed.spotPath,
                    color = spotColor,
                    style = Fill
                )
            }

            val lineStroke = Stroke(
                width = 2.4f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
            for (path in parsed.linePaths) {
                drawPath(
                    path = path,
                    color = inkColor,
                    style = lineStroke
                )
            }
        }
    }
}
