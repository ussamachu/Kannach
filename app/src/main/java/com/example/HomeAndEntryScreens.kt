package com.example

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
 * 2. SplashScreen:
 * Animated entry splash screen showing the hand-drawn sprout logo and app title,
 * then gently fading out after 1.2s.
 */
@Composable
fun SplashScreen(
    appName: String,
    onFinished: () -> Unit
) {
    val palette = LocalKannachPalette.current
    var visible by remember { mutableStateOf(true) }
    var logoScale by remember { mutableStateOf(0.7f) }

    LaunchedEffect(Unit) {
        logoScale = 1.0f
        delay(1200)
        visible = false
        delay(400)
        onFinished()
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(animationSpec = tween(400))
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
                KannachIcon(
                    icon = KannachIconType.SPROUT,
                    tint = palette.greenDeep,
                    strokeWidthDp = 2.4.dp,
                    modifier = Modifier
                        .size(96.dp)
                        .scale(logoScale)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = appName,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.greenDeep,
                    fontFamily = FontFamily.Cursive
                )
            }
        }
    }
}

/**
 * 3. AppHeader:
 * Top sticky bar containing brand, navigation buttons, month stepper with season icon,
 * year counter chip, and the hand-drawn wavy underline.
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

    val yearCount = entries.keys.count { it.startsWith("$currentYear-") }
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
            .wavyHeaderDivider(palette.greenLine, waveHeightDp = 8.dp)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // Row 1: Brand & Top Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { viewModel.vibrate(6) }
            ) {
                KannachIcon(
                    icon = KannachIconType.SPROUT,
                    tint = palette.greenDeep,
                    strokeWidthDp = 2.dp,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = strings.appTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.greenDeep
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.openCapsulePage() },
                    modifier = Modifier.size(40.dp)
                ) {
                    KannachIcon(
                        icon = KannachIconType.SEAL,
                        tint = palette.greenDeep,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(
                    onClick = { viewModel.openGallery() },
                    modifier = Modifier.size(40.dp)
                ) {
                    KannachIcon(
                        icon = KannachIconType.GRID,
                        tint = palette.greenDeep,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(
                    onClick = { viewModel.openSettings() },
                    modifier = Modifier.size(40.dp)
                ) {
                    KannachIcon(
                        icon = KannachIconType.SLIDERS,
                        tint = palette.greenDeep,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Row 2: Month / Year Stepper
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.shiftMonth(-1) },
                enabled = !isAtMinYear,
                modifier = Modifier.size(40.dp)
            ) {
                KannachIcon(
                    icon = if (isRtl) KannachIconType.CHEV_R else KannachIconType.CHEV_L,
                    tint = if (isAtMinYear) palette.inkFaint else palette.ink,
                    modifier = Modifier.size(22.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                KannachIcon(
                    icon = seasonIcon,
                    tint = palette.green,
                    strokeWidthDp = 1.8.dp,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = I18n.formatMonthLabel(currentYear, currentMonth, language),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.greenDeep
                )
            }

            IconButton(
                onClick = { viewModel.shiftMonth(1) },
                enabled = !isAtMaxYear,
                modifier = Modifier.size(40.dp)
            ) {
                KannachIcon(
                    icon = if (isRtl) KannachIconType.CHEV_L else KannachIconType.CHEV_R,
                    tint = if (isAtMaxYear) palette.inkFaint else palette.ink,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Row 3: Year Count Chip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            val countStr = strings.galleryCount.replace("{n}", yearCount.toString())
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(palette.greenSoft)
                    .border(1.5.dp, palette.greenLine, CircleShape)
                    .clickable { viewModel.openGallery() }
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = countStr,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.greenDeep
                )
            }
        }
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

    // Pulsing animation for today's cell ring
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Gesture detection for horizontal month swiping
    var totalDragX by remember { mutableStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(currentYear, currentMonth, isRtl) {
                detectHorizontalDragGestures(
                    onDragStart = { totalDragX = 0f },
                    onHorizontalDrag = { _, dragAmount -> totalDragX += dragAmount },
                    onDragEnd = {
                        if (totalDragX > 70f) {
                            // Drag right -> previous in LTR, next in RTL
                            val delta = if (isRtl) 1 else -1
                            viewModel.shiftMonth(delta)
                        } else if (totalDragX < -70f) {
                            // Drag left -> next in LTR, prev in RTL
                            val delta = if (isRtl) -1 else 1
                            viewModel.shiftMonth(delta)
                        }
                    }
                )
            }
            .verticalScroll(rememberScrollState())
            .padding(bottom = 96.dp)
    ) {
        // Banner: On This Day
        onThisDayHit?.let { hit ->
            val titleStr = strings.onThisDay.replace("{y}", hit.year.toString())
            DashedBanner(
                title = titleStr,
                subtitle = hit.entry.text.takeIf { it.isNotBlank() },
                onBannerClick = { viewModel.openSheet(hit.key) },
                onCloseClick = { viewModel.dismissOnThisDay() }
            )
        }

        // Banner: Time Capsule Ready
        readyCapsule?.let { capsule ->
            DashedBanner(
                title = strings.capsuleReadyBanner,
                subtitle = strings.capsuleTitle,
                onBannerClick = { viewModel.openReadyCapsule(capsule) },
                onCloseClick = { viewModel.dismissReadyCapsule() }
            )
        }

        // Hint Banner if Month is empty
        val monthEntriesCount = entries.keys.count {
            val keyPrefix = String.format(java.util.Locale.US, "%04d-%02d", currentYear, currentMonth)
            it.startsWith(keyPrefix)
        }
        val favCount = entries.values.count { it.fav }
        val hintText = if (filter == "fav" && favCount == 0) {
            strings.noFavs
        } else if (monthEntriesCount == 0) {
            strings.emptyHint
        } else null

        if (hintText != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp)
                    .clip(HandDrawnRadiusShape)
                    .background(palette.greenSoft)
                    .dashedHandDrawnBorder(palette.greenLine, HandDrawnRadiusShape)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = hintText,
                    fontSize = 14.sp,
                    color = palette.greenDeep,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 7-Column Grid
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 700.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                userScrollEnabled = false
            ) {
                items(daysInMonth) { index ->
                    val day = index + 1
                    val dateKey = String.format(java.util.Locale.US, "%04d-%02d-%02d", currentYear, currentMonth, day)
                    val nextDayKey = String.format(java.util.Locale.US, "%04d-%02d-%02d", currentYear, currentMonth, day + 1)

                    val isToday = dateKey == todayKey
                    val isFuture = dateKey > todayKey
                    val entry = entries[dateKey]
                    val nextEntry = entries[nextDayKey]

                    // Chain connection to adjacent day in same row (if day % 7 != 0)
                    val sameRow = (day % 7 != 0) && (day < daysInMonth)
                    val chainNext = sameRow && entry != null && nextEntry != null &&
                            (filter == "all" || (entry.fav && nextEntry.fav))

                    // Organic rotation matching HTML
                    val rot = when {
                        day % 3 == 0 -> -4f
                        day % 4 == 0 -> 3f
                        day % 5 == 0 -> -2f
                        day % 7 == 0 -> 2.5f
                        else -> 0f
                    }

                    val isDimmed = entry != null && filter == "fav" && !entry.fav

                    DayCell(
                        day = day,
                        dateKey = dateKey,
                        entry = entry,
                        isToday = isToday,
                        isFuture = isFuture,
                        isDimmed = isDimmed,
                        chainNext = chainNext,
                        rotation = rot,
                        pulseScale = pulseScale,
                        pulseAlpha = pulseAlpha,
                        onClick = { viewModel.openSheet(dateKey) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    dateKey: String,
    entry: DayEntry?,
    isToday: Boolean,
    isFuture: Boolean,
    isDimmed: Boolean,
    chainNext: Boolean,
    rotation: Float,
    pulseScale: Float,
    pulseAlpha: Float,
    onClick: () -> Unit
) {
    val palette = LocalKannachPalette.current

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = !isFuture) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Chain connecting line to next day
        if (chainNext) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(2.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 4.dp)
                    .background(palette.greenLine)
            )
        }

        // Today pulsing ring
        if (isToday) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize(0.9f)
                    .scale(pulseScale)
                    .alpha(pulseAlpha)
            ) {
                drawCircle(
                    color = palette.green,
                    radius = size.minDimension / 2f - 2.dp.toPx(),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )
                )
            }
        }

        if (entry != null && !isFuture) {
            // Drawn Glyph
            val glyphColor = try {
                Color(android.graphics.Color.parseColor(entry.color))
            } catch (_: Exception) {
                palette.green
            }

            GlyphView(
                strokes = entry.strokes,
                color = glyphColor,
                modifier = Modifier
                    .fillMaxSize(0.74f)
                    .rotate(rotation)
                    .alpha(if (isDimmed) 0.16f else 1.0f)
            )
        } else {
            // Unwritten Dot
            val dotColor = when {
                isToday -> palette.green
                isFuture -> palette.dot.copy(alpha = 0.55f)
                else -> palette.dot
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

/**
 * BottomFilterBar:
 * Floating pill at bottom with Bouquet (all), Heart (favorites), and Brush (Just Draw).
 */
