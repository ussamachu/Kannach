package com.example

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 1. GlyphView:
 * Renders normalized strokes on a 300x300 virtual canvas using quadratic Bezier curves,
 * replicating SVG <g stroke-width="11" stroke-linecap="round" stroke-linejoin="round">.
 */
@Composable
fun GlyphView(
    strokes: List<List<Point>>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val scale = size.width / StrokeMath.NORMALIZED_BOX_SIZE
        val strokeWidth = 11f * scale
        val strokeStyle = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        for (stroke in strokes) {
            if (stroke.isNotEmpty()) {
                val path = StrokeMath.composePathFrom(stroke)
                val matrix = androidx.compose.ui.graphics.Matrix().apply {
                    scale(scale, scale)
                }
                path.transform(matrix)
                drawPath(path = path, color = color, style = strokeStyle)
            }
        }
    }
}

/**
 * R1: Progressive stroke-by-stroke drawing of the sprout icon:
 * 1. Stem: M12 21V10.5
 * 2. Left leaf: M12 11c0-3.5-2.5-6-6.8-6.6C4.6 8.9 6.8 12 12 11z
 * 3. Right leaf: M12 11c0-3 2.2-5.3 6.1-5.8.6 3.7-1.3 6.3-6.1 5.8z
 * Animates across ~900ms via PathMeasure matching HTML stroke-dashoffset.
 */
@Composable
fun AnimatedSprout(
    progress: Float,
    tint: Color,
    modifier: Modifier = Modifier,
    strokeWidthDp: androidx.compose.ui.unit.Dp = 2.8.dp
) {
    val pm = remember { PathMeasure() }

    // Deconstruct overall progress [0f..1f] into sequential growth phases
    val stemProg = (progress / 0.35f).coerceIn(0f, 1f)
    val leftProg = ((progress - 0.28f) / 0.38f).coerceIn(0f, 1f)
    val rightProg = ((progress - 0.58f) / 0.42f).coerceIn(0f, 1f)

    Canvas(modifier = modifier) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val stroke = Stroke(
            width = strokeWidthDp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        fun parse(pathData: String): Path {
            val nodes = PathParser().parsePathString(pathData).toNodes()
            val path = Path()
            PathParser().addPathNodes(nodes).toPath(path)
            val matrix = Matrix().apply {
                scale(x = sx, y = sy)
            }
            path.transform(matrix)
            return path
        }

        val stemPath = parse("M12 21V10.5")
        val leftLeafPath = parse("M12 11c0-3.5-2.5-6-6.8-6.6C4.6 8.9 6.8 12 12 11z")
        val rightLeafPath = parse("M12 11c0-3 2.2-5.3 6.1-5.8.6 3.7-1.3 6.3-6.1 5.8z")

        if (stemProg > 0f) {
            val seg = Path()
            pm.setPath(stemPath, forceClosed = false)
            pm.getSegment(0f, stemProg * pm.length, seg, true)
            drawPath(seg, tint, style = stroke)
        }

        if (leftProg > 0f) {
            val seg = Path()
            pm.setPath(leftLeafPath, forceClosed = false)
            pm.getSegment(0f, leftProg * pm.length, seg, true)
            drawPath(seg, tint, style = stroke)
        }

        if (rightProg > 0f) {
            val seg = Path()
            pm.setPath(rightLeafPath, forceClosed = false)
            pm.getSegment(0f, rightProg * pm.length, seg, true)
            drawPath(seg, tint, style = stroke)
        }
    }
}

/**
 * 2. SplashScreen (R1):
 * Draws the sprout stroke-by-stroke over ~900ms via PathMeasure,
 * then fades in the app title, rests briefly, and dissolves into the main experience.
 */
@Composable
fun SplashScreen(
    appName: String,
    onFinished: () -> Unit
) {
    val palette = LocalKannachPalette.current
    val reduceMotion = rememberReduceMotion()
    var visible by remember { mutableStateOf(true) }
    val sproutProgress = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val titleAlpha = remember { Animatable(if (reduceMotion) 1f else 0f) }

    LaunchedEffect(Unit) {
        if (!reduceMotion) {
            // Draw sprout stroke by stroke over ~900ms
            sproutProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = KannachMotion.CEREMONY,
                    easing = KannachMotion.Settle
                )
            )
            // Fade in app title smoothly
            titleAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = KannachMotion.SOFT,
                    easing = KannachMotion.Settle
                )
            )
            delay(550)
        } else {
            delay(600)
        }
        visible = false
        delay(KannachMotion.SOFT.toLong())
        onFinished()
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(animationSpec = tween(KannachMotion.SOFT, easing = KannachMotion.Settle))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .paperBackground(palette.paper)
                .clickable(enabled = false) {},
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedSprout(
                    progress = sproutProgress.value,
                    tint = palette.greenDeep,
                    strokeWidthDp = 2.8.dp,
                    modifier = Modifier.size(96.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = appName,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.greenDeep,
                    fontFamily = LocalKannachHandFont.current,
                    modifier = Modifier.alpha(titleAlpha.value)
                )
            }
        }
    }
}

/**
 * 3. AppHeader (H1, H2, H4):
 * Single calm header line like a notebook page heading:
 * - Large month title in handwriting font with season icon and year.
 * - Arrow steppers with >=48dp touch targets.
 * - Hand-drawn Bookmark Ribbon in the corner that reveals an elegant Paper Flap menu.
 * - Gentle heart button to toggle favorites filter.
 * - Poetic time-of-day greeting under the month title.
 */
