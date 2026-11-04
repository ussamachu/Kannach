package com.example

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

enum class SceneRole {
    LINE,
    SPOT_GREEN,
    SPOT_ORANGE,
    SPOT_RED
}

class ScenePart(
    val d: String,
    val role: SceneRole
)

private class ParsedScenePart(
    val path: Path,
    val role: SceneRole,
    val center: Offset
)

@Composable
fun DrawnScene(
    parts: List<ScenePart>,
    active: Boolean,
    modifier: Modifier = Modifier,
    swayDegrees: Float = 0f,
    pulse: Float = 0f
) {
    val palette = LocalKannachPalette.current
    val reduceMotion = rememberReduceMotion()

    val parsedParts = remember(parts) {
        parts.map { part ->
            val path = PathParser().parsePathString(part.d).toPath()
            val bounds = path.getBounds()
            ParsedScenePart(
                path = path,
                role = part.role,
                center = bounds.center
            )
        }
    }

    val lineParts = remember(parsedParts) { parsedParts.filter { it.role == SceneRole.LINE } }
    val spotParts = remember(parsedParts) { parsedParts.filter { it.role != SceneRole.LINE } }
    val lineCount = lineParts.size

    var completedLines by remember { mutableIntStateOf(0) }
    var currentLineProgress by remember { mutableFloatStateOf(0f) }
    var spotsAlpha by remember { mutableFloatStateOf(0f) }
    var drawInDone by remember { mutableStateOf(false) }

    LaunchedEffect(active, reduceMotion, parsedParts) {
        if (!active) {
            completedLines = 0
            currentLineProgress = 0f
            spotsAlpha = 0f
            drawInDone = false
            return@LaunchedEffect
        }

        if (reduceMotion) {
            completedLines = lineCount
            currentLineProgress = 1f
            spotsAlpha = 0.55f
            drawInDone = true
            return@LaunchedEffect
        }

        completedLines = 0
        currentLineProgress = 0f
        spotsAlpha = 0f
        drawInDone = false

        val perLineMs = if (lineCount > 0) maxOf(180, 1300 / lineCount) else 180
        val lineAnim = Animatable(0f)
        for (i in 0 until lineCount) {
            lineAnim.snapTo(0f)
            lineAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = perLineMs, easing = FastOutSlowInEasing)
            ) {
                currentLineProgress = value
            }
            completedLines = i + 1
            currentLineProgress = 0f
        }

        val spotAnim = Animatable(0f)
        spotAnim.animateTo(
            targetValue = 0.55f,
            animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing)
        ) {
            spotsAlpha = value
        }
        drawInDone = true
    }

    val infiniteTransition = rememberInfiniteTransition(label = "DrawnSceneIdle")
    val swayPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "swayPhase"
    )
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulsePhase"
    )

    val pathMeasure = remember { PathMeasure() }
    val segmentPath = remember { Path() }

    Canvas(modifier = modifier.defaultMinSize(minWidth = 160.dp, minHeight = 160.dp)) {
        val scale = size.minDimension / 160f
        val offsetX = (size.width - 160f * scale) / 2f
        val offsetY = (size.height - 160f * scale) / 2f

        val currentSway = if (drawInDone && !reduceMotion && swayDegrees != 0f) {
            sin(swayPhase) * swayDegrees
        } else {
            0f
        }

        withTransform({
            translate(left = offsetX, top = offsetY)
            scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
            if (currentSway != 0f) {
                rotate(degrees = currentSway, pivot = Offset(80f, 140f))
            }
        }) {
            if (spotsAlpha > 0f) {
                for (spot in spotParts) {
                    val baseColor = when (spot.role) {
                        SceneRole.SPOT_GREEN -> palette.green
                        SceneRole.SPOT_ORANGE -> palette.orange
                        SceneRole.SPOT_RED -> palette.red
                        SceneRole.LINE -> palette.ink
                    }
                    val spotScale = if (drawInDone && !reduceMotion && pulse != 0f && spot.role == SceneRole.SPOT_RED) {
                        1f + pulse * sin(pulsePhase)
                    } else {
                        1f
                    }
                    withTransform({
                        translate(left = 4f, top = 4f)
                        if (spotScale != 1f) {
                            scale(scaleX = spotScale, scaleY = spotScale, pivot = spot.center)
                        }
                    }) {
                        drawPath(
                            path = spot.path,
                            color = baseColor.copy(alpha = spotsAlpha),
                            style = Fill
                        )
                    }
                }
            }

            val stroke = Stroke(
                width = 3.2f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )

            for (i in 0 until lineCount) {
                val linePart = lineParts[i]
                when {
                    i < completedLines -> {
                        drawPath(
                            path = linePart.path,
                            color = palette.ink,
                            style = stroke
                        )
                    }
                    i == completedLines && currentLineProgress > 0f -> {
                        segmentPath.reset()
                        pathMeasure.setPath(linePart.path, false)
                        val length = pathMeasure.length
                        if (length > 0f) {
                            pathMeasure.getSegment(0f, length * currentLineProgress, segmentPath, true)
                            drawPath(
                                path = segmentPath,
                                color = palette.ink,
                                style = stroke
                            )
                        }
                    }
                }
            }
        }
    }
}

