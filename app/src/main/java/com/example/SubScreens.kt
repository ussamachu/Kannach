package com.example

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
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
    val isOpen by viewModel.isGalleryOpen.collectAsState()
    if (!isOpen) return

    val palette = LocalKannachPalette.current
    val language by viewModel.language.collectAsState()
    val strings = I18n.forLanguage(language)
    val currentYear by viewModel.currentYear.collectAsState()
    val entries by viewModel.entries.collectAsState()

    val yearEntries = remember(entries, currentYear) {
        entries.filter { it.key.startsWith("$currentYear-") }
    }
    val totalYearDays = if (currentYear % 4 == 0 && (currentYear % 100 != 0 || currentYear % 400 == 0)) 366 else 365
    val remainingDays = (totalYearDays - yearEntries.size).coerceAtLeast(0)

    var searchQuery by remember { mutableStateOf("") }

    val filteredEntries = remember(yearEntries, searchQuery) {
        if (searchQuery.isBlank()) yearEntries.toList()
        else yearEntries.filter { it.value.text.contains(searchQuery, ignoreCase = true) }.toList()
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
                IconButton(onClick = { viewModel.closeGallery() }) {
                    KannachIcon(
                        icon = KannachIconType.X,
                        tint = palette.ink,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = strings.galleryTitle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.greenDeep
                )
                Spacer(modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Year Pill Chip
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(CircleShape)
                    .background(palette.greenSoft)
                    .border(1.5.dp, palette.greenLine, CircleShape)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = I18n.formatYear(currentYear, language),
                    fontSize = 18.sp,
                    fontFamily = LocalKannachHandFont.current,
                    color = palette.greenDeep
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = strings.galleryCount.replace("{n}", yearEntries.size.toString()),
                fontSize = 16.sp,
                color = palette.greenDeep,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Light Paper Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .shadow(3.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.paperHi)
                    .border(1.2.dp, palette.line, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    KannachIcon(
                        icon = KannachIconType.SPROUT,
                        tint = palette.green,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = if (I18n.isRtl(language)) "ابحث في كلمات ذكرياتك…" else "Search your memories…",
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

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Year Wrap & Surprise Me
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Year Wrap Button
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(palette.greenSoft)
                        .border(1.5.dp, palette.greenLine, CircleShape)
                        .clickable { viewModel.openWrap() }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = strings.wrapBtn,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.greenDeep
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Surprise Me Button
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(palette.paperHi)
                        .border(1.5.dp, palette.line, CircleShape)
                        .clickable {
                            val randKey = viewModel.getRandomEntryDateKey()
                            if (randKey != null) {
                                viewModel.closeGallery()
                                viewModel.openSheet(randKey)
                            } else {
                                viewModel.showToast(if (I18n.isRtl(language)) "لا توجد ذكريات مكتوبة بعد" else "No written memories yet")
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KannachIcon(
                            icon = KannachIconType.SPARKLE,
                            tint = palette.orange,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (I18n.isRtl(language)) "فاجئني بذكرى ✨" else "Surprise me ✨",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.ink
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredEntries.isEmpty()) {
                Text(
                    text = if (searchQuery.isNotBlank()) {
                        if (I18n.isRtl(language)) "لم نجد رسماً أو تدوينة تطابق «$searchQuery»" else "No doodles or notes matched \"$searchQuery\""
                    } else strings.galleryEmpty,
                    fontSize = 15.sp,
                    fontFamily = LocalKannachHandFont.current,
                    color = palette.inkFaint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                )
            } else {
                // Mosaic of written days as paper cards with organic rotation & dates
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.Center
                ) {
                    filteredEntries.forEachIndexed { idx, (dateKey, entry) ->
                        val glyphColor = try {
                            Color(android.graphics.Color.parseColor(entry.color))
                        } catch (_: Exception) {
                            palette.green
                        }
                        val rot = if (idx % 3 == 0) -1.8f else if (idx % 2 == 0) 1.5f else 0f

                        val dayNum = dateKey.substringAfterLast("-").toIntOrNull() ?: 1
                        val monthNum = dateKey.substring(5, 7).toIntOrNull() ?: 1
                        val monthShort = I18n.forLanguage(language).months.getOrElse(monthNum - 1) { "" }.take(4)
                        val dateLabel = "$dayNum $monthShort"

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(4.dp)
                                .rotate(rot)
                                .shadow(3.dp, RoundedCornerShape(10.dp))
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.paperHi)
                                .border(1.dp, palette.line.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                                .clickable {
                                    viewModel.closeGallery()
                                    viewModel.openSheet(dateKey)
                                }
                                .padding(horizontal = 6.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(46.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                GlyphView(
                                    strokes = entry.strokes,
                                    color = glyphColor,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = dateLabel,
                                fontSize = 9.sp,
                                fontFamily = LocalKannachHandFont.current,
                                color = palette.inkFaint,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Poetic dots description for remaining days
                val dotsPoeticText = if (I18n.isRtl(language)) {
                    "أزهرت ${yearEntries.size} من أصل $totalYearDays يوماً، وما زالت $remainingDays بذرة تنتظر دورها في الإشراق…"
                } else {
                    "${yearEntries.size} of $totalYearDays days have bloomed, while $remainingDays seeds wait for their time to shine…"
                }

                Text(
                    text = dotsPoeticText,
                    fontSize = 13.sp,
                    fontFamily = LocalKannachHandFont.current,
                    color = palette.inkSoft,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Faint dots for remaining unwritten days
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.Center
                ) {
                    repeat(remainingDays.coerceAtMost(366)) {
                        Box(
                            modifier = Modifier
                                .padding(3.5.dp)
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(palette.dot.copy(alpha = 0.55f))
                        )
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
    val isOpen by viewModel.isWrapOpen.collectAsState()
    if (!isOpen) return

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
                    label = strings.wrapDays,
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
 * 3. TimeCapsuleScreen (رسالة إلى الغد):
 * Write sealed letters to future self, choose unlock date, and view waiting / unlocked letters.
 */
@Composable
fun TimeCapsuleScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isOpen by viewModel.isCapsulePageOpen.collectAsState()
    if (!isOpen) return

    val palette = LocalKannachPalette.current
    val language by viewModel.language.collectAsState()
    val isRtl = I18n.isRtl(language)
    val strings = I18n.forLanguage(language)
    val context = LocalContext.current

    var capsuleText by remember { mutableStateOf("") }
    var selectedDate by remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        mutableStateOf(sdf.format(cal.time))
    }

    val capsules by viewModel.capsules.collectAsState()
    val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

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
                IconButton(onClick = { viewModel.closeCapsulePage() }) {
                    KannachIcon(
                        icon = if (isRtl) KannachIconType.CHEV_R else KannachIconType.CHEV_L,
                        tint = palette.ink,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = strings.capsuleTitle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.greenDeep
                )
                Spacer(modifier = Modifier.size(40.dp))
            }

            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = strings.capsuleIntro,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = palette.inkSoft
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = strings.capsuleWriteLabel,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.greenDeep
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Text field with ruled notebook lines
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)
                        .clip(HandDrawnRadiusShape)
                        .background(palette.paperHi)
                        .border(1.5.dp, palette.line, HandDrawnRadiusShape)
                        .linedPaperBackground(palette.line, 30.dp)
                        .padding(12.dp)
                ) {
                    if (capsuleText.isEmpty()) {
                        Text(
                            text = strings.capsulePlaceholder,
                            fontSize = 17.sp,
                            lineHeight = 30.sp,
                            fontFamily = LocalKannachHandFont.current,
                            color = palette.inkFaint
                        )
                    }
                    BasicTextField(
                        value = capsuleText,
                        onValueChange = { capsuleText = it },
                        textStyle = TextStyle(
                            fontSize = 17.sp,
                            lineHeight = 30.sp,
                            fontFamily = LocalKannachHandFont.current,
                            color = palette.ink
                        ),
                        cursorBrush = SolidColor(palette.greenDeep),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = strings.capsuleDateLabel,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.greenDeep
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Date Picker trigger button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.paperHi)
                        .border(1.5.dp, palette.line, RoundedCornerShape(12.dp))
                        .clickable {
                            val cal = Calendar.getInstance()
                            cal.add(Calendar.DAY_OF_YEAR, 1)
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    selectedDate = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).apply {
                                datePicker.minDate = cal.timeInMillis
                            }.show()
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = selectedDate,
                        fontSize = 16.sp,
                        color = palette.ink
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Seal Letter Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(HandDrawnRadiusShape)
                        .background(palette.green)
                        .border(1.5.dp, palette.greenDeep, HandDrawnRadiusShape)
                        .clickable {
                            viewModel.saveCapsule(capsuleText, selectedDate)
                            capsuleText = ""
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KannachIcon(icon = KannachIconType.SEAL, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.capsuleSaveBtn,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Capsule List
                if (capsules.isEmpty()) {
                    Text(
                        text = strings.capsuleEmptyList,
                        fontSize = 14.sp,
                        color = palette.inkFaint,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        capsules.sortedBy { it.unlockDate }.forEach { capsule ->
                            val isReady = capsule.unlockDate <= todayIso
                            val statusText = if (isReady) {
                                strings.capsuleReady
                            } else {
                                strings.capsuleLocked.replace("{d}", capsule.unlockDate)
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(HandDrawnRadiusShape)
                                    .background(if (isReady) palette.greenSoft else palette.paperHi)
                                    .border(
                                        1.5.dp,
                                        if (isReady) palette.green else palette.greenLine,
                                        HandDrawnRadiusShape
                                    )
                                    .clickable(enabled = isReady) {
                                        viewModel.openReadyCapsule(capsule)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = statusText,
                                        fontSize = 14.sp,
                                        fontWeight = if (isReady) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isReady) palette.greenDeep else palette.inkSoft
                                    )
                                    KannachIcon(
                                        icon = if (isReady) KannachIconType.CHECK else KannachIconType.SEAL,
                                        tint = if (isReady) palette.green else palette.inkFaint,
                                        modifier = Modifier.size(18.dp)
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
 * 4. SketchpadScreen (فقط ارسم):
 * A free drawing canvas with color picker and direct export/share to PNG.
 */
@Composable
fun SketchpadScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isOpen by viewModel.isSketchPageOpen.collectAsState()
    if (!isOpen) return

    val palette = LocalKannachPalette.current
    val language by viewModel.language.collectAsState()
    val strings = I18n.forLanguage(language)
    val context = LocalContext.current

    val strokes by viewModel.sketchStrokes.collectAsState()
    val selectedColor by viewModel.sketchColor.collectAsState()

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
                IconButton(onClick = { viewModel.closeSketchPage() }) {
                    KannachIcon(
                        icon = KannachIconType.X,
                        tint = palette.ink,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = strings.sketchTitle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.greenDeep
                )
                Spacer(modifier = Modifier.size(40.dp))
            }

            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = strings.sketchIntro,
                    fontSize = 15.sp,
                    color = palette.inkSoft,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Free Canvas (300x400)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .widthIn(max = 320.dp)
                        .aspectRatio(3f / 4f)
                        .shadow(12.dp, HandDrawnRadiusShape)
                        .clip(HandDrawnRadiusShape)
                        .background(palette.paperHi)
                        .border(2.dp, palette.ink, HandDrawnRadiusShape)
                ) {
                    DrawingCanvas(
                        strokes = strokes,
                        colorHex = selectedColor,
                        onStrokeCompleted = { viewModel.addSketchStroke(it) },
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

                Spacer(modifier = Modifier.height(12.dp))

                // Tools: Undo & Clear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(HandDrawnRadiusAltShape)
                            .background(palette.paperHi)
                            .border(1.5.dp, palette.line, HandDrawnRadiusAltShape)
                            .clickable { viewModel.undoSketchStroke() }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        KannachIcon(icon = KannachIconType.UNDO, tint = palette.inkSoft, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = strings.undo, fontSize = 13.sp, color = palette.inkSoft)
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(HandDrawnRadiusAltShape)
                            .background(palette.paperHi)
                            .border(1.5.dp, palette.line, HandDrawnRadiusAltShape)
                            .clickable { viewModel.clearSketchStrokes() }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        KannachIcon(icon = KannachIconType.X, tint = palette.inkSoft, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = strings.clear, fontSize = 13.sp, color = palette.inkSoft)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

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
                                    if (isSelected) Modifier.border(2.5.dp, palette.ink, CircleShape) else Modifier
                                )
                                .clickable { viewModel.setSketchColor(hex) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Share / Save PNG Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(HandDrawnRadiusShape)
                        .background(palette.green)
                        .border(1.5.dp, palette.greenDeep, HandDrawnRadiusShape)
                        .clickable { viewModel.shareSketch(context) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KannachIcon(icon = KannachIconType.UPLOAD, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.sketchShareBtn,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * 5. SettingsScreen (الإعدادات):
 * Language switcher, daily reminders, backup export/import, and about info.
 */
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isOpen by viewModel.isSettingsOpen.collectAsState()
    if (!isOpen) return

    val palette = LocalKannachPalette.current
    val language by viewModel.language.collectAsState()
    val strings = I18n.forLanguage(language)
    val reminder by viewModel.reminder.collectAsState()
    val context = LocalContext.current

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
                viewModel.showToast(strings.importError)
            }
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
                IconButton(onClick = { viewModel.closeSettings() }) {
                    KannachIcon(
                        icon = KannachIconType.X,
                        tint = palette.ink,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = strings.settings,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.greenDeep
                )
                Spacer(modifier = Modifier.size(40.dp))
            }

            Column(modifier = Modifier.padding(20.dp)) {
                // Section: Language
                SectionTitle(strings.language)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val isEn = language == "en"
                    val isAr = language == "ar"

                    LangButton(
                        label = "English",
                        active = isEn,
                        onClick = { viewModel.setLanguage("en") },
                        modifier = Modifier.weight(1f)
                    )
                    LangButton(
                        label = "العربية",
                        active = isAr,
                        onClick = { viewModel.setLanguage("ar") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section: Daily Reminder
                SectionTitle(strings.reminderTitle)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(HandDrawnRadiusAltShape)
                        .background(palette.greenSoft)
                        .border(2.dp, palette.greenLine, HandDrawnRadiusAltShape)
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.reminderToggle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.greenDeep
                            )
                            Switch(
                                checked = reminder.enabled,
                                onCheckedChange = { viewModel.toggleReminder(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = palette.green
                                )
                            )
                        }

                        if (reminder.enabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.reminderTimeLabel,
                                    fontSize = 15.sp,
                                    color = palette.greenDeep
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(palette.paperHi)
                                        .border(1.dp, palette.line, RoundedCornerShape(10.dp))
                                        .clickable {
                                            val parts = reminder.time.split(":").mapNotNull { it.toIntOrNull() }
                                            val h = if (parts.isNotEmpty()) parts[0] else 20
                                            val m = if (parts.size > 1) parts[1] else 0
                                            TimePickerDialog(
                                                context,
                                                { _, sH, sM ->
                                                    viewModel.setReminderTime(String.format(Locale.US, "%02d:%02d", sH, sM))
                                                },
                                                h,
                                                m,
                                                true
                                            ).show()
                                        }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = reminder.time,
                                        fontSize = 16.sp,
                                        color = palette.ink
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section: Save & Restore Data
                SectionTitle(strings.data)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(HandDrawnRadiusShape)
                        .background(palette.paperHi)
                        .border(1.5.dp, palette.ink, HandDrawnRadiusShape)
                        .clickable { viewModel.exportBackup(context) }
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.exportBtn,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.ink
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(HandDrawnRadiusShape)
                        .background(palette.paperHi)
                        .border(1.5.dp, palette.ink, HandDrawnRadiusShape)
                        .clickable { importLauncher.launch("application/json") }
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.importBtn,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.ink
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Section: About
                SectionTitle(strings.about)

                Text(
                    text = strings.aboutText,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = palette.ink
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "كُنَّاش · kannach — v1.2",
                    fontSize = 14.sp,
                    color = palette.inkFaint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    val palette = LocalKannachPalette.current
    Text(
        text = title,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = palette.greenDeep,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun LangButton(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalKannachPalette.current

    Box(
        modifier = modifier
            .clip(HandDrawnRadiusShape)
            .background(if (active) palette.greenSoft else palette.paperHi)
            .border(
                1.5.dp,
                if (active) palette.greenDeep else palette.line,
                HandDrawnRadiusShape
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                color = if (active) palette.greenDeep else palette.ink
            )
            if (active) {
                Spacer(modifier = Modifier.width(6.dp))
                KannachIcon(icon = KannachIconType.CHECK, tint = palette.greenDeep, modifier = Modifier.size(16.dp))
            }
        }
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
            .background(Color.Black.copy(alpha = 0.45f))
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
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(HandDrawnRadiusShape)
                            .background(palette.paper)
                            .border(1.5.dp, palette.ink, HandDrawnRadiusShape)
                            .clickable { data.onCancel() }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = data.cancelText,
                            color = palette.ink,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .clip(HandDrawnRadiusShape)
                            .background(palette.red)
                            .border(1.5.dp, Color(0xFFB44636), HandDrawnRadiusShape)
                            .clickable { data.onConfirm() }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = data.confirmText,
                            color = Color.White,
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
            .padding(bottom = 80.dp)
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