@Composable
fun AppHeader(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalKannachPalette.current
    val language by viewModel.language.collectAsState()
    val strings = I18n.forLanguage(language)
    val isRtl = I18n.isRtl(language)

    val currentYear by viewModel.currentYear.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val entries by viewModel.entries.collectAsState()
    val filter by viewModel.gridFilter.collectAsState()

    val season = I18n.seasonOf(currentMonth)
    val seasonIcon = when (season) {
        "winter" -> KannachIconType.WINTER
        "spring" -> KannachIconType.SPRING
        "summer" -> KannachIconType.SUMMER
        else -> KannachIconType.AUTUMN
    }

    val isAtMinYear = currentYear <= viewModel.getMinYear() && currentMonth <= 1
    val calendar = Calendar.getInstance()
    val nowYear = calendar.get(Calendar.YEAR)
    val nowMonth = calendar.get(Calendar.MONTH) + 1
    val isAtMaxYear = currentYear >= nowYear && currentMonth >= nowMonth

    // H4: Greeting logic based on time of day & entry status
    val todayKey = viewModel.getTodayKey()
    val hasTodayEntry = entries[todayKey] != null

    val isWelcomeBack = remember(entries, todayKey) {
        if (entries.isEmpty()) return@remember false
        val writtenDates = entries.keys.filter { it < todayKey }.sortedDescending()
        if (writtenDates.isEmpty()) return@remember false
        val lastDate = writtenDates.first()
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dLast = sdf.parse(lastDate)?.time ?: return@remember false
            val dToday = sdf.parse(todayKey)?.time ?: return@remember false
            val diffDays = (dToday - dLast) / (1000 * 60 * 60 * 24)
            diffDays >= 7
        } catch (_: Exception) {
            false
        }
    }

    val greetingText = remember(hasTodayEntry, isWelcomeBack, language) {
        if (hasTodayEntry) {
            strings.greetDone
        } else if (isWelcomeBack) {
            strings.greetBack
        } else {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            when (hour) {
                in 5..11 -> strings.greetMorning
                in 12..16 -> strings.greetDay
                in 17..21 -> strings.greetEvening
                else -> strings.greetNight
            }
        }
    }

    var greetingVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(600)
        greetingVisible = true
        delay(6000)
        greetingVisible = false
    }

    var isFlapOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .paperBackground(palette.paper)
            .wavyHeaderDivider(palette.greenLine, waveHeightDp = 6.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Single calm notebook header line (H1)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Month Stepper: Prev Arrow (min 48.dp touch target)
            IconButton(
                onClick = { viewModel.shiftMonth(-1) },
                enabled = !isAtMinYear,
                modifier = Modifier.size(48.dp)
            ) {
                KannachIcon(
                    icon = if (isRtl) KannachIconType.CHEV_R else KannachIconType.CHEV_L,
                    tint = if (isAtMinYear) palette.inkFaint.copy(alpha = 0.35f) else palette.ink,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Month Name & Year in Center
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val monthName = strings.months.getOrElse(currentMonth - 1) { "" }
                    Text(
                        text = monthName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = LocalKannachHandFont.current,
                        color = palette.greenDeep
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(palette.greenSoft.copy(alpha = 0.5f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        KannachIcon(
                            icon = seasonIcon,
                            tint = palette.greenDeep,
                            strokeWidthDp = 1.6.dp,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = I18n.formatYear(currentYear, language),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.greenDeep
                        )
                    }
                }

                // H4: Poetic Greeting under Month Title
                AnimatedVisibility(
                    visible = greetingVisible,
                    enter = fadeIn(animationSpec = tween(KannachMotion.SOFT)),
                    exit = fadeOut(animationSpec = tween(KannachMotion.PAGE))
                ) {
                    Text(
                        text = greetingText,
                        fontSize = 12.sp,
                        fontFamily = LocalKannachHandFont.current,
                        color = palette.inkFaint,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Month Stepper: Next Arrow (min 48.dp touch target)
            IconButton(
                onClick = { viewModel.shiftMonth(1) },
                enabled = !isAtMaxYear,
                modifier = Modifier.size(48.dp)
            ) {
                KannachIcon(
                    icon = if (isRtl) KannachIconType.CHEV_L else KannachIconType.CHEV_R,
                    tint = if (isAtMaxYear) palette.inkFaint.copy(alpha = 0.35f) else palette.ink,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Quiet Heart Button for Favorites Filter (H3, min 48.dp touch target)
            val isFavActive = filter == "fav"
            IconButton(
                onClick = {
                    viewModel.setGridFilter(if (isFavActive) "all" else "fav")
                    viewModel.vibrate(6)
                },
                modifier = Modifier.size(48.dp)
            ) {
                KannachIcon(
                    icon = KannachIconType.HEART,
                    tint = if (isFavActive) palette.red else palette.inkFaint,
                    filled = isFavActive,
                    fillColor = palette.red.copy(alpha = 0.35f),
                    modifier = Modifier.size(22.dp)
                )
            }

            // Bookmark Ribbon Button (H2, min 48.dp touch target)
            Box {
                IconButton(
                    onClick = {
                        isFlapOpen = !isFlapOpen
                        viewModel.vibrate(6)
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    KannachIcon(
                        icon = KannachIconType.RIBBON,
                        tint = if (isFlapOpen) palette.greenDeep else palette.ink,
                        filled = isFlapOpen,
                        fillColor = palette.greenSoft,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Paper Flap Dropdown Menu (H2)
                if (isFlapOpen) {
                    Popup(
                        alignment = if (isRtl) Alignment.TopStart else Alignment.TopEnd,
                        offset = IntOffset(x = if (isRtl) 16 else -16, y = 110),
                        onDismissRequest = { isFlapOpen = false },
                        properties = PopupProperties(focusable = true)
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(min = 210.dp, max = 250.dp)
                                .shadow(12.dp, HandDrawnRadiusShape)
                                .clip(HandDrawnRadiusShape)
                                .background(palette.paperHi)
                                .border(1.5.dp, palette.ink, HandDrawnRadiusShape)
                                .paperBackground(palette.paperHi)
                                .padding(vertical = 6.dp, horizontal = 4.dp)
                        ) {
                            Column {
                                // 1. Capsule (رسالة إلى الغد)
                                PaperFlapItem(
                                    icon = KannachIconType.SEAL,
                                    title = strings.capsuleTitle,
                                    onClick = {
                                        isFlapOpen = false
                                        viewModel.openCapsulePage()
                                    }
                                )

                                // 2. Gallery (الحديقة)
                                PaperFlapItem(
                                    icon = KannachIconType.GRID,
                                    title = strings.galleryTitle,
                                    onClick = {
                                        isFlapOpen = false
                                        viewModel.openGallery()
                                    }
                                )

                                // 3. Draw (مرسم حر)
                                PaperFlapItem(
                                    icon = KannachIconType.BRUSH,
                                    title = strings.sketchTitle,
                                    onClick = {
                                        isFlapOpen = false
                                        viewModel.openSketchPage()
                                    }
                                )

                                // 4. Favorites (المفضلة)
                                PaperFlapItem(
                                    icon = KannachIconType.HEART,
                                    title = strings.favorites,
                                    isHighlight = filter == "fav",
                                    onClick = {
                                        isFlapOpen = false
                                        viewModel.setGridFilter(if (filter == "fav") "all" else "fav")
                                    }
                                )

                                // 5. Settings (الإعدادات)
                                PaperFlapItem(
                                    icon = KannachIconType.SLIDERS,
                                    title = strings.settings,
                                    onClick = {
                                        isFlapOpen = false
                                        viewModel.openSettings()
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

@Composable
private fun PaperFlapItem(
    icon: KannachIconType,
    title: String,
    isHighlight: Boolean = false,
    onClick: () -> Unit
) {
    val palette = LocalKannachPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        KannachIcon(
            icon = icon,
            tint = if (isHighlight) palette.red else palette.greenDeep,
            filled = isHighlight,
            fillColor = if (isHighlight) palette.red.copy(alpha = 0.35f) else Color.Transparent,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontFamily = LocalKannachHandFont.current,
            color = palette.ink,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Dashed banner component (used for On This Day & Capsule Ready).
 */
@Composable
fun DashedBanner(
    title: String,
    subtitle: String? = null,
    onBannerClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalKannachPalette.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(HandDrawnRadiusShape)
            .background(palette.greenSoft)
            .dashedHandDrawnBorder(palette.greenLine, HandDrawnRadiusShape)
            .clickable { onBannerClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.padding(end = 28.dp)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = palette.greenDeep
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = palette.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(
            onClick = onCloseClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
        ) {
            KannachIcon(
                icon = KannachIconType.X,
                tint = palette.inkFaint,
                strokeWidthDp = 1.5.dp,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * E1, E2, E3: Folded-paper corner (Dog-ear) in the top-end corner of the month page.
 * Replaces disruptive banners with a quiet tactile folded page corner that flutters once (1000ms).
 * Priority 1: Ready Capsule (SEAL icon).
 * Priority 2: On This Day (SPARKLE icon).
 */
@Composable
fun FoldedPaperCorner(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val readyCapsule by viewModel.readyCapsule.collectAsState()
    val onThisDayHit by viewModel.onThisDayHit.collectAsState()
    val language by viewModel.language.collectAsState()
    val isRtl = I18n.isRtl(language)
    val palette = LocalKannachPalette.current
    val reduceMotion = rememberReduceMotion()

    val activeItem = when {
        readyCapsule != null -> "capsule"
        onThisDayHit != null -> "onThisDay"
        else -> null
    } ?: return

    val flutterAnim = remember { Animatable(0f) }
    LaunchedEffect(activeItem) {
        if (!reduceMotion) {
            flutterAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(1000, easing = KannachMotion.Breathe)
            )
        }
    }

    val foldSize = 52.dp
    var dragAmountX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .size(foldSize)
            .pointerInput(activeItem) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAmountX = 0f },
                    onHorizontalDrag = { _, dragAmount -> dragAmountX += dragAmount },
                    onDragEnd = {
                        if (kotlin.math.abs(dragAmountX) > 24f) {
                            viewModel.vibrate(8)
                            if (activeItem == "capsule") {
                                viewModel.dismissReadyCapsule()
                            } else {
                                viewModel.dismissOnThisDay()
                            }
                        }
                    }
                )
            }
            .clickable {
                viewModel.vibrate(10)
                if (activeItem == "capsule") {
                    readyCapsule?.let { viewModel.openReadyCapsule(it) }
                } else {
                    onThisDayHit?.let { viewModel.openSheet(it.key) }
                }
            }
    ) {
        val flutterRotation = if (!reduceMotion) {
            kotlin.math.sin(flutterAnim.value * Math.PI.toFloat() * 2f) * 4f
        } else 0f

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(flutterRotation)
        ) {
            val w = size.width
            val h = size.height

            val flapPath = Path().apply {
                if (isRtl) {
                    moveTo(0f, 0f)
                    lineTo(w, 0f)
                    lineTo(0f, h)
                    close()
                } else {
                    moveTo(w, 0f)
                    lineTo(0f, 0f)
                    lineTo(w, h)
                    close()
                }
            }

            drawPath(
                path = flapPath,
                color = palette.ink.copy(alpha = 0.12f)
            )
            drawPath(
                path = flapPath,
                color = if (activeItem == "capsule") palette.red.copy(alpha = 0.22f) else palette.greenSoft
            )
            drawPath(
                path = flapPath,
                color = if (activeItem == "capsule") palette.red else palette.greenLine,
                style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Box(
            modifier = Modifier
                .align(if (isRtl) Alignment.TopStart else Alignment.TopEnd)
                .padding(if (isRtl) PaddingValues(start = 7.dp, top = 7.dp) else PaddingValues(end = 7.dp, top = 7.dp))
        ) {
            KannachIcon(
                icon = if (activeItem == "capsule") KannachIconType.SEAL else KannachIconType.SPARKLE,
                tint = if (activeItem == "capsule") palette.red else palette.orange,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

/**
 * 4. MonthGrid:
 * 7-column calendar grid displaying days with organic rotation, pulsing ring for today,
 * connecting chain lines between consecutive filled days, and horizontal month swiping.
 */
@Composable
fun MonthGrid(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalKannachPalette.current
    val language by viewModel.language.collectAsState()
    val isRtl = I18n.isRtl(language)
    val strings = I18n.forLanguage(language)

    val currentYear by viewModel.currentYear.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val filter by viewModel.gridFilter.collectAsState()
    val entries by viewModel.entries.collectAsState()

    val todayKey = viewModel.getTodayKey()

    val daysInMonth = remember(currentYear, currentMonth) {
        val cal = Calendar.getInstance()
        cal.set(currentYear, currentMonth - 1, 1)
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val onThisDayHit by viewModel.onThisDayHit.collectAsState()
    val readyCapsule by viewModel.readyCapsule.collectAsState()

    // H5 & H9: Slow, calm breathing animation for today (3200ms Breathe curve)
    val hasTodayEntry = entries[todayKey] != null
    val todayBreathingTransition = rememberInfiniteTransition(label = "todayBreathe")
    val todayScale by todayBreathingTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (!hasTodayEntry) 1.06f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = KannachMotion.Breathe),
            repeatMode = RepeatMode.Reverse
        ),
        label = "todayScale"
    )

    val firstDayOfWeekOffset = remember(currentYear, currentMonth) {
        val cal = Calendar.getInstance()
        cal.set(currentYear, currentMonth - 1, 1)
        (cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY + 7) % 7
    }

    // Gesture detection for horizontal month swiping
    var totalDragX by remember { mutableStateOf(0f) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(currentYear, currentMonth, isRtl) {
                    detectHorizontalDragGestures(
                        onDragStart = { totalDragX = 0f },
                        onHorizontalDrag = { _, dragAmount -> totalDragX += dragAmount },
                        onDragEnd = {
                            if (totalDragX > 70f) {
                                val delta = if (isRtl) 1 else -1
                                viewModel.shiftMonth(delta)
                            } else if (totalDragX < -70f) {
                                val delta = if (isRtl) -1 else 1
                                viewModel.shiftMonth(delta)
                            }
                        }
                    )
                }
                .padding(horizontal = 12.dp)
                .padding(top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.Top
        ) {

        // Weekdays Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            val weekDays = if (isRtl) {
                listOf("أحد", "إثن", "ثلا", "أرب", "خمي", "جمع", "سبت")
            } else {
                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            }
            weekDays.forEach { wDay ->
                Text(
                    text = wDay,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.inkFaint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 7-Column Grid of Month Days with Organic Ground Lines (H6), Page Flip (D3, H8) & Connecting Vines (H7)
        val density = LocalDensity.current
        val reduceMotion = rememberReduceMotion()
        val pageSlideDistance = remember(density) { with(density) { 24.dp.roundToPx() } }

        AnimatedContent(
            targetState = Pair(currentYear, currentMonth),
            transitionSpec = {
                if (reduceMotion) {
                    fadeIn(animationSpec = tween(KannachMotion.INSTANT)) togetherWith
                        fadeOut(animationSpec = tween(KannachMotion.INSTANT))
                } else {
                    val isForward = (targetState.first * 12 + targetState.second) > (initialState.first * 12 + initialState.second)
                    val sign = if (isForward) {
                        if (isRtl) -1 else 1
                    } else {
                        if (isRtl) 1 else -1
                    }
                    val enterOffsetX = pageSlideDistance * sign
                    val exitOffsetX = -pageSlideDistance * sign

                    (slideInHorizontally(
                        animationSpec = tween(KannachMotion.PAGE, easing = KannachMotion.Settle),
                        initialOffsetX = { enterOffsetX }
                    ) + fadeIn(
                        animationSpec = tween(KannachMotion.PAGE, easing = KannachMotion.Settle)
                    )).togetherWith(
                        slideOutHorizontally(
                            animationSpec = tween(KannachMotion.PAGE, easing = KannachMotion.Settle),
                            targetOffsetX = { exitOffsetX }
                        ) + fadeOut(
                            animationSpec = tween(KannachMotion.PAGE, easing = KannachMotion.Settle)
                        )
                    )
                }
            },
            label = "MonthPageFlip"
        ) { (targetYear, targetMonth) ->
            val daysInThisMonth = remember(targetYear, targetMonth) {
                val cal = Calendar.getInstance()
                cal.set(targetYear, targetMonth - 1, 1)
                cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            }
            val firstDayOffset = remember(targetYear, targetMonth) {
                val cal = Calendar.getInstance()
                cal.set(targetYear, targetMonth - 1, 1)
                (cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY + 7) % 7
            }

            // H7: Vine draws progressively once when month is displayed (PAGE ms)
            val vineAnim = remember { Animatable(0f) }
            LaunchedEffect(targetYear, targetMonth) {
                vineAnim.snapTo(0f)
                vineAnim.animateTo(1f, tween(KannachMotion.PAGE, easing = KannachMotion.Settle))
            }

            val totalCells = firstDayOffset + daysInThisMonth
            val numWeeks = (totalCells + 6) / 7

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (weekIndex in 0 until numWeeks) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (col in 0 until 7) {
                                val cellIndex = weekIndex * 7 + col
                                if (cellIndex < firstDayOffset || (cellIndex - firstDayOffset) >= daysInThisMonth) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(0.82f)
                                    )
                                } else {
                                    val day = cellIndex - firstDayOffset + 1
                                    val dateKey = String.format(java.util.Locale.US, "%04d-%02d-%02d", targetYear, targetMonth, day)
                                    val isToday = dateKey == todayKey
                                    val isFuture = dateKey > todayKey
                                    val entry = entries[dateKey]

                                    val rot = when {
                                        day % 3 == 0 -> -2.5f
                                        day % 4 == 0 -> 2f
                                        day % 5 == 0 -> -1.5f
                                        day % 7 == 0 -> 2f
                                        else -> 0f
                                    }

                                    val isDimmed = entry != null && filter == "fav" && !entry.fav

                                    val prevDateKey = if (day > 1) String.format(java.util.Locale.US, "%04d-%02d-%02d", targetYear, targetMonth, day - 1) else null
                                    val nextDateKey = if (day < daysInThisMonth) String.format(java.util.Locale.US, "%04d-%02d-%02d", targetYear, targetMonth, day + 1) else null

                                    val hasPrevConsecutive = prevDateKey != null && entries[prevDateKey] != null && entry != null && col > 0
                                    val hasNextConsecutive = nextDateKey != null && entries[nextDateKey] != null && entry != null && col < 6

                                    DayCell(
                                        viewModel = viewModel,
                                        day = day,
                                        dateKey = dateKey,
                                        entry = entry,
                                        isToday = isToday,
                                        isFuture = isFuture,
                                        isDimmed = isDimmed,
                                        rotation = rot,
                                        todayScale = todayScale,
                                        hasPrevConsecutive = hasPrevConsecutive,
                                        hasNextConsecutive = hasNextConsecutive,
                                        vineProgress = vineAnim.value,
                                        isRtl = isRtl,
                                        todayWord = strings.todayWord,
                                        onClick = { viewModel.openSheet(dateKey) },
                                        onFutureClick = {
                                            viewModel.vibrate(10)
                                            viewModel.showToast(strings.notYet)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // H6: Delicate, organic hand-drawn faint ground line under each week row
                        OrganicGroundLine(color = palette.inkFaint.copy(alpha = 0.2f))
                    }
                }
            }
        }
        }

        // E1, E2, E3: Folded-paper corner event
        FoldedPaperCorner(
            viewModel = viewModel,
            modifier = Modifier
                .align(if (isRtl) Alignment.TopStart else Alignment.TopEnd)
                .padding(top = 2.dp, start = 8.dp, end = 8.dp)
        )
    }
}

/**
 * H6: Delicate organic hand-drawn ground line under each week row.
 * Simulates a faint notebook ruled line with gentle natural waviness.
 */
@Composable
fun OrganicGroundLine(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(5.dp)
            .padding(vertical = 1.dp)
    ) {
        val w = size.width
        val midY = size.height * 0.5f
        val path = Path().apply {
            moveTo(0f, midY)
            val segments = 6
            val step = w / segments
            for (i in 0 until segments) {
                val startX = i * step
                val endX = (i + 1) * step
                val wave = if (i % 2 == 0) -1.2f else 1.2f
                quadraticBezierTo(
                    (startX + endX) / 2f, midY + wave,
                    endX, midY
                )
            }
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        )
    }
}

/**
 * H9: Tiny hand-drawn arrow pointing to today's cell with handwritten "اليوم"
 */
@Composable
private fun TodayPointer(
    todayWord: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalKannachPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.size(width = 8.dp, height = 9.dp)) {
            val stroke = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            val p = Path().apply {
                moveTo(size.width * 0.5f, size.height)
                lineTo(size.width * 0.5f, 1.5.dp.toPx())
                moveTo(1.dp.toPx(), 3.5.dp.toPx())
                lineTo(size.width * 0.5f, 1.5.dp.toPx())
                lineTo(size.width - 1.dp.toPx(), 3.5.dp.toPx())
            }
            drawPath(p, palette.greenDeep, style = stroke)
        }
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = todayWord,
            fontSize = 9.sp,
            fontFamily = LocalKannachHandFont.current,
            fontWeight = FontWeight.Bold,
            color = palette.greenDeep
        )
    }
}

/**
 * W3 & R3: Delicate petal burst blossoming around the cell upon landing.
 * If isDoubleRing is true (celebrating user's very first mark in history),
 * blossoms with a celebratory double ring of outer petals!
 */
@Composable
private fun PetalBurst(
    color: Color,
    isDoubleRing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        anim.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = if (isDoubleRing) 850 else 650,
                easing = KannachMotion.Settle
            )
        )
    }
    val progress = anim.value
    val alpha = (1f - progress).coerceIn(0f, 1f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val startRadius = 4.dp.toPx()
        val endRadius = if (isDoubleRing) 28.dp.toPx() else 24.dp.toPx()
        val currentRadius = startRadius + (endRadius - startRadius) * progress
        val petalWidth = 2.8.dp.toPx() * (1f - progress * 0.25f)
        val petalHeight = 5.5.dp.toPx() * (1f - progress * 0.25f)

        // Ring 1 (6 petals)
        for (i in 0 until 6) {
            val angleDeg = i * 60f
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val px = centerX + (currentRadius * cos(angleRad)).toFloat()
            val py = centerY + (currentRadius * sin(angleRad)).toFloat()

            rotate(degrees = angleDeg + 90f, pivot = Offset(px, py)) {
                drawOval(
                    color = color.copy(alpha = alpha * 0.85f),
                    topLeft = Offset(px - petalWidth / 2f, py - petalHeight / 2f),
                    size = Size(petalWidth, petalHeight)
                )
            }
        }

        // Ring 2: Doubled celebration ring (8 outer petals) for the first plant in history (R3)
        if (isDoubleRing) {
            val outerRadius = startRadius + (38.dp.toPx() - startRadius) * (progress * 0.95f)
            val outerPetalW = petalWidth * 0.85f
            val outerPetalH = petalHeight * 0.85f
            for (i in 0 until 8) {
                val angleDeg = i * 45f + 22.5f
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val px = centerX + (outerRadius * cos(angleRad)).toFloat()
                val py = centerY + (outerRadius * sin(angleRad)).toFloat()

                rotate(degrees = angleDeg + 90f, pivot = Offset(px, py)) {
                    drawOval(
                        color = color.copy(alpha = alpha * 0.75f),
                        topLeft = Offset(px - outerPetalW / 2f, py - outerPetalH / 2f),
                        size = Size(outerPetalW, outerPetalH)
                    )
                }
            }
        }
    }
}

/**
 * Single day cell in the month calendar.
 * - H6: No square background or borders on written days; pressed flowers live directly on paper.
 * - H6: Day numbers are at least 11.sp.
 * - H5 & H9: Slow 3200ms breathing for today; unwritten today displays todayWord + arrow.
 * - H7: Delicate connecting vine runner between consecutive written days.
 * - H10: Future day click triggers a gentle 2.dp horizontal wobble and shows notYet toast.
 * - W3: Records cell bounds for ceremony flight; displays bloom & petal burst on landing.
 */
@Composable
private fun DayCell(
    viewModel: MainViewModel,
    day: Int,
    dateKey: String,
    entry: DayEntry?,
    isToday: Boolean,
    isFuture: Boolean,
    isDimmed: Boolean,
    rotation: Float,
    todayScale: Float,
    hasPrevConsecutive: Boolean,
    hasNextConsecutive: Boolean,
    vineProgress: Float,
    isRtl: Boolean,
    todayWord: String,
    onClick: () -> Unit,
    onFutureClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalKannachPalette.current
    val coroutineScope = rememberCoroutineScope()
    val wobbleX = remember { Animatable(0f) }

    val plantedCellPulse by viewModel.plantedCellPulse.collectAsState()
    val isFreshlyPlanted = plantedCellPulse == dateKey

    LaunchedEffect(isFreshlyPlanted) {
        if (isFreshlyPlanted) {
            delay(750)
            viewModel.clearPlantedCellPulse()
        }
    }

    // Spring bloom animation for glyph when present or freshly planted
    val bloomScale by animateFloatAsState(
        targetValue = if (entry != null) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bloomScale"
    )

    val cellShape = RoundedCornerShape(10.dp)

    Box(
        modifier = modifier
            .aspectRatio(0.82f)
            .offset { IntOffset(wobbleX.value.dp.roundToPx(), 0) }
            .clip(cellShape)
            .onGloballyPositioned { coords ->
                viewModel.registerCellBounds(dateKey, coords.positionInRoot(), coords.size)
            }
            // H6: No square backgrounds or borders; only isToday has a gentle outline
            .then(
                if (isToday) {
                    Modifier
                        .background(palette.paperHi.copy(alpha = 0.5f))
                        .border(1.2.dp, palette.green, cellShape)
                } else {
                    Modifier.background(Color.Transparent)
                }
            )
            .clickable {
                if (isFuture) {
                    coroutineScope.launch {
                        wobbleX.animateTo(2.5f, tween(50))
                        wobbleX.animateTo(-2.5f, tween(50))
                        wobbleX.animateTo(1.5f, tween(50))
                        wobbleX.animateTo(0f, tween(50))
                    }
                    onFutureClick()
                } else {
                    onClick()
                }
            }
    ) {
        // H7: Delicate connecting vine runner drawn between consecutive written days
        if (entry != null && (hasPrevConsecutive || hasNextConsecutive) && vineProgress > 0.05f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round)
                val vineColor = palette.green.copy(alpha = 0.45f)
                val centerY = size.height * 0.52f

                if (hasPrevConsecutive) {
                    val startX = if (isRtl) size.width else 0f
                    val midX = size.width * 0.5f
                    val path = Path().apply {
                        moveTo(startX, centerY + 3.dp.toPx())
                        quadraticBezierTo(
                            (startX + midX) / 2f, centerY - 5.dp.toPx(),
                            midX, centerY
                        )
                    }
                    drawPath(path, vineColor, style = stroke)
                }

                if (hasNextConsecutive) {
                    val endX = if (isRtl) 0f else size.width
                    val midX = size.width * 0.5f
                    val currentEndX = midX + (endX - midX) * vineProgress
                    val path = Path().apply {
                        moveTo(midX, centerY)
                        quadraticBezierTo(
                            (midX + endX) / 2f, centerY + 5.dp.toPx(),
                            currentEndX, centerY + 3.dp.toPx()
                        )
                    }
                    drawPath(path, vineColor, style = stroke)
                }
            }
        }

        // Day number in top corner (H6: >=11.sp)
        Text(
            text = day.toString(),
            fontSize = if (isToday) 11.5.sp else 11.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
            color = if (isToday) palette.greenDeep else palette.inkFaint.copy(alpha = if (isFuture) 0.35f else 0.7f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
        )

        // Center content: Pressed Flower GlyphView OR Breathing Sprout with Today Arrow OR Faint Dot
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp, bottom = 4.dp, start = 2.dp, end = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            if (entry != null && !isFuture) {
                val glyphColor = try {
                    Color(android.graphics.Color.parseColor(entry.color))
                } catch (_: Exception) {
                    palette.green
                }

                GlyphView(
                    strokes = entry.strokes,
                    color = glyphColor,
                    modifier = Modifier
                        .fillMaxSize(0.85f)
                        .scale(bloomScale)
                        .rotate(rotation)
                        .alpha(if (isDimmed) 0.16f else 1.0f)
                )

                if (isFreshlyPlanted) {
                    val firstCelebratedDate by viewModel.firstPlantCelebratedDate.collectAsState()
                    val isDoubleRing = firstCelebratedDate == dateKey
                    PetalBurst(color = glyphColor, isDoubleRing = isDoubleRing)
                }
            } else if (isToday) {
                // H5 & H9: Breathing sprout + TodayPointer
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    KannachIcon(
                        icon = KannachIconType.SPROUT,
                        tint = palette.green,
                        modifier = Modifier
                            .size(16.dp)
                            .scale(todayScale)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    TodayPointer(todayWord = todayWord)
                }
            } else {
                // Subtle delicate dot for unwritten days
                val dotColor = if (isFuture) palette.dot.copy(alpha = 0.25f) else palette.dot
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }
    }
}

/**
 * 5. EntrySheetOverlay:
 * Full overlay supporting:
 * - View mode ("Memory card"): centered modal with blurred backdrop scrim, doodle frame tilted -0.7deg, and ruled notebook text.
 * - Edit mode: Bottom sheet with grab handle, interactive touch drawing board, undo/clear, color palette, auto-growing text, and save/delete actions.
 */
@Composable
fun EntrySheetOverlay(
    viewModel: MainViewModel
) {
    val activeDateKey by viewModel.activeSheetDate.collectAsState()
    var currentOrExitDateKey by remember { mutableStateOf<String?>(null) }
    if (activeDateKey != null) {
        currentOrExitDateKey = activeDateKey
    }

    val isOpen = activeDateKey != null && currentOrExitDateKey != null
    val dateKey = currentOrExitDateKey ?: ""
    val mode by viewModel.sheetMode.collectAsState()
    val palette = LocalKannachPalette.current
    val language by viewModel.language.collectAsState()
    val strings = I18n.forLanguage(language)
    val formattedDate = remember(dateKey, language) {
        if (dateKey.isNotBlank()) I18n.formatDateTitle(dateKey, language) else ""
    }
    val isToday = dateKey == viewModel.getTodayKey()
    val reduceMotion = rememberReduceMotion()

    BackHandler(enabled = isOpen) {
        if (mode == SheetMode.EDIT && viewModel.writeStep.value == 2) {
            viewModel.setWriteStep(1)
        } else {
            viewModel.requestCloseSheet()
        }
    }

    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn(animationSpec = tween(if (reduceMotion) KannachMotion.INSTANT else KannachMotion.SOFT)),
        exit = fadeOut(animationSpec = tween(if (reduceMotion) KannachMotion.INSTANT else KannachMotion.SOFT))
    ) {
        // Outer Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.scrim.copy(alpha = if (mode == SheetMode.VIEW) 0.65f else 0.52f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    viewModel.requestCloseSheet()
                },
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = mode == SheetMode.VIEW,
                enter = kannachCardEnter(reduceMotion),
                exit = kannachCardExit(reduceMotion)
            ) {
                val density = LocalDensity.current
                val pageSlideDistance = remember(density) { with(density) { 24.dp.roundToPx() } }

                AnimatedContent(
                    targetState = dateKey,
                    transitionSpec = {
                        if (reduceMotion) {
                            fadeIn(animationSpec = tween(KannachMotion.INSTANT)) togetherWith
                                fadeOut(animationSpec = tween(KannachMotion.INSTANT))
                        } else {
                            val isRtl = I18n.isRtl(language)
                            val isForward = targetState > initialState
                            val sign = if (isForward) (if (isRtl) -1 else 1) else (if (isRtl) 1 else -1)
                            val enterOffsetX = pageSlideDistance * sign
                            val exitOffsetX = -pageSlideDistance * sign

                            (slideInHorizontally(
                                animationSpec = tween(KannachMotion.PAGE, easing = KannachMotion.Settle),
                                initialOffsetX = { enterOffsetX }
                            ) + fadeIn(
                                animationSpec = tween(KannachMotion.PAGE, easing = KannachMotion.Settle)
                            )).togetherWith(
                                slideOutHorizontally(
                                    animationSpec = tween(KannachMotion.PAGE, easing = KannachMotion.Settle),
                                    targetOffsetX = { exitOffsetX }
                                ) + fadeOut(
                                    animationSpec = tween(KannachMotion.PAGE, easing = KannachMotion.Settle)
                                )
                            )
                        }
                    },
                    label = "MemoryCardPageFlip"
                ) { targetKey ->
                    val cardFormattedDate = remember(targetKey, language) {
                        if (targetKey.isNotBlank()) I18n.formatDateTitle(targetKey, language) else ""
                    }
                    val cardIsToday = targetKey == viewModel.getTodayKey()
                    MemoryCardView(
                        viewModel = viewModel,
                        dateKey = targetKey,
                        formattedDate = cardFormattedDate,
                        isToday = cardIsToday
                    )
                }
            }

            AnimatedVisibility(
                visible = mode == SheetMode.EDIT,
                enter = kannachSheetEnter(reduceMotion),
                exit = kannachSheetExit(reduceMotion)
            ) {
                EntryEditBottomSheet(
                    viewModel = viewModel,
                    dateKey = dateKey,
                    formattedDate = formattedDate,
                    isToday = isToday
                )
            }
        }
    }
}

/**
 * View mode: Centered "Memory Card" rising out of the scrim.
 */
/**
 * View mode: Centered "Memory Card" rising out of the scrim.
 * M2: Uses the seasonal palette of the entry's own month!
 * M3: Displays handwriting poetic time distance line under the date.
 * M4: Horizontal swipe on the card to turn pages between previous and next entries,
 * plus a small pagination dot indicator row (max 7 dots).
 */
@Composable
private fun MemoryCardView(
    viewModel: MainViewModel,
    dateKey: String,
    formattedDate: String,
    isToday: Boolean
) {
    val language by viewModel.language.collectAsState()
    val isRtlLang = I18n.isRtl(language)
    val strings = I18n.forLanguage(language)
    val entries by viewModel.entries.collectAsState()
    val entry = entries[dateKey] ?: DayEntry()

    // M2: Seasonal palette of the entry's own month!
    val entryMonth = remember(dateKey) {
        dateKey.split("-").getOrNull(1)?.toIntOrNull() ?: 1
    }

    KannachTheme(month = entryMonth, language = language) {
        val palette = LocalKannachPalette.current
        val glyphColor = try {
            Color(android.graphics.Color.parseColor(entry.color))
        } catch (_: Exception) {
            palette.green
        }

        // Memory Navigation
        val sortedKeys = remember(entries) {
            entries.keys.filter {
                val e = entries[it]
                e != null && (e.text.isNotBlank() || e.strokes.isNotEmpty())
            }.sorted()
        }
        val currentIndex = sortedKeys.indexOf(dateKey)
        val prevKey = if (currentIndex > 0) sortedKeys[currentIndex - 1] else null
        val nextKey = if (currentIndex >= 0 && currentIndex < sortedKeys.size - 1) sortedKeys[currentIndex + 1] else null

        // M3: Poetic time distance
        val relativeDistance = remember(dateKey, language) {
            I18n.getRelativeTimeDistance(dateKey, language)
        }

        val recordedTimeStr = remember(entry.createdAt, language) {
            try {
                if (entry.createdAt > 0L) {
                    val cal = Calendar.getInstance().apply { timeInMillis = entry.createdAt }
                    val sdf = java.text.SimpleDateFormat(
                        if (isRtlLang) "دُوّنت في h:mm a" else "Recorded at h:mm a",
                        if (isRtlLang) java.util.Locale("ar") else java.util.Locale.US
                    )
                    sdf.format(cal.time)
                } else ""
            } catch (_: Exception) { "" }
        }

        // M4: Gesture detection for horizontal swiping between memories
        var dragAmountX by remember { mutableFloatStateOf(0f) }

        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 440.dp)
                .heightIn(max = 680.dp)
                .clickable(enabled = false) {}
                .pointerInput(dateKey, prevKey, nextKey, isRtlLang) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragAmountX = 0f },
                        onHorizontalDrag = { _, dragAmount -> dragAmountX += dragAmount },
                        onDragEnd = {
                            if (dragAmountX > 50f) {
                                val targetKey = if (isRtlLang) nextKey else prevKey
                                if (targetKey != null) { viewModel.vibrate(8); viewModel.openSheet(targetKey) }
                            } else if (dragAmountX < -50f) {
                                val targetKey = if (isRtlLang) prevKey else nextKey
                                if (targetKey != null) { viewModel.vibrate(8); viewModel.openSheet(targetKey) }
                            }
                        }
                    )
                }
                .shadow(24.dp, HandDrawnRadiusShape)
                .clip(HandDrawnRadiusShape)
                .background(palette.paper)
                .border(2.dp, palette.ink, HandDrawnRadiusShape)
                .paperBackground(palette.paper)
                .padding(top = 22.dp, bottom = 18.dp, start = 18.dp, end = 18.dp)
        ) {
            // Washi Tape at Top Center of Memory Card
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-14).dp)
                    .size(width = 54.dp, height = 14.dp)
                    .rotate(1.5f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(palette.greenSoft.copy(alpha = 0.9f))
                    .border(0.8.dp, palette.greenLine.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header: Date, Distance line, Today Chip, Favorite, Edit & Close Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formattedDate,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.greenDeep,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isToday) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(palette.greenSoft)
                                        .border(1.dp, palette.greenLine, CircleShape)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = strings.today,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = palette.greenDeep
                                    )
                                }
                            }
                        }

                        // M3: Poetic time distance line
                        if (relativeDistance.isNotBlank()) {
                            Text(
                                text = relativeDistance,
                                fontSize = 13.5.sp,
                                fontFamily = LocalKannachHandFont.current,
                                color = palette.greenDeep,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.toggleSheetFav(); viewModel.saveCurrentEntry() }) {
                            KannachIcon(
                                icon = KannachIconType.HEART,
                                tint = if (entry.fav) palette.red else palette.inkFaint,
                                filled = entry.fav,
                                fillColor = palette.red.copy(alpha = 0.4f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(onClick = { viewModel.switchToEditMode() }) {
                            KannachIcon(
                                icon = KannachIconType.EDIT,
                                tint = palette.greenDeep,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(onClick = { viewModel.requestCloseSheet() }) {
                            KannachIcon(
                                icon = KannachIconType.X,
                                tint = palette.inkSoft,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Doodle Frame (aspect ratio 4:5, paper tilt)
                Box(
                    modifier = Modifier
                        .widthIn(max = 220.dp)
                        .aspectRatio(4f / 5f)
                        .paperTilt(dateKey.hashCode())
                        .shadow(12.dp, HandDrawnRadiusShape)
                        .clip(HandDrawnRadiusShape)
                        .background(palette.paperHi)
                        .border(2.dp, palette.ink, HandDrawnRadiusShape)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GlyphView(
                        strokes = entry.strokes,
                        color = glyphColor,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Ruled Notebook Text
                if (entry.text.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .linedPaperBackground(palette.line, 32.dp)
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    ) {
                        Text(
                            text = entry.text,
                            fontSize = 20.sp,
                            lineHeight = 32.sp,
                            fontFamily = LocalKannachHandFont.current,
                            color = palette.ink
                        )
                    }
                }

                // M3: Secondary Creation Time
                if (recordedTimeStr.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = recordedTimeStr,
                        fontSize = 11.5.sp,
                        fontFamily = LocalKannachHandFont.current,
                        color = palette.inkFaint,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // M4: Memory Navigation Buttons & Pagination Dots Indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (prevKey != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.openSheet(prevKey) }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            KannachIcon(
                                icon = if (isRtlLang) KannachIconType.CHEV_R else KannachIconType.CHEV_L,
                                tint = palette.greenDeep,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isRtlLang) "السابقة" else "Previous",
                                fontSize = 12.sp,
                                fontFamily = LocalKannachHandFont.current,
                                color = palette.greenDeep
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(36.dp))
                    }

                    // M4: Row of small pagination dots (maximum 7 dots)
                    if (sortedKeys.size > 1) {
                        val totalDots = sortedKeys.size.coerceAtMost(7)
                        val activeDotIndex = if (sortedKeys.size <= 7) currentIndex else {
                            ((currentIndex.toFloat() / (sortedKeys.size - 1)) * (totalDots - 1)).roundToInt()
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 0 until totalDots) {
                                val isActive = i == activeDotIndex
                                Box(
                                    modifier = Modifier
                                        .size(if (isActive) 7.dp else 4.dp)
                                        .clip(CircleShape)
                                        .background(if (isActive) palette.greenDeep else palette.inkFaint.copy(alpha = 0.35f))
                                )
                            }
                        }
                    }

                    if (nextKey != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.openSheet(nextKey) }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isRtlLang) "التالية" else "Next",
                                fontSize = 12.sp,
                                fontFamily = LocalKannachHandFont.current,
                                color = palette.greenDeep
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            KannachIcon(
                                icon = if (isRtlLang) KannachIconType.CHEV_L else KannachIconType.CHEV_R,
                                tint = palette.greenDeep,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(36.dp))
                    }
                }
            }
        }
    }
}

