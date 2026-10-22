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
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
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
 * NV1–NV6b: The Paper Dock.
 * - Floating pill with 4 equal top-level tab slots (Page, Garden, Letters, Draw) + detached round Plant button.
 * - NV5 Pill: 16 dp side margins, bottom = system nav inset + 12 dp, height 64 dp,
 *   shape = HandDrawnDockShape (32/28/30/26 dp asymmetric), fill palette.paperHi,
 *   1.5 dp border palette.greenLine, stitched inner edge (dashed 1.2 dp, dash 8/6, inset 4 dp, greenLine @ 0.7 alpha),
 *   soft paper shadow from tokens (paperShadow), no Material elevation.
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

    // DF4: Cap dock label scaling: provide LocalDensity with fontScale = min(fontScale, 1.15f) for the dock only
    val baseDensity = LocalDensity.current
    val dockDensity = remember(baseDensity.density, baseDensity.fontScale) {
        Density(density = baseDensity.density, fontScale = min(baseDensity.fontScale, 1.15f))
    }

    CompositionLocalProvider(LocalDensity provides dockDensity) {
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
                    .wrapContentWidth()
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
                // NV5 & H5: Stitched Paper Pill
                Row(
                    modifier = Modifier
                        .height(54.dp)
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
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Page (Month Home)
                    PaperDockSlot(
                        doodle = DockDoodle.PAGE,
                        label = strings.dockPage,
                        selected = currentTab == Tab.PAGE,
                        reduceMotion = reduceMotion,
                        enabled = dockState == DockState.EXPANDED,
                        onClick = { viewModel.selectTab(Tab.PAGE) },
                        modifier = Modifier
                            .width(if (ENABLE_LETTERS) 64.dp else 76.dp)
                            .testTag("dock_tab_page")
                    )

                    // 2. Garden (Gallery)
                    PaperDockSlot(
                        doodle = DockDoodle.GARDEN,
                        label = strings.dockGarden,
                        selected = currentTab == Tab.GARDEN,
                        reduceMotion = reduceMotion,
                        enabled = dockState == DockState.EXPANDED,
                        onClick = { viewModel.selectTab(Tab.GARDEN) },
                        modifier = Modifier
                            .width(if (ENABLE_LETTERS) 64.dp else 76.dp)
                            .testTag("dock_tab_garden")
                    )

                    // 3. Draw (Sketchpad)
                    PaperDockSlot(
                        doodle = DockDoodle.DRAW,
                        label = strings.dockDraw,
                        selected = currentTab == Tab.DRAW,
                        reduceMotion = reduceMotion,
                        enabled = dockState == DockState.EXPANDED,
                        onClick = {
                            viewModel.selectTab(Tab.DRAW)
                            onCollapseDock()
                        },
                        modifier = Modifier
                            .width(if (ENABLE_LETTERS) 64.dp else 76.dp)
                            .testTag("dock_tab_draw")
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // NV6b & DK3 & H5: Detached Plant button inside the expanded dock only (54 dp circle, stitched inner ring inset 5 dp)
                Box(
                    modifier = Modifier
                        .size(54.dp)
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
}

@Composable
private fun PaperDockSlot(
    doodle: DockDoodle,
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

    // DK6 & DF1-DF3: No shape clip on slot; drawBehind draws the highlight;
    // icon 24dp + 2dp + label 14dp ≈ 40dp inside 64dp pill (vertical padding 6dp from pill Row).
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxHeight()
            .heightIn(min = 48.dp)
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
            .padding(horizontal = 6.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (doodle) {
                DockDoodle.PAGE -> DockDoodleIcon(
                    doodle = DockDoodle.PAGE,
                    selected = selected,
                    modifier = Modifier.size(32.dp)
                )
                DockDoodle.GARDEN -> DockDoodleIcon(
                    doodle = DockDoodle.GARDEN,
                    selected = selected,
                    modifier = Modifier.size(32.dp)
                )
                DockDoodle.DRAW -> DockDoodleIcon(
                    doodle = DockDoodle.DRAW,
                    selected = selected,
                    modifier = Modifier.size(32.dp)
                )
            }
            if (hasBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(palette.red)
                )
            }
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
        ENABLE_LETTERS && readyCapsule != null -> "capsule"
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
 * W3 & R3: Delicate petal burst blossoming around the cell upon landing.
 * If isDoubleRing is true (celebrating user's very first mark in history),
 * blossoms with a celebratory double ring of outer petals!
 */
@Composable
internal fun PetalBurst(
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
                .clip(HandDrawnRadiusShape)
                .background(palette.paper)
                .paperBackground(palette.paper)
                .padding(bottom = 18.dp, start = 18.dp, end = 18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top hand-drawn line handle (40.dp wide, palette.inkFaint, 2.dp, centred, 8.dp from the top)
                Canvas(
                    modifier = Modifier
                        .padding(top = 8.dp, bottom = 6.dp)
                        .width(40.dp)
                        .height(6.dp)
                ) {
                    inkLine(
                        from = Offset(0f, size.height / 2f),
                        to = Offset(size.width, size.height / 2f),
                        seed = dateKey.hashCode() + 7,
                        color = palette.inkFaint,
                        widthPx = 2.dp.toPx(),
                        wobblePx = 1.2.dp.toPx()
                    )
                }

                // Header: Date, Distance line, Favorite, Edit & Close Buttons (WH1-WH3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = formattedDate,
                            fontSize = 17.sp,
                            lineHeight = 24.sp,
                            style = ArabicDisplayStyle,
                            fontWeight = FontWeight.Bold,
                            color = palette.greenDeep,
                            maxLines = 2,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        // WH2 & M3: Poetic time distance line (if isToday, prefix with {today} · {relative} or just {today})
                        val cleanRelative = relativeDistance.takeIf {
                            it.isNotBlank() &&
                                !it.equals(strings.today, ignoreCase = true) &&
                                !it.equals(strings.todayWord, ignoreCase = true)
                        }.orEmpty()
                        val distanceLineText = if (isToday) {
                            if (cleanRelative.isNotBlank()) "${strings.today} · $cleanRelative" else strings.today
                        } else {
                            relativeDistance
                        }
                        if (distanceLineText.isNotBlank()) {
                            Text(
                                text = distanceLineText,
                                fontSize = 12.5.sp,
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

                // Scrollable content below the header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Doodle drawn directly on the paper
                    GlyphView(
                        strokes = entry.strokes,
                        color = glyphColor,
                        modifier = Modifier
                            .fillMaxWidth(0.62f)
                            .widthIn(max = 260.dp)
                            .aspectRatio(1f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Short hand-drawn line under doodle (width 56.dp, palette.line, 1.4.dp, wobble 1.5.dp)
                    Canvas(
                        modifier = Modifier
                            .width(56.dp)
                            .height(6.dp)
                    ) {
                        inkLine(
                            from = Offset(0f, size.height / 2f),
                            to = Offset(size.width, size.height / 2f),
                            seed = dateKey.hashCode(),
                            color = palette.line,
                            widthPx = 1.4.dp.toPx(),
                            wobblePx = 1.5.dp.toPx()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Ruled Notebook Text
                    val handSp = LocalHandwritingSp.current
                    val lineSpacingPx = with(LocalDensity.current) { (handSp * 1.75f).sp.toPx() }
                    val ruledLineStyle = TextStyle(
                        fontSize = handSp.sp,
                        lineHeight = (handSp * 1.75f).sp,
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Bottom,
                            trim = LineHeightStyle.Trim.None
                        ),
                        fontFamily = LocalKannachHandFont.current,
                        color = palette.ink
                    )
                    if (entry.text.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .linedPaperBackground(palette.line, lineSpacingPx)
                                .padding(start = 4.dp, end = 4.dp, top = 0.dp, bottom = 4.dp)
                        ) {
                            Text(
                                text = entry.text,
                                fontSize = handSp.sp,
                                lineHeight = (handSp * 1.75f).sp,
                                style = ruledLineStyle,
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

    // Full-bleed paper page (W1 / D5)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(enabled = false) {}
            .background(palette.paper)
            .paperBackground(palette.paper)
            .systemBarsPadding()
            .imePadding()
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
 * Pulse 1 — Arrive & Write (W1, W5, W6, W8, D5).
 * Calm full-bleed paper page with centered date pill, unruled text area, and solid Leave mark button.
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
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(dateKey) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

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
        // Top bar (height 56.dp, no divider)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
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

            val language by viewModel.language.collectAsState()
            val relativeDistance = remember(dateKey, language) {
                I18n.getRelativeTimeDistance(dateKey, language)
            }
            val cleanRelative = relativeDistance.takeIf {
                it.isNotBlank() &&
                    !it.equals(strings.today, ignoreCase = true) &&
                    !it.equals(strings.todayWord, ignoreCase = true)
            }.orEmpty()
            val relativeWord = if (isToday) strings.today else cleanRelative.ifBlank { relativeDistance }
            val pillDateText = if (relativeWord.isNotBlank()) {
                "$formattedDate · $relativeWord"
            } else {
                formattedDate
            }

            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.paperLo)
                    .border(1.2.dp, palette.line, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = pillDateText,
                    fontSize = 14.sp,
                    fontFamily = LocalKannachHandFont.current,
                    color = palette.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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

        // Writing area: unruled calm paper
        val writeTextStyle = TextStyle(
            fontSize = 22.sp,
            lineHeight = 34.sp,
            fontFamily = LocalKannachHandFont.current,
            color = palette.ink
        )

        Spacer(modifier = Modifier.height(96.dp))

        BasicTextField(
            value = text,
            onValueChange = { viewModel.updateSheetText(it) },
            textStyle = writeTextStyle,
            cursorBrush = SolidColor(palette.green),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 28.dp)
                .focusRequester(focusRequester)
                .testTag("diary_entry_text_input"),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxSize()) {
                    if (text.isEmpty()) {
                        Text(
                            text = strings.entryPlaceholder,
                            style = writeTextStyle.copy(color = palette.inkFaint)
                        )
                    }
                    innerTextField()
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom: solid Leave mark button at the end side
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.green)
                    .clickable { viewModel.setWriteStep(2) }
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.leaveMark,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = LocalKannachHandFont.current,
                    color = palette.onGreen
                )
                Spacer(modifier = Modifier.width(8.dp))
                KannachIcon(
                    icon = if (isRtl) KannachIconType.CHEV_L else KannachIconType.CHEV_R,
                    tint = palette.onGreen,
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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
    val points: List<Point>,
    val path: Path,
    val color: Color,
    val width: Float,
    val firstPoint: Point,
    val tool: String = "pen"
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
    currentTool: String = "pen",
    onStrokeStarted: () -> Unit = {},
    onGestureEnded: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalKannachPalette.current
    val hapticFeedback = androidx.compose.ui.platform.LocalHapticFeedback.current
    val hapticsEnabled = LocalHapticsEnabled.current
    val redrawVersion = remember { mutableIntStateOf(0) }
    val livePoints = remember { mutableListOf<Point>() }
    val livePath = remember { Path() }
    val eraserTouchPoint = remember { mutableStateOf<Point?>(null) }
    val activeColor = try {
        Color(android.graphics.Color.parseColor(currentColorHex))
    } catch (_: Exception) {
        palette.green
    }
    val eraserRadius = when {
        currentWidth <= 4f -> 8f
        currentWidth <= 8f -> 14f
        else -> 24f
    }

    val latestEraserMode by androidx.compose.runtime.rememberUpdatedState(isEraserMode)
    val latestColorHex by androidx.compose.runtime.rememberUpdatedState(currentColorHex)
    val latestWidth by androidx.compose.runtime.rememberUpdatedState(currentWidth)
    val latestTool by androidx.compose.runtime.rememberUpdatedState(currentTool)
    val latestEraserRadius by androidx.compose.runtime.rememberUpdatedState(eraserRadius)
    val latestOnStrokeCompleted by androidx.compose.runtime.rememberUpdatedState(onStrokeCompleted)
    val latestOnEraseAt by androidx.compose.runtime.rememberUpdatedState(onEraseAt)
    val latestOnStrokeStarted by androidx.compose.runtime.rememberUpdatedState(onStrokeStarted)
    val latestOnGestureEnded by androidx.compose.runtime.rememberUpdatedState(onGestureEnded)
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
                points = stroke.points,
                path = StrokeMath.composePathFrom(stroke.points),
                color = strokeColor,
                width = stroke.width,
                firstPoint = stroke.points.first(),
                tool = stroke.tool
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
                        eraserTouchPoint.value = p0
                        latestOnEraseAt(p0)
                        redrawVersion.intValue++
                        var prevErasePt = p0
                        try {
                            drag(down.id) { change ->
                                change.consume()
                                val pt = Point(
                                    x = ((change.position.x * sx) * 10f).toInt() / 10f,
                                    y = ((change.position.y * sy) * 10f).toInt() / 10f
                                )
                                eraserTouchPoint.value = pt
                                val dx = pt.x - prevErasePt.x
                                val dy = pt.y - prevErasePt.y
                                val dist = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                                if (dist > 3f) {
                                    val steps = kotlin.math.ceil(dist / 3f).toInt()
                                    for (s in 1..steps) {
                                        val frac = s.toFloat() / steps.toFloat()
                                        latestOnEraseAt(
                                            Point(
                                                x = ((prevErasePt.x + dx * frac) * 10f).toInt() / 10f,
                                                y = ((prevErasePt.y + dy * frac) * 10f).toInt() / 10f
                                            )
                                        )
                                    }
                                } else {
                                    latestOnEraseAt(pt)
                                }
                                prevErasePt = pt
                                redrawVersion.intValue++
                            }
                        } finally {
                            eraserTouchPoint.value = null
                            latestOnGestureEnded()
                            redrawVersion.intValue++
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
                                    width = latestWidth,
                                    tool = latestTool
                                )
                            )
                            livePoints.clear()
                            livePath.reset()
                            latestOnGestureEnded()
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
            val quadPath = Path()
            val crayonDash = PathEffect.dashPathEffect(floatArrayOf(7f, 2f))

            for (cached in committedPaths) {
                when (cached.tool) {
                    "brush" -> {
                        for ((a, b, segW) in StrokeMath.brushSegments(cached.points, cached.width)) {
                            drawLine(
                                color = cached.color,
                                start = Offset(a.x, a.y),
                                end = Offset(b.x, b.y),
                                strokeWidth = segW,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    "calligraphy" -> {
                        for (quad in StrokeMath.calligraphyQuads(cached.points, cached.width)) {
                            if (quad.size == 4) {
                                quadPath.reset()
                                quadPath.moveTo(quad[0].x, quad[0].y)
                                quadPath.lineTo(quad[1].x, quad[1].y)
                                quadPath.lineTo(quad[2].x, quad[2].y)
                                quadPath.lineTo(quad[3].x, quad[3].y)
                                quadPath.close()
                                drawPath(path = quadPath, color = cached.color)
                            }
                        }
                        drawPath(
                            path = cached.path,
                            color = cached.color,
                            style = Stroke(width = cached.width * 0.25f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                    "crayon" -> {
                        val pointIndex = cached.points.size
                        for (passIndex in 0 until 3) {
                            val dx = ((passIndex * 37 + pointIndex) % 5 - 2) * 0.35f
                            val dy = ((passIndex * 53) % 5 - 2) * 0.35f
                            withTransform({ translate(dx, dy) }) {
                                drawPath(
                                    path = cached.path,
                                    color = cached.color.copy(alpha = 0.6f),
                                    style = Stroke(
                                        width = cached.width * 0.8f,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round,
                                        pathEffect = crayonDash
                                    )
                                )
                            }
                        }
                    }
                    else -> {
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
                }
            }
            if (version >= 0 && !latestEraserMode && livePoints.isNotEmpty()) {
                when (currentTool) {
                    "brush" -> {
                        for ((a, b, segW) in StrokeMath.brushSegments(livePoints, latestWidth)) {
                            drawLine(
                                color = activeColor,
                                start = Offset(a.x, a.y),
                                end = Offset(b.x, b.y),
                                strokeWidth = segW,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    "calligraphy" -> {
                        for (quad in StrokeMath.calligraphyQuads(livePoints, latestWidth)) {
                            if (quad.size == 4) {
                                quadPath.reset()
                                quadPath.moveTo(quad[0].x, quad[0].y)
                                quadPath.lineTo(quad[1].x, quad[1].y)
                                quadPath.lineTo(quad[2].x, quad[2].y)
                                quadPath.lineTo(quad[3].x, quad[3].y)
                                quadPath.close()
                                drawPath(path = quadPath, color = activeColor)
                            }
                        }
                        drawPath(
                            path = livePath,
                            color = activeColor,
                            style = Stroke(width = latestWidth * 0.25f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                    "crayon" -> {
                        val pointIndex = livePoints.size
                        for (passIndex in 0 until 3) {
                            val dx = ((passIndex * 37 + pointIndex) % 5 - 2) * 0.35f
                            val dy = ((passIndex * 53) % 5 - 2) * 0.35f
                            withTransform({ translate(dx, dy) }) {
                                drawPath(
                                    path = livePath,
                                    color = activeColor.copy(alpha = 0.6f),
                                    style = Stroke(
                                        width = latestWidth * 0.8f,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round,
                                        pathEffect = crayonDash
                                    )
                                )
                            }
                        }
                    }
                    else -> {
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
            // ER3: Visible eraser ring while eraser is active and a finger is down (radius r, palette.ink at 0.5 alpha, 1.5 dp, no fill)
            val erasePt = eraserTouchPoint.value
            if (version >= 0 && latestEraserMode && erasePt != null) {
                drawCircle(
                    color = palette.ink.copy(alpha = 0.5f),
                    radius = latestEraserRadius,
                    center = Offset(erasePt.x, erasePt.y),
                    style = Stroke(width = 1.5.dp.toPx() / scaleX)
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
    val handFont = LocalKannachHandFont.current
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = pagerState.currentPage > 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(pagerState.currentPage - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .paperBackground(palette.paper)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top row (56.dp, horizontal padding 24.dp): 4 page dots in centre, Skip at the end (hidden on last page)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val isCurrent = pagerState.currentPage == i
                    val dotSize by animateDpAsState(
                        targetValue = if (isCurrent) 10.dp else 8.dp,
                        animationSpec = tween(200),
                        label = "OnboardingDotSize"
                    )
                    Canvas(modifier = Modifier.size(12.dp)) {
                        val r = dotSize.toPx() / 2f
                        if (isCurrent) {
                            drawCircle(
                                color = palette.ink,
                                radius = r,
                                center = center
                            )
                        } else {
                            drawCircle(
                                color = palette.inkFaint,
                                radius = r,
                                center = center,
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                    }
                }
            }

            if (pagerState.currentPage < 3) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .heightIn(min = 48.dp)
                        .widthIn(min = 48.dp)
                        .clickable { onFinish() }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.onboardingSkip,
                        fontSize = 15.sp,
                        fontFamily = handFont,
                        color = palette.inkSoft
                    )
                }
            }
        }

        // Four-page HorizontalPager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val isCurrentPage = pagerState.currentPage == page
            val textAlpha = (1f - abs(pageOffset)).coerceIn(0f, 1f)

            val title = when (page) {
                0 -> strings.onboardingBeat1
                1 -> strings.onboardingBeat2
                2 -> strings.onboardingBeat3
                else -> strings.onboardingBeat4
            }
            val subtitle = when (page) {
                0 -> strings.onboardingBeat1Sub
                1 -> strings.onboardingBeat2Sub
                2 -> strings.onboardingBeat3Sub
                else -> strings.onboardingBeat4Sub
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(0.6f))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .graphicsLayer {
                            translationX = pageOffset * 40.dp.toPx()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when (page) {
                        0 -> DrawnScene(SceneWelcome, active = isCurrentPage, modifier = Modifier.fillMaxSize(), swayDegrees = 2.5f)
                        1 -> SceneDrawDay(active = isCurrentPage, modifier = Modifier.fillMaxSize())
                        2 -> SceneYearGrid(active = isCurrentPage)
                        else -> DrawnScene(ScenePrivate, active = isCurrentPage, modifier = Modifier.fillMaxSize(), pulse = 0.05f)
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = handFont,
                    color = palette.ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer {
                        alpha = textAlpha
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = subtitle,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontFamily = handFont,
                    color = palette.inkSoft,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .graphicsLayer {
                            alpha = textAlpha
                        }
                )

                Spacer(modifier = Modifier.weight(0.4f))
            }
        }

        // Bottom button (height 56.dp, fillMaxWidth(0.8f), bottom padding 28.dp, centred)
        Box(
            modifier = Modifier
                .padding(bottom = 28.dp)
                .fillMaxWidth(0.8f)
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(palette.green)
                .clickable {
                    if (pagerState.currentPage < 3) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onFinish()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = if (pagerState.currentPage < 3) strings.onboardingNext else strings.onboardingStart,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)).togetherWith(fadeOut(animationSpec = tween(200)))
                },
                label = "OnboardingButtonText"
            ) { label ->
                Text(
                    text = label,
                    fontSize = 18.sp,
                    fontFamily = handFont,
                    fontWeight = FontWeight.Bold,
                    color = palette.onGreen,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