val SceneWelcome: List<ScenePart> = listOf(
    ScenePart(
        d = "M 26 118 C 44 114 62 116 80 124 C 98 116 116 114 134 118 L 134 136 C 116 132 98 134 80 142 C 62 134 44 132 26 136 Z",
        role = SceneRole.SPOT_ORANGE
    ),
    ScenePart(
        d = "M 80 96 C 64 92 50 80 48 64 C 64 64 76 76 80 92 Z M 80 80 C 96 76 110 64 112 48 C 96 48 84 60 80 76 Z M 80 48 C 70 42 68 28 80 22 C 92 28 90 42 80 48 Z",
        role = SceneRole.SPOT_GREEN
    ),
    ScenePart(
        d = "M 26 118 C 44 114 62 116 80 124 L 80 142 C 62 134 44 132 26 136 Z",
        role = SceneRole.LINE
    ),
    ScenePart(
        d = "M 134 118 C 116 114 98 116 80 124 L 80 142 C 98 134 116 132 134 136 Z",
        role = SceneRole.LINE
    ),
    ScenePart(
        d = "M 20 138 C 46 136 64 138 80 145 C 96 138 114 136 140 138",
        role = SceneRole.LINE
    ),
    ScenePart(
        d = "M 80 124 C 79 102 81 78 80 48",
        role = SceneRole.LINE
    ),
    ScenePart(
        d = "M 80 96 C 64 92 50 80 48 64 C 64 64 76 76 80 92",
        role = SceneRole.LINE
    ),
    ScenePart(
        d = "M 80 94 C 68 86 58 76 52 67",
        role = SceneRole.LINE
    ),
    ScenePart(
        d = "M 80 80 C 96 76 110 64 112 48 C 96 48 84 60 80 76",
        role = SceneRole.LINE
    ),
    ScenePart(
        d = "M 80 78 C 92 70 102 60 108 51",
        role = SceneRole.LINE
    ),
    ScenePart(
        d = "M 80 48 C 70 42 68 28 80 22 C 92 28 90 42 80 48",
        role = SceneRole.LINE
    ),
    ScenePart(
        d = "M 36 125 C 50 123 64 125 73 129",
        role = SceneRole.LINE
    )
)

