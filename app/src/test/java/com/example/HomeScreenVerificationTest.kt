package com.example

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w480dp-h1000dp-xhdpi")
class HomeScreenVerificationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `en ar fr es key parity check prints missing 0`() {
        val fields = I18n.Strings::class.java.declaredFields.filter {
            !java.lang.reflect.Modifier.isStatic(it.modifiers) && it.name != "\$jacocoData"
        }
        var missingCount = 0

        assertEquals(setOf("ar", "en", "fr", "es"), I18n.all.keys)

        for ((langCode, strings) in I18n.all) {
            for (field in fields) {
                field.isAccessible = true
                val value = field.get(strings)
                when (value) {
                    null -> {
                        println("Missing (null) in $langCode: ${field.name}")
                        missingCount++
                    }
                    is String -> if (value.isBlank()) {
                        println("Blank String in $langCode: ${field.name}")
                        missingCount++
                    }
                    is PluralString -> {
                        if (!value.isNotBlank()) {
                            println("Blank PluralString in $langCode: ${field.name}")
                            missingCount++
                        }
                        for (testN in listOf(0, 1, 2, 5, 15, 100)) {
                            if (value.format(langCode, testN, "month" to "Oct").isBlank()) {
                                println("Blank formatted PluralString($testN) in $langCode: ${field.name}")
                                missingCount++
                            }
                        }
                    }
                    is List<*> -> {
                        if (value.isEmpty() || value.any { it == null || (it is String && it.isBlank()) }) {
                            println("Empty/blank List in $langCode: ${field.name}")
                            missingCount++
                        }
                        if (field.name == "months" && value.size != 12) {
                            println("Invalid months size in $langCode: ${value.size}")
                            missingCount++
                        }
                        if (field.name == "weekdays" && value.size != 7) {
                            println("Invalid weekdays size in $langCode: ${value.size}")
                            missingCount++
                        }
                    }
                }
            }
        }

        // Must print missing: 0
        println("EN/AR key parity: missing: $missingCount")
        println("All 4 languages (AR/EN/FR/ES) key parity: missing: $missingCount")
        assertEquals("Missing keys across AR/EN/FR/ES strings", 0, missingCount)

        // Verify specific HX & Phase 1 Brand / Language rules
        assertEquals("Kannach", I18n.EN.appTitle)
        assertEquals("Kannach", I18n.FR.appTitle)
        assertEquals("Kannach", I18n.ES.appTitle)
        assertEquals("كُنَّاش", I18n.AR.appTitle)

        assertEquals("a notebook that grows", I18n.EN.appSub)
        assertEquals("un carnet qui fleurit", I18n.FR.appSub)
        assertEquals("un cuaderno que florece", I18n.ES.appSub)
        assertEquals("دفترٌ يُزهر", I18n.AR.appSub)

        assertEquals("Garden", I18n.EN.dockGarden)
        assertEquals("Jardin", I18n.FR.dockGarden)
        assertEquals("Jardín", I18n.ES.dockGarden)
        assertEquals("الحديقة", I18n.AR.dockGarden)

