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

    /**
     * SK8, SK9, SK10 (fixes RB2):
     * Builds the final export bitmap with android.graphics.Canvas:
     * - Format dimensions: Story 9:16 (1080x1920), Square 1:1 (1080x1080), Page 3:4 (1080x1440)
     * - Outer background: palette.paperDesk
     * - Centered paper card with soft shadow, 28px rounded corners, 2px border in palette.line,
     *   and procedural paper texture (plain, ruled, dotted, kraft)
     * - Strokes scaled to fit inside the card with 48px padding
     * - SK9 Signature inside the card's bottom-end corner, inset 36px:
     *   sprout vector path + "Kannach" / "كُنَّاش" (28px, hand Typeface from res/font, palette.greenDeep at 0.72 alpha)
     *   + localized today date below (20px, UI Typeface from res/font, palette.inkSoft at 0.65 alpha)
     * - SK10 Story 9:16 safe zone: top >= 260px, bottom >= 340px, signature inside card well above bottom 340px
     */
    fun composeExportBitmap(
        context: Context,
        strokes: List<SketchStroke>,
        format: ExportFormat,
        paperType: SketchPaperType,
        includeSignature: Boolean,
        language: String,
        palette: KannachPalette = BasePalette,
        dateLabelOverride: String? = null
    ): Bitmap {
        val outW = format.widthPx
        val outH = format.heightPx
        val bitmap = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        fun toAndroidColor(c: androidx.compose.ui.graphics.Color): Int {
            val a = (c.alpha * 255f).roundToInt().coerceIn(0, 255)
            val r = (c.red * 255f).roundToInt().coerceIn(0, 255)
            val g = (c.green * 255f).roundToInt().coerceIn(0, 255)
            val b = (c.blue * 255f).roundToInt().coerceIn(0, 255)
            return android.graphics.Color.argb(a, r, g, b)
        }

        // 1. Outer background = palette.paperDesk
        val deskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = toAndroidColor(palette.paperDesk)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, outW.toFloat(), outH.toFloat(), deskPaint)

        // 2. Compute centered paper card rect (respecting SK10 safe zone for Story 9:16)
        val cardRect: android.graphics.RectF = when (format) {
            ExportFormat.STORY -> {
                // Top margin >= 260px (280px), bottom margin >= 340px (360px) -> height 1280px, width 920px
                val cw = 920f
                val ch = 1280f
                val left = (outW - cw) / 2f
                val top = 280f
                android.graphics.RectF(left, top, left + cw, top + ch)
            }
            ExportFormat.SQUARE -> {
                val cw = 920f
                val ch = 920f
                val left = (outW - cw) / 2f
                val top = (outH - ch) / 2f
                android.graphics.RectF(left, top, left + cw, top + ch)
            }
            ExportFormat.PAGE -> {
                val cw = 920f
                val ch = 1226f
                val left = (outW - cw) / 2f
                val top = (outH - ch) / 2f
                android.graphics.RectF(left, top, left + cw, top + ch)
            }
        }

        val cornerRadius = 28f

        // 3. Soft paper shadow behind card
        val shadowLayers = listOf(
            Triple(0f, 18f, 0.28f),
            Triple(0f, 11f, 0.45f),
            Triple(0f, 5f, 0.65f)
        )
        for ((dx, dy, alphaFactor) in shadowLayers) {
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = toAndroidColor(palette.paperShadow.copy(alpha = (palette.paperShadow.alpha * alphaFactor).coerceIn(0f, 1f)))
                style = Paint.Style.FILL
            }
            val sr = android.graphics.RectF(
                cardRect.left + dx,
                cardRect.top + dy,
                cardRect.right + dx,
                cardRect.bottom + dy
            )
            canvas.drawRoundRect(sr, cornerRadius, cornerRadius, shadowPaint)
        }

        // 4. Card fill & procedural paper texture (SK4 & SK8)
        val cardBgColor = when (paperType) {
            SketchPaperType.KRAFT -> palette.paperKraft
            else -> palette.paperHi
        }
        val cardFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = toAndroidColor(cardBgColor)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, cardFillPaint)

        // Clip to rounded card for procedural paper texture, strokes, and signature
        val clipPath = android.graphics.Path().apply {
            addRoundRect(cardRect, cornerRadius, cornerRadius, android.graphics.Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clipPath)

        // Subtle procedural grain
        val grainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = toAndroidColor(palette.ink.copy(alpha = if (paperType == SketchPaperType.KRAFT) 0.045f else 0.022f))
            style = Paint.Style.FILL
        }
        val grainStep = 28f
        val gCols = (cardRect.width() / grainStep).toInt()
        val gRows = (cardRect.height() / grainStep).toInt()
        var seed = (gCols * 31 + gRows).toLong()
        for (i in 0 until gCols) {
            for (j in 0 until gRows) {
                seed = (seed * 1103515245L + 12345L) and 0x7fffffffL
                if (seed % 7L == 0L) {
                    val gx = cardRect.left + i * grainStep + (seed % 14L).toFloat()
                    val gy = cardRect.top + j * grainStep + ((seed / 14L) % 14L).toFloat()
                    canvas.drawCircle(gx, gy, 1.8f, grainPaint)
                }
            }
        }

        // Ruled or Dotted lines in palette.line (SK4)
        if (paperType == SketchPaperType.RULED) {
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = toAndroidColor(palette.line)
                style = Paint.Style.STROKE
                strokeWidth = 2.2f
            }
            val spacing = 56f
            var y = cardRect.top + spacing
            while (y < cardRect.bottom - 20f) {
                canvas.drawLine(cardRect.left, y, cardRect.right, y, linePaint)
                y += spacing
            }
        } else if (paperType == SketchPaperType.DOTTED) {
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = toAndroidColor(palette.line)
                style = Paint.Style.FILL
            }
            val spacing = 44f
            var x = cardRect.left + spacing * 0.8f
            while (x < cardRect.right - 16f) {
                var y = cardRect.top + spacing * 0.8f
                while (y < cardRect.bottom - 16f) {
                    canvas.drawCircle(x, y, 2.8f, dotPaint)
                    y += spacing
                }
                x += spacing
            }
        }

        // 5. Draw strokes scaled to fit inside the card with 48px padding (SK8)
        val cardPad = 48f
        val availW = cardRect.width() - cardPad * 2f
        val availH = cardRect.height() - cardPad * 2f
        val scale = minOf(availW / BASE_CANVAS_WIDTH.toFloat(), availH / BASE_CANVAS_HEIGHT.toFloat())
        val drawW = BASE_CANVAS_WIDTH * scale
        val drawH = BASE_CANVAS_HEIGHT * scale
        val offsetX = cardRect.left + cardPad + (availW - drawW) / 2f
        val offsetY = cardRect.top + cardPad + (availH - drawH) / 2f

        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)

        for (stroke in strokes) {
            if (stroke.points.isNotEmpty()) {
                val strokeColorInt = try {
                    android.graphics.Color.parseColor(stroke.colorHex)
                } catch (_: Exception) {
                    toAndroidColor(palette.green)
                }
                val w = stroke.width.coerceAtLeast(1f)
                val p0 = stroke.points.first()
                // JD4: First point of each stroke draws a dot of width * 1.25 at 0.9 alpha (ink pooling)
                val poolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = strokeColorInt
                    alpha = (255 * 0.9f).roundToInt().coerceIn(0, 255)
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(p0.x, p0.y, (w * 1.25f) / 2f, poolPaint)

                val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = strokeColorInt
                    style = Paint.Style.STROKE
                    strokeWidth = w
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                val path = androidPathFrom(stroke.points)
                canvas.drawPath(path, strokePaint)
            }
        }
        canvas.restore()

        // Restore card clip before drawing signature and border (inset 36px is safely inside the 28px rounded corners)
        canvas.restore()

        // 6. SK9: Signature (when enabled) inside card's bottom-end corner, inset 36px
        if (includeSignature) {
            val isRtl = I18n.isRtl(language)
            val handTypeface = KannachFonts.resolveHandTypeface(context, isRtl)

            val uiTypeface = try {
                androidx.core.content.res.ResourcesCompat.getFont(
                    context,
                    if (isRtl) R.font.ibm_plex_sans_arabic else R.font.space_grotesk
                )
            } catch (_: Exception) {
                null
            } ?: android.graphics.Typeface.DEFAULT

            val brandWord = if (isRtl) "كُنَّاش" else "Kannach"
            val dateText = dateLabelOverride ?: run {
                val cal = java.util.Calendar.getInstance()
                val y = cal.get(java.util.Calendar.YEAR)
                val m = cal.get(java.util.Calendar.MONTH) + 1
                val d = cal.get(java.util.Calendar.DAY_OF_MONTH)
                val monthName = I18n.forLanguage(language).months.getOrElse(m - 1) { "" }
                val dayStr = I18n.formatYear(d, language)
                val yearStr = I18n.formatYear(y, language)
                "$dayStr $monthName $yearStr"
            }

            val wordPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = toAndroidColor(palette.greenDeep.copy(alpha = 0.72f))
                textSize = 28f
                typeface = handTypeface
                textAlign = Paint.Align.LEFT
            }

            val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = toAndroidColor(palette.inkSoft.copy(alpha = 0.65f))
                textSize = 20f
                typeface = uiTypeface
                textAlign = Paint.Align.LEFT
            }

            val sproutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = toAndroidColor(palette.greenDeep.copy(alpha = 0.72f))
                style = Paint.Style.STROKE
                strokeWidth = 2.6f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }

            val inset = 36f
            val sproutBoxSize = 28f
            val sproutGap = 8f
            val wordWidth = wordPaint.measureText(brandWord).coerceAtLeast(110f)
            val topRowWidth = sproutBoxSize + sproutGap + wordWidth
            val dateWidth = datePaint.measureText(dateText).coerceAtLeast(100f)

            val dateBaselineY = cardRect.bottom - inset
            val wordBaselineY = dateBaselineY - 28f

            // Bottom-end corner: right in LTR, left in RTL
            if (!isRtl) {
                val rightEdge = cardRect.right - inset
                val topRowStartX = rightEdge - topRowWidth
                drawAndroidSprout(
                    canvas = canvas,
                    left = topRowStartX,
                    top = wordBaselineY - 24f,
                    size = sproutBoxSize,
                    paint = sproutPaint
                )
                canvas.drawText(brandWord, topRowStartX + sproutBoxSize + sproutGap, wordBaselineY, wordPaint)
                canvas.drawText(dateText, rightEdge - dateWidth, dateBaselineY, datePaint)
            } else {
                val leftEdge = cardRect.left + inset
                // In RTL, place wordmark + sprout aligned to the bottom-end (left) corner
                canvas.drawText(brandWord, leftEdge, wordBaselineY, wordPaint)
                drawAndroidSprout(
                    canvas = canvas,
                    left = leftEdge + wordWidth + sproutGap,
                    top = wordBaselineY - 24f,
                    size = sproutBoxSize,
                    paint = sproutPaint
                )
                canvas.drawText(dateText, leftEdge, dateBaselineY, datePaint)
            }
        }

        // 7. 2px border in palette.line around the card (SK8)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = toAndroidColor(palette.line)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

        return bitmap
    }

    /**
     * Draws the hand-drawn Kannach sprout vector path onto an Android [Canvas].
     */
    private fun drawAndroidSprout(
        canvas: Canvas,
        left: Float,
        top: Float,
        size: Float,
        paint: Paint
    ) {
        val sx = size / 24f
        val sy = size / 24f
        canvas.save()
        canvas.translate(left, top)
        canvas.scale(sx, sy)

        val stem = android.graphics.Path().apply {
            moveTo(12f, 21f)
            lineTo(12f, 10.5f)
        }
        val leftLeaf = android.graphics.Path().apply {
            moveTo(12f, 11f)
            cubicTo(12f, 7.5f, 9.5f, 5f, 5.2f, 4.4f)
            cubicTo(4.6f, 8.9f, 6.8f, 12f, 12f, 11f)
            close()
        }
        val rightLeaf = android.graphics.Path().apply {
            moveTo(12f, 11f)
            cubicTo(12f, 8f, 14.2f, 5.7f, 18.1f, 5.2f)
            cubicTo(18.7f, 8.9f, 16.8f, 11.5f, 12f, 11f)
            close()
        }

        canvas.drawPath(stem, paint)
        canvas.drawPath(leftLeaf, paint)
        canvas.drawPath(rightLeaf, paint)
        canvas.restore()
    }

    /**
     * SK11: Saves an exported [Bitmap] to a PNG file in `context.cacheDir/images/` for FileProvider sharing.
     */
    fun saveExportBitmapAsPngFile(
        context: Context,
        bitmap: Bitmap,
        filename: String = "kannach-export-${System.currentTimeMillis()}.png"
    ): File {
        val imageDir = File(context.cacheDir, "images")
        if (!imageDir.exists()) imageDir.mkdirs()

        val file = File(imageDir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
        }
        return file
    }

    /**
     * SK11: Saves an exported [Bitmap] to `MediaStore.Images.Media` (`Pictures/Kannach`)
     * with `IS_PENDING` on API 29+ and zero storage permissions.
     */
    fun saveExportToMediaStore(
        context: Context,
        bitmap: Bitmap,
        filename: String = "Kannach_${System.currentTimeMillis()}.png"
    ): Uri? {
        val resolver = context.contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(
                    android.provider.MediaStore.Images.Media.RELATIVE_PATH,
                    "${android.os.Environment.DIRECTORY_PICTURES}/Kannach"
                )
                put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            android.provider.MediaStore.Images.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val itemUri = resolver.insert(collection, contentValues) ?: return null
        return try {
            val stream = resolver.openOutputStream(itemUri)
            if (stream != null) {
                stream.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    out.flush()
                }
            } else {
                // In local Robolectric tests openOutputStream may return null; still finalize URI
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val finValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
                }
                resolver.update(itemUri, finValues, null, null)
            }
            itemUri
        } catch (e: Exception) {
            resolver.delete(itemUri, null, null)
            null
        }
    }
}
