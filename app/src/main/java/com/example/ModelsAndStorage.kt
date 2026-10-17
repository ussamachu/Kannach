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

const val SCHEMA_VERSION: Int = 2
private const val PREFS_NAME = "kannach_prefs"
private const val KEY_ENTRIES = "kannach.entries.v1"
private const val KEY_CAPSULES = "kannach.capsules.v1"
private const val KEY_REMINDER = "kannach.reminder"
private const val KEY_LANG = "kannach.lang"
private const val KEY_SCHEMA = "kannach.schema"
private const val KEY_DRAFTS = "kannach.drafts.v1"
private const val KEY_FIRST_RUN_COMPLETED = "kannach.first_run_completed"
private const val KEY_FIRST_PLANT_CELEBRATED = "kannach.first_plant_celebrated"

val DEFAULT_GREEN_HEX: String = "#5aa25f"

@Serializable
data class Point(
    val x: Float,
    val y: Float
)

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
    val opened: Boolean = false
)

@Serializable
data class ReminderConfig(
    val enabled: Boolean = false,
    val time: String = "20:00",
    val lastIdx: Int = -1
)

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
 * and data migrations matching Kannach's localStorage behavior.
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

    private val _languageFlow = MutableStateFlow(detectInitialLanguage())
    val languageFlow: StateFlow<String> = _languageFlow.asStateFlow()

    init {
        val initialLanguage = loadLanguage()
        _languageFlow.value = initialLanguage

        val rawEntries = loadRawEntries()
        val migratedEntries = migrate(rawEntries)
        _entriesFlow.value = migratedEntries
        persistEntries(migratedEntries)

        _draftsFlow.value = loadRawDrafts()
        _capsulesFlow.value = loadRawCapsules()
        _reminderFlow.value = loadRawReminder()
    }

    /**
     * Determines initial language from saved preference or system locale.
     */
    private fun detectInitialLanguage(): String {
        val saved = prefs.getString(KEY_LANG, null)
        if (!saved.isNullOrBlank()) return saved
        val sysLang = Locale.getDefault().language.lowercase(Locale.ROOT)
        return if (sysLang.startsWith("ar")) "ar" else "en"
    }

    private fun loadLanguage(): String {
        return prefs.getString(KEY_LANG, null) ?: detectInitialLanguage()
    }

    fun setLanguage(lang: String) {
        val normalized = if (lang.lowercase(Locale.ROOT).startsWith("ar")) "ar" else "en"
        prefs.edit().putString(KEY_LANG, normalized).apply()
        _languageFlow.value = normalized
    }

    fun getLanguage(): String = _languageFlow.value

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

    fun addCapsule(text: String, unlockDate: String): TimeCapsule {
        val capsule = TimeCapsule(
            id = "c" + System.currentTimeMillis(),
            text = text,
            unlockDate = unlockDate,
            createdAt = System.currentTimeMillis(),
            opened = false
        )
        val current = _capsulesFlow.value.toMutableList()
        current.add(capsule)
        _capsulesFlow.value = current
        persistCapsules(current)
        return capsule
    }

    fun markCapsuleOpened(capsuleId: String) {
        val current = _capsulesFlow.value.map {
            if (it.id == capsuleId) it.copy(opened = true) else it
        }
        _capsulesFlow.value = current
        persistCapsules(current)
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
