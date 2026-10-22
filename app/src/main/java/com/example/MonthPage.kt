package com.example

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale

@Composable
fun MonthPage(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalKannachPalette.current
    val handFont = LocalKannachHandFont.current

    val currentYear by viewModel.currentYear.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val entries by viewModel.entries.collectAsState()
    val gridFilter by viewModel.gridFilter.collectAsState()
    val language by viewModel.language.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()

    val strings = I18n.forLanguage(language)
    val isRtl = I18n.isRtl(language)

    val isAtMinYear = currentYear <= viewModel.getMinYear() && currentMonth <= 1
    val calendar = Calendar.getInstance()
    val nowYear = calendar.get(Calendar.YEAR)
    val nowMonth = calendar.get(Calendar.MONTH) + 1
    val nowDay = calendar.get(Calendar.DAY_OF_MONTH)
    val isCurrentMonth = currentYear == nowYear && currentMonth == nowMonth
    val isAtMaxYear = currentYear >= nowYear && currentMonth >= nowMonth

    val weekStartDow = userSettings.effectiveWeekStart(language)
    val baseSeed = currentYear * 12 + currentMonth
    val todayKey = viewModel.getTodayKey()

    var totalDragX by remember { mutableFloatStateOf(0f) }
    var showYearOverview by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val reduceMotion = rememberReduceMotion()
    val pageSlideDistance = remember(density) { with(density) { 24.dp.roundToPx() } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .paperBackground(palette.paper)
            .statusBarsPadding()
    ) {
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
        ) {
            // Header (no fixed height, no divider, no wavy line)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // 1. Top row 48 dp: settings IconButton at start, centered app name Text, heart IconButton at end
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

                    Text(
                        text = strings.appTitle,
                        fontSize = 20.sp,
                        fontFamily = handFont,
                        color = palette.inkSoft,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )

                    val isFavActive = gridFilter == "fav"
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
                            tint = if (isFavActive) palette.red else palette.inkSoft,
                            filled = isFavActive,
                            fillColor = palette.red.copy(alpha = 0.35f),
                            contentDescription = strings.favorites,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // 2. Month numeral with side chevrons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.shiftMonth(-1) },
                        enabled = !isAtMinYear,
                        modifier = Modifier
                            .size(40.dp)
                            .alpha(if (isAtMinYear) 0.3f else 1f)
                    ) {
                        KannachIcon(
                            icon = if (isRtl) KannachIconType.CHEV_R else KannachIconType.CHEV_L,
                            tint = palette.inkSoft,
                            contentDescription = strings.prevMonth,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    val headerNumber = if (isCurrentMonth) nowDay else currentMonth
                    Text(
                        text = I18n.formatNumber(headerNumber, language, userSettings.useArabicIndicNumerals),
                        fontSize = 46.sp,
                        lineHeight = 52.sp,
                        fontFamily = handFont,
                        color = if (isCurrentMonth) palette.ink else palette.ink.copy(alpha = 0.45f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clickable { showYearOverview = true }
                            .padding(horizontal = 8.dp)
                    )

                    IconButton(
                        onClick = { viewModel.shiftMonth(1) },
                        enabled = !isAtMaxYear,
                        modifier = Modifier
                            .size(40.dp)
                            .alpha(if (isAtMaxYear) 0.3f else 1f)
                    ) {
                        KannachIcon(
                            icon = if (isRtl) KannachIconType.CHEV_L else KannachIconType.CHEV_R,
                            tint = palette.inkSoft,
                            contentDescription = strings.nextMonth,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // 3. Month-and-year line
                val rawMonthYear = "${strings.months.getOrElse(currentMonth - 1) { "" }}  ${I18n.formatYear(currentYear, language)}"
                val monthYearLabel = if (language == "ar") rawMonthYear else rawMonthYear.uppercase()
                Text(
                    text = monthYearLabel,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = palette.inkSoft,
                    letterSpacing = if (language == "ar") 0.sp else 3.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.clickable(enabled = !isCurrentMonth) {
                        if (!isCurrentMonth) {
                            viewModel.setMonth(nowYear, nowMonth)
                        }
                    }
                )

                // 4. Bottom spacer
                Spacer(Modifier.height(10.dp))
            }

            // Top hand-drawn line above weekday row (Canvas 6 dp tall: palette.line, 1.2 dp, wobble 1.5 dp)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .height(6.dp)
            ) {
                val y = size.height / 2f
                inkLine(
                    from = Offset(0f, y),
                    to = Offset(size.width, y),
                    seed = baseSeed * 10 + 1,
                    color = palette.line,
                    widthPx = 1.2.dp.toPx(),
                    wobblePx = 1.5.dp.toPx()
                )
            }

            // Weekday row: 7 equal Texts, 12 sp, SemiBold, hand font, ink @ 0.8 (Saturday & Sunday: red @ 0.85)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .testTag("month_weekday_row"),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val startShift = (weekStartDow - Calendar.SUNDAY + 7) % 7
                val fullWeekdays = remember(language) { I18n.getFullWeekdays(language) }
                for (i in 0 until 7) {
                    val dayIdx = (startShift + i) % 7
                    val calDow = Calendar.SUNDAY + dayIdx
                    val isWeekend = calDow == Calendar.SATURDAY || calDow == Calendar.SUNDAY
                    val wDay = strings.weekdays[dayIdx]
                    val fullDay = fullWeekdays[dayIdx]

                    Text(
                        text = wDay,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = handFont,
                        color = if (isWeekend) palette.red.copy(alpha = 0.85f) else palette.ink.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = fullDay }
                            .testTag("weekday_header_$i")
                    )
                }
            }

            // Bottom hand-drawn line below weekday row (Canvas 6 dp tall: palette.line, 1.2 dp, wobble 1.5 dp)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .height(6.dp)
            ) {
                val y = size.height / 2f
                inkLine(
                    from = Offset(0f, y),
                    to = Offset(size.width, y),
                    seed = baseSeed * 10 + 2,
                    color = palette.line,
                    widthPx = 1.2.dp.toPx(),
                    wobblePx = 1.5.dp.toPx()
                )
            }

            // M3 & M4: Grid area under the weekday row with horizontal page-flip AnimatedContent
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(bottom = LocalDockInset.current + 8.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                val availW = maxWidth
                val availH = maxHeight

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
                    val numWeeks = ((totalCells + 6) / 7).coerceIn(5, 6)
                    val cellW = availW / 7
                    val cellH = minOf(availH / numWeeks, cellW * 2.2f)
                    val gridHeight = cellH * numWeeks

                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(gridHeight),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            // ONE Canvas behind the cells: horizontal inkLines under rows 1..numWeeks, vertical inkLines at cols 1..6
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val lineW = 1.3.dp.toPx()
                                val wobble = 2.dp.toPx()
                                val rowH = size.height / numWeeks.toFloat()
                                val colW = size.width / 7f

                                for (r in 1..numWeeks) {
                                    val y = rowH * r
                                    inkLine(
                                        from = Offset(0f, y),
                                        to = Offset(size.width, y),
                                        seed = targetYear * 100 + targetMonth * 10 + r,
                                        color = palette.line,
                                        widthPx = lineW,
                                        wobblePx = wobble
                                    )
                                }

                                for (c in 1..6) {
                                    val x = colW * c
                                    inkLine(
                                        from = Offset(x, 0f),
                                        to = Offset(x, size.height),
                                        seed = c + 40,
                                        color = palette.line,
                                        widthPx = lineW,
                                        wobblePx = wobble
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                for (weekIndex in 0 until numWeeks) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                    ) {
                                        for (col in 0 until 7) {
                                            val cellIndex = weekIndex * 7 + col
                                            if (cellIndex < firstDayOffset || (cellIndex - firstDayOffset) >= daysInThisMonth) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                )
                                            } else {
                                                val day = cellIndex - firstDayOffset + 1
                                                val dateKey = String.format(Locale.US, "%04d-%02d-%02d", targetYear, targetMonth, day)
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
                                                val isDimmed = entry != null && gridFilter == "fav" && !entry.fav

                                                MonthDayCell(
                                                    viewModel = viewModel,
                                                    day = day,
                                                    dateKey = dateKey,
                                                    entry = entry,
                                                    isToday = isToday,
                                                    isFuture = isFuture,
                                                    isDimmed = isDimmed,
                                                    rotation = rot,
                                                    cellW = cellW,
                                                    cellH = cellH,
                                                    language = language,
                                                    useArabicIndicNumerals = userSettings.useArabicIndicNumerals,
                                                    reduceMotion = reduceMotion,
                                                    notYetMessage = strings.notYet,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        FoldedPaperCorner(
            viewModel = viewModel,
            modifier = Modifier
                .align(if (isRtl) Alignment.TopStart else Alignment.TopEnd)
                .padding(top = 56.dp, start = 8.dp, end = 8.dp)
        )

        AnimatedVisibility(
            visible = showYearOverview,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.fillMaxSize()
        ) {
            BackHandler {
                showYearOverview = false
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .paperBackground(palette.paper)
                    .padding(bottom = LocalDockInset.current)
            ) {
                YearHeader(viewModel = viewModel)
                YearField(
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f),
                    onDayClick = { dateKey ->
                        val parts = dateKey.split("-")
                        if (parts.size >= 2) {
                            val year = parts[0].toIntOrNull()
                            val month = parts[1].toIntOrNull()
                            if (year != null && month != null) {
                                viewModel.setMonth(year, month)
                            }
                        }
                        showYearOverview = false
                    }
                )
            }
        }
    }
}

@Composable
private fun MonthDayCell(
    viewModel: MainViewModel,
    day: Int,
    dateKey: String,
    entry: DayEntry?,
    isToday: Boolean,
    isFuture: Boolean,
    isDimmed: Boolean,
    rotation: Float,
    cellW: Dp,
    cellH: Dp,
    language: String,
    useArabicIndicNumerals: Boolean,
    reduceMotion: Boolean,
    notYetMessage: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalKannachPalette.current
    val handFont = LocalKannachHandFont.current

    val plantedCellPulse by viewModel.plantedCellPulse.collectAsState()
    val isFreshlyPlanted = plantedCellPulse == dateKey

    LaunchedEffect(isFreshlyPlanted) {
        if (isFreshlyPlanted) {
            delay(750)
            viewModel.clearPlantedCellPulse()
        }
    }

    val todayCircleAnim = remember(dateKey) { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(isToday, reduceMotion) {
        if (isToday) {
            if (reduceMotion) {
                todayCircleAnim.snapTo(1f)
            } else {
                todayCircleAnim.snapTo(0f)
                todayCircleAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 450, easing = KannachMotion.Settle)
                )
            }
        }
    }

    val bloomScale by animateFloatAsState(
        targetValue = if (entry != null) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bloomScale"
    )

    val dayStr = remember(day, language, useArabicIndicNumerals) {
        I18n.formatNumber(day, language, useArabicIndicNumerals)
    }
    val tightNumberStyle = remember {
        TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
    }

    var numberCenter by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                viewModel.registerCellBounds(dateKey, coords.positionInRoot(), coords.size)
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (isFuture) {
                    viewModel.vibrate(10)
                    viewModel.showToast(notYetMessage)
                } else {
                    viewModel.openSheet(dateKey)
                }
            }
            .testTag("day_cell_$dateKey")
    ) {
        if (isToday) {
            val circleProgress = if (reduceMotion) 1f else todayCircleAnim.value
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = if (numberCenter != Offset.Zero) {
                    numberCenter
                } else {
                    Offset(12.dp.toPx(), 12.dp.toPx())
                }
                inkCircle(
                    center = center,
                    radiusPx = 11.dp.toPx(),
                    seed = day,
                    color = palette.red.copy(alpha = 0.85f),
                    widthPx = 1.6.dp.toPx(),
                    progress = circleProgress
                )
            }
        }

        Text(
            text = dayStr,
            fontSize = 11.sp,
            style = tightNumberStyle,
            fontFamily = handFont,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (isToday) palette.ink else palette.ink.copy(alpha = if (isFuture) 0.35f else 0.85f),
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                .onGloballyPositioned { coords ->
                    numberCenter = coords.boundsInParent().center
                }
        )

        if (entry != null && !isFuture) {
            val glyphColor = try {
                Color(android.graphics.Color.parseColor(entry.color))
            } catch (_: Exception) {
                palette.green
            }

            val doodleSize = minOf(cellW, cellH - 22.dp) * 0.86f

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 22.dp),
                contentAlignment = Alignment.Center
            ) {
                GlyphView(
                    strokes = entry.strokes,
                    color = glyphColor,
                    modifier = Modifier
                        .size(doodleSize)
                        .scale(bloomScale)
                        .rotate(rotation)
                        .alpha(if (isDimmed) 0.16f else 1.0f)
                )
            }

            if (isFreshlyPlanted) {
                val firstCelebratedDate by viewModel.firstPlantCelebratedDate.collectAsState()
                val isDoubleRing = firstCelebratedDate == dateKey
                PetalBurst(
                    color = glyphColor,
                    isDoubleRing = isDoubleRing,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
