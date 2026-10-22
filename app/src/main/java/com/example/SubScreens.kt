package com.example

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 1. GalleryScreen (حديقتك الصغيرة):
 * Full screen gallery mosaic of all doodles written in the current year,
 * with remaining unwritten days represented as delicate dots.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GalleryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalKannachPalette.current
    val language by viewModel.language.collectAsState()
    val strings = I18n.forLanguage(language)
    val currentYear by viewModel.currentYear.collectAsState()
    val entries by viewModel.entries.collectAsState()

    val todayCal = Calendar.getInstance()
    val todayKey = String.format(Locale.US, "%04d-%02d-%02d",
        todayCal.get(Calendar.YEAR),
        todayCal.get(Calendar.MONTH) + 1,
        todayCal.get(Calendar.DAY_OF_MONTH)
    )

    val yearEntries = remember(entries, currentYear) {
        entries.filter { it.key.startsWith("$currentYear-") && (it.value.strokes.isNotEmpty() || it.value.text.isNotBlank()) }
    }

    val writtenEntries = remember(yearEntries) {
        yearEntries.toList().sortedBy { it.first }
    }

    var searchQuery by remember { mutableStateOf("") }

    val matchingEntries = remember(yearEntries, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else yearEntries.filter { (_, entry) ->
            entry.text.contains(searchQuery, ignoreCase = true) ||
            searchQuery.equals(entry.color, ignoreCase = true)
        }.toList()
    }

    // Group months from the user's active history
    val activeMonthsData = remember(entries, currentYear) {
        val monthsWithEntries = entries.keys
            .filter { it.startsWith("$currentYear-") }
            .mapNotNull { it.split("-").getOrNull(1)?.toIntOrNull() }
            .toSet()
        val cal = Calendar.getInstance()
        val thisYear = cal.get(Calendar.YEAR)
        val thisMonth = cal.get(Calendar.MONTH) + 1
        val maxMonth = if (currentYear == thisYear) thisMonth else 12
        val minMonth = if (monthsWithEntries.isNotEmpty()) monthsWithEntries.minOrNull() ?: 1 else thisMonth

        (minMonth..maxMonth).map { m ->
            val monthStr = String.format(Locale.US, "%02d", m)
            val writtenInMonth = entries.filter { (k, e) ->
                k.startsWith("$currentYear-$monthStr-") && (e.strokes.isNotEmpty() || e.text.isNotBlank())
            }.toList().sortedBy { it.first }

            val calM = Calendar.getInstance().apply {
                set(Calendar.YEAR, currentYear)
                set(Calendar.MONTH, m - 1)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val daysInM = calM.getActualMaximum(Calendar.DAY_OF_MONTH)
            val limitDay = if (currentYear == thisYear && m == thisMonth) cal.get(Calendar.DAY_OF_MONTH) else daysInM
            val unwrittenDays = (1..limitDay).filter { d ->
                val dKey = String.format(Locale.US, "%04d-%02d-%02d", currentYear, m, d)
                entries[dKey] == null || (entries[dKey]!!.strokes.isEmpty() && entries[dKey]!!.text.isBlank())
            }
            Triple(m, writtenInMonth, unwrittenDays)
        }
    }

    val dockInset = LocalDockInset.current
    val dockScroll = LocalDockScroll.current
    val listState = rememberLazyListState()
    val isAtTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
    }
    LaunchedEffect(isAtTop) {
        if (isAtTop) {
            dockScroll.onReachedTop()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .paperBackground(palette.paper)
            .clickable(enabled = false) {}
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = dockInset),
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(dockScroll)
        ) {
            // Header (NV7: Settings icon in leading 48dp slot)
            item(key = "gallery_header", contentType = "header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wavyHeaderDivider(palette.greenLine, waveHeightDp = 6.dp)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { viewModel.openSettings() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        KannachIcon(
                            icon = KannachIconType.SLIDERS,
                            tint = palette.inkSoft,
                            contentDescription = strings.openSettings,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = strings.galleryTitle,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = LocalKannachHandFont.current,
                        color = palette.greenDeep
                    )
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }

            // G2: Poetic gallery sentence with days count in handwriting font
            item(key = "gallery_count", contentType = "count") {
                Text(
                    text = strings.galleryCount.format(language, yearEntries.size),
                    fontSize = 17.sp,
                    fontFamily = LocalKannachHandFont.current,
                    color = palette.greenDeep,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp)
                )
            }

            // G5: Action Links: Quiet handwritten text links (Year Wrap & Surprise Me)
            item(key = "gallery_action_links", contentType = "action_links") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.wrapBtn,
                        fontSize = 14.5.sp,
                        fontFamily = LocalKannachHandFont.current,
                        color = palette.greenDeep,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { viewModel.openWrap() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )

                    Text(
                        text = "·",
                        fontSize = 16.sp,
                        color = palette.inkFaint,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                val randKey = viewModel.getRandomEntryDateKey()
                                if (randKey != null) {
                                    viewModel.openSheet(randKey)
                                } else {
                                    viewModel.showToast(strings.noMemoriesYet)
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        KannachIcon(
                            icon = KannachIconType.SPARKLE,
                            tint = palette.orange,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.surpriseMe,
                            fontSize = 14.5.sp,
                            fontFamily = LocalKannachHandFont.current,
                            color = palette.inkSoft
                        )
                    }
                }
            }

            // Light Paper Search Bar without extra redundant icon
            item(key = "gallery_search_bar", contentType = "search_bar") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.paperHi)
                        .border(1.dp, palette.line.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = strings.searchHint,
                                    fontSize = 13.sp,
                                    fontFamily = LocalKannachUiFont.current,
                                    color = palette.inkFaint
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                textStyle = TextStyle(
                                    fontSize = 13.sp,
                                    fontFamily = LocalKannachUiFont.current,
                                    color = palette.ink
                                ),
                                cursorBrush = SolidColor(palette.greenDeep),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(20.dp)
                            ) {
                                KannachIcon(
                                    icon = KannachIconType.X,
                                    tint = palette.inkFaint,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
            item(key = "gallery_spacer_search", contentType = "spacer") {
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (searchQuery.isNotBlank()) {
                if (matchingEntries.isEmpty()) {
                    item(key = "gallery_search_empty", contentType = "empty") {
                        Text(
                            text = strings.searchNoMatch.replace("{q}", searchQuery),
                            fontSize = 15.sp,
                            fontFamily = LocalKannachHandFont.current,
                            color = palette.inkFaint,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        )
                    }
                } else {
                    item(key = "gallery_search_results", contentType = "search_results") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .clip(HandDrawnRadiusShape)
                                .background(palette.paperHi)
                                .border(1.dp, palette.line.copy(alpha = 0.6f), HandDrawnRadiusShape)
                                .padding(14.dp)
                        ) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 1400.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                userScrollEnabled = false
                            ) {
                                items(matchingEntries.size, key = { matchingEntries[it].first }, contentType = { "search_entry" }) { idx ->
                                    val (dateKey, entry) = matchingEntries[idx]
                                    val glyphColor = try {
                                        Color(android.graphics.Color.parseColor(entry.color))
                                    } catch (_: Exception) {
                                        palette.green
                                    }
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                viewModel.openSheet(dateKey)
                                            }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.size(62.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            GlyphView(
                                                strokes = entry.strokes,
                                                color = glyphColor,
                                                modifier = Modifier.fillMaxSize(0.92f)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = dateKey,
                                            fontSize = 11.sp,
                                            fontFamily = LocalKannachHandFont.current,
                                            color = palette.inkSoft,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // G2, G3, G4: Mosaic divided by months with written drawings first + gentle stagger
                if (writtenEntries.isEmpty()) {
                    item(key = "gallery_empty_card", contentType = "empty") {
                        Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                            .clip(HandDrawnRadiusShape)
                            .background(palette.paperHi)
                            .border(1.dp, palette.line.copy(alpha = 0.6f), HandDrawnRadiusShape)
                            .padding(22.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            KannachIcon(
                                icon = KannachIconType.SPROUT,
                                tint = palette.green,
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = strings.galleryEmptyTitle,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = LocalKannachHandFont.current,
                                color = palette.greenDeep,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = strings.galleryEmptySub,
                                fontSize = 13.sp,
                                fontFamily = LocalKannachHandFont.current,
                                color = palette.inkSoft,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(palette.greenSoft)
                                    .border(1.2.dp, palette.greenLine, CircleShape)
                                    .clickable {
                                        viewModel.openSheet(todayKey)
                                    }
                                    .padding(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    KannachIcon(
                                        icon = KannachIconType.SPARKLE,
                                        tint = palette.greenDeep,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = strings.plantTodayBtn,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = palette.greenDeep
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                items(
                    items = activeMonthsData,
                    key = { "month_gallery_block_${it.first}" },
                    contentType = { "month_gallery_block" }
                ) { (monthNum, writtenInMonth, unwrittenDays) ->
                            if (writtenInMonth.isNotEmpty() || unwrittenDays.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp)
                                        .clip(HandDrawnRadiusShape)
                                        .background(palette.paperHi.copy(alpha = 0.9f))
                                        .border(1.dp, palette.line.copy(alpha = 0.5f), HandDrawnRadiusShape)
                                        .padding(14.dp)
                                ) {
                                    // Month Title in handwriting
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = I18n.forLanguage(language).months.getOrElse(monthNum - 1) { "" },
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = LocalKannachHandFont.current,
                                            color = palette.greenDeep
                                        )
                                        Text(
                                            text = if (writtenInMonth.isNotEmpty()) "${writtenInMonth.size}" else "",
                                            fontSize = 13.sp,
                                            fontFamily = LocalKannachHandFont.current,
                                            color = palette.inkFaint
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Written drawings with gentle stagger <= 40ms
                                    if (writtenInMonth.isNotEmpty()) {
                                        LazyVerticalGrid(
                                            columns = GridCells.Fixed(4),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 1200.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            userScrollEnabled = false
                                        ) {
                                            items(
                                                count = writtenInMonth.size,
                                                key = { idx -> "month_${monthNum}_entry_${writtenInMonth[idx].first}" },
                                                contentType = { "month_entry" }
                                            ) { idx ->
                                                val (dateKey, entry) = writtenInMonth[idx]
                                                val glyphColor = try {
                                                    Color(android.graphics.Color.parseColor(entry.color))
                                                } catch (_: Exception) {
                                                    palette.green
                                                }
                                                val rot = remember(dateKey) {
                                                    val h = dateKey.hashCode()
                                                    ((h % 7) - 3) * 1.0f
                                                }

                                                val anim = remember { Animatable(0.7f) }
                                                LaunchedEffect(dateKey) {
                                                    delay((idx * 30L).coerceAtMost(300L))
                                                    anim.animateTo(1f, tween(300, easing = KannachMotion.Settle))
                                                }

                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .graphicsLayer {
                                                            alpha = anim.value
                                                            scaleX = anim.value
                                                            scaleY = anim.value
                                                        }
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable {
                                                            viewModel.openSheet(dateKey)
                                                        }
                                                        .padding(vertical = 4.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier.size(62.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        GlyphView(
                                                            strokes = entry.strokes,
                                                            color = glyphColor,
                                                            modifier = Modifier
                                                                .fillMaxSize(0.92f)
                                                                .rotate(rot)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    val dateLabel = remember(dateKey, language) {
                                                        try {
                                                            val parts = dateKey.split("-")
                                                            if (parts.size == 3) {
                                                                val d = parts[2].toIntOrNull() ?: 1
                                                                val mName = I18n.forLanguage(language).months.getOrElse(monthNum - 1) { "" }
                                                                "$d $mName"
                                                            } else dateKey
                                                        } catch (_: Exception) { dateKey }
                                                    }
                                                    Text(
                                                        text = dateLabel,
                                                        fontSize = 11.sp,
                                                        fontFamily = LocalKannachHandFont.current,
                                                        color = palette.inkSoft,
                                                        textAlign = TextAlign.Center,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Unwritten days placed quietly at the end of the month group
                                    if (unwrittenDays.isNotEmpty()) {
                                        if (writtenInMonth.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                        }
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            unwrittenDays.forEach { d ->
                                                val dateKey = String.format(Locale.US, "%04d-%02d-%02d", currentYear, monthNum, d)
                                                val isToday = dateKey == todayKey
                                                Box(
                                                    modifier = Modifier
                                                        .size(if (isToday) 8.dp else 4.5.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isToday) palette.greenDeep else palette.dot.copy(alpha = 0.45f))
                                                        .clickable {
                                                            viewModel.openSheet(dateKey)
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
            }
        }
    }

/**
 * 2. YearWrapScreen (موسم السنة):
 * Annual summary screen presenting stats (days written, longest streak, top color, favorites count)
 * and a 7-column mosaic of all entries.
 */
@Composable
fun YearWrapScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalKannachPalette.current
    val language by viewModel.language.collectAsState()
    val isRtl = I18n.isRtl(language)
    val strings = I18n.forLanguage(language)
    val currentYear by viewModel.currentYear.collectAsState()
    val wrapData = remember(viewModel, currentYear) { viewModel.getYearWrapData(currentYear) }
    val entries by viewModel.entries.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .paperBackground(palette.paper)
            .clickable(enabled = false) {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, palette.line))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { viewModel.closeWrap() }) {
                    KannachIcon(
                        icon = if (isRtl) KannachIconType.CHEV_R else KannachIconType.CHEV_L,
                        tint = palette.ink,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = strings.wrapTitle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.greenDeep
                )
                Spacer(modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Year Hero
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = I18n.formatYear(currentYear, language),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = LocalKannachHandFont.current,
                    color = palette.greenDeep
                )
                Text(
                    text = strings.wrapSub,
                    fontSize = 16.sp,
                    color = palette.inkSoft
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2x2 Stats Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    value = wrapData.count.toString(),
                    label = strings.wrapDays.format(language, wrapData.count),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = wrapData.bestStreak.toString(),
                    label = strings.wrapStreak,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Color Card
                val topColorObj = try {
                    Color(android.graphics.Color.parseColor(wrapData.topColor))
                } catch (_: Exception) {
                    palette.green
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(HandDrawnRadiusShape)
                        .background(palette.paperHi)
                        .border(1.5.dp, palette.line, HandDrawnRadiusShape)
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (wrapData.count > 0) topColorObj else Color.Transparent)
                                .border(1.5.dp, palette.ink, CircleShape)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = strings.wrapColor,
                            fontSize = 13.sp,
                            color = palette.inkSoft,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                StatCard(
                    value = wrapData.favCount.toString(),
                    label = strings.wrapFavs,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Year Mosaic
            if (wrapData.keys.isEmpty()) {
                Text(
                    text = strings.wrapEmpty,
                    fontSize = 15.sp,
                    color = palette.inkFaint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 600.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    userScrollEnabled = false
                ) {
                    items(wrapData.keys) { key ->
                        val entry = entries[key]
                        val glyphColor = try {
                            Color(android.graphics.Color.parseColor(entry?.color ?: DEFAULT_GREEN_HEX))
                        } catch (_: Exception) {
                            palette.green
                        }

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(palette.paperHi)
                                .border(1.5.dp, palette.line, RoundedCornerShape(9.dp))
                                .padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (entry != null) {
                                GlyphView(
                                    strokes = entry.strokes,
                                    color = glyphColor,
                                    modifier = Modifier.fillMaxSize()
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
private fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalKannachPalette.current

    Box(
        modifier = modifier
            .clip(HandDrawnRadiusShape)
            .background(palette.paperHi)
            .border(1.5.dp, palette.line, HandDrawnRadiusShape)
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LocalKannachHandFont.current,
                color = palette.greenDeep
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                color = palette.inkSoft,
                textAlign = TextAlign.Center
            )
        }
    }
}


/**
 * 4. SketchpadScreen (فقط ارسم / Just Draw — SK1–SK12 & v5 JD1–JD7 & F3):
 * Full-bleed sheet of paper with a slim 52.dp top bar, whisper centered inside the empty canvas,
 * and a two-row tools tray at the bottom.
 */
@Composable
fun SketchpadScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalKannachPalette.current
    val language by viewModel.language.collectAsState()
    val strings = I18n.forLanguage(language)
    val context = LocalContext.current
    val reduceMotion = rememberReduceMotion()

    val strokes by viewModel.sketchStrokes.collectAsState()
    val canUndo by viewModel.canUndoSketch.collectAsState()
    val canRedo by viewModel.canRedoSketch.collectAsState()
    val selectedColor by viewModel.sketchColor.collectAsState()
    val brushWidth by viewModel.sketchBrushWidth.collectAsState()
    val isEraserMode by viewModel.sketchEraserMode.collectAsState()
    val sketchTool by viewModel.sketchTool.collectAsState()
    val paperType by viewModel.sketchPaperType.collectAsState()
    val introDismissed by viewModel.sketchIntroDismissed.collectAsState()
    val whisperIndex by viewModel.sketchWhisperIndex.collectAsState()
    val moodLabelVisible by viewModel.sketchMoodLabelVisible.collectAsState()
    val pageTurnCount by viewModel.sketchPageTurnCount.collectAsState()
    val clearUndoVisible by viewModel.sketchClearUndoVisible.collectAsState()
    val savedWhisperIndex by viewModel.sketchSavedWhisperIndex.collectAsState()
    val isExportSheetOpen by viewModel.isExportSheetOpen.collectAsState()
    val exportFormat by viewModel.exportFormat.collectAsState()
    val exportSignature by viewModel.exportSignature.collectAsState()
    var isHeaderOverflowOpen by remember { mutableStateOf(false) }
    var showPaperPopover by remember { mutableStateOf(false) }
    var showSizePopover by remember { mutableStateOf(false) }

    val inkHex = "#3a523d"
    val allSwatches = remember {
        I18n.COLORS.map { it.hex } + listOf(inkHex)
    }
    val brushSizes = remember { listOf(4f, 8f, 14f) }

    val dockInset = LocalDockInset.current
    val dockScroll = LocalDockScroll.current

    // JD1: Whisper line fades in 320 ms after entry and fades out at the first stroke
    var whisperFadeInReady by remember { mutableStateOf(reduceMotion) }
    LaunchedEffect(whisperIndex, reduceMotion) {
        if (reduceMotion) {
            whisperFadeInReady = true
        } else {
            whisperFadeInReady = false
            kotlinx.coroutines.delay(KannachMotion.SOFT.toLong())
            whisperFadeInReady = true
        }
    }

    val drawWhispers = remember(language) { I18n.getDrawWhispers(language) }
    val drawSavedWhispers = remember(language) { I18n.getDrawSavedWhispers(language) }
    val currentWhisperText = drawWhispers.getOrElse(whisperIndex.coerceIn(0, 3)) { strings.drawWhisper1 }
    val currentSavedWhisperText = savedWhisperIndex?.let { idx ->
        drawSavedWhispers.getOrElse(idx.coerceIn(0, 1)) { strings.drawSaved1 }
    }

    // Show post-export whisper (JD6, 2.4 s) if active; otherwise entry whisper (JD1) until first stroke
    val activeTopWhisper: String? = when {
        currentSavedWhisperText != null -> currentSavedWhisperText
        whisperFadeInReady && !introDismissed && strokes.isEmpty() -> currentWhisperText
        else -> null
    }

    val currentInkColor = remember(selectedColor, palette.ink) {
        try {
            Color(android.graphics.Color.parseColor(selectedColor))
        } catch (_: Exception) {
            palette.ink
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .paperBackground(palette.paper)
            .clickable(enabled = false) {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // JD7: Tray's bottom edge is strictly above LocalDockInset
                .padding(bottom = dockInset)
        ) {
            // Top bar (height 52.dp, no wavy divider)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { viewModel.openSettings() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("sketch_settings_button")
                ) {
                    KannachIcon(
                        icon = KannachIconType.SLIDERS,
                        tint = palette.inkSoft,
                        contentDescription = strings.openSettings,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = strings.sketchTitle,
                    fontSize = 18.sp,
                    fontFamily = LocalKannachHandFont.current,
                    color = palette.inkSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .testTag("sketch_title")
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.openExportSheet() },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("sketch_export_button")
                    ) {
                        KannachIcon(
                            icon = KannachIconType.UPLOAD,
                            tint = palette.greenDeep,
                            contentDescription = strings.exportTitle,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    // TR1: Clear in header overflow
                    Box {
                        IconButton(
                            onClick = { isHeaderOverflowOpen = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("sketch_overflow_button")
                        ) {
                            KannachIcon(
                                icon = KannachIconType.MORE,
                                tint = palette.inkSoft,
                                contentDescription = strings.settingsMore,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = isHeaderOverflowOpen,
                            onDismissRequest = { isHeaderOverflowOpen = false },
                            containerColor = palette.paperHi
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = {
                                    Text(
                                        text = strings.clear,
                                        fontSize = 15.sp,
                                        fontFamily = LocalKannachHandFont.current,
                                        color = if (strokes.isNotEmpty()) palette.ink else palette.inkSoft.copy(alpha = 0.45f)
                                    )
                                },
                                leadingIcon = {
                                    KannachIcon(
                                        icon = KannachIconType.TRASH,
                                        tint = if (strokes.isNotEmpty()) palette.inkSoft else palette.inkSoft.copy(alpha = 0.45f),
                                        contentDescription = strings.clear,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                enabled = strokes.isNotEmpty(),
                                onClick = {
                                    isHeaderOverflowOpen = false
                                    viewModel.clearSketchStrokes()
                                },
                                modifier = Modifier.testTag("sketch_tool_clear")
                            )
                        }
                    }
                }
            }

            // Full-bleed paper canvas between the top bar and the tray
            val canvasBaseBg = when (paperType) {
                SketchPaperType.KRAFT -> palette.paperKraft
                else -> palette.paperHi
            }
            val canvasPaperModifier = when (paperType) {
                SketchPaperType.PLAIN -> Modifier.paperBackground(canvasBaseBg)
                SketchPaperType.RULED -> Modifier
                    .paperBackground(canvasBaseBg)
                    .linedPaperBackground(palette.line, 24.dp)
                SketchPaperType.DOTTED -> Modifier
                    .paperBackground(canvasBaseBg)
                    .dottedPaperBackground(palette.line, 18.dp, 1.4.dp)
                SketchPaperType.KRAFT -> Modifier.paperBackground(canvasBaseBg, drawNoise = true)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = pageTurnCount,
                    transitionSpec = {
                        if (reduceMotion) {
                            fadeIn(tween(KannachMotion.INSTANT)) togetherWith fadeOut(tween(KannachMotion.INSTANT))
                        } else {
                            val dir = if (strings.dir == "rtl") 1 else -1
                            (slideInHorizontally(
                                animationSpec = tween(KannachMotion.PAGE, easing = KannachMotion.Settle)
                            ) { fullWidth -> -dir * fullWidth } + fadeIn(
                                animationSpec = tween(KannachMotion.PAGE, easing = KannachMotion.Settle)
                            )).togetherWith(
                                slideOutHorizontally(
                                    animationSpec = tween(KannachMotion.PAGE, easing = KannachMotion.Settle)
                                ) { fullWidth -> dir * fullWidth } + fadeOut(
                                    animationSpec = tween(KannachMotion.PAGE, easing = KannachMotion.Settle)
                                )
                            )
                        }
                    },
                    label = "SketchSheetPageTurn"
                ) { _ ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RectangleShape)
                            .then(canvasPaperModifier)
                            .testTag("sketch_canvas_box"),
                        contentAlignment = Alignment.Center
                    ) {
                        val effectiveColorHex = remember(sketchTool, selectedColor) {
                            viewModel.effectiveSketchColorHex(selectedColor, sketchTool)
                        }
                        val effectiveWidth = remember(sketchTool, brushWidth) {
                            viewModel.effectiveSketchWidth(brushWidth, sketchTool)
                        }
                        DrawingCanvas(
                            sketchStrokes = strokes,
                            currentColorHex = effectiveColorHex,
                            currentWidth = effectiveWidth,
                            isEraserMode = isEraserMode,
                            onStrokeCompleted = { viewModel.addSketchStroke(it) },
                            onEraseAt = { viewModel.eraseSketchAt(it) },
                            onStrokeStarted = {
                                showPaperPopover = false
                                showSizePopover = false
                                dockScroll.collapse()
                                viewModel.onSketchStrokeStarted()
                            },
                            onGestureEnded = { viewModel.onSketchGestureEnded() },
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("sketch_drawing_canvas")
                        )

                        // Whisper text centred inside the empty canvas (17.sp, hand font, palette.inkFaint)
                        androidx.compose.animation.AnimatedVisibility(
                            visible = activeTopWhisper != null,
                            enter = fadeIn(tween(if (reduceMotion) KannachMotion.INSTANT else KannachMotion.SOFT, easing = KannachMotion.Settle)),
                            exit = fadeOut(tween(if (reduceMotion) KannachMotion.INSTANT else KannachMotion.SOFT))
                        ) {
                            Text(
                                text = activeTopWhisper ?: "",
                                fontSize = 17.sp,
                                fontFamily = LocalKannachHandFont.current,
                                color = palette.inkFaint,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(horizontal = 24.dp)
                                    .testTag(if (currentSavedWhisperText != null) "sketch_saved_whisper" else "sketch_whisper_line")
                            )
                        }
                    }
                }

                // Popovers above the tray (Paper types & Brush sizes)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    AnimatedVisibility(
                        visible = showPaperPopover,
                        enter = fadeIn(tween(if (reduceMotion) KannachMotion.INSTANT else KannachMotion.SOFT)),
                        exit = fadeOut(tween(if (reduceMotion) KannachMotion.INSTANT else KannachMotion.SOFT))
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(palette.paperHi)
                                .border(1.2.dp, palette.line, RoundedCornerShape(14.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SketchPaperType.entries.forEach { type ->
                                val isSelected = paperType == type
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(HandDrawnRadiusAltShape)
                                        .then(
                                            if (isSelected) {
                                                Modifier
                                                    .background(palette.greenSoft)
                                                    .border(1.8.dp, palette.greenDeep, HandDrawnRadiusAltShape)
                                            } else {
                                                Modifier
                                                    .background(palette.paperHi.copy(alpha = 0.65f))
                                                    .border(1.dp, palette.line, HandDrawnRadiusAltShape)
                                            }
                                        )
                                        .clickable {
                                            viewModel.setSketchPaperType(type)
                                            showPaperPopover = false
                                        }
                                        .padding(4.dp)
                                        .testTag("sketch_paper_${type.name.lowercase(Locale.ROOT)}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    PaperTypeMiniSwatch(
                                        type = type,
                                        palette = palette,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = showSizePopover,
                        enter = fadeIn(tween(if (reduceMotion) KannachMotion.INSTANT else KannachMotion.SOFT)),
                        exit = fadeOut(tween(if (reduceMotion) KannachMotion.INSTANT else KannachMotion.SOFT))
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(palette.paperHi)
                                .border(1.2.dp, palette.line, RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            brushSizes.forEach { w ->
                                val isSelected = brushWidth == w
                                val dotDp = when (w) {
                                    4f -> 8.dp
                                    8f -> 14.dp
                                    else -> 22.dp
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            viewModel.setSketchBrushWidth(w)
                                            showSizePopover = false
                                        }
                                        .testTag("sketch_brush_${w.toInt()}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(dotDp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) palette.greenDeep else palette.inkSoft)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Ephemeral mood-name label and clear-undo slot directly above the tray
            val moodName = I18n.getMoodName(selectedColor, language)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 24.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = clearUndoVisible,
                    enter = fadeIn(tween(if (reduceMotion) KannachMotion.INSTANT else KannachMotion.SOFT)),
                    exit = fadeOut(tween(if (reduceMotion) KannachMotion.INSTANT else KannachMotion.SOFT))
                ) {
                    Row(
                        modifier = Modifier
                            .clip(HandDrawnRadiusAltShape)
                            .background(palette.paperHi)
                            .border(1.2.dp, palette.greenLine, HandDrawnRadiusAltShape)
                            .clickable { viewModel.undoSketchStroke() }
                            .padding(horizontal = 12.dp, vertical = 3.dp)
                            .testTag("sketch_clear_undo_action"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        KannachIcon(
                            icon = KannachIconType.UNDO,
                            tint = palette.greenDeep,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.undo,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = LocalKannachHandFont.current,
                            color = palette.greenDeep,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = !clearUndoVisible && moodLabelVisible && !isEraserMode,
                    enter = fadeIn(tween(if (reduceMotion) KannachMotion.INSTANT else KannachMotion.INSTANT)),
                    exit = fadeOut(tween(if (reduceMotion) KannachMotion.INSTANT else KannachMotion.SOFT))
                ) {
                    Text(
                        text = moodName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = LocalKannachHandFont.current,
                        color = palette.greenDeep,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sketch_mood_label")
                    )
                }
            }

            // Tray: background palette.paper, no border, no shadow, no card, hand-drawn inkLine on top edge
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.paper)
                    .drawBehind {
                        inkLine(
                            from = Offset(0f, 0f),
                            to = Offset(size.width, 0f),
                            seed = 73,
                            color = palette.line,
                            widthPx = 1.3.dp.toPx(),
                            wobblePx = 1.5.dp.toPx()
                        )
                    }
                    .padding(horizontal = 12.dp)
                    .testTag("sketch_tools_tray")
            ) {
                // Row 1 (height 52.dp): colour swatches (30.dp dots, inkCircle scribble ring when selected) + PAPER button (32.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        allSwatches.forEachIndexed { idx, hex ->
                            val isSelected = !isEraserMode && selectedColor.equals(hex, ignoreCase = true)
                            val swatchColor = try {
                                Color(android.graphics.Color.parseColor(hex))
                            } catch (_: Exception) {
                                palette.ink
                            }

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clickable {
                                        showPaperPopover = false
                                        showSizePopover = false
                                        viewModel.setSketchColor(hex)
                                    }
                                    .drawBehind {
                                        if (isSelected) {
                                            inkCircle(
                                                center = Offset(size.width / 2f, size.height / 2f),
                                                radiusPx = 18.dp.toPx(),
                                                seed = idx + 11,
                                                color = palette.ink,
                                                widthPx = 2.dp.toPx()
                                            )
                                        }
                                    }
                                    .testTag("sketch_swatch_$idx"),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable {
                                showSizePopover = false
                                showPaperPopover = !showPaperPopover
                            }
                            .testTag("sketch_paper_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        KannachIcon(
                            icon = KannachIconType.PAPER,
                            tint = palette.ink,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Row 2 (height 76.dp): UNDO/REDO at start; PEN, MARKER, PENCIL, ERASER at end
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(76.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .alpha(if (canUndo) 1f else 0.3f)
                                .clickable(enabled = canUndo) { viewModel.undoSketchStroke() }
                                .testTag("sketch_tool_undo"),
                            contentAlignment = Alignment.Center
                        ) {
                            KannachIcon(
                                icon = KannachIconType.UNDO,
                                tint = palette.ink,
                                contentDescription = strings.undo,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .alpha(if (canRedo) 1f else 0.3f)
                                .clickable(enabled = canRedo) { viewModel.redoSketchStroke() }
                                .testTag("sketch_tool_redo"),
                            contentAlignment = Alignment.Center
                        ) {
                            KannachIcon(
                                icon = KannachIconType.REDO,
                                tint = palette.ink,
                                contentDescription = strings.undo,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val toolItems = listOf(
                            Triple(SketchTool.PEN, KannachIconType.PEN, "sketch_tool_pen"),
                            Triple(SketchTool.MARKER, KannachIconType.MARKER, "sketch_tool_marker"),
                            Triple(SketchTool.PENCIL, KannachIconType.PENCIL, "sketch_tool_pencil")
                        )
                        toolItems.forEach { (tool, iconType, tag) ->
                            val isSelected = !isEraserMode && sketchTool == tool
                            val liftDp by animateDpAsState(
                                targetValue = if (isSelected) 10.dp else 0.dp,
                                animationSpec = tween(if (reduceMotion) KannachMotion.INSTANT else KannachMotion.SOFT),
                                label = "toolLift_$tag"
                            )
                            Box(
                                modifier = Modifier
                                    .size(width = 44.dp, height = 56.dp)
                                    .offset(y = -liftDp)
                                    .alpha(if (isSelected) 1f else 0.8f)
                                    .clickable {
                                        showPaperPopover = false
                                        if (isSelected) {
                                            showSizePopover = !showSizePopover
                                        } else {
                                            showSizePopover = false
                                            viewModel.setSketchTool(tool)
                                        }
                                    }
                                    .testTag(tag),
                                contentAlignment = Alignment.Center
                            ) {
                                KannachIcon(
                                    icon = iconType,
                                    tint = palette.ink,
                                    fillColor = currentInkColor,
                                    modifier = Modifier.size(width = 44.dp, height = 56.dp)
                                )
                            }
                        }

                        val eraserLiftDp by animateDpAsState(
                            targetValue = if (isEraserMode) 10.dp else 0.dp,
                            animationSpec = tween(if (reduceMotion) KannachMotion.INSTANT else KannachMotion.SOFT),
                            label = "toolLift_eraser"
                        )
                        Box(
                            modifier = Modifier
                                .size(width = 44.dp, height = 56.dp)
                                .offset(y = -eraserLiftDp)
                                .alpha(if (isEraserMode) 1f else 0.8f)
                                .clickable {
                                    showPaperPopover = false
                                    if (isEraserMode) {
                                        showSizePopover = !showSizePopover
                                    } else {
                                        showSizePopover = false
                                        viewModel.setSketchEraserMode(true)
                                    }
                                }
                                .testTag("sketch_tool_eraser"),
                            contentAlignment = Alignment.Center
                        ) {
                            KannachIcon(
                                icon = KannachIconType.ERASER,
                                tint = palette.ink,
                                contentDescription = strings.clear,
                                modifier = Modifier.size(width = 44.dp, height = 56.dp)
                            )
                        }
                    }
                }
            }
        }

        // SK7: Export paper bottom sheet
        AnimatedVisibility(
            visible = isExportSheetOpen,
            enter = fadeIn(tween(if (reduceMotion) 0 else KannachMotion.SOFT)) +
                slideInVertically(tween(if (reduceMotion) 0 else KannachMotion.SOFT)) { it / 2 },
            exit = fadeOut(tween(if (reduceMotion) 0 else KannachMotion.SOFT)) +
                slideOutVertically(tween(if (reduceMotion) 0 else KannachMotion.SOFT)) { it / 2 }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.scrim)
                    .clickable { viewModel.closeExportSheet() }
                    .testTag("sketch_export_sheet"),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                        .clickable(enabled = false) {}
                        .clip(HandDrawnRadiusShape)
                        .paperBackground(palette.paperHi)
                        .border(2.dp, palette.ink, HandDrawnRadiusShape)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header row: Export Title + Close
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.exportTitle,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = LocalKannachHandFont.current,
                                color = palette.greenDeep
                            )
                            IconButton(
                                onClick = { viewModel.closeExportSheet() },
                                modifier = Modifier.size(48.dp)
                            ) {
                                KannachIcon(
                                    icon = KannachIconType.X,
                                    tint = palette.inkSoft,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Format chips: Story 9:16, Square 1:1, Page 3:4
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val formatOptions = listOf(
                                ExportFormat.STORY to strings.exportStory,
                                ExportFormat.SQUARE to strings.exportSquare,
                                ExportFormat.PAGE to strings.exportPage
                            )
                            formatOptions.forEach { (fmt, label) ->
                                val active = exportFormat == fmt
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .clip(HandDrawnRadiusAltShape)
                                        .background(if (active) palette.greenSoft else palette.paper)
                                        .border(
                                            width = if (active) 1.8.dp else 1.2.dp,
                                            color = if (active) palette.greenDeep else palette.line,
                                            shape = HandDrawnRadiusAltShape
                                        )
                                        .clickable { viewModel.setExportFormat(fmt) }
                                        .padding(horizontal = 8.dp, vertical = 10.dp)
                                        .testTag("export_format_${fmt.name.lowercase(Locale.ROOT)}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                        fontFamily = LocalKannachUiFont.current,
                                        color = if (active) palette.greenDeep else palette.ink,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Signature switch (default ON)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(HandDrawnRadiusAltShape)
                                .background(palette.paper)
                                .border(1.2.dp, palette.line, HandDrawnRadiusAltShape)
                                .clickable { viewModel.setExportSignature(!exportSignature) }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("export_signature_row"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                KannachIcon(
                                    icon = KannachIconType.SPROUT,
                                    tint = palette.greenDeep,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = strings.exportSignature,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = LocalKannachUiFont.current,
                                    color = palette.ink
                                )
                            }
                            Switch(
                                checked = exportSignature,
                                onCheckedChange = { viewModel.setExportSignature(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = palette.onGreen,
                                    checkedTrackColor = palette.green
                                ),
                                modifier = Modifier.testTag("export_signature_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action buttons: Save to Photos & Share
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clip(HandDrawnRadiusShape)
                                    .background(palette.paper)
                                    .border(1.5.dp, palette.greenDeep, HandDrawnRadiusShape)
                                    .clickable { viewModel.saveSketchToPhotos(context, palette) }
                                    .padding(vertical = 12.dp, horizontal = 10.dp)
                                    .testTag("export_save_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    KannachIcon(
                                        icon = KannachIconType.CHECK,
                                        tint = palette.greenDeep,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = strings.exportSave,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = LocalKannachUiFont.current,
                                        color = palette.greenDeep,
                                        maxLines = 1
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clip(HandDrawnRadiusShape)
                                    .background(palette.green)
                                    .border(1.5.dp, palette.greenDeep, HandDrawnRadiusShape)
                                    .clickable { viewModel.shareSketch(context, palette) }
                                    .padding(vertical = 12.dp, horizontal = 10.dp)
                                    .testTag("export_share_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    KannachIcon(
                                        icon = KannachIconType.UPLOAD,
                                        tint = palette.onGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = strings.exportShare,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = LocalKannachUiFont.current,
                                        color = palette.onGreen,
                                        maxLines = 1
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

/**
 * SK4: Procedural mini swatch representing one of the 4 paper types (plain, ruled, dotted, kraft).
 */
@Composable
private fun PaperTypeMiniSwatch(
    type: SketchPaperType,
    palette: KannachPalette,
    modifier: Modifier = Modifier
) {
    val bg = if (type == SketchPaperType.KRAFT) palette.paperKraft else palette.paperHi
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, palette.inkSoft.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
    ) {
        when (type) {
            SketchPaperType.PLAIN -> {
                // Warm blank sheet
            }
            SketchPaperType.RULED -> {
                val step = size.height / 4f
                for (i in 1..3) {
                    val y = i * step
                    drawLine(
                        color = palette.line,
                        start = androidx.compose.ui.geometry.Offset(3f, y),
                        end = androidx.compose.ui.geometry.Offset(size.width - 3f, y),
                        strokeWidth = 1.5f
                    )
                }
            }
            SketchPaperType.DOTTED -> {
                val cols = 3
                val rows = 3
                for (c in 1..cols) {
                    for (r in 1..rows) {
                        drawCircle(
                            color = palette.line,
                            radius = 1.8f,
                            center = androidx.compose.ui.geometry.Offset(
                                x = c * (size.width / 4f),
                                y = r * (size.height / 4f)
                            )
                        )
                    }
                }
            }
            SketchPaperType.KRAFT -> {
                // Subtle kraft grain dots
                val pts = listOf(0.25f to 0.3f, 0.7f to 0.45f, 0.4f to 0.75f, 0.75f to 0.2f)
                pts.forEach { (fx, fy) ->
                    drawCircle(
                        color = palette.ink.copy(alpha = 0.18f),
                        radius = 1.4f,
                        center = androidx.compose.ui.geometry.Offset(size.width * fx, size.height * fy)
                    )
                }
            }
        }
    }
}

/**
 * v5.1 (§4): Settings: fewer choices, nothing to get lost in.
 * One scroll, no chapter cards, at most 7 visible rows on first open + centered Made By footer:
 * 1. Language (row -> bottom sheet with 5 radio choices + Arabic-Indic numerals switch when Arabic is active)
 * 2. Daily reminder (switch; when on, time inline on the same row; one-line permission notice if missing)
 * 3. Season colors (one row, 5 choices: Auto, Spring, Summer, Autumn, Winter as small swatches inline)
 * 4. Back up my diary (row -> bottom sheet with Export, Import, Last backup: {date}, and Monthly copy reminder switch)
 * 5. Hide from recent apps (switch)
 * 6. More (expandable row, collapsed by default, state not persisted: Week starts on, Handwriting size,
 *    Add signature to exported drawings, Reminder days, Send a test reminder, Replay welcome tour)
 * 7. Tell a friend (text button)
 * — Footer: centered, 13 sp, palette.inkFaint: strings.madeBy
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val palette = LocalKannachPalette.current
    val language by viewModel.language.collectAsState()
    val languageChoice by viewModel.languageChoice.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val exportSignature by viewModel.exportSignature.collectAsState()
    val reminder by viewModel.reminder.collectAsState()
    val reduceMotion = rememberReduceMotion()
    val context = LocalContext.current

    // Sheet & expansion states (not persisted)
    var showLanguageSheet by remember { mutableStateOf(false) }
    var showBackupSheet by remember { mutableStateOf(false) }
    var showAboutSheet by remember { mutableStateOf(false) }
    var isMoreExpanded by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler(enabled = showLanguageSheet || showBackupSheet || showAboutSheet) {
        when {
            showLanguageSheet -> showLanguageSheet = false
            showBackupSheet -> showBackupSheet = false
            showAboutSheet -> showAboutSheet = false
        }
    }

    fun checkNotifGranted(): Boolean {
        val systemEnabled = try {
            androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
        } catch (_: Exception) {
            true
        }
        val runtimeGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return systemEnabled && runtimeGranted
    }

    var isNotifPermissionGranted by remember(language, reminder.enabled) {
        mutableStateOf(checkNotifGranted())
    }

    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isNotifPermissionGranted = granted && checkNotifGranted()
    }

    val openNotificationSettings = {
        try {
            val settingsIntent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {}
        }
    }

    // JSON file picker launcher for restoring backups
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val text = BufferedReader(InputStreamReader(stream)).readText()
                    viewModel.importBackup(text)
                }
            } catch (e: Exception) {
                viewModel.showToast(I18n.forLanguage(language).importError)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .paperBackground(palette.paper)
            .testTag("settings_screen_root")
    ) {
        AnimatedContent(
            targetState = language,
            transitionSpec = {
                val dur = if (reduceMotion) KannachMotion.INSTANT else KannachMotion.SOFT
                fadeIn(tween(dur)) togetherWith fadeOut(tween(dur))
            },
            label = "SettingsLanguageCrossfade"
        ) { activeLang ->
            val strings = I18n.forLanguage(activeLang)
            val isRtl = I18n.isRtl(activeLang)

            val currentLanguageDisplay = remember(languageChoice, activeLang, strings) {
                if (languageChoice == "system") {
                    strings.langFollowSystem
                } else {
                    I18n.LANGUAGE_LABELS[languageChoice] ?: I18n.LANGUAGE_LABELS[activeLang] ?: activeLang
                }
            }

            val formattedTime = remember(reminder.time, activeLang, userSettings.useArabicIndicNumerals) {
                val parts = reminder.time.split(":")
                if (parts.size == 2) {
                    val h = parts[0].toIntOrNull() ?: 20
                    val m = parts[1].toIntOrNull() ?: 0
                    val raw = String.format(Locale.US, "%02d:%02d", h, m)
                    if (I18n.normalizeCode(activeLang) == "ar" && userSettings.useArabicIndicNumerals) {
                        I18n.toArabicIndicDigits(raw)
                    } else {
                        raw
                    }
                } else {
                    reminder.time
                }
            }

            val lastSavedDateStr = remember(
                userSettings.lastBackupTimestamp,
                activeLang,
                userSettings.useArabicIndicNumerals
            ) {
                val ts = userSettings.lastBackupTimestamp
                if (ts == null || ts <= 0L) {
                    strings.settingsBackupNever
                } else {
                    I18n.formatEpochLongDate(ts, activeLang, userSettings.useArabicIndicNumerals)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(bottom = 36.dp)
                    .testTag("settings_scroll_container")
            ) {
                // Header: Back chevron row, then centred HUT doodle + Settings title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(
                        onClick = { viewModel.closeSettings() },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("settings_back_btn")
                    ) {
                        KannachIcon(
                            icon = if (isRtl) KannachIconType.CHEV_R else KannachIconType.CHEV_L,
                            tint = palette.ink,
                            contentDescription = strings.dismiss,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    KannachIcon(
                        icon = KannachIconType.PLANT,
                        tint = palette.ink,
                        modifier = Modifier.size(96.dp)
                    )
                    Text(
                        text = strings.settings,
                        fontSize = 22.sp,
                        fontFamily = LocalKannachHandFont.current,
                        color = palette.inkSoft,
                        modifier = Modifier.testTag("settings_header_title")
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Single clean scroll of rows (no chapter cards, no dividers between rows)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // =========================================================================
                    // ROW 1: Language (tap -> bottom sheet with 5 radio choices + Arabic numerals switch)
                    // =========================================================================
                    SettingsDoodleRow(
                        title = strings.language,
                        description = strings.settingsDescLanguage,
                        tile = {
                            SettingsTile(selected = null) {
                                KannachIcon(
                                    icon = KannachIconType.TALK,
                                    tint = palette.ink,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        },
                        onClick = { showLanguageSheet = true },
                        modifier = Modifier.testTag("settings_row_language")
                    )

                    // =========================================================================
                    // ROW 2: Daily reminder (toggle tile; when on, time and permission inline under description)
                    // =========================================================================
                    val onToggleReminder: (Boolean) -> Unit = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!granted && !viewModel.repository.isNotifPermissionAsked()) {
                                viewModel.repository.markNotifPermissionAsked()
                                try {
                                    notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } catch (_: Exception) {}
                            }
                        }
                        viewModel.toggleReminder(enabled)
                    }

                    SettingsDoodleRow(
                        title = strings.reminderTitle,
                        description = strings.settingsDescReminder,
                        tile = {
                            SettingsTile(
                                selected = reminder.enabled,
                                modifier = Modifier.testTag("settings_reminder_switch")
                            ) {
                                KannachIcon(
                                    icon = KannachIconType.BELL,
                                    tint = if (reminder.enabled) palette.paperHi else palette.ink,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        },
                        modifier = Modifier
                            .testTag("settings_row_reminder")
                            .toggleable(
                                value = reminder.enabled,
                                role = Role.Switch,
                                onValueChange = onToggleReminder
                            ),
                        extraContent = if (reminder.enabled || !isNotifPermissionGranted) {
                            {
                                if (reminder.enabled) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .heightIn(min = 38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(palette.paperHi)
                                            .border(1.2.dp, palette.greenLine, RoundedCornerShape(10.dp))
                                            .clickable {
                                                val parts = reminder.time.split(":").mapNotNull { it.toIntOrNull() }
                                                val h = if (parts.isNotEmpty()) parts[0] else 20
                                                val m = if (parts.size > 1) parts[1] else 0
                                                try {
                                                    TimePickerDialog(
                                                        context,
                                                        { _, sH, sM ->
                                                            viewModel.setReminderTime(
                                                                String.format(Locale.US, "%02d:%02d", sH, sM)
                                                            )
                                                        },
                                                        h,
                                                        m,
                                                        true
                                                    ).show()
                                                } catch (_: Exception) {}
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                            .testTag("settings_reminder_time_btn"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = formattedTime,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = palette.greenDeep,
                                            maxLines = 1
                                        )
                                    }
                                }

                                if (!isNotifPermissionGranted) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp, bottom = 4.dp)
                                            .testTag("settings_reminder_perm_row"),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = strings.reminderPermOff,
                                            fontSize = 13.sp,
                                            color = palette.inkSoft,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("settings_reminder_perm_status")
                                        )
                                        Box(
                                            modifier = Modifier
                                                .heightIn(min = 36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(palette.greenSoft)
                                                .border(1.dp, palette.greenLine, RoundedCornerShape(8.dp))
                                                .clickable { openNotificationSettings() }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                                .testTag("settings_reminder_perm_btn"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = strings.reminderPermSettings,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = palette.greenDeep,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        } else null
                    )

                    // =========================================================================
                    // ROW 4: Back up my diary (row -> sheet with Export, Import, Last backup, Monthly switch)
                    // =========================================================================
                    SettingsDoodleRow(
                        title = strings.settingsBackupRow,
                        description = strings.settingsDescBackup,
                        tile = {
                            SettingsTile(selected = null) {
                                KannachIcon(
                                    icon = KannachIconType.UPLOAD,
                                    tint = palette.ink,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        },
                        onClick = { showBackupSheet = true },
                        modifier = Modifier.testTag("settings_row_backup")
                    )

                    // =========================================================================
                    // ROW 5: Hide from recent apps (toggle tile)
                    // =========================================================================
                    SettingsDoodleRow(
                        title = strings.settingsHideRecents,
                        description = strings.settingsDescHide,
                        tile = {
                            SettingsTile(
                                selected = userSettings.hideInRecents,
                                modifier = Modifier.testTag("settings_hide_recents_switch")
                            ) {
                                KannachIcon(
                                    icon = KannachIconType.EYE_OFF,
                                    tint = if (userSettings.hideInRecents) palette.paperHi else palette.ink,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        },
                        modifier = Modifier
                            .testTag("settings_row_hide_recents")
                            .toggleable(
                                value = userSettings.hideInRecents,
                                role = Role.Switch,
                                onValueChange = { viewModel.setHideInRecents(it) }
                            )
                    )

                    // =========================================================================
                    // ROW 6: More (expandable row, collapsed by default, state not persisted)
                    // =========================================================================
                    SettingsDoodleRow(
                        title = strings.settingsMore,
                        description = strings.settingsDescMore,
                        tile = {
                            SettingsTile(selected = null) {
                                KannachIcon(
                                    icon = KannachIconType.MORE,
                                    tint = palette.ink,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        },
                        onClick = { isMoreExpanded = !isMoreExpanded },
                        modifier = Modifier.testTag("settings_row_more")
                    )

                    // 4.2 Inside "More" (collapsed by default)
                    AnimatedVisibility(visible = isMoreExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 10.dp)
                                .clip(HandDrawnRadiusShape)
                                .background(palette.paperHi)
                                .border(1.dp, palette.line, HandDrawnRadiusShape)
                                .padding(14.dp)
                                .testTag("settings_more_section"),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 1. Week starts on (chip row)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = strings.settingsWeekStartLabel,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = palette.inkSoft,
                                    maxLines = 1
                                )
                                val activeWeekStart = userSettings.effectiveWeekStart(activeLang)
                                val weekStartOptions = listOf(
                                    7 to strings.weekStartSat,
                                    1 to strings.weekStartSun,
                                    2 to strings.weekStartMon
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    weekStartOptions.forEach { (dow, label) ->
                                        SettingsOptionChip(
                                            label = label,
                                            selected = activeWeekStart == dow,
                                            onClick = { viewModel.setWeekStartsOn(dow) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("settings_week_start_$dow")
                                        )
                                    }
                                }
                            }

                            // 2. Handwriting size (chip row)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = strings.settingsHandSizeLabel,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = palette.inkSoft,
                                    maxLines = 1
                                )
                                val handSizeOptions = listOf(
                                    "small" to strings.handSizeSmall,
                                    "normal" to strings.handSizeNormal,
                                    "large" to strings.handSizeLarge
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    handSizeOptions.forEach { (key, label) ->
                                        SettingsOptionChip(
                                            label = label,
                                            selected = userSettings.handwritingSize == key,
                                            onClick = { viewModel.setHandwritingSize(key) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("settings_hand_size_$key")
                                        )
                                    }
                                }
                            }

                            // 3. Add signature to exported drawings (switch)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.settingsSignatureExport,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = palette.ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = exportSignature,
                                    onCheckedChange = { viewModel.setExportSignature(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = palette.onGreen,
                                        checkedTrackColor = palette.green
                                    ),
                                    modifier = Modifier.testTag("settings_signature_switch")
                                )
                            }

                            // 4. Reminder days
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = strings.reminderDaysLabel,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = palette.inkSoft,
                                    maxLines = 1
                                )
                                val weekStartDow = userSettings.effectiveWeekStart(activeLang)
                                val fullWeekdays = remember(activeLang) { I18n.getFullWeekdays(activeLang) }
                                val orderedDays = remember(weekStartDow) {
                                    val shift = (weekStartDow - Calendar.SUNDAY + 7) % 7
                                    (0 until 7).map { i -> ((shift + i) % 7) + 1 }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    orderedDays.forEach { dow ->
                                        val isDaySelected = userSettings.reminderDays.contains(dow)
                                        val dayLabel = strings.weekdays.getOrElse(dow - 1) { "" }
                                        val fullDayLabel = fullWeekdays.getOrElse(dow - 1) { dayLabel }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 40.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isDaySelected) palette.greenSoft else palette.paper)
                                                .border(
                                                    width = if (isDaySelected) 1.5.dp else 1.dp,
                                                    color = if (isDaySelected) palette.greenDeep else palette.line,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .clickable { viewModel.toggleReminderDay(dow) }
                                                .padding(vertical = 6.dp, horizontal = 2.dp)
                                                .testTag("settings_reminder_day_$dow"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = dayLabel,
                                                fontSize = 11.sp,
                                                fontWeight = if (isDaySelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isDaySelected) palette.greenDeep else palette.inkSoft,
                                                maxLines = 1,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }

                            // 5. Send a test reminder
                            SettingsPaperActionRow(
                                label = strings.reminderTestBtn,
                                icon = KannachIconType.SPARKLE,
                                onClick = { viewModel.sendTestNudge(context) },
                                modifier = Modifier.testTag("settings_test_nudge_btn")
                            )

                            // 6. Replay welcome tour
                            SettingsPaperActionRow(
                                label = strings.settingsReplayWelcome,
                                icon = KannachIconType.SPROUT,
                                onClick = { viewModel.replayWelcome() },
                                modifier = Modifier.testTag("settings_replay_welcome_btn")
                            )
                        }
                    }

                    // =========================================================================
                    // ROW 7: Tell a friend
                    // =========================================================================
                    SettingsDoodleRow(
                        title = strings.settingsTellFriend,
                        description = strings.settingsDescShare,
                        tile = {
                            SettingsTile(selected = null) {
                                KannachIcon(
                                    icon = KannachIconType.BOUQUET,
                                    tint = palette.ink,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        },
                        onClick = { viewModel.shareAppWithFriend(context) },
                        modifier = Modifier.testTag("settings_tell_friend_btn")
                    )

                    // =========================================================================
                    // ROW 8: About Kannach & privacy
                    // =========================================================================
                    SettingsDoodleRow(
                        title = strings.settingsAboutRow,
                        description = strings.settingsDescAbout,
                        tile = {
                            SettingsTile(selected = null) {
                                KannachIcon(
                                    icon = KannachIconType.ABOUT,
                                    tint = palette.ink,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        },
                        onClick = { showAboutSheet = true },
                        modifier = Modifier.testTag("settings_row_about")
                    )

                    // =========================================================================
                    // FOOTER: centered, 13 sp, palette.inkFaint: "Made By" line
                    // =========================================================================
                    Text(
                        text = strings.madeBy,
                        fontSize = 13.sp,
                        color = palette.inkFaint,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp, bottom = 8.dp)
                            .testTag("settings_version_text")
                    )
                }
            }

            // =========================================================================
            // SHEET 1: Language bottom sheet (5 radio choices + Arabic-Indic numerals switch)
            // =========================================================================
            if (showLanguageSheet) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.scrim)
                        .clickable { showLanguageSheet = false }
                        .testTag("settings_language_sheet_scrim"),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 560.dp)
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .background(palette.paperHi)
                            .border(
                                width = 1.5.dp,
                                color = palette.line,
                                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                            )
                            .clickable(enabled = false) {}
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 20.dp)
                            .testTag("settings_language_sheet"),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.language,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = LocalKannachHandFont.current,
                                color = palette.greenDeep
                            )
                            IconButton(
                                onClick = { showLanguageSheet = false },
                                modifier = Modifier.size(40.dp)
                            ) {
                                KannachIcon(
                                    icon = KannachIconType.X,
                                    tint = palette.inkSoft,
                                    contentDescription = strings.dismiss,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        val langRows = listOf(
                            "system" to strings.langFollowSystem,
                            "ar" to "العربية",
                            "en" to "English",
                            "fr" to "Français",
                            "es" to "Español"
                        )
                        langRows.forEach { (code, label) ->
                            val isSelected = if (code == "system") {
                                languageChoice == "system"
                            } else {
                                languageChoice == code
                            }
                            SettingsLanguageRow(
                                code = code,
                                label = label,
                                selected = isSelected,
                                onClick = { viewModel.setLanguage(code) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_lang_row_$code")
                            )
                        }

                        // When Arabic is active the sheet also shows one switch: "Arabic-Indic numerals"
                        if (I18n.normalizeCode(activeLang) == "ar") {
                            SettingsThinDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .padding(top = 4.dp)
                                    .testTag("settings_numerals_section"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${strings.settingsNumeralsLabel} (${strings.numeralsArabicIndic})",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = palette.ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = userSettings.useArabicIndicNumerals,
                                    onCheckedChange = { viewModel.setUseArabicIndicNumerals(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = palette.onGreen,
                                        checkedTrackColor = palette.green
                                    ),
                                    modifier = Modifier.testTag("settings_numerals_switch")
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // SHEET 2: Back up my diary bottom sheet (Export, Import, Last backup, Monthly switch)
            // =========================================================================
            if (showBackupSheet) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.scrim)
                        .clickable { showBackupSheet = false }
                        .testTag("settings_backup_sheet_scrim"),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 560.dp)
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .background(palette.paperHi)
                            .border(
                                width = 1.5.dp,
                                color = palette.line,
                                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                            )
                            .clickable(enabled = false) {}
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 20.dp)
                            .testTag("settings_backup_sheet"),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.settingsBackupRow,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = LocalKannachHandFont.current,
                                color = palette.greenDeep
                            )
                            IconButton(
                                onClick = { showBackupSheet = false },
                                modifier = Modifier.size(40.dp)
                            ) {
                                KannachIcon(
                                    icon = KannachIconType.X,
                                    tint = palette.inkSoft,
                                    contentDescription = strings.dismiss,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Export
                        SettingsPaperActionRow(
                            label = strings.exportBtn,
                            icon = KannachIconType.UPLOAD,
                            onClick = { viewModel.exportBackup(context) },
                            modifier = Modifier.testTag("settings_export_btn")
                        )

                        // Import
                        SettingsPaperActionRow(
                            label = strings.importBtn,
                            icon = KannachIconType.NOTEBOOK,
                            onClick = { importLauncher.launch("application/json") },
                            modifier = Modifier.testTag("settings_import_btn")
                        )

                        // Last backup: {date}
                        Text(
                            text = strings.settingsLastBackup.replace("{d}", lastSavedDateStr),
                            fontSize = 13.sp,
                            fontFamily = LocalKannachHandFont.current,
                            color = palette.inkSoft,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("settings_last_backup_text")
                        )

                        SettingsThinDivider()

                        // Monthly copy reminder switch (default off)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${strings.settingsCopyReminderLabel} (${strings.copyReminderMonthly})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = palette.ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = userSettings.copyReminder == "monthly",
                                onCheckedChange = { enabled ->
                                    viewModel.setCopyReminder(if (enabled) "monthly" else "off")
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = palette.onGreen,
                                    checkedTrackColor = palette.green
                                ),
                                modifier = Modifier.testTag("settings_copy_reminder_switch")
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // SHEET 3: About Kannach & privacy bottom sheet
            // =========================================================================
            if (showAboutSheet) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.scrim)
                        .clickable { showAboutSheet = false }
                        .testTag("settings_about_sheet_scrim"),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 560.dp)
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .background(palette.paperHi)
                            .clickable(enabled = false) {}
                            .navigationBarsPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 20.dp)
                            .testTag("settings_about_sheet")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { showAboutSheet = false },
                                modifier = Modifier.size(40.dp)
                            ) {
                                KannachIcon(
                                    icon = KannachIconType.X,
                                    tint = palette.inkSoft,
                                    contentDescription = strings.dismiss,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            KannachIcon(
                                icon = KannachIconType.ABOUT,
                                tint = palette.ink,
                                modifier = Modifier.size(64.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = strings.aboutTitle,
                            fontSize = 22.sp,
                            fontFamily = LocalKannachHandFont.current,
                            color = palette.greenDeep,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = strings.aboutWhatTitle,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = LocalKannachHandFont.current,
                            color = palette.ink
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = strings.aboutWhatBody,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            fontFamily = LocalKannachHandFont.current,
                            color = palette.ink
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = strings.aboutPrivacyTitle,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = LocalKannachHandFont.current,
                            color = palette.ink
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            strings.aboutPrivacyBody.split("\n").filter { it.isNotBlank() }.forEach { line ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 9.dp)
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(palette.green)
                                    )
                                    Text(
                                        text = line,
                                        fontSize = 16.sp,
                                        lineHeight = 24.sp,
                                        fontFamily = LocalKannachHandFont.current,
                                        color = palette.ink,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsThinDivider() {
    val palette = LocalKannachPalette.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(palette.line.copy(alpha = 0.65f))
    )
}

@Composable
private fun SettingsDoodleRow(
    title: String,
    description: String,
    tile: @Composable () -> Unit,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    extraContent: (@Composable () -> Unit)? = null
) {
    val palette = LocalKannachPalette.current
    val handFont = LocalKannachHandFont.current
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .then(clickModifier)
            .padding(horizontal = 4.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 19.sp,
                fontFamily = handFont,
                color = palette.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = description,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                fontFamily = handFont,
                color = palette.inkSoft,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (extraContent != null) {
                extraContent()
            }
        }
        tile()
    }
}

@Composable
private fun SettingsTile(
    selected: Boolean? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val palette = LocalKannachPalette.current
    val isSelected = selected == true
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .size(width = 64.dp, height = 52.dp)
            .clip(shape)
            .background(if (isSelected) palette.green else palette.paperLo)
            .then(
                if (isSelected) Modifier else Modifier.border(1.2.dp, palette.line, shape)
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun SettingsSeasonSwatch(
    key: String,
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalKannachPalette.current
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(
                if (key == "auto") palette.paperHi else color
            )
            .border(
                width = if (selected) 2.4.dp else 1.2.dp,
                color = if (selected) palette.ink else palette.line,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (key == "auto") {
            Canvas(modifier = Modifier.size(22.dp)) {
                drawArc(
                    color = Color(0xFF5AA25F),
                    startAngle = 180f,
                    sweepAngle = 90f,
                    useCenter = true
                )
                drawArc(
                    color = Color(0xFF6A9C3F),
                    startAngle = 270f,
                    sweepAngle = 90f,
                    useCenter = true
                )
                drawArc(
                    color = Color(0xFFC87D46),
                    startAngle = 0f,
                    sweepAngle = 90f,
                    useCenter = true
                )
                drawArc(
                    color = Color(0xFF3F7566),
                    startAngle = 90f,
                    sweepAngle = 90f,
                    useCenter = true
                )
            }
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (key == "auto") palette.ink else palette.onGreen)
            )
        }
    }
}

@Composable
private fun SettingsLanguageRow(
    code: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalKannachPalette.current
    val rowFont = when (code) {
        "ar" -> KannachFonts.handAr
        "en", "fr", "es" -> KannachFonts.handLatin
        else -> LocalKannachUiFont.current
    }

    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(HandDrawnRadiusShape)
            .background(if (selected) palette.greenSoft else palette.paper)
            .border(
                width = if (selected) 1.6.dp else 1.dp,
                color = if (selected) palette.greenDeep else palette.line,
                shape = HandDrawnRadiusShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontFamily = rowFont,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) palette.greenDeep else palette.ink
        )

        // Hand-drawn radio mark
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(
                    width = 1.6.dp,
                    color = if (selected) palette.greenDeep else palette.inkFaint,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(palette.greenDeep)
                )
            }
        }
    }
}

@Composable
private fun SettingsOptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalKannachPalette.current
    Box(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(HandDrawnRadiusShape)
            .background(if (selected) palette.greenSoft else palette.paper)
            .border(
                width = if (selected) 1.6.dp else 1.dp,
                color = if (selected) palette.greenDeep else palette.line,
                shape = HandDrawnRadiusShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) palette.greenDeep else palette.ink,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun SettingsPaperActionRow(
    label: String,
    icon: KannachIconType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalKannachPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(HandDrawnRadiusShape)
            .background(palette.paper)
            .border(1.3.dp, palette.ink.copy(alpha = 0.65f), HandDrawnRadiusShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        KannachIcon(
            icon = icon,
            tint = palette.greenDeep,
            strokeWidthDp = 1.8.dp,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = palette.ink
        )
    }
}

/**
 * 6. ConfirmDialogOverlay:
 * Hand-drawn paper confirmation modal for discard / delete confirmations.
 */
@Composable
fun ConfirmDialogOverlay(
    viewModel: MainViewModel
) {
    val dialogData by viewModel.confirmDialog.collectAsState()
    if (dialogData == null) return

    val data = dialogData!!
    val palette = LocalKannachPalette.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.scrim)
            .clickable { data.onCancel() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .widthIn(max = 340.dp)
                .clickable(enabled = false) {}
                .shadow(16.dp, HandDrawnRadiusShape)
                .clip(HandDrawnRadiusShape)
                .background(palette.paperHi)
                .border(2.dp, palette.ink, HandDrawnRadiusShape)
                .padding(22.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = data.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.greenDeep,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = data.message,
                    fontSize = 15.sp,
                    color = palette.inkSoft,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(HandDrawnRadiusShape)
                            .background(palette.paper)
                            .border(1.5.dp, palette.ink, HandDrawnRadiusShape)
                            .clickable { data.onCancel() }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag("confirm_dialog_cancel"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = data.cancelText,
                            color = palette.ink,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    val confirmBg = if (data.isDestructive) palette.red else palette.green
                    val confirmBorder = if (data.isDestructive) palette.dangerBorder else palette.greenDeep

                    Box(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(HandDrawnRadiusShape)
                            .background(confirmBg)
                            .border(1.5.dp, confirmBorder, HandDrawnRadiusShape)
                            .clickable { data.onConfirm() }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag("confirm_dialog_confirm"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = data.confirmText,
                            color = palette.onGreen,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * 7. ToastOverlay:
 * Floating pill toast at bottom center.
 */
@Composable
fun ToastOverlay(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val toastMessage by viewModel.toastMessage.collectAsState()
    val palette = LocalKannachPalette.current

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2300)
            viewModel.clearToast()
        }
    }

    AnimatedVisibility(
        visible = toastMessage != null,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = 96.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            toastMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .shadow(12.dp, CircleShape)
                        .clip(CircleShape)
                        .background(palette.ink)
                        .padding(horizontal = 22.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = msg,
                        fontSize = 15.sp,
                        color = palette.paperHi,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
