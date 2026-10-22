package com.example

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * Handles scheduling and displaying local OS notifications for:
 * 1. Daily writing reminders (with time-of-day & comeback pools, NT1–NT3).
 * 2. Time Capsule delivery on the unlocked date.
 * 3. Monthly backup copy reminder.
 */
class KannachAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val repo = KannachRepository(context)

        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val reminder = repo.getReminder()
            if (reminder.enabled) {
                NotificationHelper.scheduleDailyReminder(context, reminder.time)
            }
            if (repo.getUserSettings().copyReminder == "monthly") {
                NotificationHelper.scheduleMonthlyCopyReminder(context)
            }
            if (ENABLE_LETTERS) {
                repo.getCapsules().filter { !it.opened }.forEach { capsule ->
                    NotificationHelper.scheduleCapsuleNotification(context, capsule)
                }
            } else {
                NotificationHelper.cancelAllCapsuleNotifications(context, repo.getCapsules())
            }
            return
        }

        val type = intent.getStringExtra(EXTRA_NOTIF_TYPE) ?: TYPE_REMINDER
        val lang = repo.getLanguage()
        val strings = I18n.forLanguage(lang)

        ensureChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (type) {
            TYPE_REMINDER -> {
                val reminder = repo.getReminder()
                if (!reminder.enabled) return

                val settings = repo.getUserSettings()
                val todayDow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

                // NT3: Never guilt. If today already has an entry, send nothing.
                val hasTodayEntry = repo.getEntry(todayKey) != null
                if (!hasTodayEntry && settings.reminderDays.contains(todayDow)) {
                    val scheduledTime = intent.getStringExtra(EXTRA_SCHEDULED_TIME)
                        ?.takeIf { it.isNotBlank() }
                        ?: reminder.time

                    val message = NotificationHelper.pickAndRecordReminderMessage(
                        repo = repo,
                        strings = strings,
                        scheduledTime = scheduledTime,
                        todayKey = todayKey
                    )

                    val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_stat_kannach)
                        .setColor(0xFFC0683A.toInt())
                        .setContentTitle(strings.appTitle)
                        .setContentText(message)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .build()

                    notificationManager.notify(NOTIF_ID_REMINDER, notif)
                }

                // Reschedule for next enabled day
                NotificationHelper.scheduleDailyReminder(context, reminder.time)
            }

            TYPE_CAPSULE -> {
                if (!ENABLE_LETTERS) return
                val capsuleTitle = strings.capsuleTitle
                val capsuleSub = strings.capsuleReadyBanner

                val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_kannach)
                    .setColor(0xFFC0683A.toInt())
                    .setContentTitle(capsuleTitle)
                    .setContentText(capsuleSub)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()

                val capsuleId = intent.getIntExtra(EXTRA_CAPSULE_ID, 2000)
                notificationManager.notify(capsuleId, notif)
            }

            TYPE_COPY_REMINDER -> {
                val settings = repo.getUserSettings()
                if (settings.copyReminder != "monthly") return

                val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_kannach)
                    .setColor(0xFFC0683A.toInt())
                    .setContentTitle(strings.appTitle)
                    .setContentText(strings.copyReminderNotifMsg)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(strings.copyReminderNotifMsg))
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()

                notificationManager.notify(NOTIF_ID_COPY_REMINDER, notif)
                NotificationHelper.scheduleMonthlyCopyReminder(context)
            }
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Kannach Notifications"
            val descriptionText = "Daily writing reminders and time capsule notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "kannach_daily_channel"
        const val NOTIF_ID_REMINDER = 1001
        const val NOTIF_ID_TEST_NUDGE = 1002
        const val NOTIF_ID_COPY_REMINDER = 1003
        const val EXTRA_NOTIF_TYPE = "extra_notif_type"
        const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
        const val EXTRA_CAPSULE_ID = "extra_capsule_id"
        const val EXTRA_CAPSULE_STRING_ID = "extra_capsule_string_id"
        const val TYPE_REMINDER = "reminder"
        const val TYPE_CAPSULE = "capsule"
        const val TYPE_COPY_REMINDER = "copy_reminder"
    }
}

object NotificationHelper {

    fun capsuleRequestCode(capsuleId: String): Int {
        return 2000 + (capsuleId.hashCode() and 0x0FFFFFFF)
    }

