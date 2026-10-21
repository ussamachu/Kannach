package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Typography
import androidx.compose.ui.unit.sp
import java.util.Calendar
import kotlin.random.Random

/**
 * Authentic craft paper palette tokens faithfully ported from `:root` in `kannach.html`.
 */
@Immutable
data class KannachPalette(
    val paper: Color,
    val paperHi: Color,
    val paperLo: Color,
    val ink: Color,
    val inkSoft: Color,
    val inkFaint: Color,
    val green: Color,
    val greenDeep: Color,
    val greenSoft: Color,
    val greenLine: Color,
    val dot: Color,
    val line: Color,
    val red: Color,
    val orange: Color,
    val blue: Color,
    val ltblue: Color,
    val leaf: Color,
    val scrim: Color = Color(0x661A281E),
    val onGreen: Color = Color(0xFFFDFBF7),
    val paperDesk: Color = Color(0xFFE8E2CF),
    val todMorning: Color = Color(0xFFFDE8C8),
    val todEvening: Color = Color(0xFFE9C5A6),
    val todNight: Color = Color(0xFF33423A),
    val dangerBorder: Color = Color(0xFFB44636),
    val danger: Color = Color(0xFFD95F4C),
    val dangerBg: Color = Color(0xFFFBEBE8),
    val noise: Color = Color(0x083A523D),
    val paperShadow: Color = Color(0x1E3A523D),
    val paperKraft: Color = Color(0xFFE5D7BA)
)

// Base Default Palette
val BasePalette = KannachPalette(
    paper = Color(0xFFFBF9EF),
    paperHi = Color(0xFFFFFDF4),
    paperLo = Color(0xFFF2EDDA),
    ink = Color(0xFF3A523D),
    inkSoft = Color(0xFF7D8F7A),
    inkFaint = Color(0xFFA9B6A3),
    green = Color(0xFF56945F),
    greenDeep = Color(0xFF386B42),
    greenSoft = Color(0xFFE3EEDB),
    greenLine = Color(0xFFC8D8BF),
    dot = Color(0xFFC7D3BA),
    line = Color(0xFFDDD7C2),
    red = Color(0xFFD95F4C),
    orange = Color(0xFFE9963E),
    blue = Color(0xFF4D74AA),
    ltblue = Color(0xFF74B8CA),
    leaf = Color(0xFF5AA25F),
    scrim = Color(0x661A281E),
    onGreen = Color(0xFFFDFBF7),
    paperDesk = Color(0xFFE8E2CF),
    todMorning = Color(0xFFFDE8C8),
    todEvening = Color(0xFFE9C5A6),
    todNight = Color(0xFF33423A),
    dangerBorder = Color(0xFFB44636),
    noise = Color(0x083A523D),
    paperShadow = Color(0x1E3A523D),
    paperKraft = Color(0xFFE5D7BA)
)

// Season: Winter (months 12, 1, 2)
val WinterPalette = BasePalette.copy(
    green = Color(0xFF3F7566),
    greenDeep = Color(0xFF2C5850),
    greenSoft = Color(0xFFE2EEEA),
    greenLine = Color(0xFFC3D9D2),
    paper = Color(0xFFF5F8F6),
    paperHi = Color(0xFFFDFFFE)
)

// Season: Spring (months 3, 4, 5)
val SpringPalette = BasePalette.copy(
    green = Color(0xFF5AA25F),
    greenDeep = Color(0xFF386B42),
    paper = Color(0xFFFBFAF0),
    paperHi = Color(0xFFFFFDF5)
)

// Season: Summer (months 6, 7, 8)
val SummerPalette = BasePalette.copy(
    green = Color(0xFF6A9C3F),
    greenDeep = Color(0xFF476B28),
    greenSoft = Color(0xFFEAF0DC),
    greenLine = Color(0xFFD3DDBD),
    paper = Color(0xFFFDFAF0),
    paperHi = Color(0xFFFFFDF3)
)

