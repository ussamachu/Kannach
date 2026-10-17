package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val language by viewModel.language.collectAsState()
            val currentMonth by viewModel.currentMonth.collectAsState()

            KannachTheme(language = language, month = currentMonth) {
                KannachApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun KannachApp(viewModel: MainViewModel) {
    val language by viewModel.language.collectAsState()
    val strings = I18n.forLanguage(language)
    val palette = LocalKannachPalette.current

    var showSplash by remember { mutableStateOf(true) }

    // Screen visibility states for back handler routing
    val confirmDialog by viewModel.confirmDialog.collectAsState()
    val isWrapOpen by viewModel.isWrapOpen.collectAsState()
    val isSketchOpen by viewModel.isSketchPageOpen.collectAsState()
    val isCapsuleOpen by viewModel.isCapsulePageOpen.collectAsState()
    val isGalleryOpen by viewModel.isGalleryOpen.collectAsState()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val activeSheetDate by viewModel.activeSheetDate.collectAsState()
    val isFirstRun by viewModel.isFirstRun.collectAsState()

    // Hierarchical BackHandler strictly ported from HTML keyboard / ESC handling
    BackHandler(enabled = confirmDialog != null || isWrapOpen || isSketchOpen || isCapsuleOpen || isGalleryOpen || isSettingsOpen || activeSheetDate != null || isFirstRun) {
        when {
            confirmDialog != null -> viewModel.dismissConfirmDialog()
            isWrapOpen -> viewModel.closeWrap()
            isSketchOpen -> viewModel.closeSketchPage()
            isCapsuleOpen -> viewModel.closeCapsulePage()
            isGalleryOpen -> viewModel.closeGallery()
            isSettingsOpen -> viewModel.closeSettings()
            isFirstRun -> viewModel.completeFirstRun()
            activeSheetDate != null -> {
                if (viewModel.sheetMode.value == SheetMode.EDIT && viewModel.writeStep.value == 2) {
                    viewModel.setWriteStep(1)
                } else {
                    viewModel.requestCloseSheet()
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = palette.paper
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .paperBackground(palette.paper)
        ) {
            // Main Home Screen Structure
            Column(modifier = Modifier.fillMaxSize()) {
                AppHeader(viewModel = viewModel)
                MonthGrid(viewModel = viewModel, modifier = Modifier.weight(1f))
            }

            // Overlays & Sub-Screens (D1, G1, S1, H11: floating screens use real current month season)
            val nowMonth = remember { java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1 }
            KannachTheme(language = language, month = nowMonth) {
                AnimatedVisibility(
                    visible = isGalleryOpen,
                    enter = kannachSubScreenEnter(rememberReduceMotion()),
                    exit = kannachSubScreenExit(rememberReduceMotion())
                ) {
                    GalleryScreen(viewModel = viewModel)
                }

                AnimatedVisibility(
                    visible = isWrapOpen,
                    enter = kannachSubScreenEnter(rememberReduceMotion()),
                    exit = kannachSubScreenExit(rememberReduceMotion())
                ) {
                    YearWrapScreen(viewModel = viewModel)
                }

                AnimatedVisibility(
                    visible = isCapsuleOpen,
                    enter = kannachSubScreenEnter(rememberReduceMotion()),
                    exit = kannachSubScreenExit(rememberReduceMotion())
                ) {
                    TimeCapsuleScreen(viewModel = viewModel)
                }

                AnimatedVisibility(
                    visible = isSketchOpen,
                    enter = kannachSubScreenEnter(rememberReduceMotion()),
                    exit = kannachSubScreenExit(rememberReduceMotion())
                ) {
                    SketchpadScreen(viewModel = viewModel)
                }

                AnimatedVisibility(
                    visible = isSettingsOpen,
                    enter = kannachSubScreenEnter(rememberReduceMotion()),
                    exit = kannachSubScreenExit(rememberReduceMotion())
                ) {
                    SettingsScreen(viewModel = viewModel)
                }

                // Entry Read/Write Sheet
                EntrySheetOverlay(viewModel = viewModel)

                // Planting Ceremony Overlay (W3)
                PlantingCeremonyOverlay(viewModel = viewModel)

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

            // Animated Splash Screen on App Launch (R1)
            if (showSplash) {
                SplashScreen(
                    appName = strings.appTitle,
                    onFinished = { showSplash = false }
                )
            }
        }
    }
}