val ScenePrivate: List<ScenePart> = listOf(
    ScenePart(
        d = "M 38 26 L 114 26 C 120 26 124 30 124 36 L 124 128 C 124 134 120 138 114 138 L 38 138 Z",
        role = SceneRole.SPOT_ORANGE
    ),
    ScenePart(
        d = "M 82 94 C 72 86 64 78 64 70 C 64 64 69 60 75 61 C 78 62 80 65 82 68 C 84 65 86 62 89 61 C 95 60 100 64 100 70 C 100 78 92 86 82 94 Z",
        role = SceneRole.SPOT_RED
    ),
    ScenePart(
        d = "M 38 26 L 114 26 C 120 26 124 30 124 36 L 124 128 C 124 134 120 138 114 138 L 38 138 C 35 138 34 136 34 132 L 34 32 C 34 28 35 26 38 26 Z",
        role = SceneRole.LINE
    ),
    ScenePart(
        d = "M 46 27 L 46 137",
        role = SceneRole.LINE
    ),
    ScenePart(
        d = "M 40 142 L 118 142 C 124 142 128 138 128 132 L 128 34",
        role = SceneRole.LINE
    ),
    ScenePart(
        d = "M 82 94 C 72 86 64 78 64 70 C 64 64 69 60 75 61 C 78 62 80 65 82 68 C 84 65 86 62 89 61 C 95 60 100 64 100 70 C 100 78 92 86 82 94 Z",
        role = SceneRole.LINE
    ),
    ScenePart(
        d = "M 34 77 L 64 77",
        role = SceneRole.LINE
    ),
    ScenePart(
        d = "M 100 77 L 124 77",
        role = SceneRole.LINE
    )
)

