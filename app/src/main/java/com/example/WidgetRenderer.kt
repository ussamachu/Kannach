package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.PathParser
import java.util.Calendar
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

object WidgetRenderer {

    private const val MAX_PIXELS = 1_000_000
    private const val MIN_DIM_PX = 100

    fun renderFallback(
        context: Context,
        widthPx: Int,
        heightPx: Int
    ): Bitmap {
        val (w, h) = clampDimensions(widthPx, heightPx)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val metrics = context.resources.displayMetrics
        val density = metrics.density

        val nowMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
        val p = getSeasonalPalette(nowMonth)
        drawPaperBackground(canvas, w.toFloat(), h.toFloat(), density, p)

        val cx = w / 2f
        val cy = h / 2f
        val s = 12f * density

        val sproutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f * density
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = p.ink.toArgb()
        }

        // Vertical stem
        canvas.drawLine(cx, cy + s, cx, cy - s * 0.8f, sproutPaint)

        // Two leaf curves
        val leafPath = android.graphics.Path().apply {
            moveTo(cx, cy + s * 0.1f)
            quadTo(cx - s * 0.9f, cy - s * 0.2f, cx - s * 0.75f, cy - s * 0.85f)
            quadTo(cx - s * 0.15f, cy - s * 0.65f, cx, cy + s * 0.1f)

            moveTo(cx, cy - s * 0.15f)
            quadTo(cx + s * 0.9f, cy - s * 0.45f, cx + s * 0.75f, cy - s * 1.05f)
            quadTo(cx + s * 0.15f, cy - s * 0.85f, cx, cy - s * 0.15f)
        }
        canvas.drawPath(leafPath, sproutPaint)

