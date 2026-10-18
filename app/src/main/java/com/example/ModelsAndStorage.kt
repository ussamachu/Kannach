package com.example

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Kannach (كُنَّاش) Models & Storage Layer.
 * SCHEMA_VERSION = 2 strictly matches the web/Cordova JSON structure
 * for 100% export/import backward & forward compatibility.
 */

const val SCHEMA_VERSION: Int = 3
private const val PREFS_NAME = "kannach_prefs"
private const val KEY_ENTRIES = "kannach.entries.v1"
private const val KEY_CAPSULES = "kannach.capsules.v1"
private const val KEY_REMINDER = "kannach.reminder"
private const val KEY_LANG = "kannach.lang"
private const val KEY_SCHEMA = "kannach.schema"
private const val KEY_DRAFTS = "kannach.drafts.v1"
private const val KEY_FIRST_RUN_COMPLETED = "kannach.first_run_completed"
private const val KEY_FIRST_PLANT_CELEBRATED = "kannach.first_plant_celebrated"
private const val KEY_NOTIF_PERMISSION_ASKED = "kannach.notif_perm_asked"
const val KEY_USER_SETTINGS = "kannach.settings.v1"

val DEFAULT_GREEN_HEX: String = "#5aa25f"

@Serializable
data class Point(
    val x: Float,
    val y: Float
)

/**
 * SK1: Per-stroke style used ONLY by Just Draw (Draw tab).
 * Diary entries continue using List<List<Point>>.
 */
@Serializable
data class SketchStroke(
    val points: List<Point>,
    val colorHex: String,
    val width: Float
)

/**
 * SK4: Procedural paper types for Just Draw and exported drawings.
 */
enum class SketchPaperType {
    PLAIN, RULED, DOTTED, KRAFT
}

/**
 * SK7 & SK8: Export aspect ratio / pixel formats.
 */
enum class ExportFormat(val widthPx: Int, val heightPx: Int) {
    STORY(1080, 1920),
    SQUARE(1080, 1080),
    PAGE(1080, 1440)
}

@Serializable
data class EntryDraft(
    val text: String = "",
    val color: String = DEFAULT_GREEN_HEX,
    val fav: Boolean = false,
    val strokes: List<List<Point>> = emptyList(),
    val savedAt: Long = System.currentTimeMillis()
)

@Serializable
data class DayEntry(
    val text: String = "",
    val color: String = DEFAULT_GREEN_HEX,
    val fav: Boolean = false,
    val strokes: List<List<Point>> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class TimeCapsule(
    val id: String,
    val text: String,
    val unlockDate: String, // Format: YYYY-MM-DD
    val createdAt: Long = System.currentTimeMillis(),
    val opened: Boolean = false,
    val openedAt: Long? = null,
    val strokes: List<List<Point>> = emptyList(),
    val color: String = DEFAULT_GREEN_HEX
)

@Serializable
data class ReminderConfig(
    val enabled: Boolean = false,
    val time: String = "20:00",
    val lastIdx: Int = -1
)

/**
 * Phase 6 (§6): UserSettings persisted under SharedPreferences key `kannach.settings.v1`.
 * Explicitly excluded from diary backup payload.
 */
@Serializable
data class UserSettings(
    val reminderDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7), // Calendar.SUNDAY(1)..SATURDAY(7)
    val weekStartsOn: Int? = null, // null = locale default; 7 = Saturday, 1 = Sunday, 2 = Monday
    val useArabicIndicNumerals: Boolean = false, // false = Western 0–9, true = Arabic-Indic ٠–٩
    val handwritingSize: String = "normal", // "small" (18sp), "normal" (20sp), "large" (24sp)
    val seasonOverride: String = "auto", // "auto", "winter", "spring", "summer", "autumn"
    val reduceMotion: String = "system", // "system", "on"
    val hapticsEnabled: Boolean = true,
    val hideInRecents: Boolean = false,
    val lastBackupTimestamp: Long? = null,
    val copyReminder: String = "off", // "off", "monthly"
    val exportSignature: Boolean = true
) {
    val handwritingSp: Int
        get() = when (handwritingSize) {
            "small" -> 18
            "large" -> 24
            else -> 20
        }

    fun effectiveWeekStart(lang: String): Int {
        return weekStartsOn ?: defaultWeekStartForLocale(lang)
    }

    fun resolvedSeasonIndex(autoIndex: Int): Int {
        return when (seasonOverride) {
            "spring" -> 0
            "summer" -> 1
            "autumn" -> 2
            "winter" -> 3
            else -> autoIndex
        }
    }

    companion object {
        fun defaultWeekStartForLocale(lang: String): Int {
            return when (I18n.normalizeCode(lang)) {
                "ar" -> 7 // Saturday
                "fr", "es" -> 2 // Monday
                else -> 1 // Sunday
            }
        }
    }
}

