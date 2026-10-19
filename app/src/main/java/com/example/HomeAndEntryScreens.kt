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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
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
import androidx.compose.runtime.State
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
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
 * Scales and centers within min(width, height) so strokes always fit strictly inside non-square bounds (DL2).
 * PF2: Uses Modifier.drawWithCache keyed by (strokes, size) so StrokeMath.composePathFrom runs once per size change.
 */
@Composable
fun GlyphView(
    strokes: List<List<Point>>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Spacer(
        modifier = modifier.drawWithCache {
            val minDim = minOf(size.width, size.height)
            if (minDim <= 0f) {
                onDrawBehind {}
            } else {
                val scale = minDim / StrokeMath.NORMALIZED_BOX_SIZE
                val dx = (size.width - minDim) / 2f
                val dy = (size.height - minDim) / 2f
                val strokeWidth = 11f * scale
                val strokeStyle = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )

                val paths = ArrayList<Path>(strokes.size)
                for (stroke in strokes) {
                    if (stroke.isNotEmpty()) {
                        val path = StrokeMath.composePathFrom(stroke)
                        val matrix = androidx.compose.ui.graphics.Matrix().apply {
                            scale(scale, scale)
                            translate(dx / scale, dy / scale)
                        }
                        path.transform(matrix)
                        paths.add(path)
                    }
                }

                onDrawBehind {
                    for (path in paths) {
                        drawPath(path = path, color = color, style = strokeStyle)
                    }
                }
            }
        }
    )
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
    onFinished: () -> Unit,
    appSub: String = ""
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
                if (appSub.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = appSub,
                        fontSize = 16.sp,
                        color = palette.inkSoft,
                        fontFamily = LocalKannachHandFont.current,
                        modifier = Modifier.alpha(titleAlpha.value)
                    )
                }
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .paperBackground(palette.paper)
            .wavyHeaderDivider(palette.greenLine, waveHeightDp = 6.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // NV7 & HX1: One row: leading 48 dp Settings button, centered title block, trailing 48 dp heart toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // NV7: Leading 48dp Settings button in header (replaces dock settings)
            IconButton(
                onClick = { viewModel.openSettings() },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("header_settings_button")
            ) {
                KannachIcon(
                    icon = KannachIconType.SLIDERS,
                    tint = palette.inkSoft,
                    contentDescription = strings.openSettings,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Centered Title Block: ‹ button (48dp), Column(Month, Season+Year), › button (48dp)
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Month Stepper: Prev Arrow (48.dp touch target)
                IconButton(
                    onClick = { viewModel.shiftMonth(-1) },
                    enabled = !isAtMinYear,
                    modifier = Modifier.size(48.dp)
                ) {
                    KannachIcon(
                        icon = if (isRtl) KannachIconType.CHEV_R else KannachIconType.CHEV_L,
                        tint = if (isAtMinYear) palette.inkFaint.copy(alpha = 0.35f) else palette.ink,
                        contentDescription = strings.prevMonth,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Month Name & Year (Month on line 1, Season icon + Year on line 2, never same line)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (currentYear != nowYear || currentMonth != nowMonth) {
                                viewModel.setMonth(nowYear, nowMonth)
                            }
                        }
                        .padding(horizontal = 4.dp)
                ) {
                    val monthName = strings.months.getOrElse(currentMonth - 1) { "" }
                    Text(
                        text = monthName,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = LocalKannachHandFont.current,
                        color = palette.greenDeep,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
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
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.greenDeep,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Month Stepper: Next Arrow (48.dp touch target)
                IconButton(
                    onClick = { viewModel.shiftMonth(1) },
                    enabled = !isAtMaxYear,
                    modifier = Modifier.size(48.dp)
                ) {
                    KannachIcon(
                        icon = if (isRtl) KannachIconType.CHEV_L else KannachIconType.CHEV_R,
                        tint = if (isAtMaxYear) palette.inkFaint.copy(alpha = 0.35f) else palette.ink,
                        contentDescription = strings.nextMonth,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Trailing 48dp Heart Button for Favorites Filter (H3 & HX1)
            val isFavActive = filter == "fav"
            IconButton(
                onClick = {
                    viewModel.setGridFilter(if (isFavActive) "all" else "fav")
                    viewModel.vibrate(6)
                },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("header_fav_button")
            ) {
                KannachIcon(
                    icon = KannachIconType.HEART,
                    tint = if (isFavActive) palette.red else palette.inkFaint,
                    filled = isFavActive,
                    fillColor = palette.red.copy(alpha = 0.35f),
                    contentDescription = strings.favorites,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * NV1–NV6b: The Paper Dock.
 * - Floating pill with 4 equal top-level tab slots (Page, Garden, Letters, Draw) + detached round Plant button.
 * - NV5 Pill: 16 dp side margins, bottom = system nav inset + 12 dp, height 64 dp,
 *   shape = HandDrawnDockShape (32/28/30/26 dp asymmetric), fill palette.paperHi,
 *   1.5 dp border palette.greenLine, stitched inner edge (dashed 1.2 dp, dash 8/6, inset 4 dp, greenLine @ 0.7 alpha),
 *   soft paper shadow from tokens (paperShadow), no Material elevation, tilt paperTilt(-0.4f).
 * - NV6 Slots: 24 dp hand-drawn icons (NOTEBOOK, GRID, SEAL, BRUSH). Only the selected slot shows its label
 *   (hand font 12 sp, greenDeep) cross-faded by alpha and a hand-drawn rounded highlight behind it
 *   (greenSoft, drawn on with a path-trim animation, SOFT). Unselected slots are icon-only (inkSoft)
 *   with contentDescription from I18n. Letters shows 7 dp red dot when a capsule is ready.
 * - NV6b Plant button: Detached 64 dp circle to pill's end side (mirrored in RTL), 10 dp gap,
 *   palette.green fill, greenDeep border 1.5 dp, inner dashed ring inset 5 dp, SPROUT icon 28 dp in palette.onGreen.
 *   On press the sprout scales 1 -> 0.9 -> 1.08 -> 1 (SOFT).
 * - DK1–DK6: Collapsible into a 52 dp round green bud anchored at the bottom center (cross-fading with graphicsLayer).
 */
@Composable
fun KannachDock(
    viewModel: MainViewModel,
    dockState: DockState = DockState.EXPANDED,
    onExpandDock: () -> Unit = {},
    onCollapseDock: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalKannachPalette.current
    val language by viewModel.language.collectAsState()
    val strings = I18n.forLanguage(language)
    val currentTab by viewModel.currentTab.collectAsState()
    val readyCapsule by viewModel.readyCapsule.collectAsState()
    val reduceMotion = rememberReduceMotion()
    val coroutineScope = rememberCoroutineScope()

    val todayKey = viewModel.getTodayKey()
    val sproutScale = remember { Animatable(1f) }

    // DK5: One Float progress (0 = collapsed, 1 = expanded) animated with KannachMotion.SOFT and Settle easing.
    val initialProgress = if (dockState == DockState.EXPANDED) 1f else 0f
    val dockProgress = remember { Animatable(initialProgress) }
    var isTransitioning by remember { mutableStateOf(false) }

    LaunchedEffect(dockState, reduceMotion) {
        val target = if (dockState == DockState.EXPANDED) 1f else 0f
        if (reduceMotion) {
            isTransitioning = false
            dockProgress.snapTo(target)
        } else if (dockProgress.value != target) {
            isTransitioning = true
            try {
                dockProgress.animateTo(
                    targetValue = target,
                    animationSpec = tween(KannachMotion.SOFT, easing = KannachMotion.Settle)
                )
            } finally {
                isTransitioning = false
            }
        } else {
            isTransitioning = false
        }
    }

    // DK5: A layer at alpha 0 is not composed and not focusable.
    val showExpandedDock = (dockState == DockState.EXPANDED) || isTransitioning
    val showCollapsedBud = (dockState == DockState.COLLAPSED) || isTransitioning

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        if (showExpandedDock) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val p = dockProgress.value.coerceIn(0f, 1f)
                        alpha = p
                        val s = 0.6f + 0.4f * p
                        scaleX = s
                        scaleY = s
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .testTag("kannach_dock"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // NV5: Stitched Paper Pill (4 equal tab slots)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .paperTilt(-0.4f)
                        .paperShadow(
                            shadowColor = palette.paperShadow,
                            shape = HandDrawnDockShape,
                            offsetY = 4.dp
                        )
                        .clip(HandDrawnDockShape)
                        .background(palette.paperHi)
                        .border(1.5.dp, palette.greenLine, HandDrawnDockShape)
                        .dashedHandDrawnBorder(
                            color = palette.greenLine.copy(alpha = 0.7f),
                            shape = HandDrawnDockShape,
                            strokeWidth = 1.2.dp,
                            dashIntervals = floatArrayOf(8f, 6f),
                            inset = 4.dp
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Page (Month Home)
                    PaperDockSlot(
                        icon = KannachIconType.NOTEBOOK,
                        label = strings.dockPage,
                        selected = currentTab == Tab.PAGE,
                        reduceMotion = reduceMotion,
                        enabled = dockState == DockState.EXPANDED,
                        onClick = { viewModel.selectTab(Tab.PAGE) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dock_tab_page")
                    )

                    // 2. Garden (Gallery)
                    PaperDockSlot(
                        icon = KannachIconType.GRID,
                        label = strings.dockGarden,
                        selected = currentTab == Tab.GARDEN,
                        reduceMotion = reduceMotion,
                        enabled = dockState == DockState.EXPANDED,
                        onClick = { viewModel.selectTab(Tab.GARDEN) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dock_tab_garden")
                    )

                    // 3. Letters (Time Capsules)
                    PaperDockSlot(
                        icon = KannachIconType.SEAL,
                        label = strings.dockLetters,
                        selected = currentTab == Tab.LETTERS,
                        hasBadge = readyCapsule != null,
                        reduceMotion = reduceMotion,
                        enabled = dockState == DockState.EXPANDED,
                        onClick = { viewModel.selectTab(Tab.LETTERS) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dock_tab_letters")
                    )

                    // 4. Draw (Sketchpad)
                    PaperDockSlot(
                        icon = KannachIconType.BRUSH,
                        label = strings.dockDraw,
                        selected = currentTab == Tab.DRAW,
                        reduceMotion = reduceMotion,
                        enabled = dockState == DockState.EXPANDED,
                        onClick = {
                            viewModel.selectTab(Tab.DRAW)
                            onCollapseDock()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dock_tab_draw")
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // NV6b & DK3: Detached Plant button inside the expanded dock only (64 dp circle, stitched inner ring inset 5 dp)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .paperShadow(
                            shadowColor = palette.paperShadow,
                            shape = CircleShape,
                            offsetY = 4.dp
                        )
                        .clip(CircleShape)
                        .background(palette.green)
                        .border(1.5.dp, palette.greenDeep, CircleShape)
                        .dashedHandDrawnBorder(
                            color = palette.greenLine.copy(alpha = 0.7f),
                            shape = CircleShape,
                            strokeWidth = 1.2.dp,
                            dashIntervals = floatArrayOf(8f, 6f),
                            inset = 5.dp
                        )
                        .semantics { contentDescription = strings.dockToday }
                        .testTag("dock_plant_button")
                        .clickable(
                            enabled = dockState == DockState.EXPANDED,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            coroutineScope.launch {
                                if (!reduceMotion) {
                                    sproutScale.animateTo(
                                        targetValue = 0.9f,
                                        animationSpec = tween(KannachMotion.SOFT / 3, easing = KannachMotion.Settle)
                                    )
                                    sproutScale.animateTo(
                                        targetValue = 1.08f,
                                        animationSpec = tween(KannachMotion.SOFT / 3, easing = KannachMotion.Settle)
                                    )
                                    sproutScale.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(KannachMotion.SOFT / 3, easing = KannachMotion.Settle)
                                    )
                                }
                            }
                            viewModel.openSheet(todayKey)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    KannachIcon(
                        icon = KannachIconType.SPROUT,
                        tint = palette.onGreen,
                        strokeWidthDp = 2.dp,
                        contentDescription = strings.dockToday,
                        modifier = Modifier
                            .size(28.dp)
                            .scale(sproutScale.value)
                    )
                }
            }
        }

        // DK1: COLLAPSED — 52 dp round bud, palette.green, sprout icon in palette.onGreen,
        // centered at the bottom (above navigationBarsPadding()), same paperShadow weight as Plant button,
        // contentDescription = strings.dockOpen.
        if (showCollapsedBud) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        val p = (1f - dockProgress.value).coerceIn(0f, 1f)
                        alpha = p
                        val s = 0.6f + 0.4f * p
                        scaleX = s
                        scaleY = s
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .size(52.dp)
                    .paperShadow(
                        shadowColor = palette.paperShadow,
                        shape = CircleShape,
                        offsetY = 4.dp
                    )
                    .clip(CircleShape)
                    .background(palette.green)
                    .border(1.5.dp, palette.greenDeep, CircleShape)
                    .dashedHandDrawnBorder(
                        color = palette.greenLine.copy(alpha = 0.7f),
                        shape = CircleShape,
                        strokeWidth = 1.2.dp,
                        dashIntervals = floatArrayOf(8f, 6f),
                        inset = 4.dp
                    )
                    .semantics { contentDescription = strings.dockOpen }
                    .testTag("dock_bud_button")
                    .clickable(
                        enabled = dockState == DockState.COLLAPSED,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onExpandDock
                    ),
                contentAlignment = Alignment.Center
            ) {
                KannachIcon(
                    icon = KannachIconType.SPROUT,
                    tint = palette.onGreen,
                    strokeWidthDp = 2.dp,
                    contentDescription = strings.dockOpen,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun PaperDockSlot(
    icon: KannachIconType,
    label: String,
    selected: Boolean,
    reduceMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hasBadge: Boolean = false
) {
    val palette = LocalKannachPalette.current
    val highlightTrim = remember { Animatable(if (selected) 1f else 0f) }
    val pathMeasure = remember { PathMeasure() }

    LaunchedEffect(selected, reduceMotion) {
        if (selected) {
            if (reduceMotion) {
                highlightTrim.snapTo(1f)
            } else {
                highlightTrim.snapTo(0f)
                highlightTrim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(KannachMotion.SOFT, easing = KannachMotion.Settle)
                )
            }
        } else {
            highlightTrim.snapTo(0f)
        }
    }

    // DK6: Fixed slots — no animateContentSize; slot widths are fixed; label cross-fades by alpha for the selected slot.
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxHeight()
            .heightIn(min = 48.dp)
            .clip(HandDrawnRadiusShape)
            .semantics { contentDescription = label }
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .drawBehind {
                val progress = highlightTrim.value
                if (progress > 0.01f) {
                    val outline = HandDrawnRadiusShape.createOutline(size, layoutDirection, this)
                    val fullPath = Path()
                    when (outline) {
                        is Outline.Generic -> fullPath.addPath(outline.path)
                        is Outline.Rounded -> fullPath.addRoundRect(outline.roundRect)
                        is Outline.Rectangle -> fullPath.addRect(outline.rect)
                    }
                    drawPath(
                        path = fullPath,
                        color = palette.greenSoft.copy(alpha = progress)
                    )
                    pathMeasure.setPath(fullPath, forceClosed = true)
                    val trimmed = Path()
                    pathMeasure.getSegment(0f, pathMeasure.length * progress, trimmed, true)
                    drawPath(
                        path = trimmed,
                        color = palette.greenLine,
                        style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                KannachIcon(
                    icon = icon,
                    tint = if (selected) palette.greenDeep else palette.inkSoft,
                    strokeWidthDp = 2.dp,
                    contentDescription = label,
                    modifier = Modifier.size(24.dp)
                )
                if (hasBadge) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(palette.red)
                    )
                }
            }
            if (selected) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontFamily = LocalKannachHandFont.current,
                    fontWeight = FontWeight.Bold,
                    color = palette.greenDeep,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.graphicsLayer {
                        alpha = highlightTrim.value
                    }
                )
            }
        }
    }
}

/**
 * Dashed banner component (used for On This Day & Capsule Ready).
 */
/**
 * Helper to compute the time-of-day / status greeting line (moved from AppHeader into TodayPageCard per HM2).
 */
@Composable
fun rememberHomeGreetingText(
    entries: Map<String, DayEntry>,
    todayKey: String,
    language: String,
    strings: Strings
): String {
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

    return remember(hasTodayEntry, isWelcomeBack, language) {
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
}

/**
 * Shared tear-off desk-calendar page surface for HM2 (TodayPageCard) and HM3 (MonthPageCard).
 * - Top edge: torn zig-zag path in palette.line; two small punched holes at the top (circles, palette.paperLo).
 * - Tilt: paperTilt(-0.5f); shadow: paperShadow.
 */
@Composable
private fun TearOffPageSurface(
    modifier: Modifier = Modifier,
    showPunchedHoles: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalKannachPalette.current
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .paperTilt(-0.5f)
            .paperShadow(
                shadowColor = palette.paperShadow,
                shape = TearOffPageShape,
                offsetY = 4.dp
            )
            .clip(TearOffPageShape)
            .background(palette.paperHi)
            .then(clickModifier)
            .drawBehind {
                val w = size.width
                val h = size.height
                if (w <= 0f || h <= 0f) return@drawBehind

                // Subtle side & bottom border in greenLine
                val outline = TearOffPageShape.createOutline(size, layoutDirection, this)
                if (outline is Outline.Generic) {
                    drawPath(
                        path = outline.path,
                        color = palette.greenLine.copy(alpha = 0.85f),
                        style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                // HM2 & TC3: Top and bottom torn zig-zag paths in palette.line
                val zigStep = 8.dp.toPx().coerceAtLeast(4f)
                val zigDepth = 4.dp.toPx().coerceAtMost(h * 0.1f)
                val teethCount = (w / zigStep).toInt().coerceAtLeast(2)
                val actualStep = w / teethCount
                val zigPath = Path().apply {
                    moveTo(0f, zigDepth)
                    for (i in 0 until teethCount) {
                        lineTo((i + 0.5f) * actualStep, 0f)
                        lineTo((i + 1) * actualStep, zigDepth)
                    }
                    moveTo(0f, h - zigDepth)
                    for (i in 0 until teethCount) {
                        lineTo((i + 0.5f) * actualStep, h)
                        lineTo((i + 1) * actualStep, h - zigDepth)
                    }
                }
                drawPath(
                    path = zigPath,
                    color = palette.line,
                    style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // HM2: Two small punched holes at the top (circles, palette.paperLo)
                if (showPunchedHoles) {
                    val holeY = 13.dp.toPx()
                    val holeRadius = 4.5.dp.toPx()
                    val holeOffset = 34.dp.toPx()
                    val cx = w / 2f
                    for (hx in listOf(cx - holeOffset, cx + holeOffset)) {
                        drawCircle(
                            color = palette.paperLo,
                            radius = holeRadius,
                            center = Offset(hx, holeY)
                        )
                        drawCircle(
                            color = palette.line,
                            radius = holeRadius,
                            center = Offset(hx, holeY),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                }
            },
        content = content
    )
}

/**
 * HM2 & TC1–TC5: TodayPageCard (current month).
 * Constraint-driven tiers based on the card's own available height via BoxWithConstraints:
 * - Full (h >= 230.dp): Current layout (weekday, 72 sp day number, month+year, greeting, ruled area with 112 dp doodle + up to 3 lines of text).
 * - Compact (150.dp <= h < 230.dp): Day number 48 sp; weekday and month/year on one line beside it; doodle 72 dp; greeting hidden; ruled area shows 2 lines of text max.
 * - Mini (h < 150.dp): One row: day number 40 sp at the start, then the doodle (56 dp) or sprout icon, then one line of text (maxLines = 1, ellipsis). No ruled lines, no greeting, no month line.
 * - TC2: Nothing may exceed the card. No child uses a fixed height larger than the tier allows. The ruled area uses weight(1f) with heightIn(min = 0.dp).
 */
@Composable
fun TodayPageCard(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalKannachPalette.current
    val language by viewModel.language.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val isRtl = I18n.isRtl(language)
    val strings = I18n.forLanguage(language)
    val entries by viewModel.entries.collectAsState()
    val density = LocalDensity.current

    val todayKey = viewModel.getTodayKey()
    val todayEntry = entries[todayKey]
    val greetingText = rememberHomeGreetingText(entries, todayKey, language, strings)

    val cal = remember(todayKey) { Calendar.getInstance() }
    val nowYear = cal.get(Calendar.YEAR)
    val nowMonth = cal.get(Calendar.MONTH) + 1
    val nowDay = cal.get(Calendar.DAY_OF_MONTH)

    val weekdayName = remember(nowYear, nowMonth, nowDay, language) {
        I18n.formatWeekdayName(nowYear, nowMonth, nowDay, language)
    }
    val dayNumberStr = remember(nowDay, language, userSettings.useArabicIndicNumerals) {
        I18n.formatYear(nowDay, language, userSettings.useArabicIndicNumerals)
    }
    val monthYearStr = remember(nowYear, nowMonth, language, userSettings.useArabicIndicNumerals) {
        I18n.formatMonthLabel(nowYear, nowMonth, language, userSettings.useArabicIndicNumerals)
    }

    val tightTextStyle = remember {
        TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
    }

    TearOffPageSurface(
        modifier = modifier.fillMaxSize(),
        showPunchedHoles = true,
        onClick = {
            viewModel.vibrate(8)
            viewModel.openSheet(todayKey)
        }
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val cardHeight = maxHeight
            val tier = when {
                cardHeight >= 230.dp -> 2 // Full
                cardHeight >= 150.dp -> 1 // Compact
                else -> 0                 // Mini
            }

            if (tier == 0) {
                // Mini tier (h < 150.dp): One row: day number 40 sp at the start,
                // then the doodle (56 dp) or sprout icon, then one line of text (maxLines = 1, ellipsis).
                // No ruled lines, no greeting, no month line.
                val miniDoodleSize = (cardHeight - 20.dp).coerceIn(24.dp, 56.dp)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("today_page_mini_row"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = dayNumberStr,
                        fontSize = 40.sp,
                        lineHeight = 40.sp,
                        style = tightTextStyle,
                        fontWeight = FontWeight.Bold,
                        fontFamily = LocalKannachHandFont.current,
                        color = palette.greenDeep,
                        maxLines = 1,
                        modifier = Modifier.testTag("today_page_huge_day")
                    )

                    if (todayEntry == null) {
                        KannachIcon(
                            icon = KannachIconType.SPROUT,
                            tint = palette.green,
                            strokeWidthDp = 2.dp,
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("today_page_sprout")
                        )
                        Text(
                            text = strings.todayCardEmpty,
                            fontSize = 15.sp,
                            lineHeight = 18.sp,
                            style = tightTextStyle,
                            fontFamily = LocalKannachHandFont.current,
                            color = palette.inkSoft,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("today_page_empty_text")
                        )
                    } else {
                        val doodleColor = try {
                            Color(android.graphics.Color.parseColor(todayEntry.color))
                        } catch (_: Exception) {
                            palette.green
                        }
                        Box(
                            modifier = Modifier
                                .size(miniDoodleSize)
                                .paperTilt(-2.2f),
                            contentAlignment = Alignment.Center
                        ) {
                            GlyphView(
                                strokes = todayEntry.strokes,
                                color = doodleColor,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("today_page_doodle")
                            )
                        }
                        val displayText = todayEntry.text.trim().ifBlank { strings.drawLabel }
                        Text(
                            text = displayText,
                            fontSize = 15.sp,
                            lineHeight = 18.sp,
                            style = tightTextStyle,
                            fontFamily = LocalKannachHandFont.current,
                            color = palette.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("today_page_written_text")
                        )
                    }
                }
            } else if (tier == 1) {
                // Compact tier (150.dp <= h < 230.dp):
                // Day number 48 sp; weekday and month/year on ONE line beside it;
                // doodle 72 dp; greeting hidden; ruled area shows 2 lines of text max.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 14.dp, bottom = 6.dp)
                        .testTag("today_page_compact_col"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = dayNumberStr,
                            fontSize = 48.sp,
                            lineHeight = 48.sp,
                            style = tightTextStyle,
                            fontWeight = FontWeight.Bold,
                            fontFamily = LocalKannachHandFont.current,
                            color = palette.greenDeep,
                            maxLines = 1,
                            modifier = Modifier.testTag("today_page_huge_day")
                        )
                        Text(
                            text = "$weekdayName · $monthYearStr",
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
                            style = tightTextStyle,
                            fontWeight = FontWeight.Medium,
                            fontFamily = LocalKannachUiFont.current,
                            color = palette.inkSoft,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .testTag("today_page_compact_date_line")
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // TC2: Ruled area uses weight(1f) with heightIn(min = 0.dp)
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 0.dp)
                            .fillMaxWidth()
                            .linedPaperBackground(
                                lineColor = palette.line.copy(alpha = 0.7f),
                                lineHeightDp = 24.dp
                            )
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val ruledInnerH = maxHeight
                        if (todayEntry == null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    KannachIcon(
                                        icon = KannachIconType.SPROUT,
                                        tint = palette.green,
                                        strokeWidthDp = 2.dp,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .testTag("today_page_sprout")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = strings.todayCardEmpty,
                                        fontSize = 14.sp,
                                        lineHeight = 18.sp,
                                        style = tightTextStyle,
                                        fontFamily = LocalKannachHandFont.current,
                                        color = palette.inkSoft,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.testTag("today_page_empty_text")
                                    )
                                }
                                if (ruledInnerH >= 36.dp) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(120.dp)
                                            .height(1.dp)
                                            .background(palette.line)
                                    )
                                }
                            }
                        } else {
                            val doodleColor = try {
                                Color(android.graphics.Color.parseColor(todayEntry.color))
                            } catch (_: Exception) {
                                palette.green
                            }
                            val compactDoodleSize = (ruledInnerH - 6.dp).coerceIn(28.dp, 72.dp)
                            val lineHeightDp = with(density) { 19.sp.toDp() }
                            val allowedLines = ((ruledInnerH - 4.dp) / lineHeightDp).toInt().coerceIn(1, 2)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(compactDoodleSize)
                                        .paperTilt(-2.2f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    GlyphView(
                                        strokes = todayEntry.strokes,
                                        color = doodleColor,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag("today_page_doodle")
                                    )
                                }

                                val displayText = todayEntry.text.trim().ifBlank { strings.drawLabel }
                                Text(
                                    text = displayText,
                                    fontSize = 14.sp,
                                    lineHeight = 19.sp,
                                    style = tightTextStyle,
                                    fontFamily = LocalKannachHandFont.current,
                                    color = palette.ink,
                                    maxLines = allowedLines,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("today_page_written_text")
                                )
                            }
                        }
                    }
                }
            } else {
                // Full tier (h >= 230.dp): Current layout
                val topPad = if (cardHeight < 260.dp) 14.dp else 20.dp
                val midGap = if (cardHeight < 260.dp) 4.dp else 8.dp
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topPad, bottom = 6.dp)
                        .testTag("today_page_full_col"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Upper half: Desk-calendar header (weekday, 72sp day number, month+year, greeting)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = weekdayName,
                            fontSize = 13.sp,
                            lineHeight = 15.sp,
                            style = tightTextStyle,
                            fontWeight = FontWeight.Medium,
                            fontFamily = LocalKannachUiFont.current,
                            letterSpacing = if (isRtl) 0.sp else 1.6.sp,
                            color = palette.inkSoft,
                            maxLines = 1
                        )

                        Text(
                            text = dayNumberStr,
                            fontSize = 72.sp,
                            lineHeight = 72.sp,
                            style = tightTextStyle,
                            fontWeight = FontWeight.Bold,
                            fontFamily = LocalKannachHandFont.current,
                            color = palette.greenDeep,
                            maxLines = 1,
                            modifier = Modifier.testTag("today_page_huge_day")
                        )

                        Text(
                            text = monthYearStr,
                            fontSize = 14.sp,
                            lineHeight = 16.sp,
                            style = tightTextStyle,
                            fontWeight = FontWeight.Medium,
                            fontFamily = LocalKannachUiFont.current,
                            color = palette.inkSoft,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = greetingText,
                            fontSize = 13.sp,
                            lineHeight = 15.sp,
                            style = tightTextStyle,
                            fontFamily = LocalKannachHandFont.current,
                            color = palette.inkFaint,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("today_page_greeting")
                        )
                    }

                    Spacer(modifier = Modifier.height(midGap))

                    // Lower half: Ruled paper (linedPaperBackground), weight(1f) + heightIn(min = 0.dp) (TC2)
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 0.dp)
                            .fillMaxWidth()
                            .linedPaperBackground(
                                lineColor = palette.line.copy(alpha = 0.7f),
                                lineHeightDp = 26.dp
                            )
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val ruledInnerH = maxHeight
                        if (todayEntry == null) {
                            // Today empty: faint placeholder line + sprout + todayCardEmpty
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    KannachIcon(
                                        icon = KannachIconType.SPROUT,
                                        tint = palette.green,
                                        strokeWidthDp = 2.dp,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .testTag("today_page_sprout")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = strings.todayCardEmpty,
                                        fontSize = 15.sp,
                                        lineHeight = 18.sp,
                                        style = tightTextStyle,
                                        fontFamily = LocalKannachHandFont.current,
                                        color = palette.inkSoft,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.testTag("today_page_empty_text")
                                    )
                                }
                                if (ruledInnerH >= 36.dp) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(120.dp)
                                            .height(1.dp)
                                            .background(palette.line)
                                    )
                                }
                            }
                        } else {
                            // Today written: doodle (up to 112 dp, never exceeding ruledInnerH) beside up to 3 lines of text
                            val doodleColor = try {
                                Color(android.graphics.Color.parseColor(todayEntry.color))
                            } catch (_: Exception) {
                                palette.green
                            }
                            val fullDoodleSize = (ruledInnerH - 6.dp).coerceIn(28.dp, 112.dp)
                            val lineHeightDp = with(density) { 21.sp.toDp() }
                            val allowedLines = ((ruledInnerH - 4.dp) / lineHeightDp).toInt().coerceIn(1, 3)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(fullDoodleSize)
                                        .paperTilt(-2.2f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    GlyphView(
                                        strokes = todayEntry.strokes,
                                        color = doodleColor,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag("today_page_doodle")
                                    )
                                }

                                val displayText = todayEntry.text.trim().ifBlank { strings.drawLabel }
                                Text(
                                    text = displayText,
                                    fontSize = 15.sp,
                                    lineHeight = 21.sp,
                                    style = tightTextStyle,
                                    fontFamily = LocalKannachHandFont.current,
                                    color = palette.ink,
                                    maxLines = allowedLines,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("today_page_written_text")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * HM3: MonthPageCard (past month).
 * Same tear-off shape: big month name, a sentence (monthRecapPlanted / monthRecapQuiet),
 * and a strip of up to 5 doodles (favorites first, then newest), 56 dp each with small rotations;
 * with zero entries, a faint dotted circle instead of the strip.
 */
@Composable
fun MonthPageCard(
    viewModel: MainViewModel,
    currentYear: Int,
    currentMonth: Int,
    modifier: Modifier = Modifier
) {
    val palette = LocalKannachPalette.current
    val language by viewModel.language.collectAsState()
    val strings = I18n.forLanguage(language)
    val entries by viewModel.entries.collectAsState()

    val monthPrefix = remember(currentYear, currentMonth) {
        String.format(Locale.US, "%04d-%02d", currentYear, currentMonth)
    }
    val monthEntries = remember(entries, monthPrefix) {
        entries.entries
            .filter { it.key.startsWith(monthPrefix) }
            .sortedWith(
                compareByDescending<Map.Entry<String, DayEntry>> { it.value.fav }
                    .thenByDescending { it.key }
            )
    }
    val monthEntriesCount = monthEntries.size
    val topDoodles = remember(monthEntries) { monthEntries.take(5) }
    val monthName = strings.months.getOrElse(currentMonth - 1) { "" }

    val recapText = if (monthEntriesCount > 0) {
        strings.monthRecapPlanted.format(language, monthEntriesCount, "month" to monthName)
    } else {
        strings.monthRecapQuiet
    }

    TearOffPageSurface(
        modifier = modifier.fillMaxSize(),
        showPunchedHoles = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp, bottom = 12.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = monthName,
                    fontSize = 36.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = LocalKannachHandFont.current,
                    color = palette.greenDeep,
                    maxLines = 1
                )
                Text(
                    text = I18n.formatYear(currentYear, language),
                    fontSize = 13.sp,
                    fontFamily = LocalKannachUiFont.current,
                    color = palette.inkSoft
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = recapText,
                    fontSize = 15.sp,
                    fontFamily = LocalKannachHandFont.current,
                    color = palette.inkSoft,
                    textAlign = TextAlign.Center
                )
            }

            // Strip of up to 5 doodles (56 dp each with small rotations) or faint dotted circle when 0 entries
            if (topDoodles.isNotEmpty()) {
                val rotations = floatArrayOf(-3.5f, 2.5f, -2.0f, 3.0f, -1.5f)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    topDoodles.forEachIndexed { idx, (dateKey, dayEntry) ->
                        val doodleColor = try {
                            Color(android.graphics.Color.parseColor(dayEntry.color))
                        } catch (_: Exception) {
                            palette.green
                        }
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .rotate(rotations[idx % rotations.size])
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    viewModel.vibrate(6)
                                    viewModel.openSheet(dateKey)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            GlyphView(
                                strokes = dayEntry.strokes,
                                color = doodleColor,
                                modifier = Modifier.size(50.dp)
                            )
                        }
                    }
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("month_page_empty_circle")
                ) {
                    drawCircle(
                        color = palette.inkFaint.copy(alpha = 0.45f),
                        radius = size.minDimension / 2f - 2.dp.toPx(),
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f),
                            cap = StrokeCap.Round
                        )
                    )
                }
            }
        }
    }
}

