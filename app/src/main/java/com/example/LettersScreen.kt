package com.example

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.zIndex
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Phase 5 — Letters (CP1–CP10, RB1 & RB5):
 * - Shelf view (CP1–CP3): Top paper card "Write a letter to future you" -> Compose;
 *   sections "Waiting" (locked), "Ready" (due, with seal-crack glow), "Opened" (archive).
 *   Countdown + localized long dates (never raw ISO dates, fixing RB5).
 *   Locked tap shows toast "Not yet — {n} days to go". Long-press or delete icon opens ConfirmDialogOverlay.
 * - Full-screen Compose page (CP4–CP7, CP9):
 *   Ruled letter paper in hand font, prefilled "Dear future me,", "Written {date}" stamp,
 *   optional 3:4 doodle with 5 colors, preset date chips (1w, 1m default, 3m, 6m, 1y, Pick a date),
 *   Seal confirmation -> POST_NOTIFICATIONS one-time request -> C2 sealing ceremony -> 1s shelf highlight.
 * - Opened view (CP8): Memory-card layout with ruled text, optional doodle, relative distance copy,
 *   and quiet "Write back" / "Delete" text actions.
 */
@Composable
fun TimeCapsuleScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isComposeOpen by viewModel.isCapsuleComposeOpen.collectAsState()
    val reduceMotion = rememberReduceMotion()
    val enterTransition = kannachSubScreenEnter(reduceMotion)
    val exitTransition = kannachSubScreenExit(reduceMotion)

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = isComposeOpen,
            transitionSpec = {
                if (reduceMotion) {
                    fadeIn(tween(KannachMotion.INSTANT)) togetherWith fadeOut(tween(KannachMotion.INSTANT))
                } else {
                    enterTransition togetherWith exitTransition
                }
            },
            label = "LettersShelfOrCompose"
        ) { composeActive ->
            if (composeActive) {
                CapsuleComposeScreen(viewModel = viewModel)
            } else {
                CapsuleShelfScreen(viewModel = viewModel)
            }
        }
    }
}

/**
 * CP1–CP3: Letters Shelf Screen.
 */