        assertTrue(I18n.EN.todayCardEmpty.isNotBlank())
        assertTrue(I18n.AR.todayCardEmpty.isNotBlank())
        assertTrue(I18n.EN.monthRecapPlanted.contains("{n}") && I18n.EN.monthRecapPlanted.contains("{month}"))
        assertTrue(I18n.AR.monthRecapPlanted.contains("{n}") && I18n.AR.monthRecapPlanted.contains("{month}"))
        assertTrue(I18n.FR.monthRecapPlanted.contains("{n}") && I18n.FR.monthRecapPlanted.contains("{month}"))
        assertTrue(I18n.ES.monthRecapPlanted.contains("{n}") && I18n.ES.monthRecapPlanted.contains("{month}"))
        assertTrue(I18n.EN.monthRecapQuiet.isNotBlank())
        assertTrue(I18n.AR.monthRecapQuiet.isNotBlank())
    }

    @Test
    fun `language lookup and plurals follow LG2 and LG3 rules`() {
        // LG2: exact base code lookup before - or _, fallback to EN (never AR)
        assertSame(I18n.AR, I18n.forLanguage("ar"))
        assertSame(I18n.AR, I18n.forLanguage("ar-MA"))
        assertSame(I18n.EN, I18n.forLanguage("en-US"))
        assertSame(I18n.FR, I18n.forLanguage("fr-FR"))
        assertSame(I18n.ES, I18n.forLanguage("es_MX"))
        assertSame(I18n.EN, I18n.forLanguage("de"))
        assertSame(I18n.EN, I18n.forLanguage(""))

        assertTrue(I18n.isRtl("ar"))
        assertTrue(I18n.isRtl("ar-SA"))
        assertFalse(I18n.isRtl("en"))
        assertFalse(I18n.isRtl("fr"))
        assertFalse(I18n.isRtl("es"))

        // LG3: Plurals
        assertEquals(PluralForm.ZERO, plural("ar", 0))
        assertEquals(PluralForm.ONE, plural("ar", 1))
        assertEquals(PluralForm.TWO, plural("ar", 2))
        assertEquals(PluralForm.FEW, plural("ar", 5))
        assertEquals(PluralForm.MANY, plural("ar", 15))
        assertEquals(PluralForm.OTHER, plural("ar", 100))

        assertEquals(PluralForm.ONE, plural("fr", 0))
        assertEquals(PluralForm.ONE, plural("fr", 1))
        assertEquals(PluralForm.OTHER, plural("fr", 2))

        assertEquals(PluralForm.OTHER, plural("en", 0))
        assertEquals(PluralForm.ONE, plural("en", 1))
        assertEquals(PluralForm.OTHER, plural("en", 2))

        assertEquals(PluralForm.OTHER, plural("es", 0))
        assertEquals(PluralForm.ONE, plural("es", 1))
        assertEquals(PluralForm.OTHER, plural("es", 2))
    }

    @Test
    fun `header renders for every month name in EN AR FR ES with font scale 1_3 without year exceeding 24dp`() {
        val widths = listOf(320.dp, 360.dp, 412.dp)
        val languages = listOf("en", "ar", "fr", "es")
        val currentYear = 2026

        composeTestRule.setContent {
            val density = Density(density = 2.5f, fontScale = 1.3f)
            CompositionLocalProvider(LocalDensity provides density) {
                Column {
                    for (lang in languages) {
                        val strings = I18n.forLanguage(lang)
                        val yearText = I18n.formatYear(currentYear, lang)

                        for (month in 1..12) {
                            val monthName = strings.months[month - 1]

                            for (screenWidth in widths) {
                                Column(
                                    modifier = Modifier.width(screenWidth),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = monthName,
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Spacer(modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = yearText,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.testTag("year_${lang}_${month}_${screenWidth.value.toInt()}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Verify each combination renders as single line with height <= 24 dp
        for (lang in languages) {
            val strings = I18n.forLanguage(lang)
            val yearText = I18n.formatYear(currentYear, lang)
            assertTrue(!yearText.contains("\n"))

            for (month in 1..12) {
                val monthName = strings.months[month - 1]
                assertTrue(monthName.isNotBlank() && !monthName.contains("\n"))

                for (screenWidth in widths) {
                    val tag = "year_${lang}_${month}_${screenWidth.value.toInt()}"
                    val yearNode = composeTestRule.onNodeWithTag(tag)
                    yearNode.assertExists()
                    val heightDp = yearNode.fetchSemanticsNode().size.height / 2.5f
                    assertTrue(
                        "Year text height $heightDp dp exceeded 24 dp in month $monthName ($lang) on width $screenWidth",
                        heightDp <= 24f
                    )
                }
            }
        }
    }

    @Test
    fun `paper dock tabs and header settings follow NV1 to NV7 rules`() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = MainViewModel(app)
        vm.setLanguage("en")

        assertEquals(Tab.PAGE, vm.currentTab.value)

        composeTestRule.setContent {
            KannachTheme(language = "en", month = 10) {
                Column {
                    AppHeader(viewModel = vm)
                    KannachDock(viewModel = vm)
                }
            }
        }

        // NV7: Header has settings button
        composeTestRule.onNodeWithTag("header_settings_button").assertExists()
        composeTestRule.onNodeWithTag("header_fav_button").assertExists()

        // NV6 & NV6b: 4 tab slots + detached Plant button exist
        composeTestRule.onNodeWithTag("dock_tab_page").assertExists()
        composeTestRule.onNodeWithTag("dock_tab_garden").assertExists()
        composeTestRule.onNodeWithTag("dock_tab_letters").assertExists()
        composeTestRule.onNodeWithTag("dock_tab_draw").assertExists()
        composeTestRule.onNodeWithTag("dock_plant_button").assertExists()

        // NV2: Selecting a tab updates currentTab and closes overlays
        vm.openSettings()
        assertTrue(vm.isSettingsOpen.value)
        vm.selectTab(Tab.GARDEN)
        assertEquals(Tab.GARDEN, vm.currentTab.value)
        assertFalse(vm.isSettingsOpen.value)

        vm.selectTab(Tab.LETTERS)
        assertEquals(Tab.LETTERS, vm.currentTab.value)

        vm.selectTab(Tab.DRAW)
        assertEquals(Tab.DRAW, vm.currentTab.value)

        vm.selectTab(Tab.PAGE)
        assertEquals(Tab.PAGE, vm.currentTab.value)
    }

    @Test
    fun `phase 3 tear off card vertical gap at 360x800 and 412x915 is at most 24dp and 360x640 shows compact strip`() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = MainViewModel(app)
        vm.setLanguage("en")

        var currentScreenWidth by androidx.compose.runtime.mutableStateOf(360.dp)
        var currentScreenHeight by androidx.compose.runtime.mutableStateOf(800.dp)
        val testDensity = 2.0f

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = testDensity, fontScale = 1.0f)) {
                KannachTheme(language = "en", month = 10) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.size(currentScreenWidth, currentScreenHeight)
                    ) {
                        Column(modifier = Modifier.size(currentScreenWidth, currentScreenHeight)) {
                            AppHeader(viewModel = vm)
                            MonthGrid(viewModel = vm, modifier = Modifier.weight(1f))
                        }
                        KannachDock(
                            viewModel = vm,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }

        // 1. Test 360x800
        currentScreenWidth = 360.dp
        currentScreenHeight = 800.dp
        composeTestRule.waitForIdle()

        val card360 = composeTestRule.onNodeWithTag("home_page_card")
        card360.assertExists()
        val dock360 = composeTestRule.onNodeWithTag("kannach_dock")
        dock360.assertExists()

        val cardBottom360Px = card360.fetchSemanticsNode().boundsInRoot.bottom
        val dockTop360Px = dock360.fetchSemanticsNode().boundsInRoot.top
        val gap360Dp = (dockTop360Px - cardBottom360Px) / testDensity
        println("Phase 3 Gate A (360x800): vertical gap between card bottom and dock top = $gap360Dp dp")
        assertTrue(
            "Expected vertical gap <= 24 dp at 360x800, but was $gap360Dp dp",
            gap360Dp in 0f..24f
        )

        // 2. Test 412x915
        currentScreenWidth = 412.dp
        currentScreenHeight = 915.dp
        composeTestRule.waitForIdle()

        val card412 = composeTestRule.onNodeWithTag("home_page_card")
        card412.assertExists()
        val dock412 = composeTestRule.onNodeWithTag("kannach_dock")
        dock412.assertExists()

        val cardBottom412Px = card412.fetchSemanticsNode().boundsInRoot.bottom
        val dockTop412Px = dock412.fetchSemanticsNode().boundsInRoot.top
        val gap412Dp = (dockTop412Px - cardBottom412Px) / testDensity
        println("Phase 3 Gate A (412x915): vertical gap between card bottom and dock top = $gap412Dp dp")
        assertTrue(
            "Expected vertical gap <= 24 dp at 412x915, but was $gap412Dp dp",
            gap412Dp in 0f..24f
        )

        // 3. Test 360x640 (Short screen -> compact strip is shown)
        currentScreenWidth = 360.dp
        currentScreenHeight = 640.dp
        composeTestRule.waitForIdle()

        val compactStrip = composeTestRule.onNodeWithTag("home_compact_strip")
        compactStrip.assertExists()
        composeTestRule.onNodeWithTag("home_page_card").assertDoesNotExist()
        val stripHeightDp = compactStrip.fetchSemanticsNode().size.height / testDensity
        println("Phase 3 Gate A (360x640): compact strip shown, height = $stripHeightDp dp")
        assertEquals(64f, stripHeightDp, 2f)
    }

    @Test
    fun `phase 4 just draw export formats signature toggle eraser and zero storage permissions`() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = MainViewModel(app)

        // 1. SK1 & SK3: Per-stroke style, proximity eraser (within 14 canvas units), and 1-step undo restore
        val stroke1 = SketchStroke(
            points = listOf(Point(50f, 50f), Point(100f, 100f), Point(150f, 150f)),
            colorHex = "#5aa25f",
            width = 4f
        )
        val stroke2 = SketchStroke(
            points = listOf(Point(200f, 200f), Point(240f, 260f)),
            colorHex = "#d95f4c",
            width = 14f
        )
        vm.addSketchStroke(stroke1)
        vm.addSketchStroke(stroke2)
        assertEquals(2, vm.sketchStrokes.value.size)
        assertTrue(vm.sketchIntroDismissed.value)

        // Touch at (110, 108) -> distance to (100, 100) is ~12.8 <= 14.0 -> removes stroke1
        vm.eraseSketchAt(Point(110f, 108f))
        assertEquals(1, vm.sketchStrokes.value.size)
        assertEquals("#d95f4c", vm.sketchStrokes.value.first().colorHex)

        // One undo step restores the erased stroke
        vm.undoSketchStroke()
        assertEquals(2, vm.sketchStrokes.value.size)

        // 2. SK8, SK9, SK10: Export test for all 3 formats (Story 9:16, Square 1:1, Page 3:4)
        // with signature ON and OFF, verifying exact dimensions and non-blank signature region
        val formats = listOf(
            ExportFormat.STORY to (1080 to 1920),
            ExportFormat.SQUARE to (1080 to 1080),
            ExportFormat.PAGE to (1080 to 1440)
        )

        for ((fmt, expectedDims) in formats) {
            for (lang in listOf("en", "ar")) {
                val bmpOff = StrokeMath.composeExportBitmap(
                    context = app,
                    strokes = vm.sketchStrokes.value,
                    format = fmt,
                    paperType = SketchPaperType.PLAIN,
                    includeSignature = false,
                    language = lang,
                    palette = BasePalette,
                    dateLabelOverride = "17 Oct 2026"
                )
                val bmpOn = StrokeMath.composeExportBitmap(
                    context = app,
                    strokes = vm.sketchStrokes.value,
                    format = fmt,
                    paperType = SketchPaperType.PLAIN,
                    includeSignature = true,
                    language = lang,
                    palette = BasePalette,
                    dateLabelOverride = "17 Oct 2026"
                )

                assertEquals("Width mismatch for $fmt", expectedDims.first, bmpOn.width)
                assertEquals("Height mismatch for $fmt", expectedDims.second, bmpOn.height)
                assertEquals("Width mismatch for $fmt (off)", expectedDims.first, bmpOff.width)
                assertEquals("Height mismatch for $fmt (off)", expectedDims.second, bmpOff.height)

                // Compute card bottom-end corner region where signature sits (inset 36px inside card)
                val cardBottom = when (fmt) {
                    ExportFormat.STORY -> 280 + 1280 // 1560 (360px above bottom 1920 -> safe zone >= 340px)
                    ExportFormat.SQUARE -> (1080 - 920) / 2 + 920 // 1000
                    ExportFormat.PAGE -> (1440 - 1226) / 2 + 1226 // 1333
                }
                val cardLeft = (1080 - 920) / 2 // 80
                val cardRight = cardLeft + 920 // 1000

                // In Story 9:16, verify cardBottom (1560) leaves >= 340px safe zone at the bottom (1920 - 1560 = 360 >= 340)
                if (fmt == ExportFormat.STORY) {
                    val bottomMargin = bmpOn.height - cardBottom
                    assertTrue("Story bottom safe zone must be >= 340px, was $bottomMargin", bottomMargin >= 340)
                }

                val sigRegionXRange = if (lang == "ar") {
                    (cardLeft + 20)..(cardLeft + 340)
                } else {
                    (cardRight - 340)..(cardRight - 20)
                }
                val sigRegionYRange = (cardBottom - 110)..(cardBottom - 20)

                var differingPixels = 0
                for (x in sigRegionXRange step 2) {
                    for (y in sigRegionYRange step 2) {
                        if (bmpOn.getPixel(x, y) != bmpOff.getPixel(x, y)) {
                            differingPixels++
                        }
                    }
                }
                println("Phase 4 Gate A ($fmt, lang=$lang): differing signature pixels = $differingPixels")
                assertTrue(
                    "Expected signature region to have rendered pixels when signature=ON ($fmt, $lang), but differingPixels=$differingPixels",
                    differingPixels > 20
                )

                bmpOff.recycle()
                bmpOn.recycle()
            }
        }

        // 3. SK4: Verify all 4 procedural paper types render cleanly
        for (paper in SketchPaperType.entries) {
            val bmp = StrokeMath.composeExportBitmap(
                context = app,
                strokes = vm.sketchStrokes.value,
                format = ExportFormat.PAGE,
                paperType = paper,
                includeSignature = true,
                language = "en",
                palette = BasePalette
            )
            assertEquals(1080, bmp.width)
            assertEquals(1440, bmp.height)
            bmp.recycle()
        }

        // 4. SK12: Verify AndroidManifest.xml has zero storage permissions
        val manifestFile = java.io.File("src/main/AndroidManifest.xml").let {
            if (it.exists()) it else java.io.File("app/src/main/AndroidManifest.xml")
        }
        assertTrue("AndroidManifest.xml must exist", manifestFile.exists())
        val manifestText = manifestFile.readText()
        assertFalse("Manifest must not request WRITE_EXTERNAL_STORAGE", manifestText.contains("WRITE_EXTERNAL_STORAGE"))
        assertFalse("Manifest must not request READ_EXTERNAL_STORAGE", manifestText.contains("READ_EXTERNAL_STORAGE"))
        assertFalse("Manifest must not request READ_MEDIA_IMAGES", manifestText.contains("READ_MEDIA_IMAGES"))
    }

    @Test
    fun `phase 5 letters shelf compose countdown opened view delete and notifications`() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = MainViewModel(app)
        vm.completeFirstRun()
        vm.setLanguage("en")

        // 1. Verify Compose opens with prefilled "Dear future me," and default 1-month preset (CP4, CP6)
        vm.selectTab(Tab.LETTERS)
        vm.openCapsuleCompose()
        assertTrue(vm.isCapsuleComposeOpen.value)
        assertEquals(CapsulePreset.MONTH_1, vm.capsulePreset.value)
        assertTrue(vm.capsuleDraftText.value.startsWith("Dear future me,"))
        val oneMonthDays = I18n.daysUntilDateKey(vm.capsuleUnlockDate.value, vm.getTodayKey())
        assertTrue("Default 1-month preset should be >= 28 days away, was $oneMonthDays", oneMonthDays in 28..32)

        // 2. Test preset chips (1w, 3m, 6m, 1y)
        vm.selectCapsulePreset(CapsulePreset.WEEK_1)
        assertEquals(7, I18n.daysUntilDateKey(vm.capsuleUnlockDate.value, vm.getTodayKey()))
        vm.selectCapsulePreset(CapsulePreset.YEAR_1)
        assertTrue(I18n.daysUntilDateKey(vm.capsuleUnlockDate.value, vm.getTodayKey()) in 364..366)
        vm.selectCapsulePreset(CapsulePreset.WEEK_1)

        // 3. Optional doodle (CP5)
        vm.toggleCapsuleDoodle()
        assertTrue(vm.capsuleDoodleVisible.value)
        vm.addCapsuleDoodleStroke(listOf(Point(20f, 20f), Point(80f, 80f), Point(140f, 120f)))
        assertEquals(1, vm.capsuleDoodleStrokes.value.size)

        // 4. Seal confirmation dialog (CP7)
        vm.updateCapsuleDraftText("Dear future me,\nRemember the quiet morning light.")
        vm.requestSealCapsule()
        val dialogData = vm.confirmDialog.value
        assertTrue("Seal confirmation dialog must be shown", dialogData != null)
        assertFalse("Seal dialog is non-destructive", dialogData!!.isDestructive)
        assertEquals("Keep writing", dialogData.cancelText)
        assertTrue(
            "Unexpected seal confirm msg: ${dialogData.message}",
            dialogData.message.startsWith("Once sealed,") && dialogData.message.contains("see it until")
        )
        // RB5 check: confirmation message uses formatted long date, not raw ISO YYYY-MM-DD
        assertFalse(
            "Seal dialog message must not show raw ISO date",
            Regex("""\d{4}-\d{2}-\d{2}""").containsMatchIn(dialogData.message)
        )

        // Execute seal -> schedules notification (CP9) and adds locked letter to Waiting
        vm.sealDraftCapsuleWithCeremony()
        vm.completeSealingCeremony()
        assertEquals(1, vm.capsules.value.size)
        val lockedCapsule = vm.capsules.value.first()
        assertEquals(1, lockedCapsule.strokes.size)
        assertTrue(
            "Sealing a letter must schedule its notification",
            NotificationHelper.isCapsuleNotificationScheduled(app, lockedCapsule.id)
        )

        // 5. CP3: Tapping a locked letter shows "Not yet — {n} days to go" toast
        vm.onLockedCapsuleTap(lockedCapsule)
        val lockedToast = vm.toastMessage.value ?: ""
        assertTrue(
            "Locked tap must show 'Not yet' countdown toast, got: $lockedToast",
            lockedToast.contains("Not yet") && lockedToast.contains("7 days to go")
        )

        // 6. Add a due (Ready) letter (unlockDate <= todayKey) and verify CP10 badge + CP8 Opened view
        val readyCapsule = vm.repository.addCapsule(
            text = "Dear future me,\nYou made it through autumn.",
            unlockDate = "2020-01-01",
            strokes = listOf(listOf(Point(10f, 10f), Point(50f, 50f))),
            color = "#5aa25f"
        )
        vm.checkReadyCapsule()
        assertTrue("Ready capsule should be detected for badge/banner (CP10)", vm.readyCapsule.value != null)

        // Open the Ready letter -> marks opened, sets openedAt, opens OpenedCapsuleOverlay (CP8, CP10)
        vm.openReadyCapsule(readyCapsule)
        assertTrue("Ready badge/banner must clear once letter is opened (CP10)", vm.readyCapsule.value == null)
        val openedView = vm.openedCapsuleView.value
        assertTrue("OpenedCapsuleOverlay state should be populated (CP8)", openedView != null)
        assertTrue(openedView!!.opened)
        assertTrue("openedAt timestamp must be stored when opened (CP2)", openedView.openedAt != null)

        // Verify "Write back" action from Opened view opens Compose (CP8)
        vm.writeBackFromOpenedCapsule()
        assertTrue(vm.openedCapsuleView.value == null)
        assertTrue(vm.isCapsuleComposeOpen.value)
        vm.closeCapsuleCompose()

        // 7. CP3: Delete confirmation ("Tear this letter up?") removes letter and cancels notification
        vm.requestDeleteCapsule(lockedCapsule.id)
        val deleteDialog = vm.confirmDialog.value
        assertTrue(deleteDialog != null)
        assertEquals("Tear this letter up?", deleteDialog!!.title)
        assertTrue(deleteDialog.isDestructive)
        deleteDialog.onConfirm()
        assertFalse(vm.capsules.value.any { it.id == lockedCapsule.id })
        assertFalse(NotificationHelper.isCapsuleNotificationScheduled(app, lockedCapsule.id))

        // 8. RB1: Verify daily reminder toggle calls NotificationHelper.scheduleDailyReminder / cancelDailyReminder
        vm.toggleReminder(true)
        assertTrue(vm.reminder.value.enabled)
        assertTrue("Enabling reminder must schedule daily alarm (RB1)", NotificationHelper.isDailyReminderScheduled(app))
        vm.setReminderTime("21:15")
        assertTrue("Updating reminder time must keep daily alarm scheduled (RB1)", NotificationHelper.isDailyReminderScheduled(app))
        vm.toggleReminder(false)
        assertFalse(vm.reminder.value.enabled)
        assertFalse("Disabling reminder must cancel daily alarm (RB1)", NotificationHelper.isDailyReminderScheduled(app))
    }

    @Test
    fun phase6_gateA_userSettingsRoundTripAndExcludedFromBackup() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo1 = KannachRepository(app)

        // 1. Default language choice for new installs is "system" (LG6)
        repo1.setLanguage("system")
        assertEquals("system", repo1.getLanguageChoice())

        // 2. Save custom UserSettings to kannach.settings.v1
        val custom = UserSettings(
            reminderDays = setOf(2, 4, 6),
            weekStartsOn = 2, // Monday
            useArabicIndicNumerals = true,
            handwritingSize = "large",
            seasonOverride = "autumn",
            reduceMotion = "on",
            hapticsEnabled = false,
            hideInRecents = true,
            lastBackupTimestamp = 1760784000000L,
            copyReminder = "monthly",
            exportSignature = false
        )
        repo1.saveUserSettings(custom)

        // 3. Re-instantiate repository from SharedPreferences and verify 100% round-trip
        val repo2 = KannachRepository(app)
        val loaded = repo2.getUserSettings()
        assertEquals(custom, loaded)
        assertEquals(24, loaded.handwritingSp)
        assertEquals(2, loaded.effectiveWeekStart("ar"))

        // 4. Verify UserSettings is NOT part of diary backup JSON
        val backupJson = repo2.createBackupJson()
        assertFalse(
            "Backup JSON must not include kannach.settings.v1 fields",
            backupJson.contains("hideInRecents") || backupJson.contains("seasonOverride")
        )
    }

    @Test
    fun phase6_gateA_weekStartsOn_reordersHeadersAndShiftsDay1() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = MainViewModel(app)
        vm.completeFirstRun()
        vm.setLanguage("en")
        // Set month to May 2026 (2026-05-01 is a Friday, Calendar.FRIDAY = 6)
        vm.setMonth(2026, 5)

        composeTestRule.setContent {
            val lang by vm.language.collectAsState()
            val month by vm.currentMonth.collectAsState()
            val settings by vm.userSettings.collectAsState()
            KannachTheme(
                language = lang,
                month = month,
                seasonOverride = settings.seasonOverride,
                reduceMotionOverride = true,
                hapticsEnabled = settings.hapticsEnabled,
                handwritingSp = settings.handwritingSp
            ) {
                MonthGrid(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        // 1. Week starts on Sunday (1): header 0 is "Sun", May 1 (Friday) offset = (6 - 1 + 7) % 7 = 5
        vm.setWeekStartsOn(1)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("weekday_header_0").assertTextEquals("Sun")
        composeTestRule.onNodeWithTag("weekday_header_6").assertTextEquals("Sat")
        val boundsSun = vm.getCellBounds("2026-05-01")
        assertTrue("Cell bounds for 2026-05-01 must be recorded", boundsSun != null)

        // 2. Week starts on Monday (2): header 0 is "Mon", May 1 (Friday) offset = (6 - 2 + 7) % 7 = 4 (shifts 1 column left in LTR)
        vm.setWeekStartsOn(2)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("weekday_header_0").assertTextEquals("Mon")
        composeTestRule.onNodeWithTag("weekday_header_6").assertTextEquals("Sun")
        val boundsMon = vm.getCellBounds("2026-05-01")
        assertTrue("Day 1 must shift left when week starts on Monday instead of Sunday", boundsMon!!.offset.x < boundsSun!!.offset.x)

        // 3. Week starts on Saturday (7): header 0 is "Sat", May 1 (Friday) offset = (6 - 7 + 7) % 7 = 6 (shifts to last column)
        vm.setWeekStartsOn(7)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("weekday_header_0").assertTextEquals("Sat")
        composeTestRule.onNodeWithTag("weekday_header_6").assertTextEquals("Fri")
        val boundsSat = vm.getCellBounds("2026-05-01")
        assertTrue("Day 1 must shift right to column 6 when week starts on Saturday", boundsSat!!.offset.x > boundsSun.offset.x)
    }

    @Test
    fun phase6_gateA_arabicNumerals_and_flagSecure_and_settingsChapters() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = MainViewModel(app)
        vm.completeFirstRun()
        vm.setLanguage("ar")
        vm.setUseArabicIndicNumerals(false)
        vm.openSettings()

        var hostActivity: android.app.Activity? = null
        composeTestRule.setContent {
            hostActivity = LocalContext.current as? android.app.Activity
            KannachApp(viewModel = vm)
        }
        // Advance past splash screen
        composeTestRule.mainClock.advanceTimeBy(2500)
        composeTestRule.waitForIdle()

        // 1. Verify Arabic Numerals setting (Western 0–9 default vs Arabic-Indic ٠–٩)
        assertEquals("15", I18n.formatNumber(15, "ar"))
        assertEquals("2026", I18n.formatYear(2026, "ar"))
        assertTrue(I18n.AR.capsuleCountdown.format("ar", 5).contains("5"))

        // Switch to Arabic-Indic numerals via ViewModel / Settings
        composeTestRule.onNodeWithTag("settings_numerals_section").assertExists()
        vm.setUseArabicIndicNumerals(true)
        composeTestRule.waitForIdle()

        assertEquals("١٥", I18n.formatNumber(15, "ar"))
        assertEquals("٢٠٢٦", I18n.formatYear(2026, "ar"))
        val arPlural = I18n.AR.capsuleCountdown.format("ar", 5)
        assertTrue("Arabic plural count should use Arabic-Indic digits when enabled, got: $arPlural", arPlural.contains("٥"))

        // Verify Numerals section is hidden for non-Arabic languages
        vm.setLanguage("en")
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithTag("settings_numerals_section").assertCountEquals(0)

        // 2. Verify Hide in recent apps toggles WindowManager.LayoutParams.FLAG_SECURE on Activity window
        val activity = hostActivity!!
        val initialFlags = activity.window.attributes.flags
        assertEquals(0, initialFlags and android.view.WindowManager.LayoutParams.FLAG_SECURE)

        vm.setHideInRecents(true)
        composeTestRule.waitForIdle()
        val secureFlags = activity.window.attributes.flags
        assertTrue(
            "Enabling hideInRecents must set FLAG_SECURE on Activity window",
            (secureFlags and android.view.WindowManager.LayoutParams.FLAG_SECURE) != 0
        )

        vm.setHideInRecents(false)
        composeTestRule.waitForIdle()
        val clearedFlags = activity.window.attributes.flags
        assertEquals(
            "Disabling hideInRecents must clear FLAG_SECURE on Activity window",
            0,
            clearedFlags and android.view.WindowManager.LayoutParams.FLAG_SECURE
        )

        // 3. Verify all 6 chapters exist in Settings v2 + Test Nudge + Export Signature sync + Replay Welcome
        composeTestRule.onNodeWithTag("settings_chapter_language").assertExists()
        composeTestRule.onNodeWithTag("settings_chapter_reminders").assertExists()
        composeTestRule.onNodeWithTag("settings_chapter_writing").assertExists()
        composeTestRule.onNodeWithTag("settings_chapter_appearance").assertExists()
        composeTestRule.onNodeWithTag("settings_chapter_privacy").assertExists()
        composeTestRule.onNodeWithTag("settings_chapter_about").assertExists()

        // Test nudge picks one of the 7 messages
        val nudgeMsg = vm.sendTestNudge(app)
        assertTrue("Test nudge must be one of the 7 reminder messages", I18n.EN.reminderMsgs.contains(nudgeMsg))
        assertEquals(7, I18n.EN.reminderMsgs.size)
        assertEquals(7, I18n.AR.reminderMsgs.size)
        assertEquals(7, I18n.FR.reminderMsgs.size)
        assertEquals(7, I18n.ES.reminderMsgs.size)

        // Signature switch syncs between SK7 exportSignature and UserSettings
        vm.setExportSignature(false)
        assertFalse(vm.exportSignature.value)
        assertFalse(vm.userSettings.value.exportSignature)

        // Replay welcome closes Settings and opens FirstRunOnboarding
        vm.replayWelcome()
        composeTestRule.waitForIdle()
        assertFalse(vm.isSettingsOpen.value)
        assertTrue(vm.isFirstRun.value)
    }
}




