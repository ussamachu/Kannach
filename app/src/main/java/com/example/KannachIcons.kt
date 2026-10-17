package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class KannachIconType {
    CHEV_L, CHEV_R, FLOWER, LEAF, X, HEART, CHECK, UNDO, TRASH, EDIT,
    GRID, SLIDERS, SPROUT, WINTER, SPRING, SUMMER, AUTUMN, SEAL,
    SPARKLE, UPLOAD, BOUQUET, BRUSH, RIBBON
}

/**
 * Hand-drawn SVG icons faithful to the original web application's stroke aesthetics.
 */
@Composable
fun KannachIcon(
    icon: KannachIconType,
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = Color.Unspecified,
    strokeWidthDp: Dp = 1.8.dp,
    filled: Boolean = false,
    fillColor: Color = Color.Transparent,
    contentDescription: String? = null
) {
    val effectiveTint = if (tint != Color.Unspecified) tint else LocalKannachPalette.current.ink
    val semanticModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }

    Canvas(modifier = semanticModifier) {
        val strokePx = strokeWidthDp.toPx()
        val scaleX = size.width / 24f
        val scaleY = size.height / 24f
        val stroke = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)

        when (icon) {
            KannachIconType.CHEV_L -> drawChevL(scaleX, scaleY, effectiveTint, stroke)
            KannachIconType.CHEV_R -> drawChevR(scaleX, scaleY, effectiveTint, stroke)
            KannachIconType.FLOWER -> drawFlower(scaleX, scaleY, effectiveTint, stroke)
            KannachIconType.LEAF -> drawLeaf(scaleX, scaleY, effectiveTint, stroke)
            KannachIconType.X -> drawX(scaleX, scaleY, effectiveTint, stroke)
            KannachIconType.HEART -> drawHeart(scaleX, scaleY, effectiveTint, stroke, filled, fillColor)
            KannachIconType.CHECK -> drawCheck(scaleX, scaleY, effectiveTint, stroke)
            KannachIconType.UNDO -> drawUndo(scaleX, scaleY, effectiveTint, stroke)
            KannachIconType.TRASH -> drawTrash(scaleX, scaleY, effectiveTint, stroke)
            KannachIconType.EDIT -> drawEdit(scaleX, scaleY, effectiveTint, stroke)
            KannachIconType.GRID -> drawGrid(scaleX, scaleY, effectiveTint)
            KannachIconType.SLIDERS -> drawSliders(scaleX, scaleY, effectiveTint, stroke)
            KannachIconType.SPROUT -> drawSprout(scaleX, scaleY, effectiveTint, stroke)
            KannachIconType.WINTER -> drawWinter(scaleX, scaleY, effectiveTint, stroke)
            KannachIconType.SPRING -> drawSpring(scaleX, scaleY, effectiveTint, stroke)
            KannachIconType.SUMMER -> drawSummer(scaleX, scaleY, effectiveTint, stroke)
            KannachIconType.AUTUMN -> drawLeaf(scaleX, scaleY, effectiveTint, stroke)
            KannachIconType.SEAL -> drawSeal(scaleX, scaleY, effectiveTint, stroke)
            KannachIconType.SPARKLE -> drawSparkle(scaleX, scaleY, effectiveTint, stroke)
            KannachIconType.UPLOAD -> drawUpload(scaleX, scaleY, effectiveTint, stroke)
            KannachIconType.BOUQUET -> drawBouquet(scaleX, scaleY, effectiveTint, stroke)
            KannachIconType.BRUSH -> drawBrush(scaleX, scaleY, effectiveTint, stroke)
            KannachIconType.RIBBON -> drawRibbon(scaleX, scaleY, effectiveTint, stroke, filled, fillColor)
        }
    }
}

private fun parseSvg(pathData: String, scaleX: Float, scaleY: Float): Path {
    val nodes = PathParser().parsePathString(pathData).toNodes()
    val path = Path()
    PathParser().addPathNodes(nodes).toPath(path)
    val matrix = Matrix().apply {
        scale(x = scaleX, y = scaleY)
    }
    path.transform(matrix)
    return path
}

