package com.example

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Complete, uncompressed I18N translation dictionary for Kannach (كُنَّاش).
 * Preserves 100% of the Arabic and English text, reminder messages,
 * month names, and RTL/LTR directionality from the source HTML.
 */
object I18n {

    data class ColorOption(val key: String, val hex: String)

    val COLORS: List<ColorOption> = listOf(
        ColorOption("green", "#5aa25f"),
        ColorOption("orange", "#e9963e"),
        ColorOption("blue", "#4d74aa"),
        ColorOption("lightblue", "#74b8ca"),
        ColorOption("red", "#d95f4c")
    )

    fun getMoodName(hex: String, language: String): String {
        val rtl = isRtl(language)
        return when (hex.lowercase()) {
            "#5aa25f" -> if (rtl) "سكينة خضراء" else "Calm Green"
            "#e9963e" -> if (rtl) "دفء الشمس" else "Warm Sun"
            "#4d74aa" -> if (rtl) "هدوء المساء" else "Evening Blue"
            "#74b8ca" -> if (rtl) "غيمة صافية" else "Clear Sky"
            "#d95f4c" -> if (rtl) "نبض وشغف" else "Lively Spark"
            else -> if (rtl) "لون فريد" else "Unique Mood"
        }
    }

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

    data class Strings(
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
        val galleryCount: String,
        val galleryEmpty: String,
        val onThisDay: String,
        val dismiss: String,
        val ok: String,
        val importBtn: String,
        val importedToast: String,
        val importError: String,
        val wrapBtn: String,
        val wrapTitle: String,
        val wrapDays: String,
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
        val capsuleReady: String,
        val capsuleSavedToast: String,
        val capsuleDateError: String,
        val capsuleReadyBanner: String,
        val reminderTitle: String,
        val reminderToggle: String,
        val reminderTimeLabel: String,
        val reminderMsgs: List<String>,
        val months: List<String>,
        val draw: String,
        val sketchTitle: String,
        val sketchIntro: String,
        val sketchShareBtn: String,
        val sketchExported: String
    )

