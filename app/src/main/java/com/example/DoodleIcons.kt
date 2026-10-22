package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp

class DoodleSpec(
    val lines: List<String>,
    val spot: String?,
    val dots: List<Pair<Float, Float>> = emptyList(),
    val dotScale: Float = 0.9f
)

private class ParsedDoodleSpec(
    val linePaths: List<Path>,
    val spotPath: Path?,
    val dots: List<Pair<Float, Float>>,
    val dotScale: Float
)

fun doodleSpecFor(icon: KannachIconType): DoodleSpec? = when (icon) {
    KannachIconType.CHEV_L -> DoodleSpec(
        lines = listOf("M30 10 C26 16 20 21 15 24 C20 28 26 33 30 38"),
        spot = null
    )
    KannachIconType.CHEV_R -> DoodleSpec(
        lines = listOf("M18 10 C22 16 28 21 33 24 C28 28 22 33 18 38"),
        spot = null
    )
    KannachIconType.X -> DoodleSpec(
        lines = listOf(
            "M12 12 C18 18 30 30 36 36",
            "M36 12 C30 18 18 30 12 36"
        ),
        spot = null
    )
    KannachIconType.CHECK -> DoodleSpec(
        lines = listOf("M10 25 C15 29 18 33 21 37 C26 28 32 19 39 11"),
        spot = null
    )
    KannachIconType.UNDO -> DoodleSpec(
        lines = listOf(
            "M17 14 L9 22 L17 30",
            "M9 22 C20 18 35 20 38 33"
        ),
        spot = null
    )
    KannachIconType.REDO -> DoodleSpec(
        lines = listOf(
            "M31 14 L39 22 L31 30",
            "M39 22 C28 18 13 20 10 33"
        ),
        spot = null
    )
    KannachIconType.HEART -> {
        val heart = "M24 40 C14 33 7 27 7 19 C7 13 12 9 17 10 C20 11 22 13 24 16 C26 13 28 11 31 10 C36 9 41 13 41 19 C41 27 34 33 24 40 Z"
        DoodleSpec(
            lines = listOf(heart),
            spot = heart
        )
    }
    KannachIconType.TRASH -> DoodleSpec(
        lines = listOf(
            "M10 14 L38 14",
            "M19 14 C19 9 29 9 29 14",
            "M13 14 L15 39 C15 40 16 41 17 41 L31 41 C32 41 33 40 33 39 L35 14",
            "M21 21 L21 34",
            "M27 21 L27 34"
        ),
        spot = "M13 14 L35 14 L33 41 L15 41 Z"
    )
    KannachIconType.EDIT -> {
        val body = "M10 39 L13 30 L33 10 C35 8 38 8 39 10 C41 12 40 14 38 16 L18 36 Z"
        DoodleSpec(
            lines = listOf(body, "M13 30 L18 36"),
            spot = body
        )
    }
    KannachIconType.GRID -> DoodleSpec(
        lines = emptyList(),
        spot = "M9 9 C20 7 30 8 39 9 C40 20 39 30 39 39 C28 41 18 40 9 39 C8 28 9 18 9 9 Z",
        dots = listOf(
            14f to 14f, 24f to 14f, 34f to 14f,
            14f to 24f, 24f to 24f, 34f to 24f,
            14f to 34f, 24f to 34f, 34f to 34f
        )
    )
    KannachIconType.SLIDERS -> {
        val secondKnob = "M31 21 C34 21 35 24 35 25 C35 27 33 29 31 29 C29 29 27 27 27 25 C27 23 29 21 31 21 Z"
        DoodleSpec(
            lines = listOf(
                "M8 15 C18 14 30 16 40 15",
                "M8 25 C18 26 30 24 40 25",
                "M8 35 C18 34 30 36 40 35",
                "M17 11 C20 11 21 14 21 15 C21 17 19 19 17 19 C15 19 13 17 13 15 C13 13 15 11 17 11 Z",
                secondKnob,
                "M21 31 C24 31 25 34 25 35 C25 37 23 39 21 39 C19 39 17 37 17 35 C17 33 19 31 21 31 Z"
            ),
            spot = secondKnob
        )
    }
    KannachIconType.MORE -> DoodleSpec(
        lines = emptyList(),
        spot = null,
        dots = listOf(12f to 24f, 24f to 24f, 36f to 24f),
        dotScale = 1.2f
    )
    KannachIconType.UPLOAD -> DoodleSpec(
        lines = listOf(
            "M24 32 L24 10",
            "M15 19 C19 16 22 12 24 9 C26 12 29 16 33 19",
            "M9 30 L9 38 C9 40 10 41 12 41 L36 41 C38 41 39 40 39 38 L39 30"
        ),
        spot = "M9 30 L39 30 L39 41 L9 41 Z"
    )
    KannachIconType.ERASER -> {
        val body = "M8 31 L25 11 C26 10 28 10 29 11 L38 19 C39 20 39 22 38 23 L25 37 L14 37 Z"
        DoodleSpec(
            lines = listOf(
                body,
                "M17 24 L31 35",
                "M24 41 L40 41"
            ),
            spot = body
        )
    }
    KannachIconType.FLOWER -> {
        val topPetal = "M24 19 C20 14 20 8 24 8 C28 8 28 14 24 19 Z"
        DoodleSpec(
            lines = listOf(
                topPetal,
                "M24 29 C20 34 20 40 24 40 C28 40 28 34 24 29 Z",
                "M19 24 C14 20 8 20 8 24 C8 28 14 28 19 24 Z",
                "M29 24 C34 20 40 20 40 24 C40 28 34 28 29 24 Z",
                "M24 21 C26 21 27 23 27 24 C27 26 25 27 24 27 C22 27 21 26 21 24 C21 22 22 21 24 21 Z"
            ),
            spot = topPetal
        )
    }
    KannachIconType.SPRING -> {
        val topPetal = "M24 19 C20 14 20 8 24 8 C28 8 28 14 24 19 Z"
        DoodleSpec(
            lines = listOf(
                topPetal,
                "M24 29 C20 34 20 40 24 40 C28 40 28 34 24 29 Z",
                "M19 24 C14 20 8 20 8 24 C8 28 14 28 19 24 Z",
                "M29 24 C34 20 40 20 40 24 C40 28 34 28 29 24 Z",
                "M24 21 C26 21 27 23 27 24 C27 26 25 27 24 27 C22 27 21 26 21 24 C21 22 22 21 24 21 Z"
            ),
            spot = topPetal
        )
    }
    KannachIconType.LEAF -> {
        val outline = "M10 38 C10 20 22 10 38 9 C38 26 28 37 10 38 Z"
        DoodleSpec(
            lines = listOf(
                outline,
                "M10 38 C18 28 26 20 33 14"
            ),
            spot = outline
        )
    }
    KannachIconType.SPROUT -> {
        val rightLeaf = "M24 25 C24 18 29 13 36 13 C36 20 31 25 24 25 Z"
        DoodleSpec(
            lines = listOf(
                "M24 41 C24 33 23 27 24 20",
                "M24 29 C17 29 13 25 13 18 C20 18 24 22 24 29 Z",
                rightLeaf,
                "M12 41 C20 39 30 42 38 40"
            ),
            spot = rightLeaf
        )
    }
    KannachIconType.AUTUMN -> {
        val outline = "M24 8 C34 14 38 24 32 34 C29 38 26 40 24 41 C22 40 19 38 16 34 C10 24 14 14 24 8 Z"
        DoodleSpec(
            lines = listOf(
                outline,
                "M24 14 L24 41"
            ),
            spot = outline
        )
    }
    KannachIconType.SUMMER -> {
        val sunDisc = "M24 15 C30 15 33 19 33 24 C33 30 29 33 24 33 C18 33 15 29 15 24 C15 18 19 15 24 15 Z"
        DoodleSpec(
            lines = listOf(
                sunDisc,
                "M24 5 L24 9",
                "M24 39 L24 43",
                "M5 24 L9 24",
                "M39 24 L43 24",
                "M11 11 L14 14",
                "M34 34 L37 37",
                "M37 11 L34 14",
                "M14 34 L11 37"
            ),
            spot = sunDisc
        )
    }
    KannachIconType.WINTER -> DoodleSpec(
        lines = listOf(
            "M24 8 L24 40",
            "M10 16 L38 32",
            "M10 32 L38 16",
            "M20 11 L24 14 L28 11",
            "M20 37 L24 34 L28 37"
        ),
        spot = "M24 18 C28 18 30 21 30 24 C30 28 27 30 24 30 C20 30 18 27 18 24 C18 21 20 18 24 18 Z"
    )
    KannachIconType.SEAL -> {
        val wax = "M24 7 C30 7 31 10 35 11 C39 13 41 16 41 21 C41 26 43 28 41 33 C39 38 36 39 32 41 C28 42 26 43 22 41 C17 40 15 38 11 35 C8 31 8 28 8 23 C8 18 10 15 13 12 C17 8 20 7 24 7 Z"
        DoodleSpec(
            lines = listOf(
                wax,
                "M24 17 L26 22 L31 22 L27 26 L29 31 L24 28 L19 31 L21 26 L17 22 L22 22 Z"
            ),
            spot = wax
        )
    }
    KannachIconType.SPARKLE -> {
        val star = "M24 6 C25 16 28 21 40 24 C28 27 25 32 24 42 C23 32 20 27 8 24 C20 21 23 16 24 6 Z"
        DoodleSpec(
            lines = listOf(
                star,
                "M38 8 L38 14",
                "M35 11 L41 11"
            ),
            spot = star
        )
    }
    KannachIconType.BOUQUET -> {
        val centerHead = "M20 17 C20 14 22 13 24 13 C27 13 28 15 28 17 C28 19 26 21 24 21 C22 21 20 19 20 17 Z"
        DoodleSpec(
            lines = listOf(
                "M24 40 L24 22",
                "M24 40 L15 26",
                "M24 40 L33 26",
                centerHead,
                "M10 22 C10 19 12 18 14 18 C17 18 18 20 18 22 C18 24 16 26 14 26 C12 26 10 24 10 22 Z",
                "M30 22 C30 19 32 18 34 18 C37 18 38 20 38 22 C38 24 36 26 34 26 C32 26 30 24 30 22 Z",
                "M18 40 C22 38 26 38 30 40"
            ),
            spot = centerHead
        )
    }
    KannachIconType.BRUSH -> {
        val tip = "M10 40 C10 34 14 32 17 33 C19 34 20 36 20 38 C20 41 14 42 10 40 Z"
        DoodleSpec(
            lines = listOf(
                "M19 33 L35 11 C36 10 38 10 39 11 C40 12 40 13 39 14 L22 35",
                tip
            ),
            spot = tip
        )
    }
    KannachIconType.RIBBON -> {
        val bow = "M24 22 C18 12 8 12 9 20 C10 28 20 26 24 22 C28 26 38 28 39 20 C40 12 30 12 24 22 Z"
        DoodleSpec(
            lines = listOf(
                bow,
                "M22 26 L16 40",
                "M26 26 L32 40"
            ),
            spot = bow
        )
    }
    KannachIconType.NOTEBOOK -> {
        val leftPage = "M6 12 C14 9 20 10 24 14 L24 38 C20 34 14 33 6 36 Z"
        DoodleSpec(
            lines = listOf(
                leftPage,
                "M42 12 C34 9 28 10 24 14 L24 38 C28 34 34 33 42 36 Z",
                "M29 19 C33 18 36 18 38 19",
                "M29 24 C33 23 36 23 38 24",
                "M29 29 C32 28 35 28 37 29"
            ),
            spot = leftPage
        )
    }
    KannachIconType.BELL -> {
        val bellBody = "M12 34 C14 30 15 26 15 21 C15 14 19 10 24 10 C29 10 33 14 33 21 C33 26 34 30 36 34 Z"
        DoodleSpec(
            lines = listOf(
                bellBody,
                "M20 38 C21 41 27 41 28 38",
                "M24 6 L24 10"
            ),
            spot = bellBody
        )
    }
    KannachIconType.TALK -> {
        val bubble = "M8 12 C8 10 10 9 12 9 L36 9 C38 9 40 10 40 12 L40 28 C40 30 38 31 36 31 L22 31 L14 39 L14 31 L12 31 C10 31 8 30 8 28 Z"
        DoodleSpec(
            lines = listOf(
                bubble,
                "M15 17 L33 17",
                "M15 23 L28 23"
            ),
            spot = bubble
        )
    }
    KannachIconType.EYE_OFF -> {
        val pupil = "M24 20 C27 20 28 22 28 24 C28 26 26 28 24 28 C22 28 20 26 20 24 C20 22 22 20 24 20 Z"
        DoodleSpec(
            lines = listOf(
                "M6 24 C12 15 36 15 42 24 C36 33 12 33 6 24 Z",
                pupil,
                "M10 40 L38 8"
            ),
            spot = pupil
        )
    }
    KannachIconType.HUT -> DoodleSpec(
        lines = listOf(
            "M8 24 L24 8 L40 24",
            "M12 22 L12 40 L36 40 L36 22",
            "M21 40 L21 30 L27 30 L27 40",
            "M30 26 L33 26",
            "M5 41 C9 39 13 41 17 40",
            "M33 41 C37 39 41 41 44 40"
        ),
        spot = "M12 22 L24 11 L36 22 L36 40 L12 40 Z"
    )
}