private fun DrawScope.drawChevL(sx: Float, sy: Float, color: Color, stroke: Stroke) {
    val p = parseSvg("M14.9 5.2C11.8 8 10 10.4 9.5 12c.5 1.6 2.3 4 5.4 6.8", sx, sy)
    drawPath(p, color, style = stroke)
}

private fun DrawScope.drawChevR(sx: Float, sy: Float, color: Color, stroke: Stroke) {
    val p = parseSvg("M9.1 5.2C12.2 8 14 10.4 14.5 12c-.5 1.6-2.3 4-5.4 6.8", sx, sy)
    drawPath(p, color, style = stroke)
}

private fun DrawScope.drawFlower(sx: Float, sy: Float, color: Color, stroke: Stroke) {
    drawCircle(
        color = color,
        radius = 2.3f * sx,
        center = Offset(12f * sx, 12f * sy),
        style = stroke
    )
    val angles = floatArrayOf(0f, 72f, 144f, 216f, 288f)
    for (angle in angles) {
        rotate(degrees = angle, pivot = Offset(12f * sx, 12f * sy)) {
            val petal = parseSvg("M12 3.7c-1.7 1.9-1.7 4 0 5.2 1.7-1.2 1.7-3.3 0-5.2z", sx, sy)
            drawPath(petal, color, style = stroke)
        }
    }
}

private fun DrawScope.drawLeaf(sx: Float, sy: Float, color: Color, stroke: Stroke) {
    val p1 = parseSvg("M5.6 18.6C6.4 11.2 10.9 6.6 18.8 5.4c.5 7.9-3.6 12.8-11.1 13.6", sx, sy)
    val p2 = parseSvg("M5.6 18.6c3.4-3.8 7-7.1 11.2-10.3", sx, sy)
    drawPath(p1, color, style = stroke)
    drawPath(p2, color, style = stroke)
}

private fun DrawScope.drawX(sx: Float, sy: Float, color: Color, stroke: Stroke) {
    val p = parseSvg("M6.4 6.6c3.6 3.4 7.4 7.4 11.2 11M17.6 6.4c-3.5 3.6-7.5 7.6-11.2 11.2", sx, sy)
    drawPath(p, color, style = stroke)
}

private fun DrawScope.drawHeart(
    sx: Float,
    sy: Float,
    color: Color,
    stroke: Stroke,
    filled: Boolean,
    fillColor: Color
) {
    val p = parseSvg("M12.1 19.4C7.6 16 4.4 13 4.5 9.6 4.6 6.8 6.8 4.9 9.2 5.2c1.4.2 2.4 1.1 2.9 2.2.6-1.1 1.7-2 3.1-2.1 2.4-.2 4.5 1.7 4.5 4.4 0 3.4-3.4 6.3-7.6 9.7z", sx, sy)
    if (filled) {
        val fColor = if (fillColor != Color.Transparent) fillColor else color.copy(alpha = 0.35f)
        drawPath(p, fColor)
    }
    drawPath(p, color, style = stroke)
}

private fun DrawScope.drawCheck(sx: Float, sy: Float, color: Color, stroke: Stroke) {
    val p = parseSvg("M5.3 12.9c1.9 1.7 3.5 3.3 4.7 5 2.3-4.7 5.2-8.6 8.7-12.3", sx, sy)
    drawPath(p, color, style = stroke)
}

private fun DrawScope.drawUndo(sx: Float, sy: Float, color: Color, stroke: Stroke) {
    val p1 = parseSvg("M8.7 4.6L4.9 7.9l3.8 3.1", sx, sy)
    val p2 = parseSvg("M5.2 7.8h7.9c3.3 0 5.7 2 5.7 4.9 0 3-2.4 5-5.7 5h-3", sx, sy)
    drawPath(p1, color, style = stroke)
    drawPath(p2, color, style = stroke)
}

