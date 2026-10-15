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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class SheetMode { VIEW, EDIT }

data class OnThisDayHit(
    val key: String,
    val year: Int,
    val entry: DayEntry
)

data class ConfirmDialogData(
    val title: String,
    val message: String,
    val confirmText: String,
    val cancelText: String,
    val onConfirm: () -> Unit,
    val onCancel: () -> Unit
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

    // --- Navigation & Overlays ---
    private val _activeSheetDate = MutableStateFlow<String?>(null)
    val activeSheetDate: StateFlow<String?> = _activeSheetDate.asStateFlow()

    private val _sheetMode = MutableStateFlow(SheetMode.VIEW)
    val sheetMode: StateFlow<SheetMode> = _sheetMode.asStateFlow()

    private val _isGalleryOpen = MutableStateFlow(false)
    val isGalleryOpen: StateFlow<Boolean> = _isGalleryOpen.asStateFlow()

    private val _isWrapOpen = MutableStateFlow(false)
    val isWrapOpen: StateFlow<Boolean> = _isWrapOpen.asStateFlow()

    private val _isCapsulePageOpen = MutableStateFlow(false)
    val isCapsulePageOpen: StateFlow<Boolean> = _isCapsulePageOpen.asStateFlow()

    private val _isSketchPageOpen = MutableStateFlow(false)
    val isSketchPageOpen: StateFlow<Boolean> = _isSketchPageOpen.asStateFlow()

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

    private var initialSnapshot: String = ""

    // --- Sketchpad State ---
    private val _sketchStrokes = MutableStateFlow<List<List<Point>>>(emptyList())
    val sketchStrokes: StateFlow<List<List<Point>>> = _sketchStrokes.asStateFlow()

    private val _sketchColor = MutableStateFlow(DEFAULT_GREEN_HEX)
    val sketchColor: StateFlow<String> = _sketchColor.asStateFlow()

    // --- Banners & Feedback ---
    private val _onThisDayHit = MutableStateFlow<OnThisDayHit?>(null)
    val onThisDayHit: StateFlow<OnThisDayHit?> = _onThisDayHit.asStateFlow()

    private val _readyCapsule = MutableStateFlow<TimeCapsule?>(null)
    val readyCapsule: StateFlow<TimeCapsule?> = _readyCapsule.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _confirmDialog = MutableStateFlow<ConfirmDialogData?>(null)
    val confirmDialog: StateFlow<ConfirmDialogData?> = _confirmDialog.asStateFlow()

    init {
        checkOnThisDay()
        checkReadyCapsule()
    }

    fun vibrate(durationMs: Long = 8) {
        val app = getApplication<Application>()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
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

        val existing = entries.value[dateKey]
        if (existing != null) {
            _sheetText.value = existing.text
            _sheetStrokes.value = existing.strokes
            _sheetColor.value = existing.color
            _sheetFav.value = existing.fav
            _sheetMode.value = SheetMode.VIEW
            initialSnapshot = computeSnapshot()
        } else {
            _sheetText.value = ""
            _sheetStrokes.value = emptyList()
            _sheetColor.value = DEFAULT_GREEN_HEX
            _sheetFav.value = false
            _sheetMode.value = SheetMode.EDIT
            initialSnapshot = computeSnapshot()
        }
    }

    fun switchToEditMode() {
        vibrate(6)
        _sheetMode.value = SheetMode.EDIT
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
        if (isSheetDirty()) {
            _confirmDialog.value = ConfirmDialogData(
                title = strings.confirmCloseTitle,
                message = strings.confirmCloseMsg,
                confirmText = strings.discard,
                cancelText = strings.keep,
                onConfirm = {
                    dismissConfirmDialog()
                    if (existing != null) {
                        _sheetText.value = existing.text
                        _sheetStrokes.value = existing.strokes
                        _sheetColor.value = existing.color
                        _sheetFav.value = existing.fav
                        _sheetMode.value = SheetMode.VIEW
                        initialSnapshot = computeSnapshot()
                    } else {
                        closeSheetNow()
                    }
                },
                onCancel = { dismissConfirmDialog() }
            )
        } else {
            if (existing != null) {
                _sheetMode.value = SheetMode.VIEW
            } else {
                closeSheetNow()
            }
        }
    }

    fun closeSheetNow() {
        _activeSheetDate.value = null
        _sheetText.value = ""
        _sheetStrokes.value = emptyList()
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

        repository.saveEntry(key, newEntry)
        vibrate(14)
        initialSnapshot = computeSnapshot()
        closeSheetNow()
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

    private fun checkReadyCapsule() {
        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val ready = capsules.value.firstOrNull { !it.opened && it.unlockDate <= todayIso }
        _readyCapsule.value = ready
    }

    fun openReadyCapsule(capsule: TimeCapsule) {
        _readyCapsule.value = null
        repository.markCapsuleOpened(capsule.id)
        val strings = I18n.forLanguage(language.value)

        _confirmDialog.value = ConfirmDialogData(
            title = strings.capsuleTitle,
            message = capsule.text,
            confirmText = strings.ok,
            cancelText = strings.dismiss,
            onConfirm = { dismissConfirmDialog() },
            onCancel = { dismissConfirmDialog() }
        )
    }

    fun dismissReadyCapsule() {
        _readyCapsule.value = null
    }

    // --- Gallery & Wrap ---

    fun openGallery() {
        vibrate(6)
        _isGalleryOpen.value = true
    }

    fun closeGallery() {
        _isGalleryOpen.value = false
    }

    fun openWrap() {
        vibrate(6)
        _isGalleryOpen.value = false
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

    // --- Time Capsule Page ---

    fun openCapsulePage() {
        vibrate(6)
        _isCapsulePageOpen.value = true
    }

    fun closeCapsulePage() {
        _isCapsulePageOpen.value = false
    }

    fun saveCapsule(text: String, unlockDate: String) {
        val strings = I18n.forLanguage(language.value)
        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        if (text.isBlank()) {
            showToast(strings.writeSomething)
            return
        }
        if (unlockDate.isBlank() || unlockDate <= todayIso) {
            showToast(strings.capsuleDateError)
            return
        }

        repository.addCapsule(text.trim(), unlockDate)
        vibrate(14)
        showToast(strings.capsuleSavedToast)
    }

    // --- Sketchpad Page ---

    fun openSketchPage() {
        vibrate(6)
        _isSketchPageOpen.value = true
    }

    fun closeSketchPage() {
        _isSketchPageOpen.value = false
    }

    fun addSketchStroke(stroke: List<Point>) {
        if (stroke.isNotEmpty()) {
            _sketchStrokes.value = _sketchStrokes.value + listOf(stroke)
        }
    }

    fun undoSketchStroke() {
        if (_sketchStrokes.value.isNotEmpty()) {
            vibrate(6)
            _sketchStrokes.value = _sketchStrokes.value.dropLast(1)
        }
    }

    fun clearSketchStrokes() {
        if (_sketchStrokes.value.isNotEmpty()) {
            vibrate(6)
            _sketchStrokes.value = emptyList()
        }
    }

    fun setSketchColor(hex: String) {
        vibrate(5)
        _sketchColor.value = hex
    }

    fun shareSketch(context: Context) {
        val strings = I18n.forLanguage(language.value)
        if (_sketchStrokes.value.isEmpty()) {
            showToast(strings.writeSomething)
            return
        }

        try {
            val file = StrokeMath.saveStrokesAsPngFile(
                context = context,
                strokes = _sketchStrokes.value,
                colorHex = _sketchColor.value
            )
            val uri = StrokeMath.getShareableUri(context, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, strings.sketchTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            showToast(strings.sketchExported)
        } catch (e: Exception) {
            showToast(strings.saveError)
        }
    }

    // --- Settings, Backup & Language ---

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
        repository.saveReminder(current.copy(enabled = enabled))
    }

    fun setReminderTime(timeStr: String) {
        val current = reminder.value
        repository.saveReminder(current.copy(time = timeStr))
    }

    fun exportBackup(context: Context) {
        val strings = I18n.forLanguage(language.value)
        try {
            val file = repository.exportBackupToFile()
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
            vibrate(14)
            showToast(strings.importedToast)
        } else {
            showToast(strings.importError)
        }
    }

    private fun formatDateKey(year: Int, month: Int, day: Int): String {
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    }
}