val SceneLanguages: List<ScenePart> = listOf(
    ScenePart(d = "M 36 82 C 36 56 56 36 82 36 C 108 36 128 56 128 82 C 128 108 108 128 82 128 C 56 128 36 108 36 82 Z", role = SceneRole.SPOT_GREEN),
    ScenePart(d = "M 25 20 L 61 20 C 67 20 70 23 70 29 L 70 55 C 70 61 67 64 61 64 L 38 64 L 20 78 L 25 64 L 25 64 C 19 64 16 61 16 55 L 16 29 C 16 23 19 20 25 20 Z", role = SceneRole.SPOT_ORANGE),
    ScenePart(d = "M 101 100 L 137 100 C 143 100 146 103 146 109 L 146 135 C 146 141 143 144 137 144 L 137 144 L 142 158 L 124 144 L 101 144 C 95 144 92 141 92 135 L 92 109 C 92 103 95 100 101 100 Z", role = SceneRole.SPOT_RED),
    ScenePart(d = "M 128.0 82.0 L 128.2 84.4 L 128.3 86.9 L 128.1 89.3 L 127.8 91.7 M 84.4 128.5 L 82.0 128.4 L 79.6 128.0 L 77.2 127.6 L 74.9 127.0 L 72.6 126.4 L 70.3 125.7 L 68.1 124.9 L 65.8 124.1 L 63.6 123.3 L 61.4 122.4 L 59.2 121.4 L 57.1 120.4 L 54.9 119.2 L 52.9 118.0 L 50.9 116.6 L 49.0 115.0 L 47.2 113.3 L 45.6 111.5 L 44.1 109.5 L 42.8 107.5 L 41.7 105.3 L 40.7 103.0 L 39.9 100.7 L 39.2 98.4 L 38.7 96.1 L 38.2 93.7 L 37.8 91.4 L 37.4 89.1 L 37.1 86.7 L 36.9 84.4 L 36.7 82.0 L 36.5 79.6 L 36.5 77.2 L 36.5 74.8 L 36.7 72.4 M 77.1 35.4 L 79.5 35.2 L 82.0 35.1 L 84.4 35.3 L 86.9 35.7 L 89.2 36.2 L 91.6 36.9 L 93.9 37.8 L 96.1 38.7 L 98.2 39.7 L 100.4 40.7 L 102.5 41.8 L 104.6 42.9 L 106.6 44.1 L 108.7 45.3 L 110.7 46.6 L 112.7 48.0 L 114.6 49.4 L 116.4 51.0 L 118.2 52.7 L 119.8 54.5 L 121.3 56.5 L 122.6 58.6 L 123.7 60.7 L 124.7 63.0 L 125.5 65.3 L 126.0 67.7 L 126.5 70.1 L 126.8 72.5 L 127.0 74.9 L 127.1 77.3 L 127.1 79.6 L 127.1 82.0", role = SceneRole.LINE),
    ScenePart(d = "M 82.0 36.0 L 82.9 36.1 L 83.8 36.3 L 84.7 36.6 L 85.5 37.0 L 86.4 37.6 L 87.3 38.3 L 88.1 39.1 L 88.9 40.0 L 89.7 41.0 L 90.5 42.2 L 91.3 43.4 L 92.0 44.8 L 92.7 46.3 L 93.4 47.8 L 94.0 49.5 L 94.6 51.2 L 95.2 53.1 L 95.8 55.0 L 96.3 56.9 L 96.7 59.0 L 97.1 61.1 L 97.5 63.3 L 97.9 65.5 L 98.2 67.8 L 98.4 70.1 L 98.6 72.4 L 98.8 74.8 L 98.9 77.2 L 99.0 79.6 L 99.0 82.0 L 99.0 84.4 L 98.9 86.8 L 98.8 89.2 L 98.6 91.6 L 98.4 93.9 M 85.5 127.0 L 84.7 127.4 L 83.8 127.7 L 82.9 127.9 L 82.0 128.0", role = SceneRole.LINE),
    ScenePart(d = "M 82.0 36.0 L 81.1 36.1 L 80.2 36.3 L 79.3 36.6 L 78.5 37.0 L 77.6 37.6 L 76.7 38.3 M 65.6 70.1 L 65.4 72.4 L 65.2 74.8 L 65.1 77.2 L 65.0 79.6 L 65.0 82.0 L 65.0 84.4 L 65.1 86.8 L 65.2 89.2 L 65.4 91.6 L 65.6 93.9 L 65.8 96.2 L 66.1 98.5 L 66.5 100.7 L 66.9 102.9 L 67.3 105.0 L 67.7 107.1 L 68.2 109.0 L 68.8 110.9 L 69.4 112.8 L 70.0 114.5 L 70.6 116.2 L 71.3 117.7 L 72.0 119.2 L 72.7 120.6 L 73.5 121.8 L 74.3 123.0 L 75.1 124.0 L 75.9 124.9 L 76.7 125.7 L 77.6 126.4 L 78.5 127.0 L 79.3 127.4 L 80.2 127.7 L 81.1 127.9 L 82.0 128.0", role = SceneRole.LINE),
    ScenePart(d = "M 82.0 36.0 L 82.0 38.3 L 82.0 40.6 L 82.0 42.9 L 82.0 45.2 L 82.0 47.5 L 82.0 49.8 L 82.0 52.1 L 82.0 54.4 L 82.0 56.7 L 82.0 59.0 L 82.0 61.3 L 82.0 63.6 L 82.0 65.9 L 82.0 68.2 L 82.0 70.5 L 82.0 72.8 L 82.0 75.1 L 82.0 77.4 L 82.0 79.7 L 82.0 82.0 L 82.0 84.3 L 82.0 86.6 L 82.0 88.9 L 82.0 91.2 L 82.0 93.5 L 82.0 95.8 L 82.0 98.1 L 82.0 100.4 L 82.0 102.7 L 82.0 105.0 L 82.0 107.3 L 82.0 109.6 L 82.0 111.9 L 82.0 114.2 L 82.0 116.5 L 82.0 118.8 L 82.0 121.1 L 82.0 123.4 L 82.0 125.7 L 82.0 128.0", role = SceneRole.LINE),
    ScenePart(d = "M 36.0 82.0 L 37.5 82.2 L 39.1 82.4 L 40.6 82.6 L 42.1 82.8 L 43.7 83.0 L 45.2 83.2 L 46.7 83.4 L 48.3 83.6 L 49.8 83.8 L 51.3 84.0 L 52.9 84.2 L 54.4 84.4 L 55.9 84.5 L 57.5 84.7 L 59.0 84.8 L 60.5 85.0 L 62.1 85.1 L 63.6 85.2 L 65.1 85.4 L 66.7 85.5 L 68.2 85.6 L 69.7 85.7 L 71.3 85.7 L 72.8 85.8 L 74.3 85.9 L 75.9 85.9 L 77.4 86.0 L 78.9 86.0 L 80.5 86.0 L 82.0 86.0 L 83.5 86.0 L 85.1 86.0 L 86.6 86.0 L 88.1 85.9 L 89.7 85.9 L 91.2 85.8 L 92.7 85.7 L 94.3 85.7 L 95.8 85.6 L 97.3 85.5 L 98.9 85.4 L 100.4 85.2 L 101.9 85.1 L 103.5 85.0 L 105.0 84.8 L 106.5 84.7 L 108.1 84.5 L 109.6 84.4 L 111.1 84.2 L 112.7 84.0 L 114.2 83.8 L 115.7 83.6 L 117.3 83.4 L 118.8 83.2 L 120.3 83.0 L 121.9 82.8 L 123.4 82.6 L 124.9 82.4 L 126.5 82.2 L 128.0 82.0", role = SceneRole.LINE),
    ScenePart(d = "M 76.1 61.9 L 77.6 61.9 L 79.1 62.0 L 80.5 62.0 L 82.0 62.0 L 83.5 62.0 L 84.9 62.0 L 86.4 61.9 L 87.9 61.9 L 89.4 61.8 L 90.8 61.7 L 92.3 61.6 L 93.8 61.5 L 95.2 61.4 L 96.7 61.2 L 98.2 61.1 L 99.7 60.9 L 101.1 60.7 L 102.6 60.5 L 104.1 60.4 L 105.6 60.1 L 107.0 59.9 L 108.5 59.7 L 110.0 59.5 L 111.4 59.2 L 112.9 59.0 L 114.4 58.7 L 115.9 58.5 L 117.3 58.3 L 118.8 58.0", role = SceneRole.LINE),
    ScenePart(d = "M 45.2 106.0 L 46.7 106.3 L 48.1 106.5 L 49.6 106.7 L 51.1 107.0 L 52.6 107.2 L 54.0 107.5 L 55.5 107.7 L 57.0 107.9 L 58.4 108.1 L 59.9 108.4 L 61.4 108.5 L 62.9 108.7 L 64.3 108.9 L 65.8 109.1 L 67.3 109.2 L 68.8 109.4 L 70.2 109.5 L 71.7 109.6 L 73.2 109.7 L 74.6 109.8 L 76.1 109.9 L 77.6 109.9 L 79.1 110.0 L 80.5 110.0 L 82.0 110.0 L 83.5 110.0 L 84.9 110.0", role = SceneRole.LINE),
    ScenePart(d = "M 25 20 L 61 20 C 67 20 70 23 70 29 L 70 55 C 70 61 67 64 61 64 L 38 64 L 20 78 L 25 64 L 25 64 C 19 64 16 61 16 55 L 16 29 C 16 23 19 20 25 20 Z", role = SceneRole.LINE),
    ScenePart(d = "M 101 100 L 137 100 C 143 100 146 103 146 109 L 146 135 C 146 141 143 144 137 144 L 137 144 L 142 158 L 124 144 L 101 144 C 95 144 92 141 92 135 L 92 109 C 92 103 95 100 101 100 Z", role = SceneRole.LINE),
    ScenePart(d = "M 33 54 C 36 46 40 36 43 30 C 46 38 50 47 53 54 M 37 47 C 41 45.5 46 46 49 48", role = SceneRole.LINE),
    ScenePart(d = "M 104 114 C 104 124 108 130 116 130 C 126 130 134 128 136 116 M 118.5 137 C 118.5 134.5 122.5 134.5 122.5 137 C 122.5 139.5 118.5 139.5 118.5 137 Z", role = SceneRole.LINE)
)