    /**
     * NT1: Resolves the time-of-day pool from a scheduled time string ("HH:mm"),
     * falling back to [fallbackHour] (current hour of day):
     * - MORNING: before 12:00 (0..11)
     * - AFTERNOON: 12:00–16:59 (12..16)
     * - EVENING: 17:00–20:59 (17..20)
     * - NIGHT: 21:00+ (21..23)
     */
    fun poolForTime(
        scheduledTime: String?,
        fallbackHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    ): NotificationPool {
        val parsedHour = scheduledTime
            ?.trim()
            ?.split(":")
            ?.firstOrNull()
            ?.toIntOrNull()
            ?.takeIf { it in 0..23 }
        val hour = parsedHour ?: fallbackHour.coerceIn(0, 23)
        return when {
            hour < 12 -> NotificationPool.MORNING
            hour in 12..16 -> NotificationPool.AFTERNOON
            hour in 17..20 -> NotificationPool.EVENING
            else -> NotificationPool.NIGHT
        }
    }

    /**
     * NT1: Resolves the active [NotificationPool].
     * - If the user has had no entry for 3 or more days, returns [NotificationPool.COMEBACK]
     *   (which overrides the time pools).
     * - Otherwise chooses the time pool from [scheduledTime], falling back to the current time.
     */
    fun resolvePool(
        repo: KannachRepository,
        scheduledTime: String? = repo.getReminder().time.takeIf { it.isNotBlank() },
        todayKey: String? = null,
        fallbackHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    ): NotificationPool {
        val daysSince = repo.daysSinceLastEntry(todayKey)
        if (daysSince != null && daysSince >= 3) {
            return NotificationPool.COMEBACK
        }
        return poolForTime(scheduledTime, fallbackHour)
    }

    /**
     * NT2: Stores the last 5 shown indices per pool (`notif_recent_<pool>`) and
     * chooses randomly among the rest.
     */
    fun pickAndRecordReminderMessage(
        repo: KannachRepository,
        strings: I18n.Strings,
        scheduledTime: String? = repo.getReminder().time.takeIf { it.isNotBlank() },
        todayKey: String? = null,
        poolOverride: NotificationPool? = null
    ): String {
        val pool = poolOverride ?: resolvePool(repo, scheduledTime, todayKey)
        val msgs = strings.messagesForPool(pool)
        if (msgs.isEmpty()) return ""

        val recent = repo.getRecentNotifIndices(pool)
        val candidates = msgs.indices.filter { it !in recent }
        val chosenIdx = when {
            candidates.isNotEmpty() -> candidates[Random.nextInt(candidates.size)]
            msgs.size > 1 -> {
                val lastShown = recent.lastOrNull()
                val nonLast = msgs.indices.filter { it != lastShown }
                nonLast[Random.nextInt(nonLast.size)]
            }
            else -> 0
        }

        repo.recordNotifIndex(pool, chosenIdx)
        return msgs[chosenIdx]
    }

    /**
     * Immediately sends a test nudge notification using the active pool and non-repeating
     * selection (NT1 & NT2), and returns the chosen message text.
     */
    fun sendTestNudge(context: Context): String {
        val repo = KannachRepository(context)
        val lang = repo.getLanguage()
        val strings = I18n.forLanguage(lang)
        val reminder = repo.getReminder()
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        val message = pickAndRecordReminderMessage(
            repo = repo,
            strings = strings,
            scheduledTime = reminder.time.takeIf { it.isNotBlank() },
            todayKey = todayKey
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    KannachAlarmReceiver.CHANNEL_ID,
                    "Kannach Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.createNotificationChannel(channel)
            }
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notif = NotificationCompat.Builder(context, KannachAlarmReceiver.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_kannach)
                .setColor(0xFFC0683A.toInt())
                .setContentTitle(strings.appTitle)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.notify(KannachAlarmReceiver.NOTIF_ID_TEST_NUDGE, notif)
        } catch (_: Exception) {}