    val EN = Strings(
        dir = "ltr",
        appTitle = "Daily Garden",
        appSub = "a kannach of days",
        save = "Save",
        cancel = "Cancel",
        delete = "Delete",
        undo = "Undo",
        clear = "Clear",
        settings = "Settings",
        language = "Language",
        data = "Save & restore",
        about = "About",
        all = "All",
        favorites = "Favorites",
        today = "today",
        prevYear = "Previous year",
        nextYear = "Next year",
        openSettings = "Settings",
        notYet = "Not yet \u2014 this day hasn\u2019t bloomed",
        writeSomething = "Write a little or draw a little first",
        emptyHint = "Tap any day and plant your first little doodle",
        noFavs = "No favorites yet \u2014 mark an entry as favorite to keep it here",
        drawLabel = "leave your little mark for today",
        drawHint = "draw a tiny shape with your finger",
        colorLabel = "pick today\u2019s color",
        favLabel = "Favorite",
        editLabel = "Edit",
        entryPlaceholder = "Dear garden, today\u2026",
        confirmDeleteTitle = "Tear this page out?",
        confirmDeleteMsg = "This entry will be gone for good.",
        confirmCloseTitle = "Leave without saving?",
        confirmCloseMsg = "Your words and doodle aren\u2019t saved yet.",
        delYes = "Yes, remove",
        discard = "Discard",
        keep = "Keep writing",
        exportBtn = "Save a copy of your diary",
        exportedToast = "Your copy is ready to save",
        saveError = "Could not save \u2014 try again",
        aboutText = "Kannach is a quiet paper diary. Each day you write a few words and draw one tiny shape \u2014 by the end of the year your days have grown into a little garden. Everything stays on your device.",
        openGallery = "Your little garden",
        galleryTitle = "Your little garden",
        galleryCount = "{n} days written so far",
        galleryEmpty = "Nothing planted yet \u2014 your gallery will grow here.",
        onThisDay = "On this day, {y}\u2026",
        dismiss = "Dismiss",
        ok = "OK",
        importBtn = "Restore a saved copy",
        importedToast = "Your diary was restored",
        importError = "That file doesn\u2019t look like a Kannach copy",
        wrapBtn = "Year Wrap",
        wrapTitle = "Year Wrap",
        wrapDays = "days written",
        wrapStreak = "longest streak",
        wrapColor = "most-used color",
        wrapFavs = "favorites",
        wrapSub = "your garden this year",
        wrapEmpty = "Nothing planted yet this year to sum up.",
        capsuleTitle = "A Letter to Later",
        capsuleBtn = "Write a letter to future you",
        capsuleIntro = "Write a few words to your future self, and pick a day for it to open — you won\u2019t see it before then.",
        capsuleWriteLabel = "Your letter",
        capsulePlaceholder = "Dear future me\u2026",
        capsuleDateLabel = "Opens on",
        capsuleSaveBtn = "Seal the letter",
        capsuleEmptyList = "No letters waiting yet.",
        capsuleLocked = "Locked until {d}",
        capsuleReady = "Ready to open",
        capsuleSavedToast = "Sealed until that day",
        capsuleDateError = "Pick a day in the future",
        capsuleReadyBanner = "A letter from your past self arrived today",
        reminderTitle = "Daily reminder",
        reminderToggle = "Remind me to write",
        reminderTimeLabel = "At",
        reminderMsgs = listOf(
            "Today has a color. What\u2019s yours?",
            "Your garden is one dot lighter today \u2014 want to fix that?",
            "A tiny doodle takes ten seconds. Today\u2019s is waiting.",
            "Somewhere in today, there\u2019s a sentence worth keeping.",
            "The page is still blank. That\u2019s kind of exciting.",
            "Future you will want to know what today felt like.",
            "No pressure \u2014 even one word grows the garden."
        ),
        months = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        ),
        draw = "Draw",
        sketchTitle = "Just Draw",
        sketchIntro = "A free page \u2014 draw anything, then save or share it.",
        sketchShareBtn = "Save or share this drawing",
        sketchExported = "Ready to save or share"
    )

    val AR = Strings(
        dir = "rtl",
        appTitle = "حديقة الأيام",
        appSub = "كُنَّاش يومي",
        save = "حفظ",
        cancel = "إلغاء",
        delete = "حذف",
        undo = "تراجع",
        clear = "مسح",
        settings = "الإعدادات",
        language = "اللغة",
        data = "الحفظ والاسترجاع",
        about = "حول",
        all = "الكل",
        favorites = "المفضلة",
        today = "اليوم",
        prevYear = "السنة السابقة",
        nextYear = "السنة التالية",
        openSettings = "الإعدادات",
        notYet = "ليس بعد — هذا اليوم لم يُزهر",
        writeSomething = "اكتب شيئًا أو ارسم شيئًا أولًا",
        emptyHint = "المس أي يوم وازرع أول رسمة صغيرة",
        noFavs = "لا مفضّلات بعد — علّم تدوينة كمفضّلة لتبقى هنا",
        drawLabel = "اترك بصمتك الصغيرة لهذا اليوم",
        drawHint = "ارسم شكلًا صغيرًا بإصبعك",
        colorLabel = "اختر لون اليوم",
        favLabel = "مفضلة",
        editLabel = "تعديل",
        entryPlaceholder = "عزيزتي الحديقة، اليوم…",
        confirmDeleteTitle = "ننتزع هذه الصفحة؟",
        confirmDeleteMsg = "سيُحذف هذا اليوم نهائيًا.",
        confirmCloseTitle = "المغادرة دون حفظ؟",
        confirmCloseMsg = "كلماتك ورسمتك لم تُحفَظ بعد.",
        delYes = "نعم، احذف",
        discard = "تجاهل",
        keep = "متابعة الكتابة",
        exportBtn = "احفظ نسخة من يومياتك",
        exportedToast = "نسختك جاهزة للحفظ",
        saveError = "تعذر الحفظ — حاول مرة أخرى",
        aboutText = "كُنَّاش دفتر ورقي هادئ. تكتب كل يوم كلمات قليلة وترسم شكلًا صغيرًا واحدًا، حتى تتحول أيامك مع نهاية السنة إلى حديقة صغيرة. كل شيء يبقى على جهازك.",
        openGallery = "حديقتك الصغيرة",
        galleryTitle = "حديقتك الصغيرة",
        galleryCount = "{n} يومًا كُتب حتى الآن",
        galleryEmpty = "لم يُزرع شيء بعد — ستنمو حديقتك هنا.",
        onThisDay = "في مثل هذا اليوم، {y}…",
        dismiss = "إغلاق",
        ok = "حسنًا",
        importBtn = "استرجع نسخة محفوظة",
        importedToast = "تم استرجاع يومياتك",
        importError = "هذا الملف لا يبدو نسخة من كُنَّاش",
        wrapBtn = "موسم السنة",
        wrapTitle = "موسم السنة",
        wrapDays = "يومًا مكتوبًا",
        wrapStreak = "أطول سلسلة أيام",
        wrapColor = "اللون الأكثر استخدامًا",
        wrapFavs = "مفضّلة",
        wrapSub = "حديقتك في هذه السنة",
        wrapEmpty = "لا شيء بعد في هذه السنة لتلخيصه.",
        capsuleTitle = "رسالة إلى الغد",
        capsuleBtn = "اكتب رسالة لنفسك المستقبلية",
        capsuleIntro = "اكتب كلمات لنفسك القادمة، واختر يومًا تُفتح فيه — لن تراها قبل ذلك اليوم.",
        capsuleWriteLabel = "رسالتك",
        capsulePlaceholder = "رسالة إلى الغد…",
        capsuleDateLabel = "تُفتح في",
        capsuleSaveBtn = "اختم الرسالة",
        capsuleEmptyList = "لا رسائل مؤجلة بعد.",
        capsuleLocked = "مقفلة حتى {d}",
        capsuleReady = "جاهزة للفتح",
        capsuleSavedToast = "أُغلقت الرسالة حتى ذلك اليوم",
        capsuleDateError = "اختر يومًا في المستقبل",
        capsuleReadyBanner = "رسالة من نفسك القديمة وصلت اليوم",
        reminderTitle = "تذكير يومي",
        reminderToggle = "ذكّرني بالكتابة",
        reminderTimeLabel = "في الساعة",
        reminderMsgs = listOf(
            "لكل يوم لون. ما هو لونك اليوم؟",
            "حديقتك اليوم فيها نقطة فارغة — تملأها؟",
            "رسمة صغيرة تأخذ عشر ثوانٍ، رسمة اليوم بانتظارك",
            "في مكان ما من يومك جملة تستحق أن تُكتب",
            "الصفحة ما زالت بيضاء، وهذا مثير بعض الشيء",
            "نفسك القادمة تريد أن تعرف كيف كان إحساسك اليوم",
            "بلا أي ضغط — حتى كلمة واحدة تكبّر الحديقة"
        ),
        months = listOf(
            "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
            "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
        ),
        draw = "ارسم",
        sketchTitle = "فقط ارسم",
        sketchIntro = "صفحة حرة — ارسم أي شيء، ثم احفظه أو شاركه.",
        sketchShareBtn = "احفظ أو شارك هذه الرسمة",
        sketchExported = "جاهزة للحفظ أو المشاركة"
    )

    fun forLanguage(lang: String): Strings {
        return if (lang.lowercase(Locale.ROOT).startsWith("ar")) AR else EN
    }

    fun isRtl(lang: String): Boolean {
        return lang.lowercase(Locale.ROOT).startsWith("ar")
    }

    fun seasonOf(month: Int): String {
        return when (month) {
            12, 1, 2 -> "winter"
            3, 4, 5 -> "spring"
            6, 7, 8 -> "summer"
            else -> "autumn"
        }
    }

    fun formatYear(year: Int, lang: String): String {
        val locale = if (isRtl(lang)) Locale("ar") else Locale.US
        return String.format(locale, "%d", year)
    }

    fun formatMonthLabel(year: Int, month: Int, lang: String): String {
        val strings = forLanguage(lang)
        val monthName = strings.months.getOrElse(month - 1) { "" }
        return "$monthName ${formatYear(year, lang)}"
    }

    fun formatDateTitle(dateKey: String, lang: String): String {
        val parts = dateKey.split("-").mapNotNull { it.toIntOrNull() }
        if (parts.size != 3) return dateKey
        val year = parts[0]
        val month = parts[1]
        val day = parts[2]

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
        }
        val locale = if (isRtl(lang)) Locale("ar") else Locale.US
        val format = SimpleDateFormat("EEEE, d MMMM yyyy", locale)
        return format.format(cal.time)
    }
}
