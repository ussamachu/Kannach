package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.compose.ui.graphics.Path
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

/**
 * Mathematical routines for smoothing, normalizing, and rasterizing
 * freehand strokes in Kannach (كُنَّاش).
 *
 * Faithfully matches the exact bezier curve midpoints and 300x300 normalization
 * mathematics from `kannach.html`.
 */
object StrokeMath {

    const val BASE_CANVAS_WIDTH = 300
    const val BASE_CANVAS_HEIGHT = 400
    const val NORMALIZED_BOX_SIZE = 300f
    const val NORMALIZED_PADDING = 24f
    const val DEFAULT_PAPER_COLOR = "#fbf9ef"

    /**
     * Converts a list of raw points into a smooth Compose [androidx.compose.ui.graphics.Path]
     * using quadratic Bezier curves passing through segment midpoints.
     *
     * Exact replication of `pathFrom(pts)`:
     * - 0 points: empty path
     * - 1 point: tiny line (M x y L x+0.1 y+0.1)
     * - 2 points: direct line (M x1 y1 L x2 y2)
     * - 3+ points: Q control_point midpoint, ending with L last_point
     */
    fun composePathFrom(pts: List<Point>): Path {
        val path = Path()
        if (pts.isEmpty()) return path

        val p0 = pts[0]
        path.moveTo(p0.x, p0.y)

        if (pts.size == 1) {
            path.lineTo(p0.x + 0.1f, p0.y + 0.1f)
            return path
        }

        if (pts.size == 2) {
            path.lineTo(pts[1].x, pts[1].y)
            return path
        }

        for (i in 1 until pts.size - 1) {
            val curr = pts[i]
            val next = pts[i + 1]
            val mx = (curr.x + next.x) / 2f
            val my = (curr.y + next.y) / 2f
            path.quadraticTo(curr.x, curr.y, mx, my)
        }

        val pl = pts.last()
        path.lineTo(pl.x, pl.y)
        return path
    }

    /**
     * Converts points into an Android native [android.graphics.Path] for Canvas drawing / rasterization.
     */
    fun androidPathFrom(pts: List<Point>): android.graphics.Path {
        val path = android.graphics.Path()
        if (pts.isEmpty()) return path

        val p0 = pts[0]
        path.moveTo(p0.x, p0.y)

        if (pts.size == 1) {
            path.lineTo(p0.x + 0.1f, p0.y + 0.1f)
            return path
        }

        if (pts.size == 2) {
            path.lineTo(pts[1].x, pts[1].y)
            return path
        }

        for (i in 1 until pts.size - 1) {
            val curr = pts[i]
            val next = pts[i + 1]
            val mx = (curr.x + next.x) / 2f
            val my = (curr.y + next.y) / 2f
            path.quadTo(curr.x, curr.y, mx, my)
        }

        val pl = pts.last()
        path.lineTo(pl.x, pl.y)
        return path
    }

    /**
     * Returns SVG path data string (e.g. "M... Q... L..."), matching JavaScript `pathFrom(pts)`.
     */
    fun svgPathDataFrom(pts: List<Point>): String {
        if (pts.isEmpty()) return ""
        val p0 = pts[0]
        if (pts.size == 1) return "M${p0.x} ${p0.y}L${p0.x + 0.1f} ${p0.y + 0.1f}"
        if (pts.size == 2) return "M${p0.x} ${p0.y}L${pts[1].x} ${pts[1].y}"

        val sb = java.lang.StringBuilder("M${p0.x} ${p0.y}")
        for (i in 1 until pts.size - 1) {
            val curr = pts[i]
            val next = pts[i + 1]
            val mx = roundToOneDecimal((curr.x + next.x) / 2f)
            val my = roundToOneDecimal((curr.y + next.y) / 2f)
            sb.append("Q${curr.x} ${curr.y} $mx $my")
        }
        val pl = pts.last()
        sb.append("L${pl.x} ${pl.y}")
        return sb.toString()
    }