        return bitmap
    }

    fun renderYear(context: Context, widthPx: Int, heightPx: Int): Bitmap {
        val (w, h) = clampDimensions(widthPx, heightPx)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val metrics = context.resources.displayMetrics
        val density = metrics.density
        @Suppress("DEPRECATION")
        val scaledDensity = metrics.scaledDensity

        val repo = KannachRepository(context.applicationContext)
        val entries = repo.getEntries()
        val language = repo.getLanguage()
        val useArabicIndic = repo.getUserSettings().useArabicIndicNumerals
        val strings = I18n.forLanguage(language)
        val isRtl = I18n.isRtl(language)

        val nowCal = Calendar.getInstance()
        val nowYear = nowCal.get(Calendar.YEAR)
        val nowMonth = nowCal.get(Calendar.MONTH) + 1
        val nowDay = nowCal.get(Calendar.DAY_OF_MONTH)
        val todayKey = String.format(Locale.US, "%04d-%02d-%02d", nowYear, nowMonth, nowDay)

        val p = getSeasonalPalette(nowMonth)
        drawPaperBackground(canvas, w.toFloat(), h.toFloat(), density, p)

        val typeface = KannachFonts.resolveHandTypeface(context, isRtl)
        val boldTypeface = Typeface.create(typeface, Typeface.BOLD)

        val pad = 16f * density
        val footerH = 28f * density

        fun x(isStart: Boolean): Float {
            return if (isStart) {
                if (isRtl) w - pad else pad
            } else {
                if (isRtl) pad else w - pad
            }
        }

        fun alignFor(isStart: Boolean): Paint.Align {
            return if (isStart) {
                if (isRtl) Paint.Align.RIGHT else Paint.Align.LEFT
            } else {
                if (isRtl) Paint.Align.LEFT else Paint.Align.RIGHT
            }
        }

        val yearPrefix = String.format(Locale.US, "%04d-", nowYear)
        val plantedThisYear = entries.count { (key, entry) ->
            key.startsWith(yearPrefix) && isValidEntry(entry)
        }

        // Footer
        val footerBaseline = h - pad - 4f * density
        val yearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = boldTypeface
            textSize = 20f * scaledDensity
            color = p.ink.toArgb()
            textAlign = alignFor(isStart = true)
        }
        val yearText = I18n.formatNumber(nowYear, language, useArabicIndic)
        canvas.drawText(yearText, x(isStart = true), footerBaseline, yearPaint)

        val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = 12f * scaledDensity
            color = p.inkSoft.toArgb()
            textAlign = alignFor(isStart = false)
        }
        val countFormatted = I18n.formatNumber(plantedThisYear, language, useArabicIndic)
        val countText = String.format(strings.widgetDaysPlanted, countFormatted)
        canvas.drawText(countText, x(isStart = false), footerBaseline, countPaint)

        // Field area above footer
        val fieldW = max(1f, w - 2f * pad)
        val fieldH = max(1f, h - 2f * pad - footerH)

        val yearCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, nowYear)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val daysInYear = yearCal.getActualMaximum(Calendar.DAY_OF_YEAR)

        var cell = floor(sqrt((fieldW * fieldH).toDouble() / daysInYear.toDouble())).toFloat().coerceAtLeast(1f)
        var cols = max(1, floor(fieldW / cell).toInt())
        while (ceil(daysInYear.toDouble() / cols.toDouble()) * cell > fieldH && cell > 1f) {
            cell -= 1f
            cols = max(1, floor(fieldW / cell).toInt())
        }

        val gridW = cols * cell
        val startX = pad + (fieldW - gridW) / 2f
        val startY = pad

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.2f * density
            color = withAlpha(p.green.toArgb(), 0.5f)
        }
        val todayFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = p.green.toArgb()
        }
        val pastDotColor = withAlpha(p.ink.toArgb(), 0.55f)
        val futureDotColor = withAlpha(p.ink.toArgb(), 0.22f)
        val defaultGreenArgb = p.green.toArgb()
        val doodleBox = RectF()

        for (i in 0 until daysInYear) {
            yearCal.set(Calendar.DAY_OF_YEAR, i + 1)
            val m = yearCal.get(Calendar.MONTH) + 1
            val d = yearCal.get(Calendar.DAY_OF_MONTH)
            val dateKey = String.format(Locale.US, "%04d-%02d-%02d", nowYear, m, d)

            val rawCol = i % cols
            val col = if (isRtl) (cols - 1 - rawCol) else rawCol
            val row = i / cols

            val jx = (((i * 7919) % 13) - 6) / 6f * 0.10f * cell
            val jy = (((i * 104729) % 13) - 6) / 6f * 0.10f * cell

            val cx = startX + (col + 0.5f) * cell + jx
            val cy = startY + (row + 0.5f) * cell + jy

            val isToday = dateKey == todayKey
            val isPast = dateKey < todayKey
            val isFirstOfMonth = d == 1
            val entry = entries[dateKey]?.takeIf { isValidEntry(it) }

            if (entry != null) {
                val half = (cell * 1.35f) / 2f
                doodleBox.set(cx - half, cy - half, cx + half, cy + half)
                drawDoodle(canvas, entry, doodleBox, 1f, defaultGreenArgb)
                if (isToday) {
                    canvas.drawCircle(cx, cy, cell * 0.34f, ringPaint)
                }
            } else if (isToday) {
                canvas.drawCircle(cx, cy, cell * 0.2f, todayFillPaint)
                canvas.drawCircle(cx, cy, cell * 0.34f, ringPaint)
            } else {
                val baseRadius = if (isPast) {
                    max(1.2f * density, cell * 0.09f)
                } else {
                    max(1.0f * density, cell * 0.07f)
                }
                val radius = if (isFirstOfMonth) baseRadius * 1.5f else baseRadius
                dotPaint.color = if (isPast) pastDotColor else futureDotColor
                canvas.drawCircle(cx, cy, radius, dotPaint)
            }
        }

        return bitmap
    }

    fun renderToday(context: Context, widthPx: Int, heightPx: Int, compact: Boolean): Bitmap {
        val (w, h) = clampDimensions(widthPx, heightPx)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val metrics = context.resources.displayMetrics
        val density = metrics.density
        @Suppress("DEPRECATION")
        val scaledDensity = metrics.scaledDensity

        val repo = KannachRepository(context.applicationContext)
        val entries = repo.getEntries()
        val language = repo.getLanguage()
        val useArabicIndic = repo.getUserSettings().useArabicIndicNumerals
        val strings = I18n.forLanguage(language)
        val isRtl = I18n.isRtl(language)

        val nowCal = Calendar.getInstance()
        val nowYear = nowCal.get(Calendar.YEAR)
        val nowMonth = nowCal.get(Calendar.MONTH) + 1
        val nowDay = nowCal.get(Calendar.DAY_OF_MONTH)
        val todayKey = String.format(Locale.US, "%04d-%02d-%02d", nowYear, nowMonth, nowDay)

        val p = getSeasonalPalette(nowMonth)
        drawPaperBackground(canvas, w.toFloat(), h.toFloat(), density, p)

        val typeface = KannachFonts.resolveHandTypeface(context, isRtl)
        val boldTypeface = Typeface.create(typeface, Typeface.BOLD)

        val pad = (if (compact) 12f else 14f) * density

        fun x(isStart: Boolean): Float {
            return if (isStart) {
                if (isRtl) w - pad else pad
            } else {
                if (isRtl) pad else w - pad
            }
        }

        fun alignFor(isStart: Boolean): Paint.Align {
            return if (isStart) {
                if (isRtl) Paint.Align.RIGHT else Paint.Align.LEFT
            } else {
                if (isRtl) Paint.Align.LEFT else Paint.Align.RIGHT
            }
        }

        val validSorted = entries.entries
            .filter { isValidEntry(it.value) }
            .sortedBy { it.key }

        val totalValidCount = validSorted.size
        val todayEntry = entries[todayKey]?.takeIf { isValidEntry(it) }

        val rankNumber = if (todayEntry != null) {
            validSorted.indexOfFirst { it.key == todayKey } + 1
        } else {
            totalValidCount + 1
        }

        // Top row
        val topFontSizeSp = if (compact) 11f else 12f
        val topBaseline = pad + topFontSizeSp * scaledDensity
        if (!compact) {
            val plantedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.typeface = typeface
                textSize = 12f * scaledDensity
                color = p.inkSoft.toArgb()
                textAlign = alignFor(isStart = true)
            }
            val nStr = I18n.formatNumber(totalValidCount, language, useArabicIndic)
            canvas.drawText(
                String.format(strings.widgetDaysPlanted, nStr),
                x(isStart = true),
                topBaseline,
                plantedPaint
            )
        }

        val rankPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = topFontSizeSp * scaledDensity
            color = withAlpha(p.inkSoft.toArgb(), 0.8f)
            textAlign = alignFor(isStart = false)
        }
        val rankStr = "#" + I18n.formatNumber(rankNumber, language, useArabicIndic)
        canvas.drawText(rankStr, x(isStart = false), topBaseline, rankPaint)

        // Bottom row
        val weekdaySp = if (compact) 13f else 15f
        val dateSp = if (compact) 12f else 14f
        val bottomBaseline = h - pad - 2f * density

        val dowIdx = (nowCal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY + 7) % 7
        val rawWeekday = I18n.getFullWeekdays(language).getOrElse(dowIdx) { "" }
        val normLang = I18n.normalizeCode(language)
        val weekdayText = if (normLang == "en" || normLang == "fr" || normLang == "es") {
            rawWeekday.lowercase(I18n.localeFor(language))
        } else {
            rawWeekday
        }

        val weekdayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = boldTypeface
            textSize = weekdaySp * scaledDensity
            color = p.ink.toArgb()
            textAlign = alignFor(isStart = true)
        }
        canvas.drawText(weekdayText, x(isStart = true), bottomBaseline, weekdayPaint)

        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = dateSp * scaledDensity
            color = p.inkSoft.toArgb()
            textAlign = alignFor(isStart = false)
        }
        val ddStr = formatTwoDigit(nowDay, language, useArabicIndic)
        val mmStr = formatTwoDigit(nowMonth, language, useArabicIndic)
        val ddMmText = "$ddStr.$mmStr"
        canvas.drawText(ddMmText, x(isStart = false), bottomBaseline, datePaint)

        // Centre area between top row and bottom row
        val topAreaBottom = topBaseline + 6f * density
        val bottomAreaTop = bottomBaseline - weekdaySp * scaledDensity - 6f * density
        val freeH = max(24f * density, bottomAreaTop - topAreaBottom)
        val freeW = max(24f * density, w - 2f * pad)

        val boxFraction = if (compact) 0.70f else 0.62f
        val boxSide = min(freeW, freeH * boxFraction)
        val showPromptUnder = todayEntry == null
        val promptShift = if (showPromptUnder) 7f * density else 0f

        val centerX = w / 2f
        val centerY = (topAreaBottom + bottomAreaTop) / 2f - promptShift
        val doodleBox = RectF(
            centerX - boxSide / 2f,
            centerY - boxSide / 2f,
            centerX + boxSide / 2f,
            centerY + boxSide / 2f
        )

        if (todayEntry != null) {
            drawDoodle(canvas, todayEntry, doodleBox, 1f, p.green.toArgb())
        } else {
            drawSproutDoodle(canvas, doodleBox, p.ink.toArgb(), withAlpha(p.green.toArgb(), 0.5f), density)
        }

        if (showPromptUnder) {
            val promptPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.typeface = typeface
                textSize = 11f * scaledDensity
                color = p.inkSoft.toArgb()
                textAlign = Paint.Align.CENTER
            }
            val promptY = min(bottomAreaTop - 2f * density, doodleBox.bottom + 13f * scaledDensity)
            canvas.drawText(strings.widgetPlantToday, centerX, promptY, promptPaint)
        }

        return bitmap
    }

    private fun drawDoodle(
        canvas: Canvas,
        entry: DayEntry,
        box: RectF,
        alphaMul: Float,
        defaultGreenArgb: Int
    ) {
        if (entry.strokes.isEmpty()) return
        val side = min(box.width(), box.height())
        if (side <= 0f) return

        val scale = side / StrokeMath.NORMALIZED_BOX_SIZE
        val dx = box.left + (box.width() - StrokeMath.NORMALIZED_BOX_SIZE * scale) / 2f
        val dy = box.top + (box.height() - StrokeMath.NORMALIZED_BOX_SIZE * scale) / 2f

        val baseColor = parseEntryColor(entry.color, defaultGreenArgb)
        val strokeColor = withAlpha(baseColor, alphaMul)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 11f * scale
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = strokeColor
        }

        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate(dx, dy)
        }

        for (points in entry.strokes) {
            if (points.isEmpty()) continue
            val srcPath = StrokeMath.composePathFrom(points).asAndroidPath()
            val copyPath = android.graphics.Path(srcPath)
            copyPath.transform(matrix)
            canvas.drawPath(copyPath, paint)
        }
    }

    private fun drawSproutDoodle(
        canvas: Canvas,
        box: RectF,
        lineColorArgb: Int,
        spotColorArgb: Int,
        density: Float
    ) {
        val spec = doodleSpecFor(KannachIconType.SPROUT) ?: return
        val side = min(box.width(), box.height())
        if (side <= 0f) return

        val scale = side / 48f
        val dx = box.left + (box.width() - 48f * scale) / 2f
        val dy = box.top + (box.height() - 48f * scale) / 2f

        val lineMatrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate(dx, dy)
        }

        spec.spot?.let { spotStr ->
            val spotPath = android.graphics.Path(PathParser().parsePathString(spotStr).toPath().asAndroidPath())
            val spotMatrix = Matrix().apply {
                postTranslate(2.5f, 2.5f)
                postScale(scale, scale)
                postTranslate(dx, dy)
            }
            spotPath.transform(spotMatrix)
            val spotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = spotColorArgb
            }
            canvas.drawPath(spotPath, spotPaint)
        }

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = max(1.5f * density, 2.4f * scale)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = lineColorArgb
        }

        for (lineStr in spec.lines) {
            val path = android.graphics.Path(PathParser().parsePathString(lineStr).toPath().asAndroidPath())
            path.transform(lineMatrix)
            canvas.drawPath(path, linePaint)
        }
    }

    private fun drawPaperBackground(
        canvas: Canvas,
        w: Float,
        h: Float,
        density: Float,
        p: KannachPalette
    ) {
        val strokeW = 1.2f * density
        val inset = strokeW / 2f
        val rect = RectF(inset, inset, w - inset, h - inset)
        val radius = 26f * density

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = p.paper.toArgb()
        }
        canvas.drawRoundRect(rect, radius, radius, fillPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeW
            color = p.line.toArgb()
        }
        canvas.drawRoundRect(rect, radius, radius, borderPaint)
    }

    fun clampDimensions(widthPx: Int, heightPx: Int): Pair<Int, Int> {
        var w = max(MIN_DIM_PX, widthPx)
        var h = max(MIN_DIM_PX, heightPx)
        val total = w.toLong() * h.toLong()
        if (total > MAX_PIXELS) {
            val scale = sqrt(MAX_PIXELS.toDouble() / total.toDouble())
            w = max(MIN_DIM_PX, (w * scale).roundToInt())
            h = max(MIN_DIM_PX, (h * scale).roundToInt())
        }
        return w to h
    }

    private fun isValidEntry(entry: DayEntry): Boolean {
        return entry.text.isNotBlank() || entry.strokes.isNotEmpty()
    }

    private fun formatTwoDigit(n: Int, language: String, useArabicIndic: Boolean): String {
        val zero = I18n.formatNumber(0, language, useArabicIndic)
        val formatted = I18n.formatNumber(n, language, useArabicIndic)
        return if (n in 0..9) "$zero$formatted" else formatted
    }

    private fun parseEntryColor(hex: String, fallbackArgb: Int): Int {
        if (hex.isBlank()) return fallbackArgb
        return try {
            AndroidColor.parseColor(hex.trim())
        } catch (_: Exception) {
            fallbackArgb
        }
    }

    private fun withAlpha(argb: Int, alphaMul: Float): Int {
        val baseA = AndroidColor.alpha(argb)
        val newA = (baseA * alphaMul.coerceIn(0f, 1f)).roundToInt().coerceIn(0, 255)
        return (argb and 0x00FFFFFF) or (newA shl 24)
    }
}
