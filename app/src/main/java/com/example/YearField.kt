package com.example

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

private const val COLS = 14

data class DayInfo(
    val dateKey: String,
    val month: Int,
    val day: Int
)

@Composable
fun YearHeader(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalKannachPalette.current
    val language by viewModel.language.collectAsState()
    val strings = I18n.forLanguage(language)
    val isRtl = I18n.isRtl(language)

    val currentYear by viewModel.currentYear.collectAsState()
    val filter by viewModel.gridFilter.collectAsState()

    val calendar = remember { Calendar.getInstance() }
    val nowYear = calendar.get(Calendar.YEAR)
    val nowMonth = calendar.get(Calendar.MONTH) + 1
    val isAtMinYear = currentYear <= viewModel.getMinYear()
    val isAtMaxYear = currentYear >= nowYear

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .paperBackground(palette.paper)
            .height(56.dp)
            .padding(horizontal = 16.dp),
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

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = { viewModel.shiftYear(-1) },
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

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.green)
                    .clickable { viewModel.setMonth(nowYear, nowMonth) }
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(
                    text = I18n.formatYear(currentYear, language),
                    fontSize = 20.sp,
                    fontFamily = LocalKannachHandFont.current,
                    fontWeight = FontWeight.Bold,
                    color = palette.paperHi,
                    maxLines = 1
                )
            }

            IconButton(
                onClick = { viewModel.shiftYear(1) },
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
                tint = if (isFavActive) palette.red else palette.inkSoft,
                filled = isFavActive,
                fillColor = palette.red.copy(alpha = 0.35f),
                contentDescription = strings.favorites,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun YearCaption(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalKannachPalette.current
    val language by viewModel.language.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val currentYear by viewModel.currentYear.collectAsState()
    val entries by viewModel.entries.collectAsState()
    val todayKey = viewModel.getTodayKey()

    val cal = remember(todayKey) { Calendar.getInstance() }
    val nowYear = cal.get(Calendar.YEAR)
    val nowMonth = cal.get(Calendar.MONTH) + 1
    val nowDay = cal.get(Calendar.DAY_OF_MONTH)

    val captionText = remember(currentYear, nowYear, nowMonth, nowDay, entries, language, userSettings.useArabicIndicNumerals) {
        if (currentYear == nowYear) {
            I18n.formatTodayRowDate(nowYear, nowMonth, nowDay, language, userSettings.useArabicIndicNumerals)
        } else {
            val yearPrefix = String.format(Locale.US, "%04d-", currentYear)
            val planted = entries.keys.count { it.startsWith(yearPrefix) }
            val daysInYear = Calendar.getInstance().apply {
                set(currentYear, Calendar.JANUARY, 1)
            }.getActualMaximum(Calendar.DAY_OF_YEAR)
            val plantedStr = I18n.formatNumber(planted, language, userSettings.useArabicIndicNumerals)
            val totalStr = I18n.formatNumber(daysInYear, language, userSettings.useArabicIndicNumerals)
            "$plantedStr / $totalStr"
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = captionText,
            fontSize = 15.sp,
            fontFamily = LocalKannachHandFont.current,
            color = palette.inkSoft,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .then(
                    if (currentYear == nowYear) {
                        Modifier.clickable { viewModel.openSheet(todayKey) }
                    } else {
                        Modifier
                    }
                )
                .testTag("year_caption")
        )
    }
}

@Composable
fun YearField(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onDayClick: ((String) -> Unit)? = null
) {
    val palette = LocalKannachPalette.current
    val language by viewModel.language.collectAsState()
    val strings = I18n.forLanguage(language)
    val isRtl = I18n.isRtl(language)

    val currentYear by viewModel.currentYear.collectAsState()
    val entries by viewModel.entries.collectAsState()
    val gridFilter by viewModel.gridFilter.collectAsState()
    val activeSheetDate by viewModel.activeSheetDate.collectAsState()
    val plantedCellPulse by viewModel.plantedCellPulse.collectAsState()
    val firstCelebratedDate by viewModel.firstPlantCelebratedDate.collectAsState()
    val plantingAnim by viewModel.plantingAnimation.collectAsState()

    val todayKey = viewModel.getTodayKey()
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

    val dayInfos = remember(currentYear) {
        val baseCal = Calendar.getInstance().apply {
            set(currentYear, Calendar.JANUARY, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val daysInYear = baseCal.getActualMaximum(Calendar.DAY_OF_YEAR)
        List(daysInYear) { i ->
            val cal = Calendar.getInstance().apply {
                set(currentYear, Calendar.JANUARY, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_YEAR, i)
            }
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH) + 1
            val d = cal.get(Calendar.DAY_OF_MONTH)
            DayInfo(
                dateKey = String.format(Locale.US, "%04d-%02d-%02d", y, m, d),
                month = m,
                day = d
            )
        }
    }

    val daysInYear = dayInfos.size
    val rows = ceil(daysInYear / 14f).toInt().coerceAtLeast(1)
    val density = LocalDensity.current
    var totalDragX by remember { mutableStateOf(0f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(currentYear, isRtl) {
                detectHorizontalDragGestures(
                    onDragStart = { totalDragX = 0f },
                    onHorizontalDrag = { _, dragAmount -> totalDragX += dragAmount },
                    onDragEnd = {
                        if (totalDragX > 70f) {
                            val delta = if (isRtl) 1 else -1
                            viewModel.shiftYear(delta)
                        } else if (totalDragX < -70f) {
                            val delta = if (isRtl) -1 else 1
                            viewModel.shiftYear(delta)
                        }
                    }
                )
            }
            .padding(top = 4.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        val widthBound = minOf(maxWidth - 32.dp, maxWidth) / COLS
        val heightBound = (maxHeight - 8.dp) / rows
        val cell = minOf(widthBound, heightBound).coerceAtLeast(8.dp)
        val cellPx = with(density) { cell.toPx() }
        val fieldWidth = cell * COLS
        val fieldHeight = cell * rows

        Box(
            modifier = Modifier
                .size(width = fieldWidth, height = fieldHeight)
                .pointerInput(currentYear, isRtl, cell, onDayClick) {
                    detectTapGestures { off ->
                        if (off.x < 0f || off.y < 0f || cellPx <= 0f) return@detectTapGestures
                        val rawCol = (off.x / cellPx).toInt()
                        val row = (off.y / cellPx).toInt()
                        if (rawCol !in 0..13 || row < 0) return@detectTapGestures
                        val col = if (isRtl) 13 - rawCol else rawCol
                        val i = row * COLS + col
                        if (col !in 0..13 || i !in 0 until daysInYear) return@detectTapGestures

                        val tappedDateKey = dayInfos[i].dateKey
                        if (onDayClick != null) {
                            viewModel.vibrate(8)
                            onDayClick(tappedDateKey)
                        } else {
                            val liveTodayKey = viewModel.getTodayKey()
                            if (tappedDateKey > liveTodayKey) {
                                viewModel.vibrate(10)
                                viewModel.showToast(strings.notYet)
                            } else {
                                viewModel.vibrate(8)
                                viewModel.openSheet(tappedDateKey)
                            }
                        }
                    }
                }
                .testTag("year_field_box")
        ) {
            val pastColor = palette.ink.copy(alpha = 0.55f)
            val futureColor = palette.ink.copy(alpha = 0.22f)

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clearAndSetSemantics { }
            ) {
                val rPast = 2.2.dp.toPx()
                val rFuture = 1.8.dp.toPx()
                val rMonthStart = 3.2.dp.toPx()
                val cPx = size.width / COLS

                for (i in 0 until daysInYear) {
                    val info = dayInfos[i]
                    val dKey = info.dateKey
                    val isFuture = dKey > todayKey

                    // Today without entry is handled below; days with an entry are drawn as doodles below
                    if (dKey == todayKey) continue
                    if (!isFuture && entries[dKey] != null) continue

                    val col = if (isRtl) 13 - (i % COLS) else i % COLS
                    val row = i / COLS
                    val jx = (((i * 7919) % 13) - 6) / 6f * 0.10f * cPx
                    val jy = (((i * 104729) % 13) - 6) / 6f * 0.10f * cPx
                    val cx = (col + 0.5f) * cPx + jx
                    val cy = (row + 0.5f) * cPx + jy

                    val radius = when {
                        info.day == 1 -> rMonthStart
                        isFuture -> rFuture
                        else -> rPast
                    }
                    val dotColor = if (isFuture) futureColor else pastColor

                    drawCircle(
                        color = dotColor,
                        radius = radius,
                        center = Offset(cx, cy)
                    )
                }
            }

            val glyphSize = cell * 0.92f
            val glyphSizePx = with(density) { glyphSize.toPx() }
            val todayDotSize = cell * 0.38f
            val todayDotSizePx = with(density) { todayDotSize.toPx() }
            val todayRingSize = cell * 0.62f
            val todayRingSizePx = with(density) { todayRingSize.toPx() }

            for (i in 0 until daysInYear) {
                val info = dayInfos[i]
                val dKey = info.dateKey
                val isToday = dKey == todayKey
                val isFuture = dKey > todayKey
                val entry = if (!isFuture) entries[dKey] else null
                val isPlantTarget = isToday ||
                    entry != null ||
                    dKey == activeSheetDate ||
                    dKey == plantingAnim?.dateKey ||
                    dKey == plantedCellPulse

                if (!isPlantTarget) continue

                val col = if (isRtl) 13 - (i % COLS) else i % COLS
                val row = i / COLS
                val jx = (((i * 7919) % 13) - 6) / 6f * 0.10f * cellPx
                val jy = (((i * 104729) % 13) - 6) / 6f * 0.10f * cellPx
                val cx = (col + 0.5f) * cellPx + jx
                val cy = (row + 0.5f) * cellPx + jy

                key(dKey) {
                    // 5. Plant animation target bounds registration (only for today, days with entries, and day being planted)
                    Box(
                        modifier = Modifier
                            .size(cell)
                            .offset {
                                IntOffset(
                                    (cx - cellPx / 2f).roundToInt(),
                                    (cy - cellPx / 2f).roundToInt()
                                )
                            }
                            .onGloballyPositioned { coords ->
                                if (onDayClick == null) {
                                    viewModel.registerCellBounds(dKey, coords.positionInRoot(), coords.size)
                                }
                            }
                    )

                    // 4. Today indicator: ring (breathing when empty, static when written) + filled circle when empty
                    if (isToday) {
                        Box(
                            modifier = Modifier
                                .size(todayRingSize)
                                .offset {
                                    IntOffset(
                                        (cx - todayRingSizePx / 2f).roundToInt(),
                                        (cy - todayRingSizePx / 2f).roundToInt()
                                    )
                                }
                                .graphicsLayer {
                                    val s = if (entry == null) todayScaleAnim.value else 1f
                                    scaleX = s
                                    scaleY = s
                                }
                                .border(1.4.dp, palette.green.copy(alpha = 0.5f), CircleShape)
                        )

                        if (entry == null) {
                            Box(
                                modifier = Modifier
                                    .size(todayDotSize)
                                    .offset {
                                        IntOffset(
                                            (cx - todayDotSizePx / 2f).roundToInt(),
                                            (cy - todayDotSizePx / 2f).roundToInt()
                                        )
                                    }
                                    .clip(CircleShape)
                                    .background(palette.green)
                            )
                        }
                    }

                    // 1, 2, 3. Doodles, favourites filter, and bloom + PetalBurst
                    if (entry != null) {
                        val isFreshlyPlanted = plantedCellPulse == dKey
                        var bloomTargetReached by remember(dKey, isFreshlyPlanted) {
                            mutableStateOf(!isFreshlyPlanted)
                        }
                        LaunchedEffect(isFreshlyPlanted) {
                            if (isFreshlyPlanted) {
                                bloomTargetReached = true
                                delay(750)
                                viewModel.clearPlantedCellPulse()
                            }
                        }
                        val bloomScale by animateFloatAsState(
                            targetValue = if (bloomTargetReached) 1f else 0.4f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "bloomScale"
                        )

                        val day = info.day
                        val rot = when {
                            day % 3 == 0 -> -2.5f
                            day % 4 == 0 -> 2f
                            day % 5 == 0 -> -1.5f
                            day % 7 == 0 -> 2f
                            else -> 0f
                        }
                        val isDimmed = gridFilter == "fav" && !entry.fav
                        val glyphColor = try {
                            Color(android.graphics.Color.parseColor(entry.color))
                        } catch (_: Exception) {
                            palette.green
                        }

                        val glyphOffset = IntOffset(
                            (cx - glyphSizePx / 2f).roundToInt(),
                            (cy - glyphSizePx / 2f).roundToInt()
                        )

                        GlyphView(
                            strokes = entry.strokes,
                            color = glyphColor,
                            modifier = Modifier
                                .size(glyphSize)
                                .offset { glyphOffset }
                                .scale(bloomScale)
                                .rotate(rot)
                                .alpha(if (isDimmed) 0.16f else 1.0f)
                        )

                        if (isFreshlyPlanted) {
                            Box(
                                modifier = Modifier
                                    .size(glyphSize)
                                    .offset { glyphOffset }
                            ) {
                                PetalBurst(
                                    color = glyphColor,
                                    isDoubleRing = firstCelebratedDate == dKey
                                )
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
                .padding(top = 2.dp, start = 8.dp, end = 8.dp)
        )
    }
}