// Season: Autumn (months 9, 10, 11)
val AutumnPalette = BasePalette.copy(
    green = Color(0xFF4F8A52),
    greenDeep = Color(0xFF33613A),
    greenSoft = Color(0xFFE6EEDB),
    greenLine = Color(0xFFCDDABB),
    paper = Color(0xFFF8F4E8),
    paperHi = Color(0xFFFFFCF2)
)

fun getSeasonalPalette(month: Int, seasonOverride: String = "auto"): KannachPalette {
    return when (seasonOverride.lowercase()) {
        "winter" -> WinterPalette
        "spring" -> SpringPalette
        "summer" -> SummerPalette
        "autumn" -> AutumnPalette
        else -> when (month) {
            12, 1, 2 -> WinterPalette
            3, 4, 5 -> SpringPalette
            6, 7, 8 -> SummerPalette
            else -> AutumnPalette
        }
    }
}

val LocalKannachPalette = staticCompositionLocalOf { BasePalette }
val LocalKannachLanguage = compositionLocalOf { "ar" }

/**
 * Organic paper tilt modifier imitating physical sheets resting naturally on a wooden desk.
 * Cycles subtly between gentle organic angles: [-0.6°, +0.5°, -0.25°].
 */
fun Modifier.paperTilt(index: Int = 0): Modifier {
    val angles = floatArrayOf(-0.6f, 0.5f, -0.25f)
    val angle = angles[kotlin.math.abs(index) % angles.size]
    return this.rotate(angle)
}

fun Modifier.paperTilt(angleDegrees: Float): Modifier = this.rotate(angleDegrees)

/**
 * Organic hand-drawn rounded shape imitating:
 * border-radius: 17px 255px 19px 235px / 235px 19px 255px 17px;
 */
class HandDrawnShape(
    private val radiusTopLeft: Float = 17f,
    private val radiusTopRight: Float = 26f,
    private val radiusBottomRight: Float = 19f,
    private val radiusBottomLeft: Float = 24f,
    private val altWobble: Boolean = false
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val w = size.width
        val h = size.height
        val path = Path()

        if (w <= 0f || h <= 0f) return Outline.Generic(path)

        with(density) {
            val rTL = if (altWobble) radiusTopRight.dp.toPx() else radiusTopLeft.dp.toPx()
            val rTR = if (altWobble) radiusTopLeft.dp.toPx() else radiusTopRight.dp.toPx()
            val rBR = if (altWobble) radiusBottomLeft.dp.toPx() else radiusBottomRight.dp.toPx()
            val rBL = if (altWobble) radiusBottomRight.dp.toPx() else radiusBottomLeft.dp.toPx()

            val roundRect = RoundRect(
                rect = Rect(0f, 0f, w, h),
                topLeft = CornerRadius(rTL, rTL * 0.9f),
                topRight = CornerRadius(rTR * 1.1f, rTR),
                bottomRight = CornerRadius(rBR, rBR * 1.1f),
                bottomLeft = CornerRadius(rBL * 0.9f, rBL)
            )
            path.addRoundRect(roundRect)
        }
        return Outline.Generic(path)
    }
}

val HandDrawnRadiusShape = HandDrawnShape(17f, 26f, 19f, 24f, altWobble = false)
val HandDrawnRadiusAltShape = HandDrawnShape(26f, 17f, 24f, 19f, altWobble = true)
val HandDrawnDockShape = HandDrawnShape(32f, 28f, 30f, 26f, altWobble = false)

/**
 * HM2, HM3 & TC3: Tear-off desk-calendar page shape with torn zig-zag top and bottom edges.
 */
object TearOffPageShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val w = size.width
        val h = size.height
        val path = Path()
        if (w <= 0f || h <= 0f) return Outline.Generic(path)

        with(density) {
            val zigStep = 8.dp.toPx().coerceAtLeast(4f)
            val zigDepth = 4.dp.toPx().coerceAtMost(h * 0.1f)

            val teethCount = (w / zigStep).toInt().coerceAtLeast(2)
            val actualStep = w / teethCount

            // Top torn edge (left to right)
            path.moveTo(0f, zigDepth)
            for (i in 0 until teethCount) {
                val midX = (i + 0.5f) * actualStep
                val endX = (i + 1) * actualStep
                path.lineTo(midX, 0f)
                path.lineTo(endX, zigDepth)
            }

            // Right edge down to bottom torn edge
            path.lineTo(w, h - zigDepth)

            // Bottom torn edge (right to left)
            for (i in teethCount - 1 downTo 0) {
                val midX = (i + 0.5f) * actualStep
                val startX = i * actualStep
                path.lineTo(midX, h)
                path.lineTo(startX, h - zigDepth)
            }

            // Left edge back to top-left
            path.close()
        }
        return Outline.Generic(path)
    }
}

// PF4: Cache noise pattern into a 256x256 ImageBitmap once per palette color, avoiding 13k noise loop on every redraw
private val noiseBitmapCache = java.util.concurrent.ConcurrentHashMap<Int, ImageBitmap>()

private fun getOrCreateNoiseBitmap(noiseColorArgb: Int): ImageBitmap {
    return noiseBitmapCache.computeIfAbsent(noiseColorArgb) {
        val width = 256
        val height = 256
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint().apply {
            color = noiseColorArgb
            isAntiAlias = true
        }
        val step = 14f
        val cols = (width / step).toInt()
        val rows = (height / step).toInt()
        var seed = 1337L
        for (i in 0 until cols) {
            for (j in 0 until rows) {
                seed = (seed * 1103515245L + 12345L) and 0x7fffffffL
                if (seed % 7L == 0L) {
                    val ox = i * step + (seed % 10L).toFloat()
                    val oy = j * step + ((seed / 10L) % 10L).toFloat()
                    canvas.drawCircle(ox, oy, 1.1f, paint)
                }
            }
        }
        bitmap.asImageBitmap()
    }
}

/**
 * NV5 & PF4: Soft paper shadow built once per size change via drawWithCache without Material elevation.
 */