private fun DrawScope.drawTrash(sx: Float, sy: Float, color: Color, stroke: Stroke) {
    val p1 = parseSvg("M5.4 7.3l.8 11.4c.1 1.2 1 2.2 2.3 2.2h7c1.2 0 2.2-1 2.3-2.2l.8-11.3", sx, sy)
    val p2 = parseSvg("M4.2 7.3c5.2.4 10.4.4 15.6 0", sx, sy)
    val p3 = parseSvg("M9.4 7c.2-1.5.5-2.7 1-3.4 1-.4 2.2-.4 3.2 0 .5.7.8 1.9 1 3.5", sx, sy)
    val p4 = parseSvg("M9.9 10.7c.2 2.8.4 5.5.7 8.1M14.1 10.7c-.2 2.8-.4 5.5-.7 8.1", sx, sy)
    drawPath(p1, color, style = stroke)
    drawPath(p2, color, style = stroke)
    drawPath(p3, color, style = stroke)
    drawPath(p4, color, style = stroke)
}

private fun DrawScope.drawEdit(sx: Float, sy: Float, color: Color, stroke: Stroke) {
    val p1 = parseSvg("M4.6 19.3l.9-4.3L15.5 5c.9-.9 2.3-.9 3.2 0l.2.2c.9.9.9 2.3 0 3.2L9 18.4l-4.4.9z", sx, sy)
    val p2 = parseSvg("M13.3 6.9l3.8 3.8", sx, sy)
    drawPath(p1, color, style = stroke)
    drawPath(p2, color, style = stroke)
}

private fun DrawScope.drawGrid(sx: Float, sy: Float, color: Color) {
    val coords = floatArrayOf(6f, 12f, 18f)
    for (x in coords) {
        for (y in coords) {
            drawCircle(
                color = color,
                radius = 1.6f * sx,
                center = Offset(x * sx, y * sy)
            )
        }
    }
}

private fun DrawScope.drawSliders(sx: Float, sy: Float, color: Color, stroke: Stroke) {
    val p1 = parseSvg("M4 7h9M16.6 7H20", sx, sy)
    val p2 = parseSvg("M4 12.2h3.4M10 12.2H20", sx, sy)
    val p3 = parseSvg("M4 17.4h10.6M18 17.4H20", sx, sy)
    drawPath(p1, color, style = stroke)
    drawCircle(color, 1.9f * sx, Offset(13f * sx, 7f * sy), style = stroke)
    drawPath(p2, color, style = stroke)
    drawCircle(color, 1.9f * sx, Offset(7.7f * sx, 12.2f * sy), style = stroke)
    drawPath(p3, color, style = stroke)
    drawCircle(color, 1.9f * sx, Offset(16f * sx, 17.4f * sy), style = stroke)
}

private fun DrawScope.drawSprout(sx: Float, sy: Float, color: Color, stroke: Stroke) {
    val p1 = parseSvg("M12 21V10.5", sx, sy)
    val p2 = parseSvg("M12 11c0-3.5-2.5-6-6.8-6.6C4.6 8.9 6.8 12 12 11z", sx, sy)
    val p3 = parseSvg("M12 11c0-3 2.2-5.3 6.1-5.8.6 3.7-1.3 6.3-6.1 5.8z", sx, sy)
    drawPath(p1, color, style = stroke)
    drawPath(p2, color, style = stroke)
    drawPath(p3, color, style = stroke)
}

private fun DrawScope.drawWinter(sx: Float, sy: Float, color: Color, stroke: Stroke) {
    val p = parseSvg("M12 3v18M5 6.5l14 11M19 6.5L5 17.5", sx, sy)
    drawPath(p, color, style = stroke)
}

private fun DrawScope.drawSpring(sx: Float, sy: Float, color: Color, stroke: Stroke) {
    val p1 = parseSvg("M12 21v-7.3", sx, sy)
    val p2 = parseSvg("M8.2 11.3c0-2.8 1.6-4.8 3.8-5.5 2.2.7 3.8 2.7 3.8 5.5-1.9 1-5.7 1-7.6 0z", sx, sy)
    drawPath(p1, color, style = stroke)
    drawPath(p2, color, style = stroke)
}

