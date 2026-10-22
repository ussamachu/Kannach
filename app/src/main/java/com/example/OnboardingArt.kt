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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
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

    Canvas(modifier = modifier) {
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

    Canvas(modifier = modifier) {
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

            val cp = circleProgress.value
            if (cp > 0f) {
                val targetIndex = 20
                val targetCol = targetIndex % cols
                val targetRow = targetIndex / cols
                val numCenter = Offset(
                    x = targetCol * cellW + 9.dp.toPx(),
                    y = targetRow * cellH + 8.dp.toPx()
                )
                inkCircle(
                    center = numCenter,
                    radiusPx = 7.dp.toPx(),
                    seed = 720,
                    color = palette.red,
                    widthPx = 1.5.dp.toPx(),
                    progress = cp
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
                                Text(
                                    text = dayNum.toString(),
                                    fontSize = 9.sp,
                                    fontFamily = handFont,
                                    color = palette.ink.copy(alpha = 0.45f),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(start = 3.dp, top = 1.dp)
                                )
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