/**
 * W1, W2, W3, W5, W6, W7, W8:
 * The 3-Pulse Write Ritual (Arrive -> Leave your mark -> Plant in garden).
 * Completely replaces the long form with a poetic, full-height notebook page ritual.
 */
@Composable
private fun EntryEditBottomSheet(
    viewModel: MainViewModel,
    dateKey: String,
    formattedDate: String,
    isToday: Boolean
) {
    val palette = LocalKannachPalette.current
    val strings = I18n.forLanguage(viewModel.language.collectAsState().value)
    val isRtl = I18n.isRtl(viewModel.language.collectAsState().value)
    val writeStep by viewModel.writeStep.collectAsState()
    val text by viewModel.sheetText.collectAsState()
    val strokes by viewModel.sheetStrokes.collectAsState()
    val selectedColor by viewModel.sheetColor.collectAsState()
    val isFav by viewModel.sheetFav.collectAsState()

    val entries by viewModel.entries.collectAsState()
    val hasExisting = entries.containsKey(dateKey)
    val reduceMotion = rememberReduceMotion()

    // Full-height authentic journal paper page (W1)
    Box(
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .fillMaxHeight(0.93f)
            .widthIn(max = 520.dp)
            .clickable(enabled = false) {}
            .shadow(24.dp, HandDrawnRadiusShape)
            .clip(HandDrawnRadiusShape)
            .background(palette.paper)
            .border(2.dp, palette.ink, HandDrawnRadiusShape)
            .paperBackground(palette.paper)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        AnimatedContent(
            targetState = writeStep,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally(
                        animationSpec = tween(
                            if (reduceMotion) KannachMotion.INSTANT else KannachMotion.PAGE,
                            easing = KannachMotion.Settle
                        )
                    ) { if (isRtl) -it / 6 else it / 6 } + fadeIn(
                        animationSpec = tween(if (reduceMotion) KannachMotion.INSTANT else KannachMotion.PAGE)
                    )).togetherWith(
                        slideOutHorizontally(
                            animationSpec = tween(
                                if (reduceMotion) KannachMotion.INSTANT else KannachMotion.PAGE,
                                easing = KannachMotion.Settle
                            )
                        ) { if (isRtl) it / 6 else -it / 6 } + fadeOut(
                            animationSpec = tween(if (reduceMotion) KannachMotion.INSTANT else KannachMotion.PAGE)
                        )
                    )
                } else {
                    (slideInHorizontally(
                        animationSpec = tween(
                            if (reduceMotion) KannachMotion.INSTANT else KannachMotion.PAGE,
                            easing = KannachMotion.Settle
                        )
                    ) { if (isRtl) it / 6 else -it / 6 } + fadeIn(
                        animationSpec = tween(if (reduceMotion) KannachMotion.INSTANT else KannachMotion.PAGE)
                    )).togetherWith(
                        slideOutHorizontally(
                            animationSpec = tween(
                                if (reduceMotion) KannachMotion.INSTANT else KannachMotion.PAGE,
                                easing = KannachMotion.Settle
                            )
                        ) { if (isRtl) -it / 6 else it / 6 } + fadeOut(
                            animationSpec = tween(if (reduceMotion) KannachMotion.INSTANT else KannachMotion.PAGE)
                        )
                    )
                }
            },
            label = "RitualPulse"
        ) { step ->
            if (step == 1) {
                WritePulseOne(
                    viewModel = viewModel,
                    dateKey = dateKey,
                    formattedDate = formattedDate,
                    isToday = isToday,
                    isFav = isFav,
                    text = text,
                    strings = strings,
                    isRtl = isRtl,
                    reduceMotion = reduceMotion
                )
            } else {
                WritePulseTwo(
                    viewModel = viewModel,
                    dateKey = dateKey,
                    formattedDate = formattedDate,
                    hasExisting = hasExisting,
                    strokes = strokes,
                    selectedColor = selectedColor,
                    strings = strings,
                    isRtl = isRtl,
                    reduceMotion = reduceMotion
                )
            }
        }
    }
}

