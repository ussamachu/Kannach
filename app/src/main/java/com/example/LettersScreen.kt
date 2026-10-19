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
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
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

            // CP1: Top paper card "Write a letter to future you" (tap -> Compose)
            item(key = "capsule_compose_card", contentType = "compose_action") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                        .heightIn(min = 76.dp)
                        .paperTilt(-0.4f)
                        .paperShadow(
                            shadowColor = palette.paperShadow,
                            shape = HandDrawnRadiusShape,
                            offsetY = 4.dp
                        )
                        .clip(HandDrawnRadiusShape)
                        .background(palette.paperHi)
                        .border(1.5.dp, palette.greenLine, HandDrawnRadiusShape)
                        .dashedHandDrawnBorder(
                            color = palette.greenLine.copy(alpha = 0.65f),
                            shape = HandDrawnRadiusShape,
                            strokeWidth = 1.2.dp,
                            dashIntervals = floatArrayOf(8f, 6f),
                            inset = 4.dp
                        )
                        .clickable { viewModel.openCapsuleCompose() }
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                        .testTag("capsule_compose_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.capsuleBtn,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = LocalKannachHandFont.current,
                                color = palette.greenDeep
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = strings.capsuleIntro,
                                fontSize = 13.5.sp,
                                lineHeight = 19.sp,
                                color = palette.inkSoft
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(palette.greenSoft)
                                .border(1.3.dp, palette.green, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            KannachIcon(
                                icon = KannachIconType.SEAL,
                                tint = palette.greenDeep,
                                strokeWidthDp = 2.dp,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
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
                // Section 1: Waiting (locked)
                if (waitingCapsules.isNotEmpty()) {
                    item(key = "capsule_section_header_waiting", contentType = "section_header") {
                        Box(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 4.dp)) {
                            ShelfSectionHeader(
                                title = strings.capsuleSectionWaiting,
                                count = waitingCapsules.size,
                                testTag = "capsule_section_waiting"
                            )
                        }
                    }
                    items(
                        items = waitingCapsules,
                        key = { "waiting_${it.id}" },
                        contentType = { "waiting_envelope" }
                    ) { capsule ->
                        val idx = waitingCapsules.indexOf(capsule)
                        val longUnlockDate = I18n.formatLongDate(capsule.unlockDate, language)
                        val daysLeft = I18n.daysUntilDateKey(capsule.unlockDate, todayIso)
                        val subtitle = "${strings.capsuleLocked.replace("{d}", longUnlockDate)} · ${strings.capsuleCountdown.format(language, daysLeft)}"

                        Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                            EnvelopeCard(
                                capsule = capsule,
                                state = EnvelopeState.WAITING,
                                statusText = subtitle,
                                previewText = null,
                                isHighlighted = highlightedId == capsule.id,
                                reduceMotion = reduceMotion,
                                tiltIndex = idx,
                                deleteContentDescription = strings.delete,
                                onTap = { viewModel.onLockedCapsuleTap(capsule) },
                                onDelete = { viewModel.requestDeleteCapsule(capsule.id) }
                            )
                        }
                    }
                }

                // Section 2: Ready (due, with seal-crack glow)
                if (readyCapsules.isNotEmpty()) {
                    item(key = "capsule_section_header_ready", contentType = "section_header") {
                        Box(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 4.dp)) {
                            ShelfSectionHeader(
                                title = strings.capsuleSectionReady,
                                count = readyCapsules.size,
                                testTag = "capsule_section_ready"
                            )
                        }
                    }
                    items(
                        items = readyCapsules,
                        key = { "ready_${it.id}" },
                        contentType = { "ready_envelope" }
                    ) { capsule ->
                        val idx = readyCapsules.indexOf(capsule)
                        Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                            EnvelopeCard(
                                capsule = capsule,
                                state = EnvelopeState.READY,
                                statusText = strings.capsuleReady,
                                previewText = null,
                                isHighlighted = highlightedId == capsule.id,
                                reduceMotion = reduceMotion,
                                tiltIndex = idx + 3,
                                deleteContentDescription = strings.delete,
                                onTap = { viewModel.openReadyCapsule(capsule) },
                                onDelete = { viewModel.requestDeleteCapsule(capsule.id) }
                            )
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
                                tiltIndex = idx + 6,
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

/**
 * CP1, CP2, CP3: Hand-drawn paper Envelope Card with envelope flap lines, wax seal icon
 * (with seal-crack glow when READY), 1s highlight when newly sealed, and long-press / button delete.
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
        else -> palette.line
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

    // Seal-crack glow animation for READY state (CP1)
    val infiniteTransition = rememberInfiniteTransition(label = "readySealGlow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = if (state == EnvelopeState.READY && !reduceMotion) 0.85f else 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = KannachMotion.Breathe),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sealGlowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .paperTilt(tiltIndex)
            .paperShadow(
                shadowColor = palette.paperShadow,
                shape = HandDrawnRadiusShape,
                offsetY = 3.dp
            )
            .clip(HandDrawnRadiusShape)
            .background(bgColor)
            .border(
                width = if (isHighlighted || state == EnvelopeState.READY) 1.8.dp else 1.5.dp,
                color = borderColor,
                shape = HandDrawnRadiusShape
            )
            .drawBehind {
                // Subtle triangular envelope flap lines across the top
                val flapPath = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width * 0.5f, size.height * 0.42f)
                    lineTo(size.width, 0f)
                }
                drawPath(
                    path = flapPath,
                    color = palette.line.copy(alpha = 0.55f),
                    style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
            .combinedClickable(
                onClick = onTap,
                onLongClick = onDelete
            )
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp)
            .testTag("capsule_card_${capsule.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Wax seal icon with seal-crack glow for READY
            Box(
                modifier = Modifier.size(42.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state == EnvelopeState.READY) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = palette.orange.copy(alpha = glowPulse * 0.45f),
                            radius = size.minDimension * 0.5f
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            when (state) {
                                EnvelopeState.READY -> palette.red.copy(alpha = 0.18f)
                                EnvelopeState.WAITING -> palette.paperLo
                                EnvelopeState.OPENED -> palette.paper
                            }
                        )
                        .border(
                            width = 1.2.dp,
                            color = when (state) {
                                EnvelopeState.READY -> palette.red
                                EnvelopeState.WAITING -> palette.greenLine
                                EnvelopeState.OPENED -> palette.line
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    KannachIcon(
                        icon = KannachIconType.SEAL,
                        tint = when (state) {
                            EnvelopeState.READY -> palette.red
                            EnvelopeState.WAITING -> palette.greenDeep
                            EnvelopeState.OPENED -> palette.inkFaint
                        },
                        strokeWidthDp = 1.8.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    // Crack line across the wax seal when READY (CP1)
                    if (state == EnvelopeState.READY) {
                        Canvas(modifier = Modifier.size(18.dp)) {
                            val crack = Path().apply {
                                moveTo(size.width * 0.35f, size.height * 0.15f)
                                lineTo(size.width * 0.55f, size.height * 0.48f)
                                lineTo(size.width * 0.42f, size.height * 0.62f)
                                lineTo(size.width * 0.65f, size.height * 0.88f)
                            }
                            drawPath(
                                path = crack,
                                color = palette.paperHi,
                                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = statusText,
                    fontSize = 14.5.sp,
                    fontWeight = if (state == EnvelopeState.READY) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (state == EnvelopeState.READY) palette.greenDeep else palette.ink,
                    modifier = Modifier.testTag("capsule_status_${capsule.id}")
                )
                if (!previewText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = previewText,
                        fontSize = 15.sp,
                        fontFamily = LocalKannachHandFont.current,
                        color = palette.inkSoft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Quiet 48dp delete icon button (CP3)
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(48.dp)
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
}

/**
 * Phase 5 — Letters compose (LC1–LC5):
 * LC1 — Order of the page, top to bottom:
 *   1. Top bar: back, title `capsuleComposeTitle`, and a quiet draft status (`capsuleDraftSaved` / `capsuleDraftSaving`).
 *   2. Letter header on paper: "To: me" line (`capsuleToMe`) and today's date, formatted in the app language.
 *   3. When to open — segmented row of quick choices: Tomorrow · One week · One month · Six months · One year · Pick a date.
 *      Below it, one plain sentence with the result: "Opens on Sunday, 18 October 2027" (localized, with the weekday).
 *      If the picked date is not in the future, show the sentence in `palette.danger` and block sealing.
 *   4. Optional title (single line, hint `capsuleTitleHint`).
 *   5. Body: large lined paper, hand font, 20 sp, line height 36 sp, `linedPaperBackground` line spacing equal to 36 sp.
 *      Minimum height 220 dp, grows with text.
 *   6. Optional attachments row (existing "add drawing" action).
 *   7. Sticky footer above the keyboard (`imePadding()`): primary Seal button (green, 52 dp).
 *      While disabled, a one-line helper explains the missing piece ("Write something first" / "Choose when it opens").
 * LC2 — Draft safety: 800 ms debounced autosave to `capsule_draft` in SharedPreferences; back sheet with Keep draft · Discard · Continue writing; sealing clears draft.
 * LC3 — Seal confirmation: one-step confirm sheet with envelope preview (title + opens-on sentence), `capsuleSealWarning`, and "Seal" / "Not yet".
 * LC4 — Accessibility: Targets ≥ 48 dp; quick-choice chips have `selected` semantics; every input has a TalkBack label; font scale 1.3 without clipping.
 * LC5 — Performance: Body field holds text in its own state and hoists only on 800 ms debounce.
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

            // Scrollable middle area containing LC1 items 2, 3, 4, 5, 6
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // LC1.2: Letter header on paper: "To: me" line (`capsuleToMe`) and today's date, formatted in the app language
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(HandDrawnRadiusAltShape)
                        .background(palette.paperHi)
                        .border(1.2.dp, palette.line, HandDrawnRadiusAltShape)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("capsule_letter_header"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.capsuleToMe,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = LocalKannachHandFont.current,
                        color = palette.greenDeep
                    )
                    Text(
                        text = todayFormatted,
                        fontSize = 14.sp,
                        fontFamily = LocalKannachHandFont.current,
                        color = palette.inkSoft,
                        modifier = Modifier.testTag("capsule_written_stamp")
                    )
                }

                // LC1.3: When to open — segmented row of quick choices:
                // Tomorrow · One week · One month · Six months · One year · Pick a date
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = strings.capsuleDateLabel,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.greenDeep
                    )

                    val presets = listOf(
                        CapsulePreset.TOMORROW to strings.capsulePresetTomorrow,
                        CapsulePreset.WEEK_1 to strings.capsulePreset1w,
                        CapsulePreset.MONTH_1 to strings.capsulePreset1m,
                        CapsulePreset.MONTH_6 to strings.capsulePreset6m,
                        CapsulePreset.YEAR_1 to strings.capsulePreset1y
                    )

                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(HandDrawnRadiusShape)
                            .background(palette.paperLo.copy(alpha = 0.55f))
                            .border(1.2.dp, palette.line, HandDrawnRadiusShape)
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { (preset, label) ->
                            val isSelected = selectedPreset == preset
                            Box(
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .clip(HandDrawnRadiusAltShape)
                                    .background(if (isSelected) palette.greenSoft else palette.paperHi)
                                    .border(
                                        width = if (isSelected) 1.6.dp else 1.dp,
                                        color = if (isSelected) palette.greenDeep else palette.line,
                                        shape = HandDrawnRadiusAltShape
                                    )
                                    .semantics(mergeDescendants = true) {
                                        selected = isSelected
                                        role = Role.Tab
                                        contentDescription = label
                                    }
                                    .clickable { viewModel.selectCapsulePreset(preset) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("capsule_preset_${preset.name.lowercase(Locale.ROOT)}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) palette.greenDeep else palette.ink
                                )
                            }
                        }

                        // "Pick a date" quick choice chip
                        val isCustom = selectedPreset == CapsulePreset.CUSTOM
                        Box(
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clip(HandDrawnRadiusAltShape)
                                .background(if (isCustom) palette.greenSoft else palette.paperHi)
                                .border(
                                    width = if (isCustom) 1.6.dp else 1.dp,
                                    color = if (isCustom) palette.greenDeep else palette.line,
                                    shape = HandDrawnRadiusAltShape
                                )
                                .semantics(mergeDescendants = true) {
                                    selected = isCustom
                                    role = Role.Tab
                                    contentDescription = strings.capsulePickDate
                                }
                                .clickable {
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
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("capsule_preset_custom"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.capsulePickDate,
                                fontSize = 13.5.sp,
                                fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCustom) palette.greenDeep else palette.ink
                            )
                        }
                    }

                    // LC1.3: One plain sentence with the result: "Opens on Sunday, 18 October 2027"
                    // If the picked date is not in the future, show the sentence in `palette.danger` and block sealing.
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

                // LC1.4: Optional title (single line, hint `capsuleTitleHint`)
                CapsuleTitleInputField(
                    sessionId = sessionId,
                    initialTitle = initialTitle,
                    hint = strings.capsuleTitleHint,
                    palette = palette,
                    onTitleChanged = { viewModel.onCapsuleTitleInputChanged(it) }
                )

                // LC1.5 & LC5: Body: large lined paper, hand font, 20 sp, line height 36 sp,
                // linedPaperBackground line spacing equal to 36 sp, minimum height 220 dp, grows with text.
                // Holds text in its own local state and hoists only on 800 ms debounce.
                CapsuleBodyPaperField(
                    sessionId = sessionId,
                    initialBody = initialBody,
                    placeholder = strings.capsulePlaceholder,
                    talkBackLabel = strings.capsuleWriteLabel,
                    palette = palette,
                    onBodyChanged = { viewModel.onCapsuleBodyInputChanged(it) }
                )

                // LC1.6: Optional attachments row (existing "add drawing" action)
                Column(modifier = Modifier.fillMaxWidth()) {
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

        // LC3: Seal Confirmation Sheet: envelope preview (title + opens-on sentence),
        // `capsuleSealWarning`, and "Seal" / "Not yet".
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
                    // Envelope preview card (title + opens-on sentence)
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
                                    color = palette.line.copy(alpha = 0.6f),
                                    style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
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
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(palette.red.copy(alpha = 0.16f))
                                    .border(1.4.dp, palette.red, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                KannachIcon(
                                    icon = KannachIconType.SEAL,
                                    tint = palette.red,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                val previewTitle = initialTitle.trim().ifEmpty { strings.capsuleToMe }
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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

                        // "Seal"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(HandDrawnRadiusShape)
                                .background(palette.green)
                                .border(1.5.dp, palette.greenDeep, HandDrawnRadiusShape)
                                .clickable {
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
                                }
                                .padding(vertical = 12.dp, horizontal = 12.dp)
                                .testTag("capsule_seal_confirm_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                KannachIcon(
                                    icon = KannachIconType.SEAL,
                                    tint = palette.onGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.capsuleSealNow,
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = LocalKannachHandFont.current,
                                    color = palette.onGreen
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
 * LC1.4, LC4 & LC5: Single-line optional title field holding its own local state
 * so typing does not recompose the parent screen.
 */
@Composable
private fun CapsuleTitleInputField(
    sessionId: Int,
    initialTitle: String,
    hint: String,
    palette: KannachPalette,
    onTitleChanged: (String) -> Unit
) {
    var localTitle by remember(sessionId) { mutableStateOf(initialTitle) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(HandDrawnRadiusAltShape)
            .background(palette.paperHi)
            .border(1.3.dp, palette.line, HandDrawnRadiusAltShape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (localTitle.isEmpty()) {
            Text(
                text = hint,
                fontSize = 18.sp,
                fontFamily = LocalKannachHandFont.current,
                color = palette.inkFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        BasicTextField(
            value = localTitle,
            onValueChange = { newVal ->
                val singleLineVal = newVal.replace("\n", " ")
                localTitle = singleLineVal
                onTitleChanged(singleLineVal)
            },
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = LocalKannachHandFont.current,
                color = palette.ink
            ),
            cursorBrush = SolidColor(palette.greenDeep),
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = hint
                }
                .testTag("capsule_title_input")
        )
    }
}

/**
 * LC1.5, LC4 & LC5: Large lined paper body field:
 * - Hand font, 20 sp, line height exactly 36 sp
 * - `linedPaperBackground` line spacing equal to 36 sp so text sits on the lines
 * - Minimum height 220 dp, grows with text
 * - Holds text in its own state (`localBody`) so key presses never recompose the whole screen
 */
@Composable
private fun CapsuleBodyPaperField(
    sessionId: Int,
    initialBody: String,
    placeholder: String,
    talkBackLabel: String,
    palette: KannachPalette,
    onBodyChanged: (String) -> Unit
) {
    var localBody by remember(sessionId) { mutableStateOf(initialBody) }
    val density = LocalDensity.current
    val lineSpacingDp = with(density) { 36.sp.toDp() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp)
            .paperShadow(
                shadowColor = palette.paperShadow,
                shape = HandDrawnRadiusShape,
                offsetY = 4.dp
            )
            .clip(HandDrawnRadiusShape)
            .background(palette.paperHi)
            .border(1.5.dp, palette.line, HandDrawnRadiusShape)
            .linedPaperBackground(palette.line, lineSpacingDp)
            .padding(horizontal = 14.dp, vertical = 4.dp)
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
                .heightIn(min = 212.dp)
                .semantics {
                    contentDescription = talkBackLabel
                }
                .testTag("capsule_compose_input")
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
                    .background(palette.red)
                    .border(2.dp, palette.dangerBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                KannachIcon(
                    icon = KannachIconType.SEAL,
                    tint = palette.onGreen,
                    strokeWidthDp = 2.dp,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