@Composable
fun BottomFilterBar(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalKannachPalette.current
    val filter by viewModel.gridFilter.collectAsState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(palette.paperHi)
                .border(2.dp, palette.ink, CircleShape)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // All entries button
            IconButton(
                onClick = { viewModel.setGridFilter("all") },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (filter == "all") palette.green else Color.Transparent)
            ) {
                KannachIcon(
                    icon = KannachIconType.BOUQUET,
                    tint = if (filter == "all") Color.White else palette.inkSoft,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Favorites button
            IconButton(
                onClick = { viewModel.setGridFilter("fav") },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (filter == "fav") palette.green else Color.Transparent)
            ) {
                KannachIcon(
                    icon = KannachIconType.HEART,
                    tint = if (filter == "fav") Color.White else palette.inkSoft,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Just Draw (Sketchpad) button
            IconButton(
                onClick = { viewModel.openSketchPage() },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
            ) {
                KannachIcon(
                    icon = KannachIconType.BRUSH,
                    tint = palette.inkSoft,
                    modifier = Modifier.size(22.dp)
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
    if (activeDateKey == null) return

    val dateKey = activeDateKey!!
    val mode by viewModel.sheetMode.collectAsState()
    val palette = LocalKannachPalette.current
    val language by viewModel.language.collectAsState()
    val strings = I18n.forLanguage(language)
    val formattedDate = I18n.formatDateTitle(dateKey, language)
    val isToday = dateKey == viewModel.getTodayKey()

    BackHandler {
        viewModel.requestCloseSheet()
    }

    // Outer Scrim
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = if (mode == SheetMode.VIEW) 0.58f else 0.42f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                viewModel.requestCloseSheet()
            },
        contentAlignment = if (mode == SheetMode.VIEW) Alignment.Center else Alignment.BottomCenter
    ) {
        if (mode == SheetMode.VIEW) {
            MemoryCardView(
                viewModel = viewModel,
                dateKey = dateKey,
                formattedDate = formattedDate,
                isToday = isToday
            )
        } else {
            EntryEditBottomSheet(
                viewModel = viewModel,
                dateKey = dateKey,
                formattedDate = formattedDate,
                isToday = isToday
            )
        }
    }
}

/**
 * View mode: Centered "Memory Card" rising out of the scrim.
 */
@Composable
private fun MemoryCardView(
    viewModel: MainViewModel,
    dateKey: String,
    formattedDate: String,
    isToday: Boolean
) {
    val palette = LocalKannachPalette.current
    val strings = I18n.forLanguage(viewModel.language.collectAsState().value)
    val entries by viewModel.entries.collectAsState()
    val entry = entries[dateKey] ?: DayEntry()

    val glyphColor = try {
        Color(android.graphics.Color.parseColor(entry.color))
    } catch (_: Exception) {
        palette.green
    }

    Box(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .widthIn(max = 440.dp)
            .heightIn(max = 680.dp)
            .clickable(enabled = false) {}
            .shadow(24.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .border(2.dp, palette.ink, RoundedCornerShape(22.dp))
            .paperBackground(palette.paper)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Date, Today Chip, Favorite & Edit Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
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
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Doodle Frame (aspect ratio 4:5, rotated -0.7 deg)
            Box(
                modifier = Modifier
                    .widthIn(max = 220.dp)
                    .aspectRatio(4f / 5f)
                    .rotate(-0.7f)
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

            Spacer(modifier = Modifier.height(20.dp))

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
                        fontSize = 19.sp,
                        lineHeight = 32.sp,
                        fontFamily = FontFamily.Cursive,
                        color = palette.ink
                    )
                }
            }
        }
    }
}

