package com.example

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

typealias Strings = I18n.Strings

/**
 * NT1: Notification pools replacing the flat reminderMsgs list.
 * - MORNING (before 12:00)
 * - AFTERNOON (12:00–16:59)
 * - EVENING (17:00–20:59)
 * - NIGHT (21:00+)
 * - COMEBACK (no entry for 3 or more days; overrides time pools)
 */
enum class NotificationPool {
    MORNING,
    AFTERNOON,
    EVENING,
    NIGHT,
    COMEBACK
}

object I18n {

    data class ColorOption(val key: String, val hex: String)

    val COLORS: List<ColorOption> = listOf(
        ColorOption("green", "#5aa25f"),
        ColorOption("orange", "#e9963e"),
        ColorOption("blue", "#4d74aa"),
        ColorOption("lightblue", "#74b8ca"),
        ColorOption("red", "#d95f4c")
    )

    val GENTLE_PROMPTS_AR = listOf(
        "ما هي اللحظة الصغيرة التي تود لو تحتفظ بها من اليوم؟",
        "لو كان لهذا اليوم طعم أو رائحة، ماذا سيكون؟",
        "شيء بسيط جعل قلبك أهدأ اليوم…",
        "كلمة واحدة تلخص شعورك الآن؟",
        "ما الذي أدهشك أو جعلك تبتسم اليوم؟"
    )

    val GENTLE_PROMPTS_EN = listOf(
        "What tiny moment from today do you want to keep?",
        "If today had a scent or sound, what would it be?",
        "One small thing that brought you peace today…",
        "One word that sums up how you feel right now?",
        "What made you smile or pause today?"
    )

    fun getGentlePrompts(language: String): List<String> =
        if (isRtl(language)) GENTLE_PROMPTS_AR else GENTLE_PROMPTS_EN

