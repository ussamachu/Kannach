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
    WEEK_1, MONTH_1, MONTH_3, MONTH_6, YEAR_1, CUSTOM
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

    // --- Sketchpad State (SK1–SK8) ---
    private val _sketchStrokes = MutableStateFlow<List<SketchStroke>>(emptyList())
    val sketchStrokes: StateFlow<List<SketchStroke>> = _sketchStrokes.asStateFlow()

    private val _sketchUndoStack = mutableListOf<List<SketchStroke>>()

    private val _sketchColor = MutableStateFlow(DEFAULT_GREEN_HEX)
    val sketchColor: StateFlow<String> = _sketchColor.asStateFlow()

    private val _sketchBrushWidth = MutableStateFlow(8f)
    val sketchBrushWidth: StateFlow<Float> = _sketchBrushWidth.asStateFlow()

    private val _sketchEraserMode = MutableStateFlow(false)
    val sketchEraserMode: StateFlow<Boolean> = _sketchEraserMode.asStateFlow()

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

    // --- Letters / Time Capsule State (CP1–CP10) ---
    private val _isCapsuleComposeOpen = MutableStateFlow(false)
    val isCapsuleComposeOpen: StateFlow<Boolean> = _isCapsuleComposeOpen.asStateFlow()

    private val _capsuleDraftText = MutableStateFlow("")
    val capsuleDraftText: StateFlow<String> = _capsuleDraftText.asStateFlow()

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
        checkReadyCapsule()
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
        if (_currentTab.value != tab) {
            vibrate(6)
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

    // --- Time Capsule / Letters Page (CP1–CP10) ---

    fun computePresetDateKey(preset: CapsulePreset): String {
        val cal = Calendar.getInstance()
        when (preset) {
            CapsulePreset.WEEK_1 -> cal.add(Calendar.DAY_OF_YEAR, 7)
            CapsulePreset.MONTH_1 -> cal.add(Calendar.MONTH, 1)
            CapsulePreset.MONTH_3 -> cal.add(Calendar.MONTH, 3)
            CapsulePreset.MONTH_6 -> cal.add(Calendar.MONTH, 6)
            CapsulePreset.YEAR_1 -> cal.add(Calendar.YEAR, 1)
            CapsulePreset.CUSTOM -> cal.add(Calendar.MONTH, 1)
        }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    fun openCapsulePage() {
        selectTab(Tab.LETTERS)
    }

    fun closeCapsulePage() {
        selectTab(Tab.PAGE)
    }

    fun openCapsuleCompose() {
        vibrate(6)
        _openedCapsuleView.value = null
        val strings = I18n.forLanguage(language.value)
        _capsuleDraftText.value = "${strings.capsulePrefill}\n"
        _capsuleDoodleVisible.value = false
        _capsuleDoodleStrokes.value = emptyList()
        _capsuleDoodleColor.value = DEFAULT_GREEN_HEX
        _capsulePreset.value = CapsulePreset.MONTH_1
        _capsuleUnlockDate.value = computePresetDateKey(CapsulePreset.MONTH_1)
        _isCapsuleComposeOpen.value = true
    }

    fun closeCapsuleCompose() {
        _isCapsuleComposeOpen.value = false
    }

    fun updateCapsuleDraftText(text: String) {
        _capsuleDraftText.value = text
    }

    fun toggleCapsuleDoodle() {
        vibrate(5)
        _capsuleDoodleVisible.value = !_capsuleDoodleVisible.value
    }

    fun addCapsuleDoodleStroke(stroke: List<Point>) {
        if (stroke.isNotEmpty()) {
            _capsuleDoodleStrokes.value = _capsuleDoodleStrokes.value + listOf(stroke)
        }
    }

    fun undoCapsuleDoodleStroke() {
        if (_capsuleDoodleStrokes.value.isNotEmpty()) {
            vibrate(6)
            _capsuleDoodleStrokes.value = _capsuleDoodleStrokes.value.dropLast(1)
        }
    }

    fun clearCapsuleDoodleStrokes() {
        if (_capsuleDoodleStrokes.value.isNotEmpty()) {
            vibrate(6)
            _capsuleDoodleStrokes.value = emptyList()
        }
    }

    fun selectCapsuleDoodleColor(hex: String) {
        vibrate(5)
        _capsuleDoodleColor.value = hex
    }

    fun selectCapsulePreset(preset: CapsulePreset) {
        vibrate(5)
        _capsulePreset.value = preset
        if (preset != CapsulePreset.CUSTOM) {
            _capsuleUnlockDate.value = computePresetDateKey(preset)
        }
    }

    fun setCustomCapsuleDate(dateKey: String) {
        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        if (dateKey <= todayIso) {
            showToast(I18n.forLanguage(language.value).capsuleDateError)
            return
        }
        vibrate(5)
        _capsulePreset.value = CapsulePreset.CUSTOM
        _capsuleUnlockDate.value = dateKey
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
     * CP7: Validates the draft letter and opens the confirmation dialog:
     * "Once sealed, you won't see it until {date}." (cancel = "Keep writing").
     */
    fun requestSealCapsule(onConfirmedPermissionCheck: () -> Unit = {}) {
        val strings = I18n.forLanguage(language.value)
        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val text = _capsuleDraftText.value.trim()
        val unlockDate = _capsuleUnlockDate.value

        if (text.isBlank()) {
            showToast(strings.writeSomething)
            return
        }
        if (unlockDate.isBlank() || unlockDate <= todayIso) {
            showToast(strings.capsuleDateError)
            return
        }

        val longDate = I18n.formatLongDate(unlockDate, language.value)
        _confirmDialog.value = ConfirmDialogData(
            title = strings.capsuleSaveBtn,
            message = strings.capsuleSealConfirmMsg.replace("{d}", longDate),
            confirmText = strings.capsuleSaveBtn,
            cancelText = strings.capsuleKeepWriting,
            isDestructive = false,
            onConfirm = {
                dismissConfirmDialog()
                onConfirmedPermissionCheck()
                sealDraftCapsuleWithCeremony()
            },
            onCancel = { dismissConfirmDialog() }
        )
    }

    fun sealDraftCapsuleWithCeremony() {
        val text = _capsuleDraftText.value.trim()
        val unlockDate = _capsuleUnlockDate.value
        val strokes = _capsuleDoodleStrokes.value
        val color = _capsuleDoodleColor.value

        val capsule = saveCapsule(text, unlockDate, strokes, color) ?: return
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
        color: String = DEFAULT_GREEN_HEX
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
        val capsule = repository.addCapsule(text.trim(), unlockDate, normalizedStrokes, color)
        NotificationHelper.scheduleCapsuleNotification(getApplication(), capsule)
        checkReadyCapsule()
        vibrate(14)
        showToast(strings.capsuleSavedToast)
        return capsule
    }

    // --- Sketchpad Page (SK1–SK11) ---

    fun openSketchPage() {
        selectTab(Tab.DRAW)
    }

    fun closeSketchPage() {
        selectTab(Tab.PAGE)
    }

    fun addSketchStroke(stroke: SketchStroke) {
        if (stroke.points.isNotEmpty()) {
            _sketchUndoStack.add(_sketchStrokes.value)
            _sketchStrokes.value = _sketchStrokes.value + listOf(stroke)
            _sketchIntroDismissed.value = true
        }
    }

    fun addSketchStroke(points: List<Point>) {
        if (points.isNotEmpty()) {
            addSketchStroke(
                SketchStroke(
                    points = points,
                    colorHex = _sketchColor.value,
                    width = _sketchBrushWidth.value
                )
            )
        }
    }

    /**
     * SK3: Eraser mode removes a stroke when a touch point comes within 14 canvas units
     * of any of its points. One undo step restores it.
     */
    fun eraseSketchAt(pt: Point) {
        val current = _sketchStrokes.value
        if (current.isEmpty()) return

        val remaining = current.filterNot { stroke ->
            stroke.points.any { p ->
                Math.hypot((p.x - pt.x).toDouble(), (p.y - pt.y).toDouble()) <= 14.0
            }
        }
        if (remaining.size != current.size) {
            vibrate(5)
            _sketchUndoStack.add(current)
            _sketchStrokes.value = remaining
        }
    }

    fun undoSketchStroke() {
        if (_sketchUndoStack.isNotEmpty()) {
            vibrate(6)
            _sketchStrokes.value = _sketchUndoStack.removeAt(_sketchUndoStack.lastIndex)
        } else if (_sketchStrokes.value.isNotEmpty()) {
            vibrate(6)
            _sketchStrokes.value = _sketchStrokes.value.dropLast(1)
        }
    }

    fun clearSketchStrokes() {
        if (_sketchStrokes.value.isNotEmpty()) {
            vibrate(6)
            _sketchUndoStack.add(_sketchStrokes.value)
            _sketchStrokes.value = emptyList()
        }
    }

    fun requestClearSketchStrokes() {
        if (_sketchStrokes.value.isEmpty()) return
        val strings = I18n.forLanguage(language.value)
        _confirmDialog.value = ConfirmDialogData(
            title = strings.clear,
            message = strings.confirmDeleteMsg,
            confirmText = strings.clear,
            cancelText = strings.keep,
            onConfirm = {
                dismissConfirmDialog()
                clearSketchStrokes()
            },
            onCancel = { dismissConfirmDialog() }
        )
    }

    fun setSketchColor(hex: String) {
        vibrate(5)
        _sketchColor.value = hex
        _sketchEraserMode.value = false
    }

    fun setSketchBrushWidth(width: Float) {
        vibrate(5)
        _sketchBrushWidth.value = width
        _sketchEraserMode.value = false
    }

    fun toggleSketchEraser() {
        vibrate(5)
        _sketchEraserMode.value = !_sketchEraserMode.value
    }

    fun setSketchEraserMode(enabled: Boolean) {
        vibrate(5)
        _sketchEraserMode.value = enabled
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
                showToast(strings.exportSaved)
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
            showToast(strings.sketchExported)
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