/**
 * Edit mode: Bottom Sheet with interactive drawing board, tools, and actions.
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
    val text by viewModel.sheetText.collectAsState()
    val strokes by viewModel.sheetStrokes.collectAsState()
    val selectedColor by viewModel.sheetColor.collectAsState()
    val isFav by viewModel.sheetFav.collectAsState()

    val entries by viewModel.entries.collectAsState()
    val hasExisting = entries.containsKey(dateKey)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 760.dp)
            .clickable(enabled = false) {}
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .border(2.dp, palette.ink, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .paperBackground(palette.paper)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Drag handle to dismiss
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.requestCloseSheet() }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 54.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(palette.inkFaint)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = formattedDate,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.greenDeep
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

                    IconButton(onClick = { viewModel.toggleSheetFav() }) {
                        KannachIcon(
                            icon = KannachIconType.HEART,
                            tint = if (isFav) palette.red else palette.inkFaint,
                            filled = isFav,
                            fillColor = palette.red.copy(alpha = 0.4f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Auto-growing Notebook Text Input
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)
                        .linedPaperBackground(palette.line, 32.dp)
                ) {
                    if (text.isEmpty()) {
                        Text(
                            text = strings.entryPlaceholder,
                            fontSize = 19.sp,
                            lineHeight = 32.sp,
                            fontFamily = FontFamily.Cursive,
                            color = palette.inkFaint,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = { viewModel.updateSheetText(it) },
                        textStyle = TextStyle(
                            fontSize = 19.sp,
                            lineHeight = 32.sp,
                            fontFamily = FontFamily.Cursive,
                            color = palette.ink
                        ),
                        cursorBrush = SolidColor(palette.greenDeep),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = strings.drawLabel,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Cursive,
                    color = palette.greenDeep,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Interactive Drawing Board (300x400 coordinate space)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .widthIn(max = 280.dp)
                        .aspectRatio(3f / 4f)
                        .rotate(-0.7f)
                        .align(Alignment.CenterHorizontally)
                        .shadow(12.dp, HandDrawnRadiusShape)
                        .clip(HandDrawnRadiusShape)
                        .background(palette.paperHi)
                        .border(2.dp, palette.ink, HandDrawnRadiusShape)
                ) {
                    DrawingCanvas(
                        strokes = strokes,
                        colorHex = selectedColor,
                        onStrokeCompleted = { viewModel.addSheetStroke(it) },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (strokes.isEmpty()) {
                        Text(
                            text = strings.drawHint,
                            fontSize = 15.sp,
                            color = palette.inkFaint,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tools: Undo & Clear
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

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = strings.colorLabel,
                    fontSize = 13.sp,
                    color = palette.inkFaint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Color Swatches
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
                                .size(32.dp)
                                .scale(if (isSelected) 1.12f else 1.0f)
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

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Bottom Actions Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .background(palette.paper)
                    .border(BorderStroke(1.dp, palette.line))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                if (hasExisting) {
                    Text(
                        text = strings.delete,
                        color = palette.red,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { viewModel.deleteCurrentEntry() }
                            .padding(8.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }

                Text(
                    text = strings.cancel,
                    color = palette.ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable { viewModel.requestCloseSheet() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(HandDrawnRadiusShape)
                        .background(palette.green)
                        .border(1.5.dp, palette.greenDeep, HandDrawnRadiusShape)
                        .clickable { viewModel.saveCurrentEntry() }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KannachIcon(
                            icon = KannachIconType.CHECK,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.save,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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