        return message
    }

    fun scheduleDailyReminder(context: Context, timeStr: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val repo = KannachRepository(context)
        val enabledDays = repo.getUserSettings().reminderDays
        if (enabledDays.isEmpty()) {
            cancelDailyReminder(context)
            return
        }

        val parts = timeStr.split(":").mapNotNull { it.toIntOrNull() }
        val hour = if (parts.isNotEmpty()) parts[0] else 20
        val minute = if (parts.size > 1) parts[1] else 0

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
            var guard = 0
            while (!enabledDays.contains(get(Calendar.DAY_OF_WEEK)) && guard < 7) {
                add(Calendar.DAY_OF_YEAR, 1)
                guard++
            }
        }

        val intent = Intent(context, KannachAlarmReceiver::class.java).apply {
            putExtra(KannachAlarmReceiver.EXTRA_NOTIF_TYPE, KannachAlarmReceiver.TYPE_REMINDER)
            putExtra(KannachAlarmReceiver.EXTRA_SCHEDULED_TIME, timeStr)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            KannachAlarmReceiver.NOTIF_ID_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                pendingIntent
            )
        } catch (_: Exception) {}
    }

    fun cancelDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, KannachAlarmReceiver::class.java).apply {
            putExtra(KannachAlarmReceiver.EXTRA_NOTIF_TYPE, KannachAlarmReceiver.TYPE_REMINDER)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            KannachAlarmReceiver.NOTIF_ID_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        } catch (_: Exception) {}
    }

    fun scheduleMonthlyCopyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 30)
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val intent = Intent(context, KannachAlarmReceiver::class.java).apply {
            putExtra(KannachAlarmReceiver.EXTRA_NOTIF_TYPE, KannachAlarmReceiver.TYPE_COPY_REMINDER)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            KannachAlarmReceiver.NOTIF_ID_COPY_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                pendingIntent
            )
        } catch (_: Exception) {}
    }

    fun cancelMonthlyCopyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, KannachAlarmReceiver::class.java).apply {
            putExtra(KannachAlarmReceiver.EXTRA_NOTIF_TYPE, KannachAlarmReceiver.TYPE_COPY_REMINDER)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            KannachAlarmReceiver.NOTIF_ID_COPY_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        } catch (_: Exception) {}
    }

    /**
     * CP9 & RB1: Schedules a local notification for [capsule] at 09:00 AM on its [TimeCapsule.unlockDate].
     */
    fun scheduleCapsuleNotification(context: Context, capsule: TimeCapsule) {
        if (!ENABLE_LETTERS) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val parts = capsule.unlockDate.split("-").mapNotNull { it.toIntOrNull() }
        if (parts.size != 3) return

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, parts[0])
            set(Calendar.MONTH, parts[1] - 1)
            set(Calendar.DAY_OF_MONTH, parts[2])
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val reqCode = capsuleRequestCode(capsule.id)
        val intent = Intent(context, KannachAlarmReceiver::class.java).apply {
            putExtra(KannachAlarmReceiver.EXTRA_NOTIF_TYPE, KannachAlarmReceiver.TYPE_CAPSULE)
            putExtra(KannachAlarmReceiver.EXTRA_CAPSULE_ID, reqCode)
            putExtra(KannachAlarmReceiver.EXTRA_CAPSULE_STRING_ID, capsule.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                pendingIntent
            )
        } catch (_: Exception) {}
    }

    /**
     * CP9: Cancels a scheduled unlock notification when a capsule is opened or deleted.
     */
    fun cancelCapsuleNotification(context: Context, capsuleId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val reqCode = capsuleRequestCode(capsuleId)
        val intent = Intent(context, KannachAlarmReceiver::class.java).apply {
            putExtra(KannachAlarmReceiver.EXTRA_NOTIF_TYPE, KannachAlarmReceiver.TYPE_CAPSULE)
            putExtra(KannachAlarmReceiver.EXTRA_CAPSULE_ID, reqCode)
            putExtra(KannachAlarmReceiver.EXTRA_CAPSULE_STRING_ID, capsuleId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        } catch (_: Exception) {}
    }

    /**
     * LF3: Cancels all existing capsule alarms on startup when ENABLE_LETTERS is false.
     */
    fun cancelAllCapsuleNotifications(context: Context, capsules: List<TimeCapsule>) {
        capsules.forEach { cancelCapsuleNotification(context, it.id) }
    }

    /**
     * Checks whether a capsule unlock PendingIntent is currently registered.
     */
    fun isCapsuleNotificationScheduled(context: Context, capsuleId: String): Boolean {
        val reqCode = capsuleRequestCode(capsuleId)
        val intent = Intent(context, KannachAlarmReceiver::class.java).apply {
            putExtra(KannachAlarmReceiver.EXTRA_NOTIF_TYPE, KannachAlarmReceiver.TYPE_CAPSULE)
            putExtra(KannachAlarmReceiver.EXTRA_CAPSULE_ID, reqCode)
            putExtra(KannachAlarmReceiver.EXTRA_CAPSULE_STRING_ID, capsuleId)
        }
        val existing = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        return existing != null
    }

    /**
     * Checks whether the daily reminder PendingIntent is currently registered.
     */
    fun isDailyReminderScheduled(context: Context): Boolean {
        val intent = Intent(context, KannachAlarmReceiver::class.java).apply {
            putExtra(KannachAlarmReceiver.EXTRA_NOTIF_TYPE, KannachAlarmReceiver.TYPE_REMINDER)
        }
        val existing = PendingIntent.getBroadcast(
            context,
            KannachAlarmReceiver.NOTIF_ID_REMINDER,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        return existing != null
    }
}