@Composable
fun SceneDrawDay(
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalKannachPalette.current
    val reduceMotion = rememberReduceMotion()

    val trailPath = remember {
        PathParser().parsePathString("M30 120 C40 80 70 70 78 96 C86 120 52 124 56 100 C60 76 100 70 118 92 C130 106 126 124 120 130").toPath()
    }
    val spotPath = remember {
        PathParser().parsePathString("M44 96 C44 76 70 72 96 82 C120 90 124 114 104 124 C82 134 44 122 44 96 Z").toPath()
    }
    val pencilBodyPath = remember {
        PathParser().parsePathString("M0 0 L8 -14 L40 -46 C43 -49 48 -49 51 -46 C54 -43 54 -38 51 -35 L19 -3 Z").toPath()
    }
    val pencilEraserFillPath = remember {
        PathParser().parsePathString("M40 -46 C43 -49 48 -49 51 -46 C54 -43 54 -38 51 -35 Z").toPath()
    }
    val pencilEraserLinePath = remember {
        PathParser().parsePathString("M40 -46 L51 -35").toPath()
    }
    val pencilTipLinePath = remember {
        PathParser().parsePathString("M8 -14 L19 -3").toPath()
    }

    var progress by remember { mutableFloatStateOf(0f) }
    var drawInDone by remember { mutableStateOf(false) }

    LaunchedEffect(active, reduceMotion) {
        if (!active) {
            progress = 0f
            drawInDone = false
            return@LaunchedEffect
        }
        if (reduceMotion) {
            progress = 1f
            drawInDone = true
            return@LaunchedEffect
        }
        progress = 0f
        drawInDone = false
        val anim = Animatable(0f)
        anim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2200, easing = FastOutSlowInEasing)
        ) {
            progress = value
        }
        drawInDone = true
    }

    val infiniteTransition = rememberInfiniteTransition(label = "SceneDrawDayIdle")
    val bobPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bobPhase"
    )

    val pathMeasure = remember { PathMeasure() }
    val segmentPath = remember { Path() }

    Canvas(modifier = modifier.defaultMinSize(minWidth = 160.dp, minHeight = 160.dp)) {
        val scale = size.minDimension / 160f
        val offsetX = (size.width - 160f * scale) / 2f
        val offsetY = (size.height - 160f * scale) / 2f

        val bobOffset = if (drawInDone && !reduceMotion) {
            sin(bobPhase) * 2f
        } else {
            0f
        }

        withTransform({
            translate(left = offsetX, top = offsetY)
            scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
        }) {
            if (progress > 0f) {
                withTransform({
                    translate(left = 4f, top = 4f)
                }) {
                    drawPath(
                        path = spotPath,
                        color = palette.orange.copy(alpha = progress * 0.5f),
                        style = Fill
                    )
                }
            }

            pathMeasure.setPath(trailPath, false)
            val length = pathMeasure.length
            val dist = (length * progress).coerceIn(0f, length)

            if (dist > 0f) {
                segmentPath.reset()
                pathMeasure.getSegment(0f, dist, segmentPath, true)
                drawPath(
                    path = segmentPath,
                    color = palette.ink,
                    style = Stroke(
                        width = 3.2f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            if (active && length > 0f) {
                val headPos = pathMeasure.getPosition(dist)
                val headTan = pathMeasure.getTangent(dist)
                val posX = if (headPos.x.isFinite() && headTan.x.isFinite()) headPos.x else 30f
                val posY = if (headPos.y.isFinite() && headTan.y.isFinite()) headPos.y else 120f

                val pencilStroke = Stroke(
                    width = 3f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )

                withTransform({
                    translate(left = posX, top = posY + bobOffset)
                    rotate(degrees = -10f, pivot = Offset.Zero)
                }) {
                    drawPath(
                        path = pencilEraserFillPath,
                        color = palette.red.copy(alpha = 0.6f),
                        style = Fill
                    )
                    drawPath(
                        path = pencilBodyPath,
                        color = palette.ink,
                        style = pencilStroke
                    )
                    drawPath(
                        path = pencilEraserLinePath,
                        color = palette.ink,
                        style = pencilStroke
                    )
                    drawPath(
                        path = pencilTipLinePath,
                        color = palette.ink,
                        style = pencilStroke
                    )
                }
            }
        }
    }
}

private val YearGridDoodleCells = listOf(2, 5, 9, 12, 16, 20, 23, 27)
private val YearGridDoodleIcons = listOf(
    KannachIconType.SPROUT,
    KannachIconType.FLOWER,
    KannachIconType.SUMMER,
    KannachIconType.LEAF,
    KannachIconType.SPARKLE,
    KannachIconType.BOUQUET,
    KannachIconType.AUTUMN,
    KannachIconType.SPRING
)

@Composable
fun SceneYearGrid(
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalKannachPalette.current
    val handFont = LocalKannachHandFont.current
    val reduceMotion = rememberReduceMotion()

    val doodleScales = remember { List(8) { Animatable(0f) } }
    val circleProgress = remember { Animatable(0f) }

    LaunchedEffect(active, reduceMotion) {
        if (!active) {
            doodleScales.forEach { it.snapTo(0f) }
            circleProgress.snapTo(0f)
            return@LaunchedEffect
        }
        if (reduceMotion) {
            doodleScales.forEach { it.snapTo(1f) }
            circleProgress.snapTo(1f)
            return@LaunchedEffect
        }

        doodleScales.forEach { it.snapTo(0f) }
        circleProgress.snapTo(0f)

        for (i in doodleScales.indices) {
            if (i > 0) {
                delay(330L)
            }
            launch {
                doodleScales[i].snapTo(0.3f)
                doodleScales[i].animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
        }

        delay(1500L)
        circleProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing)
        )
    }

    Box(modifier = modifier.aspectRatio(7f / 6f)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cols = 7
            val rows = 5
            val cellW = size.width / cols
            val cellH = size.height / rows
            val lineW = 1.3.dp.toPx()
            val wobble = 1.5.dp.toPx()

            for (r in 1 until rows) {
                val y = r * cellH
                inkLine(
                    from = Offset(0f, y),
                    to = Offset(size.width, y),
                    seed = 500 + r,
                    color = palette.line,
                    widthPx = lineW,
                    wobblePx = wobble
                )
            }

            for (c in 1 until cols) {
                val x = c * cellW
                inkLine(
                    from = Offset(x, 0f),
                    to = Offset(x, size.height),
                    seed = 600 + c,
                    color = palette.line,
                    widthPx = lineW,
                    wobblePx = wobble
                )
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            for (r in 0 until 5) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    for (c in 0 until 7) {
                        val cellIndex = r * 7 + c
                        val dayNum = cellIndex + 1
                        val doodleIdx = YearGridDoodleCells.indexOf(cellIndex)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            if (dayNum <= 31) {
                                val isTargetCell = cellIndex == 20
                                val isCircled = isTargetCell && circleProgress.value > 0f
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(start = 2.dp, top = 2.dp)
                                        .size(width = 18.dp, height = 16.dp)
                                        .then(
                                            if (isTargetCell) {
                                                Modifier.drawBehind {
                                                    inkCircle(
                                                        center = Offset(size.width / 2f, size.height / 2f),
                                                        radiusPx = 10.dp.toPx(),
                                                        seed = 720,
                                                        color = palette.red,
                                                        widthPx = 1.6.dp.toPx(),
                                                        progress = circleProgress.value
                                                    )
                                                }
                                            } else {
                                                Modifier
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayNum.toString(),
                                        fontSize = 9.sp,
                                        lineHeight = 9.sp,
                                        fontFamily = handFont,
                                        fontWeight = if (isCircled) FontWeight.Bold else null,
                                        color = if (isCircled) palette.ink else palette.ink.copy(alpha = 0.45f),
                                        style = LocalTextStyle.current.copy(
                                            lineHeightStyle = LineHeightStyle(
                                                alignment = LineHeightStyle.Alignment.Center,
                                                trim = LineHeightStyle.Trim.Both
                                            ),
                                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                                        ),
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.wrapContentSize(unbounded = true)
                                    )
                                }
                            }
                            if (doodleIdx >= 0) {
                                val scaleVal = doodleScales[doodleIdx].value
                                if (scaleVal > 0f) {
                                    KannachIcon(
                                        icon = YearGridDoodleIcons[doodleIdx],
                                        tint = palette.ink,
                                        modifier = Modifier
                                            .size(26.dp)
                                            .align(Alignment.Center)
                                            .graphicsLayer {
                                                scaleX = scaleVal
                                                scaleY = scaleVal
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

