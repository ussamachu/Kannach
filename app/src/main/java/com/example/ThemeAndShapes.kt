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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
    val leaf: Color
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
    leaf = Color(0xFF5AA25F)
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

fun getSeasonalPalette(month: Int): KannachPalette {
    return when (month) {
        12, 1, 2 -> WinterPalette
        3, 4, 5 -> SpringPalette
        6, 7, 8 -> SummerPalette
        else -> AutumnPalette
    }
}

val LocalKannachPalette = staticCompositionLocalOf { BasePalette }
val LocalKannachLanguage = compositionLocalOf { "ar" }

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

/**
 * Custom Modifier providing warm craft paper background with subtle noise grain.
 */
fun Modifier.paperBackground(
    color: Color = Color(0xFFFBF9EF),
    drawNoise: Boolean = true
): Modifier = this.drawBehind {
    drawRect(color = color)
    if (drawNoise) {
        val step = 14f
        val cols = (size.width / step).toInt()
        val rows = (size.height / step).toInt()
        val noiseColor = Color(0xFF3A523D).copy(alpha = 0.024f)

        var seed = (cols * 31 + rows).toLong()
        for (i in 0 until cols) {
            for (j in 0 until rows) {
                seed = (seed * 1103515245L + 12345L) and 0x7fffffffL
                if (seed % 7L == 0L) {
                    val ox = i * step + (seed % 10L).toFloat()
                    val oy = j * step + ((seed / 10L) % 10L).toFloat()
                    drawCircle(
                        color = noiseColor,
                        radius = 1.1f,
                        center = Offset(ox, oy)
                    )
                }
            }
        }
    }
}

/**
 * Draws notebook ruled lines behind text (like repeating-linear-gradient).
 */
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
 * Draws a hand-drawn wavy border divider below headers, matching:
 * M0 4 C5 0, 10 8, 15 4 S25 0, 30 4 S38 8, 40 4
 */
fun Modifier.wavyHeaderDivider(
    color: Color,
    waveHeightDp: Dp = 8.dp
): Modifier = this.drawBehind {
    val waveH = waveHeightDp.toPx()
    val waveW = waveH * 5f // 40px width for 8px height
    val startY = size.height - waveH / 2f
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
 */
fun Modifier.dashedHandDrawnBorder(
    color: Color,
    shape: Shape = HandDrawnRadiusShape,
    strokeWidth: Dp = 2.dp,
    dashIntervals: FloatArray = floatArrayOf(14f, 10f)
): Modifier = this.drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    val strokeStyle = Stroke(
        width = strokeWidth.toPx(),
        pathEffect = PathEffect.dashPathEffect(dashIntervals, 0f),
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
    )
    when (outline) {
        is Outline.Generic -> drawPath(outline.path, color, style = strokeStyle)
        is Outline.Rounded -> drawRoundRect(
            color = color,
            topLeft = Offset(outline.roundRect.left, outline.roundRect.top),
            size = Size(outline.roundRect.width, outline.roundRect.height),
            cornerRadius = outline.roundRect.topLeftCornerRadius,
            style = strokeStyle
        )
        is Outline.Rectangle -> drawRect(color, style = strokeStyle)
    }
}

// Google Fonts locally installed via font-util
val PlaypenSansArabicFamily = FontFamily(Font(R.font.playpen_sans_arabic, FontWeight.Normal))
val PlaypenSansFamily = FontFamily(Font(R.font.playpen_sans, FontWeight.Normal))
val IbmPlexArabicFamily = FontFamily(Font(R.font.ibm_plex_sans_arabic, FontWeight.Normal))
val SpaceGroteskFamily = FontFamily(Font(R.font.space_grotesk, FontWeight.Normal))

/**
 * Authentic handwritten font for journal entries, notes, and titles.
 * Contains ONLY Playpen Sans Arabic to ensure Arabic handwriting renders flawlessly without fallback issues.
 */
val KannachHandFont = FontFamily(
    Font(R.font.playpen_sans_arabic, FontWeight.Normal)
)

/**
 * Clean UI font for labels, buttons, and app metadata.
 * Pairs IBM Plex Sans Arabic and Space Grotesk.
 */
val KannachUiFont = FontFamily(
    Font(R.font.ibm_plex_sans_arabic, FontWeight.Normal),
    Font(R.font.space_grotesk, FontWeight.Normal)
)

val LocalKannachHandFont = staticCompositionLocalOf { KannachHandFont }
val LocalKannachUiFont = staticCompositionLocalOf { KannachUiFont }

/**
 * Root theme composable for Kannach.
 * Handles dynamic RTL/LTR directionality, typography, and seasonal color palette updates.
 */
@Composable
fun KannachTheme(
    language: String = "ar",
    month: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    content: @Composable () -> Unit
) {
    val palette = getSeasonalPalette(month)
    val isRtl = I18n.isRtl(language)
    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    val activeHandFont = if (isRtl) KannachHandFont else PlaypenSansFamily
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
        bodyLarge = TextStyle(fontFamily = activeHandFont, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 28.sp),
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
        LocalLayoutDirection provides layoutDirection
    ) {
        MaterialTheme(
            colorScheme = m3Colors,
            typography = typography,
            content = content
        )
    }
}