/**
 * Pulse 1 — Arrive & Write (W1, W5, W6, W8).
 * Large handwritten date, soft-filling heart, ruled paper filling the page, prompt box deleted.
 */
@Composable
private fun WritePulseOne(
    viewModel: MainViewModel,
    dateKey: String,
    formattedDate: String,
    isToday: Boolean,
    isFav: Boolean,
    text: String,
    strings: I18n.Strings,
    isRtl: Boolean,
    reduceMotion: Boolean
) {
    val palette = LocalKannachPalette.current

    val heartFillColor by animateColorAsState(
        targetValue = if (isFav) palette.red.copy(alpha = 0.45f) else Color.Transparent,
        animationSpec = tween(if (reduceMotion) KannachMotion.INSTANT else KannachMotion.SOFT),
        label = "heartFill"
    )
    val heartTint by animateColorAsState(
        targetValue = if (isFav) palette.red else palette.inkFaint,
        animationSpec = tween(if (reduceMotion) KannachMotion.INSTANT else KannachMotion.SOFT),
        label = "heartTint"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { viewModel.requestCloseSheet() },
                modifier = Modifier.size(48.dp)
            ) {
                KannachIcon(
                    icon = KannachIconType.X,
                    tint = palette.inkSoft,
                    modifier = Modifier.size(20.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = formattedDate,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = LocalKannachHandFont.current,
                    color = palette.greenDeep,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isToday) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(palette.greenSoft)
                            .border(1.dp, palette.greenLine, CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = strings.today,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.greenDeep
                        )
                    }
                }
            }

            IconButton(
                onClick = { viewModel.toggleSheetFav() },
                modifier = Modifier.size(48.dp)
            ) {
                KannachIcon(
                    icon = KannachIconType.HEART,
                    tint = heartTint,
                    filled = isFav,
                    fillColor = heartFillColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Ruled Paper Text Area extending to bottom of page (W6, W8: No canvas, no prompt box)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .linedPaperBackground(palette.line, 32.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            if (text.isEmpty()) {
                Text(
                    text = strings.entryPlaceholder,
                    fontSize = 20.sp,
                    lineHeight = 32.sp,
                    fontFamily = LocalKannachHandFont.current,
                    color = palette.inkFaint,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            BasicTextField(
                value = text,
                onValueChange = { viewModel.updateSheetText(it) },
                textStyle = TextStyle(
                    fontSize = 20.sp,
                    lineHeight = 32.sp,
                    fontFamily = LocalKannachHandFont.current,
                    color = palette.ink
                ),
                cursorBrush = SolidColor(palette.greenDeep),
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Corner Next Step Arrow (W1)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(HandDrawnRadiusShape)
                    .background(palette.greenSoft)
                    .border(1.3.dp, palette.green, HandDrawnRadiusShape)
                    .clickable { viewModel.setWriteStep(2) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isRtl) "ترك البصمة" else "Leave mark",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = LocalKannachHandFont.current,
                    color = palette.greenDeep
                )
                Spacer(modifier = Modifier.width(8.dp))
                KannachIcon(
                    icon = if (isRtl) KannachIconType.CHEV_L else KannachIconType.CHEV_R,
                    tint = palette.greenDeep,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Pulse 2 — Leave your mark (W2, W7).
 * Drawing board only (3:4 inner dashed border), undo/clear, 5 swatches, poetic mood name,
 * quiet delete link if existing, hand-drawn Plant button (W3).
 */
@Composable
private fun WritePulseTwo(
    viewModel: MainViewModel,
    dateKey: String,
    formattedDate: String,
    hasExisting: Boolean,
    strokes: List<List<Point>>,
    selectedColor: String,
    strings: I18n.Strings,
    isRtl: Boolean,
    reduceMotion: Boolean
) {
    val palette = LocalKannachPalette.current
    var canvasOffsetInRoot by remember { mutableStateOf(Offset.Zero) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header: Back arrow to Step 1 + Title + Close
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { viewModel.setWriteStep(1) },
                modifier = Modifier.size(48.dp)
            ) {
                KannachIcon(
                    icon = if (isRtl) KannachIconType.CHEV_R else KannachIconType.CHEV_L,
                    tint = palette.greenDeep,
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = strings.drawLabel,
                fontSize = 16.sp,
                fontFamily = LocalKannachHandFont.current,
                color = palette.greenDeep,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = { viewModel.requestCloseSheet() },
                modifier = Modifier.size(48.dp)
            ) {
                KannachIcon(
                    icon = KannachIconType.X,
                    tint = palette.inkSoft,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Drawing board with 3:4 aspect ratio and inner dashed border
        Box(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .widthIn(max = 260.dp)
                .aspectRatio(3f / 4f)
                .paperTilt(1)
                .shadow(12.dp, HandDrawnRadiusShape)
                .clip(HandDrawnRadiusShape)
                .background(palette.paperHi)
                .border(2.dp, palette.ink, HandDrawnRadiusShape)
                .onGloballyPositioned { coords ->
                    canvasOffsetInRoot = coords.positionInRoot()
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp)
                    .dashedHandDrawnBorder(
                        color = palette.greenLine.copy(alpha = 0.65f),
                        shape = HandDrawnRadiusShape,
                        strokeWidth = 1.3.dp,
                        dashIntervals = floatArrayOf(8f, 6f)
                    )
            )

            DrawingCanvas(
                strokes = strokes,
                colorHex = selectedColor,
                onStrokeCompleted = { viewModel.addSheetStroke(it) },
                modifier = Modifier.fillMaxSize()
            )

            if (strokes.isEmpty()) {
                Text(
                    text = strings.drawHint,
                    fontSize = 14.sp,
                    fontFamily = LocalKannachHandFont.current,
                    color = palette.inkFaint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Undo & Clear buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolButton(
                icon = KannachIconType.UNDO,
                text = strings.undo,
                onClick = { viewModel.undoSheetStroke() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            ToolButton(
                icon = KannachIconType.X,
                text = strings.clear,
                onClick = { viewModel.clearSheetStrokes() }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 5 Color swatches
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            I18n.COLORS.forEach { colorOpt ->
                val hex = colorOpt.hex
                val isSelected = selectedColor.equals(hex, ignoreCase = true)
                val color = Color(android.graphics.Color.parseColor(hex))

                Box(
                    modifier = Modifier
                        .padding(horizontal = 7.dp)
                        .size(34.dp)
                        .scale(if (isSelected) 1.15f else 1.0f)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (isSelected) {
                                Modifier.border(2.5.dp, palette.ink, CircleShape)
                            } else Modifier
                        )
                        .clickable { viewModel.selectSheetColor(hex) }
                )
            }
        }

        // Poetic mood name in handwriting font
        val moodName = I18n.getMoodName(selectedColor, viewModel.language.collectAsState().value)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "« $moodName »",
            fontFamily = LocalKannachHandFont.current,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.greenDeep,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Bottom actions: Delete (W7) + Hand-drawn Plant button (W3)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (hasExisting) Arrangement.SpaceBetween else Arrangement.Center
        ) {
            if (hasExisting) {
                Text(
                    text = strings.delete,
                    color = palette.red,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { viewModel.deleteCurrentEntry() }
                        .padding(10.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(HandDrawnRadiusShape)
                    .background(palette.green)
                    .border(1.6.dp, palette.greenDeep, HandDrawnRadiusShape)
                    .clickable {
                        viewModel.startPlantingCeremony(canvasOffsetInRoot, reduceMotion)
                    }
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KannachIcon(
                        icon = KannachIconType.SPROUT,
                        tint = palette.onGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.plantBtn,
                        color = palette.onGreen,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = LocalKannachHandFont.current
                    )
                }
            }
        }
    }
}

/**
 * W3: Garden Planting Ceremony Overlay.
 * Flying doodle shrinks and arcs from the drawing canvas position to its target cell
 * over CEREMONY (900ms) with Settle easing, triggering bloom haptics and petal burst.
 */
@Composable
fun PlantingCeremonyOverlay(viewModel: MainViewModel) {
    val animState by viewModel.plantingAnimation.collectAsState()
    val anim = animState ?: return
    val reduceMotion = rememberReduceMotion()
    val palette = LocalKannachPalette.current

    if (reduceMotion) {
        LaunchedEffect(anim.dateKey) {
            viewModel.completePlantingCeremony()
        }
        return
    }

    val progress = remember { Animatable(0f) }

    LaunchedEffect(anim.dateKey) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = KannachMotion.CEREMONY,
                easing = KannachMotion.Settle
            )
        )
        viewModel.completePlantingCeremony()
    }

    val p = progress.value
    val currentX = anim.startOffset.x + (anim.targetOffset.x - anim.startOffset.x) * p
    val arcHeight = with(LocalDensity.current) { 70.dp.toPx() }
    val arcOffset = sin(p * Math.PI.toFloat()) * arcHeight
    val currentY = anim.startOffset.y + (anim.targetOffset.y - anim.startOffset.y) * p - arcOffset

    val targetW = anim.targetSize.width.toFloat().coerceAtLeast(10f)
    val targetH = anim.targetSize.height.toFloat().coerceAtLeast(10f)
    val initialSizePx = with(LocalDensity.current) { 200.dp.toPx() }
    val currentW = initialSizePx + (targetW - initialSizePx) * p
    val currentH = initialSizePx + (targetH - initialSizePx) * p

    val glyphColor = try {
        Color(android.graphics.Color.parseColor(anim.colorHex))
    } catch (_: Exception) {
        palette.green
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .offset {
                    androidx.compose.ui.unit.IntOffset(currentX.roundToInt(), currentY.roundToInt())
                }
                .size(
                    width = with(LocalDensity.current) { currentW.toDp() },
                    height = with(LocalDensity.current) { currentH.toDp() }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (anim.strokes.isNotEmpty()) {
                GlyphView(
                    strokes = anim.strokes,
                    color = glyphColor,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                KannachIcon(
                    icon = KannachIconType.SPROUT,
                    tint = glyphColor,
                    modifier = Modifier.fillMaxSize(0.7f)
                )
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: KannachIconType,
    text: String,
    onClick: () -> Unit
) {
    val palette = LocalKannachPalette.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(HandDrawnRadiusAltShape)
            .background(palette.paperHi)
            .border(1.5.dp, palette.line, HandDrawnRadiusAltShape)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        KannachIcon(icon = icon, tint = palette.inkSoft, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, fontSize = 13.sp, color = palette.inkSoft)
    }
}

/**
 * Interactive touch drawing surface capturing points on a virtual 300x400 canvas.
 */
@Composable
fun DrawingCanvas(
    strokes: List<List<Point>>,
    colorHex: String,
    onStrokeCompleted: (List<Point>) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalKannachPalette.current
    val currentStroke = remember { mutableStateListOf<Point>() }
    val drawColor = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (_: Exception) {
        palette.green
    }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val sx = 300f / size.width
                        val sy = 400f / size.height
                        val pt = Point(
                            x = ((offset.x * sx) * 10f).toInt() / 10f,
                            y = ((offset.y * sy) * 10f).toInt() / 10f
                        )
                        currentStroke.clear()
                        currentStroke.add(pt)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val sx = 300f / size.width
                        val sy = 400f / size.height
                        val pt = Point(
                            x = ((change.position.x * sx) * 10f).toInt() / 10f,
                            y = ((change.position.y * sy) * 10f).toInt() / 10f
                        )
                        if (currentStroke.isEmpty() || Math.hypot(
                                (pt.x - currentStroke.last().x).toDouble(),
                                (pt.y - currentStroke.last().y).toDouble()
                            ) > 2.0
                        ) {
                            currentStroke.add(pt)
                        }
                    },
                    onDragEnd = {
                        if (currentStroke.isNotEmpty()) {
                            onStrokeCompleted(currentStroke.toList())
                            currentStroke.clear()
                        }
                    },
                    onDragCancel = {
                        currentStroke.clear()
                    }
                )
            }
    ) {
        val scaleX = size.width / 300f
        val scaleY = size.height / 400f
        val strokeWidth = 8f * scaleX
        val strokeStyle = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        // Draw existing strokes
        for (stroke in strokes) {
            if (stroke.isNotEmpty()) {
                val path = StrokeMath.composePathFrom(stroke)
                val matrix = androidx.compose.ui.graphics.Matrix().apply {
                    scale(scaleX, scaleY)
                }
                path.transform(matrix)
                drawPath(path = path, color = drawColor, style = strokeStyle)
            }
        }

        // Draw current active stroke
        if (currentStroke.isNotEmpty()) {
            val path = StrokeMath.composePathFrom(currentStroke.toList())
            val matrix = androidx.compose.ui.graphics.Matrix().apply {
                scale(scaleX, scaleY)
            }
            path.transform(matrix)
            drawPath(path = path, color = drawColor, style = strokeStyle)
        }
    }
}

/**
 * R2: 3-beat first run onboarding welcoming new users into the quiet paper world of Kannach:
 * - Beat 1: The sprout draws itself via AnimatedSprout + "كُنَّاش دفتر ورقي هادئ."
 * - Beat 2: An authentic sample day cell sprouts a gentle flower glyph via GlyphView.
 * - Beat 3: A poetic call to action: "المس أي يوم لتغرس أول رسمة صغيرة."
 * Skippable after Beat 1.
 */
@Composable
fun FirstRunOnboarding(
    strings: I18n.Strings,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalKannachPalette.current
    val reduceMotion = rememberReduceMotion()
    var currentBeat by remember { mutableIntStateOf(1) }
    var visible by remember { mutableStateOf(true) }

    // Sample flower strokes for Beat 2 rendered faithfully in normalized 300x300 space
    val sampleStrokes = remember {
        listOf(
            // Stem: curve upward
            listOf(
                Point(150f, 240f),
                Point(150f, 200f),
                Point(150f, 150f)
            ),
            // Left petal curve
            listOf(
                Point(150f, 150f),
                Point(115f, 120f),
                Point(125f, 95f),
                Point(150f, 150f)
            ),
            // Right petal curve
            listOf(
                Point(150f, 150f),
                Point(185f, 120f),
                Point(175f, 95f),
                Point(150f, 150f)
            ),
            // Upper blossom bud
            listOf(
                Point(150f, 150f),
                Point(140f, 85f),
                Point(160f, 85f),
                Point(150f, 150f)
            )
        )
    }

    // Animated sprout for Beat 1
    val sproutProgress = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(currentBeat) {
        if (currentBeat == 1 && !reduceMotion) {
            sproutProgress.snapTo(0f)
            sproutProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = KannachMotion.CEREMONY,
                    easing = KannachMotion.Settle
                )
            )
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(KannachMotion.PAGE)),
        exit = fadeOut(animationSpec = tween(KannachMotion.PAGE))
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .paperBackground(palette.paper)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header / Skip Button (Visible and interactive throughout or after Beat 1)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Skip Button (>=48dp touch target)
                Text(
                    text = strings.onboardingSkip,
                    fontSize = 15.sp,
                    fontFamily = LocalKannachHandFont.current,
                    color = palette.inkFaint,
                    modifier = Modifier
                        .clickable {
                            visible = false
                            onFinish()
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            // Central Content per Beat
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.weight(0.7f))

                AnimatedContent(
                    targetState = currentBeat,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(KannachMotion.PAGE, easing = KannachMotion.Settle)) +
                                slideInVertically(animationSpec = tween(KannachMotion.PAGE, easing = KannachMotion.Settle)) { it / 4 })
                            .togetherWith(
                                fadeOut(animationSpec = tween(KannachMotion.SOFT)) +
                                        slideOutVertically(animationSpec = tween(KannachMotion.SOFT)) { -it / 6 }
                            )
                    },
                    label = "OnboardingBeatTransition"
                ) { beat ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        when (beat) {
                            1 -> {
                                // Beat 1: Sprout drawn stroke-by-stroke + poetic intro
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AnimatedSprout(
                                        progress = sproutProgress.value,
                                        tint = palette.greenDeep,
                                        strokeWidthDp = 3.2.dp,
                                        modifier = Modifier.size(100.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(32.dp))

                                Text(
                                    text = strings.onboardingBeat1,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = LocalKannachHandFont.current,
                                    color = palette.greenDeep,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = strings.onboardingBeat1Sub,
                                    fontSize = 16.sp,
                                    fontFamily = LocalKannachHandFont.current,
                                    color = palette.inkSoft,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 24.sp,
                                    modifier = Modifier.widthIn(max = 320.dp)
                                )
                            }
                            2 -> {
                                // Beat 2: Authentic sample day cell sprouting a flower glyph
                                Box(
                                    modifier = Modifier
                                        .size(130.dp)
                                        .clip(HandDrawnRadiusShape)
                                        .background(palette.paperHi)
                                        .dashedHandDrawnBorder(color = palette.line, shape = HandDrawnRadiusShape, strokeWidth = 1.8.dp)
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = "15",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = LocalKannachHandFont.current,
                                            color = palette.inkSoft
                                        )
                                        Box(
                                            modifier = Modifier.weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            GlyphView(
                                                strokes = sampleStrokes,
                                                color = palette.green,
                                                modifier = Modifier.fillMaxSize(0.85f)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(32.dp))

                                Text(
                                    text = strings.onboardingBeat2,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = LocalKannachHandFont.current,
                                    color = palette.greenDeep,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = strings.onboardingBeat2Sub,
                                    fontSize = 16.sp,
                                    fontFamily = LocalKannachHandFont.current,
                                    color = palette.inkSoft,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 24.sp,
                                    modifier = Modifier.widthIn(max = 320.dp)
                                )
                            }
                            3 -> {
                                // Beat 3: Invitation to touch any day and begin
                                Box(
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(CircleShape)
                                        .background(palette.paperHi)
                                        .dashedHandDrawnBorder(color = palette.green, shape = CircleShape, strokeWidth = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    KannachIcon(
                                        icon = KannachIconType.SPROUT,
                                        tint = palette.greenDeep,
                                        strokeWidthDp = 2.6.dp,
                                        modifier = Modifier.size(54.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(32.dp))

                                Text(
                                    text = strings.onboardingBeat3,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = LocalKannachHandFont.current,
                                    color = palette.greenDeep,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = strings.onboardingBeat3Sub,
                                    fontSize = 16.sp,
                                    fontFamily = LocalKannachHandFont.current,
                                    color = palette.inkSoft,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 24.sp,
                                    modifier = Modifier.widthIn(max = 320.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Beat Dots Indicator (1, 2, 3)
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 28.dp)
                ) {
                    for (i in 1..3) {
                        val isCurrent = i == currentBeat
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 5.dp)
                                .size(if (isCurrent) 10.dp else 7.dp)
                                .clip(CircleShape)
                                .background(if (isCurrent) palette.greenDeep else palette.line)
                        )
                    }
                }

                // Action Button (Next or Start)
                if (currentBeat < 3) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clip(HandDrawnRadiusAltShape)
                            .background(palette.paperHi)
                            .border(1.5.dp, palette.line, HandDrawnRadiusAltShape)
                            .clickable {
                                currentBeat += 1
                            }
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = strings.onboardingNext,
                            fontSize = 17.sp,
                            fontFamily = LocalKannachHandFont.current,
                            fontWeight = FontWeight.Bold,
                            color = palette.greenDeep
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 54.dp)
                            .clip(HandDrawnRadiusAltShape)
                            .background(palette.green)
                            .clickable {
                                visible = false
                                onFinish()
                            }
                            .padding(horizontal = 24.dp, vertical = 14.dp)
                    ) {
                        KannachIcon(
                            icon = KannachIconType.SPROUT,
                            tint = palette.onGreen,
                            strokeWidthDp = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.onboardingStart,
                            fontSize = 18.sp,
                            fontFamily = LocalKannachHandFont.current,
                            fontWeight = FontWeight.Bold,
                            color = palette.onGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}