@Composable
internal fun RenderDoodleIcon(
    icon: KannachIconType,
    spec: DoodleSpec,
    modifier: Modifier,
    effectiveTint: Color,
    strokeWidthDp: Dp,
    filled: Boolean,
    fillColor: Color
) {
    val palette = LocalKannachPalette.current

    val parsed = remember(icon) {
        ParsedDoodleSpec(
            linePaths = spec.lines.map { d -> PathParser().parsePathString(d).toPath() },
            spotPath = spec.spot?.let { d -> PathParser().parsePathString(d).toPath() },
            dots = spec.dots,
            dotScale = spec.dotScale
        )
    }

    val spotColor: Color? = when (icon) {
        KannachIconType.HEART -> if (filled) {
            if (fillColor != Color.Transparent && fillColor != Color.Unspecified) fillColor else palette.red.copy(alpha = 0.45f)
        } else {
            palette.red.copy(alpha = 0.25f)
        }
        KannachIconType.EDIT,
        KannachIconType.ERASER,
        KannachIconType.AUTUMN,
        KannachIconType.SUMMER,
        KannachIconType.SPARKLE,
        KannachIconType.NOTEBOOK,
        KannachIconType.BELL,
        KannachIconType.HUT -> palette.orange.copy(alpha = 0.45f)
        KannachIconType.TRASH,
        KannachIconType.FLOWER,
        KannachIconType.BOUQUET,
        KannachIconType.SEAL,
        KannachIconType.BRUSH,
        KannachIconType.RIBBON -> palette.red.copy(alpha = 0.45f)
        KannachIconType.UPLOAD,
        KannachIconType.SLIDERS,
        KannachIconType.GRID,
        KannachIconType.SPRING,
        KannachIconType.SPROUT,
        KannachIconType.LEAF,
        KannachIconType.WINTER,
        KannachIconType.TALK,
        KannachIconType.EYE_OFF -> palette.green.copy(alpha = 0.45f)
        else -> null
    }

    val lineWidth = 2.4f * (strokeWidthDp.value / 1.8f)

    Canvas(modifier = modifier) {
        val scale = size.minDimension / 48f
        val offsetX = (size.width - 48f * scale) / 2f
        val offsetY = (size.height - 48f * scale) / 2f

        withTransform({
            translate(left = offsetX, top = offsetY)
            scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
        }) {
            val spPath = parsed.spotPath
            if (spPath != null && spotColor != null) {
                withTransform({
                    translate(left = 2.5f, top = 2.5f)
                }) {
                    drawPath(
                        path = spPath,
                        color = spotColor,
                        style = Fill
                    )
                }
            }

            val lineStroke = Stroke(
                width = lineWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
            for (path in parsed.linePaths) {
                drawPath(
                    path = path,
                    color = effectiveTint,
                    style = lineStroke
                )
            }

            val dotRadius = lineWidth * parsed.dotScale
            for ((x, y) in parsed.dots) {
                drawCircle(
                    color = effectiveTint,
                    radius = dotRadius,
                    center = Offset(x, y)
                )
            }
        }
    }
}