    /**
     * Centers and scales raw strokes into a cohesive 300x300 bounding box
     * with 24px padding on each side.
     *
     * Exact replication of `normalizeStrokes(strokes)`:
     * - Computes minX, minY, maxX, maxY across all strokes
     * - BOX = 300 - PAD * 2 = 252
     * - s = min(BOX/w, BOX/h, 3)
     * - ox = PAD + (BOX - w*s)/2 - minX*s
     * - oy = PAD + (BOX - h*s)/2 - minY*s
     * - Points rounded to 1 decimal place
     */
    fun normalizeStrokes(strokes: List<List<Point>>): List<List<Point>> {
        val validStrokes = strokes.filter { it.isNotEmpty() }
        if (validStrokes.isEmpty()) return emptyList()

        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY

        for (stroke in validStrokes) {
            for (p in stroke) {
                if (p.x < minX) minX = p.x
                if (p.y < minY) minY = p.y
                if (p.x > maxX) maxX = p.x
                if (p.y > maxY) maxY = p.y
            }
        }

        if (minX.isInfinite() || maxX.isInfinite()) return emptyList()

        val rawW = maxX - minX
        val rawH = maxY - minY
        val w = if (rawW > 0f) rawW else 1f
        val h = if (rawH > 0f) rawH else 1f

        val pad = NORMALIZED_PADDING
        val box = NORMALIZED_BOX_SIZE - pad * 2f // 252f
        val s = minOf(box / w, box / h, 3f)

        val ox = pad + (box - w * s) / 2f - minX * s
        val oy = pad + (box - h * s) / 2f - minY * s

        return validStrokes.map { stroke ->
            stroke.map { p ->
                val nx = roundToOneDecimal(p.x * s + ox)
                val ny = roundToOneDecimal(p.y * s + oy)
                Point(nx, ny)
            }
        }
    }

    private fun roundToOneDecimal(v: Float): Float {
        return (v * 10f).roundToInt() / 10f
    }

    /**
     * Converts strokes into an Android [Bitmap] with `#fbf9ef` paper background,
     * faithfully reproducing the canvas rasterization in `svgToPngBlob(svgEl, scale)`.
     *
     * @param strokes List of strokes (each a list of points)
     * @param colorHex Ink stroke color in hex (e.g. "#5aa25f")
     * @param scale Resolution multiplier (default 3x, yielding 900x1200 image)
     * @param baseWidth Base canvas coordinate width (default 300)
     * @param baseHeight Base canvas coordinate height (default 400)
     */
    fun strokesToBitmap(
        strokes: List<List<Point>>,
        colorHex: String,
        scale: Float = 3f,
        baseWidth: Int = BASE_CANVAS_WIDTH,
        baseHeight: Int = BASE_CANVAS_HEIGHT
    ): Bitmap {
        val width = (baseWidth * scale).toInt()
        val height = (baseHeight * scale).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw #fbf9ef paper background
        val bgPaint = Paint().apply {
            color = android.graphics.Color.parseColor(DEFAULT_PAPER_COLOR)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Apply scale transform so coordinates 0..300 / 0..400 map cleanly
        canvas.save()
        canvas.scale(scale, scale)

        val inkColor = try {
            android.graphics.Color.parseColor(colorHex)
        } catch (e: Exception) {
            android.graphics.Color.parseColor(DEFAULT_GREEN_HEX)
        }

        val strokePaint = Paint().apply {
            color = inkColor
            style = Paint.Style.STROKE
            strokeWidth = 8f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }

        for (stroke in strokes) {
            if (stroke.isNotEmpty()) {
                val path = androidPathFrom(stroke)
                canvas.drawPath(path, strokePaint)
            }
        }

        canvas.restore()
        return bitmap
    }

    /**
     * Rasterizes strokes to a high-resolution PNG file saved in `context.cacheDir/images/`.
     *
     * @return File reference ready for sharing via FileProvider or media store.
     */
    fun saveStrokesAsPngFile(
        context: Context,
        strokes: List<List<Point>>,
        colorHex: String,
        filename: String = "kannach-sketch-${System.currentTimeMillis()}.png",
        scale: Float = 3f
    ): File {
        val imageDir = File(context.cacheDir, "images")
        if (!imageDir.exists()) imageDir.mkdirs()

        val file = File(imageDir, filename)
        val bitmap = strokesToBitmap(strokes, colorHex, scale = scale)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
        }
        bitmap.recycle()
        return file
    }

    /**
     * Resolves a content [Uri] for a cached image or export file via Android [FileProvider].
     */
    fun getShareableUri(context: Context, file: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }
}
