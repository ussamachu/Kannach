package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class SheetMode { VIEW, EDIT }

enum class Tab { PAGE, GARDEN, LETTERS, DRAW }

enum class SketchTool { PEN, BRUSH, CALLIGRAPHY, MARKER, PENCIL, CRAYON }

data class CellBounds(
    val offset: Offset,
    val size: IntSize
)

data class PlantingAnimation(
    val dateKey: String,
    val text: String,
    val colorHex: String,
    val fav: Boolean,
    val strokes: List<List<Point>>,
    val startOffset: Offset,
    val targetOffset: Offset,
    val targetSize: IntSize
)

data class OnThisDayHit(
    val key: String,
    val year: Int,
    val entry: DayEntry
)

enum class CapsulePreset {
    TOMORROW, WEEK_1, MONTH_1, MONTH_3, MONTH_6, YEAR_1, CUSTOM
}

enum class CapsuleDraftStatus {
    IDLE, SAVING, SAVED
}

data class ConfirmDialogData(
    val title: String,
    val message: String,
    val confirmText: String,
    val cancelText: String,
    val onConfirm: () -> Unit,
    val onCancel: () -> Unit,
    val isDestructive: Boolean = true
)

data class YearWrapData(
    val year: Int,
    val count: Int,
    val favCount: Int,
    val topColor: String,
    val bestStreak: Int,
    val keys: List<String>
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = KannachRepository(application)

    private val calendar = Calendar.getInstance()
    private val nowYear = calendar.get(Calendar.YEAR)
    private val nowMonth = calendar.get(Calendar.MONTH) + 1
    private val nowDay = calendar.get(Calendar.DAY_OF_MONTH)

    fun getTodayKey(): String = formatDateKey(nowYear, nowMonth, nowDay)

    // --- State: Calendar Grid ---
    private val _currentYear = MutableStateFlow(nowYear)
    val currentYear: StateFlow<Int> = _currentYear.asStateFlow()

    private val _currentMonth = MutableStateFlow(nowMonth)
    val currentMonth: StateFlow<Int> = _currentMonth.asStateFlow()

    private val _gridFilter = MutableStateFlow("all") // "all" or "fav"
    val gridFilter: StateFlow<String> = _gridFilter.asStateFlow()

    // --- Repository Flows ---
    val entries: StateFlow<Map<String, DayEntry>> = repository.entriesFlow
    val capsules: StateFlow<List<TimeCapsule>> = repository.capsulesFlow
    val reminder: StateFlow<ReminderConfig> = repository.reminderFlow
    val language: StateFlow<String> = repository.languageFlow
    val languageChoice: StateFlow<String> = repository.languageChoiceFlow
    val userSettings: StateFlow<UserSettings> = repository.userSettingsFlow

    // --- Navigation & Overlays (NV1, NV2) ---
    private val _currentTab = MutableStateFlow(Tab.PAGE)
    val currentTab: StateFlow<Tab> = _currentTab.asStateFlow()

    private val _activeSheetDate = MutableStateFlow<String?>(null)
    val activeSheetDate: StateFlow<String?> = _activeSheetDate.asStateFlow()

    private val _sheetMode = MutableStateFlow(SheetMode.VIEW)
    val sheetMode: StateFlow<SheetMode> = _sheetMode.asStateFlow()

    private val _isWrapOpen = MutableStateFlow(false)
    val isWrapOpen: StateFlow<Boolean> = _isWrapOpen.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    // --- Active Entry Editing State ---
    private val _sheetText = MutableStateFlow("")
    val sheetText: StateFlow<String> = _sheetText.asStateFlow()

    private val _sheetStrokes = MutableStateFlow<List<List<Point>>>(emptyList())
    val sheetStrokes: StateFlow<List<List<Point>>> = _sheetStrokes.asStateFlow()

    private val _sheetColor = MutableStateFlow(DEFAULT_GREEN_HEX)
    val sheetColor: StateFlow<String> = _sheetColor.asStateFlow()

    private val _sheetFav = MutableStateFlow(false)
    val sheetFav: StateFlow<Boolean> = _sheetFav.asStateFlow()

    // Ritual 3-pulse step (W1, W2, W3): 1 = Write/Arrive, 2 = Draw/Mark
    private val _writeStep = MutableStateFlow(1)
    val writeStep: StateFlow<Int> = _writeStep.asStateFlow()

    fun setWriteStep(step: Int) {
        vibrate(6)
        _writeStep.value = step
    }

    // Garden cell positions recorded via onGloballyPositioned (W3)
    private val _cellBoundsMap = mutableMapOf<String, CellBounds>()

    fun registerCellBounds(dateKey: String, offset: Offset, size: IntSize) {
        _cellBoundsMap[dateKey] = CellBounds(offset, size)
    }

    fun getCellBounds(dateKey: String): CellBounds? = _cellBoundsMap[dateKey]

    // Ceremony animation state (W3)
    private val _plantingAnimation = MutableStateFlow<PlantingAnimation?>(null)
    val plantingAnimation: StateFlow<PlantingAnimation?> = _plantingAnimation.asStateFlow()

    private val _plantedCellPulse = MutableStateFlow<String?>(null)
    val plantedCellPulse: StateFlow<String?> = _plantedCellPulse.asStateFlow()

    private var initialSnapshot: String = ""

    // --- Sketchpad State (SK1–SK8 & ER1–ER5 & TR1–TR3) ---
    private val _sketchStrokes = MutableStateFlow<List<SketchStroke>>(emptyList())
    val sketchStrokes: StateFlow<List<SketchStroke>> = _sketchStrokes.asStateFlow()

    private val _sketchUndoStack = mutableListOf<List<SketchStroke>>()
    private val _sketchRedoStack = mutableListOf<List<SketchStroke>>()
    private var eraseGestureUndoPushed = false

    private val _canUndoSketch = MutableStateFlow(false)
    val canUndoSketch: StateFlow<Boolean> = _canUndoSketch.asStateFlow()

    private val _canRedoSketch = MutableStateFlow(false)
    val canRedoSketch: StateFlow<Boolean> = _canRedoSketch.asStateFlow()

    private val _sketchColor = MutableStateFlow(DEFAULT_GREEN_HEX)
    val sketchColor: StateFlow<String> = _sketchColor.asStateFlow()

    private val _sketchBrushWidth = MutableStateFlow(8f)
    val sketchBrushWidth: StateFlow<Float> = _sketchBrushWidth.asStateFlow()

    private val _sketchEraserMode = MutableStateFlow(false)
    val sketchEraserMode: StateFlow<Boolean> = _sketchEraserMode.asStateFlow()

    private val _sketchTool = MutableStateFlow(SketchTool.PEN)
    val sketchTool: StateFlow<SketchTool> = _sketchTool.asStateFlow()

    private val _sketchPaperType = MutableStateFlow(SketchPaperType.PLAIN)
    val sketchPaperType: StateFlow<SketchPaperType> = _sketchPaperType.asStateFlow()

    private val _sketchIntroDismissed = MutableStateFlow(false)
    val sketchIntroDismissed: StateFlow<Boolean> = _sketchIntroDismissed.asStateFlow()

    private val _isExportSheetOpen = MutableStateFlow(false)
    val isExportSheetOpen: StateFlow<Boolean> = _isExportSheetOpen.asStateFlow()

    private val _exportFormat = MutableStateFlow(ExportFormat.STORY)
    val exportFormat: StateFlow<ExportFormat> = _exportFormat.asStateFlow()

    private val _exportSignature = MutableStateFlow(repository.getUserSettings().exportSignature)
    val exportSignature: StateFlow<Boolean> = _exportSignature.asStateFlow()

    // --- Just Draw v5 Emotional Polish State (JD1, JD3, JD5, JD6) ---
    private var lastWhisperIndex = -1
    private val _sketchWhisperIndex = MutableStateFlow(0)
    val sketchWhisperIndex: StateFlow<Int> = _sketchWhisperIndex.asStateFlow()

    private val _sketchMoodLabelVisible = MutableStateFlow(false)
    val sketchMoodLabelVisible: StateFlow<Boolean> = _sketchMoodLabelVisible.asStateFlow()
    private var moodLabelJob: kotlinx.coroutines.Job? = null

    private val _sketchPageTurnCount = MutableStateFlow(0)
    val sketchPageTurnCount: StateFlow<Int> = _sketchPageTurnCount.asStateFlow()

    private val _sketchClearUndoVisible = MutableStateFlow(false)
    val sketchClearUndoVisible: StateFlow<Boolean> = _sketchClearUndoVisible.asStateFlow()
    private var clearUndoJob: kotlinx.coroutines.Job? = null

    private var lastSavedWhisperIndex = -1
    private val _sketchSavedWhisperIndex = MutableStateFlow<Int?>(null)
    val sketchSavedWhisperIndex: StateFlow<Int?> = _sketchSavedWhisperIndex.asStateFlow()
    private var savedWhisperJob: kotlinx.coroutines.Job? = null

    // --- Letters / Time Capsule State (CP1–CP10 & LC1–LC5) ---
    private val _isCapsuleComposeOpen = MutableStateFlow(false)
    val isCapsuleComposeOpen: StateFlow<Boolean> = _isCapsuleComposeOpen.asStateFlow()

    private val _capsuleComposeSessionId = MutableStateFlow(0)
    val capsuleComposeSessionId: StateFlow<Int> = _capsuleComposeSessionId.asStateFlow()

    private val _capsuleTitleText = MutableStateFlow("")
    val capsuleTitleText: StateFlow<String> = _capsuleTitleText.asStateFlow()

    private val _capsuleDraftText = MutableStateFlow("")
    val capsuleDraftText: StateFlow<String> = _capsuleDraftText.asStateFlow()

    // LC5: Lightweight boolean that only flips when body transitions between blank and non-blank
    private val _capsuleHasBodyText = MutableStateFlow(false)
    val capsuleHasBodyText: StateFlow<Boolean> = _capsuleHasBodyText.asStateFlow()

    private var liveCapsuleTitle: String = ""
    private var liveCapsuleBody: String = ""
    private var initialCapsuleSnapshot: String = ""
    private var capsuleDraftAutosaveJob: kotlinx.coroutines.Job? = null

    private val _capsuleDraftStatus = MutableStateFlow(CapsuleDraftStatus.IDLE)
    val capsuleDraftStatus: StateFlow<CapsuleDraftStatus> = _capsuleDraftStatus.asStateFlow()

    private val _isCapsuleBackSheetOpen = MutableStateFlow(false)
    val isCapsuleBackSheetOpen: StateFlow<Boolean> = _isCapsuleBackSheetOpen.asStateFlow()

    private val _isCapsuleSealSheetOpen = MutableStateFlow(false)
    val isCapsuleSealSheetOpen: StateFlow<Boolean> = _isCapsuleSealSheetOpen.asStateFlow()

    private val _capsuleDoodleVisible = MutableStateFlow(false)
    val capsuleDoodleVisible: StateFlow<Boolean> = _capsuleDoodleVisible.asStateFlow()

    private val _capsuleDoodleStrokes = MutableStateFlow<List<List<Point>>>(emptyList())
    val capsuleDoodleStrokes: StateFlow<List<List<Point>>> = _capsuleDoodleStrokes.asStateFlow()

    private val _capsuleDoodleColor = MutableStateFlow(DEFAULT_GREEN_HEX)
    val capsuleDoodleColor: StateFlow<String> = _capsuleDoodleColor.asStateFlow()

    private val _capsulePreset = MutableStateFlow(CapsulePreset.MONTH_1)
    val capsulePreset: StateFlow<CapsulePreset> = _capsulePreset.asStateFlow()

    private val _capsuleUnlockDate = MutableStateFlow(computePresetDateKey(CapsulePreset.MONTH_1))
    val capsuleUnlockDate: StateFlow<String> = _capsuleUnlockDate.asStateFlow()

    private val _isSealingCeremonyActive = MutableStateFlow(false)
    val isSealingCeremonyActive: StateFlow<Boolean> = _isSealingCeremonyActive.asStateFlow()

    private val _highlightedCapsuleId = MutableStateFlow<String?>(null)
    val highlightedCapsuleId: StateFlow<String?> = _highlightedCapsuleId.asStateFlow()

    private val _openedCapsuleView = MutableStateFlow<TimeCapsule?>(null)
    val openedCapsuleView: StateFlow<TimeCapsule?> = _openedCapsuleView.asStateFlow()

    // --- Banners & Feedback ---
    private val _onThisDayHit = MutableStateFlow<OnThisDayHit?>(null)
    val onThisDayHit: StateFlow<OnThisDayHit?> = _onThisDayHit.asStateFlow()

    private val _readyCapsule = MutableStateFlow<TimeCapsule?>(null)
    val readyCapsule: StateFlow<TimeCapsule?> = _readyCapsule.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _confirmDialog = MutableStateFlow<ConfirmDialogData?>(null)
    val confirmDialog: StateFlow<ConfirmDialogData?> = _confirmDialog.asStateFlow()

    // --- First Run & Milestone Celebrations (R2, R3) ---
    private val _isFirstRun = MutableStateFlow(repository.isFirstRun())
    val isFirstRun: StateFlow<Boolean> = _isFirstRun.asStateFlow()

    fun completeFirstRun() {
        repository.markFirstRunCompleted()
        _isFirstRun.value = false
    }

    private val _firstPlantCelebratedDate = MutableStateFlow<String?>(null)
    val firstPlantCelebratedDate: StateFlow<String?> = _firstPlantCelebratedDate.asStateFlow()

    init {
        checkOnThisDay()
        if (ENABLE_LETTERS) {
            checkReadyCapsule()
        } else {
            NotificationHelper.cancelAllCapsuleNotifications(getApplication(), repository.getCapsules())
        }
        pickNextSketchWhisper()
        if (reminder.value.enabled) {
            NotificationHelper.scheduleDailyReminder(getApplication(), reminder.value.time)
        }
        if (userSettings.value.copyReminder == "monthly") {
            NotificationHelper.scheduleMonthlyCopyReminder(getApplication())
        }
    }

    fun vibrate(durationMs: Long = 8) {
        if (!userSettings.value.hapticsEnabled) return
        val app = getApplication<Application>()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrator = app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun dismissConfirmDialog() {
        _confirmDialog.value = null
    }

    // --- Calendar Navigation ---

    fun getMinYear(): Int {
        val entryYears = entries.value.keys.mapNotNull { it.substringBefore("-").toIntOrNull() }
        return if (entryYears.isNotEmpty()) minOf(entryYears.minOrNull() ?: 2000, 2000) else 2000
    }

    fun shiftMonth(delta: Int) {
        var y = _currentYear.value
        var m = _currentMonth.value + delta

        if (m < 1) {
            m = 12
            y--
        } else if (m > 12) {
            m = 1
            y++
        }

        // Check bounds
        if (y < getMinYear() || (y == getMinYear() && m < 1)) return
        if (y > nowYear || (y == nowYear && m > nowMonth)) return

        _currentYear.value = y
        _currentMonth.value = m
        vibrate(6)
    }

    fun setMonth(year: Int, month: Int) {
        if (year < getMinYear() || year > nowYear) return
        if (year == nowYear && month > nowMonth) return
        _currentYear.value = year
        _currentMonth.value = month
        vibrate(6)
    }

    fun shiftYear(delta: Int) {
        val y = _currentYear.value + delta
        if (y < getMinYear() || y > nowYear) return
        _currentYear.value = y
        _currentMonth.value = if (y == nowYear) nowMonth else 1
        vibrate(6)
    }

    fun setGridFilter(filter: String) {
        _gridFilter.value = filter
    }

    // --- Entry Sheet ---

    private fun computeSnapshot(): String {
        return "${_sheetText.value}|${_sheetColor.value}|${_sheetFav.value}|${_sheetStrokes.value.size}"
    }

    fun isSheetDirty(): Boolean {
        return computeSnapshot() != initialSnapshot
    }

    fun openSheet(dateKey: String) {
        if (dateKey > getTodayKey()) {
            val str = I18n.forLanguage(language.value)
            showToast(str.notYet)
            return
        }
        vibrate(8)
        _activeSheetDate.value = dateKey
        _writeStep.value = 1

        val existing = entries.value[dateKey]
        if (existing != null) {
            _sheetText.value = existing.text
            _sheetStrokes.value = existing.strokes
            _sheetColor.value = existing.color
            _sheetFav.value = existing.fav
            _sheetMode.value = SheetMode.VIEW
            initialSnapshot = computeSnapshot()
        } else {
            val draft = repository.getDraft(dateKey)
            if (draft != null) {
                _sheetText.value = draft.text
                _sheetStrokes.value = draft.strokes
                _sheetColor.value = draft.color
                _sheetFav.value = draft.fav
            } else {
                _sheetText.value = ""
                _sheetStrokes.value = emptyList()
                _sheetColor.value = DEFAULT_GREEN_HEX
                _sheetFav.value = false
            }
            _sheetMode.value = SheetMode.EDIT
            initialSnapshot = computeSnapshot()
        }
    }

    fun getPreviousEntryDateKey(currentKey: String): String? {
        val sortedKeys = entries.value.keys.filter {
            val e = entries.value[it]
            e != null && (e.text.isNotBlank() || e.strokes.isNotEmpty())
        }.sorted()
        val index = sortedKeys.indexOf(currentKey)
        return if (index > 0) sortedKeys[index - 1] else null
    }

    fun getNextEntryDateKey(currentKey: String): String? {
        val sortedKeys = entries.value.keys.filter {
            val e = entries.value[it]
            e != null && (e.text.isNotBlank() || e.strokes.isNotEmpty())
        }.sorted()
        val index = sortedKeys.indexOf(currentKey)
        return if (index >= 0 && index < sortedKeys.size - 1) sortedKeys[index + 1] else null
    }

    fun getRandomEntryDateKey(): String? {
        val validKeys = entries.value.keys.filter {
            val e = entries.value[it]
            e != null && (e.text.isNotBlank() || e.strokes.isNotEmpty())
        }
        return if (validKeys.isNotEmpty()) validKeys.random() else null
    }

    fun switchToEditMode() {
        vibrate(6)
        _sheetMode.value = SheetMode.EDIT
        _writeStep.value = 1
    }

    fun updateSheetText(text: String) {
        _sheetText.value = text
    }

    fun addSheetStroke(stroke: List<Point>) {
        if (stroke.isNotEmpty()) {
            _sheetStrokes.value = _sheetStrokes.value + listOf(stroke)
        }
    }

    fun undoSheetStroke() {
        if (_sheetStrokes.value.isNotEmpty()) {
            vibrate(6)
            _sheetStrokes.value = _sheetStrokes.value.dropLast(1)
        }
    }

    fun clearSheetStrokes() {
        if (_sheetStrokes.value.isNotEmpty()) {
            vibrate(6)
            _sheetStrokes.value = emptyList()
        }
    }

    fun selectSheetColor(hex: String) {
        vibrate(5)
        _sheetColor.value = hex
    }

    fun toggleSheetFav() {
        vibrate(8)
        _sheetFav.value = !_sheetFav.value
    }

    fun requestCloseSheet() {
        val key = _activeSheetDate.value ?: return
        val existing = entries.value[key]

        if (_sheetMode.value == SheetMode.VIEW) {
            closeSheetNow()
            return
        }

        val strings = I18n.forLanguage(language.value)
        if (existing != null) {
            // Editing existing entry: warn if dirty
            if (isSheetDirty()) {
                _confirmDialog.value = ConfirmDialogData(
                    title = strings.confirmCloseTitle,
                    message = strings.confirmCloseMsg,
                    confirmText = strings.discard,
                    cancelText = strings.keep,
                    onConfirm = {
                        dismissConfirmDialog()
                        _sheetText.value = existing.text
                        _sheetStrokes.value = existing.strokes
                        _sheetColor.value = existing.color
                        _sheetFav.value = existing.fav
                        _sheetMode.value = SheetMode.VIEW
                        _writeStep.value = 1
                        initialSnapshot = computeSnapshot()
                    },
                    onCancel = { dismissConfirmDialog() }
                )
            } else {
                _sheetMode.value = SheetMode.VIEW
                _writeStep.value = 1
            }
        } else {
            // New entry (W4): auto-save draft if has content, show draftKept, no discard dialog
            val text = _sheetText.value.trim()
            val strokes = _sheetStrokes.value
            if (text.isNotEmpty() || strokes.isNotEmpty()) {
                repository.saveDraft(
                    key,
                    EntryDraft(
                        text = _sheetText.value,
                        color = _sheetColor.value,
                        fav = _sheetFav.value,
                        strokes = strokes
                    )
                )
                showToast(strings.draftKept)
            } else {
                repository.clearDraft(key)
            }
            closeSheetNow()
        }
    }

    fun closeSheetNow() {
        _activeSheetDate.value = null
        _sheetText.value = ""
        _sheetStrokes.value = emptyList()
        _writeStep.value = 1
        initialSnapshot = ""
    }

    fun saveCurrentEntry() {
        val key = _activeSheetDate.value ?: return
        val text = _sheetText.value.trim()
        val strokes = _sheetStrokes.value

        val strings = I18n.forLanguage(language.value)
        if (text.isEmpty() && strokes.isEmpty()) {
            showToast(strings.writeSomething)
            return
        }

        val existing = entries.value[key]
        val normalized = StrokeMath.normalizeStrokes(strokes)

        val newEntry = DayEntry(
            text = text,
            color = _sheetColor.value,
            fav = _sheetFav.value,
            strokes = normalized,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val wasFirstEver = !repository.isFirstPlantCelebrated() && entries.value.isEmpty()

        repository.saveEntry(key, newEntry)
        repository.clearDraft(key)
        if (wasFirstEver) {
            repository.markFirstPlantCelebrated()
            _firstPlantCelebratedDate.value = key
            vibrate(24)
        } else {
            vibrate(14)
        }
        _plantedCellPulse.value = key
        initialSnapshot = computeSnapshot()
        closeSheetNow()
    }

    fun startPlantingCeremony(startOffset: Offset, reduceMotion: Boolean = false) {
        val key = _activeSheetDate.value ?: return
        val text = _sheetText.value.trim()
        val strokes = _sheetStrokes.value
        val strings = I18n.forLanguage(language.value)

        if (text.isEmpty() && strokes.isEmpty()) {
            showToast(strings.writeSomething)
            return
        }

        val cell = getCellBounds(key)
        if (reduceMotion || cell == null) {
            saveCurrentEntry()
            return
        }

        _plantingAnimation.value = PlantingAnimation(
            dateKey = key,
            text = text,
            colorHex = _sheetColor.value,
            fav = _sheetFav.value,
            strokes = strokes,
            startOffset = startOffset,
            targetOffset = cell.offset,
            targetSize = cell.size
        )
        closeSheetNow()
    }

    fun completePlantingCeremony() {
        val anim = _plantingAnimation.value ?: return
        val existing = entries.value[anim.dateKey]
        val normalized = StrokeMath.normalizeStrokes(anim.strokes)

        val newEntry = DayEntry(
            text = anim.text,
            color = anim.colorHex,
            fav = anim.fav,
            strokes = normalized,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val wasFirstEver = !repository.isFirstPlantCelebrated() && entries.value.isEmpty()

        repository.saveEntry(anim.dateKey, newEntry)
        repository.clearDraft(anim.dateKey)
        if (wasFirstEver) {
            repository.markFirstPlantCelebrated()
            _firstPlantCelebratedDate.value = anim.dateKey
            vibrate(24)
        } else {
            vibrate(14)
        }
        _plantedCellPulse.value = anim.dateKey
        _plantingAnimation.value = null
    }

    fun clearPlantedCellPulse() {
        _plantedCellPulse.value = null
    }

    fun deleteCurrentEntry() {
        val key = _activeSheetDate.value ?: return
        val strings = I18n.forLanguage(language.value)

        _confirmDialog.value = ConfirmDialogData(
            title = strings.confirmDeleteTitle,
            message = strings.confirmDeleteMsg,
            confirmText = strings.delYes,
            cancelText = strings.keep,
            onConfirm = {
                repository.deleteEntry(key)
                repository.clearDraft(key)
                dismissConfirmDialog()
                vibrate(10)
                closeSheetNow()
            },
            onCancel = { dismissConfirmDialog() }
        )
    }

    // --- On This Day & Capsules ---

    private fun checkOnThisDay() {
        val fromYear = getMinYear()
        for (yy in (nowYear - 1) downTo fromYear) {
            val key = formatDateKey(yy, nowMonth, nowDay)
            val entry = entries.value[key]
            if (entry != null) {
                _onThisDayHit.value = OnThisDayHit(key, yy, entry)
                return
            }
        }
        _onThisDayHit.value = null
    }

    fun dismissOnThisDay() {
        _onThisDayHit.value = null
    }

    fun checkReadyCapsule() {
        if (!ENABLE_LETTERS) {
            _readyCapsule.value = null
            return
        }
        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val ready = capsules.value.firstOrNull { !it.opened && it.unlockDate <= todayIso }
        _readyCapsule.value = ready
    }

    fun openReadyCapsule(capsule: TimeCapsule) {
        val opened = repository.markCapsuleOpened(capsule.id)
            ?: capsule.copy(opened = true, openedAt = System.currentTimeMillis())
        NotificationHelper.cancelCapsuleNotification(getApplication(), capsule.id)
        checkReadyCapsule()
        vibrate(10)
        _openedCapsuleView.value = opened
    }

    fun openArchivedCapsule(capsule: TimeCapsule) {
        vibrate(8)
        _openedCapsuleView.value = capsule
    }

    fun closeOpenedCapsuleView() {
        _openedCapsuleView.value = null
    }

    fun writeBackFromOpenedCapsule() {
        _openedCapsuleView.value = null
        selectTab(Tab.LETTERS)
        openCapsuleCompose()
    }

    fun dismissReadyCapsule() {
        _readyCapsule.value = null
    }

    // --- Tabs, Gallery & Wrap (NV1, NV2) ---

    fun selectTab(tab: Tab) {
        if (tab == Tab.LETTERS && !ENABLE_LETTERS) return
        if (_currentTab.value != tab) {
            vibrate(6)
            if (tab == Tab.DRAW) {
                pickNextSketchWhisper()
            }
            _currentTab.value = tab
        } else if (tab == Tab.PAGE && (_currentYear.value != nowYear || _currentMonth.value != nowMonth)) {
            vibrate(6)
            _currentYear.value = nowYear
            _currentMonth.value = nowMonth
        }
        _isWrapOpen.value = false
        _isSettingsOpen.value = false
        _isExportSheetOpen.value = false
        _isCapsuleComposeOpen.value = false
    }

    fun openGallery() {
        selectTab(Tab.GARDEN)
    }

    fun closeGallery() {
        selectTab(Tab.PAGE)
    }

    fun openWrap() {
        vibrate(6)
        _isWrapOpen.value = true
    }

    fun closeWrap() {
        _isWrapOpen.value = false
    }

    fun getYearWrapData(year: Int): YearWrapData {
        val yearEntries = entries.value.filter { it.key.startsWith("$year-") }
        val count = yearEntries.size
        val favCount = yearEntries.values.count { it.fav }

        val colorCounts = mutableMapOf<String, Int>()
        yearEntries.values.forEach { e ->
            colorCounts[e.color] = (colorCounts[e.color] ?: 0) + 1
        }
        val topColor = colorCounts.maxByOrNull { it.value }?.key ?: DEFAULT_GREEN_HEX

        val sortedKeys = yearEntries.keys.sorted()
        var bestStreak = 0
        var currentStreak = 0
        var prevDate: Date? = null
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        for (k in sortedKeys) {
            val d = sdf.parse(k)
            if (d != null) {
                if (prevDate != null && (d.time - prevDate.time) == 86400000L) {
                    currentStreak++
                } else {
                    currentStreak = 1
                }
                if (currentStreak > bestStreak) bestStreak = currentStreak
                prevDate = d
            }
        }

        return YearWrapData(
            year = year,
            count = count,
            favCount = favCount,
            topColor = topColor,
            bestStreak = bestStreak,
            keys = sortedKeys
        )
    }

    // --- Time Capsule / Letters Page (CP1–CP10 & LC1–LC5) ---

    fun computePresetDateKey(preset: CapsulePreset): String {
        val cal = Calendar.getInstance()
        when (preset) {
            CapsulePreset.TOMORROW -> cal.add(Calendar.DAY_OF_YEAR, 1)
            CapsulePreset.WEEK_1 -> cal.add(Calendar.DAY_OF_YEAR, 7)
            CapsulePreset.MONTH_1 -> cal.add(Calendar.MONTH, 1)
            CapsulePreset.MONTH_3 -> cal.add(Calendar.MONTH, 3)
            CapsulePreset.MONTH_6 -> cal.add(Calendar.MONTH, 6)
            CapsulePreset.YEAR_1 -> cal.add(Calendar.YEAR, 1)
            CapsulePreset.CUSTOM -> cal.add(Calendar.MONTH, 1)
        }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    private fun buildCapsuleSnapshot(
        title: String,
        body: String,
        preset: CapsulePreset,
        unlockDate: String,
        strokes: List<List<Point>>,
        color: String
    ): String {
        return "${title.trim()}|${body.trim()}|${preset.name}|$unlockDate|${strokes.size}|$color"
    }

    private fun persistCapsuleDraftNow() {
        val draft = CapsuleDraft(
            title = liveCapsuleTitle,
            body = liveCapsuleBody,
            presetName = _capsulePreset.value.name,
            unlockDate = _capsuleUnlockDate.value,
            strokes = _capsuleDoodleStrokes.value,
            color = _capsuleDoodleColor.value,
            doodleVisible = _capsuleDoodleVisible.value,
            updatedAt = System.currentTimeMillis()
        )
        repository.saveCapsuleDraft(draft)
    }

    /**
     * LC2 & LC5: Debounced autosave (800 ms) of title, body, date choice, and attachment
     * to SharedPreferences key `capsule_draft`.
     */
    private fun scheduleCapsuleDraftAutosave() {
        if (_capsuleDraftStatus.value != CapsuleDraftStatus.SAVING) {
            _capsuleDraftStatus.value = CapsuleDraftStatus.SAVING
        }
        capsuleDraftAutosaveJob?.cancel()
        capsuleDraftAutosaveJob = viewModelScope.launch {
            kotlinx.coroutines.delay(800L)
            _capsuleTitleText.value = liveCapsuleTitle
            _capsuleDraftText.value = liveCapsuleBody
            persistCapsuleDraftNow()
            _capsuleDraftStatus.value = CapsuleDraftStatus.SAVED
        }
    }

    fun openCapsulePage() {
        selectTab(Tab.LETTERS)
    }

    fun closeCapsulePage() {
        selectTab(Tab.PAGE)
    }

    fun openCapsuleCompose() {
        vibrate(6)
        capsuleDraftAutosaveJob?.cancel()
        _openedCapsuleView.value = null
        _isCapsuleBackSheetOpen.value = false
        _isCapsuleSealSheetOpen.value = false

        val savedDraft = repository.getCapsuleDraft()
        if (savedDraft != null && (
                savedDraft.title.isNotBlank() ||
                    savedDraft.body.isNotBlank() ||
                    savedDraft.strokes.isNotEmpty() ||
                    savedDraft.unlockDate.isNotBlank()
                )
        ) {
            liveCapsuleTitle = savedDraft.title
            liveCapsuleBody = savedDraft.body
            _capsuleTitleText.value = savedDraft.title
            _capsuleDraftText.value = savedDraft.body
            _capsuleHasBodyText.value = savedDraft.body.trim().isNotEmpty()
            val preset = runCatching { CapsulePreset.valueOf(savedDraft.presetName) }
                .getOrDefault(CapsulePreset.MONTH_1)
            _capsulePreset.value = preset
            _capsuleUnlockDate.value = savedDraft.unlockDate.ifBlank { computePresetDateKey(preset) }
            _capsuleDoodleStrokes.value = savedDraft.strokes
            _capsuleDoodleColor.value = savedDraft.color.ifBlank { DEFAULT_GREEN_HEX }
            _capsuleDoodleVisible.value = savedDraft.doodleVisible || savedDraft.strokes.isNotEmpty()
            _capsuleDraftStatus.value = CapsuleDraftStatus.SAVED
            initialCapsuleSnapshot = "" // Restored draft triggers Keep/Discard/Continue sheet on back
        } else {
            val defaultPreset = CapsulePreset.MONTH_1
            val defaultUnlock = computePresetDateKey(defaultPreset)
            liveCapsuleTitle = ""
            liveCapsuleBody = ""
            _capsuleTitleText.value = ""
            _capsuleDraftText.value = ""
            _capsuleHasBodyText.value = false
            _capsulePreset.value = defaultPreset
            _capsuleUnlockDate.value = defaultUnlock
            _capsuleDoodleStrokes.value = emptyList()
            _capsuleDoodleColor.value = DEFAULT_GREEN_HEX
            _capsuleDoodleVisible.value = false
            _capsuleDraftStatus.value = CapsuleDraftStatus.IDLE
            initialCapsuleSnapshot = buildCapsuleSnapshot(
                title = "",
                body = "",
                preset = defaultPreset,
                unlockDate = defaultUnlock,
                strokes = emptyList(),
                color = DEFAULT_GREEN_HEX
            )
        }
        _capsuleComposeSessionId.value += 1
        _isCapsuleComposeOpen.value = true
    }

    /**
     * LC2: On back with unsaved changes (or active draft), show bottom sheet:
     * Keep draft · Discard · Continue writing.
     */
    fun requestCloseCapsuleCompose() {
        if (_isCapsuleSealSheetOpen.value) {
            _isCapsuleSealSheetOpen.value = false
            return
        }
        if (_isCapsuleBackSheetOpen.value) {
            _isCapsuleBackSheetOpen.value = false
            return
        }
        val currentSnapshot = buildCapsuleSnapshot(
            title = liveCapsuleTitle,
            body = liveCapsuleBody,
            preset = _capsulePreset.value,
            unlockDate = _capsuleUnlockDate.value,
            strokes = _capsuleDoodleStrokes.value,
            color = _capsuleDoodleColor.value
        )
        val hasContent = liveCapsuleTitle.isNotBlank() ||
            liveCapsuleBody.isNotBlank() ||
            _capsuleDoodleStrokes.value.isNotEmpty()
        val isDirty = hasContent || currentSnapshot != initialCapsuleSnapshot || repository.hasCapsuleDraft()

        if (isDirty) {
            vibrate(6)
            _isCapsuleBackSheetOpen.value = true
        } else {
            capsuleDraftAutosaveJob?.cancel()
            _isCapsuleComposeOpen.value = false
        }
    }

    fun closeCapsuleCompose() {
        requestCloseCapsuleCompose()
    }

    fun keepCapsuleDraftAndClose() {
        vibrate(6)
        capsuleDraftAutosaveJob?.cancel()
        _capsuleTitleText.value = liveCapsuleTitle
        _capsuleDraftText.value = liveCapsuleBody
        persistCapsuleDraftNow()
        _capsuleDraftStatus.value = CapsuleDraftStatus.SAVED
        _isCapsuleBackSheetOpen.value = false
        _isCapsuleComposeOpen.value = false
        showToast(I18n.forLanguage(language.value).draftKept)
    }

    fun discardCapsuleDraftAndClose() {
        vibrate(8)
        capsuleDraftAutosaveJob?.cancel()
        repository.clearCapsuleDraft()
        liveCapsuleTitle = ""
        liveCapsuleBody = ""
        _capsuleTitleText.value = ""
        _capsuleDraftText.value = ""
        _capsuleHasBodyText.value = false
        _capsuleDoodleStrokes.value = emptyList()
        _capsuleDoodleVisible.value = false
        _capsuleDraftStatus.value = CapsuleDraftStatus.IDLE
        _isCapsuleBackSheetOpen.value = false
        _isCapsuleComposeOpen.value = false
    }

    fun continueWritingCapsuleDraft() {
        vibrate(5)
        _isCapsuleBackSheetOpen.value = false
    }

    /**
     * LC5: Holds title in local field state and hoists to StateFlow only on 800ms debounce.
     */
    fun onCapsuleTitleInputChanged(newTitle: String) {
        if (liveCapsuleTitle == newTitle) return
        liveCapsuleTitle = newTitle
        scheduleCapsuleDraftAutosave()
    }

    /**
     * LC5: Holds body text in local field state and hoists to StateFlow only on 800ms debounce.
     * Only flips `_capsuleHasBodyText` when empty/non-empty changes so the screen never recomposes per keystroke.
     */
    fun onCapsuleBodyInputChanged(newBody: String) {
        if (liveCapsuleBody == newBody) return
        liveCapsuleBody = newBody
        val hasBody = newBody.trim().isNotEmpty()
        if (_capsuleHasBodyText.value != hasBody) {
            _capsuleHasBodyText.value = hasBody
        }
        scheduleCapsuleDraftAutosave()
    }

    fun updateCapsuleDraftText(text: String) {
        onCapsuleBodyInputChanged(text)
    }

    fun toggleCapsuleDoodle() {
        vibrate(5)
        _capsuleDoodleVisible.value = !_capsuleDoodleVisible.value
        scheduleCapsuleDraftAutosave()
    }

    fun addCapsuleDoodleStroke(stroke: List<Point>) {
        if (stroke.isNotEmpty()) {
            _capsuleDoodleStrokes.value = _capsuleDoodleStrokes.value + listOf(stroke)
            scheduleCapsuleDraftAutosave()
        }
    }

    fun undoCapsuleDoodleStroke() {
        if (_capsuleDoodleStrokes.value.isNotEmpty()) {
            vibrate(6)
            _capsuleDoodleStrokes.value = _capsuleDoodleStrokes.value.dropLast(1)
            scheduleCapsuleDraftAutosave()
        }
    }

    fun clearCapsuleDoodleStrokes() {
        if (_capsuleDoodleStrokes.value.isNotEmpty()) {
            vibrate(6)
            _capsuleDoodleStrokes.value = emptyList()
            scheduleCapsuleDraftAutosave()
        }
    }

    fun selectCapsuleDoodleColor(hex: String) {
        vibrate(5)
        _capsuleDoodleColor.value = hex
        scheduleCapsuleDraftAutosave()
    }

    fun selectCapsulePreset(preset: CapsulePreset) {
        vibrate(5)
        _capsulePreset.value = preset
        if (preset != CapsulePreset.CUSTOM) {
            _capsuleUnlockDate.value = computePresetDateKey(preset)
        }
        scheduleCapsuleDraftAutosave()
    }

    fun setCustomCapsuleDate(dateKey: String) {
        vibrate(5)
        _capsulePreset.value = CapsulePreset.CUSTOM
        _capsuleUnlockDate.value = dateKey
        scheduleCapsuleDraftAutosave()
    }

    fun onLockedCapsuleTap(capsule: TimeCapsule) {
        val daysLeft = I18n.daysUntilDateKey(capsule.unlockDate, getTodayKey())
        val strings = I18n.forLanguage(language.value)
        vibrate(6)
        showToast(strings.capsuleNotYetToast.format(language.value, daysLeft))
    }

    fun requestDeleteCapsule(capsuleId: String) {
        val strings = I18n.forLanguage(language.value)
        _confirmDialog.value = ConfirmDialogData(
            title = strings.capsuleDeleteTitle,
            message = strings.capsuleDeleteMsg,
            confirmText = strings.delYes,
            cancelText = strings.keep,
            isDestructive = true,
            onConfirm = {
                deleteCapsuleNow(capsuleId)
                dismissConfirmDialog()
            },
            onCancel = { dismissConfirmDialog() }
        )
    }

    fun deleteCapsuleNow(capsuleId: String) {
        repository.deleteCapsule(capsuleId)
        NotificationHelper.cancelCapsuleNotification(getApplication(), capsuleId)
        if (_openedCapsuleView.value?.id == capsuleId) {
            _openedCapsuleView.value = null
        }
        if (_highlightedCapsuleId.value == capsuleId) {
            _highlightedCapsuleId.value = null
        }
        checkReadyCapsule()
        vibrate(10)
    }

    /**
     * LC3: Tapping Seal opens the one-step confirm sheet showing the envelope preview
     * (title + opens-on sentence), warning copy, and "Seal" / "Not yet".
     */
    fun requestSealCapsule() {
        val strings = I18n.forLanguage(language.value)
        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val text = liveCapsuleBody.trim()
        val derivedTitle = text.replace("\n", " ").trim().take(28)
        _capsuleTitleText.value = derivedTitle
        _capsuleDraftText.value = liveCapsuleBody
        val unlockDate = _capsuleUnlockDate.value

        if (text.isBlank()) {
            showToast(strings.capsuleHelperWriteFirst)
            return
        }
        if (unlockDate.isBlank() || unlockDate <= todayIso) {
            showToast(strings.capsuleHelperChooseDate)
            return
        }

        vibrate(6)
        _isCapsuleSealSheetOpen.value = true
    }

    fun closeCapsuleSealSheet() {
        vibrate(5)
        _isCapsuleSealSheetOpen.value = false
    }

    fun confirmSealCapsule(onConfirmedPermissionCheck: () -> Unit = {}) {
        _isCapsuleSealSheetOpen.value = false
        capsuleDraftAutosaveJob?.cancel()
        repository.clearCapsuleDraft()
        _capsuleDraftStatus.value = CapsuleDraftStatus.IDLE
        onConfirmedPermissionCheck()
        sealDraftCapsuleWithCeremony()
    }

    fun sealDraftCapsuleWithCeremony() {
        val text = liveCapsuleBody.trim()
        val title = text.replace("\n", " ").trim().take(28)
        val unlockDate = _capsuleUnlockDate.value
        val strokes = _capsuleDoodleStrokes.value
        val color = _capsuleDoodleColor.value

        val capsule = saveCapsule(
            text = text,
            unlockDate = unlockDate,
            strokes = strokes,
            color = color,
            title = title
        ) ?: return
        _isSealingCeremonyActive.value = true
        _highlightedCapsuleId.value = capsule.id
    }

    fun completeSealingCeremony() {
        _isSealingCeremonyActive.value = false
        _isCapsuleComposeOpen.value = false
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000L)
            _highlightedCapsuleId.value = null
        }
    }

    fun saveCapsule(
        text: String,
        unlockDate: String,
        strokes: List<List<Point>> = emptyList(),
        color: String = DEFAULT_GREEN_HEX,
        title: String = ""
    ): TimeCapsule? {
        val strings = I18n.forLanguage(language.value)
        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        if (text.isBlank()) {
            showToast(strings.writeSomething)
            return null
        }
        if (unlockDate.isBlank() || unlockDate <= todayIso) {
            showToast(strings.capsuleDateError)
            return null
        }

        val normalizedStrokes = if (strokes.isNotEmpty()) {
            StrokeMath.normalizeStrokes(strokes)
        } else {
            emptyList()
        }
        val finalTitle = title.trim().ifEmpty {
            text.replace("\n", " ").trim().take(28)
        }
        val capsule = repository.addCapsule(
            text = text.trim(),
            unlockDate = unlockDate,
            strokes = normalizedStrokes,
            color = color,
            title = finalTitle
        )
        if (ENABLE_LETTERS) {
            NotificationHelper.scheduleCapsuleNotification(getApplication(), capsule)
            checkReadyCapsule()
        }
        vibrate(14)
        showToast(strings.capsuleSavedToast)
        return capsule
    }

    // --- Sketchpad Page (SK1–SK11 & JD1–JD6) ---

    fun pickNextSketchWhisper() {
        val candidates = (0..3).filter { it != lastWhisperIndex }
        val chosen = candidates.random()
        lastWhisperIndex = chosen
        _sketchWhisperIndex.value = chosen
        if (_sketchStrokes.value.isEmpty()) {
            _sketchIntroDismissed.value = false
        }
    }

    fun openSketchPage() {
        selectTab(Tab.DRAW)
    }

    fun closeSketchPage() {
        selectTab(Tab.PAGE)
    }

    private fun syncSketchUndoRedoState() {
        _canUndoSketch.value = _sketchUndoStack.isNotEmpty() || _sketchStrokes.value.isNotEmpty()
        _canRedoSketch.value = _sketchRedoStack.isNotEmpty()
    }

    fun getSketchEraserRadius(brushWidth: Float = _sketchBrushWidth.value): Float = when {
        brushWidth <= 4f -> 8f
        brushWidth <= 8f -> 14f
        else -> 24f
    }

    private fun pointToSegmentDistSq(p: Point, a: Point, b: Point): Double {
        val abx = (b.x - a.x).toDouble()
        val aby = (b.y - a.y).toDouble()
        val apx = (p.x - a.x).toDouble()
        val apy = (p.y - a.y).toDouble()
        val lenSq = abx * abx + aby * aby
        if (lenSq <= 1e-6) return apx * apx + apy * apy
        val t = ((apx * abx + apy * aby) / lenSq).coerceIn(0.0, 1.0)
        val cx = a.x + t * abx
        val cy = a.y + t * aby
        val dx = p.x - cx
        val dy = p.y - cy
        return dx * dx + dy * dy
    }

    fun onSketchStrokeStarted() {
        eraseGestureUndoPushed = false
        if (!_sketchEraserMode.value) {
            _sketchIntroDismissed.value = true
            if (_sketchClearUndoVisible.value) {
                clearUndoJob?.cancel()
                _sketchClearUndoVisible.value = false
            }
        }
    }

    fun onSketchGestureEnded() {
        eraseGestureUndoPushed = false
    }

    fun addSketchStroke(stroke: SketchStroke) {
        if (stroke.points.isNotEmpty()) {
            eraseGestureUndoPushed = false
            _sketchUndoStack.add(_sketchStrokes.value)
            _sketchRedoStack.clear()
            _sketchStrokes.value = _sketchStrokes.value + listOf(stroke)
            syncSketchUndoRedoState()
            _sketchIntroDismissed.value = true
            if (_sketchClearUndoVisible.value) {
                clearUndoJob?.cancel()
                _sketchClearUndoVisible.value = false
            }
        }
    }

    fun effectiveSketchColorHex(
        rawColor: String = _sketchColor.value,
        tool: SketchTool = _sketchTool.value
    ): String {
        val raw = rawColor.trim()
        val sixHex = if (raw.length == 9 && raw.startsWith("#")) {
            raw.takeLast(6)
        } else {
            raw.removePrefix("#").takeLast(6)
        }
        return when (tool) {
            SketchTool.PEN -> if (raw.length == 9 && raw.startsWith("#")) "#$sixHex" else raw
            SketchTool.BRUSH, SketchTool.CALLIGRAPHY, SketchTool.CRAYON -> raw
            SketchTool.MARKER -> "#61$sixHex"
            SketchTool.PENCIL -> "#D9$sixHex"
        }
    }

    fun effectiveSketchWidth(
        rawWidth: Float = _sketchBrushWidth.value,
        tool: SketchTool = _sketchTool.value
    ): Float {
        return when (tool) {
            SketchTool.PEN -> rawWidth
            SketchTool.BRUSH -> rawWidth * 1.6f
            SketchTool.CALLIGRAPHY -> rawWidth * 1.4f
            SketchTool.MARKER -> rawWidth * 2.4f
            SketchTool.PENCIL -> (rawWidth * 0.75f).coerceAtLeast(2f)
            SketchTool.CRAYON -> rawWidth * 1.3f
        }
    }

    fun addSketchStroke(points: List<Point>) {
        if (points.isNotEmpty()) {
            addSketchStroke(
                SketchStroke(
                    points = points,
                    colorHex = effectiveSketchColorHex(),
                    width = effectiveSketchWidth(),
                    tool = _sketchTool.value.name.lowercase()
                )
            )
        }
    }

    /**
     * ER1–ER5: Partial erase splitting affected strokes at points/segments within radius r
     * (Small 8, Medium 14, Large 24; segments densified every 3 units before testing).
     * Pushes one undo snapshot at the first erased point of a gesture.
     */
    fun eraseSketchAt(pt: Point) {
        val current = _sketchStrokes.value
        if (current.isEmpty()) return

        val r = getSketchEraserRadius().toDouble()
        val rSq = r * r

        // ER5: Quick check so we make no list copies if no stroke is touched
        val anyHit = current.any { stroke ->
            val pts = stroke.points
            pts.any { p ->
                val dx = (p.x - pt.x).toDouble()
                val dy = (p.y - pt.y).toDouble()
                dx * dx + dy * dy <= rSq
            } || (0 until pts.lastIndex).any { i ->
                pointToSegmentDistSq(pt, pts[i], pts[i + 1]) <= rSq
            }
        }
        if (!anyHit) return

        // ER2: Push one undo snapshot at the first erased point of a gesture
        if (!eraseGestureUndoPushed) {
            _sketchUndoStack.add(current)
            eraseGestureUndoPushed = true
            _sketchRedoStack.clear()
        }

        val updated = ArrayList<SketchStroke>(current.size + 2)
        for (stroke in current) {
            val pts = stroke.points
            val strokeHit = pts.any { p ->
                val dx = (p.x - pt.x).toDouble()
                val dy = (p.y - pt.y).toDouble()
                dx * dx + dy * dy <= rSq
            } || (0 until pts.lastIndex).any { i ->
                pointToSegmentDistSq(pt, pts[i], pts[i + 1]) <= rSq
            }

            if (!strokeHit) {
                // ER5: Reuse unaffected stroke without copying
                updated.add(stroke)
                continue
            }

            // ER1: Insert intermediate points every 3 units before testing so fast moves do not skip
            val dense = ArrayList<Point>(pts.size * 2)
            for (i in pts.indices) {
                val a = pts[i]
                dense.add(a)
                if (i < pts.lastIndex) {
                    val b = pts[i + 1]
                    val dx = b.x - a.x
                    val dy = b.y - a.y
                    val dist = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                    if (dist > 3f) {
                        val steps = kotlin.math.ceil(dist / 3f).toInt()
                        for (s in 1 until steps) {
                            val frac = s.toFloat() / steps.toFloat()
                            dense.add(
                                Point(
                                    x = ((a.x + dx * frac) * 10f).toInt() / 10f,
                                    y = ((a.y + dy * frac) * 10f).toInt() / 10f
                                )
                            )
                        }
                    }
                }
            }

            // Mark every stroke point whose distance is <= r, and every point on a segment whose closest distance is <= r
            val marked = BooleanArray(dense.size)
            for (i in dense.indices) {
                val p = dense[i]
                val dx = (p.x - pt.x).toDouble()
                val dy = (p.y - pt.y).toDouble()
                if (dx * dx + dy * dy <= rSq) {
                    marked[i] = true
                }
            }
            for (i in 0 until dense.lastIndex) {
                if ((!marked[i] || !marked[i + 1]) && pointToSegmentDistSq(pt, dense[i], dense[i + 1]) <= rSq) {
                    marked[i] = true
                    marked[i + 1] = true
                }
            }

            // Split affected stroke at marked points into contiguous runs of length >= 2 (drop 1-point runs)
            var runStart = -1
            for (i in dense.indices) {
                if (!marked[i]) {
                    if (runStart == -1) runStart = i
                } else if (runStart != -1) {
                    if (i - runStart >= 2) {
                        updated.add(
                            SketchStroke(
                                points = dense.subList(runStart, i).toList(),
                                colorHex = stroke.colorHex,
                                width = stroke.width,
                                tool = stroke.tool
                            )
                        )
                    }
                    runStart = -1
                }
            }
            if (runStart != -1 && dense.size - runStart >= 2) {
                updated.add(
                    SketchStroke(
                        points = dense.subList(runStart, dense.size).toList(),
                        colorHex = stroke.colorHex,
                        width = stroke.width,
                        tool = stroke.tool
                    )
                )
            }
        }

        _sketchStrokes.value = updated
        syncSketchUndoRedoState()
    }

    fun undoSketchStroke() {
        eraseGestureUndoPushed = false
        if (_sketchClearUndoVisible.value) {
            clearUndoJob?.cancel()
            _sketchClearUndoVisible.value = false
        }
        if (_sketchUndoStack.isNotEmpty()) {
            vibrate(6)
            _sketchRedoStack.add(_sketchStrokes.value)
            _sketchStrokes.value = _sketchUndoStack.removeAt(_sketchUndoStack.lastIndex)
            syncSketchUndoRedoState()
        } else if (_sketchStrokes.value.isNotEmpty()) {
            vibrate(6)
            _sketchRedoStack.add(_sketchStrokes.value)
            _sketchStrokes.value = _sketchStrokes.value.dropLast(1)
            syncSketchUndoRedoState()
        }
    }

    fun redoSketchStroke() {
        eraseGestureUndoPushed = false
        if (_sketchRedoStack.isNotEmpty()) {
            vibrate(6)
            _sketchUndoStack += _sketchStrokes.value
            _sketchStrokes.value = _sketchRedoStack.removeAt(_sketchRedoStack.lastIndex)
            syncSketchUndoRedoState()
        }
    }

    /**
     * JD5: Clearing is a page turn. The sheet slides out by KannachMotion.PAGE and fresh paper
     * slides in; an Undo action appears for 5 s.
     */
    fun clearSketchStrokes() {
        eraseGestureUndoPushed = false
        if (_sketchStrokes.value.isNotEmpty()) {
            vibrate(6)
            _sketchUndoStack.add(_sketchStrokes.value)
            _sketchRedoStack.clear()
            _sketchStrokes.value = emptyList()
            syncSketchUndoRedoState()
            _sketchPageTurnCount.value = _sketchPageTurnCount.value + 1
            _sketchClearUndoVisible.value = true
            clearUndoJob?.cancel()
            clearUndoJob = viewModelScope.launch {
                kotlinx.coroutines.delay(5000L)
                _sketchClearUndoVisible.value = false
            }
        }
    }

    fun requestClearSketchStrokes() {
        clearSketchStrokes()
    }

    /**
     * JD3 & ER4: Selecting a swatch returns to pen mode and shows its mood name as a 1-line label
     * above the tray for 1.2 s, then fades.
     */
    fun setSketchColor(hex: String) {
        vibrate(5)
        eraseGestureUndoPushed = false
        _sketchColor.value = hex
        _sketchEraserMode.value = false
        _sketchMoodLabelVisible.value = true
        moodLabelJob?.cancel()
        moodLabelJob = viewModelScope.launch {
            kotlinx.coroutines.delay(1200L)
            _sketchMoodLabelVisible.value = false
        }
    }

    /**
     * ER1 & ER4: The three size dots set both pen width and eraser radius (Small 8, Medium 14, Large 24).
     * Selecting a size dot keeps the current mode (pen or eraser).
     */
    fun setSketchBrushWidth(width: Float) {
        vibrate(5)
        _sketchBrushWidth.value = width
    }

    fun toggleSketchEraser() {
        vibrate(5)
        eraseGestureUndoPushed = false
        _sketchEraserMode.value = !_sketchEraserMode.value
    }

    fun setSketchEraserMode(enabled: Boolean) {
        vibrate(5)
        eraseGestureUndoPushed = false
        _sketchEraserMode.value = enabled
    }

    fun setSketchTool(tool: SketchTool) {
        _sketchTool.value = tool
        setSketchEraserMode(false)
    }

    fun setSketchPaperType(paperType: SketchPaperType) {
        vibrate(5)
        _sketchPaperType.value = paperType
    }

    fun openExportSheet() {
        val strings = I18n.forLanguage(language.value)
        if (_sketchStrokes.value.isEmpty()) {
            showToast(strings.writeSomething)
            return
        }
        vibrate(6)
        _isExportSheetOpen.value = true
    }

    fun closeExportSheet() {
        _isExportSheetOpen.value = false
    }

    fun setExportFormat(format: ExportFormat) {
        vibrate(5)
        _exportFormat.value = format
    }

    fun setExportSignature(enabled: Boolean) {
        vibrate(5)
        _exportSignature.value = enabled
        repository.updateUserSettings { it.copy(exportSignature = enabled) }
    }

    /**
     * JD6: On a successful save/share show one whisper line from drawSaved1..2 for 2.4 s. No confetti.
     */
    fun triggerSketchSavedWhisper() {
        val nextIdx = if (lastSavedWhisperIndex == 0) 1 else 0
        lastSavedWhisperIndex = nextIdx
        _sketchSavedWhisperIndex.value = nextIdx
        savedWhisperJob?.cancel()
        savedWhisperJob = viewModelScope.launch {
            kotlinx.coroutines.delay(2400L)
            _sketchSavedWhisperIndex.value = null
        }
    }

    fun saveSketchToPhotos(context: Context, palette: KannachPalette = BasePalette): android.net.Uri? {
        val strings = I18n.forLanguage(language.value)
        if (_sketchStrokes.value.isEmpty()) {
            showToast(strings.writeSomething)
            return null
        }

        return try {
            val bitmap = StrokeMath.composeExportBitmap(
                context = context,
                strokes = _sketchStrokes.value,
                format = _exportFormat.value,
                paperType = _sketchPaperType.value,
                includeSignature = _exportSignature.value,
                language = language.value,
                palette = palette
            )
            val uri = StrokeMath.saveExportToMediaStore(context, bitmap)
            bitmap.recycle()
            if (uri != null) {
                vibrate(12)
                _isExportSheetOpen.value = false
                triggerSketchSavedWhisper()
            } else {
                showToast(strings.saveError)
            }
            uri
        } catch (e: Exception) {
            showToast(strings.saveError)
            null
        }
    }

    fun shareSketch(context: Context, palette: KannachPalette = BasePalette) {
        val strings = I18n.forLanguage(language.value)
        if (_sketchStrokes.value.isEmpty()) {
            showToast(strings.writeSomething)
            return
        }

        try {
            val bitmap = StrokeMath.composeExportBitmap(
                context = context,
                strokes = _sketchStrokes.value,
                format = _exportFormat.value,
                paperType = _sketchPaperType.value,
                includeSignature = _exportSignature.value,
                language = language.value,
                palette = palette
            )
            val file = StrokeMath.saveExportBitmapAsPngFile(context, bitmap)
            bitmap.recycle()
            val uri = StrokeMath.getShareableUri(context, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, strings.exportTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            _isExportSheetOpen.value = false
            context.startActivity(chooser)
            triggerSketchSavedWhisper()
        } catch (e: Exception) {
            showToast(strings.saveError)
        }
    }

    // --- Settings, Backup & Language (Phase 6 Settings v2) ---

    fun openSettings() {
        vibrate(6)
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun setLanguage(lang: String) {
        vibrate(6)
        repository.setLanguage(lang)
    }

    fun toggleReminder(enabled: Boolean) {
        vibrate(6)
        val current = reminder.value
        val updated = current.copy(enabled = enabled)
        repository.saveReminder(updated)
        if (enabled) {
            NotificationHelper.scheduleDailyReminder(getApplication(), updated.time)
        } else {
            NotificationHelper.cancelDailyReminder(getApplication())
        }
    }

    fun setReminderTime(timeStr: String) {
        val current = reminder.value
        val updated = current.copy(time = timeStr)
        repository.saveReminder(updated)
        if (updated.enabled) {
            NotificationHelper.scheduleDailyReminder(getApplication(), timeStr)
        }
    }

    fun toggleReminderDay(dayOfWeek: Int) {
        if (dayOfWeek !in 1..7) return
        vibrate(5)
        val currentDays = userSettings.value.reminderDays
        val updatedDays = if (currentDays.contains(dayOfWeek)) {
            if (currentDays.size > 1) currentDays - dayOfWeek else currentDays
        } else {
            currentDays + dayOfWeek
        }
        repository.updateUserSettings { it.copy(reminderDays = updatedDays) }
        if (reminder.value.enabled) {
            NotificationHelper.scheduleDailyReminder(getApplication(), reminder.value.time)
        }
    }

    fun sendTestNudge(context: Context): String {
        vibrate(8)
        val message = NotificationHelper.sendTestNudge(context)
        showToast(message)
        return message
    }

    fun setWeekStartsOn(dayOfWeek: Int?) {
        vibrate(5)
        repository.updateUserSettings { it.copy(weekStartsOn = dayOfWeek) }
    }

    fun setUseArabicIndicNumerals(enabled: Boolean) {
        vibrate(5)
        repository.updateUserSettings { it.copy(useArabicIndicNumerals = enabled) }
    }

    fun setHandwritingSize(size: String) {
        if (size !in listOf("small", "normal", "large")) return
        vibrate(5)
        repository.updateUserSettings { it.copy(handwritingSize = size) }
    }

    fun setSeasonOverride(season: String) {
        if (season !in listOf("auto", "winter", "spring", "summer", "autumn")) return
        vibrate(5)
        repository.updateUserSettings { it.copy(seasonOverride = season) }
    }

    fun setReduceMotion(mode: String) {
        if (mode !in listOf("system", "on")) return
        vibrate(5)
        repository.updateUserSettings { it.copy(reduceMotion = mode) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        repository.updateUserSettings { it.copy(hapticsEnabled = enabled) }
        if (enabled) {
            vibrate(6)
        }
    }

    fun setHideInRecents(enabled: Boolean) {
        vibrate(5)
        repository.updateUserSettings { it.copy(hideInRecents = enabled) }
    }

    fun setCopyReminder(mode: String) {
        if (mode !in listOf("off", "monthly")) return
        vibrate(5)
        repository.updateUserSettings { it.copy(copyReminder = mode) }
        if (mode == "monthly") {
            NotificationHelper.scheduleMonthlyCopyReminder(getApplication())
        } else {
            NotificationHelper.cancelMonthlyCopyReminder(getApplication())
        }
    }

    fun recordBackupSaved(timestamp: Long = System.currentTimeMillis()) {
        repository.updateUserSettings { it.copy(lastBackupTimestamp = timestamp) }
    }

    fun exportBackup(context: Context) {
        val strings = I18n.forLanguage(language.value)
        try {
            val file = repository.exportBackupToFile()
            recordBackupSaved(System.currentTimeMillis())
            val uri = StrokeMath.getShareableUri(context, file)

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, strings.exportBtn).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            showToast(strings.exportedToast)
        } catch (e: Exception) {
            showToast(strings.saveError)
        }
    }

    fun importBackup(jsonString: String) {
        val strings = I18n.forLanguage(language.value)
        val result = repository.restoreFromBackupJson(jsonString)
        if (result.isSuccess) {
            checkOnThisDay()
            checkReadyCapsule()
            vibrate(14)
            showToast(strings.importedToast)
        } else {
            showToast(strings.importError)
        }
    }

    fun replayWelcome() {
        vibrate(6)
        _isSettingsOpen.value = false
        _isFirstRun.value = true
    }

    fun shareAppWithFriend(context: Context) {
        vibrate(6)
        val strings = I18n.forLanguage(language.value)
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, strings.settingsTellFriendMsg)
            }
            val chooser = Intent.createChooser(shareIntent, strings.settingsTellFriend).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {}
    }

    private fun formatDateKey(year: Int, month: Int, day: Int): String {
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    }
}