    class Strings(
        val dir: String,
        val appTitle: String,
        val appSub: String,
        val save: String,
        val cancel: String,
        val delete: String,
        val undo: String,
        val clear: String,
        val settings: String,
        val language: String,
        val data: String,
        val about: String,
        val all: String,
        val favorites: String,
        val today: String,
        val prevYear: String,
        val nextYear: String,
        val prevMonth: String,
        val nextMonth: String,
        val openSettings: String,
        val notYet: String,
        val writeSomething: String,
        val emptyHint: String,
        val noFavs: String,
        val drawLabel: String,
        val drawHint: String,
        val colorLabel: String,
        val favLabel: String,
        val editLabel: String,
        val entryPlaceholder: String,
        val settingsDescLanguage: String,
        val settingsDescReminder: String,
        val settingsDescSeasons: String,
        val settingsDescBackup: String,
        val settingsDescHide: String,
        val settingsDescMore: String,
        val settingsDescShare: String,
        val settingsAboutRow: String,
        val settingsDescAbout: String,
        val aboutTitle: String,
        val aboutWhatTitle: String,
        val aboutWhatBody: String,
        val aboutPrivacyTitle: String,
        val aboutPrivacyBody: String,
        val confirmDeleteTitle: String,
        val confirmDeleteMsg: String,
        val confirmCloseTitle: String,
        val confirmCloseMsg: String,
        val delYes: String,
        val discard: String,
        val keep: String,
        val exportBtn: String,
        val exportedToast: String,
        val saveError: String,
        val aboutText: String,
        val openGallery: String,
        val galleryTitle: String,
        val galleryCount: PluralString,
        val galleryEmpty: String,
        val galleryEmptyTitle: String,
        val galleryEmptySub: String,
        val plantTodayBtn: String,
        val searchHint: String,
        val searchNoMatch: String,
        val noMemoriesYet: String,
        val onThisDay: String,
        val dismiss: String,
        val ok: String,
        val importBtn: String,
        val importedToast: String,
        val importError: String,
        val wrapBtn: String,
        val wrapTitle: String,
        val wrapDays: PluralString,
        val wrapStreak: String,
        val wrapColor: String,
        val wrapFavs: String,
        val wrapSub: String,
        val wrapEmpty: String,
        val capsuleTitle: String,
        val capsuleBtn: String,
        val capsuleIntro: String,
        val capsuleWriteLabel: String,
        val capsulePlaceholder: String,
        val capsuleDateLabel: String,
        val capsuleSaveBtn: String,
        val capsuleEmptyList: String,
        val capsuleLocked: String,
        val capsuleCountdown: PluralString,
        val capsuleReady: String,
        val capsuleSavedToast: String,
        val capsuleDateError: String,
        val capsuleReadyBanner: String,
        val reminderTitle: String,
        val reminderToggle: String,
        val reminderTimeLabel: String,
        val reminderMsgs: List<String>,
        val months: List<String>,
        val weekdays: List<String>,
        val draw: String,
        val sketchTitle: String,
        val sketchIntro: String,
        val sketchShareBtn: String,
        val sketchExported: String,
        val plantBtn: String,
        val leaveMark: String,
        val draftKept: String,
        val todayWord: String,
        val surpriseMe: String,
        val shareMosaic: String,
        val openToday: String,
        val greetMorning: String,
        val greetDay: String,
        val greetEvening: String,
        val greetNight: String,
        val greetDone: String,
        val greetBack: String,
        val todayRowEmpty: String,
        val onboardingBeat1: String,
        val onboardingBeat1Sub: String,
        val onboardingBeat2: String,
        val onboardingBeat2Sub: String,
        val onboardingBeat3: String,
        val onboardingBeat3Sub: String,
        val onboardingNext: String,
        val onboardingSkip: String,
        val onboardingStart: String,
        val dockPage: String,
        val dockGarden: String,
        val dockLetters: String,
        val dockToday: String,
        val dockDraw: String,
        val dockSettings: String,
        val dockOpen: String,
        val todayCardEmpty: String,
        val monthRecapPlanted: PluralString,
        val monthRecapQuiet: String,
        val moodGreen: String,
        val moodOrange: String,
        val moodBlue: String,
        val moodLightblue: String,
        val moodRed: String,
        val moodCustom: String,
        val distYesterday: String,
        val distAgo: PluralString,
        val distWeeks: PluralString,
        val distMonths: PluralString,
        val distFuture: String,
        val recordedAt: String,
        val prevEntry: String,
        val nextEntry: String,
        val exportTitle: String,
        val exportStory: String,
        val exportSquare: String,
        val exportPage: String,
        val exportSignature: String,
        val exportSave: String,
        val exportShare: String,
        val exportSaved: String,
        val capsuleSectionWaiting: String,
        val capsuleSectionReady: String,
        val capsuleSectionOpened: String,
        val capsuleOpenedMeta: String,
        val capsuleNotYetToast: PluralString,
        val capsuleDeleteTitle: String,
        val capsuleDeleteMsg: String,
        val capsuleSalutation: String,
        val capsulePrefill: String,
        val capsuleWrittenStamp: String,
        val capsuleAddDoodle: String,
        val capsulePresetTomorrow: String,
        val capsulePreset1w: String,
        val capsulePreset1m: String,
        val capsulePreset3m: String,
        val capsulePreset6m: String,
        val capsulePreset1y: String,
        val capsulePickDate: String,
        val capsuleSealConfirmMsg: String,
        val capsuleKeepWriting: String,
        val capsuleWriteBack: String,
        val capsuleNotifPermNote: String,
        val capsuleComposeTitle: String,
        val capsuleDraftSaved: String,
        val capsuleDraftSaving: String,
        val capsuleToMe: String,
        val capsuleOpensOnFull: String,
        val capsuleTitleHint: String,
        val capsuleHelperWriteFirst: String,
        val capsuleHelperChooseDate: String,
        val capsuleDraftSheetTitle: String,
        val capsuleDraftKeep: String,
        val capsuleDraftDiscard: String,
        val capsuleDraftContinue: String,
        val capsuleSealWarning: String,
        val capsuleSealNow: String,
        val capsuleHoldToSeal: String,
        val capsuleSealNotYet: String,
        val langFollowSystem: String,
        val reminderDaysLabel: String,
        val reminderPermOn: String,
        val reminderPermOff: String,
        val reminderPermAllow: String,
        val reminderPermSettings: String,
        val reminderTestBtn: String,
        val settingsWeekStartLabel: String,
        val weekStartSat: String,
        val weekStartSun: String,
        val weekStartMon: String,
        val settingsNumeralsLabel: String,
        val numeralsWestern: String,
        val numeralsArabicIndic: String,
        val settingsHandSizeLabel: String,
        val handSizeSmall: String,
        val handSizeNormal: String,
        val handSizeLarge: String,
        val settingsHideRecents: String,
        val settingsLastBackup: String,
        val settingsBackupNever: String,
        val settingsCopyReminderLabel: String,
        val copyReminderMonthly: String,
        val copyReminderNotifMsg: String,
        val settingsSignatureExport: String,
        val settingsReplayWelcome: String,
        val settingsTellFriend: String,
        val settingsTellFriendMsg: String,
        val settingsBackupRow: String,
        val settingsMore: String,
        val madeBy: String,
        val drawWhisper1: String,
        val drawWhisper2: String,
        val drawWhisper3: String,
        val drawWhisper4: String,
        val drawSaved1: String,
        val drawSaved2: String,
        val notifMorning: List<String>,
        val notifAfternoon: List<String>,
        val notifEvening: List<String>,
        val notifNight: List<String>,
        val notifComeback: List<String>
    ) {
        fun messagesForPool(pool: NotificationPool): List<String> {
            return when (pool) {
                NotificationPool.MORNING -> notifMorning.ifEmpty { reminderMsgs }
                NotificationPool.AFTERNOON -> notifAfternoon.ifEmpty { reminderMsgs }
                NotificationPool.EVENING -> notifEvening.ifEmpty { reminderMsgs }
                NotificationPool.NIGHT -> notifNight.ifEmpty { reminderMsgs }
                NotificationPool.COMEBACK -> notifComeback.ifEmpty { reminderMsgs }
            }
        }
    }