private fun DrawScope.drawSummer(sx: Float, sy: Float, color: Color, stroke: Stroke) {
    drawCircle(color, 3.6f * sx, Offset(12f * sx, 12f * sy), style = stroke)
    val rays = parseSvg("M12 4v2.2M12 17.8V20M4 12h2.2M17.8 12H20M6.3 6.3l1.6 1.6M16.1 16.1l1.6 1.6M17.7 6.3l-1.6 1.6M7.9 16.1l-1.6 1.6", sx, sy)
    drawPath(rays, color, style = stroke)
}

private fun DrawScope.drawSeal(sx: Float, sy: Float, color: Color, stroke: Stroke) {
    val p1 = parseSvg("M3.6 6.6h16.8v11H3.6z", sx, sy)
    val p2 = parseSvg("M4 7l8 6.4L20 7", sx, sy)
    drawPath(p1, color, style = stroke)
    drawPath(p2, color, style = stroke)
}

private fun DrawScope.drawSparkle(sx: Float, sy: Float, color: Color, stroke: Stroke) {
    val p1 = parseSvg("M12 3.5c.5 3.4 2 5.3 5.6 6-3.6.7-5.1 2.6-5.6 6-.5-3.4-2-5.3-5.6-6 3.6-.7 5.1-2.6 5.6-6z", sx, sy)
    val p2 = parseSvg("M19 15.5c.2 1.4.8 2.1 2.2 2.4-1.4.3-2 1-2.2 2.4-.2-1.4-.8-2.1-2.2-2.4 1.4-.3 2-1 2.2-2.4z", sx, sy)
    drawPath(p1, color, style = stroke)
    drawPath(p2, color, style = stroke)
}

private fun DrawScope.drawUpload(sx: Float, sy: Float, color: Color, stroke: Stroke) {
    val p1 = parseSvg("M12 15.5V4.8M8 8.4l4-3.6 4 3.6", sx, sy)
    val p2 = parseSvg("M4.6 15.8v2.6c0 1 .8 1.8 1.8 1.8h11.2c1 0 1.8-.8 1.8-1.8v-2.6", sx, sy)
    drawPath(p1, color, style = stroke)
    drawPath(p2, color, style = stroke)
}

private fun DrawScope.drawBouquet(sx: Float, sy: Float, color: Color, stroke: Stroke) {
    val stem = parseSvg("M12 21v-8.4", sx, sy)
    drawPath(stem, color, style = stroke)
    drawCircle(color, 2.5f * sx, Offset(8.6f * sx, 8f * sy), style = stroke)
    drawCircle(color, 2.5f * sx, Offset(15.4f * sx, 8f * sy), style = stroke)
    drawCircle(color, 2.5f * sx, Offset(12f * sx, 5.6f * sy), style = stroke)
}

private fun DrawScope.drawBrush(sx: Float, sy: Float, color: Color, stroke: Stroke) {
    val p1 = parseSvg("M4.2 20c.9-4 2.8-7 5.7-8.9", sx, sy)
    val p2 = parseSvg("M9.4 11.6c1.7-3 4.3-5.3 8.4-6.4-1 4.2-3.2 7-6.3 8.6", sx, sy)
    drawPath(p1, color, style = stroke)
    drawPath(p2, color, style = stroke)
    drawCircle(color, 1.7f * sx, Offset(6.3f * sx, 17.3f * sy), style = stroke)
}

private fun DrawScope.drawRibbon(sx: Float, sy: Float, color: Color, stroke: Stroke, filled: Boolean, fillColor: Color) {
    val p = parseSvg("M7 3h10v18l-5-4.2-5 4.2z", sx, sy)
    if (filled && fillColor != Color.Transparent) {
        drawPath(p, fillColor)
    }
    drawPath(p, color, style = stroke)
}

