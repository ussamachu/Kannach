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
        val sketchExported: String,
        val plantBtn: String,
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
        val onboardingBeat1: String,
        val onboardingBeat1Sub: String,
        val onboardingBeat2: String,
        val onboardingBeat2Sub: String,
        val onboardingBeat3: String,
        val onboardingBeat3Sub: String,
        val onboardingNext: String,
        val onboardingSkip: String,
        val onboardingStart: String
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
        sketchExported = "Ready to save or share",
        plantBtn = "Plant it",
        draftKept = "Draft kept for later",
        todayWord = "Today",
        surpriseMe = "Surprise with a memory",
        shareMosaic = "Share year mosaic",
        openToday = "Record today's mark",
        greetMorning = "A dewy morning.. how rests your heart with the dawn?",
        greetDay = "A blessed day.. a quiet moment to plant a gentle mark",
        greetEvening = "A pleasant evening.. what paused your soul today?",
        greetNight = "Nighttime stillness.. thoughts settle and marks find peace",
        greetDone = "Today's mark rests in peace.. be well",
        greetBack = "Welcome back.. the notebook was waiting for your return",
        onboardingBeat1 = "Kannach is a quiet paper diary.",
        onboardingBeat1Sub = "An intimate space for your days and thoughts, without noise.",
        onboardingBeat2 = "A small mark for every day",
        onboardingBeat2Sub = "A few brush strokes sprout a small flower telling the story of your day.",
        onboardingBeat3 = "Touch any day to plant your first small mark.",
        onboardingBeat3Sub = "Your garden grows day by day with every memory you keep.",
        onboardingNext = "Next",
        onboardingSkip = "Skip",
        onboardingStart = "Begin your garden"
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
        writeSomething = "كتابة بضع كلمات أو رسم أثر صغير أولاً",
        emptyHint = "لمس أي يوم لغرس أول أثر صغير",
        noFavs = "لا مفضّلات بعد — تمييز أي تدوينة بنجمة لتبقى هنا",
        drawLabel = "ترك بصمة صغيرة لهذا اليوم",
        drawHint = "رسم أثر صغير باللمس",
        colorLabel = "اختيار مزاج اليوم",
        favLabel = "مفضلة",
        editLabel = "تعديل",
        entryPlaceholder = "عزيزتي الحديقة، اليوم…",
        confirmDeleteTitle = "انتزاع هذه الصفحة؟",
        confirmDeleteMsg = "سيُمحى أثر هذا اليوم نهائياً.",
        confirmCloseTitle = "المغادرة دون حفظ؟",
        confirmCloseMsg = "الكلمات والرسمة لم تُحفظا بعد.",
        delYes = "تأكيد الانتزاع",
        discard = "تجاهل",
        keep = "متابعة التدوين",
        exportBtn = "حفظ نسخة من اليوميات",
        exportedToast = "نسختك جاهزة للحفظ",
        saveError = "تعذر الحفظ — المحاولة مرة أخرى",
        aboutText = "كُنَّاش دفتر ورقي هادئ. تكتب كل يوم كلمات قليلة وترسم شكلًا صغيرًا واحدًا، حتى تتحول أيامك مع نهاية السنة إلى حديقة صغيرة. كل شيء يبقى على جهازك.",
        openGallery = "حديقتك الصغيرة",
        galleryTitle = "حديقتك الصغيرة",
        galleryCount = "{n} يومًا كُتب حتى الآن",
        galleryEmpty = "لم يُزرع شيء بعد — ستنمو حديقتك هنا.",
        onThisDay = "في مثل هذا اليوم، {y}…",
        dismiss = "إغلاق",
        ok = "حسنًا",
        importBtn = "استرجاع نسخة محفوظة",
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
        capsuleBtn = "كتابة رسالة لنفسك المستقبلية",
        capsuleIntro = "تدوين كلمات لنفسك القادمة، واختيار يوم تُفتح فيه — لن تراها قبل ذلك اليوم.",
        capsuleWriteLabel = "رسالتك",
        capsulePlaceholder = "رسالة إلى الغد…",
        capsuleDateLabel = "تُفتح في",
        capsuleSaveBtn = "ختم الرسالة",
        capsuleEmptyList = "لا رسائل مؤجلة بعد.",
        capsuleLocked = "مقفلة حتى {d}",
        capsuleReady = "جاهزة للفتح",
        capsuleSavedToast = "أُغلقت الرسالة حتى ذلك اليوم",
        capsuleDateError = "اختيار يوم في المستقبل",
        capsuleReadyBanner = "رسالة من نفسك القديمة وصلت اليوم",
        reminderTitle = "تذكير يومي",
        reminderToggle = "التذكير بالتدوين",
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
        draw = "رسم",
        sketchTitle = "مساحة الرسم",
        sketchIntro = "صفحة حرة — رسم ما يخطر في البال، ثم الحفظ أو المشاركة.",
        sketchShareBtn = "حفظ أو مشاركة هذا الرسم",
        sketchExported = "جاهزة للحفظ أو المشاركة",
        plantBtn = "غرس الأثر",
        draftKept = "حُفظت المسودة لوقت لاحق",
        todayWord = "اليوم",
        surpriseMe = "مفاجأة بذكرى",
        shareMosaic = "مشاركة فسيفساء السنة",
        openToday = "تدوين أثر اليوم",
        greetMorning = "صباح نديّ.. كيف حال قلبك مع بزوغ الضوء؟",
        greetDay = "يوم مبارك.. لحظة هدوء لغرس أثر لطيف",
        greetEvening = "مساء طيب.. ما الذي استوقف روحك اليوم؟",
        greetNight = "سكون الليل.. تهادت الخواطر وآن للأثر أن يهدأ",
        greetDone = "أثر اليوم مغروس بسلام.. دمت بخير",
        greetBack = "أهلاً بالعودة.. الدفتر كان بانتظار خطوتك",
        onboardingBeat1 = "كُنَّاش دفتر ورقي هادئ.",
        onboardingBeat1Sub = "مساحة حميمة تسكن فيها أيامك وخواطرك دون ضجيج.",
        onboardingBeat2 = "أثر مرسوم لكل يوم",
        onboardingBeat2Sub = "بضع ضربات قلم تنبت زهرة صغيرة تحكي ما كان.",
        onboardingBeat3 = "المس أي يوم لتغرس أول رسمة صغيرة.",
        onboardingBeat3Sub = "تنمو حديقتك يوماً بعد يوم مع كل أثر تدونه.",
        onboardingNext = "متابعة",
        onboardingSkip = "تخطي",
        onboardingStart = "غرس الأثر الأول"
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
        val isAr = isRtl(lang)

        return when {
            diffDays == 0 -> if (isAr) "اليوم" else "Today"
            diffDays == 1 -> if (isAr) "أمس" else "Yesterday"
            diffDays in 2..10 -> if (isAr) "منذ $diffDays أيام" else "$diffDays days ago"
            diffDays in 11..29 -> if (isAr) "منذ $diffDays يوماً" else "$diffDays days ago"
            diffDays in 30..364 -> {
                val months = (diffDays / 30).coerceAtLeast(1)
                if (isAr) {
                    when (months) {
                        1 -> "منذ شهر"
                        2 -> "منذ شهرين"
                        in 3..10 -> "منذ $months أشهر"
                        else -> "منذ $months شهراً"
                    }
                } else {
                    if (months == 1) "1 month ago" else "$months months ago"
                }
            }
            diffDays >= 365 -> {
                val years = (diffDays / 365).coerceAtLeast(1)
                if (isAr) {
                    when (years) {
                        1 -> "منذ عام"
                        2 -> "منذ عامين"
                        in 3..10 -> "منذ $years أعوام"
                        else -> "منذ $years عاماً"
                    }
                } else {
                    if (years == 1) "1 year ago" else "$years years ago"
                }
            }
            else -> if (isAr) "في المستقبل" else "In the future"
        }
    }
}