    val EN: Strings get() = com.example.EN
    val AR: Strings get() = com.example.AR
    val FR: Strings get() = com.example.FR
    val ES: Strings get() = com.example.ES

    val all: Map<String, Strings> by lazy {
        linkedMapOf(
            "ar" to AR,
            "en" to EN,
            "fr" to FR,
            "es" to ES
        )
    }

    val LANGUAGE_LABELS: Map<String, String> = linkedMapOf(
        "ar" to "العربية",
        "en" to "English",
        "fr" to "Français",
        "es" to "Español"
    )

    @Volatile
    var useArabicIndicNumerals: Boolean = false

    fun toArabicIndicDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(('\u0660'.code + (ch.code - '0'.code)).toChar())
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun applyNumerals(
        input: String,
        lang: String,
        useArabicIndic: Boolean = useArabicIndicNumerals
    ): String {
        val isAr = isRtl(lang)
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (isAr && useArabicIndic) {
                if (ch in '0'..'9') {
                    sb.append(('\u0660'.code + (ch.code - '0'.code)).toChar())
                } else {
                    sb.append(ch)
                }
            } else {
                if (ch in '\u0660'..'\u0669') {
                    sb.append(('0'.code + (ch.code - '\u0660'.code)).toChar())
                } else {
                    sb.append(ch)
                }
            }
        }
        return sb.toString()
    }

    fun formatNumber(
        n: Int,
        lang: String,
        useArabicIndic: Boolean = useArabicIndicNumerals
    ): String {
        return applyNumerals(n.toString(), lang, useArabicIndic)
    }

    fun normalizeCode(code: String): String {
        return code.trim().lowercase(Locale.ROOT).substringBefore('-').substringBefore('_')
    }

    fun resolveSystemLanguage(): String {
        val sysLang = normalizeCode(Locale.getDefault().language)
        return if (all.containsKey(sysLang)) sysLang else "en"
    }

    fun forLanguage(code: String): Strings {
        val normalized = if (code == "system") resolveSystemLanguage() else normalizeCode(code)
        return all[normalized] ?: EN
    }

    fun isRtl(code: String): Boolean {
        val normalized = if (code == "system") resolveSystemLanguage() else normalizeCode(code)
        return normalized == "ar"
    }

    fun localeFor(code: String): Locale {
        val normalized = if (code == "system") resolveSystemLanguage() else normalizeCode(code)
        return when (normalized) {
            "ar" -> Locale("ar")
            "fr" -> Locale.FRENCH
            "es" -> Locale("es")
            else -> Locale.US
        }
    }

    fun getMoodName(hex: String, language: String): String {
        val s = forLanguage(language)
        return when (hex.lowercase(Locale.ROOT)) {
            "#5aa25f" -> s.moodGreen
            "#e9963e" -> s.moodOrange
            "#4d74aa" -> s.moodBlue
            "#74b8ca" -> s.moodLightblue
            "#d95f4c" -> s.moodRed
            else -> s.moodCustom
        }
    }

    fun getDrawWhispers(language: String): List<String> {
        val s = forLanguage(language)
        return listOf(s.drawWhisper1, s.drawWhisper2, s.drawWhisper3, s.drawWhisper4)
    }

    fun getDrawSavedWhispers(language: String): List<String> {
        val s = forLanguage(language)
        return listOf(s.drawSaved1, s.drawSaved2)
    }

    fun seasonOf(month: Int): String {
        return when (month) {
            12, 1, 2 -> "winter"
            3, 4, 5 -> "spring"
            6, 7, 8 -> "summer"
            else -> "autumn"
        }
    }

    fun formatYear(year: Int, lang: String, useArabicIndic: Boolean = useArabicIndicNumerals): String {
        return applyNumerals(String.format(localeFor(lang), "%d", year), lang, useArabicIndic)
    }

    fun formatWeekdayName(year: Int, month: Int, day: Int, lang: String): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
        }
        val locale = localeFor(lang)
        val raw = SimpleDateFormat("EEEE", locale).format(cal.time)
        return if (isRtl(lang)) raw else raw.uppercase(locale)
    }

    /**
     * C3: Full weekday names (Sun…Sat) for accessibility contentDescription.
     */
    fun getFullWeekdays(lang: String): List<String> {
        val normalized = if (lang == "system") resolveSystemLanguage() else normalizeCode(lang)
        return when (normalized) {
            "ar" -> listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
            "fr" -> listOf("Dimanche", "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi")
            "es" -> listOf("Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")
            else -> listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        }
    }

    fun formatMonthLabel(year: Int, month: Int, lang: String, useArabicIndic: Boolean = useArabicIndicNumerals): String {
        val strings = forLanguage(lang)
        val monthName = strings.months.getOrElse(month - 1) { "" }
        return "$monthName ${formatYear(year, lang, useArabicIndic)}"
    }

    /**
     * HR2: Formats weekday and date for the Today row on Home:
     * e.g., `Tue · 20 Oct` / `الثلاثاء · 20 أكتوبر` (respecting Arabic-Indic numerals setting).
     */
    fun formatTodayRowDate(
        year: Int,
        month: Int,
        day: Int,
        lang: String,
        useArabicIndic: Boolean = useArabicIndicNumerals
    ): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
        }
        val dayStr = applyNumerals(day.toString(), lang, useArabicIndic)
        return if (isRtl(lang)) {
            val dowIdx = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY + 7) % 7
            val weekday = getFullWeekdays(lang).getOrElse(dowIdx) { "" }
            val monthName = forLanguage(lang).months.getOrElse(month - 1) { "" }
            "$weekday · $dayStr $monthName"
        } else {
            val locale = localeFor(lang)
            val shortWeekday = SimpleDateFormat("EEE", locale).format(cal.time).trimEnd('.')
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
            val shortMonth = SimpleDateFormat("MMM", locale).format(cal.time).trimEnd('.')
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
            "$shortWeekday · $dayStr $shortMonth"
        }
    }

    fun formatDateTitle(dateKey: String, lang: String): String {
        val parts = dateKey.split("-").mapNotNull { it.toIntOrNull() }
        if (parts.size != 3) return applyNumerals(dateKey, lang)
        val year = parts[0]
        val month = parts[1]
        val day = parts[2]

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
        }
        val format = SimpleDateFormat("EEEE, d MMMM yyyy", localeFor(lang))
        return applyNumerals(format.format(cal.time), lang)
    }

    fun formatRecordedTime(timestampMs: Long, lang: String): String {
        if (timestampMs <= 0L) return ""
        return try {
            val cal = Calendar.getInstance().apply { timeInMillis = timestampMs }
            val timeStr = SimpleDateFormat("h:mm a", localeFor(lang)).format(cal.time)
            applyNumerals(forLanguage(lang).recordedAt.replace("{t}", timeStr), lang)
        } catch (_: Exception) {
            ""
        }
    }

    fun getRelativeTimeDistance(dateKey: String, lang: String): String {
        val parts = dateKey.split("-").mapNotNull { it.toIntOrNull() }
        if (parts.size != 3) return ""
        val targetCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, parts[0])
            set(Calendar.MONTH, parts[1] - 1)
            set(Calendar.DAY_OF_MONTH, parts[2])
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffDays = ((todayCal.timeInMillis - targetCal.timeInMillis) / (24 * 60 * 60 * 1000L)).toInt()
        val s = forLanguage(lang)

        return when {
            diffDays < 0 -> s.distFuture
            diffDays == 0 -> s.todayWord
            diffDays == 1 -> s.distYesterday
            diffDays in 2..6 -> s.distAgo.format(lang, diffDays)
            diffDays in 7..29 -> {
                val weeks = (diffDays / 7).coerceAtLeast(1)
                s.distWeeks.format(lang, weeks)
            }
            else -> {
                val months = (diffDays / 30).coerceAtLeast(1)
                s.distMonths.format(lang, months)
            }
        }
    }

    /**
     * CP2 & CP6: Formats a YYYY-MM-DD dateKey as a localized long date (e.g. "17 November 2026"),
     * never returning a raw ISO date.
     */
    fun formatLongDate(dateKey: String, lang: String): String {
        val parts = dateKey.split("-").mapNotNull { it.toIntOrNull() }
        if (parts.size != 3) return applyNumerals(dateKey, lang)
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, parts[0])
            set(Calendar.MONTH, parts[1] - 1)
            set(Calendar.DAY_OF_MONTH, parts[2])
        }
        return applyNumerals(SimpleDateFormat("d MMMM yyyy", localeFor(lang)).format(cal.time), lang)
    }

    /**
     * LS4: Postmark top line ("20 OCT" / "٢٠ أكتوبر" in Arabic-Indic digits when that setting is on).
     */
    fun formatPostmarkDayMonth(
        dateKey: String,
        lang: String,
        useArabicIndic: Boolean = useArabicIndicNumerals
    ): String {
        val parts = dateKey.split("-").mapNotNull { it.toIntOrNull() }
        if (parts.size != 3) return applyNumerals(dateKey, lang, useArabicIndic)
        val year = parts[0]
        val month = parts[1]
        val day = parts[2]
        val dayStr = applyNumerals(day.toString(), lang, useArabicIndic)
        return if (isRtl(lang)) {
            val monthName = forLanguage(lang).months.getOrElse(month - 1) { "" }
            "$dayStr $monthName"
        } else {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, day)
            }
            val locale = localeFor(lang)
            val shortMonth = SimpleDateFormat("MMM", locale).format(cal.time).trimEnd('.').uppercase(locale)
            "$dayStr $shortMonth"
        }
    }

    /**
     * LS4: Postmark bottom line (year in 9 sp, respecting Arabic-Indic digits setting).
     */
    fun formatPostmarkYear(
        dateKey: String,
        lang: String,
        useArabicIndic: Boolean = useArabicIndicNumerals
    ): String {
        val parts = dateKey.split("-").mapNotNull { it.toIntOrNull() }
        if (parts.isEmpty()) return ""
        return formatYear(parts[0], lang, useArabicIndic)
    }

    /**
     * LC1.3: Formats a YYYY-MM-DD dateKey with localized weekday + day + month + year
     * (e.g., "Sunday, 18 October 2027" in EN, "الأحد، 18 أكتوبر 2027" in AR).
     */
    fun formatFullWeekdayDate(dateKey: String, lang: String): String {
        val parts = dateKey.split("-").mapNotNull { it.toIntOrNull() }
        if (parts.size != 3) return applyNumerals(dateKey, lang)
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, parts[0])
            set(Calendar.MONTH, parts[1] - 1)
            set(Calendar.DAY_OF_MONTH, parts[2])
        }
        val normalized = if (lang == "system") resolveSystemLanguage() else normalizeCode(lang)
        val pattern = when (normalized) {
            "ar" -> "EEEE، d MMMM yyyy"
            "fr" -> "EEEE d MMMM yyyy"
            "es" -> "EEEE, d 'de' MMMM 'de' yyyy"
            else -> "EEEE, d MMMM yyyy"
        }
        return applyNumerals(SimpleDateFormat(pattern, localeFor(lang)).format(cal.time), lang)
    }

    /**
     * CP2, CP4, CP8: Formats a millisecond timestamp as a localized long date.
     */
    fun formatLongDateFromMillis(timestampMs: Long, lang: String): String {
        if (timestampMs <= 0L) return ""
        val cal = Calendar.getInstance().apply { timeInMillis = timestampMs }
        return applyNumerals(SimpleDateFormat("d MMMM yyyy", localeFor(lang)).format(cal.time), lang)
    }

    fun formatEpochLongDate(
        timestampMs: Long,
        lang: String,
        useArabicIndic: Boolean = useArabicIndicNumerals
    ): String {
        if (timestampMs <= 0L) return ""
        val cal = Calendar.getInstance().apply { timeInMillis = timestampMs }
        return applyNumerals(SimpleDateFormat("d MMMM yyyy", localeFor(lang)).format(cal.time), lang, useArabicIndic)
    }

    /**
     * CP2, CP3, CP6: Computes the number of days from today (or a reference dateKey) until [unlockDateKey].
     */
    fun daysUntilDateKey(unlockDateKey: String, todayKey: String? = null): Int {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val baseKey = todayKey ?: sdf.format(java.util.Date())
            val dUnlock = sdf.parse(unlockDateKey)?.time ?: return 1
            val dToday = sdf.parse(baseKey)?.time ?: return 1
            val diff = kotlin.math.round((dUnlock - dToday).toDouble() / (24.0 * 60.0 * 60.0 * 1000.0)).toInt()
            diff.coerceAtLeast(1)
        } catch (_: Exception) {
            1
        }
    }

    /**
     * CP8: Computes relative time distance copy from a creation timestamp in milliseconds.
     */
    fun getRelativeTimeDistanceFromMillis(timestampMs: Long, lang: String): String {
        if (timestampMs <= 0L) return ""
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date(timestampMs))
        return getRelativeTimeDistance(dateKey, lang)
    }
}