fun Modifier.paperShadow(
    shadowColor: Color,
    shape: Shape = HandDrawnRadiusShape,
    offsetY: Dp = 4.dp
): Modifier = this.drawWithCache {
    val dy = offsetY.toPx()
    val layers = listOf(
        Pair(dy * 1.35f, 0.35f),
        Pair(dy * 0.9f, 0.55f),
        Pair(dy * 0.45f, 0.75f)
    )
    val outline = shape.createOutline(size, layoutDirection, this)
    val genericShadowPaths = if (outline is Outline.Generic) {
        layers.map { (shiftY, alphaFactor) ->
            val p = Path().apply {
                addPath(outline.path, Offset(0f, shiftY))
            }
            val c = shadowColor.copy(alpha = (shadowColor.alpha * alphaFactor).coerceIn(0f, 1f))
            Pair(p, c)
        }
    } else null

    onDrawBehind {
        if (genericShadowPaths != null) {
            for ((p, c) in genericShadowPaths) {
                drawPath(p, c)
            }
        } else {
            layers.forEach { (shiftY, alphaFactor) ->
                val c = shadowColor.copy(alpha = (shadowColor.alpha * alphaFactor).coerceIn(0f, 1f))
                when (outline) {
                    is Outline.Rounded -> {
                        drawRoundRect(
                            color = c,
                            topLeft = Offset(outline.roundRect.left, outline.roundRect.top + shiftY),
                            size = Size(outline.roundRect.width, outline.roundRect.height),
                            cornerRadius = outline.roundRect.topLeftCornerRadius
                        )
                    }
                    is Outline.Rectangle -> {
                        drawRect(
                            color = c,
                            topLeft = Offset(0f, shiftY),
                            size = size
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}

/**
 * PF4: Custom Modifier providing warm craft paper background with tiled noise grain via drawWithCache.
 * Generates noise once into a 256x256 ImageBitmap and renders it using an ImageShader.
 */
fun Modifier.paperBackground(
    color: Color = Color(0xFFFBF9EF),
    drawNoise: Boolean = true
): Modifier = this.drawWithCache {
    val noisePaint = if (drawNoise) {
        val noiseColor = Color(0xFF3A523D).copy(alpha = 0.024f)
        val noiseBitmap = getOrCreateNoiseBitmap(noiseColor.toArgb())
        val shader = ImageShader(
            image = noiseBitmap,
            tileModeX = TileMode.Repeated,
            tileModeY = TileMode.Repeated
        )
        Paint().apply { this.shader = shader }
    } else null

    onDrawBehind {
        drawRect(color = color)
        if (noisePaint != null) {
            drawIntoCanvas { canvas ->
                canvas.drawRect(
                    Rect(0f, 0f, size.width, size.height),
                    noisePaint
                )
            }
        }
    }
}

/**
 * Draws notebook ruled lines behind text (like repeating-linear-gradient).
 */
fun Modifier.linedPaperBackground(
    lineColor: Color,
    lineHeightPx: Float
): Modifier = this.drawBehind {
    if (lineHeightPx <= 0f) return@drawBehind

    val totalHeight = size.height
    var y = lineHeightPx
    val strokeWidth = 1.5f

    while (y < totalHeight) {
        drawLine(
            color = lineColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = strokeWidth
        )
        y += lineHeightPx
    }
}

fun Modifier.linedPaperBackground(
    lineColor: Color,
    lineHeightDp: Dp = 32.dp
): Modifier = this.drawBehind {
    val lineHeight = lineHeightDp.toPx()
    if (lineHeight <= 0f) return@drawBehind

    val totalHeight = size.height
    var y = lineHeight
    val strokeWidth = 1.5f

    while (y < totalHeight) {
        drawLine(
            color = lineColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = strokeWidth
        )
        y += lineHeight
    }
}

/**
 * SK4: Draws a subtle dotted notebook grid behind canvas/paper using palette.line.
 */
fun Modifier.dottedPaperBackground(
    dotColor: Color,
    spacingDp: Dp = 20.dp,
    dotRadiusDp: Dp = 1.4.dp
): Modifier = this.drawBehind {
    val step = spacingDp.toPx()
    if (step <= 0f) return@drawBehind
    val radius = dotRadiusDp.toPx()

    var x = step * 0.75f
    while (x < size.width) {
        var y = step * 0.75f
        while (y < size.height) {
            drawCircle(
                color = dotColor,
                radius = radius,
                center = Offset(x, y)
            )
            y += step
        }
        x += step
    }
}

/**
 * Draws a hand-drawn wavy border divider below headers, matching:
 * M0 4 C5 0, 10 8, 15 4 S25 0, 30 4 S38 8, 40 4
 */
fun Modifier.wavyHeaderDivider(
    color: Color,
    waveHeightDp: Dp = 8.dp,
    isOnTop: Boolean = false
): Modifier = this.drawBehind {
    val waveH = waveHeightDp.toPx()
    val waveW = waveH * 5f // 40px width for 8px height
    val startY = if (isOnTop) waveH / 2f else size.height - waveH / 2f
    val path = Path()

    var x = 0f
    path.moveTo(0f, startY)
    while (x < size.width) {
        val w5 = waveW * (5f / 40f)
        val w10 = waveW * (10f / 40f)
        val w15 = waveW * (15f / 40f)
        val w20 = waveW * (20f / 40f)
        val w25 = waveW * (25f / 40f)
        val w30 = waveW * (30f / 40f)
        val w38 = waveW * (38f / 40f)
        val w40 = waveW

        path.cubicTo(
            x + w5, startY - waveH / 2f,
            x + w10, startY + waveH / 2f,
            x + w15, startY
        )
        path.cubicTo(
            x + w20, startY - waveH / 2f,
            x + w25, startY + waveH / 2f,
            x + w30, startY
        )
        path.cubicTo(
            x + w38, startY + waveH / 2f,
            x + w40, startY - waveH / 2f,
            x + w40, startY
        )
        x += waveW
    }

    drawPath(
        path = path,
        color = color.copy(alpha = 0.85f),
        style = Stroke(width = 1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

/**
 * Draws an authentic dashed hand-drawn border matching CSS `border: 2px dashed var(--green-line)`.
 * Supports optional inset (e.g., 4.dp or 5.dp for NV5/NV6b stitched inner edge).
 */
fun Modifier.dashedHandDrawnBorder(
    color: Color,
    shape: Shape = HandDrawnRadiusShape,
    strokeWidth: Dp = 2.dp,
    dashIntervals: FloatArray = floatArrayOf(14f, 10f),
    inset: Dp = 0.dp
): Modifier = this.drawBehind {
    val insetPx = inset.toPx()
    val targetSize = Size(
        (size.width - insetPx * 2f).coerceAtLeast(0f),
        (size.height - insetPx * 2f).coerceAtLeast(0f)
    )
    if (targetSize.width <= 0f || targetSize.height <= 0f) return@drawBehind
    val outline = shape.createOutline(targetSize, layoutDirection, this)
    val strokeStyle = Stroke(
        width = strokeWidth.toPx(),
        pathEffect = PathEffect.dashPathEffect(dashIntervals, 0f),
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
    )
    when (outline) {
        is Outline.Generic -> {
            val shifted = Path().apply {
                addPath(outline.path, Offset(insetPx, insetPx))
            }
            drawPath(shifted, color, style = strokeStyle)
        }
        is Outline.Rounded -> drawRoundRect(
            color = color,
            topLeft = Offset(outline.roundRect.left + insetPx, outline.roundRect.top + insetPx),
            size = Size(outline.roundRect.width, outline.roundRect.height),
            cornerRadius = outline.roundRect.topLeftCornerRadius,
            style = strokeStyle
        )
        is Outline.Rectangle -> drawRect(
            color = color,
            topLeft = Offset(insetPx, insetPx),
            size = targetSize,
            style = strokeStyle
        )
    }
}

// Google Fonts locally installed via font-util (Arabic hand font routed via KannachFonts.handAr — FT1)
val PlaypenSansFamily = KannachFonts.handLatin
val IbmPlexArabicFamily = FontFamily(Font(R.font.ibm_plex_sans_arabic, FontWeight.Normal))
val SpaceGroteskFamily = FontFamily(Font(R.font.space_grotesk, FontWeight.Normal))

/**
 * FT1: Authentic handwritten font for journal entries, notes, titles, letters, and sketch labels.
 * Routed exclusively through KannachFonts.handAr as the single swap point.
 */
val KannachHandFont: FontFamily = KannachFonts.handAr

/**
 * Clean UI font for labels, buttons, and app metadata.
 * Pairs IBM Plex Sans Arabic and Space Grotesk.
 */
val KannachUiFont = FontFamily(
    Font(R.font.ibm_plex_sans_arabic, FontWeight.Normal),
    Font(R.font.space_grotesk, FontWeight.Normal)
)

val LocalKannachHandFont = staticCompositionLocalOf { KannachFonts.handAr }
val LocalKannachUiFont = staticCompositionLocalOf { KannachUiFont }

/**
 * C1: Shared TextStyle for large Arabic display text (AppHeader month name,
 * TodayPageCard day name, MonthPageCard month name, write-page date) so tall
 * Playpen Sans Arabic ascenders/hamzas are never clipped.
 */
val ArabicDisplayStyle = TextStyle(
    lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
        alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
        trim = androidx.compose.ui.text.style.LineHeightStyle.Trim.None
    ),
    platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = true)
)

/**
 * Root theme composable for Kannach.
 * Handles dynamic RTL/LTR directionality, typography, and seasonal color palette updates.
 */
@Composable
fun KannachTheme(
    language: String = "ar",
    month: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    seasonOverride: String = "auto",
    reduceMotionOverride: Boolean = false,
    hapticsEnabled: Boolean = true,
    handwritingSp: Int = 19,
    content: @Composable () -> Unit
) {
    val palette = getSeasonalPalette(month, seasonOverride)
    val isRtl = I18n.isRtl(language)
    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    val activeHandFont = if (isRtl) KannachFonts.handAr else KannachFonts.handLatin
    val activeUiFont = if (isRtl) IbmPlexArabicFamily else SpaceGroteskFamily

    val m3Colors = lightColorScheme(
        primary = palette.green,
        onPrimary = Color.White,
        primaryContainer = palette.greenSoft,
        onPrimaryContainer = palette.greenDeep,
        secondary = palette.leaf,
        background = palette.paper,
        onBackground = palette.ink,
        surface = palette.paperHi,
        onSurface = palette.ink,
        surfaceVariant = palette.greenSoft,
        onSurfaceVariant = palette.inkSoft,
        outline = palette.greenLine,
        error = palette.red
    )

    val typography = Typography(
        displayLarge = TextStyle(fontFamily = activeHandFont, fontWeight = FontWeight.Bold, fontSize = 34.sp),
        displayMedium = TextStyle(fontFamily = activeHandFont, fontWeight = FontWeight.Bold, fontSize = 28.sp),
        displaySmall = TextStyle(fontFamily = activeHandFont, fontWeight = FontWeight.Bold, fontSize = 22.sp),
        headlineLarge = TextStyle(fontFamily = activeHandFont, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
        headlineMedium = TextStyle(fontFamily = activeHandFont, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
        headlineSmall = TextStyle(fontFamily = activeUiFont, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
        titleLarge = TextStyle(fontFamily = activeUiFont, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
        titleMedium = TextStyle(fontFamily = activeUiFont, fontWeight = FontWeight.Medium, fontSize = 14.sp),
        titleSmall = TextStyle(fontFamily = activeUiFont, fontWeight = FontWeight.Medium, fontSize = 13.sp),
        bodyLarge = TextStyle(fontFamily = activeHandFont, fontWeight = FontWeight.Normal, fontSize = handwritingSp.sp, lineHeight = (handwritingSp + 10).sp),
        bodyMedium = TextStyle(fontFamily = activeUiFont, fontWeight = FontWeight.Normal, fontSize = 14.sp),
        bodySmall = TextStyle(fontFamily = activeUiFont, fontWeight = FontWeight.Normal, fontSize = 12.sp),
        labelLarge = TextStyle(fontFamily = activeUiFont, fontWeight = FontWeight.Medium, fontSize = 14.sp),
        labelMedium = TextStyle(fontFamily = activeUiFont, fontWeight = FontWeight.Normal, fontSize = 12.sp),
        labelSmall = TextStyle(fontFamily = activeUiFont, fontWeight = FontWeight.Normal, fontSize = 11.sp)
    )

    CompositionLocalProvider(
        LocalKannachPalette provides palette,
        LocalKannachLanguage provides language,
        LocalKannachHandFont provides activeHandFont,
        LocalKannachUiFont provides activeUiFont,
        LocalReduceMotionOverride provides reduceMotionOverride,
        LocalHapticsEnabled provides hapticsEnabled,
        LocalHandwritingSp provides handwritingSp,
        LocalLayoutDirection provides layoutDirection
    ) {
        MaterialTheme(
            colorScheme = m3Colors,
            typography = typography,
            content = content
        )
    }
}