@Serializable
data class BackupPayload(
    val app: String = "kannach",
    val version: Int = SCHEMA_VERSION,
    val exportedAt: String,
    val entries: Map<String, DayEntry> = emptyMap(),
    val capsules: List<TimeCapsule> = emptyList()
)

/**
 * Local offline repository managing entries, time capsules, reminder settings,
 * user settings, and data migrations matching Kannach's storage behavior.
 */
class KannachRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = true
    }

    private val _entriesFlow = MutableStateFlow<Map<String, DayEntry>>(emptyMap())
    val entriesFlow: StateFlow<Map<String, DayEntry>> = _entriesFlow.asStateFlow()

    private val _capsulesFlow = MutableStateFlow<List<TimeCapsule>>(emptyList())
    val capsulesFlow: StateFlow<List<TimeCapsule>> = _capsulesFlow.asStateFlow()

    private val _reminderFlow = MutableStateFlow(ReminderConfig())
    val reminderFlow: StateFlow<ReminderConfig> = _reminderFlow.asStateFlow()

    private val _draftsFlow = MutableStateFlow<Map<String, EntryDraft>>(emptyMap())
    val draftsFlow: StateFlow<Map<String, EntryDraft>> = _draftsFlow.asStateFlow()

    private val _userSettingsFlow = MutableStateFlow(UserSettings())
    val userSettingsFlow: StateFlow<UserSettings> = _userSettingsFlow.asStateFlow()

    private val _languageChoiceFlow = MutableStateFlow(loadLanguageChoice())
    val languageChoiceFlow: StateFlow<String> = _languageChoiceFlow.asStateFlow()

    private val _languageFlow = MutableStateFlow(detectInitialLanguage())
    val languageFlow: StateFlow<String> = _languageFlow.asStateFlow()

    init {
        val choice = loadLanguageChoice()
        _languageChoiceFlow.value = choice
        val initialLanguage = loadLanguage()
        _languageFlow.value = initialLanguage

        val loadedSettings = loadRawUserSettings()
        _userSettingsFlow.value = loadedSettings
        I18n.useArabicIndicNumerals = loadedSettings.useArabicIndicNumerals

        val rawEntries = loadRawEntries()
        val migratedEntries = migrate(rawEntries)
        _entriesFlow.value = migratedEntries
        persistEntries(migratedEntries)

        _draftsFlow.value = loadRawDrafts()
        _capsulesFlow.value = loadRawCapsules()
        _reminderFlow.value = loadRawReminder()
    }

    private fun resolveSystemLanguage(): String {
        val sysLang = I18n.normalizeCode(Locale.getDefault().language)
        return if (I18n.all.containsKey(sysLang)) sysLang else "en"
    }

    private fun loadLanguageChoice(): String {
        val saved = prefs.getString(KEY_LANG, null)
        if (saved.isNullOrBlank() || saved.equals("system", ignoreCase = true)) {
            return "system"
        }
        val normSaved = I18n.normalizeCode(saved)
        return if (I18n.all.containsKey(normSaved)) normSaved else "system"
    }

    /**
     * Determines initial language from saved preference or system locale.
     */
    private fun detectInitialLanguage(): String {
        val saved = prefs.getString(KEY_LANG, null)
        if (!saved.isNullOrBlank() && !saved.equals("system", ignoreCase = true)) {
            val normSaved = I18n.normalizeCode(saved)
            if (I18n.all.containsKey(normSaved)) return normSaved
        }
        return resolveSystemLanguage()
    }

    private fun loadLanguage(): String {
        return detectInitialLanguage()
    }

    fun setLanguage(lang: String) {
        if (lang.equals("system", ignoreCase = true)) {
            prefs.edit().putString(KEY_LANG, "system").apply()
            _languageChoiceFlow.value = "system"
            _languageFlow.value = resolveSystemLanguage()
        } else {
            val norm = I18n.normalizeCode(lang)
            val normalized = if (I18n.all.containsKey(norm)) norm else "en"
            prefs.edit().putString(KEY_LANG, normalized).apply()
            _languageChoiceFlow.value = normalized
            _languageFlow.value = normalized
        }
    }

    fun getLanguageChoice(): String = _languageChoiceFlow.value

    fun getLanguage(): String = _languageFlow.value

    // --- User Settings (kannach.settings.v1) ---

    private fun loadRawUserSettings(): UserSettings {
        val rawJson = prefs.getString(KEY_USER_SETTINGS, null) ?: return UserSettings()
        return try {
            val decoded = json.decodeFromString<UserSettings>(rawJson)
            val validDays = decoded.reminderDays.filter { it in 1..7 }.toSet()
            decoded.copy(
                reminderDays = if (validDays.isEmpty()) setOf(1, 2, 3, 4, 5, 6, 7) else validDays
            )
        } catch (_: Exception) {
            UserSettings()
        }
    }

    fun getUserSettings(): UserSettings = _userSettingsFlow.value

    fun saveUserSettings(settings: UserSettings) {
        _userSettingsFlow.value = settings
        I18n.useArabicIndicNumerals = settings.useArabicIndicNumerals
        try {
            val rawJson = json.encodeToString(settings)
            prefs.edit().putString(KEY_USER_SETTINGS, rawJson).apply()
        } catch (_: Exception) {}
    }

    fun updateUserSettings(transform: (UserSettings) -> UserSettings): UserSettings {
        val updated = transform(_userSettingsFlow.value)
        saveUserSettings(updated)
        return updated
    }

    // --- Schema Migration ---

    /**
     * Replicates the exact `migrate(store)` JS logic from the original Kannach app:
     * - Ensures `fav` is boolean (defaults to false)
     * - Ensures `strokes` is non-null list
     * - Ensures `color` is valid string (defaults to green #5aa25f)
     * - Updates saved schema version to SCHEMA_VERSION
     */
    fun migrate(rawStore: Map<String, DayEntry>): Map<String, DayEntry> {
        val savedVersion = prefs.getInt(KEY_SCHEMA, 1)
        if (savedVersion >= SCHEMA_VERSION) {
            return rawStore
        }

        val migrated = rawStore.mapValues { (_, entry) ->
            entry.copy(
                fav = entry.fav,
                strokes = entry.strokes,
                color = if (entry.color.isNotBlank()) entry.color else DEFAULT_GREEN_HEX
            )
        }
        prefs.edit().putInt(KEY_SCHEMA, SCHEMA_VERSION).apply()
        return migrated
    }

    // --- Day Entries CRUD ---

    private fun loadRawEntries(): Map<String, DayEntry> {
        val rawJson = prefs.getString(KEY_ENTRIES, null) ?: return emptyMap()
        return try {
            json.decodeFromString<Map<String, DayEntry>>(rawJson)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun persistEntries(map: Map<String, DayEntry>) {
        try {
            val rawJson = json.encodeToString(map)
            prefs.edit().putString(KEY_ENTRIES, rawJson).apply()
        } catch (e: Exception) {
            // Keep in-memory state intact even if persistence fails
        }
    }

    fun getEntries(): Map<String, DayEntry> = _entriesFlow.value

    fun getEntry(dateKey: String): DayEntry? = _entriesFlow.value[dateKey]

    fun saveEntry(dateKey: String, entry: DayEntry) {
        val current = _entriesFlow.value.toMutableMap()
        current[dateKey] = entry
        _entriesFlow.value = current
        persistEntries(current)
    }

    fun deleteEntry(dateKey: String) {
        val current = _entriesFlow.value.toMutableMap()
        if (current.remove(dateKey) != null) {
            _entriesFlow.value = current
            persistEntries(current)
        }
    }

    // --- Drafts CRUD (W4) ---

    private fun loadRawDrafts(): Map<String, EntryDraft> {
        val rawJson = prefs.getString(KEY_DRAFTS, null) ?: return emptyMap()
        return try {
            json.decodeFromString<Map<String, EntryDraft>>(rawJson)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun persistDrafts(map: Map<String, EntryDraft>) {
        try {
            val rawJson = json.encodeToString(map)
            prefs.edit().putString(KEY_DRAFTS, rawJson).apply()
        } catch (_: Exception) {}
    }

    fun getDraft(dateKey: String): EntryDraft? = _draftsFlow.value[dateKey]

    fun saveDraft(dateKey: String, draft: EntryDraft) {
        val current = _draftsFlow.value.toMutableMap()
        current[dateKey] = draft
        _draftsFlow.value = current
        persistDrafts(current)
    }

    fun clearDraft(dateKey: String) {
        val current = _draftsFlow.value.toMutableMap()
        if (current.remove(dateKey) != null) {
            _draftsFlow.value = current
            persistDrafts(current)
        }
    }

    fun hasDraft(dateKey: String): Boolean = _draftsFlow.value.containsKey(dateKey)

    // --- Time Capsules CRUD ---

    private fun loadRawCapsules(): List<TimeCapsule> {
        val rawJson = prefs.getString(KEY_CAPSULES, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<TimeCapsule>>(rawJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun persistCapsules(list: List<TimeCapsule>) {
        try {
            val rawJson = json.encodeToString(list)
            prefs.edit().putString(KEY_CAPSULES, rawJson).apply()
        } catch (e: Exception) {
            // Error handling
        }
    }

    fun getCapsules(): List<TimeCapsule> = _capsulesFlow.value

    fun addCapsule(
        text: String,
        unlockDate: String,
        strokes: List<List<Point>> = emptyList(),
        color: String = DEFAULT_GREEN_HEX
    ): TimeCapsule {
        val capsule = TimeCapsule(
            id = "c" + System.currentTimeMillis(),
            text = text,
            unlockDate = unlockDate,
            createdAt = System.currentTimeMillis(),
            opened = false,
            openedAt = null,
            strokes = strokes,
            color = color
        )
        val current = _capsulesFlow.value.toMutableList()
        current.add(capsule)
        _capsulesFlow.value = current
        persistCapsules(current)
        return capsule
    }

    fun markCapsuleOpened(capsuleId: String, openedAt: Long = System.currentTimeMillis()): TimeCapsule? {
        var openedCapsule: TimeCapsule? = null
        val current = _capsulesFlow.value.map {
            if (it.id == capsuleId) {
                val updated = it.copy(
                    opened = true,
                    openedAt = it.openedAt ?: openedAt
                )
                openedCapsule = updated
                updated
            } else {
                it
            }
        }
        _capsulesFlow.value = current
        persistCapsules(current)
        return openedCapsule
    }

    fun deleteCapsule(capsuleId: String) {
        val current = _capsulesFlow.value.filterNot { it.id == capsuleId }
        if (current.size != _capsulesFlow.value.size) {
            _capsulesFlow.value = current
            persistCapsules(current)
        }
    }

    fun isNotifPermissionAsked(): Boolean {
        return prefs.getBoolean(KEY_NOTIF_PERMISSION_ASKED, false)
    }

    fun markNotifPermissionAsked() {
        prefs.edit().putBoolean(KEY_NOTIF_PERMISSION_ASKED, true).apply()
    }

    // --- Daily Reminder Config ---

    private fun loadRawReminder(): ReminderConfig {
        val rawJson = prefs.getString(KEY_REMINDER, null) ?: return ReminderConfig()
        return try {
            json.decodeFromString<ReminderConfig>(rawJson)
        } catch (e: Exception) {
            ReminderConfig()
        }
    }

    fun getReminder(): ReminderConfig = _reminderFlow.value

    fun saveReminder(config: ReminderConfig) {
        _reminderFlow.value = config
        try {
            val rawJson = json.encodeToString(config)
            prefs.edit().putString(KEY_REMINDER, rawJson).apply()
        } catch (e: Exception) {
            // Error handling
        }
    }

    fun updateReminderLastIdx(idx: Int) {
        val updated = _reminderFlow.value.copy(lastIdx = idx)
        saveReminder(updated)
    }

    // --- First Run & Milestone Celebrations (R2, R3) ---

    fun isFirstRun(): Boolean {
        return !prefs.getBoolean(KEY_FIRST_RUN_COMPLETED, false) && _entriesFlow.value.isEmpty()
    }

    fun markFirstRunCompleted() {
        prefs.edit().putBoolean(KEY_FIRST_RUN_COMPLETED, true).apply()
    }

    fun isFirstPlantCelebrated(): Boolean {
        return prefs.getBoolean(KEY_FIRST_PLANT_CELEBRATED, false)
    }

    fun markFirstPlantCelebrated() {
        prefs.edit().putBoolean(KEY_FIRST_PLANT_CELEBRATED, true).apply()
    }

    // --- Export & Import Backup (Matching original Kannach schema v2) ---

    /**
     * Creates a complete backup JSON payload matching original Kannach export format.
     */
    fun createBackupJson(): String {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val payload = BackupPayload(
            app = "kannach",
            version = SCHEMA_VERSION,
            exportedAt = isoFormat.format(Date()),
            entries = _entriesFlow.value,
            capsules = _capsulesFlow.value
        )
        return json.encodeToString(payload)
    }

    /**
     * Writes backup JSON to a file in cache/exports for easy sharing via FileProvider.
     */
    fun exportBackupToFile(): File {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayKey = dateFormat.format(Date())
        val file = File(exportDir, "kannach-backup-$todayKey.json")
        file.writeText(createBackupJson(), Charsets.UTF_8)
        return file
    }

    /**
     * Restores entries and capsules from a backup JSON string.
     * Replicates the merge-and-migrate behavior of the original web app.
     */
    fun restoreFromBackupJson(jsonString: String): Result<Unit> {
        return try {
            val payload = json.decodeFromString<BackupPayload>(jsonString)
            if (payload.app != "kannach" && payload.entries.isEmpty() && payload.capsules.isEmpty()) {
                return Result.failure(IllegalArgumentException("Invalid Kannach backup file format"))
            }

            // Merge entries (restored entries overwrite or add to existing)
            val currentEntries = _entriesFlow.value.toMutableMap()
            payload.entries.forEach { (key, entry) ->
                currentEntries[key] = entry
            }
            val migratedEntries = migrate(currentEntries)
            _entriesFlow.value = migratedEntries
            persistEntries(migratedEntries)

            // Merge capsules (skip duplicates by id)
            val currentCapsules = _capsulesFlow.value.toMutableList()
            payload.capsules.forEach { restored ->
                if (currentCapsules.none { it.id == restored.id }) {
                    currentCapsules.add(restored)
                }
            }
            _capsulesFlow.value = currentCapsules
            persistCapsules(currentCapsules)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