/**
 * HM4: Short screens compact 64 dp single-line strip (date + greeting).
 * Shown when remaining height < 160 dp so the page never scrolls.
 */
@Composable
fun CompactPageStrip(
    viewModel: MainViewModel,
    currentYear: Int,
    currentMonth: Int,
    modifier: Modifier = Modifier
) {
    val palette = LocalKannachPalette.current
    val language by viewModel.language.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val strings = I18n.forLanguage(language)
    val entries by viewModel.entries.collectAsState()

    val todayKey = viewModel.getTodayKey()
    val greetingText = rememberHomeGreetingText(entries, todayKey, language, strings)

    val cal = remember(todayKey) { Calendar.getInstance() }
    val nowYear = cal.get(Calendar.YEAR)
    val nowMonth = cal.get(Calendar.MONTH) + 1
    val nowDay = cal.get(Calendar.DAY_OF_MONTH)
    val isCurrentMonth = currentYear == nowYear && currentMonth == nowMonth

    val monthName = strings.months.getOrElse(currentMonth - 1) { "" }
    val leadText = if (isCurrentMonth) {
        "${I18n.formatYear(nowDay, language, userSettings.useArabicIndicNumerals)} $monthName"
    } else {
        monthName
    }
    val trailText = if (isCurrentMonth) {
        greetingText
    } else {
        val monthPrefix = String.format(Locale.US, "%04d-%02d", currentYear, currentMonth)
        val count = entries.keys.count { it.startsWith(monthPrefix) }
        if (count > 0) {
            strings.monthRecapPlanted.format(language, count, "month" to monthName)
        } else {
            strings.monthRecapQuiet
        }
    }

    TearOffPageSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        showPunchedHoles = false,
        onClick = if (isCurrentMonth) {
            {
                viewModel.vibrate(8)
                viewModel.openSheet(todayKey)
            }
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KannachIcon(
                icon = KannachIconType.SPROUT,
                tint = palette.green,
                strokeWidthDp = 1.8.dp,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = leadText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LocalKannachHandFont.current,
                color = palette.greenDeep,
                maxLines = 1
            )
            Text(
                text = "·",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = palette.inkFaint
            )
            Text(
                text = trailText,
                fontSize = 14.sp,
                fontFamily = LocalKannachHandFont.current,
                color = palette.inkSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
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

    // H5 & H9 & PF1: Slow, calm breathing animation for today (3200ms Breathe curve)
    // todayScale is read only inside today's DayCell graphicsLayer lambda so other cells and the grid never recompose.
    val hasTodayEntry = entries[todayKey] != null
    val todayBreathingTransition = rememberInfiniteTransition(label = "todayBreathe")
    val todayScaleAnim = todayBreathingTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (!hasTodayEntry) 1.06f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = KannachMotion.Breathe),
            repeatMode = RepeatMode.Reverse
        ),
        label = "todayBreathingScale"
    )

    val userSettings by viewModel.userSettings.collectAsState()
    val weekStartDow = userSettings.effectiveWeekStart(language)

    val firstDayOfWeekOffset = remember(currentYear, currentMonth, weekStartDow) {
        val cal = Calendar.getInstance()
        cal.set(currentYear, currentMonth - 1, 1)
        (cal.get(Calendar.DAY_OF_WEEK) - weekStartDow + 7) % 7
    }

    // Gesture detection for horizontal month swiping
    var totalDragX by remember { mutableStateOf(0f) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val totalHeight = maxHeight
        val totalWidth = maxWidth
        val dockInset = LocalDockInset.current

        // TC4: Grid yields before the card clips.
        // If the card's available height is below the Full threshold (230.dp),
        // MonthGrid lowers the cell height ratio from 0.9f down to a floor of 0.78f to give the card room.
        val totalCellsForCurrent = firstDayOfWeekOffset + daysInMonth
        val numWeeksForCurrent = ((totalCellsForCurrent + 6) / 7).coerceAtLeast(4)
        val gridAvailableWidth = (totalWidth - 24.dp).coerceAtLeast(140.dp)
        val singleCellWidth = ((gridAvailableWidth - 36.dp) / 7f).coerceAtLeast(20.dp)
        val nonGridOverhead = 4.dp + 28.dp + 8.dp + dockInset + ((numWeeksForCurrent - 1) * 4).dp
        val maxGridHeightForFullCard = totalHeight - nonGridOverhead - 230.dp
        val baseCellHeight = singleCellWidth / 0.9f
        val desiredCellHeight = maxGridHeightForFullCard / numWeeksForCurrent
        val cellRatioFloor = 0.78f
        val defaultCellRatio = 0.9f
        val yieldedRatio = if (desiredCellHeight >= baseCellHeight) {
            defaultCellRatio
        } else {
            val scale = (desiredCellHeight / baseCellHeight).coerceIn(cellRatioFloor / defaultCellRatio, 1f)
            (defaultCellRatio * scale).coerceIn(cellRatioFloor, defaultCellRatio)
        }
        // Compose aspectRatio is width / height; scaling height by (yieldedRatio / defaultCellRatio) increases width/height
        val effectiveComposeAspectRatio = defaultCellRatio * (defaultCellRatio / yieldedRatio)

        // HM1 & TC3: MonthGrid is a non-scrolling Column using LocalDockInset as bottom padding
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
                .padding(top = 4.dp)
                .padding(bottom = dockInset),
            verticalArrangement = Arrangement.Top
        ) {

            // Weekdays Row (ordered starting from weekStartDow)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .testTag("month_weekday_row"),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                val startShift = (weekStartDow - Calendar.SUNDAY + 7) % 7
                val weekDays = remember(strings.weekdays, startShift) {
                    (0 until 7).map { i -> strings.weekdays[(startShift + i) % 7] }
                }
                weekDays.forEachIndexed { idx, wDay ->
                    Text(
                        text = wDay,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.inkFaint,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("weekday_header_$idx")
                    )
                }
            }

            // 7-Column Grid of Month Days with Page Flip (D3, H8) & DL1–DL4
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
                val firstDayOffset = remember(targetYear, targetMonth, weekStartDow) {
                    val cal = Calendar.getInstance()
                    cal.set(targetYear, targetMonth - 1, 1)
                    (cal.get(Calendar.DAY_OF_WEEK) - weekStartDow + 7) % 7
                }

                val totalCells = firstDayOffset + daysInThisMonth
                val numWeeks = (totalCells + 6) / 7

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (weekIndex in 0 until numWeeks) {
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
                                            .aspectRatio(effectiveComposeAspectRatio)
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

                                    DayCell(
                                        viewModel = viewModel,
                                        day = day,
                                        dateKey = dateKey,
                                        entry = entry,
                                        isToday = isToday,
                                        isFuture = isFuture,
                                        isDimmed = isDimmed,
                                        rotation = rot,
                                        todayScale = if (isToday) todayScaleAnim else null,
                                        cellAspectRatio = effectiveComposeAspectRatio,
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
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // TC1–TC5: Tear-off day/month page owning all remaining height (weight(1f), heightIn(min = 0.dp))
            val nowCal = remember { Calendar.getInstance() }
            val isCurrentMonth = currentYear == nowCal.get(Calendar.YEAR) &&
                    currentMonth == (nowCal.get(Calendar.MONTH) + 1)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 0.dp)
                    .fillMaxWidth()
            ) {
                if (isCurrentMonth) {
                    TodayPageCard(
                        viewModel = viewModel,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("home_page_card")
                    )
                } else {
                    MonthPageCard(
                        viewModel = viewModel,
                        currentYear = currentYear,
                        currentMonth = currentMonth,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("home_page_card")
                    )
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
 * Single day cell in the month calendar (DL1–DL4):
 * - DL1: No ground lines or per-day connecting lines.
 * - DL2: Each written/today cell is a Column with a 14 dp reserved band at the top holding the day number
 *        (10 sp, palette.ink at 0.6 alpha, start-aligned), then the doodle area filling the rest
 *        (GlyphView with fillMaxSize() and 2 dp padding). No overlay, no negative offsets.
 * - DL3: Empty day keeps the HX5 stack: number and 5 dp dot centered together, one stack. Future days at 0.35 alpha.
 * - DL4: Today keeps its border and the breathing sprout (only when today is empty). Today's number is palette.green, 11 sp, semi-bold.
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
    todayScale: State<Float>? = null,
    cellAspectRatio: Float,
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
    val language by viewModel.language.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val dayStr = remember(day, language, userSettings.useArabicIndicNumerals) {
        I18n.formatNumber(day, language, userSettings.useArabicIndicNumerals)
    }
    val tightNumberStyle = remember {
        TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
    }

    Box(
        modifier = modifier
            .aspectRatio(cellAspectRatio)
            .offset { IntOffset(wobbleX.value.dp.roundToPx(), 0) }
            .clip(cellShape)
            .onGloballyPositioned { coords ->
                viewModel.registerCellBounds(dateKey, coords.positionInRoot(), coords.size)
            }
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
            .testTag("day_cell_$dateKey")
    ) {
        if (entry != null && !isFuture) {
            // DL2 & DL4: Column with 14 dp band at the top holding the day number, then doodle area filling the rest
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .padding(start = 3.dp, end = 2.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = dayStr,
                        fontSize = if (isToday) 11.sp else 10.sp,
                        lineHeight = if (isToday) 11.sp else 10.sp,
                        style = tightNumberStyle,
                        fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isToday) palette.green else palette.ink.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val glyphColor = try {
                        Color(android.graphics.Color.parseColor(entry.color))
                    } catch (_: Exception) {
                        palette.green
                    }

                    GlyphView(
                        strokes = entry.strokes,
                        color = glyphColor,
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(bloomScale)
                            .rotate(rotation)
                            .alpha(if (isDimmed) 0.16f else 1.0f)
                    )

                    if (isFreshlyPlanted) {
                        val firstCelebratedDate by viewModel.firstPlantCelebratedDate.collectAsState()
                        val isDoubleRing = firstCelebratedDate == dateKey
                        PetalBurst(color = glyphColor, isDoubleRing = isDoubleRing)
                    }
                }
            }
        } else if (isToday) {
            // DL4: Today unwritten: 14 dp top band with Today's number (palette.green, 11 sp, semi-bold) + breathing sprout below
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .padding(start = 3.dp, end = 2.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = dayStr,
                        fontSize = 11.sp,
                        lineHeight = 11.sp,
                        style = tightNumberStyle,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.green,
                        maxLines = 1
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    KannachIcon(
                        icon = KannachIconType.SPROUT,
                        tint = palette.green,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                val s = todayScale?.value ?: 1f
                                scaleX = s
                                scaleY = s
                            }
                    )
                }
            }
        } else {
            // DL3: Empty (past or future) day = number and 5 dp dot centered together, one stack. Future days at 0.35 alpha.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (isFuture) 0.35f else 0.7f)
            ) {
                Text(
                    text = dayStr,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.inkFaint
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(palette.dot)
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
            I18n.formatRecordedTime(entry.createdAt, language)
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
                val handSp = LocalHandwritingSp.current
                if (entry.text.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .linedPaperBackground(palette.line, (handSp + 12).dp)
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    ) {
                        Text(
                            text = entry.text,
                            fontSize = handSp.sp,
                            lineHeight = (handSp + 12).sp,
                            fontFamily = LocalKannachHandFont.current,
                            color = palette.ink,
                            modifier = Modifier.testTag("memory_card_text")
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
                                text = strings.prevEntry,
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
                                text = strings.nextEntry,
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
        val handSp = LocalHandwritingSp.current
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .linedPaperBackground(palette.line, (handSp + 12).dp)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            if (text.isEmpty()) {
                Text(
                    text = strings.entryPlaceholder,
                    fontSize = handSp.sp,
                    lineHeight = (handSp + 12).sp,
                    fontFamily = LocalKannachHandFont.current,
                    color = palette.inkFaint,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            BasicTextField(
                value = text,
                onValueChange = { viewModel.updateSheetText(it) },
                textStyle = TextStyle(
                    fontSize = handSp.sp,
                    lineHeight = (handSp + 12).sp,
                    fontFamily = LocalKannachHandFont.current,
                    color = palette.ink
                ),
                cursorBrush = SolidColor(palette.greenDeep),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("diary_entry_text_input")
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
                    text = strings.leaveMark,
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
/**
 * Interactive touch drawing surface capturing points on a virtual 300x400 canvas.
 * PF3: Uses awaitEachGesture without touch slop (a tap with no movement draws a dot),
 * caches committed strokes in remembered Paths, extends only live stroke per move via quadraticTo,
 * and redraws via a single mutableIntStateOf version counter.
 */
@Composable
fun DrawingCanvas(
    strokes: List<List<Point>>,
    colorHex: String,
    onStrokeCompleted: (List<Point>) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalKannachPalette.current
    val redrawVersion = remember { mutableIntStateOf(0) }
    val livePoints = remember { mutableListOf<Point>() }
    val livePath = remember { Path() }
    val drawColor = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (_: Exception) {
        palette.green
    }

    val latestOnStrokeCompleted by androidx.compose.runtime.rememberUpdatedState(onStrokeCompleted)

    val committedPaths = remember(strokes) {
        strokes.filter { it.isNotEmpty() }.map { stroke ->
            StrokeMath.composePathFrom(stroke) to stroke.first()
        }
    }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    val sx = 300f / size.width
                    val sy = 400f / size.height
                    val p0 = Point(
                        x = ((down.position.x * sx) * 10f).toInt() / 10f,
                        y = ((down.position.y * sy) * 10f).toInt() / 10f
                    )
                    livePoints.clear()
                    livePoints.add(p0)
                    livePath.reset()
                    livePath.moveTo(p0.x, p0.y)
                    livePath.lineTo(p0.x + 0.1f, p0.y + 0.1f)
                    var prevPoint = p0
                    var prevMidPoint: Point? = null
                    redrawVersion.intValue++

                    var hasMoved = false
                    drag(down.id) { change ->
                        change.consume()
                        val pt = Point(
                            x = ((change.position.x * sx) * 10f).toInt() / 10f,
                            y = ((change.position.y * sy) * 10f).toInt() / 10f
                        )
                        if (Math.hypot(
                                (pt.x - livePoints.last().x).toDouble(),
                                (pt.y - livePoints.last().y).toDouble()
                            ) > 2.0
                        ) {
                            livePoints.add(pt)
                            if (livePoints.size == 2) {
                                livePath.reset()
                                livePath.moveTo(p0.x, p0.y)
                                val mid = Point((p0.x + pt.x) / 2f, (p0.y + pt.y) / 2f)
                                livePath.lineTo(mid.x, mid.y)
                                prevMidPoint = mid
                            } else {
                                val mid = Point((prevPoint.x + pt.x) / 2f, (prevPoint.y + pt.y) / 2f)
                                livePath.quadraticTo(prevPoint.x, prevPoint.y, mid.x, mid.y)
                                prevMidPoint = mid
                            }
                            prevPoint = pt
                            hasMoved = true
                            redrawVersion.intValue++
                        }
                    }

                    if (livePoints.isNotEmpty()) {
                        if (hasMoved && livePoints.size >= 2) {
                            livePath.lineTo(prevPoint.x, prevPoint.y)
                        }
                        latestOnStrokeCompleted(livePoints.toList())
                        livePoints.clear()
                        livePath.reset()
                        redrawVersion.intValue++
                    }
                }
            }
    ) {
        val version = redrawVersion.intValue
        val scaleX = size.width / 300f
        val scaleY = size.height / 400f

        withTransform({
            scale(scaleX, scaleY, Offset.Zero)
        }) {
            for ((path, firstPt) in committedPaths) {
                drawCircle(
                    color = drawColor.copy(alpha = 0.9f),
                    radius = (8f * 1.25f) / 2f,
                    center = Offset(firstPt.x, firstPt.y)
                )
                drawPath(path = path, color = drawColor, style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            if (version >= 0 && livePoints.isNotEmpty()) {
                val firstPt = livePoints.first()
                drawCircle(
                    color = drawColor.copy(alpha = 0.9f),
                    radius = (8f * 1.25f) / 2f,
                    center = Offset(firstPt.x, firstPt.y)
                )
                drawPath(path = livePath, color = drawColor, style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

private data class CachedSketchStroke(
    val path: Path,
    val color: Color,
    val width: Float,
    val firstPoint: Point
)

/**
 * SK1 & SK3 & PF3 & JD3 & JD4: Parameterized DrawingCanvas overload for Just Draw (Draw tab)
 * supporting per-stroke color & width (SketchStroke) and 14-canvas-unit proximity eraser mode.
 * - JD3: One light tick on first stroke only (HapticFeedbackType.TextHandleMove), respecting
 *   the system touch-feedback setting. No haptics while drawing.
 * - JD4: Round caps and joins. The first point of each stroke also draws a dot of width * 1.25
 *   at 0.9 alpha (ink pooling). Width stays constant per stroke.
 */
@Composable
fun DrawingCanvas(
    sketchStrokes: List<SketchStroke>,
    currentColorHex: String,
    currentWidth: Float,
    isEraserMode: Boolean,
    onStrokeCompleted: (SketchStroke) -> Unit,
    onEraseAt: (Point) -> Unit,
    onStrokeStarted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalKannachPalette.current
    val hapticFeedback = androidx.compose.ui.platform.LocalHapticFeedback.current
    val hapticsEnabled = LocalHapticsEnabled.current
    val redrawVersion = remember { mutableIntStateOf(0) }
    val livePoints = remember { mutableListOf<Point>() }
    val livePath = remember { Path() }
    val activeColor = try {
        Color(android.graphics.Color.parseColor(currentColorHex))
    } catch (_: Exception) {
        palette.green
    }

    val latestEraserMode by androidx.compose.runtime.rememberUpdatedState(isEraserMode)
    val latestColorHex by androidx.compose.runtime.rememberUpdatedState(currentColorHex)
    val latestWidth by androidx.compose.runtime.rememberUpdatedState(currentWidth)
    val latestOnStrokeCompleted by androidx.compose.runtime.rememberUpdatedState(onStrokeCompleted)
    val latestOnEraseAt by androidx.compose.runtime.rememberUpdatedState(onEraseAt)
    val latestOnStrokeStarted by androidx.compose.runtime.rememberUpdatedState(onStrokeStarted)
    val latestStrokesEmpty by androidx.compose.runtime.rememberUpdatedState(sketchStrokes.isEmpty())
    val latestHapticsEnabled by androidx.compose.runtime.rememberUpdatedState(hapticsEnabled)

    val committedPaths = remember(sketchStrokes) {
        sketchStrokes.filter { it.points.isNotEmpty() }.map { stroke ->
            val strokeColor = try {
                Color(android.graphics.Color.parseColor(stroke.colorHex))
            } catch (_: Exception) {
                palette.green
            }
            CachedSketchStroke(
                path = StrokeMath.composePathFrom(stroke.points),
                color = strokeColor,
                width = stroke.width,
                firstPoint = stroke.points.first()
            )
        }
    }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    val wasFirstStroke = !latestEraserMode && latestStrokesEmpty
                    latestOnStrokeStarted()
                    if (wasFirstStroke && latestHapticsEnabled) {
                        hapticFeedback.performHapticFeedback(
                            androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove
                        )
                    }
                    val sx = 300f / size.width
                    val sy = 400f / size.height
                    val p0 = Point(
                        x = ((down.position.x * sx) * 10f).toInt() / 10f,
                        y = ((down.position.y * sy) * 10f).toInt() / 10f
                    )

                    if (latestEraserMode) {
                        latestOnEraseAt(p0)
                        drag(down.id) { change ->
                            change.consume()
                            val pt = Point(
                                x = ((change.position.x * sx) * 10f).toInt() / 10f,
                                y = ((change.position.y * sy) * 10f).toInt() / 10f
                            )
                            latestOnEraseAt(pt)
                        }
                    } else {
                        livePoints.clear()
                        livePoints.add(p0)
                        livePath.reset()
                        livePath.moveTo(p0.x, p0.y)
                        livePath.lineTo(p0.x + 0.1f, p0.y + 0.1f)
                        var prevPoint = p0
                        var prevMidPoint: Point? = null
                        redrawVersion.intValue++

                        var hasMoved = false
                        drag(down.id) { change ->
                            change.consume()
                            val pt = Point(
                                x = ((change.position.x * sx) * 10f).toInt() / 10f,
                                y = ((change.position.y * sy) * 10f).toInt() / 10f
                            )
                            if (Math.hypot(
                                    (pt.x - livePoints.last().x).toDouble(),
                                    (pt.y - livePoints.last().y).toDouble()
                                ) > 2.0
                            ) {
                                livePoints.add(pt)
                                if (livePoints.size == 2) {
                                    livePath.reset()
                                    livePath.moveTo(p0.x, p0.y)
                                    val mid = Point((p0.x + pt.x) / 2f, (p0.y + pt.y) / 2f)
                                    livePath.lineTo(mid.x, mid.y)
                                    prevMidPoint = mid
                                } else {
                                    val mid = Point((prevPoint.x + pt.x) / 2f, (prevPoint.y + pt.y) / 2f)
                                    livePath.quadraticTo(prevPoint.x, prevPoint.y, mid.x, mid.y)
                                    prevMidPoint = mid
                                }
                                prevPoint = pt
                                hasMoved = true
                                redrawVersion.intValue++
                            }
                        }

                        if (livePoints.isNotEmpty()) {
                            if (hasMoved && livePoints.size >= 2) {
                                livePath.lineTo(prevPoint.x, prevPoint.y)
                            }
                            latestOnStrokeCompleted(
                                SketchStroke(
                                    points = livePoints.toList(),
                                    colorHex = latestColorHex,
                                    width = latestWidth
                                )
                            )
                            livePoints.clear()
                            livePath.reset()
                            redrawVersion.intValue++
                        }
                    }
                }
            }
    ) {
        val version = redrawVersion.intValue
        val scaleX = size.width / 300f
        val scaleY = size.height / 400f

        withTransform({
            scale(scaleX, scaleY, Offset.Zero)
        }) {
            for (cached in committedPaths) {
                // JD4: First point of each stroke draws a dot of width * 1.25 at 0.9 alpha (ink pooling)
                drawCircle(
                    color = cached.color.copy(alpha = 0.9f),
                    radius = (cached.width * 1.25f) / 2f,
                    center = Offset(cached.firstPoint.x, cached.firstPoint.y)
                )
                drawPath(
                    path = cached.path,
                    color = cached.color,
                    style = Stroke(width = cached.width, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
            if (version >= 0 && !latestEraserMode && livePoints.isNotEmpty()) {
                val firstPt = livePoints.first()
                drawCircle(
                    color = activeColor.copy(alpha = 0.9f),
                    radius = (latestWidth * 1.25f) / 2f,
                    center = Offset(firstPt.x, firstPt.y)
                )
                drawPath(
                    path = livePath,
                    color = activeColor,
                    style = Stroke(width = latestWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
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
