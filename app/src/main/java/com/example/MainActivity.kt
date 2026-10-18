package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val language by viewModel.language.collectAsState()
            val currentMonth by viewModel.currentMonth.collectAsState()
            val userSettings by viewModel.userSettings.collectAsState()

            androidx.compose.runtime.LaunchedEffect(userSettings.hideInRecents) {
                if (userSettings.hideInRecents) {
                    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            KannachTheme(
                language = language,
                month = currentMonth,
                seasonOverride = userSettings.seasonOverride,
                reduceMotionOverride = userSettings.reduceMotion == "on",
                hapticsEnabled = userSettings.hapticsEnabled,
                handwritingSp = userSettings.handwritingSp
            ) {
                KannachApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KannachApp(viewModel: MainViewModel) {
    val language by viewModel.language.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val strings = I18n.forLanguage(language)
    val isRtl = I18n.isRtl(language)
    val palette = LocalKannachPalette.current
    val reduceMotion = rememberReduceMotion()
    val density = LocalDensity.current
    val context = androidx.compose.ui.platform.LocalContext.current

    androidx.compose.runtime.LaunchedEffect(userSettings.hideInRecents) {
        val activity = context as? android.app.Activity
        if (activity != null) {
            if (userSettings.hideInRecents) {
                activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    var showSplash by remember { mutableStateOf(true) }

    // Navigation & Overlay states (NV1, NV2, SK7, CP4, CP7, CP8)
    val currentTab by viewModel.currentTab.collectAsState()
    val confirmDialog by viewModel.confirmDialog.collectAsState()
    val isWrapOpen by viewModel.isWrapOpen.collectAsState()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val isExportSheetOpen by viewModel.isExportSheetOpen.collectAsState()
    val isCapsuleComposeOpen by viewModel.isCapsuleComposeOpen.collectAsState()
    val openedCapsuleView by viewModel.openedCapsuleView.collectAsState()
    val isSealingCeremonyActive by viewModel.isSealingCeremonyActive.collectAsState()
    val activeSheetDate by viewModel.activeSheetDate.collectAsState()
    val isFirstRun by viewModel.isFirstRun.collectAsState()

    // NV2 & RB4: Hierarchical BackHandler: close topmost overlay first; from non-PAGE tab go to PAGE
    BackHandler(
        enabled = confirmDialog != null || openedCapsuleView != null || isCapsuleComposeOpen ||
                isExportSheetOpen || activeSheetDate != null || isWrapOpen ||
                isSettingsOpen || isFirstRun || currentTab != Tab.PAGE
    ) {
        when {
            confirmDialog != null -> viewModel.dismissConfirmDialog()
            openedCapsuleView != null -> viewModel.closeOpenedCapsuleView()
            isCapsuleComposeOpen -> viewModel.closeCapsuleCompose()
            isExportSheetOpen -> viewModel.closeExportSheet()
            activeSheetDate != null -> {
                if (viewModel.sheetMode.value == SheetMode.EDIT && viewModel.writeStep.value == 2) {
                    viewModel.setWriteStep(1)
                } else {
                    viewModel.requestCloseSheet()
                }
            }
            isWrapOpen -> viewModel.closeWrap()
            isSettingsOpen -> viewModel.closeSettings()
            isFirstRun -> viewModel.completeFirstRun()
            currentTab != Tab.PAGE -> viewModel.selectTab(Tab.PAGE)
        }
    }

    val nowMonth = remember { java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1 }
    val tabSlidePx = remember(density) { with(density) { 8.dp.roundToPx() } }
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0

    @Suppress("UnusedMaterial3ScaffoldPaddingParameter")
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = palette.paper
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .paperBackground(palette.paper)
        ) {
            // NV1 & NV4: Top-level tabs with crossfade + 8 dp directional slide (mirrored in RTL), SOFT
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    if (reduceMotion) {
                        fadeIn(animationSpec = tween(KannachMotion.INSTANT)) togetherWith
                            fadeOut(animationSpec = tween(KannachMotion.INSTANT))
                    } else {
                        val isForward = targetState.ordinal > initialState.ordinal
                        val sign = if (isForward) {
                            if (isRtl) -1 else 1
                        } else {
                            if (isRtl) 1 else -1
                        }
                        val enterOffset = tabSlidePx * sign
                        val exitOffset = -tabSlidePx * sign

                        (fadeIn(animationSpec = tween(KannachMotion.SOFT, easing = KannachMotion.Settle)) +
                            slideInHorizontally(
                                animationSpec = tween(KannachMotion.SOFT, easing = KannachMotion.Settle),
                                initialOffsetX = { enterOffset }
                            )) togetherWith
                            (fadeOut(animationSpec = tween(KannachMotion.SOFT, easing = KannachMotion.Settle)) +
                                slideOutHorizontally(
                                    animationSpec = tween(KannachMotion.SOFT, easing = KannachMotion.Settle),
                                    targetOffsetX = { exitOffset }
                                ))
                    }
                },
                label = "KannachTabTransition",
                modifier = Modifier.fillMaxSize()
            ) { tab ->
                when (tab) {
                    Tab.PAGE -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            AppHeader(viewModel = viewModel)
                            MonthGrid(viewModel = viewModel, modifier = Modifier.weight(1f))
                        }
                    }
                    Tab.GARDEN -> {
                        KannachTheme(
                            language = language,
                            month = nowMonth,
                            seasonOverride = userSettings.seasonOverride,
                            reduceMotionOverride = userSettings.reduceMotion == "on",
                            hapticsEnabled = userSettings.hapticsEnabled,
                            handwritingSp = userSettings.handwritingSp
                        ) {
                            GalleryScreen(viewModel = viewModel)
                        }
                    }
                    Tab.LETTERS -> {
                        KannachTheme(
                            language = language,
                            month = nowMonth,
                            seasonOverride = userSettings.seasonOverride,
                            reduceMotionOverride = userSettings.reduceMotion == "on",
                            hapticsEnabled = userSettings.hapticsEnabled,
                            handwritingSp = userSettings.handwritingSp
                        ) {
                            TimeCapsuleScreen(viewModel = viewModel)
                        }
                    }
                    Tab.DRAW -> {
                        KannachTheme(
                            language = language,
                            month = nowMonth,
                            seasonOverride = userSettings.seasonOverride,
                            reduceMotionOverride = userSettings.reduceMotion == "on",
                            hapticsEnabled = userSettings.hapticsEnabled,
                            handwritingSp = userSettings.handwritingSp
                        ) {
                            SketchpadScreen(viewModel = viewModel)
                        }
                    }
                }
            }

            // NV3: Paper Dock stays mounted on all four tabs, above their content.
            // Hidden (slide down + fade, SOFT) while write/memory overlay, Settings, Year Wrap,
            // export sheet, letter compose/opened/ceremony, onboarding, splash, or keyboard (IME) is visible.
            val isDockVisible = !showSplash && !isFirstRun && activeSheetDate == null &&
                    !isSettingsOpen && !isWrapOpen && !isExportSheetOpen &&
                    !isCapsuleComposeOpen && openedCapsuleView == null && !isSealingCeremonyActive &&
                    confirmDialog == null && !isImeVisible

            AnimatedVisibility(
                visible = isDockVisible,
                enter = if (reduceMotion) {
                    fadeIn(animationSpec = tween(KannachMotion.INSTANT))
                } else {
                    fadeIn(animationSpec = tween(KannachMotion.SOFT, easing = KannachMotion.Settle)) +
                        slideInVertically(
                            animationSpec = tween(KannachMotion.SOFT, easing = KannachMotion.Settle),
                            initialOffsetY = { it }
                        )
                },
                exit = if (reduceMotion) {
                    fadeOut(animationSpec = tween(KannachMotion.INSTANT))
                } else {
                    fadeOut(animationSpec = tween(KannachMotion.SOFT, easing = KannachMotion.Settle)) +
                        slideOutVertically(
                            animationSpec = tween(KannachMotion.SOFT, easing = KannachMotion.Settle),
                            targetOffsetY = { it }
                        )
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                KannachDock(viewModel = viewModel)
            }

            // Overlays (Year Wrap, Settings, Entry Sheet, Planting Ceremony, Opened Letter, Sealing Ceremony, Confirm & Toast)
            KannachTheme(
                language = language,
                month = nowMonth,
                seasonOverride = userSettings.seasonOverride,
                reduceMotionOverride = userSettings.reduceMotion == "on",
                hapticsEnabled = userSettings.hapticsEnabled,
                handwritingSp = userSettings.handwritingSp
            ) {
                AnimatedVisibility(
                    visible = isWrapOpen,
                    enter = kannachSubScreenEnter(reduceMotion),
                    exit = kannachSubScreenExit(reduceMotion)
                ) {
                    YearWrapScreen(viewModel = viewModel)
                }

                AnimatedVisibility(
                    visible = isSettingsOpen,
                    enter = kannachSubScreenEnter(reduceMotion),
                    exit = kannachSubScreenExit(reduceMotion)
                ) {
                    SettingsScreen(viewModel = viewModel)
                }

                // Entry Read/Write Sheet
                EntrySheetOverlay(viewModel = viewModel)

                // Planting Ceremony Overlay (W3)
                PlantingCeremonyOverlay(viewModel = viewModel)

                // Opened Letter View Overlay (CP8)
                OpenedCapsuleOverlay(viewModel = viewModel)

                // Sealing Ceremony Overlay (CP7 / C2)
                SealingCeremonyOverlay(viewModel = viewModel)

                // Confirm & Toast Modals
                ConfirmDialogOverlay(viewModel = viewModel)
                ToastOverlay(
                    viewModel = viewModel,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // First Run Onboarding (R2)
            if (!showSplash && isFirstRun) {
                FirstRunOnboarding(
                    strings = strings,
                    onFinish = { viewModel.completeFirstRun() }
                )
            }

            // Animated Splash Screen on App Launch (R1, BR2)
            if (showSplash) {
                SplashScreen(
                    appName = strings.appTitle,
                    appSub = strings.appSub,
                    onFinished = { showSplash = false }
                )
            }
        }
    }
}
