package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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

    // Hierarchical BackHandler strictly ported from HTML keyboard / ESC handling
    BackHandler(enabled = confirmDialog != null || isWrapOpen || isSketchOpen || isCapsuleOpen || isGalleryOpen || isSettingsOpen || activeSheetDate != null) {
        when {
            confirmDialog != null -> viewModel.dismissConfirmDialog()
            isWrapOpen -> viewModel.closeWrap()
            isSketchOpen -> viewModel.closeSketchPage()
            isCapsuleOpen -> viewModel.closeCapsulePage()
            isGalleryOpen -> viewModel.closeGallery()
            isSettingsOpen -> viewModel.closeSettings()
            activeSheetDate != null -> viewModel.requestCloseSheet()
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

            // Floating Bottom Filter Bar
            BottomFilterBar(
                viewModel = viewModel,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // Overlays & Sub-Screens
            GalleryScreen(viewModel = viewModel)
            YearWrapScreen(viewModel = viewModel)
            TimeCapsuleScreen(viewModel = viewModel)
            SketchpadScreen(viewModel = viewModel)
            SettingsScreen(viewModel = viewModel)

            // Entry Read/Write Sheet
            EntrySheetOverlay(viewModel = viewModel)

            // Confirm & Toast Modals
            ConfirmDialogOverlay(viewModel = viewModel)
            ToastOverlay(
                viewModel = viewModel,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // Animated Splash Screen on App Launch
            if (showSplash) {
                SplashScreen(
                    appName = strings.appTitle,
                    onFinished = { showSplash = false }
                )
            }
        }
    }
}
