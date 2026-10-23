package com.example

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import kotlin.math.PI
import kotlin.math.sin

enum class DockDoodle {
    PAGE,
    GARDEN,
    DRAW
}

private sealed class ParsedDockDoodle {
    class Page(
        val leftPage: Path,
        val rightPage: Path,
        val lines: List<Path>
    ) : ParsedDockDoodle()

    class Garden(
        val stem: Path,
        val leftLeaf: Path,
        val rightLeaf: Path,
        val ground: Path
    ) : ParsedDockDoodle()

    class Draw(
        val body: Path,
        val tipLine: Path,
        val scribble: Path
    ) : ParsedDockDoodle()
}

private fun parseSvgPath(d: String): Path =
    PathParser().parsePathString(d).toPath()

@Composable
fun DockDoodleIcon(
    doodle: DockDoodle,
    selected: Boolean,
    modifier: Modifier = Modifier,
    playKey: Int = 0
) {
    val palette = LocalKannachPalette.current
    val reduceMotion = rememberReduceMotion()
    val animProgress = remember { Animatable(1f) }
    var lastPlayKey by remember { mutableIntStateOf(playKey) }

    LaunchedEffect(playKey) {
        if (playKey != lastPlayKey) {
            lastPlayKey = playKey
            if (reduceMotion) {
                animProgress.snapTo(1f)
            } else {
                animProgress.snapTo(0f)
                animProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 420, easing = KannachMotion.Settle)
                )
            }
        }
    }

    val parsed = remember(doodle) {
        when (doodle) {
            DockDoodle.PAGE -> {
                val leftPage = parseSvgPath("M6 12 C14 9 20 10 24 14 L24 38 C20 34 14 33 6 36 Z")
                val rightPage = parseSvgPath("M42 12 C34 9 28 10 24 14 L24 38 C28 34 34 33 42 36 Z")
                val line1 = parseSvgPath("M29 19 C33 18 36 18 38 19")
                val line2 = parseSvgPath("M29 24 C33 23 36 23 38 24")
                val line3 = parseSvgPath("M29 29 C32 28 35 28 37 29")
                ParsedDockDoodle.Page(
                    leftPage = leftPage,
                    rightPage = rightPage,
                    lines = listOf(line1, line2, line3)
                )
            }
            DockDoodle.GARDEN -> {
                val stem = parseSvgPath("M24 41 C24 33 23 27 24 20")
                val leftLeaf = parseSvgPath("M24 29 C17 29 13 25 13 18 C20 18 24 22 24 29 Z")
                val rightLeaf = parseSvgPath("M24 25 C24 18 29 13 36 13 C36 20 31 25 24 25 Z")
                val ground = parseSvgPath("M12 41 C20 39 30 42 38 40")
                ParsedDockDoodle.Garden(
                    stem = stem,
                    leftLeaf = leftLeaf,
                    rightLeaf = rightLeaf,
                    ground = ground
                )
            }
            DockDoodle.DRAW -> {
                val body = parseSvgPath("M10 39 L13 30 L33 10 C35 8 38 8 39 10 C41 12 40 14 38 16 L18 36 Z")
                val tipLine = parseSvgPath("M13 30 L18 36")
                val scribble = parseSvgPath("M24 42 C28 40 32 43 40 41")
                ParsedDockDoodle.Draw(
                    body = body,
                    tipLine = tipLine,
                    scribble = scribble
                )
            }
        }
    }

    val pathMeasure = remember { PathMeasure() }
    val segmentPath = remember { Path() }
    val lineStroke = remember {
        Stroke(
            width = 2.4f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
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
        val progress = animProgress.value
        val scale = size.minDimension / 48f
        val offsetX = (size.width - 48f * scale) / 2f
        val offsetY = (size.height - 48f * scale) / 2f

        withTransform({
            translate(left = offsetX, top = offsetY)
            scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
        }) {
            when (parsed) {
                is ParsedDockDoodle.Garden -> {
                    val pivot = Offset(24f, 41f)
                    val plantScale = 0.82f + 0.18f * progress + (0.08 * sin(progress * PI)).toFloat()
                    val swayDegrees = (sin(progress * 2.0 * PI) * 7.0).toFloat()

                    withTransform({
                        rotate(degrees = swayDegrees, pivot = pivot)
                        scale(scaleX = plantScale, scaleY = plantScale, pivot = pivot)
                    }) {
                        withTransform({
                            translate(left = 2.5f, top = 2.5f)
                        }) {
                            drawPath(
                                path = parsed.rightLeaf,
                                color = spotColor,
                                style = Fill
                            )
                        }
                        drawPath(path = parsed.stem, color = inkColor, style = lineStroke)
                        drawPath(path = parsed.leftLeaf, color = inkColor, style = lineStroke)
                        drawPath(path = parsed.rightLeaf, color = inkColor, style = lineStroke)
                    }
                    drawPath(path = parsed.ground, color = inkColor, style = lineStroke)
                }

                is ParsedDockDoodle.Page -> {
                    withTransform({
                        translate(left = 2.5f, top = 2.5f)
                    }) {
                        drawPath(
                            path = parsed.leftPage,
                            color = spotColor,
                            style = Fill
                        )
                    }
                    drawPath(path = parsed.leftPage, color = inkColor, style = lineStroke)

                    val flipDegrees = (-sin(progress * PI) * 10.0).toFloat()
                    val textEndFraction = (0.3f + 0.7f * progress).coerceIn(0f, 1f)

                    withTransform({
                        rotate(degrees = flipDegrees, pivot = Offset(24f, 24f))
                    }) {
                        drawPath(path = parsed.rightPage, color = inkColor, style = lineStroke)
                        for (line in parsed.lines) {
                            segmentPath.reset()
                            pathMeasure.setPath(line, false)
                            pathMeasure.getSegment(
                                0f,
                                pathMeasure.length * textEndFraction,
                                segmentPath,
                                true
                            )
                            drawPath(path = segmentPath, color = inkColor, style = lineStroke)
                        }
                    }
                }

                is ParsedDockDoodle.Draw -> {
                    val shift = sin(progress * PI).toFloat() * 3f
                    withTransform({
                        translate(left = -shift, top = shift)
                    }) {
                        withTransform({
                            translate(left = 2.5f, top = 2.5f)
                        }) {
                            drawPath(
                                path = parsed.body,
                                color = spotColor,
                                style = Fill
                            )
                        }
                        drawPath(path = parsed.body, color = inkColor, style = lineStroke)
                        drawPath(path = parsed.tipLine, color = inkColor, style = lineStroke)
                    }

                    val scribbleFraction = progress.coerceIn(0f, 1f)
                    if (scribbleFraction > 0f) {
                        segmentPath.reset()
                        pathMeasure.setPath(parsed.scribble, false)
                        pathMeasure.getSegment(
                            0f,
                            pathMeasure.length * scribbleFraction,
                            segmentPath,
                            true
                        )
                        drawPath(path = segmentPath, color = inkColor, style = lineStroke)
                    }
                }
            }
        }
    }
}