@Composable
private fun CapsuleShelfScreen(
    viewModel: MainViewModel
) {
    val palette = LocalKannachPalette.current
    val language by viewModel.language.collectAsState()
    val strings = I18n.forLanguage(language)
    val capsules by viewModel.capsules.collectAsState()
    val highlightedId by viewModel.highlightedCapsuleId.collectAsState()
    val reduceMotion = rememberReduceMotion()

    val todayIso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    val waitingCapsules = remember(capsules, todayIso) {
        capsules.filter { !it.opened && it.unlockDate > todayIso }.sortedBy { it.unlockDate }
    }
    val readyCapsules = remember(capsules, todayIso) {
        capsules.filter { !it.opened && it.unlockDate <= todayIso }.sortedBy { it.unlockDate }
    }
    val openedCapsules = remember(capsules) {
        capsules.filter { it.opened }.sortedByDescending { it.openedAt ?: it.createdAt }
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
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .paperBackground(palette.paper)
            .testTag("capsule_shelf_screen")
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = dockInset),
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(dockScroll)
        ) {
            // Header (NV7: Settings icon in leading 48dp slot)
            item(key = "letters_header", contentType = "header") {
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
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("letters_header_settings_button")
                    ) {
                        KannachIcon(
                            icon = KannachIconType.SLIDERS,
                            tint = palette.inkSoft,
                            contentDescription = strings.openSettings,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = strings.capsuleTitle,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = LocalKannachHandFont.current,
                        color = palette.greenDeep
                    )
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }

            // LS1: Replace the big call-to-action card with a blank 3:2 letter sheet
            item(key = "capsule_compose_card", contentType = "compose_action") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                        .aspectRatio(1.5f)
                        .paperTilt(-1f)
                        .paperShadow(
                            shadowColor = palette.paperShadow,
                            shape = HandDrawnRadiusShape,
                            offsetY = 4.dp
                        )
                        .clip(HandDrawnRadiusShape)
                        .paperBackground(palette.paperHi)
                        .linedPaperBackground(
                            lineColor = palette.line.copy(alpha = 0.65f),
                            lineHeightDp = 28.dp
                        )
                        .border(1.5.dp, palette.line, HandDrawnRadiusShape)
                        .clickable { viewModel.openCapsuleCompose() }
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                        .testTag("capsule_compose_card")
                ) {
                    Text(
                        text = strings.capsuleSalutation,
                        fontSize = 20.sp,
                        fontFamily = LocalKannachHandFont.current,
                        color = palette.ink.copy(alpha = 0.55f),
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                    KannachIcon(
                        icon = KannachIconType.EDIT,
                        tint = palette.inkSoft,
                        contentDescription = strings.capsuleBtn,
                        strokeWidthDp = 1.8.dp,
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.BottomEnd)
                    )
                }
            }

            // CP1: Empty shelf vs Sections ("Waiting", "Ready", "Opened")
            if (capsules.isEmpty()) {
                item(key = "capsule_empty_shelf", contentType = "empty_shelf") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp, horizontal = 16.dp)
                            .testTag("capsule_empty_shelf"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(palette.paperHi)
                                .dashedHandDrawnBorder(
                                    color = palette.line,
                                    shape = CircleShape,
                                    strokeWidth = 1.5.dp
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            KannachIcon(
                                icon = KannachIconType.SEAL,
                                tint = palette.inkFaint,
                                strokeWidthDp = 1.8.dp,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = strings.capsuleEmptyList,
                            fontSize = 17.sp,
                            fontFamily = LocalKannachHandFont.current,
                            color = palette.inkSoft,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Section 1: Waiting (locked) — LS2..LS5
                if (waitingCapsules.isNotEmpty()) {
                    item(key = "capsule_section_header_waiting", contentType = "section_header") {
                        Box(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 8.dp)) {
                            ShelfSectionHeader(
                                title = strings.capsuleSectionWaiting,
                                count = waitingCapsules.size,
                                testTag = "capsule_section_waiting"
                            )
                        }
                    }
                    item(key = "waiting_envelopes_stack", contentType = "waiting_stack") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy((-10).dp)
                        ) {
                            waitingCapsules.forEachIndexed { idx, capsule ->
                                val daysLeft = I18n.daysUntilDateKey(capsule.unlockDate, todayIso)
                                val countdownSentence = strings.capsuleCountdown.format(language, daysLeft)

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .zIndex((waitingCapsules.size - idx).toFloat())
                                ) {
                                    EnvelopeCard(
                                        capsule = capsule,
                                        state = EnvelopeState.WAITING,
                                        statusText = countdownSentence,
                                        previewText = null,
                                        isHighlighted = highlightedId == capsule.id,
                                        reduceMotion = reduceMotion,
                                        tiltIndex = idx,
                                        language = language,
                                        deleteContentDescription = strings.delete,
                                        onTap = { viewModel.onLockedCapsuleTap(capsule) },
                                        onDelete = { viewModel.requestDeleteCapsule(capsule.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 2: Ready (due, with seal-crack glow)
                if (readyCapsules.isNotEmpty()) {
                    item(key = "capsule_section_header_ready", contentType = "section_header") {
                        Box(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 8.dp)) {
                            ShelfSectionHeader(
                                title = strings.capsuleSectionReady,
                                count = readyCapsules.size,
                                testTag = "capsule_section_ready"
                            )
                        }
                    }
                    item(key = "ready_envelopes_stack", contentType = "ready_stack") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy((-10).dp)
                        ) {
                            readyCapsules.forEachIndexed { idx, capsule ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .zIndex((readyCapsules.size - idx).toFloat())
                                ) {
                                    EnvelopeCard(
                                        capsule = capsule,
                                        state = EnvelopeState.READY,
                                        statusText = strings.capsuleReady,
                                        previewText = null,
                                        isHighlighted = highlightedId == capsule.id,
                                        reduceMotion = reduceMotion,
                                        tiltIndex = idx,
                                        language = language,
                                        deleteContentDescription = strings.delete,
                                        onTap = { viewModel.openReadyCapsule(capsule) },
                                        onDelete = { viewModel.requestDeleteCapsule(capsule.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 3: Opened (archive)
                if (openedCapsules.isNotEmpty()) {
                    item(key = "capsule_section_header_opened", contentType = "section_header") {
                        Box(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 4.dp)) {
                            ShelfSectionHeader(
                                title = strings.capsuleSectionOpened,
                                count = openedCapsules.size,
                                testTag = "capsule_section_opened"
                            )
                        }
                    }
                    items(
                        items = openedCapsules,
                        key = { "opened_${it.id}" },
                        contentType = { "opened_envelope" }
                    ) { capsule ->
                        val idx = openedCapsules.indexOf(capsule)
                        val writtenLong = I18n.formatLongDateFromMillis(capsule.createdAt, language)
                        val openedLong = if (capsule.openedAt != null && capsule.openedAt > 0L) {
                            I18n.formatLongDateFromMillis(capsule.openedAt, language)
                        } else {
                            I18n.formatLongDate(capsule.unlockDate, language)
                        }
                        val metaLine = strings.capsuleOpenedMeta
                            .replace("{w}", writtenLong)
                            .replace("{o}", openedLong)

                        Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                            EnvelopeCard(
                                capsule = capsule,
                                state = EnvelopeState.OPENED,
                                statusText = metaLine,
                                previewText = capsule.text.lineSequence().firstOrNull { it.isNotBlank() },
                                isHighlighted = false,
                                reduceMotion = reduceMotion,
                                tiltIndex = idx,
                                language = language,
                                deleteContentDescription = strings.delete,
                                onTap = { viewModel.openArchivedCapsule(capsule) },
                                onDelete = { viewModel.requestDeleteCapsule(capsule.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShelfSectionHeader(
    title: String,
    count: Int,
    testTag: String
) {
    val palette = LocalKannachPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = LocalKannachHandFont.current,
            color = palette.greenDeep
        )
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(palette.paperHi)
                .border(1.dp, palette.line, CircleShape)
                .padding(horizontal = 8.dp, vertical = 1.dp)
        ) {
            Text(
                text = count.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.inkSoft
            )
        }
    }
}

private enum class EnvelopeState {
    WAITING, READY, OPENED
}

private val EnvelopeRectShape = HandDrawnShape(
    radiusTopLeft = 4f,
    radiusTopRight = 6f,
    radiusBottomRight = 5f,
    radiusBottomLeft = 4f,
    altWobble = true
)

/**
 * LS2–LS5: 16:10 hand-drawn rectangle envelope, V flap of two dashed ink lines from top corners
 * to center, alternating tilt (-1.2° / +1.0°), 28 dp green wax seal with sprout icon on flap tip,
 * 46 dp rotated (-8°) dashed postmark at top-end, and 16 sp hand countdown line at bottom-start.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EnvelopeCard(
    capsule: TimeCapsule,
    state: EnvelopeState,
    statusText: String,
    previewText: String?,
    isHighlighted: Boolean,
    reduceMotion: Boolean,
    tiltIndex: Int,
    language: String,
    deleteContentDescription: String,
    onTap: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = LocalKannachPalette.current

    val targetBg = when {
        isHighlighted -> palette.greenSoft
        state == EnvelopeState.READY -> palette.greenSoft
        state == EnvelopeState.OPENED -> palette.paperHi.copy(alpha = 0.78f)
        else -> palette.paperHi
    }
    val targetBorder = when {
        isHighlighted -> palette.greenDeep
        state == EnvelopeState.READY -> palette.green
        else -> palette.ink.copy(alpha = 0.32f)
    }

    val bgColor by animateColorAsState(
        targetValue = targetBg,
        animationSpec = tween(if (reduceMotion) KannachMotion.INSTANT else KannachMotion.SOFT),
        label = "envelopeBg"
    )
    val borderColor by animateColorAsState(
        targetValue = targetBorder,
        animationSpec = tween(if (reduceMotion) KannachMotion.INSTANT else KannachMotion.SOFT),
        label = "envelopeBorder"
    )

    // LS2: Alternating tilt of -1.2° / +1.0°
    val tiltAngle = if (tiltIndex % 2 == 0) -1.2f else 1.0f

    val postmarkDayMonth = remember(capsule.unlockDate, language, I18n.useArabicIndicNumerals) {
        I18n.formatPostmarkDayMonth(capsule.unlockDate, language)
    }
    val postmarkYear = remember(capsule.unlockDate, language, I18n.useArabicIndicNumerals) {
        I18n.formatPostmarkYear(capsule.unlockDate, language)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.6f)
            .paperTilt(tiltAngle)
            .paperShadow(
                shadowColor = palette.paperShadow,
                shape = EnvelopeRectShape,
                offsetY = 3.dp
            )
            .clip(EnvelopeRectShape)
            .paperBackground(bgColor)
            .border(
                width = if (isHighlighted || state == EnvelopeState.READY) 1.8.dp else 1.4.dp,
                color = borderColor,
                shape = EnvelopeRectShape
            )
            .drawBehind {
                // LS2: Flap drawn as a V of two dashed ink lines from the top corners to the center
                val flapPath = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width * 0.5f, size.height * 0.5f)
                    lineTo(size.width, 0f)
                }
                drawPath(
                    path = flapPath,
                    color = palette.ink.copy(alpha = 0.38f),
                    style = Stroke(
                        width = 1.3.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f), 0f)
                    )
                )
            }
            .combinedClickable(
                onClick = onTap,
                onLongClick = onDelete
            )
            .padding(14.dp)
            .testTag("capsule_card_${capsule.id}")
    ) {
        // LS4: Postmark at top-end corner (46 dp circle, dashed ring, rotated -8°)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(46.dp)
                .rotate(-8f)
                .drawBehind {
                    drawCircle(
                        color = palette.inkSoft.copy(alpha = 0.65f),
                        radius = size.minDimension * 0.5f - 1.dp.toPx(),
                        style = Stroke(
                            width = 1.1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                        )
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = postmarkDayMonth,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.inkSoft,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = postmarkYear,
                    fontSize = 9.sp,
                    color = palette.inkSoft,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // LS3: Wax seal (28 dp palette.green circle with sprout icon in palette.onGreen on flap tip, thin darker ring)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(28.dp)
                .clip(CircleShape)
                .background(palette.green)
                .border(1.2.dp, palette.greenDeep, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            KannachIcon(
                icon = KannachIconType.SPROUT,
                tint = palette.onGreen,
                strokeWidthDp = 1.8.dp,
                modifier = Modifier.size(16.dp)
            )
        }

        // LS5: Countdown line at bottom-start (hand font 16 sp, maxLines = 1, lineHeight = 1.3 * fontSize)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(end = 44.dp)
        ) {
            if (!previewText.isNullOrBlank()) {
                Text(
                    text = previewText,
                    fontSize = 15.sp,
                    fontFamily = LocalKannachHandFont.current,
                    color = palette.inkSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
            Text(
                text = statusText,
                fontSize = 16.sp,
                lineHeight = (16f * 1.3f).sp,
                fontFamily = LocalKannachHandFont.current,
                color = if (state == EnvelopeState.READY) palette.greenDeep else palette.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("capsule_status_${capsule.id}")
            )
        }

        // Quiet 48dp delete icon button at bottom-end
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(48.dp)
                .offset(x = 8.dp, y = 8.dp)
                .testTag("capsule_delete_${capsule.id}")
        ) {
            KannachIcon(
                icon = KannachIconType.TRASH,
                tint = palette.inkFaint,
                contentDescription = deleteContentDescription,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * LC7: Scalloped postage-stamp shape (64×76 dp) with perforated edges drawn via a scalloped Path.
 */
private fun buildScallopedStampPath(size: Size, density: Density): Path {
    val w = size.width
    val h = size.height
    val r = with(density) { 3.2.dp.toPx() }
    val step = r * 3.1f

    return Path().apply {
        moveTo(0f, 0f)

        // Top edge (left -> right)
        val topCount = ((w - step) / step).toInt().coerceAtLeast(2)
        val topSpan = w / topCount
        for (i in 0 until topCount) {
            val cx = (i + 0.5f) * topSpan
            lineTo(cx - r, 0f)
            arcTo(
                rect = Rect(cx - r, -r, cx + r, r),
                startAngleDegrees = 180f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
        }
        lineTo(w, 0f)

        // Right edge (top -> bottom)
        val sideCount = ((h - step) / step).toInt().coerceAtLeast(3)
        val sideSpan = h / sideCount
        for (i in 0 until sideCount) {
            val cy = (i + 0.5f) * sideSpan
            lineTo(w, cy - r)
            arcTo(
                rect = Rect(w - r, cy - r, w + r, cy + r),
                startAngleDegrees = 270f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
        }
        lineTo(w, h)

        // Bottom edge (right -> left)
        for (i in topCount - 1 downTo 0) {
            val cx = (i + 0.5f) * topSpan
            lineTo(cx + r, h)
            arcTo(
                rect = Rect(cx - r, h - r, cx + r, h + r),
                startAngleDegrees = 0f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
        }
        lineTo(0f, h)

        // Left edge (bottom -> top)
        for (i in sideCount - 1 downTo 0) {
            val cy = (i + 0.5f) * sideSpan
            lineTo(0f, cy + r)
            arcTo(
                rect = Rect(-r, cy - r, r, cy + r),
                startAngleDegrees = 90f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
        }
        lineTo(0f, 0f)
        close()
    }
}

private val PostageStampShape: Shape = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline = Outline.Generic(buildScallopedStampPath(size, density))
}

/**
 * Phase 5 & v5.2 §2.2 — Letters compose (LC1–LC9):
 * - LC6: One sheet of lined paper (`linedPaperBackground`, line spacing = 36 sp) filling the width with a 12 dp margin:
 *   1. Non-editable first line in hand font: salutation from LS1 (`capsuleSalutation`).
 *   2. Editable body below it, 20 sp hand font, 36 sp line height, local state + 800 ms debounce (LC5).
 *   3. Non-editable last line at bottom-end: `— {today’s date}` in the app language.
 *   4. Title field removed; envelope title is derived from the first 28 characters of the body.
 * - LC7: "When should it find you?" as 6 postage-stamp chips (perforated scalloped Path, 64×76 dp)
 *   with a rotated postmark ink ring over the selected stamp, plus the plain `opensOnSentence` below.
 * - LC8: Hold-to-seal button inside the LC3 confirm sheet.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CapsuleComposeScreen(
    viewModel: MainViewModel
) {
    val palette = LocalKannachPalette.current
    val language by viewModel.language.collectAsState()
    val strings = I18n.forLanguage(language)
    val isRtl = I18n.isRtl(language)
    val context = LocalContext.current
    val reduceMotion = rememberReduceMotion()
    val density = LocalDensity.current

    val sessionId by viewModel.capsuleComposeSessionId.collectAsState()
    val initialTitle by viewModel.capsuleTitleText.collectAsState()
    val initialBody by viewModel.capsuleDraftText.collectAsState()
    val hasBodyText by viewModel.capsuleHasBodyText.collectAsState()
    val draftStatus by viewModel.capsuleDraftStatus.collectAsState()
    val isBackSheetOpen by viewModel.isCapsuleBackSheetOpen.collectAsState()
    val isSealSheetOpen by viewModel.isCapsuleSealSheetOpen.collectAsState()

    val doodleVisible by viewModel.capsuleDoodleVisible.collectAsState()
    val doodleStrokes by viewModel.capsuleDoodleStrokes.collectAsState()
    val doodleColor by viewModel.capsuleDoodleColor.collectAsState()
    val selectedPreset by viewModel.capsulePreset.collectAsState()
    val unlockDate by viewModel.capsuleUnlockDate.collectAsState()

    val todayIso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val todayFormatted = remember(language) {
        I18n.formatLongDateFromMillis(System.currentTimeMillis(), language)
    }

    val isDateInFuture = remember(unlockDate, todayIso) {
        unlockDate.isNotBlank() && unlockDate > todayIso
    }
    val fullUnlockWeekdayDate = remember(unlockDate, language) {
        I18n.formatFullWeekdayDate(unlockDate, language)
    }
    val opensOnSentence = remember(fullUnlockWeekdayDate, isDateInFuture, language) {
        if (unlockDate.isBlank()) {
            strings.capsuleDateError
        } else {
            strings.capsuleOpensOnFull.replace("{d}", fullUnlockWeekdayDate)
        }
    }

    val isSealEnabled = hasBodyText && isDateInFuture
    val disabledHelperText = when {
        !hasBodyText -> strings.capsuleHelperWriteFirst
        !isDateInFuture -> strings.capsuleHelperChooseDate
        else -> ""
    }

    // CP9: One-time POST_NOTIFICATIONS runtime permission request on first seal
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Proceed regardless of grant/denial
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .paperBackground(palette.paper)
            .testTag("capsule_compose_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // LC1.1: Top bar: back, title `capsuleComposeTitle`, and quiet draft status at the end
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wavyHeaderDivider(palette.greenLine, waveHeightDp = 6.dp)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { viewModel.requestCloseCapsuleCompose() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("capsule_compose_close")
                ) {
                    KannachIcon(
                        icon = if (isRtl) KannachIconType.CHEV_R else KannachIconType.CHEV_L,
                        tint = palette.inkSoft,
                        contentDescription = strings.cancel,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = strings.capsuleComposeTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = LocalKannachHandFont.current,
                    color = palette.greenDeep,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp),
                    textAlign = TextAlign.Start
                )

                val statusLabel = when (draftStatus) {
                    CapsuleDraftStatus.SAVING -> strings.capsuleDraftSaving
                    CapsuleDraftStatus.SAVED -> strings.capsuleDraftSaved
                    CapsuleDraftStatus.IDLE -> ""
                }
                if (statusLabel.isNotEmpty()) {
                    Text(
                        text = statusLabel,
                        fontSize = 12.5.sp,
                        fontFamily = LocalKannachHandFont.current,
                        color = palette.inkFaint,
                        maxLines = 1,
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .testTag("capsule_draft_status")
                    )
                } else {
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }

            // Scrollable middle area containing LC6 letter sheet, LC7 stamps, and optional doodle
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // LC6: One sheet of lined paper (`linedPaperBackground`, line spacing = 36 sp)
                // filling the width with a 12 dp margin.
                CapsuleLetterSheet(
                    sessionId = sessionId,
                    salutation = strings.capsuleSalutation,
                    initialBody = initialBody,
                    placeholder = strings.capsulePlaceholder,
                    talkBackLabel = strings.capsuleWriteLabel,
                    todaySignature = "— $todayFormatted",
                    palette = palette,
                    onBodyChanged = { viewModel.onCapsuleBodyInputChanged(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                )

                // LC7: "When should it find you?" as six 64×76 dp postage-stamp chips
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = strings.capsuleDateLabel,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = LocalKannachHandFont.current,
                        color = palette.greenDeep,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    val presets = listOf(
                        CapsulePreset.TOMORROW to strings.capsulePresetTomorrow,
                        CapsulePreset.WEEK_1 to strings.capsulePreset1w,
                        CapsulePreset.MONTH_1 to strings.capsulePreset1m,
                        CapsulePreset.MONTH_6 to strings.capsulePreset6m,
                        CapsulePreset.YEAR_1 to strings.capsulePreset1y
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        presets.forEach { (preset, label) ->
                            val isSelected = selectedPreset == preset
                            PostageStampChip(
                                label = label,
                                isSelected = isSelected,
                                palette = palette,
                                density = density,
                                onClick = { viewModel.selectCapsulePreset(preset) },
                                modifier = Modifier.testTag("capsule_preset_${preset.name.lowercase(Locale.ROOT)}")
                            )
                        }

                        val isCustom = selectedPreset == CapsulePreset.CUSTOM
                        PostageStampChip(
                            label = strings.capsulePickDate,
                            isSelected = isCustom,
                            palette = palette,
                            density = density,
                            onClick = {
                                val defaultCal = Calendar.getInstance().apply {
                                    add(Calendar.DAY_OF_YEAR, 1)
                                }
                                val parts = unlockDate.split("-").mapNotNull { it.toIntOrNull() }
                                val initYear = parts.getOrNull(0) ?: defaultCal.get(Calendar.YEAR)
                                val initMonth = (parts.getOrNull(1) ?: (defaultCal.get(Calendar.MONTH) + 1)) - 1
                                val initDay = parts.getOrNull(2) ?: defaultCal.get(Calendar.DAY_OF_MONTH)

                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        viewModel.setCustomCapsuleDate(
                                            String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                                        )
                                    },
                                    initYear,
                                    initMonth,
                                    initDay
                                ).show()
                            },
                            modifier = Modifier.testTag("capsule_preset_custom")
                        )
                    }

                    // LC1.3 & LC7: Plain sentence with the result below the stamps row
                    Text(
                        text = opensOnSentence,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = LocalKannachHandFont.current,
                        color = if (isDateInFuture) palette.greenDeep else palette.danger,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .testTag("capsule_opening_date_summary")
                    )
                }

                // LC1.6: Optional attachments row (existing "add drawing" action)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(HandDrawnRadiusAltShape)
                            .background(if (doodleVisible) palette.greenSoft else palette.paperHi)
                            .border(
                                1.5.dp,
                                if (doodleVisible) palette.green else palette.line,
                                HandDrawnRadiusAltShape
                            )
                            .semantics(mergeDescendants = true) {
                                contentDescription = strings.capsuleAddDoodle
                            }
                            .clickable { viewModel.toggleCapsuleDoodle() }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("capsule_toggle_doodle"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            KannachIcon(
                                icon = KannachIconType.BRUSH,
                                tint = palette.greenDeep,
                                strokeWidthDp = 1.9.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = strings.capsuleAddDoodle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = LocalKannachHandFont.current,
                                color = palette.greenDeep
                            )
                        }
                        KannachIcon(
                            icon = if (doodleVisible) KannachIconType.X else KannachIconType.FLOWER,
                            tint = palette.inkSoft,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = doodleVisible,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.78f)
                                    .widthIn(max = 240.dp)
                                    .aspectRatio(3f / 4f)
                                    .clip(HandDrawnRadiusShape)
                                    .background(palette.paperHi)
                                    .border(1.8.dp, palette.ink, HandDrawnRadiusShape)
                                    .testTag("capsule_doodle_canvas")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp)
                                        .dashedHandDrawnBorder(
                                            color = palette.greenLine.copy(alpha = 0.65f),
                                            shape = HandDrawnRadiusShape,
                                            strokeWidth = 1.2.dp,
                                            dashIntervals = floatArrayOf(8f, 6f)
                                        )
                                )
                                DrawingCanvas(
                                    strokes = doodleStrokes,
                                    colorHex = doodleColor,
                                    onStrokeCompleted = { viewModel.addCapsuleDoodleStroke(it) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 5 color swatches + Undo / Clear (all >= 48 dp targets)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                I18n.COLORS.forEach { colorOpt ->
                                    val hex = colorOpt.hex
                                    val isSelected = doodleColor.equals(hex, ignoreCase = true)
                                    val c = Color(android.graphics.Color.parseColor(hex))
                                    val moodName = I18n.getMoodName(hex, language)
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .semantics {
                                                selected = isSelected
                                                contentDescription = moodName
                                            }
                                            .clickable { viewModel.selectCapsuleDoodleColor(hex) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(if (isSelected) 32.dp else 26.dp)
                                                .clip(CircleShape)
                                                .background(c)
                                                .then(
                                                    if (isSelected) Modifier.border(2.2.dp, palette.ink, CircleShape)
                                                    else Modifier
                                                )
                                        )
                                    }
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .clip(HandDrawnRadiusAltShape)
                                        .background(palette.paperHi)
                                        .border(1.2.dp, palette.line, HandDrawnRadiusAltShape)
                                        .clickable { viewModel.undoCapsuleDoodleStroke() }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    KannachIcon(icon = KannachIconType.UNDO, tint = palette.inkSoft, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = strings.undo, fontSize = 13.sp, color = palette.inkSoft)
                                }

                                Row(
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .clip(HandDrawnRadiusAltShape)
                                        .background(palette.paperHi)
                                        .border(1.2.dp, palette.line, HandDrawnRadiusAltShape)
                                        .clickable { viewModel.clearCapsuleDoodleStrokes() }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    KannachIcon(icon = KannachIconType.X, tint = palette.inkSoft, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = strings.clear, fontSize = 13.sp, color = palette.inkSoft)
                                }
                            }
                        }
                    }
                }
            }

            // LC1.7: Sticky footer above the keyboard (`imePadding()`):
            // Primary Seal button (green, 52 dp). While disabled, a one-line helper explains the missing piece.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wavyHeaderDivider(palette.line, waveHeightDp = 5.dp, isOnTop = true)
                    .background(palette.paper)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isSealEnabled && disabledHelperText.isNotEmpty()) {
                    Text(
                        text = disabledHelperText,
                        fontSize = 13.5.sp,
                        fontFamily = LocalKannachHandFont.current,
                        color = if (!hasBodyText) palette.inkSoft else palette.danger,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .testTag("capsule_seal_helper")
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .then(
                            if (isSealEnabled) {
                                Modifier.paperShadow(
                                    shadowColor = palette.paperShadow,
                                    shape = HandDrawnRadiusShape,
                                    offsetY = 3.dp
                                )
                            } else Modifier
                        )
                        .clip(HandDrawnRadiusShape)
                        .background(if (isSealEnabled) palette.green else palette.paperLo)
                        .border(
                            width = 1.6.dp,
                            color = if (isSealEnabled) palette.greenDeep else palette.line,
                            shape = HandDrawnRadiusShape
                        )
                        .semantics(mergeDescendants = true) {
                            contentDescription = strings.capsuleSaveBtn
                            role = Role.Button
                            if (!isSealEnabled) disabled()
                        }
                        .clickable(enabled = isSealEnabled) {
                            viewModel.requestSealCapsule()
                        }
                        .padding(vertical = 12.dp, horizontal = 16.dp)
                        .testTag("capsule_seal_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KannachIcon(
                            icon = KannachIconType.SEAL,
                            tint = if (isSealEnabled) palette.onGreen else palette.inkFaint,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.capsuleSaveBtn,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = LocalKannachHandFont.current,
                            color = if (isSealEnabled) palette.onGreen else palette.inkFaint
                        )
                    }
                }
            }
        }

        // LC2: Draft Exit Bottom Sheet ("Keep draft · Discard · Continue writing")
        AnimatedVisibility(
            visible = isBackSheetOpen,
            enter = fadeIn(tween(if (reduceMotion) 0 else KannachMotion.SOFT)) +
                slideInVertically(tween(if (reduceMotion) 0 else KannachMotion.SOFT)) { it / 2 },
            exit = fadeOut(tween(if (reduceMotion) 0 else KannachMotion.SOFT)) +
                slideOutVertically(tween(if (reduceMotion) 0 else KannachMotion.SOFT)) { it / 2 }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.scrim)
                    .clickable { viewModel.continueWritingCapsuleDraft() }
                    .testTag("capsule_draft_back_sheet"),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                        .clickable(enabled = false) {}
                        .paperShadow(palette.paperShadow, HandDrawnRadiusShape, offsetY = 6.dp)
                        .clip(HandDrawnRadiusShape)
                        .paperBackground(palette.paperHi)
                        .border(2.dp, palette.ink, HandDrawnRadiusShape)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = strings.capsuleDraftSheetTitle,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = LocalKannachHandFont.current,
                        color = palette.greenDeep,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Keep draft
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(HandDrawnRadiusShape)
                            .background(palette.green)
                            .border(1.5.dp, palette.greenDeep, HandDrawnRadiusShape)
                            .clickable { viewModel.keepCapsuleDraftAndClose() }
                            .padding(vertical = 12.dp, horizontal = 16.dp)
                            .testTag("capsule_draft_keep_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.capsuleDraftKeep,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = LocalKannachHandFont.current,
                            color = palette.onGreen
                        )
                    }

                    // Discard
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(HandDrawnRadiusShape)
                            .background(palette.dangerBg)
                            .border(1.5.dp, palette.dangerBorder, HandDrawnRadiusShape)
                            .clickable { viewModel.discardCapsuleDraftAndClose() }
                            .padding(vertical = 12.dp, horizontal = 16.dp)
                            .testTag("capsule_draft_discard_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.capsuleDraftDiscard,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = LocalKannachHandFont.current,
                            color = palette.danger
                        )
                    }

                    // Continue writing
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(HandDrawnRadiusShape)
                            .background(palette.paper)
                            .border(1.5.dp, palette.line, HandDrawnRadiusShape)
                            .clickable { viewModel.continueWritingCapsuleDraft() }
                            .padding(vertical = 12.dp, horizontal = 16.dp)
                            .testTag("capsule_draft_continue_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.capsuleDraftContinue,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = LocalKannachHandFont.current,
                            color = palette.ink
                        )
                    }
                }
            }
        }

        // LC3 & LC8: Seal Confirmation Sheet: envelope preview (first 28 chars of body + opens-on sentence),
        // `capsuleSealWarning`, "Not yet", and press-and-hold "Hold to seal" button.
        AnimatedVisibility(
            visible = isSealSheetOpen,
            enter = fadeIn(tween(if (reduceMotion) 0 else KannachMotion.SOFT)) +
                slideInVertically(tween(if (reduceMotion) 0 else KannachMotion.SOFT)) { it / 2 },
            exit = fadeOut(tween(if (reduceMotion) 0 else KannachMotion.SOFT)) +
                slideOutVertically(tween(if (reduceMotion) 0 else KannachMotion.SOFT)) { it / 2 }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.scrim)
                    .clickable { viewModel.closeCapsuleSealSheet() }
                    .testTag("capsule_seal_confirm_sheet"),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                        .clickable(enabled = false) {}
                        .paperShadow(palette.paperShadow, HandDrawnRadiusShape, offsetY = 6.dp)
                        .clip(HandDrawnRadiusShape)
                        .paperBackground(palette.paperHi)
                        .border(2.dp, palette.ink, HandDrawnRadiusShape)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Envelope preview card (first 28 chars of body + opens-on sentence + green sprout wax seal)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .paperTilt(-0.4f)
                            .paperShadow(palette.paperShadow, HandDrawnRadiusShape, offsetY = 3.dp)
                            .clip(HandDrawnRadiusShape)
                            .background(palette.paper)
                            .border(1.6.dp, palette.greenLine, HandDrawnRadiusShape)
                            .drawBehind {
                                val flapPath = Path().apply {
                                    moveTo(0f, 0f)
                                    lineTo(size.width * 0.5f, size.height * 0.42f)
                                    lineTo(size.width, 0f)
                                }
                                drawPath(
                                    path = flapPath,
                                    color = palette.ink.copy(alpha = 0.45f),
                                    style = Stroke(
                                        width = 1.2.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                                    )
                                )
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("capsule_seal_envelope_preview")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(palette.green)
                                    .border(1.5.dp, palette.greenDeep, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                KannachIcon(
                                    icon = KannachIconType.SPROUT,
                                    tint = palette.onGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                val previewTitle = initialTitle.trim()
                                    .ifEmpty { initialBody.replace("\n", " ").trim().take(28) }
                                    .ifEmpty { strings.capsuleSalutation }
                                Text(
                                    text = previewTitle,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = LocalKannachHandFont.current,
                                    color = palette.ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = opensOnSentence,
                                    fontSize = 14.sp,
                                    fontFamily = LocalKannachHandFont.current,
                                    color = palette.greenDeep
                                )
                            }
                        }
                    }

                    Text(
                        text = strings.capsuleSealWarning,
                        fontSize = 15.sp,
                        fontFamily = LocalKannachHandFont.current,
                        color = palette.inkSoft,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("capsule_seal_warning_text")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // "Not yet"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(HandDrawnRadiusShape)
                                .background(palette.paper)
                                .border(1.5.dp, palette.line, HandDrawnRadiusShape)
                                .clickable { viewModel.closeCapsuleSealSheet() }
                                .padding(vertical = 12.dp, horizontal = 12.dp)
                                .testTag("capsule_seal_not_yet_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.capsuleSealNotYet,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = LocalKannachHandFont.current,
                                color = palette.ink
                            )
                        }

                        // LC8: Press-and-hold "Hold to seal" button
                        HoldToSealButton(
                            label = strings.capsuleHoldToSeal,
                            palette = palette,
                            reduceMotion = reduceMotion,
                            onHoldConfirmed = {
                                viewModel.confirmSealCapsule(
                                    onConfirmedPermissionCheck = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            !viewModel.repository.isNotifPermissionAsked()
                                        ) {
                                            viewModel.repository.markNotifPermissionAsked()
                                            val granted = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.POST_NOTIFICATIONS
                                            ) == PackageManager.PERMISSION_GRANTED
                                            if (!granted) {
                                                try {
                                                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                } catch (_: Exception) {}
                                            }
                                        }
                                    }
                                )
                            },
                            modifier = Modifier
                                .weight(1.25f)
                                .testTag("capsule_seal_confirm_btn")
                        )
                    }
                }
            }
        }
    }
}

/**
 * LC7: Postage-stamp chip (64×76 dp, perforated scalloped Path, rotated postmark ink ring when selected).
 */
@Composable
private fun PostageStampChip(
    label: String,
    isSelected: Boolean,
    palette: KannachPalette,
    density: Density,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 64.dp, height = 76.dp)
            .paperShadow(
                shadowColor = palette.paperShadow,
                shape = PostageStampShape,
                offsetY = if (isSelected) 3.dp else 1.5.dp
            )
            .clip(PostageStampShape)
            .background(if (isSelected) palette.greenSoft else palette.paperHi)
            .drawWithContent {
                val stampPath = buildScallopedStampPath(size, density)
                drawPath(
                    path = stampPath,
                    color = if (isSelected) palette.greenDeep else palette.line,
                    style = Stroke(
                        width = (if (isSelected) 1.6.dp else 1.1.dp).toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
                drawContent()
                if (isSelected) {
                    // Rotated postmark ink ring over the selected stamp
                    withTransform({
                        rotate(degrees = -12f, pivot = center)
                    }) {
                        val ringRadius = size.minDimension * 0.40f
                        drawCircle(
                            color = palette.greenDeep.copy(alpha = 0.72f),
                            radius = ringRadius,
                            center = center,
                            style = Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f), 0f)
                            )
                        )
                        drawCircle(
                            color = palette.greenDeep.copy(alpha = 0.38f),
                            radius = ringRadius * 0.80f,
                            center = center,
                            style = Stroke(width = 0.9.dp.toPx())
                        )
                    }
                }
            }
            .semantics(mergeDescendants = true) {
                selected = isSelected
                role = Role.Tab
                contentDescription = label
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.5.sp,
            lineHeight = 15.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontFamily = LocalKannachHandFont.current,
            color = if (isSelected) palette.greenDeep else palette.ink,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * LC8: Press-and-hold green seal button inside the confirm sheet.
 * Fills progressively while held (~650 ms) and triggers `onHoldConfirmed` when full.
 */
@Composable
private fun HoldToSealButton(
    label: String,
    palette: KannachPalette,
    reduceMotion: Boolean,
    onHoldConfirmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val holdProgress = remember { Animatable(0f) }

    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(HandDrawnRadiusShape)
            .background(palette.green)
            .drawWithContent {
                val p = holdProgress.value.coerceIn(0f, 1f)
                if (p > 0f) {
                    drawRect(
                        color = palette.greenDeep.copy(alpha = 0.42f),
                        topLeft = Offset.Zero,
                        size = Size(size.width * p, size.height)
                    )
                }
                drawContent()
            }
            .border(1.6.dp, palette.greenDeep, HandDrawnRadiusShape)
            .semantics(mergeDescendants = true) {
                contentDescription = label
                role = Role.Button
                onClick(label = label) {
                    onHoldConfirmed()
                    true
                }
            }
            .pointerInput(reduceMotion) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    if (reduceMotion) {
                        val up = waitForUpOrCancellation()
                        if (up != null) {
                            onHoldConfirmed()
                        }
                    } else {
                        var completed = false
                        val holdJob = scope.launch {
                            holdProgress.snapTo(0f)
                            holdProgress.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(durationMillis = 650)
                            )
                            completed = true
                            onHoldConfirmed()
                        }
                        waitForUpOrCancellation()
                        if (!completed) {
                            holdJob.cancel()
                            scope.launch {
                                holdProgress.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(durationMillis = 180)
                                )
                            }
                        }
                    }
                }
            }
            .padding(vertical = 12.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KannachIcon(
                icon = KannachIconType.SPROUT,
                tint = palette.onGreen,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LocalKannachHandFont.current,
                color = palette.onGreen,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * LC6 (1–4) & LC5: Single sheet of lined letter paper:
 * 1. Lined paper (`linedPaperBackground`, line spacing = text line height 36 sp) filling the width with a 12 dp margin.
 * 2. First line, non-editable, hand font: salutation from LS1 (`capsuleSalutation`).
 * 3. Body below it, editable, 20 sp hand font, 36 sp line height, local state + 800 ms debounce.
 * 4. Last line, non-editable, bottom-end: `— {today’s date}` in the app language.
 */
@Composable
private fun CapsuleLetterSheet(
    sessionId: Int,
    salutation: String,
    initialBody: String,
    placeholder: String,
    talkBackLabel: String,
    todaySignature: String,
    palette: KannachPalette,
    onBodyChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var localBody by remember(sessionId) { mutableStateOf(initialBody) }
    val density = LocalDensity.current
    val lineSpacingDp = with(density) { 36.sp.toDp() }

    Column(
        modifier = modifier
            .paperShadow(
                shadowColor = palette.paperShadow,
                shape = HandDrawnRadiusShape,
                offsetY = 4.dp
            )
            .clip(HandDrawnRadiusShape)
            .paperBackground(palette.paperHi)
            .border(1.5.dp, palette.line, HandDrawnRadiusShape)
            .linedPaperBackground(palette.line, lineSpacingDp)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("capsule_letter_header")
    ) {
        // LC6.2: First line, non-editable, hand font: the salutation from LS1
        Text(
            text = salutation,
            fontSize = 20.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = LocalKannachHandFont.current,
            color = palette.greenDeep,
            modifier = Modifier.fillMaxWidth()
        )

        // LC6.3: Body below it, editable, 20 sp hand font, 36 sp line height
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 216.dp)
        ) {
            if (localBody.isEmpty()) {
                Text(
                    text = placeholder,
                    fontSize = 20.sp,
                    lineHeight = 36.sp,
                    fontFamily = LocalKannachHandFont.current,
                    color = palette.inkFaint
                )
            }
            BasicTextField(
                value = localBody,
                onValueChange = { newVal ->
                    localBody = newVal
                    onBodyChanged(newVal)
                },
                textStyle = TextStyle(
                    fontSize = 20.sp,
                    lineHeight = 36.sp,
                    fontFamily = LocalKannachHandFont.current,
                    color = palette.ink
                ),
                cursorBrush = SolidColor(palette.greenDeep),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 216.dp)
                    .semantics {
                        contentDescription = talkBackLabel
                    }
                    .testTag("capsule_compose_input")
            )
        }

        // LC6.4: Last line, non-editable, bottom-end: `— {today’s date}` in the app language
        Text(
            text = todaySignature,
            fontSize = 16.sp,
            lineHeight = 36.sp,
            fontFamily = LocalKannachHandFont.current,
            color = palette.inkSoft,
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.End)
                .testTag("capsule_written_stamp")
        )
    }
}

/**
 * CP8: Opened Letter View (Memory-card layout).
 * Displays letter text on ruled lines, optional doodle, relative distance copy,
 * and two quiet text actions: "Write back" (opens Compose) and "Delete" (confirm).
 */
@Composable
fun OpenedCapsuleOverlay(
    viewModel: MainViewModel
) {
    val activeCapsule by viewModel.openedCapsuleView.collectAsState()
    val palette = LocalKannachPalette.current
    val language by viewModel.language.collectAsState()
    val strings = I18n.forLanguage(language)
    val reduceMotion = rememberReduceMotion()

    AnimatedVisibility(
        visible = activeCapsule != null,
        enter = kannachCardEnter(reduceMotion),
        exit = kannachCardExit(reduceMotion)
    ) {
        val capsule = activeCapsule ?: return@AnimatedVisibility
        val writtenLong = remember(capsule.createdAt, language) {
            I18n.formatLongDateFromMillis(capsule.createdAt, language)
        }
        val distanceCopy = remember(capsule.createdAt, language) {
            I18n.getRelativeTimeDistanceFromMillis(capsule.createdAt, language)
        }
        val glyphColor = try {
            Color(android.graphics.Color.parseColor(capsule.color))
        } catch (_: Exception) {
            palette.green
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.scrim.copy(alpha = 0.65f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    viewModel.closeOpenedCapsuleView()
                }
                .testTag("opened_capsule_overlay"),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 440.dp)
                    .heightIn(max = 680.dp)
                    .clickable(enabled = false) {}
                    .shadow(24.dp, HandDrawnRadiusShape)
                    .clip(HandDrawnRadiusShape)
                    .background(palette.paper)
                    .border(2.dp, palette.ink, HandDrawnRadiusShape)
                    .paperBackground(palette.paper)
                    .padding(top = 22.dp, bottom = 18.dp, start = 18.dp, end = 18.dp)
            ) {
                // Washi tape at top center
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
                    // Header: Written date + relative distance line + Close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.capsuleWrittenStamp.replace("{d}", writtenLong),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.greenDeep
                            )
                            if (distanceCopy.isNotBlank()) {
                                Text(
                                    text = distanceCopy,
                                    fontSize = 14.sp,
                                    fontFamily = LocalKannachHandFont.current,
                                    color = palette.greenDeep,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .testTag("opened_capsule_distance")
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.closeOpenedCapsuleView() },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("opened_capsule_close_btn")
                        ) {
                            KannachIcon(
                                icon = KannachIconType.X,
                                tint = palette.inkSoft,
                                contentDescription = strings.dismiss,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Optional doodle if present (CP5 & CP8)
                    if (capsule.strokes.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 200.dp)
                                .aspectRatio(3f / 4f)
                                .paperTilt(capsule.id.hashCode())
                                .shadow(10.dp, HandDrawnRadiusShape)
                                .clip(HandDrawnRadiusShape)
                                .background(palette.paperHi)
                                .border(1.8.dp, palette.ink, HandDrawnRadiusShape)
                                .padding(14.dp)
                                .testTag("opened_capsule_doodle"),
                            contentAlignment = Alignment.Center
                        ) {
                            GlyphView(
                                strokes = capsule.strokes,
                                color = glyphColor,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Letter text on ruled lines (CP8)
                    val handSp = LocalHandwritingSp.current
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .linedPaperBackground(palette.line, (handSp + 12).dp)
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    ) {
                        Text(
                            text = capsule.text,
                            fontSize = handSp.sp,
                            lineHeight = (handSp + 12).sp,
                            fontFamily = LocalKannachHandFont.current,
                            color = palette.ink,
                            modifier = Modifier.testTag("opened_capsule_text")
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // CP8: Two quiet text actions: Write back (opens Compose) and Delete (confirm)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.requestDeleteCapsule(capsule.id) }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("capsule_opened_delete_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.delete,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.red
                            )
                        }

                        Box(
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clip(HandDrawnRadiusAltShape)
                                .background(palette.greenSoft)
                                .border(1.3.dp, palette.green, HandDrawnRadiusAltShape)
                                .clickable { viewModel.writeBackFromOpenedCapsule() }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .testTag("capsule_write_back_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.capsuleWriteBack,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = LocalKannachHandFont.current,
                                color = palette.greenDeep
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * CP7 (C2 in the emotional plan): Sealing Ceremony Overlay.
 * Plays over CEREMONY (900ms) when confirming "Seal the letter", folding the envelope
 * and stamping the wax seal before returning to the Letters shelf.
 */
@Composable
fun SealingCeremonyOverlay(
    viewModel: MainViewModel
) {
    val isActive by viewModel.isSealingCeremonyActive.collectAsState()
    if (!isActive) return

    val palette = LocalKannachPalette.current
    val reduceMotion = rememberReduceMotion()
    val progress = remember { Animatable(0f) }

    LaunchedEffect(isActive) {
        if (reduceMotion) {
            viewModel.completeSealingCeremony()
        } else {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = KannachMotion.CEREMONY,
                    easing = KannachMotion.Settle
                )
            )
            viewModel.completeSealingCeremony()
        }
    }

    val p = progress.value
    val sealScale = if (p < 0.45f) {
        1.45f - (p / 0.45f) * 0.45f
    } else {
        1.0f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.scrim.copy(alpha = 0.55f * (1f - p * 0.3f)))
            .testTag("sealing_ceremony_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(240.dp)
                .height(160.dp)
                .paperTilt(-0.5f)
                .paperShadow(
                    shadowColor = palette.paperShadow,
                    shape = HandDrawnRadiusShape,
                    offsetY = 6.dp
                )
                .clip(HandDrawnRadiusShape)
                .background(palette.paperHi)
                .border(2.dp, palette.ink, HandDrawnRadiusShape)
                .drawBehind {
                    val flapProgress = (p * 1.6f).coerceIn(0f, 1f)
                    val flapPath = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width * 0.5f, size.height * 0.54f * flapProgress)
                        lineTo(size.width, 0f)
                        close()
                    }
                    drawPath(
                        path = flapPath,
                        color = palette.paperLo
                    )
                    drawPath(
                        path = flapPath,
                        color = palette.ink,
                        style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .scale(sealScale)
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(palette.green)
                    .border(2.dp, palette.greenDeep, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                KannachIcon(
                    icon = KannachIconType.SPROUT,
                    tint = palette.onGreen,
                    strokeWidthDp = 2.dp,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
